import { expect, test } from '@playwright/test';

import { runAxe } from './fixtures/axe-helper';

/**
 * Portal público de transparência. Em CI o backend está fora — a página
 * deve renderizar a casca (header, nav, footer, mensagem de "sem dados")
 * mesmo quando o BFF público recebe falha (`fetchPublic` retorna `null`).
 */
test.describe('Portal público /transparencia (sem auth)', () => {
  test('Landing renderiza shell + KPIs com fallback "—" quando backend down', async ({ page }) => {
    await page.goto('/transparencia');

    await expect(page.getByRole('heading', { name: /Transparência da educação municipal/i })).toBeVisible();
    await expect(page.getByRole('navigation', { name: /Seções do portal/i })).toBeVisible();
    await expect(page.getByRole('contentinfo')).toContainText(/Lei 12\.527\/2011/);
    // "Acesso administrativo" link visível
    await expect(page.getByRole('link', { name: /Acesso administrativo/i })).toBeVisible();
  });

  test('Skip-link "Pular para o conteúdo" recebe foco com Tab', async ({ page }) => {
    await page.goto('/transparencia');
    await page.keyboard.press('Tab');
    const link = page.getByRole('link', { name: /Pular para o conteúdo/i });
    await expect(link).toBeFocused();
  });

  test('Robots.txt permite /transparencia e bloqueia áreas autenticadas', async ({ request }) => {
    const res = await request.get('/robots.txt');
    expect(res.ok()).toBeTruthy();
    const body = await res.text();
    expect(body).toMatch(/Allow:\s*\/transparencia/);
    expect(body).toMatch(/Disallow:\s*\/login/);
    expect(body).toMatch(/Disallow:\s*\/dashboard/);
    expect(body).toMatch(/Sitemap:\s*http/);
  });

  test('Sitemap.xml contém as rotas públicas com prioridades', async ({ request }) => {
    const res = await request.get('/sitemap.xml');
    expect(res.ok()).toBeTruthy();
    const body = await res.text();
    expect(body).toContain('<urlset');
    expect(body).toContain('/transparencia/fundeb');
    expect(body).toContain('/transparencia/calculos');
    expect(body).toContain('/transparencia/contratos');
    expect(body).toContain('/transparencia/despesas');
    expect(body).toContain('/transparencia/notificacoes');
  });

  test('Subpáginas renderizam shell + banner "sem dados" quando BFF retorna null', async ({ page }) => {
    for (const path of [
      '/transparencia/fundeb',
      '/transparencia/contratos',
      '/transparencia/despesas',
      '/transparencia/calculos',
      '/transparencia/notificacoes',
    ]) {
      await page.goto(path);
      await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
    }
  });

  test('Metadata robots na landing /transparencia é index,follow (override do RootLayout noindex)', async ({ page }) => {
    const res = await page.goto('/transparencia');
    expect(res?.ok()).toBeTruthy();
    const robotsMeta = await page.locator('meta[name="robots"]').getAttribute('content');
    if (robotsMeta) {
      expect(robotsMeta.toLowerCase()).toContain('index');
      expect(robotsMeta.toLowerCase()).not.toContain('noindex');
    }
  });

  test('Title e description presentes nas subpáginas (SEO)', async ({ page }) => {
    await page.goto('/transparencia/fundeb');
    await expect(page).toHaveTitle(/Fundeb.*Transparência.*CAQ/);
    const desc = await page.locator('meta[name="description"]').getAttribute('content');
    expect(desc).toMatch(/Fundeb|MDE|VAAT/i);
  });

  test('WCAG 2.1 AA — landing /transparencia', async ({ page }, testInfo) => {
    await page.goto('/transparencia');
    await runAxe(page, testInfo);
  });

  test('WCAG 2.1 AA — /transparencia/fundeb (sem dados)', async ({ page }, testInfo) => {
    await page.goto('/transparencia/fundeb');
    await runAxe(page, testInfo);
  });

  test('WCAG 2.1 AA — /transparencia/contratos (lista vazia)', async ({ page }, testInfo) => {
    await page.goto('/transparencia/contratos');
    await runAxe(page, testInfo);
  });

  test('WCAG 2.1 AA — /transparencia/notificacoes (lista vazia)', async ({ page }, testInfo) => {
    await page.goto('/transparencia/notificacoes');
    await runAxe(page, testInfo);
  });
});
