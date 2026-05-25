package br.com.sctec.taskflow.controller;

import br.com.sctec.taskflow.domain.enums.Criticidade;
import br.com.sctec.taskflow.domain.enums.StatusTarefa;
import br.com.sctec.taskflow.dto.TransitionStatusRequest;
import br.com.sctec.taskflow.dto.TaskRequest;
import br.com.sctec.taskflow.dto.TaskResponse;
import br.com.sctec.taskflow.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

/**
 * Endpoints REST para gerenciamento de tarefas.
 *
 * <p>Base path: {@code /api/v1/tasks}</p>
 */
@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    /**
     * Cria uma nova tarefa com prioridade calculada automaticamente.
     *
     * @return 201 Created com a tarefa criada no corpo e Location header
     */
    @PostMapping
    public ResponseEntity<TaskResponse> create(@Valid @RequestBody TaskRequest request) {
        TaskResponse response = taskService.create(request);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    /**
     * Lista todas as tarefas com paginação e filtros opcionais.
     * Ordenação padrão: scorePrioridade DESC, prazo ASC.
     *
     * @param status      filtro opcional por status
     * @param criticidade filtro opcional por criticidade
     * @param page        número da página (0-indexed, padrão 0)
     * @param size        tamanho da página (padrão 20)
     * @return 200 OK com página de tarefas
     */
    @GetMapping
    public ResponseEntity<Page<TaskResponse>> findAll(
            @RequestParam(required = false) StatusTarefa status,
            @RequestParam(required = false) Criticidade criticidade,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "scorePrioridade")
                        .and(Sort.by(Sort.Direction.ASC, "prazo")));

        return ResponseEntity.ok(taskService.findAll(status, criticidade, pageable));
    }

    /**
     * Retorna uma tarefa pelo identificador.
     *
     * @return 200 OK com a tarefa, ou 404 se não encontrada
     */
    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(taskService.findById(id));
    }

    /**
     * Atualiza uma tarefa existente e recalcula a prioridade.
     *
     * @return 200 OK com a tarefa atualizada
     */
    @PutMapping("/{id}")
    public ResponseEntity<TaskResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody TaskRequest request) {
        return ResponseEntity.ok(taskService.update(id, request));
    }

    /**
     * Realiza a transição de status de uma tarefa conforme as regras da máquina de estados.
     *
     * <p>Transições válidas:
     * <ul>
     *   <li>PENDENTE → EM_ANDAMENTO, CONCLUIDA, CANCELADA</li>
     *   <li>EM_ANDAMENTO → CONCLUIDA, CANCELADA</li>
     * </ul>
     *
     * @return 200 OK com a tarefa atualizada, 404 se não encontrada, 422 se transição inválida
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<TaskResponse> transition(
            @PathVariable UUID id,
            @Valid @RequestBody TransitionStatusRequest request) {
        return ResponseEntity.ok(taskService.transition(id, request));
    }

    /**
     * Remove permanentemente uma tarefa.
     *
     * @return 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        taskService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
