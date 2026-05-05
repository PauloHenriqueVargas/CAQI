import { expect, test } from '@playwright/test';

import { runAxe } from './fixtures/axe-helper';

test.describe('Roteamento público (não autenticado)', () => {
  test('GET / redireciona para /login (via /dashboard, middleware NextAuth)', async ({ page }) => {
    await page.goto('/');
    await expect(page).toHaveURL(/\/login/);
  });

  test('GET /dashboard redireciona para /login com callbackUrl preservado', async ({ page }) => {
    await page.goto('/dashboard');
    await expect(page).toHaveURL(/\/login\?callbackUrl=/);
    const url = new URL(page.url());
    expect(url.searchParams.get('callbackUrl')).toContain('/dashboard');
  });

  test('GET /despesas redireciona para /login (rota protegida)', async ({ page }) => {
    await page.goto('/despesas');
    await expect(page).toHaveURL(/\/login\?callbackUrl=/);
  });

  test('GET /profile/mfa redireciona para /login (rota protegida)', async ({ page }) => {
    await page.goto('/profile/mfa');
    await expect(page).toHaveURL(/\/login\?callbackUrl=/);
  });

  test('Página de login com callbackUrl é acessível e WCAG-compliant', async ({ page }, testInfo) => {
    await page.goto('/login?callbackUrl=%2Fdespesas');
    await expect(page.getByRole('heading', { name: /Sistema CAQ\/CAQi/i })).toBeVisible();
    await runAxe(page, testInfo);
  });
});
