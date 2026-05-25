package br.com.sctec.taskflow.domain.enums;

public enum StatusTarefa {
    PENDENTE,
    EM_ANDAMENTO,
    CONCLUIDA,
    CANCELADA;

    /**
     * Retorna {@code true} se este status representa um estado terminal,
     * ou seja, a tarefa não pode mais ser modificada.
     */
    public boolean isEncerrada() {
        return this == CONCLUIDA || this == CANCELADA;
    }
}
