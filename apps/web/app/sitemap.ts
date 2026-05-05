import type { MetadataRoute } from 'next';

const BASE_URL = process.env.NEXTAUTH_URL ?? 'http://localhost:3000';

/**
 * Apenas as rotas /transparencia/** são públicas e indexáveis.
 * Tudo sob /(authenticated)/** é proprietário (LRF não exige indexação;
 * LAI exige acesso, não indexação) — fica fora.
 */
export default function sitemap(): MetadataRoute.Sitemap {
  const now = new Date();
  return [
    { url: `${BASE_URL}/transparencia`,                lastModified: now, changeFrequency: 'daily',   priority: 1.0 },
    { url: `${BASE_URL}/transparencia/conselhos`,      lastModified: now, changeFrequency: 'daily',   priority: 0.95 },
    { url: `${BASE_URL}/transparencia/fundeb`,         lastModified: now, changeFrequency: 'daily',   priority: 0.9 },
    { url: `${BASE_URL}/transparencia/calculos`,       lastModified: now, changeFrequency: 'weekly',  priority: 0.8 },
    { url: `${BASE_URL}/transparencia/contratos`,      lastModified: now, changeFrequency: 'weekly',  priority: 0.8 },
    { url: `${BASE_URL}/transparencia/despesas`,       lastModified: now, changeFrequency: 'daily',   priority: 0.8 },
    { url: `${BASE_URL}/transparencia/notificacoes`,   lastModified: now, changeFrequency: 'daily',   priority: 0.7 },
    { url: `${BASE_URL}/transparencia/publicacoes`,    lastModified: now, changeFrequency: 'daily',   priority: 0.6 },
  ];
}
