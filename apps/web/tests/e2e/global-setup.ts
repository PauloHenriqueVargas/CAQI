import { chromium, type FullConfig } from '@playwright/test';
import fs from 'node:fs';
import path from 'node:path';

/**
 * Faz login uma única vez antes da suíte de testes autenticados e salva
 * o cookie de sessão NextAuth em `tests/e2e/.auth/admin.json`. Os specs
 * com `storageState: 'tests/e2e/.auth/admin.json'` carregam esse estado
 * automaticamente, evitando refazer login a cada teste.
 *
 * Pré-requisitos: webServer Next.js + mock-backend já rodando (Playwright
 * espera por eles antes de chamar globalSetup).
 */
export default async function globalSetup(config: FullConfig) {
  const baseURL = config.projects[0]?.use?.baseURL ?? 'http://127.0.0.1:3000';
  const authDir = path.join(__dirname, '.auth');
  const authFile = path.join(authDir, 'admin.json');
  fs.mkdirSync(authDir, { recursive: true });

  const browser = await chromium.launch();
  const context = await browser.newContext();
  const page = await context.newPage();

  await page.goto(`${baseURL}/login`);
  await page.getByLabel('Usuário').fill('admin_test');
  await page.getByLabel('Senha').fill('senha_qualquer_e2e');
  // Sem MFA: mock backend retorna enabled=false em /api/v1/auth/mfa/status
  await page.getByRole('button', { name: /Entrar/i }).click();

  // Espera o redirect pós-login para /dashboard (ou rota qualquer protegida)
  await page.waitForURL((url) => !url.pathname.startsWith('/login'), { timeout: 30_000 });

  await context.storageState({ path: authFile });
  await browser.close();

  process.stdout.write(`[global-setup] Sessão admin gravada em ${authFile}\n`);
}
