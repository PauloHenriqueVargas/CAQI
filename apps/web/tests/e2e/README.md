# Testes E2E + WCAG

Testes ponta-a-ponta com [Playwright](https://playwright.dev/) e auditoria de
acessibilidade com [axe-core](https://github.com/dequelabs/axe-core) (regras
`wcag2a`, `wcag2aa`, `wcag21a`, `wcag21aa`).

## Escopo

Cobertura atual divide em **2 projects** Playwright (definidos em
`playwright.config.ts`):

### `public` (rotas sem auth, backend down)

Testa que a casca renderiza e degrada graciosamente quando o BFF retorna null:
- `login.spec.ts` — formulário, validação HTML5, normalização MFA, erro genérico
- `redirect.spec.ts` — middleware NextAuth redireciona rotas privadas → `/login`
- `transparencia.spec.ts` — portal LAI/LRF público com 7 subpáginas + sitemap + robots

### `authenticated` (rotas privadas, mock backend)

Testa fluxos completos com sessão pré-criada e backend mockado:
- `authenticated.spec.ts` — Dashboard, /calculos, /calculos/[id], /fundeb (gauges
  cumprem), /simulacoes (presets + apply), /notificacoes, /despesas, /contratos,
  /profile/mfa, logout — cada um com WCAG 2.1 AA

### Mock backend (`mock-backend/server.mjs`)

Servidor Node.js stdlib (sem deps) que escuta em portas 9991-9994 e responde
fixtures JSON para os endpoints consumidos. Cobre:
- `/api/v1/caqi/insumos` (probe de credenciais NextAuth)
- `/api/v1/auth/mfa/status` (enabled=false → permite login sem TOTP)
- `/api/v1/caqi/calculos`, `/calculos/{id}`, `/simulacoes/presets`, `/simulacoes/presets/{nome}`
- `/api/v1/fundeb/execucao` com cumpre=true em todas vinculações
- `/api/v1/financeiro/{contratos,despesas,fornecedores,fontes-recurso,receitas}`
- `/api/v1/compliance/notificacoes`
- `/api/public/transparencia/**` (espelham os mesmos fixtures com cache 5min)

Vantagens vs subir docker-compose com Spring:
- 100ms de boot vs 90s
- 0 dependências (Node stdlib)
- Determinístico (fixtures fixas)
- Permite testar caminhos de erro injetando 4xx/5xx pontuais

### Global setup (`global-setup.ts`)

Antes da suíte autenticada rodar, faz login pela UI uma única vez e salva o
cookie de sessão NextAuth em `tests/e2e/.auth/admin.json` (gitignored).
Os specs autenticados carregam esse storage state via Playwright project config —
sem refazer login a cada teste.

## Como rodar localmente

```bash
cd apps/web

# 1. Instalar deps + browsers (apenas 1ª vez)
npm install
npm run test:e2e:install

# 2. Build da aplicação (o config sobe `next start` automaticamente)
npm run build

# 3. Rodar testes (todos os projects)
npm run test:e2e

# Rodar apenas público (rápido):
npx playwright test --project=public

# Rodar apenas autenticado:
npx playwright test --project=authenticated
```

O `playwright.config.ts` sobe **2 webServers** em paralelo:
- mock-backend em `127.0.0.1:9991-9994` (Node, ~100ms boot)
- `next start` em `127.0.0.1:3000`

Para rodar contra um ambiente real (staging com backend completo), exporte
`E2E_BASE_URL`:

```bash
E2E_BASE_URL=https://caqi.homolog.exemplo.gov.br npm run test:e2e
```

Nesse modo nenhum webServer é iniciado — Playwright assume que tudo está rodando.

## Relatórios

- HTML report: `apps/web/playwright-report/index.html`
- Trace por teste falho (com timeline + DOM): `apps/web/test-results/`
- JSON do axe-core anexado a cada teste WCAG (ver aba *Attachments*)

No CI, ambos são publicados como artifacts do workflow `web-e2e`.
