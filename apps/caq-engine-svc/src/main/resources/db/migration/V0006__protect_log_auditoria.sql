-- Fase 9.B — Defesa em profundidade da cadeia imutável de auditoria.
--
-- A AuditChainService (caq-compliance-svc) garante deteção de adulteração
-- via hashes SHA-256 encadeados. Mas se um atacante com acesso direto ao
-- banco fizer UPDATE/DELETE/TRUNCATE em log_auditoria, ele pode tentar
-- recalcular toda a cadeia e camuflar. Esta migration impede essa ação no
-- nível do banco, deixando apenas INSERT permitido para a aplicação.
--
-- Camadas:
--   1. REVOKE UPDATE/DELETE/TRUNCATE do role 'caqi' (usuário da aplicação)
--   2. Trigger BEFORE que raises exception caso alguém com privilégio
--      elevado (DBA) tente alterar — força que a operação seja consciente
--      e auditada externamente
--
-- Para fins forenses ou de migração legítima, o procedimento aprovado é:
--   1. Conectar como SUPERUSER
--   2. ALTER TABLE log_auditoria DISABLE TRIGGER prevent_log_auditoria_changes;
--   3. Realizar a operação documentada
--   4. ALTER TABLE log_auditoria ENABLE TRIGGER ...
--   5. Registrar em ata + RIPD do incidente

-- ─────────────────── REVOKE de privilégios ───────────────────
REVOKE UPDATE, DELETE, TRUNCATE ON log_auditoria FROM PUBLIC;

-- O usuário 'caqi' da aplicação só pode INSERT e SELECT.
-- Ajuste o nome do role se sua instância usa outro usuário aplicacional.
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'caqi') THEN
    EXECUTE 'REVOKE UPDATE, DELETE, TRUNCATE ON log_auditoria FROM caqi';
    EXECUTE 'GRANT INSERT, SELECT ON log_auditoria TO caqi';
    EXECUTE 'GRANT USAGE, SELECT ON SEQUENCE log_auditoria_log_id_seq TO caqi';
  END IF;
END $$;

-- ─────────────────── Trigger de bloqueio ───────────────────
CREATE OR REPLACE FUNCTION block_log_auditoria_changes() RETURNS trigger AS $$
BEGIN
  RAISE EXCEPTION 'log_auditoria é append-only (cadeia SHA-256 imutável). '
                  'UPDATE/DELETE/TRUNCATE bloqueados. '
                  'Procedimento de exceção: ver docs/operacao/HARDENING_PRODUCAO.md §5.'
    USING ERRCODE = '42501'; -- insufficient_privilege
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS prevent_log_auditoria_changes ON log_auditoria;
CREATE TRIGGER prevent_log_auditoria_changes
  BEFORE UPDATE OR DELETE OR TRUNCATE ON log_auditoria
  FOR EACH STATEMENT
  EXECUTE FUNCTION block_log_auditoria_changes();

COMMENT ON TRIGGER prevent_log_auditoria_changes ON log_auditoria IS
  'Defesa em profundidade da cadeia SHA-256 — Fase 9.B (V0006). Desligar apenas '
  'sob procedimento documentado em HARDENING_PRODUCAO.md.';
