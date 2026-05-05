-- V0012 — Censo Escolar/INEP — microdados aluno-level (MATRICULA_*.csv)
--
-- Sprint 5.D: complementa V0009 (que importa ESCOLAS.csv com agregados)
-- com os microdados por matrícula, permitindo análises demográficas
-- (cobertura por cor/raça, NEE, distribuição etária por etapa).
--
-- LGPD: NÃO armazenamos nome, CPF, data de nascimento ou identificadores
-- diretos. ID_ALUNO INEP é pseudonimizado via SHA-256 com salt por
-- tenant (env CAQI_CENSO_PSEUDONIMIZACAO_SALT). DPIA atualizado em
-- docs/legal/DPIA_RIPD.md §3.2.

BEGIN;

CREATE TABLE IF NOT EXISTS censo_matricula (
  matricula_censo_id  BIGSERIAL PRIMARY KEY,
  importacao_id       BIGINT      NOT NULL REFERENCES censo_importacao(importacao_id) ON DELETE CASCADE,
  ano_censo           INTEGER     NOT NULL,
  id_matricula_inep   VARCHAR(20) NOT NULL,            -- INEP ID_MATRICULA — público
  id_aluno_hash       CHAR(64)    NOT NULL,            -- SHA-256(ID_ALUNO || salt) — pseudonimizado
  escola_inep         VARCHAR(20) NOT NULL,
  escola_id           BIGINT      REFERENCES escola(escola_id) ON DELETE SET NULL,
  etapa_codigo        VARCHAR(30) NOT NULL,            -- CRECHE/PRE/EF1/EF2/EM/EJA/PROF (mapeado de TP_ETAPA_ENSINO)
  tp_etapa_ensino     SMALLINT,                        -- código original INEP (1-77)
  idade               SMALLINT,                        -- NU_IDADE_REFERENCIA (calculada pelo INEP)
  tp_sexo             SMALLINT,                        -- 1=M, 2=F (INEP)
  tp_cor_raca         SMALLINT,                        -- 0=não declarada, 1=branca, 2=preta, 3=parda, 4=amarela, 5=indígena
  tp_zona_residencial SMALLINT,                        -- 1=urbana, 2=rural
  in_necessidade_especial BOOLEAN NOT NULL DEFAULT FALSE,
  necessidades_codigos    TEXT,                        -- CSV de IN_*: 'CEGUEIRA,SURDEZ,DEFICIENCIA_INTELECTUAL', etc.
  CONSTRAINT uq_censo_matricula UNIQUE (importacao_id, id_matricula_inep)
);

-- Índices voltados a marts demográficos
CREATE INDEX IF NOT EXISTS ix_censo_matricula_escola
  ON censo_matricula (escola_id);

CREATE INDEX IF NOT EXISTS ix_censo_matricula_ano_etapa
  ON censo_matricula (ano_censo, etapa_codigo);

CREATE INDEX IF NOT EXISTS ix_censo_matricula_demografia
  ON censo_matricula (ano_censo, tp_cor_raca, tp_sexo);

CREATE INDEX IF NOT EXISTS ix_censo_matricula_nee
  ON censo_matricula (ano_censo, in_necessidade_especial)
  WHERE in_necessidade_especial = TRUE;

-- Estende censo_importacao para contadores específicos do MATRICULA
ALTER TABLE censo_importacao
  ADD COLUMN IF NOT EXISTS subtipo_arquivo VARCHAR(20),
  ADD COLUMN IF NOT EXISTS matriculas_inseridas INTEGER NOT NULL DEFAULT 0;
-- subtipo_arquivo: 'ESCOLAS' (V0009) ou 'MATRICULA' (V0012). Default NULL para
-- não quebrar imports antigos que já presumem ESCOLAS.

COMMIT;
