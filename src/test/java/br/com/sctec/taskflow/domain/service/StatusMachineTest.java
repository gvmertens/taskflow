package br.com.sctec.taskflow.domain.service;

import br.com.sctec.taskflow.domain.enums.StatusTarefa;
import br.com.sctec.taskflow.domain.exception.TarefaEncerradaException;
import br.com.sctec.taskflow.domain.exception.TransicaoInvalidaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes unitários para {@link StatusMachine} — tarefa 4.2.
 *
 * <p>Estratégia: POJO puro — sem Spring, sem mocks. Cada teste invoca
 * {@code validarTransicao} e verifica se a transição é aceita silenciosamente
 * ou se a exceção correta é lançada com o tipo e mensagem esperados.</p>
 *
 * <p>Cobertura:
 * <ul>
 *   <li>5 transições válidas</li>
 *   <li>Rejeição de EM_ANDAMENTO → PENDENTE</li>
 *   <li>Rejeição de qualquer transição a partir de CONCLUIDA</li>
 *   <li>Rejeição de qualquer transição a partir de CANCELADA</li>
 * </ul>
 */
@DisplayName("StatusMachine — validação de transições de status")
class StatusMachineTest {

    private StatusMachine statusMachine;

    @BeforeEach
    void setUp() {
        statusMachine = new StatusMachine();
    }

    // =========================================================================
    // Transições válidas — não devem lançar exceção
    // =========================================================================

    @Nested
    @DisplayName("Transições válidas")
    class TransicoesValidas {

        @Test
        @DisplayName("PENDENTE → EM_ANDAMENTO deve ser aceita")
        void pendente_paraEmAndamento_deveSerAceita() {
            assertThatCode(() ->
                    statusMachine.validarTransicao(StatusTarefa.PENDENTE, StatusTarefa.EM_ANDAMENTO))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("PENDENTE → CONCLUIDA deve ser aceita")
        void pendente_paraConcluida_deveSerAceita() {
            assertThatCode(() ->
                    statusMachine.validarTransicao(StatusTarefa.PENDENTE, StatusTarefa.CONCLUIDA))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("PENDENTE → CANCELADA deve ser aceita")
        void pendente_paraCancelada_deveSerAceita() {
            assertThatCode(() ->
                    statusMachine.validarTransicao(StatusTarefa.PENDENTE, StatusTarefa.CANCELADA))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("EM_ANDAMENTO → CONCLUIDA deve ser aceita")
        void emAndamento_paraConcluida_deveSerAceita() {
            assertThatCode(() ->
                    statusMachine.validarTransicao(StatusTarefa.EM_ANDAMENTO, StatusTarefa.CONCLUIDA))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("EM_ANDAMENTO → CANCELADA deve ser aceita")
        void emAndamento_paraCancelada_deveSerAceita() {
            assertThatCode(() ->
                    statusMachine.validarTransicao(StatusTarefa.EM_ANDAMENTO, StatusTarefa.CANCELADA))
                    .doesNotThrowAnyException();
        }
    }

    // =========================================================================
    // Transição inválida — EM_ANDAMENTO → PENDENTE
    // =========================================================================

    @Nested
    @DisplayName("Transição inválida: EM_ANDAMENTO → PENDENTE")
    class TransicaoInvalidaEmAndamentoParaPendente {

        @Test
        @DisplayName("EM_ANDAMENTO → PENDENTE deve lançar TransicaoInvalidaException")
        void emAndamento_paraPendente_deveLancarTransicaoInvalidaException() {
            assertThatThrownBy(() ->
                    statusMachine.validarTransicao(StatusTarefa.EM_ANDAMENTO, StatusTarefa.PENDENTE))
                    .isInstanceOf(TransicaoInvalidaException.class);
        }

        @Test
        @DisplayName("TransicaoInvalidaException deve conter os status de origem e destino")
        void emAndamento_paraPendente_excecaoDeveConterStatusCorretos() {
            assertThatThrownBy(() ->
                    statusMachine.validarTransicao(StatusTarefa.EM_ANDAMENTO, StatusTarefa.PENDENTE))
                    .isInstanceOf(TransicaoInvalidaException.class)
                    .satisfies(ex -> {
                        TransicaoInvalidaException tie = (TransicaoInvalidaException) ex;
                        org.assertj.core.api.Assertions.assertThat(tie.getStatusAtual())
                                .isEqualTo(StatusTarefa.EM_ANDAMENTO);
                        org.assertj.core.api.Assertions.assertThat(tie.getStatusDestino())
                                .isEqualTo(StatusTarefa.PENDENTE);
                    });
        }
    }

    // =========================================================================
    // Estados terminais — CONCLUIDA não permite nenhuma transição
    // =========================================================================

    @Nested
    @DisplayName("Estado terminal: CONCLUIDA rejeita qualquer transição")
    class EstadoTerminalConcluida {

        @ParameterizedTest(name = "CONCLUIDA → {0} deve lançar TarefaEncerradaException")
        @EnumSource(StatusTarefa.class)
        @DisplayName("CONCLUIDA → qualquer status deve lançar TarefaEncerradaException")
        void concluida_paraQualquerStatus_deveLancarTarefaEncerradaException(StatusTarefa destino) {
            assertThatThrownBy(() ->
                    statusMachine.validarTransicao(StatusTarefa.CONCLUIDA, destino))
                    .isInstanceOf(TarefaEncerradaException.class);
        }

        @Test
        @DisplayName("TarefaEncerradaException deve informar o status CONCLUIDA")
        void concluida_excecaoDeveInformarStatusConcluida() {
            assertThatThrownBy(() ->
                    statusMachine.validarTransicao(StatusTarefa.CONCLUIDA, StatusTarefa.PENDENTE))
                    .isInstanceOf(TarefaEncerradaException.class)
                    .satisfies(ex -> {
                        TarefaEncerradaException tee = (TarefaEncerradaException) ex;
                        org.assertj.core.api.Assertions.assertThat(tee.getStatusAtual())
                                .isEqualTo(StatusTarefa.CONCLUIDA);
                    });
        }
    }

    // =========================================================================
    // Estados terminais — CANCELADA não permite nenhuma transição
    // =========================================================================

    @Nested
    @DisplayName("Estado terminal: CANCELADA rejeita qualquer transição")
    class EstadoTerminalCancelada {

        @ParameterizedTest(name = "CANCELADA → {0} deve lançar TarefaEncerradaException")
        @EnumSource(StatusTarefa.class)
        @DisplayName("CANCELADA → qualquer status deve lançar TarefaEncerradaException")
        void cancelada_paraQualquerStatus_deveLancarTarefaEncerradaException(StatusTarefa destino) {
            assertThatThrownBy(() ->
                    statusMachine.validarTransicao(StatusTarefa.CANCELADA, destino))
                    .isInstanceOf(TarefaEncerradaException.class);
        }

        @Test
        @DisplayName("TarefaEncerradaException deve informar o status CANCELADA")
        void cancelada_excecaoDeveInformarStatusCancelada() {
            assertThatThrownBy(() ->
                    statusMachine.validarTransicao(StatusTarefa.CANCELADA, StatusTarefa.EM_ANDAMENTO))
                    .isInstanceOf(TarefaEncerradaException.class)
                    .satisfies(ex -> {
                        TarefaEncerradaException tee = (TarefaEncerradaException) ex;
                        org.assertj.core.api.Assertions.assertThat(tee.getStatusAtual())
                                .isEqualTo(StatusTarefa.CANCELADA);
                    });
        }
    }

    // =========================================================================
    // Mensagens de exceção
    // =========================================================================

    @Nested
    @DisplayName("Mensagens das exceções")
    class MensagensDeExcecao {

        @Test
        @DisplayName("TransicaoInvalidaException deve conter os nomes dos status na mensagem")
        void transicaoInvalida_mensagemDeveConterStatusDeOrigemEDestino() {
            assertThatThrownBy(() ->
                    statusMachine.validarTransicao(StatusTarefa.EM_ANDAMENTO, StatusTarefa.PENDENTE))
                    .isInstanceOf(TransicaoInvalidaException.class)
                    .hasMessageContaining("EM_ANDAMENTO")
                    .hasMessageContaining("PENDENTE");
        }

        @Test
        @DisplayName("TarefaEncerradaException deve conter o status terminal na mensagem")
        void tarefaEncerrada_mensagemDeveConterStatusTerminal() {
            assertThatThrownBy(() ->
                    statusMachine.validarTransicao(StatusTarefa.CONCLUIDA, StatusTarefa.PENDENTE))
                    .isInstanceOf(TarefaEncerradaException.class)
                    .hasMessageContaining("CONCLUIDA");
        }
    }
}
