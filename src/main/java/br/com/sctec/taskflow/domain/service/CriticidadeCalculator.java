package br.com.sctec.taskflow.domain.service;

import br.com.sctec.taskflow.domain.entity.Task;
import br.com.sctec.taskflow.domain.enums.Criticidade;

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

    // Limiares de prazo (em dias) — centralizados para facilitar manutenção
    private static final long LIMIAR_URGENTE = 0;
    private static final long LIMIAR_ALTA    = 2;
    private static final long LIMIAR_MEDIA   = 7;

    /**
     * Calcula a criticidade efetiva da tarefa em relação à data de referência.
     *
     * @param task       a tarefa a ser avaliada (não nula)
     * @param referencia a data de referência para o cálculo (normalmente {@code LocalDate.now()})
     * @return a {@link Criticidade} calculada
     */
    public Criticidade calcular(Task task, LocalDate referencia) {
        // Regra: tarefas encerradas nunca são recalculadas
        if (task.getStatus().isEncerrada()) {
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

        if (diasRestantes <= LIMIAR_URGENTE) return Criticidade.URGENTE; // Prazo hoje ou já passou → CRITICAL
        if (diasRestantes <= LIMIAR_ALTA)    return Criticidade.ALTA;    // Próximos 2 dias → HIGH
        if (diasRestantes <= LIMIAR_MEDIA)   return Criticidade.MEDIA;   // Próximos 7 dias → MEDIUM
        return Criticidade.BAIXA;                                         // Após 7 dias → LOW
    }

    // -------------------------------------------------------------------------
    // Bônus por criticidade informada pelo usuário
    // -------------------------------------------------------------------------

    /**
     * Eleva a criticidade base em um nível quando o usuário marcou a tarefa
     * como {@code ALTA} (equivalente à categoria WORK da especificação).
     * O teto é {@code URGENTE}.
     *
     * @param base               criticidade calculada pelo prazo
     * @param criticidadeUsuario criticidade informada no request
     * @return criticidade final após aplicação do bônus
     */
    private Criticidade aplicarBonus(Criticidade base, Criticidade criticidadeUsuario) {
        if (!deveElevar(criticidadeUsuario)) {
            return base;
        }
        return elevarUmNivel(base);
    }

    /**
     * Define quais criticidades informadas pelo usuário ativam o bônus de elevação.
     */
    private boolean deveElevar(Criticidade criticidadeUsuario) {
        return criticidadeUsuario == Criticidade.ALTA;
    }

    /**
     * Eleva a criticidade em exatamente um nível, respeitando o teto {@code URGENTE}.
     */
    private Criticidade elevarUmNivel(Criticidade criticidade) {
        return switch (criticidade) {
            case BAIXA   -> Criticidade.MEDIA;   // LOW    → MEDIUM
            case MEDIA   -> Criticidade.ALTA;    // MEDIUM → HIGH
            case ALTA    -> Criticidade.URGENTE; // HIGH   → CRITICAL
            case URGENTE -> Criticidade.URGENTE; // CRITICAL já é o teto
        };
    }
}
