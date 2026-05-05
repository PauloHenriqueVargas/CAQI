-- V0010 — Publicação automática LRF art. 48-A
-- Estende publicacao_portal (V0001) para suportar:
--  - referência temporal (ano ou ano+mês)
--  - snapshot JSONB do conteúdo no momento da publicação (audit trail
--    público — cidadão pode pedir o histórico via SIC)
--  - dedup natural por (tipo, referencia, conteudo_hash)
-- LRF art. 48-A exige disponibilização da execução orçamentária em
-- tempo real (até 24h após executado). O scheduler interno publica
-- snapshots quando o conteúdo muda; conteúdo idêntico ao anterior
-- não gera nova entrada.

BEGIN;

ALTER TABLE publicacao_portal
  ADD COLUMN IF NOT EXISTS referencia    VARCHAR(20),
  ADD COLUMN IF NOT EXISTS tamanho_bytes BIGINT,
  ADD COLUMN IF NOT EXISTS snapshot      JSONB;

-- Dedup: a mesma (tipo, referencia) com mesmo hash não é re-publicada
CREATE UNIQUE INDEX IF NOT EXISTS uq_publicacao_tipo_ref_hash
  ON publicacao_portal (tipo, referencia, conteudo_hash)
  WHERE conteudo_hash IS NOT NULL AND referencia IS NOT NULL;

-- Listagem ordenada por tipo + recência
CREATE INDEX IF NOT EXISTS ix_publicacao_tipo_data
  ON publicacao_portal (tipo, data_publicacao DESC);

CREATE INDEX IF NOT EXISTS ix_publicacao_referencia
  ON publicacao_portal (referencia);

COMMIT;
