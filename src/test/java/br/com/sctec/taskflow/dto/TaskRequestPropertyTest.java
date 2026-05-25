package br.com.sctec.taskflow.dto;

import br.com.sctec.taskflow.domain.enums.Criticidade;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import net.jqwik.api.*;
import org.junit.jupiter.api.BeforeAll;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests para validação de entrada do {@link TaskRequest} — tasks 13.1 e 13.2.
 *
 * <p>Estratégia: usa o {@link Validator} do Jakarta Bean Validation diretamente,
 * sem Spring context. Cada property gera 100 amostras aleatórias e verifica que
 * a constraint é sempre violada para entradas inválidas.</p>
 *
 * <ul>
 *   <li>Property 8 (13.1): Título em branco é sempre rejeitado — {@code @NotBlank}</li>
 *   <li>Property 9 (13.2): Prazo passado é sempre rejeitado — {@code @FutureOrPresent}</li>
 * </ul>
 *
 * <p><strong>Validates: Requirements 1.3, 1.4, 3.4</strong></p>
 */
class TaskRequestPropertyTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    // =========================================================================
    // Property 8 — Título em branco é sempre rejeitado
    // Validates: Requirement 1.3
    // =========================================================================

    /**
     * Para qualquer string em branco (nula, vazia ou só espaços), a validação
     * do campo {@code titulo} deve sempre produzir pelo menos uma violação.
     */
    @Property(tries = 100)
    @Label("Property 8: Titulo em branco e sempre rejeitado na criacao")
    void tituloBrancoESempreRejeitado(@ForAll("titulosBrancos") String titulo) {
        TaskRequest request = new TaskRequest(
                titulo,
                "Descrição válida",
                LocalDate.now().plusDays(10),
                Criticidade.MEDIA
        );

        Set<ConstraintViolation<TaskRequest>> violations = validator.validate(request);

        assertThat(violations)
                .as("Titulo em branco deve gerar violacao de @NotBlank")
                .anyMatch(v -> v.getPropertyPath().toString().equals("titulo"));
    }

    /**
     * Título nulo também deve ser rejeitado pelo {@code @NotBlank}.
     */
    @Property(tries = 1)
    @Label("Property 8b: Titulo nulo e sempre rejeitado")
    void tituloNuloESempreRejeitado() {
        TaskRequest request = new TaskRequest(
                null,
                "Descrição válida",
                LocalDate.now().plusDays(10),
                Criticidade.MEDIA
        );

        Set<ConstraintViolation<TaskRequest>> violations = validator.validate(request);

        assertThat(violations)
                .as("Titulo nulo deve gerar violacao de @NotBlank")
                .anyMatch(v -> v.getPropertyPath().toString().equals("titulo"));
    }

    // =========================================================================
    // Property 9 — Prazo passado é sempre rejeitado
    // Validates: Requirements 1.4, 3.4
    // =========================================================================

    /**
     * Para qualquer data estritamente no passado, a validação do campo
     * {@code prazo} deve sempre produzir pelo menos uma violação.
     */
    @Property(tries = 100)
    @Label("Property 9: Prazo passado e sempre rejeitado")
    void prazoPassadoESempreRejeitado(@ForAll("datesNoPassado") LocalDate prazo) {
        TaskRequest request = new TaskRequest(
                "Título válido",
                "Descrição válida",
                prazo,
                Criticidade.MEDIA
        );

        Set<ConstraintViolation<TaskRequest>> violations = validator.validate(request);

        assertThat(violations)
                .as("Prazo no passado deve gerar violacao de @FutureOrPresent")
                .anyMatch(v -> v.getPropertyPath().toString().equals("prazo"));
    }

    /**
     * Prazo nulo deve ser rejeitado pelo {@code @NotNull}.
     */
    @Property(tries = 1)
    @Label("Property 9b: Prazo nulo e sempre rejeitado")
    void prazoNuloESempreRejeitado() {
        TaskRequest request = new TaskRequest(
                "Título válido",
                "Descrição válida",
                null,
                Criticidade.MEDIA
        );

        Set<ConstraintViolation<TaskRequest>> violations = validator.validate(request);

        assertThat(violations)
                .as("Prazo nulo deve gerar violacao de @NotNull")
                .anyMatch(v -> v.getPropertyPath().toString().equals("prazo"));
    }

    /**
     * Request completamente válido não deve gerar nenhuma violação.
     * Garante que os providers não estão gerando falsos positivos.
     */
    @Property(tries = 1)
    @Label("Sanidade: request valido nao deve gerar violacoes")
    void requestValidoNaoGeraViolacoes() {
        TaskRequest request = new TaskRequest(
                "Título válido",
                "Descrição",
                LocalDate.now().plusDays(5),
                Criticidade.ALTA
        );

        Set<ConstraintViolation<TaskRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    // =========================================================================
    // Providers de dados arbitrários
    // =========================================================================

    /**
     * Gera strings em branco: vazia ou composta apenas por espaços/tabs/newlines.
     * {@code @NotBlank} rejeita null, "" e strings com apenas whitespace.
     */
    @Provide
    Arbitrary<String> titulosBrancos() {
        // Gera strings compostas apenas por caracteres de espaço em branco (0 a 20 chars)
        Arbitrary<String> apenasEspacos = Arbitraries.strings()
                .withChars(' ', '\t', '\n', '\r')
                .ofMinLength(0)
                .ofMaxLength(20);

        // String vazia
        Arbitrary<String> vazia = Arbitraries.just("");

        return Arbitraries.oneOf(apenasEspacos, vazia);
    }

    /**
     * Gera datas estritamente no passado (1 a 3650 dias antes de hoje).
     */
    @Provide
    Arbitrary<LocalDate> datesNoPassado() {
        return Arbitraries.longs()
                .between(1L, 3650L)
                .map(dias -> LocalDate.now().minusDays(dias));
    }
}
