-- Fase 2.B — distinção CAQi (mínimo) vs CAQ (adequado).
--
-- O CAQ é a meta de qualidade adequada (PNE/Meta 20); o CAQi é o piso
-- mínimo a ser garantido. Os custos por insumo divergem: ex. um professor
-- "adequado" pode requerer salário superior ao mínimo, materiais melhores,
-- ratio aluno/professor menor etc. Cada custo_insumo passa a ter um
-- 'perfil' associado.

ALTER TABLE custo_insumo
  ADD COLUMN IF NOT EXISTS perfil VARCHAR(20) NOT NULL DEFAULT 'minimo';

ALTER TABLE custo_insumo
  DROP CONSTRAINT IF EXISTS custo_insumo_perfil_check;

ALTER TABLE custo_insumo
  ADD CONSTRAINT custo_insumo_perfil_check
  CHECK (perfil IN ('minimo','adequado'));

-- Índice para a busca por (insumo, vigência, perfil) — caminho quente do motor.
DROP INDEX IF EXISTS idx_custo_insumo_vigencia;
CREATE INDEX IF NOT EXISTS idx_custo_insumo_vig_perfil
  ON custo_insumo(insumo_id, perfil, vigencia_inicio DESC);
