package br.com.sctec.taskflow.repository;

import br.com.sctec.taskflow.domain.entity.Task;
import br.com.sctec.taskflow.domain.enums.Criticidade;
import br.com.sctec.taskflow.domain.enums.StatusTarefa;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {

    /** Lista tarefas filtrando por status, com paginação. */
    Page<Task> findByStatus(StatusTarefa status, Pageable pageable);

    /** Lista tarefas filtrando por criticidade, com paginação. */
    Page<Task> findByCriticidade(Criticidade criticidade, Pageable pageable);

    /** Lista tarefas filtrando por status e criticidade, com paginação. */
    Page<Task> findByStatusAndCriticidade(StatusTarefa status, Criticidade criticidade, Pageable pageable);
}
