import type { MetadataRoute } from 'next';

const BASE_URL = process.env.NEXTAUTH_URL ?? 'http://localhost:3000';

/**
 * Permite indexar somente o portal público (/transparencia/**).
 * Bloqueia tudo o que é administrativo/proprietário ou que poderia vazar
 * superfície de auth (login, dashboard, BFF interno).
 */
export default function robots(): MetadataRoute.Robots {
  return {
    rules: [
      {
        userAgent: '*',
        allow: ['/transparencia', '/transparencia/'],
        disallow: [
          '/login',
          '/dashboard',
          '/calculos',
          '/contratos',
          '/despesas',
          '/fornecedores',
          '/receitas',
          '/simulacoes',
          '/notificacoes',
          '/fundeb',
          '/profile/',
          '/api/',
          '/_next/',
        ],
      },
    ],
    sitemap: `${BASE_URL}/sitemap.xml`,
  };
}
