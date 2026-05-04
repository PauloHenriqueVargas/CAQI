/**
 * Endpoints internos dos microserviços Spring.
 *
 * No BFF (server-side), use estas URLs para falar com os serviços via rede
 * interna (docker-compose ou k8s ClusterIP). Nunca exponha-as ao browser.
 */

const required = (name: string, fallback?: string): string => {
  const v = process.env[name] ?? fallback;
  if (!v) {
    throw new Error(`Variável de ambiente ${name} é obrigatória`);
  }
  return v;
};

export const SERVICES = {
  engine: required("CAQ_ENGINE_URL", "http://localhost:8081"),
  financeiro: required("CAQ_FINANCEIRO_URL", "http://localhost:8082"),
  escolar: required("CAQ_ESCOLAR_URL", "http://localhost:8083"),
  compliance: required("CAQ_COMPLIANCE_URL", "http://localhost:8084"),
} as const;

export type ServiceName = keyof typeof SERVICES;

export async function callService<T>(
  service: ServiceName,
  path: string,
  init?: RequestInit,
): Promise<T> {
  const url = `${SERVICES[service]}${path}`;
  const res = await fetch(url, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      Accept: "application/json",
      ...(init?.headers ?? {}),
    },
    // BFF — sem cache padrão; rotas que cacheiam pedem explicitamente
    cache: "no-store",
  });
  if (!res.ok) {
    throw new Error(`${service} ${path} falhou: ${res.status} ${res.statusText}`);
  }
  return res.json() as Promise<T>;
}
