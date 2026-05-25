package br.com.sctec.taskflow.domain.model;

import br.com.sctec.taskflow.domain.enums.Criticidade;
import br.com.sctec.taskflow.domain.enums.StatusTarefa;

import java.time.Instant;
import java.util.UUID;

/**
 * Entidade de domínio pura representando uma Tarefa.
 * <p>
 * Esta classe é um POJO sem dependências de framework (sem JPA, sem Lombok,
 * sem Spring). A persistência é responsabilidade da camada de infraestrutura
 * via {@code TarefaEntity}.
 * </p>
 *
 * <p>Campos nullable: {@code descricao} e {@code concluidoEm}.</p>
 */
public class Tarefa {

    private UUID id;
    private String titulo;
    private String descricao;          // nullable
    private Instant prazo;
    private Criticidade criticidade;
    private StatusTarefa status;
    private int scorePrioridade;
    private Instant criadoEm;
    private Instant atualizadoEm;
    private Instant concluidoEm;       // nullable, preenchido ao concluir

    // -------------------------------------------------------------------------
    // Construtores
    // -------------------------------------------------------------------------

    public Tarefa() {
    }

    public Tarefa(UUID id,
                  String titulo,
                  String descricao,
                  Instant prazo,
                  Criticidade criticidade,
                  StatusTarefa status,
                  int scorePrioridade,
                  Instant criadoEm,
                  Instant atualizadoEm,
                  Instant concluidoEm) {
        this.id = id;
        this.titulo = titulo;
        this.descricao = descricao;
        this.prazo = prazo;
        this.criticidade = criticidade;
        this.status = status;
        this.scorePrioridade = scorePrioridade;
        this.criadoEm = criadoEm;
        this.atualizadoEm = atualizadoEm;
        this.concluidoEm = concluidoEm;
    }

    // -------------------------------------------------------------------------
    // Getters e Setters
    // -------------------------------------------------------------------------

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public Instant getPrazo() {
        return prazo;
    }

    public void setPrazo(Instant prazo) {
        this.prazo = prazo;
    }

    public Criticidade getCriticidade() {
        return criticidade;
    }

    public void setCriticidade(Criticidade criticidade) {
        this.criticidade = criticidade;
    }

    public StatusTarefa getStatus() {
        return status;
    }

    public void setStatus(StatusTarefa status) {
        this.status = status;
    }

    public int getScorePrioridade() {
        return scorePrioridade;
    }

    public void setScorePrioridade(int scorePrioridade) {
        this.scorePrioridade = scorePrioridade;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(Instant criadoEm) {
        this.criadoEm = criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(Instant atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }

    public Instant getConcluidoEm() {
        return concluidoEm;
    }

    public void setConcluidoEm(Instant concluidoEm) {
        this.concluidoEm = concluidoEm;
    }

    // -------------------------------------------------------------------------
    // Object overrides
    // -------------------------------------------------------------------------

    @Override
    public String toString() {
        return "Tarefa{" +
                "id=" + id +
                ", titulo='" + titulo + '\'' +
                ", criticidade=" + criticidade +
                ", status=" + status +
                ", scorePrioridade=" + scorePrioridade +
                ", prazo=" + prazo +
                ", criadoEm=" + criadoEm +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tarefa tarefa = (Tarefa) o;
        return id != null && id.equals(tarefa.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
