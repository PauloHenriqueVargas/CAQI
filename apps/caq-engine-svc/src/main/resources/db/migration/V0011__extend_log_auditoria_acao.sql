-- V0011 — Estende log_auditoria.acao
-- Cleanup descoberto pela camada analítica (Sprint 5.C): a Fase 6.C
-- introduziu valores como "publicar:fundeb_execucao" (24 chars) e
-- "executar_lote_manual:admin" (>10 chars) que estouravam o VARCHAR(10)
-- original definido na V0001.
--
-- ALTER TABLE não dispara o trigger de proteção da V0006 (que cobre
-- apenas UPDATE/DELETE/TRUNCATE) e o role caqi é owner da tabela,
-- portanto pode reestruturar.

BEGIN;
ALTER TABLE log_auditoria ALTER COLUMN acao TYPE VARCHAR(60);
COMMIT;
