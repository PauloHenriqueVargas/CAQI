-- SEED — APENAS DEV/TEST.
-- Conjunto financeiro mínimo para validar FundebService (caq-financeiro-svc) com
-- todos os 3 percentuais (MDE 25% / Fundeb 70% / VAAT 15%) ACIMA do mínimo.

BEGIN;

-- ─────────────── Fontes de recurso ───────────────
INSERT INTO fonte_recurso (tipo, descricao) VALUES ('Propria', 'Recursos próprios do município') ON CONFLICT DO NOTHING;
INSERT INTO fonte_recurso (tipo, descricao) VALUES ('VAAF',    'Fundeb — VAAF (cota-parte)')      ON CONFLICT DO NOTHING;
INSERT INTO fonte_recurso (tipo, descricao) VALUES ('VAAT',    'Fundeb — Complementação VAAT')    ON CONFLICT DO NOTHING;
INSERT INTO fonte_recurso (tipo, descricao) VALUES ('VAAR',    'Fundeb — Complementação VAAR')    ON CONFLICT DO NOTHING;
INSERT INTO fonte_recurso (tipo, descricao) VALUES ('Outras',  'Outras fontes')                   ON CONFLICT DO NOTHING;

-- ─────────────── Receitas (ano 2025) ───────────────
-- MDE base = impostos + transferencias = 180.000
INSERT INTO receita (competencia, valor, origem, pcasp) VALUES ('202503', 100000, 'impostos',     '1.7.1.0.00.00');
INSERT INTO receita (competencia, valor, origem, pcasp) VALUES ('202504',  80000, 'transferencias','1.7.2.0.00.00');
-- Fundeb total = 280.000
INSERT INTO receita (competencia, valor, origem, pcasp) VALUES ('202505', 200000, 'Fundeb_VAAF',  '1.7.5.1.00.00');
INSERT INTO receita (competencia, valor, origem, pcasp) VALUES ('202506',  50000, 'Fundeb_VAAT',  '1.7.5.2.00.00');
INSERT INTO receita (competencia, valor, origem, pcasp) VALUES ('202507',  30000, 'Fundeb_VAAR',  '1.7.5.3.00.00');

-- ─────────────── Despesas (ano 2025) ───────────────
-- D1: 200k pessoal docente Fundeb VAAF → conta 70% pessoal Fundeb
INSERT INTO despesa (competencia, natureza, valor, fonte_recurso_id, pcasp, siope_grupo)
SELECT '202506', '3.1.90.11', 200000, fr.fonte_recurso_id, '3.1.90.11.00', 'MDE'
FROM fonte_recurso fr WHERE fr.tipo = 'VAAF';

-- D2: 30k pessoal coordenador Fundeb VAAR → conta 70% pessoal Fundeb
INSERT INTO despesa (competencia, natureza, valor, fonte_recurso_id, pcasp, siope_grupo)
SELECT '202507', '3.1.90.11', 30000, fr.fonte_recurso_id, '3.1.90.11.00', 'MDE'
FROM fonte_recurso fr WHERE fr.tipo = 'VAAR';

-- D3: 20k material consumo MDE Próprio
INSERT INTO despesa (competencia, natureza, valor, fonte_recurso_id, pcasp, siope_grupo)
SELECT '202508', '3.3.90.30', 20000, fr.fonte_recurso_id, '3.3.90.30.00', 'MDE'
FROM fonte_recurso fr WHERE fr.tipo = 'Propria';

-- D4: 15k serviços terceiros MDE Próprio
INSERT INTO despesa (competencia, natureza, valor, fonte_recurso_id, pcasp, siope_grupo)
SELECT '202509', '3.3.90.39', 15000, fr.fonte_recurso_id, '3.3.90.39.00', 'MDE'
FROM fonte_recurso fr WHERE fr.tipo = 'Propria';

-- D5: 10k equipamentos VAAT (capital) → conta 15% VAAT capital
INSERT INTO despesa (competencia, natureza, valor, fonte_recurso_id, pcasp, siope_grupo)
SELECT '202510', '4.4.90.52', 10000, fr.fonte_recurso_id, '4.4.90.52.00', 'MDE'
FROM fonte_recurso fr WHERE fr.tipo = 'VAAT';

-- D6: 5k obras VAAT (capital) → conta 15% VAAT capital
INSERT INTO despesa (competencia, natureza, valor, fonte_recurso_id, pcasp, siope_grupo)
SELECT '202511', '4.4.90.51', 5000, fr.fonte_recurso_id, '4.4.90.51.00', 'MDE'
FROM fonte_recurso fr WHERE fr.tipo = 'VAAT';

-- Verificação esperada:
--   MDE      = 280.000 / 180.000 = 155,56% ≥ 25%   ✓
--   Fundeb70 = 230.000 / 280.000 = 82,14%  ≥ 70%   ✓
--   VAAT15   =  15.000 /  50.000 = 30,00%  ≥ 15%   ✓

COMMIT;
