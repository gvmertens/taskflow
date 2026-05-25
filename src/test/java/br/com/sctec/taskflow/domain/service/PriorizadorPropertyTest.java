package br.com.sctec.taskflow.domain.service;

import br.com.sctec.taskflow.domain.enums.Criticidade;
import br.com.sctec.taskflow.domain.enums.StatusTarefa;
import br.com.sctec.taskflow.domain.model.Tarefa;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.Positive;
import org.assertj.core.api.Assertions;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Property-based tests para {@link Priorizador} — tarefas 3.2 a 3.7.
 *
 * <p>Cada {@code @Property} valida uma invariante do domínio usando jqwik 1.8.x
 * com {@code tries = 100} amostras geradas aleatoriamente.</p>
 *
 * <ul>
 *   <li>Property 1 (3.2): Score sempre dentro dos limites [0, 100]</li>
 *   <li>Property 2 (3.3): Tarefa vencida recebe score máximo (100)</li>
 *   <li>Property 3 (3.4): Criticidade determina ordenação quando prazo e idade são iguais</li>
 *   <li>Property 4 (3.5): Prazo mais próximo gera score maior quando criticidade e idade são iguais</li>
 *   <li>Property 5 (3.6): Tarefa mais antiga recebe score maior quando criticidade e prazo são iguais</li>
 *   <li>Property 6 (3.7): Determinismo — mesma entrada sempre produz mesmo resultado</li>
 * </ul>
 */
class PriorizadorPropertyTest {

    private final Priorizador priorizador = new Priorizador();

    /** Instante de referência fixo para todos os testes. */
    private static final Instant REFERENCIA = Instant.parse("2026-05-25T12:00:00Z");

    // =========================================================================
    // Helpers
    // =========================================================================

    private Tarefa tarefa(Instant prazo, Criticidade criticidade, Instant criadoEm) {
        return new Tarefa(
                UUID.randomUUID(),
                "Tarefa gerada",
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
    // Property 1 — Score sempre dentro dos limites [0, 100]
    // Validates: Requirement 6.2
    // =========================================================================

    /**
     * Para qualquer combinação válida de prazo futuro, criticidade e idade,
     * o score deve estar sempre no intervalo [0, 100].
     */
    @Property(tries = 100)
    @Label("Property 1: Score sempre dentro dos limites [0, 100]")
    void scoreSempreDentroDoLimite(
            @ForAll("prazosValidos") Instant prazo,
            @ForAll Criticidade criticidade,
            @ForAll("idadesValidas") Instant criadoEm
    ) {
        Tarefa t = tarefa(prazo, criticidade, criadoEm);

        int score = priorizador.calcular(t, REFERENCIA);

        Assertions.assertThat(score)
                .as("Score deve estar entre 0 e 100")
                .isBetween(0, 100);
    }

    // =========================================================================
    // Property 2 — Tarefa vencida recebe score máximo (100)
    // Validates: Requirement 6.6
    // =========================================================================

    /**
     * Para qualquer tarefa cujo prazo seja anterior ou igual à referência,
     * o score deve ser exatamente 100.
     */
    @Property(tries = 100)
    @Label("Property 2: Tarefa vencida recebe score máximo (100)")
    void tarefaVencidaRecebeScoreMaximo(
            @ForAll("prazosVencidos") Instant prazo,
            @ForAll Criticidade criticidade,
            @ForAll("idadesValidas") Instant criadoEm
    ) {
        Tarefa t = tarefa(prazo, criticidade, criadoEm);

        int score = priorizador.calcular(t, REFERENCIA);

        Assertions.assertThat(score)
                .as("Tarefa vencida deve ter score = 100")
                .isEqualTo(100);
    }

    // =========================================================================
    // Property 3 — Criticidade determina ordenação quando prazo e idade são iguais
    // Validates: Requirement 6.3
    // =========================================================================

    /**
     * Quando prazo e idade são idênticos, uma tarefa com criticidade mais alta
     * deve ter score maior ou igual à de criticidade mais baixa.
     *
     * <p>Ordem: BAIXA &lt; MEDIA &lt; ALTA &lt; URGENTE</p>
     */
    @Property(tries = 100)
    @Label("Property 3: Criticidade mais alta gera score maior ou igual quando prazo e idade são iguais")
    void criticidadeMaisAltaGeraScoreMaiorOuIgual(
            @ForAll("prazosValidos") Instant prazo,
            @ForAll("idadesValidas") Instant criadoEm
    ) {
        Tarefa baixa   = tarefa(prazo, Criticidade.BAIXA,   criadoEm);
        Tarefa media   = tarefa(prazo, Criticidade.MEDIA,   criadoEm);
        Tarefa alta    = tarefa(prazo, Criticidade.ALTA,    criadoEm);
        Tarefa urgente = tarefa(prazo, Criticidade.URGENTE, criadoEm);

        int sBaixa   = priorizador.calcular(baixa,   REFERENCIA);
        int sMedia   = priorizador.calcular(media,   REFERENCIA);
        int sAlta    = priorizador.calcular(alta,    REFERENCIA);
        int sUrgente = priorizador.calcular(urgente, REFERENCIA);

        Assertions.assertThat(sBaixa)
                .as("BAIXA <= MEDIA")
                .isLessThanOrEqualTo(sMedia);
        Assertions.assertThat(sMedia)
                .as("MEDIA <= ALTA")
                .isLessThanOrEqualTo(sAlta);
        Assertions.assertThat(sAlta)
                .as("ALTA <= URGENTE")
                .isLessThanOrEqualTo(sUrgente);
    }

    // =========================================================================
    // Property 4 — Prazo mais próximo gera score maior quando criticidade e idade são iguais
    // Validates: Requirement 6.4
    // =========================================================================

    /**
     * Para tarefas com mesma criticidade e mesma idade, aquela com prazo mais
     * próximo (mas ainda futuro) deve ter score maior ou igual.
     */
    @Property(tries = 100)
    @Label("Property 4: Prazo mais próximo gera score maior ou igual quando criticidade e idade são iguais")
    void prazoMaisProximoGeraScoreMaiorOuIgual(
            @ForAll Criticidade criticidade,
            @ForAll("idadesValidas") Instant criadoEm,
            @ForAll @Positive @IntRange(min = 1, max = 500) int horasA,
            @ForAll @Positive @IntRange(min = 1, max = 500) int horasB
    ) {
        // Garante que prazoA < prazoB (prazoA é mais próximo)
        Instant prazoProximo  = REFERENCIA.plus(Math.min(horasA, horasB), ChronoUnit.HOURS);
        Instant prazoDistante = REFERENCIA.plus(Math.max(horasA, horasB) + 1L, ChronoUnit.HOURS);

        Tarefa tProximo  = tarefa(prazoProximo,  criticidade, criadoEm);
        Tarefa tDistante = tarefa(prazoDistante, criticidade, criadoEm);

        int sProximo  = priorizador.calcular(tProximo,  REFERENCIA);
        int sDistante = priorizador.calcular(tDistante, REFERENCIA);

        Assertions.assertThat(sProximo)
                .as("Prazo mais próximo deve ter score >= prazo mais distante")
                .isGreaterThanOrEqualTo(sDistante);
    }

    // =========================================================================
    // Property 5 — Tarefa mais antiga recebe score maior quando criticidade e prazo são iguais
    // Validates: Requirement 6.5
    // =========================================================================

    /**
     * Para tarefas com mesma criticidade e mesmo prazo, a mais antiga (criadoEm
     * mais distante no passado) deve ter score maior ou igual.
     */
    @Property(tries = 100)
    @Label("Property 5: Tarefa mais antiga recebe score maior ou igual quando criticidade e prazo são iguais")
    void tarefaMaisAntigaRecebeScoreMaiorOuIgual(
            @ForAll Criticidade criticidade,
            @ForAll("prazosValidos") Instant prazo,
            @ForAll @Positive @IntRange(min = 1, max = 90) int diasA,
            @ForAll @Positive @IntRange(min = 1, max = 90) int diasB
    ) {
        Instant maisAntiga = REFERENCIA.minus(Math.max(diasA, diasB), ChronoUnit.DAYS);
        Instant maisNova   = REFERENCIA.minus(Math.min(diasA, diasB), ChronoUnit.DAYS);

        Tarefa tAntiga = tarefa(prazo, criticidade, maisAntiga);
        Tarefa tNova   = tarefa(prazo, criticidade, maisNova);

        int sAntiga = priorizador.calcular(tAntiga, REFERENCIA);
        int sNova   = priorizador.calcular(tNova,   REFERENCIA);

        Assertions.assertThat(sAntiga)
                .as("Tarefa mais antiga deve ter score >= tarefa mais nova")
                .isGreaterThanOrEqualTo(sNova);
    }

    // =========================================================================
    // Property 6 — Determinismo: mesma entrada sempre produz mesmo resultado
    // Validates: Requirement 6.8
    // =========================================================================

    /**
     * Chamar {@code calcular} duas vezes com os mesmos argumentos deve sempre
     * retornar o mesmo valor — o cálculo é puro e sem efeitos colaterais.
     */
    @Property(tries = 100)
    @Label("Property 6: Determinismo — mesma entrada sempre produz mesmo resultado")
    void calculoDeterministico(
            @ForAll("prazosValidos") Instant prazo,
            @ForAll Criticidade criticidade,
            @ForAll("idadesValidas") Instant criadoEm
    ) {
        Tarefa t = tarefa(prazo, criticidade, criadoEm);

        int score1 = priorizador.calcular(t, REFERENCIA);
        int score2 = priorizador.calcular(t, REFERENCIA);

        Assertions.assertThat(score1)
                .as("Cálculo deve ser determinístico")
                .isEqualTo(score2);
    }

    // =========================================================================
    // Providers de dados arbitrários
    // =========================================================================

    /** Gera instantes de prazo no futuro (1h a 720h após a referência). */
    @Provide
    Arbitrary<Instant> prazosValidos() {
        return Arbitraries.longs()
                .between(1L, 720L)
                .map(horas -> REFERENCIA.plus(horas, ChronoUnit.HOURS));
    }

    /** Gera instantes de prazo no passado ou igual à referência (0 a 365 dias antes). */
    @Provide
    Arbitrary<Instant> prazosVencidos() {
        return Arbitraries.longs()
                .between(0L, 365L)
                .map(dias -> REFERENCIA.minus(dias, ChronoUnit.DAYS));
    }

    /** Gera instantes de criação no passado (1 a 120 dias antes da referência). */
    @Provide
    Arbitrary<Instant> idadesValidas() {
        return Arbitraries.longs()
                .between(1L, 120L)
                .map(dias -> REFERENCIA.minus(dias, ChronoUnit.DAYS));
    }
}
