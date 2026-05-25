package br.com.sctec.taskflow.domain.exception;

import br.com.sctec.taskflow.domain.enums.StatusTarefa;

/**
 * Lançada quando se tenta realizar uma transição de status que não é permitida
 * pelas regras da StatusMachine (ex.: EM_ANDAMENTO → PENDENTE).
 */
public class TransicaoInvalidaException extends RuntimeException {

    private final StatusTarefa statusAtual;
    private final StatusTarefa statusDestino;

    public TransicaoInvalidaException(StatusTarefa statusAtual, StatusTarefa statusDestino) {
        super("Transição de " + statusAtual + " para " + statusDestino + " não é permitida.");
        this.statusAtual = statusAtual;
        this.statusDestino = statusDestino;
    }

    public StatusTarefa getStatusAtual() {
        return statusAtual;
    }

    public StatusTarefa getStatusDestino() {
        return statusDestino;
    }
}
