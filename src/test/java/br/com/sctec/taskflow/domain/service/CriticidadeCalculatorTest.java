package br.com.sctec.taskflow.domain.service;

import br.com.sctec.taskflow.domain.entity.Task;
import br.com.sctec.taskflow.domain.enums.Criticidade;
import br.com.sctec.taskflow.domain.enums.StatusTarefa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitários para {@link CriticidadeCalculator}.
 *
 * <p>Estratégia: POJO puro — sem Spring, sem mocks. Cada teste constrói
 * uma {@link Task} com prazo e criticidade específicos e verifica o resultado
 * do cálculo em relação a uma data de referência fixa.</p>
 */
@DisplayName("CriticidadeCalculator — cálculo de prioridade")
class CriticidadeCalculatorTest {

    private CriticidadeCalculator calculator;

    /** Data de referência fixa para todos os testes — elimina flakiness por data real. */
    private static final LocalDate HOJE = LocalDate.of(2026, 5, 25);

    @BeforeEach
    void setUp() {
        calculator = new CriticidadeCalculator();
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /** Cria uma tarefa ativa (PENDENTE) com prazo e criticidade informados. */
    private Task tarefaAtiva(LocalDate prazo, Criticidade criticidade) {
        return Task.builder()
                .titulo("Tarefa de teste")
                .prazo(prazo)
                .criticidade(criticidade)
                .status(StatusTarefa.PENDENTE)
                .build();
    }

    /** Cria uma tarefa com status específico. */
    private Task tarefaComStatus(LocalDate prazo, Criticidade criticidade, StatusTarefa status) {
        return Task.builder()
                .titulo("Tarefa de teste")
                .prazo(prazo)
                .criticidade(criticidade)
                .status(status)
                .build();
    }

    // =========================================================================
    // Cálculo base por prazo (sem bônus — criticidade BAIXA não eleva)
    // =========================================================================

    @Nested
    @DisplayName("Cálculo base por prazo")
    class CalculoBasePorPrazo {

        @Test
        @DisplayName("Tarefa com prazo vencido ontem deve retornar URGENTE")
        void prazoVencidoOntem_deveRetornarUrgente() {
            Task task = tarefaAtiva(HOJE.minusDays(1), Criticidade.BAIXA);

            Criticidade resultado = calculator.calcular(task, HOJE);

            assertThat(resultado).isEqualTo(Criticidade.URGENTE);
        }

        @Test
        @DisplayName("Tarefa com prazo vencendo hoje deve retornar URGENTE")
        void prazoVencendoHoje_deveRetornarUrgente() {
            Task task = tarefaAtiva(HOJE, Criticidade.BAIXA);

            Criticidade resultado = calculator.calcular(task, HOJE);

            assertThat(resultado).isEqualTo(Criticidade.URGENTE);
        }

        @Test
        @DisplayName("Tarefa com prazo vencido há 30 dias deve retornar URGENTE")
        void prazoVencidoHaMuitoTempo_deveRetornarUrgente() {
            Task task = tarefaAtiva(HOJE.minusDays(30), Criticidade.BAIXA);

            Criticidade resultado = calculator.calcular(task, HOJE);

            assertThat(resultado).isEqualTo(Criticidade.URGENTE);
        }

        @Test
        @DisplayName("Tarefa com prazo em 1 dia deve retornar ALTA")
        void prazoEmUmDia_deveRetornarAlta() {
            Task task = tarefaAtiva(HOJE.plusDays(1), Criticidade.BAIXA);

            Criticidade resultado = calculator.calcular(task, HOJE);

            assertThat(resultado).isEqualTo(Criticidade.ALTA);
        }

        @Test
        @DisplayName("Tarefa com prazo em exatamente 2 dias deve retornar ALTA (limite do limiar)")
        void prazoEmDoisDias_deveRetornarAlta() {
            Task task = tarefaAtiva(HOJE.plusDays(2), Criticidade.BAIXA);

            Criticidade resultado = calculator.calcular(task, HOJE);

            assertThat(resultado).isEqualTo(Criticidade.ALTA);
        }

        @Test
        @DisplayName("Tarefa com prazo em 3 dias deve retornar MEDIA")
        void prazoEmTresDias_deveRetornarMedia() {
            Task task = tarefaAtiva(HOJE.plusDays(3), Criticidade.BAIXA);

            Criticidade resultado = calculator.calcular(task, HOJE);

            assertThat(resultado).isEqualTo(Criticidade.MEDIA);
        }

        @Test
        @DisplayName("Tarefa com prazo em exatamente 7 dias deve retornar MEDIA (limite do limiar)")
        void prazoEmSeteDias_deveRetornarMedia() {
            Task task = tarefaAtiva(HOJE.plusDays(7), Criticidade.BAIXA);

            Criticidade resultado = calculator.calcular(task, HOJE);

            assertThat(resultado).isEqualTo(Criticidade.MEDIA);
        }

        @Test
        @DisplayName("Tarefa com prazo em 8 dias deve retornar BAIXA")
        void prazoEmOitoDias_deveRetornarBaixa() {
            Task task = tarefaAtiva(HOJE.plusDays(8), Criticidade.BAIXA);

            Criticidade resultado = calculator.calcular(task, HOJE);

            assertThat(resultado).isEqualTo(Criticidade.BAIXA);
        }

        @Test
        @DisplayName("Tarefa com prazo em 30 dias deve retornar BAIXA")
        void prazoEmTrintaDias_deveRetornarBaixa() {
            Task task = tarefaAtiva(HOJE.plusDays(30), Criticidade.BAIXA);

            Criticidade resultado = calculator.calcular(task, HOJE);

            assertThat(resultado).isEqualTo(Criticidade.BAIXA);
        }
    }

    // =========================================================================
    // Bônus por criticidade ALTA (equivalente à categoria WORK)
    // =========================================================================

    @Nested
    @DisplayName("Elevação de prioridade por criticidade ALTA do usuário")
    class BonusCriticidadeAlta {

        @Test
        @DisplayName("Prazo em 30 dias + criticidade ALTA deve elevar BAIXA para MEDIA")
        void prazoDistante_criticidadeAlta_deveElevarBaixaParaMedia() {
            Task task = tarefaAtiva(HOJE.plusDays(30), Criticidade.ALTA);

            Criticidade resultado = calculator.calcular(task, HOJE);

            assertThat(resultado).isEqualTo(Criticidade.MEDIA);
        }

        @Test
        @DisplayName("Prazo em 5 dias + criticidade ALTA deve elevar MEDIA para ALTA")
        void prazoMedio_criticidadeAlta_deveElevarMediaParaAlta() {
            Task task = tarefaAtiva(HOJE.plusDays(5), Criticidade.ALTA);

            Criticidade resultado = calculator.calcular(task, HOJE);

            assertThat(resultado).isEqualTo(Criticidade.ALTA);
        }

        @Test
        @DisplayName("Prazo em 1 dia + criticidade ALTA deve elevar ALTA para URGENTE")
        void prazoProximo_criticidadeAlta_deveElevarAltaParaUrgente() {
            Task task = tarefaAtiva(HOJE.plusDays(1), Criticidade.ALTA);

            Criticidade resultado = calculator.calcular(task, HOJE);

            assertThat(resultado).isEqualTo(Criticidade.URGENTE);
        }

        @Test
        @DisplayName("Prazo vencido + criticidade ALTA deve manter URGENTE (teto máximo)")
        void prazoVencido_criticidadeAlta_deveManterUrgente() {
            Task task = tarefaAtiva(HOJE.minusDays(1), Criticidade.ALTA);

            Criticidade resultado = calculator.calcular(task, HOJE);

            // Base já é URGENTE; bônus não ultrapassa o teto
            assertThat(resultado).isEqualTo(Criticidade.URGENTE);
        }

        @Test
        @DisplayName("Criticidade BAIXA não deve elevar a prioridade base")
        void criticidadeBaixa_naoDeveElevar() {
            Task task = tarefaAtiva(HOJE.plusDays(5), Criticidade.BAIXA);

            Criticidade resultado = calculator.calcular(task, HOJE);

            // Sem bônus: prazo em 5 dias → MEDIA
            assertThat(resultado).isEqualTo(Criticidade.MEDIA);
        }

        @Test
        @DisplayName("Criticidade MEDIA não deve elevar a prioridade base")
        void criticidadeMedia_naoDeveElevar() {
            Task task = tarefaAtiva(HOJE.plusDays(5), Criticidade.MEDIA);

            Criticidade resultado = calculator.calcular(task, HOJE);

            // Sem bônus: prazo em 5 dias → MEDIA
            assertThat(resultado).isEqualTo(Criticidade.MEDIA);
        }

        @Test
        @DisplayName("Criticidade URGENTE informada pelo usuário não deve elevar a prioridade base")
        void criticidadeUrgente_naoDeveElevar() {
            Task task = tarefaAtiva(HOJE.plusDays(5), Criticidade.URGENTE);

            Criticidade resultado = calculator.calcular(task, HOJE);

            // Sem bônus: prazo em 5 dias → MEDIA (apenas ALTA ativa o bônus)
            assertThat(resultado).isEqualTo(Criticidade.MEDIA);
        }
    }

    // =========================================================================
    // Tarefas encerradas — nunca recalculadas
    // =========================================================================

    @Nested
    @DisplayName("Tarefas encerradas não devem ser recalculadas")
    class TarefasEncerradas {

        @Test
        @DisplayName("Tarefa CONCLUIDA deve retornar a criticidade atual sem recalcular")
        void tarefaConcluida_deveRetornarCriticidadeAtual() {
            // Prazo vencido → normalmente seria URGENTE, mas status CONCLUIDA congela o valor
            Task task = tarefaComStatus(HOJE.minusDays(10), Criticidade.BAIXA, StatusTarefa.CONCLUIDA);

            Criticidade resultado = calculator.calcular(task, HOJE);

            assertThat(resultado).isEqualTo(Criticidade.BAIXA);
        }

        @Test
        @DisplayName("Tarefa CANCELADA deve retornar a criticidade atual sem recalcular")
        void tarefaCancelada_deveRetornarCriticidadeAtual() {
            // Prazo vencido → normalmente seria URGENTE, mas status CANCELADA congela o valor
            Task task = tarefaComStatus(HOJE.minusDays(5), Criticidade.MEDIA, StatusTarefa.CANCELADA);

            Criticidade resultado = calculator.calcular(task, HOJE);

            assertThat(resultado).isEqualTo(Criticidade.MEDIA);
        }

        @Test
        @DisplayName("Tarefa PENDENTE com prazo vencido deve ser recalculada normalmente")
        void tarefaPendente_prazoVencido_deveRecalcular() {
            Task task = tarefaComStatus(HOJE.minusDays(5), Criticidade.BAIXA, StatusTarefa.PENDENTE);

            Criticidade resultado = calculator.calcular(task, HOJE);

            assertThat(resultado).isEqualTo(Criticidade.URGENTE);
        }

        @Test
        @DisplayName("Tarefa EM_ANDAMENTO com prazo vencido deve ser recalculada normalmente")
        void tarefaEmAndamento_prazoVencido_deveRecalcular() {
            Task task = tarefaComStatus(HOJE.minusDays(3), Criticidade.BAIXA, StatusTarefa.EM_ANDAMENTO);

            Criticidade resultado = calculator.calcular(task, HOJE);

            assertThat(resultado).isEqualTo(Criticidade.URGENTE);
        }
    }

    // =========================================================================
    // Limites de fronteira (boundary values)
    // =========================================================================

    @Nested
    @DisplayName("Valores de fronteira dos limiares de prazo")
    class ValoresDeFronteira {

        @Test
        @DisplayName("Prazo exatamente em 0 dias (hoje) deve ser URGENTE — fronteira inferior do limiar URGENTE")
        void prazoZeroDias_fronteira() {
            Task task = tarefaAtiva(HOJE, Criticidade.BAIXA);
            assertThat(calculator.calcular(task, HOJE)).isEqualTo(Criticidade.URGENTE);
        }

        @Test
        @DisplayName("Prazo em 1 dia deve ser ALTA — primeiro dia acima do limiar URGENTE")
        void prazoUmDia_fronteira() {
            Task task = tarefaAtiva(HOJE.plusDays(1), Criticidade.BAIXA);
            assertThat(calculator.calcular(task, HOJE)).isEqualTo(Criticidade.ALTA);
        }

        @Test
        @DisplayName("Prazo em 2 dias deve ser ALTA — fronteira superior do limiar ALTA")
        void prazoDoisDias_fronteira() {
            Task task = tarefaAtiva(HOJE.plusDays(2), Criticidade.BAIXA);
            assertThat(calculator.calcular(task, HOJE)).isEqualTo(Criticidade.ALTA);
        }

        @Test
        @DisplayName("Prazo em 3 dias deve ser MEDIA — primeiro dia acima do limiar ALTA")
        void prazoTresDias_fronteira() {
            Task task = tarefaAtiva(HOJE.plusDays(3), Criticidade.BAIXA);
            assertThat(calculator.calcular(task, HOJE)).isEqualTo(Criticidade.MEDIA);
        }

        @Test
        @DisplayName("Prazo em 7 dias deve ser MEDIA — fronteira superior do limiar MEDIA")
        void prazoSeteDias_fronteira() {
            Task task = tarefaAtiva(HOJE.plusDays(7), Criticidade.BAIXA);
            assertThat(calculator.calcular(task, HOJE)).isEqualTo(Criticidade.MEDIA);
        }

        @Test
        @DisplayName("Prazo em 8 dias deve ser BAIXA — primeiro dia acima do limiar MEDIA")
        void prazoOitoDias_fronteira() {
            Task task = tarefaAtiva(HOJE.plusDays(8), Criticidade.BAIXA);
            assertThat(calculator.calcular(task, HOJE)).isEqualTo(Criticidade.BAIXA);
        }
    }
}
