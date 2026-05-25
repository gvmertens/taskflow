package br.com.sctec.taskflow.dto;

import br.com.sctec.taskflow.domain.enums.StatusTarefa;
import jakarta.validation.constraints.NotNull;

/**
 * DTO de entrada para a operação de transição de status de uma tarefa.
 *
 * <p>Utilizado no endpoint {@code PATCH /api/v1/tasks/{id}/status}.</p>
 *
 * @param status novo status desejado para a tarefa (obrigatório)
 */
public record TransitionStatusRequest(
        @NotNull(message = "O status de destino é obrigatório")
        StatusTarefa status
) {
}
