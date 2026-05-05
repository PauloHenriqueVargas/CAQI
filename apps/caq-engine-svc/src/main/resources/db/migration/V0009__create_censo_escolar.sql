-- V0009 — Importação de Censo Escolar/INEP
-- Microdados publicados anualmente pelo INEP (Lei 9.394/96 art. 5°§1° + LDB).
-- Layout ESCOLAS_*.csv (Latin-1, ';'-separated): NU_ANO_CENSO, CO_ENTIDADE,
-- NO_ENTIDADE, CO_MUNICIPIO, TP_DEPENDENCIA (3=Municipal), TP_LOCALIZACAO
-- (1=Urbana, 2=Rural), QT_MAT_INF_CRE/PRE, QT_MAT_FUND_AI/AF, QT_MAT_MED, etc.

BEGIN;

-- ─────────────────────────────────────────────────────────────────────
-- censo_importacao: trilha de auditoria de cada import
-- (idempotência por hash SHA-256 do arquivo + ano_censo)
-- ─────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS censo_importacao (
  importacao_id          BIGSERIAL PRIMARY KEY,
  ano_censo              INTEGER     NOT NULL,
  arquivo_nome           TEXT        NOT NULL,
  arquivo_hash_sha256    CHAR(64)    NOT NULL,
  arquivo_tamanho_bytes  BIGINT,
  cod_municipio_ibge     CHAR(7)     NOT NULL,
  registros_processados  INTEGER     NOT NULL DEFAULT 0,
  registros_municipio    INTEGER     NOT NULL DEFAULT 0,
  escolas_inseridas      INTEGER     NOT NULL DEFAULT 0,
  escolas_atualizadas    INTEGER     NOT NULL DEFAULT 0,
  matriculas_total       INTEGER     NOT NULL DEFAULT 0,
  status                 VARCHAR(20) NOT NULL CHECK (status IN ('em_andamento','concluida','falhou','simulada')),
  erro_mensagem          TEXT,
  criado_em              TIMESTAMP   NOT NULL DEFAULT now(),
  criado_por             VARCHAR(60) NOT NULL,
  CONSTRAINT uq_censo_importacao UNIQUE (ano_censo, arquivo_hash_sha256, cod_municipio_ibge)
);

CREATE INDEX IF NOT EXISTS ix_censo_importacao_ano
  ON censo_importacao (ano_censo, criado_em DESC);

-- ─────────────────────────────────────────────────────────────────────
-- censo_matricula_resumo: agregação de matrículas por (escola, etapa, ano)
-- A tabela operacional `matricula` é por aluno; o Censo só fornece
-- contagens agregadas. Mantemos esta tabela separada para preservar a
-- procedência (microdados oficiais INEP) sem misturar com dados
-- operacionais municipais (SED/SIGE futuros).
-- ─────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS censo_matricula_resumo (
  resumo_id              BIGSERIAL PRIMARY KEY,
  importacao_id          BIGINT      NOT NULL REFERENCES censo_importacao(importacao_id) ON DELETE CASCADE,
  ano_censo              INTEGER     NOT NULL,
  escola_inep            VARCHAR(20) NOT NULL,
  escola_id              BIGINT      REFERENCES escola(escola_id) ON DELETE SET NULL,
  etapa_codigo           VARCHAR(30) NOT NULL,
  qtd_alunos             INTEGER     NOT NULL CHECK (qtd_alunos >= 0),
  CONSTRAINT uq_censo_matricula_resumo UNIQUE (ano_censo, escola_inep, etapa_codigo, importacao_id)
);

CREATE INDEX IF NOT EXISTS ix_censo_matricula_ano_etapa
  ON censo_matricula_resumo (ano_censo, etapa_codigo);

CREATE INDEX IF NOT EXISTS ix_censo_matricula_escola
  ON censo_matricula_resumo (escola_id);

-- ─────────────────────────────────────────────────────────────────────
-- Reforça que escola.inep_id é único quando presente (tinha sido NULL-able
-- na V0001 sem unique). Censo usa CO_ENTIDADE como natural key.
-- ─────────────────────────────────────────────────────────────────────
CREATE UNIQUE INDEX IF NOT EXISTS uq_escola_inep_id
  ON escola (inep_id) WHERE inep_id IS NOT NULL;

COMMIT;
