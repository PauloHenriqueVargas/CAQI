import Link from 'next/link';

import { fetchService } from '@/lib/api-client';
import type { SimulacaoPresetDto } from '@/lib/types';

import { PresetCatalog } from './PresetCatalog';

export const dynamic = 'force-dynamic';

export default async function SimulacoesPage() {
  let presets: SimulacaoPresetDto[] = [];
  let erro: string | null = null;
  try {
    presets = await fetchService<SimulacaoPresetDto[]>(
      'engine',
      '/api/v1/caqi/simulacoes/presets',
    );
  } catch (e) {
    erro = e instanceof Error ? e.message : 'Falha ao carregar presets';
  }

  return (
    <div className="space-y-8">
      <header>
        <p className="text-xs font-semibold uppercase tracking-widest text-brand">
          Cenários típicos · CACS-Fundeb / CME
        </p>
        <h1 className="text-2xl font-bold">Simulador "e se?"</h1>
        <p className="mt-1 max-w-3xl text-sm text-slate-500">
          Aplique um <strong>preset pré-configurado</strong> para visualizar o impacto orçamentário
          de políticas educacionais sem precisar montar overrides manualmente. Cada cenário traz
          base legal e impacto esperado. Nada é persistido — apenas comparativo atual vs simulado.
        </p>
      </header>

      {erro ? (
        <p role="alert" className="rounded border border-danger/40 bg-danger/5 p-4 text-sm text-danger">
          Falha ao carregar presets: {erro}
        </p>
      ) : (
        <PresetCatalog presets={presets} />
      )}

      <section className="rounded-lg border border-slate-200 bg-slate-50 p-5 text-sm">
        <h2 className="text-sm font-semibold uppercase tracking-wider text-slate-500">
          Cenário customizado
        </h2>
        <p className="mt-2 text-slate-600">
          Para combinar overrides arbitrários ou simular fora dos padrões do catálogo:
        </p>
        <Link
          href="/simulacoes/nova"
          className="mt-3 inline-block rounded border border-brand px-4 py-1.5 font-semibold text-brand hover:bg-brand hover:text-brand-fg"
        >
          Nova simulação manual →
        </Link>
      </section>
    </div>
  );
}
