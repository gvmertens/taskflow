package br.com.sctec.taskflow.service;

import br.com.sctec.taskflow.domain.entity.Task;
import br.com.sctec.taskflow.domain.enums.Criticidade;
import br.com.sctec.taskflow.domain.enums.StatusTarefa;
import br.com.sctec.taskflow.domain.service.CriticidadeCalculator;
import br.com.sctec.taskflow.dto.TaskRequest;
import br.com.sctec.taskflow.dto.TaskResponse;
import br.com.sctec.taskflow.repository.TaskRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para {@link TaskService}.
 *
 * <p>Estratégia: mocks de {@link TaskRepository} e {@link CriticidadeCalculator}
 * via Mockito — sem Spring context, sem banco de dados.</p>
 *
 * <p><strong>Validates: Requirements 10.2, 10.3</strong></p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TaskService — casos de uso da aplicação")
class TaskServiceTest {

    @Mock
    private TaskRepository repository;

    @Mock
    private CriticidadeCalculator criticidadeCalculator;

    @InjectMocks
    private TaskService service;

    // =========================================================================
    // Helpers
    // =========================================================================

    private static final LocalDate PRAZO_FUTURO = LocalDate.now().plusDays(10);

    /** Cria um TaskRequest válido com os valores fornecidos. */
    private TaskRequest request(String titulo, LocalDate prazo, Criticidade criticidade) {
        return new TaskRequest(titulo, "Descrição de teste", prazo, criticidade);
    }

    /** Cria uma Task salva (com ID) simulando o retorno do repositório. */
    private Task taskSalva(UUID id, String titulo, LocalDate prazo,
                           Criticidade criticidade, StatusTarefa status) {
        return Task.builder()
                .id(id)
                .titulo(titulo)
                .descricao("Descrição de teste")
                .prazo(prazo)
                .criticidade(criticidade)
                .status(status)
                .scorePrioridade(0)
                .build();
    }

    // =========================================================================
    // create
    // =========================================================================

    @Nested
    @DisplayName("create — criação de nova tarefa")
    class Create {

        @Test
        @DisplayName("Deve criar tarefa com status PENDENTE independente do request")
        void deveCriarTarefaComStatusPendente() {
            TaskRequest req = request("Minha tarefa", PRAZO_FUTURO, Criticidade.MEDIA);
            UUID id = UUID.randomUUID();

            when(criticidadeCalculator.calcular(any(Task.class), any(LocalDate.class)))
                    .thenReturn(Criticidade.MEDIA);
            when(repository.save(any(Task.class)))
                    .thenAnswer(inv -> {
                        Task t = inv.getArgument(0);
                        return taskSalva(id, t.getTitulo(), t.getPrazo(), t.getCriticidade(), t.getStatus());
                    });

            TaskResponse response = service.create(req);

            assertThat(response.status()).isEqualTo(StatusTarefa.PENDENTE);
        }

        @Test
        @DisplayName("Deve calcular criticidade via CriticidadeCalculator antes de salvar")
        void deveCalcularCriticidadeEfetiva() {
            TaskRequest req = request("Tarefa urgente", PRAZO_FUTURO, Criticidade.ALTA);
            UUID id = UUID.randomUUID();

            when(criticidadeCalculator.calcular(any(Task.class), any(LocalDate.class)))
                    .thenReturn(Criticidade.URGENTE);
            when(repository.save(any(Task.class)))
                    .thenAnswer(inv -> {
                        Task t = inv.getArgument(0);
                        return taskSalva(id, t.getTitulo(), t.getPrazo(), t.getCriticidade(), t.getStatus());
                    });

            TaskResponse response = service.create(req);

            assertThat(response.criticidade()).isEqualTo(Criticidade.URGENTE);
            verify(criticidadeCalculator).calcular(any(Task.class), any(LocalDate.class));
        }

        @Test
        @DisplayName("Deve persistir a criticidade calculada (não a do request) na entidade salva")
        void devePersistirCriticidadeCalculada() {
            // Request com BAIXA, mas calculator retorna URGENTE (prazo vencido)
            TaskRequest req = request("Tarefa", PRAZO_FUTURO, Criticidade.BAIXA);
            UUID id = UUID.randomUUID();

            when(criticidadeCalculator.calcular(any(Task.class), any(LocalDate.class)))
                    .thenReturn(Criticidade.URGENTE);

            ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
            when(repository.save(captor.capture()))
                    .thenAnswer(inv -> {
                        Task t = inv.getArgument(0);
                        return taskSalva(id, t.getTitulo(), t.getPrazo(), t.getCriticidade(), t.getStatus());
                    });

            service.create(req);

            // A entidade enviada ao repositório deve ter a criticidade calculada
            assertThat(captor.getValue().getCriticidade()).isEqualTo(Criticidade.URGENTE);
        }

        @Test
        @DisplayName("Deve mapear título, descrição e prazo do request para a entidade")
        void deveMapearCamposDoRequest() {
            LocalDate prazo = LocalDate.now().plusDays(5);
            TaskRequest req = new TaskRequest("Título específico", "Descrição específica", prazo, Criticidade.MEDIA);
            UUID id = UUID.randomUUID();

            when(criticidadeCalculator.calcular(any(Task.class), any(LocalDate.class)))
                    .thenReturn(Criticidade.MEDIA);

            ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
            when(repository.save(captor.capture()))
                    .thenAnswer(inv -> {
                        Task t = inv.getArgument(0);
                        return taskSalva(id, t.getTitulo(), t.getPrazo(), t.getCriticidade(), t.getStatus());
                    });

            service.create(req);

            Task entidade = captor.getValue();
            assertThat(entidade.getTitulo()).isEqualTo("Título específico");
            assertThat(entidade.getDescricao()).isEqualTo("Descrição específica");
            assertThat(entidade.getPrazo()).isEqualTo(prazo);
        }

        @Test
        @DisplayName("Deve chamar repository.save exatamente uma vez")
        void deveChamarSaveUmaVez() {
            TaskRequest req = request("Tarefa", PRAZO_FUTURO, Criticidade.MEDIA);
            UUID id = UUID.randomUUID();

            when(criticidadeCalculator.calcular(any(Task.class), any(LocalDate.class)))
                    .thenReturn(Criticidade.MEDIA);
            when(repository.save(any(Task.class)))
                    .thenReturn(taskSalva(id, "Tarefa", PRAZO_FUTURO, Criticidade.MEDIA, StatusTarefa.PENDENTE));

            service.create(req);

            verify(repository, times(1)).save(any(Task.class));
        }
    }

    // =========================================================================
    // findById
    // =========================================================================

    @Nested
    @DisplayName("findById — busca por identificador")
    class FindById {

        @Test
        @DisplayName("Deve retornar TaskResponse quando tarefa existe")
        void deveRetornarTaskResponseQuandoExiste() {
            UUID id = UUID.randomUUID();
            Task task = taskSalva(id, "Tarefa encontrada", PRAZO_FUTURO, Criticidade.MEDIA, StatusTarefa.PENDENTE);

            when(repository.findById(id)).thenReturn(Optional.of(task));

            TaskResponse response = service.findById(id);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(id);
            assertThat(response.titulo()).isEqualTo("Tarefa encontrada");
            assertThat(response.status()).isEqualTo(StatusTarefa.PENDENTE);
            assertThat(response.criticidade()).isEqualTo(Criticidade.MEDIA);
        }

        @Test
        @DisplayName("Deve lançar EntityNotFoundException quando ID não existe")
        void deveLancarEntityNotFoundExceptionQuandoIdNaoExiste() {
            UUID idInexistente = UUID.randomUUID();

            when(repository.findById(idInexistente)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findById(idInexistente))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining(idInexistente.toString());
        }

        @Test
        @DisplayName("Deve chamar repository.findById com o ID correto")
        void deveChamarFindByIdComIdCorreto() {
            UUID id = UUID.randomUUID();
            Task task = taskSalva(id, "Tarefa", PRAZO_FUTURO, Criticidade.BAIXA, StatusTarefa.PENDENTE);

            when(repository.findById(id)).thenReturn(Optional.of(task));

            service.findById(id);

            verify(repository).findById(id);
        }
    }

    // =========================================================================
    // findAll
    // =========================================================================

    @Nested
    @DisplayName("findAll — listagem com filtros opcionais")
    class FindAll {

        private final Pageable pageable = PageRequest.of(0, 20);

        @Test
        @DisplayName("Sem filtros: deve delegar para repository.findAll(Pageable)")
        void semFiltros_deveDelegarParaFindAll() {
            Page<Task> paginaVazia = new PageImpl<>(List.of());
            when(repository.findAll(pageable)).thenReturn(paginaVazia);

            Page<TaskResponse> resultado = service.findAll(null, null, pageable);

            assertThat(resultado).isNotNull();
            verify(repository).findAll(pageable);
            verify(repository, never()).findByStatus(any(), any());
            verify(repository, never()).findByCriticidade(any(), any());
            verify(repository, never()).findByStatusAndCriticidade(any(), any(), any());
        }

        @Test
        @DisplayName("Apenas status: deve delegar para repository.findByStatus")
        void apenasStatus_deveDelegarParaFindByStatus() {
            Page<Task> pagina = new PageImpl<>(List.of());
            when(repository.findByStatus(StatusTarefa.PENDENTE, pageable)).thenReturn(pagina);

            Page<TaskResponse> resultado = service.findAll(StatusTarefa.PENDENTE, null, pageable);

            assertThat(resultado).isNotNull();
            verify(repository).findByStatus(StatusTarefa.PENDENTE, pageable);
            verify(repository, never()).findAll(pageable);
            verify(repository, never()).findByCriticidade(any(), any());
            verify(repository, never()).findByStatusAndCriticidade(any(), any(), any());
        }

        @Test
        @DisplayName("Apenas criticidade: deve delegar para repository.findByCriticidade")
        void apenasCriticidade_deveDelegarParaFindByCriticidade() {
            Page<Task> pagina = new PageImpl<>(List.of());
            when(repository.findByCriticidade(Criticidade.ALTA, pageable)).thenReturn(pagina);

            Page<TaskResponse> resultado = service.findAll(null, Criticidade.ALTA, pageable);

            assertThat(resultado).isNotNull();
            verify(repository).findByCriticidade(Criticidade.ALTA, pageable);
            verify(repository, never()).findAll(pageable);
            verify(repository, never()).findByStatus(any(), any());
            verify(repository, never()).findByStatusAndCriticidade(any(), any(), any());
        }

        @Test
        @DisplayName("Status e criticidade: deve delegar para repository.findByStatusAndCriticidade")
        void statusECriticidade_deveDelegarParaFindByStatusAndCriticidade() {
            Page<Task> pagina = new PageImpl<>(List.of());
            when(repository.findByStatusAndCriticidade(StatusTarefa.EM_ANDAMENTO, Criticidade.URGENTE, pageable))
                    .thenReturn(pagina);

            Page<TaskResponse> resultado = service.findAll(StatusTarefa.EM_ANDAMENTO, Criticidade.URGENTE, pageable);

            assertThat(resultado).isNotNull();
            verify(repository).findByStatusAndCriticidade(StatusTarefa.EM_ANDAMENTO, Criticidade.URGENTE, pageable);
            verify(repository, never()).findAll(pageable);
            verify(repository, never()).findByStatus(any(), any());
            verify(repository, never()).findByCriticidade(any(), any());
        }

        @Test
        @DisplayName("Deve mapear cada Task da página para TaskResponse")
        void deveMappearTasksParaTaskResponse() {
            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();
            List<Task> tasks = List.of(
                    taskSalva(id1, "Tarefa 1", PRAZO_FUTURO, Criticidade.ALTA, StatusTarefa.PENDENTE),
                    taskSalva(id2, "Tarefa 2", PRAZO_FUTURO, Criticidade.MEDIA, StatusTarefa.EM_ANDAMENTO)
            );
            when(repository.findAll(pageable)).thenReturn(new PageImpl<>(tasks));

            Page<TaskResponse> resultado = service.findAll(null, null, pageable);

            assertThat(resultado.getContent()).hasSize(2);
            assertThat(resultado.getContent().get(0).id()).isEqualTo(id1);
            assertThat(resultado.getContent().get(1).id()).isEqualTo(id2);
        }

        @ParameterizedTest(name = "Filtro por status {0}")
        @EnumSource(StatusTarefa.class)
        @DisplayName("Deve aceitar qualquer valor de StatusTarefa como filtro")
        void deveAceitarQualquerStatusComoFiltro(StatusTarefa status) {
            when(repository.findByStatus(eq(status), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            Page<TaskResponse> resultado = service.findAll(status, null, pageable);

            assertThat(resultado).isNotNull();
            verify(repository).findByStatus(eq(status), any(Pageable.class));
        }

        @ParameterizedTest(name = "Filtro por criticidade {0}")
        @EnumSource(Criticidade.class)
        @DisplayName("Deve aceitar qualquer valor de Criticidade como filtro")
        void deveAceitarQualquerCriticidadeComoFiltro(Criticidade criticidade) {
            when(repository.findByCriticidade(eq(criticidade), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            Page<TaskResponse> resultado = service.findAll(null, criticidade, pageable);

            assertThat(resultado).isNotNull();
            verify(repository).findByCriticidade(eq(criticidade), any(Pageable.class));
        }
    }

    // =========================================================================
    // update
    // =========================================================================

    @Nested
    @DisplayName("update — atualização de tarefa ativa")
    class Update {

        @Test
        @DisplayName("Deve atualizar campos e recalcular criticidade para tarefa PENDENTE")
        void deveAtualizarCamposERecalcularCriticidade() {
            UUID id = UUID.randomUUID();
            Task taskExistente = taskSalva(id, "Título antigo", PRAZO_FUTURO, Criticidade.BAIXA, StatusTarefa.PENDENTE);
            LocalDate novoPrazo = LocalDate.now().plusDays(3);
            TaskRequest req = new TaskRequest("Título novo", "Nova descrição", novoPrazo, Criticidade.ALTA);

            when(repository.findById(id)).thenReturn(Optional.of(taskExistente));
            when(criticidadeCalculator.calcular(any(Task.class), any(LocalDate.class)))
                    .thenReturn(Criticidade.URGENTE);
            when(repository.save(any(Task.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            TaskResponse response = service.update(id, req);

            assertThat(response.titulo()).isEqualTo("Título novo");
            assertThat(response.descricao()).isEqualTo("Nova descrição");
            assertThat(response.prazo()).isEqualTo(novoPrazo);
            assertThat(response.criticidade()).isEqualTo(Criticidade.URGENTE);
            verify(criticidadeCalculator).calcular(any(Task.class), any(LocalDate.class));
        }

        @Test
        @DisplayName("Deve atualizar tarefa com status EM_ANDAMENTO normalmente")
        void deveAtualizarTarefaEmAndamento() {
            UUID id = UUID.randomUUID();
            Task taskExistente = taskSalva(id, "Tarefa", PRAZO_FUTURO, Criticidade.MEDIA, StatusTarefa.EM_ANDAMENTO);
            TaskRequest req = request("Tarefa atualizada", PRAZO_FUTURO, Criticidade.MEDIA);

            when(repository.findById(id)).thenReturn(Optional.of(taskExistente));
            when(criticidadeCalculator.calcular(any(Task.class), any(LocalDate.class)))
                    .thenReturn(Criticidade.MEDIA);
            when(repository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

            TaskResponse response = service.update(id, req);

            assertThat(response).isNotNull();
            assertThat(response.titulo()).isEqualTo("Tarefa atualizada");
        }

        @Test
        @DisplayName("Deve lançar IllegalStateException ao tentar atualizar tarefa CONCLUIDA")
        void deveLancarExcecaoParaTarefaConcluida() {
            UUID id = UUID.randomUUID();
            Task taskEncerrada = taskSalva(id, "Tarefa", PRAZO_FUTURO, Criticidade.MEDIA, StatusTarefa.CONCLUIDA);
            TaskRequest req = request("Novo título", PRAZO_FUTURO, Criticidade.MEDIA);

            when(repository.findById(id)).thenReturn(Optional.of(taskEncerrada));

            assertThatThrownBy(() -> service.update(id, req))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("CONCLUIDA");
        }

        @Test
        @DisplayName("Deve lançar IllegalStateException ao tentar atualizar tarefa CANCELADA")
        void deveLancarExcecaoParaTarefaCancelada() {
            UUID id = UUID.randomUUID();
            Task taskEncerrada = taskSalva(id, "Tarefa", PRAZO_FUTURO, Criticidade.MEDIA, StatusTarefa.CANCELADA);
            TaskRequest req = request("Novo título", PRAZO_FUTURO, Criticidade.MEDIA);

            when(repository.findById(id)).thenReturn(Optional.of(taskEncerrada));

            assertThatThrownBy(() -> service.update(id, req))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("CANCELADA");
        }

        @Test
        @DisplayName("Deve lançar EntityNotFoundException quando ID não existe no update")
        void deveLancarEntityNotFoundExceptionQuandoIdNaoExiste() {
            UUID idInexistente = UUID.randomUUID();
            TaskRequest req = request("Título", PRAZO_FUTURO, Criticidade.MEDIA);

            when(repository.findById(idInexistente)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(idInexistente, req))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining(idInexistente.toString());
        }

        @Test
        @DisplayName("Não deve chamar repository.save quando tarefa está encerrada")
        void naoDeveSalvarQuandoTarefaEncerrada() {
            UUID id = UUID.randomUUID();
            Task taskEncerrada = taskSalva(id, "Tarefa", PRAZO_FUTURO, Criticidade.MEDIA, StatusTarefa.CONCLUIDA);
            TaskRequest req = request("Novo título", PRAZO_FUTURO, Criticidade.MEDIA);

            when(repository.findById(id)).thenReturn(Optional.of(taskEncerrada));

            assertThatThrownBy(() -> service.update(id, req))
                    .isInstanceOf(IllegalStateException.class);

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Deve chamar repository.save exatamente uma vez ao atualizar tarefa ativa")
        void deveChamarSaveUmaVezParaTarefaAtiva() {
            UUID id = UUID.randomUUID();
            Task taskExistente = taskSalva(id, "Tarefa", PRAZO_FUTURO, Criticidade.MEDIA, StatusTarefa.PENDENTE);
            TaskRequest req = request("Novo título", PRAZO_FUTURO, Criticidade.MEDIA);

            when(repository.findById(id)).thenReturn(Optional.of(taskExistente));
            when(criticidadeCalculator.calcular(any(Task.class), any(LocalDate.class)))
                    .thenReturn(Criticidade.MEDIA);
            when(repository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

            service.update(id, req);

            verify(repository, times(1)).save(any(Task.class));
        }
    }

    // =========================================================================
    // delete
    // =========================================================================

    @Nested
    @DisplayName("delete — remoção permanente de tarefa")
    class Delete {

        @Test
        @DisplayName("Deve remover tarefa existente chamando repository.delete")
        void deveRemoverTarefaExistente() {
            UUID id = UUID.randomUUID();
            Task task = taskSalva(id, "Tarefa a remover", PRAZO_FUTURO, Criticidade.BAIXA, StatusTarefa.PENDENTE);

            when(repository.findById(id)).thenReturn(Optional.of(task));

            service.delete(id);

            verify(repository).delete(task);
        }

        @Test
        @DisplayName("Deve lançar EntityNotFoundException ao tentar deletar ID inexistente")
        void deveLancarEntityNotFoundExceptionQuandoIdNaoExiste() {
            UUID idInexistente = UUID.randomUUID();

            when(repository.findById(idInexistente)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.delete(idInexistente))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining(idInexistente.toString());
        }

        @Test
        @DisplayName("Não deve chamar repository.delete quando ID não existe")
        void naoDeveChamarDeleteQuandoIdNaoExiste() {
            UUID idInexistente = UUID.randomUUID();

            when(repository.findById(idInexistente)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.delete(idInexistente))
                    .isInstanceOf(EntityNotFoundException.class);

            verify(repository, never()).delete(any(Task.class));
        }

        @Test
        @DisplayName("Deve chamar repository.delete com a entidade correta")
        void deveChamarDeleteComEntidadeCorreta() {
            UUID id = UUID.randomUUID();
            Task task = taskSalva(id, "Tarefa", PRAZO_FUTURO, Criticidade.MEDIA, StatusTarefa.EM_ANDAMENTO);

            when(repository.findById(id)).thenReturn(Optional.of(task));

            service.delete(id);

            ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
            verify(repository).delete(captor.capture());
            assertThat(captor.getValue().getId()).isEqualTo(id);
        }

        @Test
        @DisplayName("Deve permitir deletar tarefa em qualquer status ativo")
        void devePermitirDeletarTarefaEmQualquerStatus() {
            for (StatusTarefa status : StatusTarefa.values()) {
                UUID id = UUID.randomUUID();
                Task task = taskSalva(id, "Tarefa", PRAZO_FUTURO, Criticidade.BAIXA, status);

                when(repository.findById(id)).thenReturn(Optional.of(task));

                service.delete(id);

                verify(repository).delete(task);
            }
        }
    }
}
