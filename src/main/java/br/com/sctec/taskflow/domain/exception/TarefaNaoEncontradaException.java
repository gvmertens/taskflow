package br.com.sctec.taskflow.domain.exception;

import java.util.UUID;

/**
 * Lançada quando uma tarefa não é encontrada no repositório pelo ID informado.
 */
public class TarefaNaoEncontradaException extends RuntimeException {

    private final UUID id;

    public TarefaNaoEncontradaException(UUID id) {
        super("Tarefa não encontrada com id: " + id);
        this.id = id;
    }

    public UUID getId() {
        return id;
    }
}
