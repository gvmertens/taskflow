package br.com.sctec.taskflow.domain.exception;

import br.com.sctec.taskflow.domain.enums.StatusTarefa;

/**
 * Lançada quando se tenta realizar uma operação em uma tarefa que já está
 * em estado terminal (CONCLUIDA ou CANCELADA).
 */
public class TarefaEncerradaException extends RuntimeException {

    private final StatusTarefa statusAtual;

    public TarefaEncerradaException(StatusTarefa statusAtual) {
        super("Tarefa já encerrada com status: " + statusAtual
                + ". Tarefas com status CONCLUIDA ou CANCELADA não aceitam mais transições.");
        this.statusAtual = statusAtual;
    }

    public StatusTarefa getStatusAtual() {
        return statusAtual;
    }
}
