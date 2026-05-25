package br.com.sctec.taskflow.dto;

import br.com.sctec.taskflow.domain.entity.Task;
import br.com.sctec.taskflow.domain.enums.Criticidade;
import br.com.sctec.taskflow.domain.enums.StatusTarefa;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Representação de saída de uma tarefa.
 * Construído a partir da entidade via factory method {@link #from(Task)}.
 */
public record TaskResponse(
        UUID id,
        String titulo,
        String descricao,
        LocalDate prazo,
        Criticidade criticidade,
        StatusTarefa status,
        Integer scorePrioridade,
        Instant concluidaEm,
        Instant criadoEm,
        Instant atualizadoEm
) {
    public static TaskResponse from(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitulo(),
                task.getDescricao(),
                task.getPrazo(),
                task.getCriticidade(),
                task.getStatus(),
                task.getScorePrioridade(),
                task.getConcluidaEm(),
                task.getCriadoEm(),
                task.getAtualizadoEm()
        );
    }
}
