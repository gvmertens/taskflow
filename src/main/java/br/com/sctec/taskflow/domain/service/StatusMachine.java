package br.com.sctec.taskflow.domain.service;

import br.com.sctec.taskflow.domain.enums.StatusTarefa;
import br.com.sctec.taskflow.domain.exception.TarefaEncerradaException;
import br.com.sctec.taskflow.domain.exception.TransicaoInvalidaException;

import java.util.Map;
import java.util.Set;

/**
 * Componente de domínio puro (POJO) responsável por validar as transições
 * de status de uma tarefa conforme as regras de negócio definidas.
 *
 * <p>Transições válidas:
 * <ul>
 *   <li>PENDENTE → EM_ANDAMENTO</li>
 *   <li>PENDENTE → CONCLUIDA</li>
 *   <li>PENDENTE → CANCELADA</li>
 *   <li>EM_ANDAMENTO → CONCLUIDA</li>
 *   <li>EM_ANDAMENTO → CANCELADA</li>
 * </ul>
 *
 * <p>Transições inválidas:
 * <ul>
 *   <li>EM_ANDAMENTO → PENDENTE — lança {@link TransicaoInvalidaException}</li>
 *   <li>CONCLUIDA → qualquer — lança {@link TarefaEncerradaException}</li>
 *   <li>CANCELADA → qualquer — lança {@link TarefaEncerradaException}</li>
 * </ul>
 */
public class StatusMachine {

    /**
     * Mapa de transições válidas: cada status de origem mapeia para o conjunto
     * de status de destino permitidos.
     */
    private static final Map<StatusTarefa, Set<StatusTarefa>> TRANSICOES_VALIDAS = Map.of(
            StatusTarefa.PENDENTE, Set.of(
                    StatusTarefa.EM_ANDAMENTO,
                    StatusTarefa.CONCLUIDA,
                    StatusTarefa.CANCELADA
            ),
            StatusTarefa.EM_ANDAMENTO, Set.of(
                    StatusTarefa.CONCLUIDA,
                    StatusTarefa.CANCELADA
            )
    );

    /**
     * Valida se a transição do status {@code atual} para o status {@code destino} é permitida.
     *
     * @param atual    status atual da tarefa
     * @param destino  status de destino desejado
     * @throws TarefaEncerradaException   se {@code atual} for {@code CONCLUIDA} ou {@code CANCELADA}
     * @throws TransicaoInvalidaException se a transição não for permitida pelas regras de negócio
     */
    public void validarTransicao(StatusTarefa atual, StatusTarefa destino) {
        // Estados terminais não permitem nenhuma transição
        if (atual == StatusTarefa.CONCLUIDA || atual == StatusTarefa.CANCELADA) {
            throw new TarefaEncerradaException(atual);
        }

        // Verifica se a transição está no conjunto de transições válidas
        Set<StatusTarefa> destinosPermitidos = TRANSICOES_VALIDAS.getOrDefault(atual, Set.of());
        if (!destinosPermitidos.contains(destino)) {
            throw new TransicaoInvalidaException(atual, destino);
        }
    }
}
