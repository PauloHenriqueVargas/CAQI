-- Fase 9.B — MFA TOTP (RFC 6238).
--
-- Cada usuário pode opcionalmente habilitar segundo fator (TOTP via app
-- como Google Authenticator, Aegis, 1Password, etc.). O secret é
-- armazenado em base32 (compatível com a maioria dos apps).
--
-- O fluxo completo de login com MFA é fora do escopo MVP — esta migration
-- + service + endpoints disponibilizam a infraestrutura. Fase 9.B
-- completa: integrar com NextAuth para exigir o código no callback do
-- CredentialsProvider (após validar usuário/senha contra Basic Auth do
-- backend).

CREATE TABLE IF NOT EXISTS usuario_mfa (
  username        VARCHAR(64)  PRIMARY KEY,
  secret_base32   VARCHAR(64)  NOT NULL,
  enabled         BOOLEAN      NOT NULL DEFAULT FALSE,
  created_at      TIMESTAMP    NOT NULL DEFAULT now(),
  enabled_at      TIMESTAMP,
  ultima_validacao TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_usuario_mfa_enabled ON usuario_mfa(enabled);

COMMENT ON TABLE  usuario_mfa IS 'Segredo TOTP por usuário — RFC 6238. Pre-condição para MFA na Fase 9.B completa.';
COMMENT ON COLUMN usuario_mfa.secret_base32 IS 'Base32 do secret de 160 bits (20 bytes) — compatível com Google Authenticator e similares.';
COMMENT ON COLUMN usuario_mfa.enabled IS 'False = setup iniciado mas usuário ainda não validou primeiro código; True = MFA ativo.';
