import AxeBuilder from '@axe-core/playwright';
import { expect, type Page, type TestInfo } from '@playwright/test';

/**
 * Roda uma varredura axe-core na página atual exigindo conformidade
 * WCAG 2.1 nível A + AA. Em caso de violações, anexa o relatório ao
 * resultado do Playwright para inspeção pós-mortem.
 *
 * Tags aplicadas: wcag2a, wcag2aa, wcag21a, wcag21aa.
 *
 * Regras desabilitadas (intencional):
 *  - "region" — landmarks são adicionados pelo layout autenticado;
 *    páginas de erro/login não precisam de role=main duplicado.
 */
export async function runAxe(page: Page, testInfo: TestInfo, options?: { allow?: string[] }) {
  const allow = options?.allow ?? [];
  const builder = new AxeBuilder({ page })
    .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'])
    .disableRules(['region', ...allow]);

  const results = await builder.analyze();
  await testInfo.attach('axe-report', {
    body: JSON.stringify(results, null, 2),
    contentType: 'application/json',
  });
  expect(
    results.violations,
    `WCAG violations:\n${results.violations.map((v) => `  • ${v.id}: ${v.help} (${v.nodes.length} nodes)`).join('\n')}`,
  ).toEqual([]);
}
