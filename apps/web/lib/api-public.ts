import { SERVICES, type ServiceName } from './services';

/**
 * Cliente HTTP do BFF para endpoints PÚBLICOS (LAI / LRF art. 48-A).
 *
 * Não exige sessão e NÃO injeta credencial — bate em /api/public/transparencia/**
 * dos serviços, que estão configurados como permitAll no Spring Security.
 *
 * Cache server-side de 5 min via revalidate (alinha com o Cache-Control
 * `max-age=300, public` enviado pelos backends — Sprint 6.A).
 *
 * Em caso de falha do backend, retorna `null` para a página decidir como
 * degradar (ex.: mostrar mensagem amigável em vez de 500). Erros são
 * logados mas não propagam para o usuário público.
 */
export async function fetchPublic<T>(
  service: ServiceName,
  path: string,
): Promise<T | null> {
  const url = `${SERVICES[service]}${path}`;
  try {
    const res = await fetch(url, {
      headers: { Accept: 'application/json' },
      next: { revalidate: 300 },
    });
    if (!res.ok) {
      console.error(`[public-bff] ${service}${path} → ${res.status} ${res.statusText}`);
      return null;
    }
    return (await res.json()) as T;
  } catch (e) {
    console.error(`[public-bff] ${service}${path} unreachable:`, e);
    return null;
  }
}
