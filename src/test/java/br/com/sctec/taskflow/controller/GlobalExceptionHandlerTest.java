package br.com.sctec.taskflow.controller;

import br.com.sctec.taskflow.domain.enums.StatusTarefa;
import br.com.sctec.taskflow.domain.exception.TarefaEncerradaException;
import br.com.sctec.taskflow.domain.exception.TransicaoInvalidaException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitários para {@link GlobalExceptionHandler} — tarefa 11.7.
 *
 * <p>Estratégia: instancia o handler diretamente (sem Spring context) e
 * invoca cada método de tratamento, verificando o {@link ProblemDetail}
 * retornado — status HTTP, título e ausência de detalhes internos.</p>
 *
 * <p><strong>Validates: Requirements 7.2, 7.4, 7.5, 8.5</strong></p>
 */
@DisplayName("GlobalExceptionHandler — formato padronizado de respostas de erro")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    // =========================================================================
    // 404 — EntityNotFoundException
    // =========================================================================

    @Nested
    @DisplayName("404 Not Found — EntityNotFoundException")
    class NotFound {

        @Test
        @DisplayName("Deve retornar status 404")
        void deveRetornarStatus404() {
            EntityNotFoundException ex = new EntityNotFoundException("Tarefa não encontrada: abc");

            ProblemDetail result = handler.handleNotFound(ex);

            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        @DisplayName("Deve incluir a mensagem da exceção no detail")
        void deveIncluirMensagemNoDetail() {
            EntityNotFoundException ex = new EntityNotFoundException("Tarefa não encontrada: abc");

            ProblemDetail result = handler.handleNotFound(ex);

            assertThat(result.getDetail()).isEqualTo("Tarefa não encontrada: abc");
        }

        @Test
        @DisplayName("Deve ter título padronizado")
        void deveTerTituloPadronizado() {
            ProblemDetail result = handler.handleNotFound(new EntityNotFoundException("msg"));

            assertThat(result.getTitle()).isEqualTo("Recurso não encontrado");
        }
    }

    // =========================================================================
    // 409 — IllegalStateException
    // =========================================================================

    @Nested
    @DisplayName("409 Conflict — IllegalStateException")
    class Conflict {

        @Test
        @DisplayName("Deve retornar status 409")
        void deveRetornarStatus409() {
            ProblemDetail result = handler.handleConflict(new IllegalStateException("tarefa encerrada"));

            assertThat(result.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        }

        @Test
        @DisplayName("Deve incluir a mensagem da exceção no detail")
        void deveIncluirMensagemNoDetail() {
            ProblemDetail result = handler.handleConflict(new IllegalStateException("tarefa encerrada"));

            assertThat(result.getDetail()).isEqualTo("tarefa encerrada");
        }

        @Test
        @DisplayName("Deve ter título padronizado")
        void deveTerTituloPadronizado() {
            ProblemDetail result = handler.handleConflict(new IllegalStateException("msg"));

            assertThat(result.getTitle()).isEqualTo("Operação inválida");
        }
    }

    // =========================================================================
    // 422 — TarefaEncerradaException
    // =========================================================================

    @Nested
    @DisplayName("422 Unprocessable Entity — TarefaEncerradaException")
    class TarefaEncerrada {

        @Test
        @DisplayName("Deve retornar status 422")
        void deveRetornarStatus422() {
            TarefaEncerradaException ex = new TarefaEncerradaException(StatusTarefa.CONCLUIDA);

            ProblemDetail result = handler.handleTarefaEncerrada(ex);

            assertThat(result.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
        }

        @Test
        @DisplayName("Deve ter título padronizado")
        void deveTerTituloPadronizado() {
            ProblemDetail result = handler.handleTarefaEncerrada(
                    new TarefaEncerradaException(StatusTarefa.CANCELADA));

            assertThat(result.getTitle()).isEqualTo("Tarefa encerrada");
        }

        @Test
        @DisplayName("Deve incluir statusAtual como propriedade extra")
        void deveIncluirStatusAtualComoPropriedade() {
            TarefaEncerradaException ex = new TarefaEncerradaException(StatusTarefa.CONCLUIDA);

            ProblemDetail result = handler.handleTarefaEncerrada(ex);

            assertThat(result.getProperties()).containsKey("statusAtual");
            assertThat(result.getProperties().get("statusAtual")).isEqualTo(StatusTarefa.CONCLUIDA);
        }

        @Test
        @DisplayName("Deve incluir a mensagem da exceção no detail")
        void deveIncluirMensagemNoDetail() {
            TarefaEncerradaException ex = new TarefaEncerradaException(StatusTarefa.CONCLUIDA);

            ProblemDetail result = handler.handleTarefaEncerrada(ex);

            assertThat(result.getDetail()).contains("CONCLUIDA");
        }
    }

    // =========================================================================
    // 422 — TransicaoInvalidaException
    // =========================================================================

    @Nested
    @DisplayName("422 Unprocessable Entity — TransicaoInvalidaException")
    class TransicaoInvalida {

        @Test
        @DisplayName("Deve retornar status 422")
        void deveRetornarStatus422() {
            TransicaoInvalidaException ex = new TransicaoInvalidaException(
                    StatusTarefa.EM_ANDAMENTO, StatusTarefa.PENDENTE);

            ProblemDetail result = handler.handleTransicaoInvalida(ex);

            assertThat(result.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
        }

        @Test
        @DisplayName("Deve ter título padronizado")
        void deveTerTituloPadronizado() {
            ProblemDetail result = handler.handleTransicaoInvalida(
                    new TransicaoInvalidaException(StatusTarefa.EM_ANDAMENTO, StatusTarefa.PENDENTE));

            assertThat(result.getTitle()).isEqualTo("Transição inválida");
        }

        @Test
        @DisplayName("Deve incluir statusAtual como propriedade extra")
        void deveIncluirStatusAtualComoPropriedade() {
            TransicaoInvalidaException ex = new TransicaoInvalidaException(
                    StatusTarefa.EM_ANDAMENTO, StatusTarefa.PENDENTE);

            ProblemDetail result = handler.handleTransicaoInvalida(ex);

            assertThat(result.getProperties()).containsKey("statusAtual");
            assertThat(result.getProperties().get("statusAtual")).isEqualTo(StatusTarefa.EM_ANDAMENTO);
        }

        @Test
        @DisplayName("Deve incluir statusDestino como propriedade extra")
        void deveIncluirStatusDestinoComoPropriedade() {
            TransicaoInvalidaException ex = new TransicaoInvalidaException(
                    StatusTarefa.EM_ANDAMENTO, StatusTarefa.PENDENTE);

            ProblemDetail result = handler.handleTransicaoInvalida(ex);

            assertThat(result.getProperties()).containsKey("statusDestino");
            assertThat(result.getProperties().get("statusDestino")).isEqualTo(StatusTarefa.PENDENTE);
        }

        @Test
        @DisplayName("Deve incluir a mensagem da exceção no detail")
        void deveIncluirMensagemNoDetail() {
            TransicaoInvalidaException ex = new TransicaoInvalidaException(
                    StatusTarefa.EM_ANDAMENTO, StatusTarefa.PENDENTE);

            ProblemDetail result = handler.handleTransicaoInvalida(ex);

            assertThat(result.getDetail()).contains("EM_ANDAMENTO").contains("PENDENTE");
        }
    }

    // =========================================================================
    // 400 — MethodArgumentNotValidException
    // =========================================================================

    @Nested
    @DisplayName("400 Bad Request — MethodArgumentNotValidException")
    class ValidationError {

        @Test
        @DisplayName("Deve retornar status 400")
        void deveRetornarStatus400() throws Exception {
            MethodArgumentNotValidException ex = buildValidationException("titulo", "não pode ser vazio");

            ProblemDetail result = handler.handleValidation(ex);

            assertThat(result.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        }

        @Test
        @DisplayName("Deve ter título padronizado")
        void deveTerTituloPadronizado() throws Exception {
            MethodArgumentNotValidException ex = buildValidationException("titulo", "não pode ser vazio");

            ProblemDetail result = handler.handleValidation(ex);

            assertThat(result.getTitle()).isEqualTo("Dados inválidos");
        }

        @Test
        @DisplayName("Deve incluir nome do campo e mensagem no detail")
        void deveIncluirCampoEMensagemNoDetail() throws Exception {
            MethodArgumentNotValidException ex = buildValidationException("titulo", "não pode ser vazio");

            ProblemDetail result = handler.handleValidation(ex);

            assertThat(result.getDetail()).contains("titulo").contains("não pode ser vazio");
        }

        /** Constrói um MethodArgumentNotValidException com um FieldError sintético. */
        private MethodArgumentNotValidException buildValidationException(String field, String message)
                throws Exception {
            BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "target");
            bindingResult.addError(new FieldError("target", field, message));
            return new MethodArgumentNotValidException(null, bindingResult);
        }
    }

    // =========================================================================
    // 503 — DataAccessException (tarefa 11.5)
    // =========================================================================

    @Nested
    @DisplayName("503 Service Unavailable — DataAccessException")
    class DataAccess {

        @Test
        @DisplayName("Deve retornar status 503")
        void deveRetornarStatus503() {
            DataAccessException ex = new DataRetrievalFailureException("connection refused");

            ProblemDetail result = handler.handleDataAccess(ex);

            assertThat(result.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
        }

        @Test
        @DisplayName("Deve ter título padronizado")
        void deveTerTituloPadronizado() {
            ProblemDetail result = handler.handleDataAccess(
                    new DataRetrievalFailureException("erro interno"));

            assertThat(result.getTitle()).isEqualTo("Serviço temporariamente indisponível");
        }

        @Test
        @DisplayName("Nao deve expor detalhes internos da excecao no detail")
        void naoDeveExporDetalhesInternos() {
            DataAccessException ex = new DataRetrievalFailureException("senha=secret host=db-prod");

            ProblemDetail result = handler.handleDataAccess(ex);

            // A mensagem interna da exceção NÃO deve aparecer no detail
            assertThat(result.getDetail()).doesNotContain("senha=secret").doesNotContain("db-prod");
        }

        @Test
        @DisplayName("Deve retornar mensagem generica ao cliente")
        void deveRetornarMensagemGenerica() {
            ProblemDetail result = handler.handleDataAccess(
                    new DataRetrievalFailureException("qualquer erro"));

            assertThat(result.getDetail()).isNotBlank();
        }
    }

    // =========================================================================
    // 500 — Exception fallback (tarefa 11.6)
    // =========================================================================

    @Nested
    @DisplayName("500 Internal Server Error — Exception fallback")
    class GenericError {

        @Test
        @DisplayName("Deve retornar status 500")
        void deveRetornarStatus500() {
            ProblemDetail result = handler.handleGeneric(new RuntimeException("erro inesperado"));

            assertThat(result.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }

        @Test
        @DisplayName("Deve ter título padronizado")
        void deveTerTituloPadronizado() {
            ProblemDetail result = handler.handleGeneric(new Exception("qualquer erro"));

            assertThat(result.getTitle()).isEqualTo("Erro interno do servidor");
        }

        @Test
        @DisplayName("Nao deve expor stack trace ou mensagem interna no detail")
        void naoDeveExporStackTraceOuMensagemInterna() {
            Exception ex = new RuntimeException("NullPointerException at line 42 in SecretClass");

            ProblemDetail result = handler.handleGeneric(ex);

            assertThat(result.getDetail())
                    .doesNotContain("NullPointerException")
                    .doesNotContain("SecretClass")
                    .doesNotContain("line 42");
        }

        @Test
        @DisplayName("Deve retornar mensagem generica ao cliente")
        void deveRetornarMensagemGenerica() {
            ProblemDetail result = handler.handleGeneric(new Exception("qualquer erro"));

            assertThat(result.getDetail()).isNotBlank();
        }

        @Test
        @DisplayName("Deve tratar qualquer subclasse de Exception")
        void deveTratarQualquerSubclasseDeException() {
            ProblemDetail result = handler.handleGeneric(new IllegalArgumentException("arg invalido"));

            assertThat(result.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }
}
