package br.com.sctec.taskflow.controller;

import br.com.sctec.taskflow.domain.exception.TarefaEncerradaException;
import br.com.sctec.taskflow.domain.exception.TransicaoInvalidaException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Tratamento centralizado de exceções para a API REST.
 *
 * <p>Retorna respostas no formato {@link ProblemDetail} (RFC 7807),
 * nativo do Spring 6+.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Recurso não encontrado → 404 Not Found.
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ProblemDetail handleNotFound(EntityNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setTitle("Recurso não encontrado");
        problem.setDetail(ex.getMessage());
        return problem;
    }

    /**
     * Tarefa encerrada não pode ser modificada → 409 Conflict.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleConflict(IllegalStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problem.setTitle("Operação inválida");
        problem.setDetail(ex.getMessage());
        return problem;
    }

    /**
     * Tentativa de transição a partir de estado terminal → 422 Unprocessable Entity.
     */
    @ExceptionHandler(TarefaEncerradaException.class)
    public ProblemDetail handleTarefaEncerrada(TarefaEncerradaException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_ENTITY);
        problem.setTitle("Tarefa encerrada");
        problem.setDetail(ex.getMessage());
        problem.setProperty("statusAtual", ex.getStatusAtual());
        return problem;
    }

    /**
     * Transição de status não permitida pelas regras de negócio → 422 Unprocessable Entity.
     */
    @ExceptionHandler(TransicaoInvalidaException.class)
    public ProblemDetail handleTransicaoInvalida(TransicaoInvalidaException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_ENTITY);
        problem.setTitle("Transição inválida");
        problem.setDetail(ex.getMessage());
        problem.setProperty("statusAtual", ex.getStatusAtual());
        problem.setProperty("statusDestino", ex.getStatusDestino());
        return problem;
    }

    /**
     * Falha de validação do Bean Validation → 400 Bad Request.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        String detalhes = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Dados inválidos");
        problem.setDetail(detalhes);
        return problem;
    }
}
