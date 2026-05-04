-- SEED — APENAS DEV/TEST.
--
-- Insere custos vigentes para perfil='adequado' (CAQ adequado) — valores
-- ILUSTRATIVOS, mais altos que o mínimo (CAQi). Em produção, esses
-- valores virão de estudos institucionais (Anped, Campanha pelo Direito
-- à Educação, Tribunal de Contas etc.) e devem refletir o "padrão
-- adequado" da Meta 20 do PNE.
--
-- Os custos do perfil='minimo' já foram inseridos em V9001 (com o default
-- 'minimo' adicionado pela V0003 ALTER COLUMN).
--
-- Markup ilustrativo (justificativa em comentários):
--   PES-001 (Professor): R$ 85.000 → R$ 130.000  (+53% — piso CAQ adequado)
--   PES-002 (Coordenador): R$ 95.000 → R$ 140.000
--   MOB-001 (Carteira): R$ 280 → R$ 480           (ergonômica/regulável)
--   MAT-001 (Kit): R$ 350 → R$ 550                (mais material e qualidade)
--   SER-001 (Limpeza): R$ 5.000 → R$ 8.000        (frequência maior)
--   TEC-001 (Internet): R$ 300 → R$ 800           (banda larga real)
--   MAN-001 (Manutenção): R$ 20.000 → R$ 35.000   (preventiva + corretiva)
--   INF-001 (Sala manut.): R$ 15.000 → R$ 25.000  (climatização, luz, acústica)
--   EQP-001 (Computador): R$ 3.500 → R$ 5.500     (mais robusto, garantia estendida)

BEGIN;

INSERT INTO custo_insumo (insumo_id, custo_unitario, fonte_preco, indice_atualizacao, vigencia_inicio, perfil)
SELECT insumo_id,  25000, 'Estudo CAQ', 'IPCA', DATE '2025-01-01', 'adequado' FROM insumo WHERE codigo='INF-001';
INSERT INTO custo_insumo (insumo_id, custo_unitario, fonte_preco, indice_atualizacao, vigencia_inicio, perfil)
SELECT insumo_id,   5500, 'Estudo CAQ', 'IPCA', DATE '2025-01-01', 'adequado' FROM insumo WHERE codigo='EQP-001';
INSERT INTO custo_insumo (insumo_id, custo_unitario, fonte_preco, indice_atualizacao, vigencia_inicio, perfil)
SELECT insumo_id,    480, 'Estudo CAQ', 'IPCA', DATE '2025-01-01', 'adequado' FROM insumo WHERE codigo='MOB-001';
INSERT INTO custo_insumo (insumo_id, custo_unitario, fonte_preco, indice_atualizacao, vigencia_inicio, perfil)
SELECT insumo_id,    550, 'Estudo CAQ', 'IPCA', DATE '2025-01-01', 'adequado' FROM insumo WHERE codigo='MAT-001';
INSERT INTO custo_insumo (insumo_id, custo_unitario, fonte_preco, indice_atualizacao, vigencia_inicio, perfil)
SELECT insumo_id,   8000, 'Estudo CAQ', 'IPCA', DATE '2025-01-01', 'adequado' FROM insumo WHERE codigo='SER-001';
INSERT INTO custo_insumo (insumo_id, custo_unitario, fonte_preco, indice_atualizacao, vigencia_inicio, perfil)
SELECT insumo_id, 130000, 'Estudo CAQ', 'IPCA', DATE '2025-01-01', 'adequado' FROM insumo WHERE codigo='PES-001';
INSERT INTO custo_insumo (insumo_id, custo_unitario, fonte_preco, indice_atualizacao, vigencia_inicio, perfil)
SELECT insumo_id, 140000, 'Estudo CAQ', 'IPCA', DATE '2025-01-01', 'adequado' FROM insumo WHERE codigo='PES-002';
INSERT INTO custo_insumo (insumo_id, custo_unitario, fonte_preco, indice_atualizacao, vigencia_inicio, perfil)
SELECT insumo_id,  35000, 'Estudo CAQ', 'IPCA', DATE '2025-01-01', 'adequado' FROM insumo WHERE codigo='MAN-001';
INSERT INTO custo_insumo (insumo_id, custo_unitario, fonte_preco, indice_atualizacao, vigencia_inicio, perfil)
SELECT insumo_id,    800, 'Estudo CAQ', 'IPCA', DATE '2025-01-01', 'adequado' FROM insumo WHERE codigo='TEC-001';

COMMIT;
