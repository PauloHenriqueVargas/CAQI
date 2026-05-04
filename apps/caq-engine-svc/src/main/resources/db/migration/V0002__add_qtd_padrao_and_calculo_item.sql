-- Fase 2 — extensões para suportar o motor CAQ/CAQi.
--
-- 1) Adiciona qtd_padrao em insumo (multiplicador da unidade — ex.: 12 meses, 15 unidades)
-- 2) Cria calculo_caq_item (memória de cálculo: 1 linha por insumo aplicado num calculo)

ALTER TABLE insumo
  ADD COLUMN IF NOT EXISTS qtd_padrao NUMERIC(12,4) NOT NULL DEFAULT 1;

CREATE TABLE IF NOT EXISTS calculo_caq_item (
  item_id           BIGSERIAL PRIMARY KEY,
  calculo_id        BIGINT       NOT NULL REFERENCES calculo_caq(calculo_id) ON DELETE CASCADE,
  insumo_id         BIGINT       NOT NULL REFERENCES insumo(insumo_id),
  insumo_codigo     VARCHAR(30)  NOT NULL,
  insumo_nome       TEXT         NOT NULL,
  tipo_aplicacao    VARCHAR(15)  NOT NULL,
  qtd_aplicada      NUMERIC(12,4) NOT NULL,
  custo_unitario    NUMERIC(14,2) NOT NULL,
  custo_anual       NUMERIC(14,4) NOT NULL,        -- qtd × custo_unitario
  divisor           NUMERIC(14,4) NOT NULL,        -- 1 / alunos_por_turma / total_alunos_escola
  custo_aluno_ano   NUMERIC(14,4) NOT NULL,        -- custo_anual / divisor
  base_calculo      TEXT          NOT NULL,        -- explicação textual auditável
  created_at        TIMESTAMP     DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_calculo_caq_item_calculo ON calculo_caq_item(calculo_id);
CREATE INDEX IF NOT EXISTS idx_calculo_caq_item_insumo  ON calculo_caq_item(insumo_id);

-- Garantir que custo_insumo permite buscar por vigência
CREATE INDEX IF NOT EXISTS idx_custo_insumo_vigencia
  ON custo_insumo(insumo_id, vigencia_inicio DESC);

CREATE INDEX IF NOT EXISTS idx_matricula_escola_etapa_situacao
  ON matricula(escola_id, etapa_id, situacao);

CREATE INDEX IF NOT EXISTS idx_parametro_etapa_vigencia
  ON parametro_etapa(etapa_id, vigencia_inicio DESC);
