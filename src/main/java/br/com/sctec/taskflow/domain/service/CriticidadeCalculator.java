package br.com.sctec.taskflow.domain.service;

import br.com.sctec.taskflow.domain.entity.Task;
import br.com.sctec.taskflow.domain.enums.Criticidade;
import br.com.sctec.taskflow.domain.enums.StatusTarefa;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Componente de domínio puro responsável pelo cálculo automático da
 * {@link Criticidade} de uma tarefa com base no prazo e na criticidade
 * informada pelo usuário.
 *
 * <h3>Regras de negócio</h3>
 * <ol>
 *   <li>Tarefas com status {@code CONCLUIDA} ou {@code CANCELADA} nunca são
 *       recalculadas — retorna a criticidade atual sem alteração.</li>
 *   <li>Cálculo base pelo prazo (em relação à data de referência):
 *     <ul>
 *       <li>Prazo hoje ou no passado → {@code URGENTE} (CRITICAL)</li>
 *       <li>Prazo nos próximos 2 dias → {@code ALTA} (HIGH)</li>
 *       <li>Prazo nos próximos 7 dias → {@code MEDIA} (MEDIUM)</li>
 *       <li>Prazo após 7 dias → {@code BAIXA} (LOW)</li>
 *     </ul>
 *   </li>
 *   <li>A criticidade informada pelo usuário eleva o resultado em um nível
 *       quando é {@code ALTA} (equivalente à categoria WORK da especificação).
 *       O teto é {@code URGENTE}.</li>
 * </ol>
 *
 * <p>Esta classe é um POJO puro — sem dependências de Spring ou JPA —
 * garantindo testabilidade isolada e determinismo total.</p>
 */
public class CriticidadeCalculator {

    /**
     * Calcula a criticidade efetiva da tarefa em relação à data de referência.
     *
     * @param task       a tarefa a ser avaliada (não nula)
     * @param referencia a data de referência para o cálculo (normalmente {@code LocalDate.now()})
     * @return a {@link Criticidade} calculada
     */
    public Criticidade calcular(Task task, LocalDate referencia) {
        // Regra: tarefas encerradas nunca são recalculadas
        if (isEncerrada(task.getStatus())) {
            return task.getCriticidade();
        }

        Criticidade base = calcularPorPrazo(task.getPrazo(), referencia);
        return aplicarBonus(base, task.getCriticidade());
    }

    // -------------------------------------------------------------------------
    // Cálculo base por prazo
    // -------------------------------------------------------------------------

    /**
     * Determina a criticidade base conforme a distância entre o prazo e a
     * data de referência.
     *
     * @param prazo      data de vencimento da tarefa
     * @param referencia data de referência (hoje)
     * @return criticidade base
     */
    private Criticidade calcularPorPrazo(LocalDate prazo, LocalDate referencia) {
        long diasRestantes = ChronoUnit.DAYS.between(referencia, prazo);

        if (diasRestantes <= 0) {
            // Prazo hoje ou já passou → CRITICAL
            return Criticidade.URGENTE;
        } else if (diasRestantes <= 2) {
            // Próximos 2 dias → HIGH
            return Criticidade.ALTA;
        } else if (diasRestantes <= 7) {
            // Próximos 7 dias → MEDIUM
            return Criticidade.MEDIA;
        } else {
            // Após 7 dias → LOW
            return Criticidade.BAIXA;
        }
    }

    // -------------------------------------------------------------------------
    // Bônus por criticidade informada pelo usuário
    // -------------------------------------------------------------------------

    /**
     * Eleva a criticidade base em um nível quando o usuário marcou a tarefa
     * como {@code ALTA} (equivalente à categoria WORK da especificação).
     * O teto é {@code URGENTE}.
     *
     * @param base              criticidade calculada pelo prazo
     * @param criticidadeUsuario criticidade informada no request
     * @return criticidade final após aplicação do bônus
     */
    private Criticidade aplicarBonus(Criticidade base, Criticidade criticidadeUsuario) {
        if (criticidadeUsuario != Criticidade.ALTA) {
            return base;
        }

        return switch (base) {
            case BAIXA   -> Criticidade.MEDIA;   // LOW  → MEDIUM
            case MEDIA   -> Criticidade.ALTA;    // MEDIUM → HIGH
            case ALTA    -> Criticidade.URGENTE; // HIGH → CRITICAL
            case URGENTE -> Criticidade.URGENTE; // CRITICAL já é o teto
        };
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private boolean isEncerrada(StatusTarefa status) {
        return status == StatusTarefa.CONCLUIDA || status == StatusTarefa.CANCELADA;
    }
}
