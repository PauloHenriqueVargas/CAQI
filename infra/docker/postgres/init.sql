-- Inicialização do PostgreSQL para desenvolvimento local.
-- Cria schemas por contexto de microserviço (database-per-service em Fase 2).
-- O usuário 'caqi' tem privilégios em todos os schemas.

CREATE SCHEMA IF NOT EXISTS engine     AUTHORIZATION caqi;
CREATE SCHEMA IF NOT EXISTS financeiro AUTHORIZATION caqi;
CREATE SCHEMA IF NOT EXISTS escolar    AUTHORIZATION caqi;
CREATE SCHEMA IF NOT EXISTS compliance AUTHORIZATION caqi;

-- Extensões úteis: criptografia (campos sensíveis) e UUID v7 (ordenável).
CREATE EXTENSION IF NOT EXISTS pgcrypto;
-- pg_trgm para busca textual em escolas/contratos
CREATE EXTENSION IF NOT EXISTS pg_trgm;
