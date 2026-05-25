package br.com.sctec.taskflow.domain.service;

import br.com.sctec.taskflow.domain.enums.Criticidade;
import br.com.sctec.taskflow.domain.model.Tarefa;

import java.time.Duration;
import java.time.Instant;

/**
 * Componente de domínio puro responsável pelo cálculo do score de prioridade
 * de uma {@link Tarefa}.
 *
 * <p>Fórmula: {@code score = clamp(criticidadeScore + deadlineScore + ageBonus, 0, 100)}</p>
 *
 * <p>Regra especial: se a tarefa estiver vencida (prazo &lt; referencia),
 * retorna 100 imediatamente, sem somar os demais componentes.</p>
 *
 * <p>Esta classe é um POJO puro — sem dependências de Spring ou JPA —
 * garantindo testabilidade isolada e determinismo total.</p>
 */
public class Priorizador {

    /**
     * Calcula o score de prioridade da tarefa em relação ao instante de referência.
     *
     * @param tarefa     a tarefa a ser avaliada (não nula)
     * @param referencia o instante de referência para o cálculo (não nulo)
     * @return score inteiro no intervalo [0, 100]
     */
    public int calcular(Tarefa tarefa, Instant referencia) {
        // Tarefa vencida: score máximo imediato
        if (!tarefa.getPrazo().isAfter(referencia)) {
            return 100;
        }

        int score = criticidadeScore(tarefa.getCriticidade())
                + deadlineScore(tarefa.getPrazo(), referencia)
                + ageBonus(tarefa.getCriadoEm(), referencia);

        return clamp(score, 0, 100);
    }

    // -------------------------------------------------------------------------
    // Componentes do cálculo
    // -------------------------------------------------------------------------

    /**
     * Peso fixo por nível de criticidade.
     * BAIXA=10, MEDIA=25, ALTA=50, URGENTE=70
     */
    private int criticidadeScore(Criticidade criticidade) {
        return switch (criticidade) {
            case BAIXA   -> 10;
            case MEDIA   -> 25;
            case ALTA    -> 50;
            case URGENTE -> 70;
        };
    }

    /**
     * Score baseado na proximidade do prazo.
     * Pré-condição: prazo > referencia (tarefa não vencida).
     *
     * ≤ 24h  → +25
     * ≤ 72h  → +15
     * ≤ 168h → +8
     * caso contrário → +0
     */
    private int deadlineScore(Instant prazo, Instant referencia) {
        long horasRestantes = Duration.between(referencia, prazo).toHours();

        if (horasRestantes <= 24) {
            return 25;
        } else if (horasRestantes <= 72) {
            return 15;
        } else if (horasRestantes <= 168) {
            return 8;
        } else {
            return 0;
        }
    }

    /**
     * Bônus de desempate por idade da tarefa.
     *
     * ≥ 30 dias → +5
     * ≥ 7 dias  → +3
     * caso contrário → +0
     */
    private int ageBonus(Instant criadoEm, Instant referencia) {
        long diasDeVida = Duration.between(criadoEm, referencia).toDays();

        if (diasDeVida >= 30) {
            return 5;
        } else if (diasDeVida >= 7) {
            return 3;
        } else {
            return 0;
        }
    }

    /**
     * Restringe {@code value} ao intervalo [{@code min}, {@code max}].
     */
    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
