-- ============================================================
-- TaskFlow API — Migration V1
-- Cria a tabela principal de tarefas com constraints e índices
-- Requirements: 8.1, 8.2, 8.3, 8.4
-- ============================================================

CREATE TABLE tarefas (
    id               UUID         NOT NULL DEFAULT gen_random_uuid(),
    titulo           VARCHAR(255) NOT NULL,
    descricao        TEXT,
    prazo            TIMESTAMPTZ  NOT NULL,
    criticidade      VARCHAR(10)  NOT NULL,
    status           VARCHAR(15)  NOT NULL DEFAULT 'PENDENTE',
    score_prioridade INTEGER      NOT NULL DEFAULT 0,
    criado_em        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    atualizado_em    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    concluido_em     TIMESTAMPTZ,

    CONSTRAINT pk_tarefas      PRIMARY KEY (id),
    CONSTRAINT chk_criticidade CHECK (criticidade IN ('BAIXA', 'MEDIA', 'ALTA', 'URGENTE')),
    CONSTRAINT chk_status      CHECK (status IN ('PENDENTE', 'EM_ANDAMENTO', 'CONCLUIDA', 'CANCELADA')),
    CONSTRAINT chk_score       CHECK (score_prioridade BETWEEN 0 AND 100)
);

-- Índice para filtro por status (GET /api/v1/tasks?status=...)
CREATE INDEX idx_tarefas_status      ON tarefas (status);

-- Índice para filtro por criticidade (GET /api/v1/tasks?criticidade=...)
CREATE INDEX idx_tarefas_criticidade ON tarefas (criticidade);

-- Índice composto para ordenação padrão da listagem: score DESC, prazo ASC
CREATE INDEX idx_tarefas_score_prazo ON tarefas (score_prioridade DESC, prazo ASC);
