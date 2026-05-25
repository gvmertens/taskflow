-- ============================================================
-- TaskFlow API — Migration V2
-- Cria a tabela 'tasks' mapeada pela entidade Task (JPA)
-- Nota: V1 criou 'tarefas' (nome legado); esta migration cria
--       a tabela com o nome correto usado pela camada de domínio.
-- ============================================================

CREATE TABLE IF NOT EXISTS tasks (
    id               UUID         NOT NULL DEFAULT gen_random_uuid(),
    titulo           VARCHAR(255) NOT NULL,
    descricao        TEXT,
    prazo            DATE         NOT NULL,
    criticidade      VARCHAR(10)  NOT NULL,
    status           VARCHAR(15)  NOT NULL DEFAULT 'PENDENTE',
    score_prioridade INTEGER      NOT NULL DEFAULT 0,
    concluida_em     TIMESTAMPTZ,
    criado_em        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    atualizado_em    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_tasks          PRIMARY KEY (id),
    CONSTRAINT chk_tasks_criticidade CHECK (criticidade IN ('BAIXA', 'MEDIA', 'ALTA', 'URGENTE')),
    CONSTRAINT chk_tasks_status      CHECK (status IN ('PENDENTE', 'EM_ANDAMENTO', 'CONCLUIDA', 'CANCELADA')),
    CONSTRAINT chk_tasks_score       CHECK (score_prioridade BETWEEN 0 AND 100)
);

-- Índices para filtros e ordenação padrão
CREATE INDEX IF NOT EXISTS idx_tasks_status       ON tasks (status);
CREATE INDEX IF NOT EXISTS idx_tasks_criticidade  ON tasks (criticidade);
CREATE INDEX IF NOT EXISTS idx_tasks_score_prazo  ON tasks (score_prioridade DESC, prazo ASC);
