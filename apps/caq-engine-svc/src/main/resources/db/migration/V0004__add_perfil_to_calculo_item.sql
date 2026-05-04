-- Fase 2.B — distinção CAQi/CAQ na memória de cálculo.
-- Cada item passa a registrar o perfil (minimo/adequado) ao qual pertence.

ALTER TABLE calculo_caq_item
  ADD COLUMN IF NOT EXISTS perfil VARCHAR(20) NOT NULL DEFAULT 'minimo';

ALTER TABLE calculo_caq_item
  DROP CONSTRAINT IF EXISTS calculo_caq_item_perfil_check;

ALTER TABLE calculo_caq_item
  ADD CONSTRAINT calculo_caq_item_perfil_check
  CHECK (perfil IN ('minimo','adequado'));

CREATE INDEX IF NOT EXISTS idx_calculo_caq_item_perfil
  ON calculo_caq_item(calculo_id, perfil);
