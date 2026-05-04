import { getServerSession } from 'next-auth';
import { authOptions } from './auth-options';
import { SERVICES, type ServiceName } from './services';

/**
 * Cliente HTTP do BFF — RODA APENAS NO SERVIDOR (Server Components ou API Routes).
 * Pega a credencial Basic Auth da sessão e propaga para o serviço Spring alvo.
 *
 * Nunca exporte/importe este módulo em Client Components — credenciais não vão para o browser.
 */
export async function fetchService<T>(
  service: ServiceName,
  path: string,
  init?: RequestInit,
): Promise<T> {
  const session = await getServerSession(authOptions);
  if (!session) throw new Error('Sessão expirada — refaça login');
  const basicAuth = (session as typeof session & { basicAuth?: string }).basicAuth;
  if (!basicAuth) throw new Error('Sessão sem credencial Basic — refaça login');

  const url = `${SERVICES[service]}${path}`;
  const res = await fetch(url, {
    ...init,
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
      Authorization: `Basic ${basicAuth}`,
      ...(init?.headers ?? {}),
    },
    cache: 'no-store',
  });
  if (!res.ok) {
    const body = await res.text().catch(() => '');
    throw new ApiError(res.status, `${service}${path}: ${res.status} ${res.statusText}`, body);
  }
  return res.json() as Promise<T>;
}

export class ApiError extends Error {
  constructor(public readonly status: number, message: string, public readonly body: string) {
    super(message);
  }
}
