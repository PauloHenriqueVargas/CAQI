import { defineConfig, devices } from '@playwright/test';

const PORT = Number(process.env.PORT ?? 3000);
const BASE_URL = process.env.E2E_BASE_URL ?? `http://127.0.0.1:${PORT}`;

export default defineConfig({
  testDir: './tests/e2e',
  timeout: 60_000,
  expect: { timeout: 10_000 },
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 2 : undefined,
  reporter: process.env.CI
    ? [['github'], ['html', { open: 'never', outputFolder: 'playwright-report' }]]
    : [['list'], ['html', { open: 'never', outputFolder: 'playwright-report' }]],
  // Storage state com sessão NextAuth pré-autenticada (gerado em globalSetup)
  globalSetup: require.resolve('./tests/e2e/global-setup'),
  use: {
    baseURL: BASE_URL,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    locale: 'pt-BR',
    timezoneId: 'America/Sao_Paulo',
  },
  projects: [
    // Testes públicos (sem auth) — não usam storage state
    {
      name: 'public',
      testMatch: /(login|redirect|transparencia)\.spec\.ts/,
      use: { ...devices['Desktop Chrome'] },
    },
    // Testes autenticados — carregam cookie de sessão pré-criado
    {
      name: 'authenticated',
      testMatch: /authenticated\.spec\.ts/,
      use: {
        ...devices['Desktop Chrome'],
        storageState: 'tests/e2e/.auth/admin.json',
      },
    },
  ],
  webServer: process.env.E2E_BASE_URL
    ? undefined
    : [
        // Mock dos 4 microserviços Spring — rodam paralelo ao Next.js,
        // health-check no engine garante boot completo antes do globalSetup.
        {
          command: 'node tests/e2e/mock-backend/server.mjs',
          url: 'http://127.0.0.1:9991/api/v1/health',
          timeout: 30_000,
          reuseExistingServer: !process.env.CI,
        },
        // Next.js production build
        {
          command: 'npm run start',
          url: BASE_URL,
          timeout: 120_000,
          reuseExistingServer: !process.env.CI,
          env: {
            PORT: String(PORT),
            NEXTAUTH_SECRET: 'e2e_dummy_secret_not_for_production_use_only',
            NEXTAUTH_URL: BASE_URL,
            CAQ_ENGINE_URL: 'http://127.0.0.1:9991',
            CAQ_FINANCEIRO_URL: 'http://127.0.0.1:9992',
            CAQ_ESCOLAR_URL: 'http://127.0.0.1:9993',
            CAQ_COMPLIANCE_URL: 'http://127.0.0.1:9994',
            CAQI_MUNICIPIO_NOME: 'Município E2E',
          },
        },
      ],
});
