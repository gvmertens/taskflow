package br.com.sctec.taskflow.domain.service;

import br.com.sctec.taskflow.domain.enums.Criticidade;
import br.com.sctec.taskflow.domain.enums.StatusTarefa;
import br.com.sctec.taskflow.domain.model.Tarefa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitários concretos para {@link Priorizador} — tarefa 3.8.
 *
 * <p>Estratégia: POJO puro — sem Spring, sem mocks. Cada teste constrói
 * uma {@link Tarefa} com prazo, criticidade e idade específicos e verifica
 * o score calculado em relação a um instante de referência fixo.</p>
 */
@DisplayName("Priorizador — testes unitários concretos")
class PriorizadorTest {

    private Priorizador priorizador;

    /** Instante de referência fixo — elimina flakiness por data real. */
    private static final Instant AGORA = Instant.parse("2026-05-25T12:00:00Z");

    @BeforeEach
    void setUp() {
        priorizador = new Priorizador();
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private Tarefa tarefa(Instant prazo, Criticidade criticidade, Instant criadoEm) {
        return new Tarefa(
                UUID.randomUUID(),
                "Tarefa de teste",
                null,
                prazo,
                criticidade,
                StatusTarefa.PENDENTE,
                0,
                criadoEm,
                criadoEm,
                null
        );
    }

    // =========================================================================
    // Tarefa vencida → score máximo imediato
    // =========================================================================

    @Nested
    @DisplayName("Tarefa vencida")
    class TarefaVencida {

        @Test
        @DisplayName("Tarefa com prazo exatamente igual à referência deve retornar 100")
        void prazoIgualReferencia_deveRetornar100() {
            Tarefa t = tarefa(AGORA, Criticidade.BAIXA, AGORA.minus(1, ChronoUnit.DAYS));

            assertThat(priorizador.calcular(t, AGORA)).isEqualTo(100);
        }

        @Test
        @DisplayName("Tarefa com prazo 1 segundo antes da referência deve retornar 100")
        void prazoUmSegundoAntes_deveRetornar100() {
            Tarefa t = tarefa(AGORA.minusSeconds(1), Criticidade.BAIXA, AGORA.minus(1, ChronoUnit.DAYS));

            assertThat(priorizador.calcular(t, AGORA)).isEqualTo(100);
        }

        @Test
        @DisplayName("Tarefa URGENTE vencida há 30 dias deve retornar 100 (não soma componentes)")
        void urgenteVencidaHaMuito_deveRetornar100() {
            Tarefa t = tarefa(AGORA.minus(30, ChronoUnit.DAYS), Criticidade.URGENTE,
                    AGORA.minus(60, ChronoUnit.DAYS));

            assertThat(priorizador.calcular(t, AGORA)).isEqualTo(100);
        }
    }

    // =========================================================================
    // Componente criticidadeScore
    // =========================================================================

    @Nested
    @DisplayName("Componente criticidadeScore")
    class CriticidadeScore {

        /** Prazo distante (>168h) e tarefa nova (<7 dias) — isola o componente de criticidade. */
        private static final Instant PRAZO_DISTANTE = Instant.parse("2026-06-30T12:00:00Z");
        private static final Instant CRIADO_RECENTE = Instant.parse("2026-05-24T12:00:00Z");

        @Test
        @DisplayName("BAIXA deve contribuir com 10 pontos")
        void baixa_contribui10() {
            Tarefa t = tarefa(PRAZO_DISTANTE, Criticidade.BAIXA, CRIADO_RECENTE);
            // deadlineScore=0 (>168h), ageBonus=0 (<7 dias) → score = 10
            assertThat(priorizador.calcular(t, AGORA)).isEqualTo(10);
        }

        @Test
        @DisplayName("MEDIA deve contribuir com 25 pontos")
        void media_contribui25() {
            Tarefa t = tarefa(PRAZO_DISTANTE, Criticidade.MEDIA, CRIADO_RECENTE);
            assertThat(priorizador.calcular(t, AGORA)).isEqualTo(25);
        }

        @Test
        @DisplayName("ALTA deve contribuir com 50 pontos")
        void alta_contribui50() {
            Tarefa t = tarefa(PRAZO_DISTANTE, Criticidade.ALTA, CRIADO_RECENTE);
            assertThat(priorizador.calcular(t, AGORA)).isEqualTo(50);
        }

        @Test
        @DisplayName("URGENTE deve contribuir com 70 pontos")
        void urgente_contribui70() {
            Tarefa t = tarefa(PRAZO_DISTANTE, Criticidade.URGENTE, CRIADO_RECENTE);
            assertThat(priorizador.calcular(t, AGORA)).isEqualTo(70);
        }
    }

    // =========================================================================
    // Componente deadlineScore
    // =========================================================================

    @Nested
    @DisplayName("Componente deadlineScore")
    class DeadlineScore {

        /** Criticidade BAIXA (10 pts) e tarefa nova (<7 dias) — isola o componente de prazo. */
        private static final Instant CRIADO_RECENTE = Instant.parse("2026-05-24T12:00:00Z");

        @Test
        @DisplayName("Prazo em exatamente 24h deve contribuir com +25 pontos")
        void prazoEm24h_contribui25() {
            Instant prazo = AGORA.plus(24, ChronoUnit.HOURS);
            Tarefa t = tarefa(prazo, Criticidade.BAIXA, CRIADO_RECENTE);
            // 10 (BAIXA) + 25 (≤24h) + 0 (age) = 35
            assertThat(priorizador.calcular(t, AGORA)).isEqualTo(35);
        }

        @Test
        @DisplayName("Prazo em 1h deve contribuir com +25 pontos (≤24h)")
        void prazoEm1h_contribui25() {
            Instant prazo = AGORA.plus(1, ChronoUnit.HOURS);
            Tarefa t = tarefa(prazo, Criticidade.BAIXA, CRIADO_RECENTE);
            assertThat(priorizador.calcular(t, AGORA)).isEqualTo(35);
        }

        @Test
        @DisplayName("Prazo em 25h deve contribuir com +15 pontos (≤72h)")
        void prazoEm25h_contribui15() {
            Instant prazo = AGORA.plus(25, ChronoUnit.HOURS);
            Tarefa t = tarefa(prazo, Criticidade.BAIXA, CRIADO_RECENTE);
            // 10 + 15 + 0 = 25
            assertThat(priorizador.calcular(t, AGORA)).isEqualTo(25);
        }

        @Test
        @DisplayName("Prazo em exatamente 72h deve contribuir com +15 pontos")
        void prazoEm72h_contribui15() {
            Instant prazo = AGORA.plus(72, ChronoUnit.HOURS);
            Tarefa t = tarefa(prazo, Criticidade.BAIXA, CRIADO_RECENTE);
            assertThat(priorizador.calcular(t, AGORA)).isEqualTo(25);
        }

        @Test
        @DisplayName("Prazo em 73h deve contribuir com +8 pontos (≤168h)")
        void prazoEm73h_contribui8() {
            Instant prazo = AGORA.plus(73, ChronoUnit.HOURS);
            Tarefa t = tarefa(prazo, Criticidade.BAIXA, CRIADO_RECENTE);
            // 10 + 8 + 0 = 18
            assertThat(priorizador.calcular(t, AGORA)).isEqualTo(18);
        }

        @Test
        @DisplayName("Prazo em exatamente 168h deve contribuir com +8 pontos")
        void prazoEm168h_contribui8() {
            Instant prazo = AGORA.plus(168, ChronoUnit.HOURS);
            Tarefa t = tarefa(prazo, Criticidade.BAIXA, CRIADO_RECENTE);
            assertThat(priorizador.calcular(t, AGORA)).isEqualTo(18);
        }

        @Test
        @DisplayName("Prazo em 169h deve contribuir com +0 pontos (>168h)")
        void prazoEm169h_contribui0() {
            Instant prazo = AGORA.plus(169, ChronoUnit.HOURS);
            Tarefa t = tarefa(prazo, Criticidade.BAIXA, CRIADO_RECENTE);
            // 10 + 0 + 0 = 10
            assertThat(priorizador.calcular(t, AGORA)).isEqualTo(10);
        }
    }

    // =========================================================================
    // Componente ageBonus
    // =========================================================================

    @Nested
    @DisplayName("Componente ageBonus")
    class AgeBonus {

        /** Prazo distante (>168h) e criticidade BAIXA — isola o componente de idade. */
        private static final Instant PRAZO_DISTANTE = Instant.parse("2026-06-30T12:00:00Z");

        @Test
        @DisplayName("Tarefa com menos de 7 dias de vida deve contribuir com +0 pontos")
        void idadeMenos7Dias_contribui0() {
            Instant criadoEm = AGORA.minus(6, ChronoUnit.DAYS);
            Tarefa t = tarefa(PRAZO_DISTANTE, Criticidade.BAIXA, criadoEm);
            // 10 + 0 + 0 = 10
            assertThat(priorizador.calcular(t, AGORA)).isEqualTo(10);
        }

        @Test
        @DisplayName("Tarefa com exatamente 7 dias de vida deve contribuir com +3 pontos")
        void idade7Dias_contribui3() {
            Instant criadoEm = AGORA.minus(7, ChronoUnit.DAYS);
            Tarefa t = tarefa(PRAZO_DISTANTE, Criticidade.BAIXA, criadoEm);
            // 10 + 0 + 3 = 13
            assertThat(priorizador.calcular(t, AGORA)).isEqualTo(13);
        }

        @Test
        @DisplayName("Tarefa com 15 dias de vida deve contribuir com +3 pontos (≥7 e <30)")
        void idade15Dias_contribui3() {
            Instant criadoEm = AGORA.minus(15, ChronoUnit.DAYS);
            Tarefa t = tarefa(PRAZO_DISTANTE, Criticidade.BAIXA, criadoEm);
            assertThat(priorizador.calcular(t, AGORA)).isEqualTo(13);
        }

        @Test
        @DisplayName("Tarefa com exatamente 30 dias de vida deve contribuir com +5 pontos")
        void idade30Dias_contribui5() {
            Instant criadoEm = AGORA.minus(30, ChronoUnit.DAYS);
            Tarefa t = tarefa(PRAZO_DISTANTE, Criticidade.BAIXA, criadoEm);
            // 10 + 0 + 5 = 15
            assertThat(priorizador.calcular(t, AGORA)).isEqualTo(15);
        }

        @Test
        @DisplayName("Tarefa com 60 dias de vida deve contribuir com +5 pontos (≥30)")
        void idade60Dias_contribui5() {
            Instant criadoEm = AGORA.minus(60, ChronoUnit.DAYS);
            Tarefa t = tarefa(PRAZO_DISTANTE, Criticidade.BAIXA, criadoEm);
            assertThat(priorizador.calcular(t, AGORA)).isEqualTo(15);
        }
    }

    // =========================================================================
    // Cenários compostos — combinação dos três componentes
    // =========================================================================

    @Nested
    @DisplayName("Cenários compostos")
    class CenariosCompostos {

        @Test
        @DisplayName("URGENTE com prazo em 1h deve retornar 95 (70+25+0)")
        void urgente_prazoEm1h_semIdade() {
            Instant prazo = AGORA.plus(1, ChronoUnit.HOURS);
            Instant criadoEm = AGORA.minus(1, ChronoUnit.DAYS);
            Tarefa t = tarefa(prazo, Criticidade.URGENTE, criadoEm);
            // 70 + 25 + 0 = 95
            assertThat(priorizador.calcular(t, AGORA)).isEqualTo(95);
        }

        @Test
        @DisplayName("MEDIA com prazo em 5 dias e 10 dias de idade deve retornar 36 (25+8+3)")
        void media_prazoEm5Dias_idade10Dias() {
            // 5 dias = 120h → deadlineScore = +8 (≤168h)
            Instant prazo = AGORA.plus(5 * 24, ChronoUnit.HOURS);
            Instant criadoEm = AGORA.minus(10, ChronoUnit.DAYS);
            Tarefa t = tarefa(prazo, Criticidade.MEDIA, criadoEm);
            // 25 + 8 + 3 = 36
            assertThat(priorizador.calcular(t, AGORA)).isEqualTo(36);
        }

        @Test
        @DisplayName("URGENTE com prazo em 30 min e 35 dias de idade deve ser clampado em 100")
        void urgente_prazoMuitoProximo_idadeGrande_clampado100() {
            Instant prazo = AGORA.plus(30, ChronoUnit.MINUTES);
            Instant criadoEm = AGORA.minus(35, ChronoUnit.DAYS);
            Tarefa t = tarefa(prazo, Criticidade.URGENTE, criadoEm);
            // 70 + 25 + 5 = 100 → clamp não altera
            assertThat(priorizador.calcular(t, AGORA)).isEqualTo(100);
        }

        @Test
        @DisplayName("BAIXA com prazo distante e tarefa nova deve retornar 10 (score mínimo real)")
        void baixa_prazoDistante_tarefaNova_scoreMinimo() {
            Instant prazo = Instant.parse("2027-01-01T00:00:00Z");
            Instant criadoEm = AGORA.minus(1, ChronoUnit.DAYS);
            Tarefa t = tarefa(prazo, Criticidade.BAIXA, criadoEm);
            // 10 + 0 + 0 = 10
            assertThat(priorizador.calcular(t, AGORA)).isEqualTo(10);
        }
    }
}
