package br.com.sctec.taskflow.dto;

import br.com.sctec.taskflow.domain.enums.Criticidade;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Payload de criação e atualização de tarefas.
 * Score de prioridade e status são ignorados — calculados/gerenciados internamente.
 */
public record TaskRequest(

        @NotBlank(message = "O título não pode ser vazio")
        String titulo,

        String descricao,

        @NotNull(message = "O prazo é obrigatório")
        @Future(message = "O prazo deve ser uma data futura")
        LocalDate prazo,

        @NotNull(message = "A criticidade é obrigatória")
        Criticidade criticidade
) {}
