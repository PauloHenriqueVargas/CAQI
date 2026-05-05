# Testes E2E + WCAG

Testes ponta-a-ponta com [Playwright](https://playwright.dev/) e auditoria de
acessibilidade com [axe-core](https://github.com/dequelabs/axe-core) (regras
`wcag2a`, `wcag2aa`, `wcag21a`, `wcag21aa`).

## Escopo

Os testes cobrem **rotas públicas / não autenticadas** porque o pipeline de CI
não sobe os 4 microserviços Spring Boot (engine, financeiro, escolar,
compliance). Cobertura atual:

- `/login` — formulário, validação HTML5, normalização do campo MFA, erro
  genérico em backend indisponível, axe-core scan
- `/`, `/dashboard`, `/despesas`, `/profile/mfa` — middleware NextAuth deve
  redirecionar para `/login?callbackUrl=...`
- Login com `callbackUrl` — preserva o redirect e mantém a11y

Telas autenticadas (cálculos, simulações, despesas, fundeb, MFA) precisam do
stack completo rodando — testar manualmente em ambiente de homologação ou em
um job separado que faça `docker compose up` antes do `playwright test`.

## Como rodar localmente

```bash
cd apps/web

# 1. Instalar deps + browsers (apenas 1ª vez)
npm install
npm run test:e2e:install

# 2. Build da aplicação (o config sobe `next start` automaticamente)
npm run build

# 3. Rodar testes
npm run test:e2e
```

O `playwright.config.ts` sobe `next start` em `127.0.0.1:3000` apontando para
URLs de microserviços fictícias (`9991`–`9994`). Isso é proposital: o teste
de login com backend indisponível valida que a UI mostra erro genérico
(sem enumerar usuários).

Para rodar contra um ambiente já levantado (ex.: staging), exporte
`E2E_BASE_URL`:

```bash
E2E_BASE_URL=https://caqi.homolog.exemplo.gov.br npm run test:e2e
```

## Relatórios

- HTML report: `apps/web/playwright-report/index.html`
- Trace por teste falho (com timeline + DOM): `apps/web/test-results/`
- JSON do axe-core anexado a cada teste WCAG (ver aba *Attachments*)

No CI, ambos são publicados como artifacts do workflow `web-e2e`.
