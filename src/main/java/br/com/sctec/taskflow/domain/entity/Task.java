package br.com.sctec.taskflow.domain.entity;

import br.com.sctec.taskflow.domain.enums.Criticidade;
import br.com.sctec.taskflow.domain.enums.Status;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "tasks")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Task {

    @Id
    @UuidGenerator
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(nullable = false)
    private LocalDate prazo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Criticidade criticidade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Status status = Status.PENDENTE;

    @Column(nullable = false)
    @Builder.Default
    private Integer scorePrioridade = 0;

    /** Instante em que a tarefa foi concluída (nulo enquanto não encerrada). */
    private Instant concluidaEm;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant criadoEm;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant atualizadoEm;
}
