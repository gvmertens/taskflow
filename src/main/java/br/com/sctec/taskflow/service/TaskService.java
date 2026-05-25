package br.com.sctec.taskflow.service;

import br.com.sctec.taskflow.domain.entity.Task;
import br.com.sctec.taskflow.domain.enums.Criticidade;
import br.com.sctec.taskflow.domain.enums.StatusTarefa;
import br.com.sctec.taskflow.domain.service.CriticidadeCalculator;
import br.com.sctec.taskflow.dto.TaskRequest;
import br.com.sctec.taskflow.dto.TaskResponse;
import br.com.sctec.taskflow.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository repository;
    private final CriticidadeCalculator criticidadeCalculator;

    // -------------------------------------------------------------------------
    // CRUD principal
    // -------------------------------------------------------------------------

    /**
     * Cria uma nova tarefa com status PENDENTE e score calculado automaticamente.
     */
    @Transactional
    public TaskResponse create(TaskRequest request) {
        Task task = Task.builder()
                .titulo(request.titulo())
                .descricao(request.descricao())
                .prazo(request.prazo())
                .criticidade(request.criticidade())
                .status(StatusTarefa.PENDENTE)
                .build();

        // Calcula a criticidade efetiva com base no prazo e na criticidade informada
        Criticidade criticidadeEfetiva = criticidadeCalculator.calcular(task, LocalDate.now());
        task.setCriticidade(criticidadeEfetiva);

        return TaskResponse.from(repository.save(task));
    }

    /**
     * Retorna todas as tarefas paginadas, com filtros opcionais de status e criticidade.
     * Ordenação padrão: scorePrioridade DESC, prazo ASC (definida no Pageable do controller).
     */
    @Transactional(readOnly = true)
    public Page<TaskResponse> findAll(StatusTarefa status, Criticidade criticidade, Pageable pageable) {
        Page<Task> page;

        if (status != null && criticidade != null) {
            page = repository.findByStatusAndCriticidade(status, criticidade, pageable);
        } else if (status != null) {
            page = repository.findByStatus(status, pageable);
        } else if (criticidade != null) {
            page = repository.findByCriticidade(criticidade, pageable);
        } else {
            page = repository.findAll(pageable);
        }

        return page.map(TaskResponse::from);
    }

    /**
     * Retorna uma tarefa pelo seu identificador.
     *
     * @throws jakarta.persistence.EntityNotFoundException se não encontrada
     */
    @Transactional(readOnly = true)
    public TaskResponse findById(UUID id) {
        Task task = getOrThrow(id);
        return TaskResponse.from(task);
    }

    /**
     * Atualiza título, descrição, prazo e criticidade de uma tarefa ativa.
     * Recalcula o score de prioridade após a atualização.
     *
     * @throws jakarta.persistence.EntityNotFoundException se não encontrada
     * @throws IllegalStateException se a tarefa estiver encerrada (CONCLUIDA ou CANCELADA)
     */
    @Transactional
    public TaskResponse update(UUID id, TaskRequest request) {
        Task task = getOrThrow(id);

        if (task.getStatus().isEncerrada()) {
            throw new IllegalStateException(
                    "Não é possível atualizar uma tarefa encerrada (status: " + task.getStatus() + ")");
        }

        task.setTitulo(request.titulo());
        task.setDescricao(request.descricao());
        task.setPrazo(request.prazo());
        task.setCriticidade(request.criticidade());

        // Recalcula a criticidade efetiva após a atualização dos dados
        Criticidade criticidadeEfetiva = criticidadeCalculator.calcular(task, LocalDate.now());
        task.setCriticidade(criticidadeEfetiva);

        return TaskResponse.from(repository.save(task));
    }

    /**
     * Remove permanentemente uma tarefa pelo identificador.
     *
     * @throws jakarta.persistence.EntityNotFoundException se não encontrada
     */
    @Transactional
    public void delete(UUID id) {
        Task task = getOrThrow(id);
        repository.delete(task);
    }

    // -------------------------------------------------------------------------
    // Helpers privados
    // -------------------------------------------------------------------------

    private Task getOrThrow(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Tarefa não encontrada: " + id));
    }
}
