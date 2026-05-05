import { expect, test } from '@playwright/test';

import { runAxe } from './fixtures/axe-helper';

test.describe('Página de login (público)', () => {
  test('renderiza formulário com campos obrigatórios e link de marca', async ({ page }) => {
    await page.goto('/login');

    await expect(page.getByRole('heading', { name: /Sistema CAQ\/CAQi/i })).toBeVisible();
    await expect(page.getByLabel('Usuário')).toBeVisible();
    await expect(page.getByLabel('Senha')).toBeVisible();
    await expect(page.getByLabel(/Código MFA/i)).toBeVisible();
    await expect(page.getByRole('button', { name: /Entrar/i })).toBeEnabled();
  });

  test('aplica autocomplete correto nos campos de credenciais (acessibilidade/UX)', async ({ page }) => {
    await page.goto('/login');
    await expect(page.getByLabel('Usuário')).toHaveAttribute('autocomplete', 'username');
    await expect(page.getByLabel('Senha')).toHaveAttribute('autocomplete', 'current-password');
    await expect(page.getByLabel(/Código MFA/i)).toHaveAttribute('autocomplete', 'one-time-code');
  });

  test('exige usuário/senha (validação HTML5 nativa)', async ({ page }) => {
    await page.goto('/login');
    await page.getByRole('button', { name: /Entrar/i }).click();

    const usuarioInvalid = await page.getByLabel('Usuário').evaluate(
      (el: HTMLInputElement) => !el.validity.valid && el.validity.valueMissing,
    );
    expect(usuarioInvalid).toBe(true);
  });

  test('campo MFA normaliza para maiúsculas e remove caracteres não alfanuméricos', async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel(/Código MFA/i).fill('abc 123!@#xyz');
    await expect(page.getByLabel(/Código MFA/i)).toHaveValue('ABC123XYZ');
  });

  test('submissão com backend indisponível mostra mensagem de erro genérica (sem enumeração)', async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel('Usuário').fill('usuario_inexistente');
    await page.getByLabel('Senha').fill('senha_qualquer_123');
    await page.getByRole('button', { name: /Entrar/i }).click();

    const alert = page.getByRole('alert');
    await expect(alert).toBeVisible({ timeout: 15_000 });
    await expect(alert).toContainText(/inválidos/i);
    await expect(page).toHaveURL(/\/login/);
  });

  test('WCAG 2.1 AA — sem violações axe-core', async ({ page }, testInfo) => {
    await page.goto('/login');
    await runAxe(page, testInfo);
  });

  test('WCAG após erro de submissão (alerta visível)', async ({ page }, testInfo) => {
    await page.goto('/login');
    await page.getByLabel('Usuário').fill('x');
    await page.getByLabel('Senha').fill('y');
    await page.getByRole('button', { name: /Entrar/i }).click();
    await expect(page.getByRole('alert')).toBeVisible({ timeout: 15_000 });
    await runAxe(page, testInfo);
  });
});
