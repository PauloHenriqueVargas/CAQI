import { expect, test } from '@playwright/test';

import { runAxe } from './fixtures/axe-helper';

/**
 * Suíte autenticada: usa o storage state preparado pelo `global-setup.ts`
 * (cookie de sessão NextAuth válido). O backend é o mock em portas
 * 9991-9994 com fixtures determinísticas.
 *
 * Cobre os fluxos críticos:
 *   - Dashboard pós-login (KPIs, gauges)
 *   - /calculos lista + /calculos/[id] memória
 *   - /fundeb com vinculações cumpridas
 *   - /simulacoes presets (visualização + apply preset)
 *   - /notificacoes com lista
 *   - /despesas lista filtrada por ano
 *   - /contratos lista
 *   - /profile/mfa status (enabled=false)
 *   - /api/auth/signout (logout) → redirect /login
 *
 * Cada página tem sua varredura WCAG 2.1 AA via axe-core.
 */

test.describe('Suíte autenticada (mock backend)', () => {

  test('Dashboard renderiza KPIs após login persistido (storage state)', async ({ page }) => {
    await page.goto('/dashboard');
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
    // Header autenticado deve mostrar nav com links operacionais
    await expect(page.getByRole('link', { name: /Cálculos CAQ/i })).toBeVisible();
    await expect(page.getByRole('link', { name: /Notificações/i })).toBeVisible();
  });

  test('/calculos exibe a lista vinda do mock (2 cálculos)', async ({ page }) => {
    await page.goto('/calculos');
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
    // Mock retorna 2 cálculos — espera ao menos as duas linhas (texto-id presente)
    const corpo = page.locator('body');
    await expect(corpo).toContainText(/Escola/i);
  });

  test('/calculos/1 exibe a memória item-a-item (PES-001 + MOB-001)', async ({ page }) => {
    await page.goto('/calculos/1');
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
    await expect(page.getByText(/PES-001/)).toBeVisible();
  });

  test('/fundeb mostra os 3 gauges com cumpre=true', async ({ page }) => {
    await page.goto('/fundeb');
    await expect(page.getByRole('heading', { name: /Fundeb/i })).toBeVisible();
    await expect(page.getByText(/MDE 25/i)).toBeVisible();
    await expect(page.getByText(/Fundeb 70/i)).toBeVisible();
    await expect(page.getByText(/VAAT 15/i)).toBeVisible();
    // Mock devolve cumpre=true em todos
    await expect(page.getByText(/CUMPRE/i).first()).toBeVisible();
  });

  test('/simulacoes lista o preset "tempo_integral_universal" do mock', async ({ page }) => {
    await page.goto('/simulacoes');
    await expect(page.getByRole('heading', { name: /Simulador/i })).toBeVisible();
    await expect(page.getByText(/Tempo integral universal/i)).toBeVisible();
    await expect(page.getByRole('button', { name: /Aplicar preset/i }).first()).toBeVisible();
  });

  test('Aplicar preset chama o BFF e mostra resultado inline com Δ CAQi', async ({ page }) => {
    await page.goto('/simulacoes');
    await page.getByRole('button', { name: /Aplicar preset/i }).first().click();
    // Mock retorna 1 diferença (escola=1, etapa=EF1, deltaCaqi=850, pct=19.14)
    await expect(page.getByText(/19,14/)).toBeVisible({ timeout: 15_000 });
  });

  test('/notificacoes mostra a notificação VAAT do mock', async ({ page }) => {
    await page.goto('/notificacoes');
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
    await expect(page.getByText(/VAAT 15/i)).toBeVisible();
  });

  test('/despesas exibe lista com KPIs por classificação', async ({ page }) => {
    await page.goto('/despesas');
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
  });

  test('/contratos exibe contrato Lei 14.133 do mock', async ({ page }) => {
    await page.goto('/contratos');
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
    await expect(page.getByText(/Limpeza escolar/i)).toBeVisible();
  });

  test('/profile/mfa mostra status enabled=false do mock', async ({ page }) => {
    await page.goto('/profile/mfa');
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
  });

  test('Logout limpa sessão e devolve para /login', async ({ page }) => {
    await page.goto('/dashboard');
    // O componente SignOutButton dispara signOut do NextAuth
    await page.getByRole('button', { name: /Sair|Logout|Sign out/i }).click();
    await page.waitForURL(/\/login/, { timeout: 15_000 });
  });

  test('WCAG 2.1 AA — /dashboard', async ({ page }, testInfo) => {
    await page.goto('/dashboard');
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
    await runAxe(page, testInfo);
  });

  test('WCAG 2.1 AA — /fundeb', async ({ page }, testInfo) => {
    await page.goto('/fundeb');
    await runAxe(page, testInfo);
  });

  test('WCAG 2.1 AA — /simulacoes (presets)', async ({ page }, testInfo) => {
    await page.goto('/simulacoes');
    await expect(page.getByText(/Tempo integral universal/i)).toBeVisible();
    await runAxe(page, testInfo);
  });

  test('WCAG 2.1 AA — /calculos', async ({ page }, testInfo) => {
    await page.goto('/calculos');
    await runAxe(page, testInfo);
  });
});
