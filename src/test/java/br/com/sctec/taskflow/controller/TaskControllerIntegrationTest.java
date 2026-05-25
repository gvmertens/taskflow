package br.com.sctec.taskflow.controller;

import br.com.sctec.taskflow.domain.enums.Criticidade;
import br.com.sctec.taskflow.domain.enums.StatusTarefa;
import br.com.sctec.taskflow.repository.TaskRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração para {@link TaskController}.
 *
 * <p>Sobe o contexto Spring completo com um banco PostgreSQL real via
 * Testcontainers. O Flyway aplica as migrations automaticamente antes
 * de cada suite.</p>
 *
 * <p>Cada teste parte de um banco limpo (limpeza via {@link #limparBanco()})
 * para garantir isolamento entre os cenários.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("TaskController — testes de integração")
class TaskControllerIntegrationTest {

    // =========================================================================
    // Infraestrutura — Testcontainers
    // =========================================================================

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("taskflow_test")
            .withUsername("taskflow")
            .withPassword("taskflow");

    @DynamicPropertySource
    static void configurarDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.properties.hibernate.dialect",
                () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    // =========================================================================
    // Dependências injetadas
    // =========================================================================

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaskRepository taskRepository;

    // =========================================================================
    // Setup
    // =========================================================================

    @BeforeEach
    void limparBanco() {
        taskRepository.deleteAll();
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private static final String BASE_URL = "/api/v1/tasks";
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    /** Monta o JSON de request com os campos fornecidos. */
    private String requestJson(String titulo, String prazo, String criticidade) {
        return """
                {
                  "titulo": "%s",
                  "descricao": "Descrição de teste",
                  "prazo": "%s",
                  "criticidade": "%s"
                }
                """.formatted(titulo, prazo, criticidade);
    }

    /** Extrai o campo "id" do JSON de resposta. */
    private String extrairId(MvcResult result) throws Exception {
        String json = result.getResponse().getContentAsString();
        return objectMapper.readTree(json).get("id").asText();
    }

    // =========================================================================
    // POST /api/v1/tasks
    // =========================================================================

    @Nested
    @DisplayName("POST /api/v1/tasks — criação de tarefa")
    class CriacaoTarefa {

        @Test
        @DisplayName("Deve retornar 201 e calcular URGENTE para prazo vencido")
        void criarTarefa_prazoVencido_deveRetornar201ComUrgente() throws Exception {
            String prazoVencido = LocalDate.now().minusDays(1).format(ISO_DATE);
            // Nota: @FutureOrPresent no DTO impede prazo passado via validação.
            // Usamos prazo = hoje para simular o limite URGENTE sem violar a constraint.
            String prazoHoje = LocalDate.now().format(ISO_DATE);

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson("Tarefa urgente", prazoHoje, "BAIXA")))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.criticidade").value("URGENTE"))
                    .andExpect(jsonPath("$.status").value("PENDENTE"))
                    .andExpect(header().string("Location", containsString(BASE_URL)));
        }

        @Test
        @DisplayName("Deve retornar 201 e calcular BAIXA para prazo em 30 dias sem bônus")
        void criarTarefa_prazoDistante_deveRetornar201ComBaixa() throws Exception {
            String prazoDistante = LocalDate.now().plusDays(30).format(ISO_DATE);

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson("Tarefa futura", prazoDistante, "BAIXA")))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.criticidade").value("BAIXA"))
                    .andExpect(jsonPath("$.status").value("PENDENTE"));
        }

        @Test
        @DisplayName("Deve retornar 201 e elevar BAIXA para MEDIA quando criticidade ALTA e prazo em 30 dias")
        void criarTarefa_prazoDistante_criticidadeAlta_deveElevarParaMedia() throws Exception {
            String prazoDistante = LocalDate.now().plusDays(30).format(ISO_DATE);

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson("Tarefa work", prazoDistante, "ALTA")))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.criticidade").value("MEDIA"));
        }

        @Test
        @DisplayName("Deve retornar 201 e calcular ALTA para prazo em 1 dia sem bônus")
        void criarTarefa_prazoEmUmDia_deveRetornarAlta() throws Exception {
            String prazoAmanha = LocalDate.now().plusDays(1).format(ISO_DATE);

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson("Tarefa próxima", prazoAmanha, "BAIXA")))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.criticidade").value("ALTA"));
        }

        @Test
        @DisplayName("Deve retornar 201 e calcular MEDIA para prazo em 5 dias sem bônus")
        void criarTarefa_prazoEmCincoDias_deveRetornarMedia() throws Exception {
            String prazo = LocalDate.now().plusDays(5).format(ISO_DATE);

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson("Tarefa média", prazo, "BAIXA")))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.criticidade").value("MEDIA"));
        }

        @Test
        @DisplayName("Deve retornar 400 quando título está em branco")
        void criarTarefa_tituloEmBranco_deveRetornar400() throws Exception {
            String prazo = LocalDate.now().plusDays(5).format(ISO_DATE);

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson("", prazo, "BAIXA")))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Deve retornar 400 quando prazo está no passado")
        void criarTarefa_prazoNoPassado_deveRetornar400() throws Exception {
            String prazoPassado = LocalDate.now().minusDays(1).format(ISO_DATE);

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson("Tarefa inválida", prazoPassado, "BAIXA")))
                    .andExpect(status().isBadRequest());
        }
    }

    // =========================================================================
    // GET /api/v1/tasks
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/tasks — listagem de tarefas")
    class ListagemTarefas {

        @Test
        @DisplayName("Deve retornar 200 com lista vazia quando não há tarefas")
        void listarTarefas_semDados_deveRetornar200ComListaVazia() throws Exception {
            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content", hasSize(0)))
                    .andExpect(jsonPath("$.totalElements").value(0));
        }

        @Test
        @DisplayName("Deve retornar 200 com todas as tarefas criadas")
        void listarTarefas_comDados_deveRetornar200ComTarefas() throws Exception {
            String prazo = LocalDate.now().plusDays(10).format(ISO_DATE);

            // Cria duas tarefas
            mockMvc.perform(post(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson("Tarefa 1", prazo, "BAIXA")));
            mockMvc.perform(post(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson("Tarefa 2", prazo, "MEDIA")));

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(2)))
                    .andExpect(jsonPath("$.totalElements").value(2));
        }

        @Test
        @DisplayName("Deve retornar 200 filtrando por status PENDENTE")
        void listarTarefas_filtrandoPorStatus_deveRetornarApenasPendentes() throws Exception {
            String prazo = LocalDate.now().plusDays(10).format(ISO_DATE);

            mockMvc.perform(post(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson("Tarefa pendente", prazo, "BAIXA")));

            mockMvc.perform(get(BASE_URL).param("status", "PENDENTE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].status").value("PENDENTE"));
        }

        @Test
        @DisplayName("Deve retornar 200 filtrando por criticidade")
        void listarTarefas_filtrandoPorCriticidade_deveRetornarApenasCriticidadeFiltrada() throws Exception {
            // Prazo hoje → URGENTE
            String prazoHoje = LocalDate.now().format(ISO_DATE);
            // Prazo em 30 dias → BAIXA
            String prazoDistante = LocalDate.now().plusDays(30).format(ISO_DATE);

            mockMvc.perform(post(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson("Tarefa urgente", prazoHoje, "BAIXA")));
            mockMvc.perform(post(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson("Tarefa baixa", prazoDistante, "BAIXA")));

            mockMvc.perform(get(BASE_URL).param("criticidade", "URGENTE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].criticidade").value("URGENTE"));
        }
    }

    // =========================================================================
    // GET /api/v1/tasks/{id}
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/tasks/{id} — busca por ID")
    class BuscaPorId {

        @Test
        @DisplayName("Deve retornar 200 com a tarefa quando ID existe")
        void buscarPorId_existente_deveRetornar200() throws Exception {
            String prazo = LocalDate.now().plusDays(5).format(ISO_DATE);

            MvcResult criacao = mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson("Tarefa buscada", prazo, "BAIXA")))
                    .andReturn();

            String id = extrairId(criacao);

            mockMvc.perform(get(BASE_URL + "/{id}", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id))
                    .andExpect(jsonPath("$.titulo").value("Tarefa buscada"));
        }

        @Test
        @DisplayName("Deve retornar 404 quando ID não existe")
        void buscarPorId_inexistente_deveRetornar404() throws Exception {
            mockMvc.perform(get(BASE_URL + "/{id}", "00000000-0000-0000-0000-000000000000"))
                    .andExpect(status().isNotFound());
        }
    }

    // =========================================================================
    // PUT /api/v1/tasks/{id}
    // =========================================================================

    @Nested
    @DisplayName("PUT /api/v1/tasks/{id} — atualização e recálculo de prioridade")
    class AtualizacaoTarefa {

        @Test
        @DisplayName("Deve retornar 200 e recalcular criticidade para URGENTE ao aproximar o prazo")
        void atualizarTarefa_prazoAproximado_deveRecalcularParaUrgente() throws Exception {
            // Cria com prazo distante → BAIXA
            String prazoDistante = LocalDate.now().plusDays(30).format(ISO_DATE);
            MvcResult criacao = mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson("Tarefa a atualizar", prazoDistante, "BAIXA")))
                    .andExpect(jsonPath("$.criticidade").value("BAIXA"))
                    .andReturn();

            String id = extrairId(criacao);

            // Atualiza com prazo hoje → deve recalcular para URGENTE
            String prazoHoje = LocalDate.now().format(ISO_DATE);
            mockMvc.perform(put(BASE_URL + "/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson("Tarefa a atualizar", prazoHoje, "BAIXA")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.criticidade").value("URGENTE"));
        }

        @Test
        @DisplayName("Deve retornar 200 e elevar criticidade ao mudar para ALTA com prazo em 30 dias")
        void atualizarTarefa_mudandoCriticidadeParaAlta_deveElevarPrioridade() throws Exception {
            String prazoDistante = LocalDate.now().plusDays(30).format(ISO_DATE);
            MvcResult criacao = mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson("Tarefa work", prazoDistante, "BAIXA")))
                    .andExpect(jsonPath("$.criticidade").value("BAIXA"))
                    .andReturn();

            String id = extrairId(criacao);

            // Atualiza criticidade para ALTA → bônus eleva BAIXA para MEDIA
            mockMvc.perform(put(BASE_URL + "/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson("Tarefa work", prazoDistante, "ALTA")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.criticidade").value("MEDIA"));
        }

        @Test
        @DisplayName("Deve retornar 200 e recalcular MEDIA para prazo em 5 dias")
        void atualizarTarefa_prazoEmCincoDias_deveRecalcularParaMedia() throws Exception {
            String prazoDistante = LocalDate.now().plusDays(30).format(ISO_DATE);
            MvcResult criacao = mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson("Tarefa média", prazoDistante, "BAIXA")))
                    .andReturn();

            String id = extrairId(criacao);

            String prazoMedio = LocalDate.now().plusDays(5).format(ISO_DATE);
            mockMvc.perform(put(BASE_URL + "/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson("Tarefa média", prazoMedio, "BAIXA")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.criticidade").value("MEDIA"));
        }

        @Test
        @DisplayName("Deve retornar 404 ao tentar atualizar tarefa inexistente")
        void atualizarTarefa_inexistente_deveRetornar404() throws Exception {
            String prazo = LocalDate.now().plusDays(5).format(ISO_DATE);

            mockMvc.perform(put(BASE_URL + "/{id}", "00000000-0000-0000-0000-000000000000")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson("Tarefa inexistente", prazo, "BAIXA")))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Deve retornar 400 ao tentar atualizar com dados inválidos")
        void atualizarTarefa_dadosInvalidos_deveRetornar400() throws Exception {
            String prazoDistante = LocalDate.now().plusDays(30).format(ISO_DATE);
            MvcResult criacao = mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson("Tarefa válida", prazoDistante, "BAIXA")))
                    .andReturn();

            String id = extrairId(criacao);

            // Título em branco → 400
            mockMvc.perform(put(BASE_URL + "/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson("", prazoDistante, "BAIXA")))
                    .andExpect(status().isBadRequest());
        }
    }

    // =========================================================================
    // DELETE /api/v1/tasks/{id}
    // =========================================================================

    @Nested
    @DisplayName("DELETE /api/v1/tasks/{id} — remoção de tarefa")
    class RemocaoTarefa {

        @Test
        @DisplayName("Deve retornar 204 ao remover tarefa existente")
        void removerTarefa_existente_deveRetornar204() throws Exception {
            String prazo = LocalDate.now().plusDays(10).format(ISO_DATE);
            MvcResult criacao = mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson("Tarefa a remover", prazo, "BAIXA")))
                    .andReturn();

            String id = extrairId(criacao);

            mockMvc.perform(delete(BASE_URL + "/{id}", id))
                    .andExpect(status().isNoContent());

            // Confirma que foi removida
            mockMvc.perform(get(BASE_URL + "/{id}", id))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Deve retornar 404 ao tentar remover tarefa inexistente")
        void removerTarefa_inexistente_deveRetornar404() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/{id}", "00000000-0000-0000-0000-000000000000"))
                    .andExpect(status().isNotFound());
        }
    }
}
