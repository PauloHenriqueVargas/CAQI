-- Fase 3.B — tabela notificacao para o caq-compliance-svc.
-- Notificações são geradas pelo AvaliadorComplianceService quando regras
-- legais (MDE 25%, Fundeb 70%, VAAT 15%, VAAR) são violadas ou se aproximam
-- de gap crítico. Ficam abertas até resolução pelo gestor.

CREATE TABLE IF NOT EXISTS notificacao (
  notificacao_id   BIGSERIAL PRIMARY KEY,
  tipo             VARCHAR(60) NOT NULL,        -- VIOLACAO_MDE_25, VIOLACAO_FUNDEB_70, VIOLACAO_VAAT_15, GAP_CAQ_ALTO, etc.
  severidade       VARCHAR(20) NOT NULL,        -- info | warn | alta | critica
  titulo           TEXT NOT NULL,
  descricao        TEXT,
  ano_referencia   INTEGER NOT NULL,
  base_legal       TEXT,                        -- ex.: "Lei 14.113/2020 art. 26"
  payload_json     JSONB,                       -- detalhes para auditoria (valores, percentuais, gap)
  status           VARCHAR(20) NOT NULL DEFAULT 'aberta',
  created_at       TIMESTAMP   DEFAULT now(),
  resolved_at      TIMESTAMP,
  CONSTRAINT notificacao_severidade_check CHECK (severidade IN ('info','warn','alta','critica')),
  CONSTRAINT notificacao_status_check     CHECK (status IN ('aberta','em_analise','resolvida','ignorada'))
);

CREATE INDEX IF NOT EXISTS idx_notificacao_status_ano ON notificacao(status, ano_referencia);
CREATE INDEX IF NOT EXISTS idx_notificacao_tipo       ON notificacao(tipo);
CREATE INDEX IF NOT EXISTS idx_notificacao_created    ON notificacao(created_at DESC);

-- log_auditoria: a tabela já existe em V0001. Para o chain SHA-256 (Merkle),
-- adicionamos um índice cronológico (FOR UPDATE por id mais recente).
CREATE INDEX IF NOT EXISTS idx_log_auditoria_id_desc ON log_auditoria(log_id DESC);
