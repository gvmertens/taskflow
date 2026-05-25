Você é um especialista em Clean Code e princípios SOLID.

Analise o código abaixo e identifique violações de SOLID ou boas práticas:

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

Raciocine sobre cada problema encontrado e proponha a refatoração.
Para cada mudança:
1. Identifique qual princípio está sendo violado
2. Explique o impacto do problema
3. Mostre o código refatorado
4. Explique por que a nova versão é melhor

Restrições:
- Mantenha compatibilidade com os testes existentes
- Não altere os contratos dos endpoints REST