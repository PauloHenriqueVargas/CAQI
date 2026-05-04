-- Fase 9.B (parcial 4) — Backup codes para recuperação de MFA.
--
-- Adiciona JSONB array de SHA-256 hashes dos códigos de recuperação. Códigos
-- são one-time-use (removidos do array ao serem consumidos). Geração:
--   - 8 códigos por usuário, 10 chars alfanuméricos uppercase
--   - Conjunto reduzido (sem 0/O/1/I — ambíguos visualmente)
--   - Entropia ≈ 50 bits por código (32^10 = 1.13e15 possibilidades)
-- Hash SHA-256 sem salt é suficiente — a entropia do código é alta.
-- Apresentados ao usuário UMA ÚNICA VEZ no enable() ou regenerate().

ALTER TABLE usuario_mfa
  ADD COLUMN IF NOT EXISTS backup_codes_hashes JSONB NOT NULL DEFAULT '[]'::jsonb;

ALTER TABLE usuario_mfa
  ADD COLUMN IF NOT EXISTS backup_codes_generated_at TIMESTAMP;

COMMENT ON COLUMN usuario_mfa.backup_codes_hashes IS
  'Array JSON de SHA-256 hex dos backup codes ainda válidos. Cada uso remove o hash do array.';
