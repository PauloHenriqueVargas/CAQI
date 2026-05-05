'use client';

import { FormEvent, useState } from 'react';

import { fmt } from '@/lib/types';
import type { SimulacaoPresetDto, SimulacaoResultadoDto } from '@/lib/types';

interface Props {
  presets: SimulacaoPresetDto[];
}

const ANO_PADRAO = new Date().getFullYear();

export function PresetCatalog({ presets }: Props) {
  return (
    <div className="space-y-6">
      {presets.map((p) => (
        <PresetCard key={p.nome} preset={p} />
      ))}
    </div>
  );
}

function PresetCard({ preset }: { preset: SimulacaoPresetDto }) {
  const [resultado, setResultado] = useState<SimulacaoResultadoDto | null>(null);
  const [erro, setErro] = useState<string | null>(null);
  const [carregando, setCarregando] = useState(false);

  async function aplicar(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setErro(null);
    setCarregando(true);
    setResultado(null);

    const fd = new FormData(e.currentTarget);
    const ano = Number(fd.get('ano'));
    const escolas = String(fd.get('escolas') ?? '')
      .split(',')
      .map((s) => s.trim())
      .filter(Boolean);

    if (escolas.length === 0) {
      setErro('Informe ao menos um ID de escola.');
      setCarregando(false);
      return;
    }

    try {
      const res = await fetch(`/api/simular/presets/${encodeURIComponent(preset.nome)}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ ano, escolas }),
      });
      const body = await res.json();
      if (!res.ok) {
        setErro(body.erro ?? `Erro ${res.status}`);
      } else {
        setResultado(body as SimulacaoResultadoDto);
      }
    } catch (err) {
      setErro(err instanceof Error ? err.message : 'Erro desconhecido');
    } finally {
      setCarregando(false);
    }
  }

  return (
    <article className="rounded-lg border-2 border-slate-200 bg-white p-6 shadow-sm">
      <header className="space-y-1">
        <h3 className="text-lg font-bold">{preset.titulo}</h3>
        <p className="text-xs font-medium uppercase tracking-wider text-slate-500">
          Etapas: {preset.etapasRecomendadas.join(' · ')}
        </p>
      </header>

      <p className="mt-3 text-sm text-slate-700">{preset.descricao}</p>

      <dl className="mt-4 grid gap-3 text-xs md:grid-cols-2">
        <div>
          <dt className="font-semibold uppercase tracking-wider text-slate-500">Impacto esperado</dt>
          <dd className="mt-0.5 text-slate-700">{preset.impactoEsperado}</dd>
        </div>
        <div>
          <dt className="font-semibold uppercase tracking-wider text-slate-500">Base legal</dt>
          <dd className="mt-0.5 text-slate-700">{preset.baseLegal.join(' · ')}</dd>
        </div>
      </dl>

      {hasOverrides(preset) && (
        <details className="mt-3 rounded border border-slate-200 bg-slate-50 p-3 text-xs">
          <summary className="cursor-pointer font-semibold text-slate-700">Ver overrides aplicados</summary>
          <div className="mt-2 space-y-1 text-slate-600">
            {Object.entries(preset.alunosPorTurma).length > 0 && (
              <p>
                <strong>alunosPorTurma:</strong>{' '}
                {Object.entries(preset.alunosPorTurma).map(([k, v]) => `${k}=${v}`).join(', ')}
              </p>
            )}
            {Object.entries(preset.qtdPadraoInsumos).length > 0 && (
              <p>
                <strong>qtdPadraoInsumos:</strong>{' '}
                {Object.entries(preset.qtdPadraoInsumos).map(([k, v]) => `${k}=${v}`).join(', ')}
              </p>
            )}
            {Object.entries(preset.custoMultiplierInsumos).length > 0 && (
              <p>
                <strong>custoMultiplierInsumos:</strong>{' '}
                {Object.entries(preset.custoMultiplierInsumos).map(([k, v]) => `${k}=×${v}`).join(', ')}
              </p>
            )}
          </div>
        </details>
      )}

      <form onSubmit={aplicar} className="mt-4 grid gap-3 md:grid-cols-[120px_1fr_auto]">
        <label className="block">
          <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">Ano</span>
          <input
            type="number"
            name="ano"
            required
            defaultValue={ANO_PADRAO}
            min={2020}
            className="mt-1 block w-full rounded border border-slate-300 px-3 py-1.5 text-sm focus:border-brand focus:outline-none focus:ring-1 focus:ring-brand"
          />
        </label>
        <label className="block">
          <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">
            Escolas (IDs separados por vírgula)
          </span>
          <input
            type="text"
            name="escolas"
            required
            defaultValue="1"
            placeholder="1, 2, 3"
            className="mt-1 block w-full rounded border border-slate-300 px-3 py-1.5 text-sm focus:border-brand focus:outline-none focus:ring-1 focus:ring-brand"
          />
        </label>
        <button
          type="submit"
          disabled={carregando}
          className="self-end rounded bg-brand px-4 py-1.5 text-sm font-semibold text-brand-fg hover:bg-brand/90 disabled:opacity-60"
        >
          {carregando ? 'Aplicando…' : 'Aplicar preset'}
        </button>
      </form>

      {erro && (
        <p role="alert" className="mt-3 rounded bg-danger/10 px-3 py-2 text-sm text-danger">
          {erro}
        </p>
      )}

      {resultado && <ResultTable resultado={resultado} />}
    </article>
  );
}

function hasOverrides(p: SimulacaoPresetDto): boolean {
  return (
    Object.keys(p.alunosPorTurma).length > 0 ||
    Object.keys(p.qtdPadraoInsumos).length > 0 ||
    Object.keys(p.custoMultiplierInsumos).length > 0
  );
}

function ResultTable({ resultado }: { resultado: SimulacaoResultadoDto }) {
  return (
    <section
      aria-label={`Resultado da simulação para ${resultado.ano}`}
      className="mt-4 rounded border border-brand/30 bg-brand/5 p-4"
    >
      <h4 className="text-sm font-semibold text-slate-700">
        Resultado · ano {resultado.ano} · {resultado.diferencas.length} item(ns)
      </h4>
      {resultado.diferencas.length === 0 ? (
        <p className="mt-2 text-sm text-slate-500">
          Nenhuma combinação (escola × etapa) gerou diferença — verifique se a escola tem
          parâmetros para as etapas recomendadas do preset.
        </p>
      ) : (
        <div className="mt-2 overflow-x-auto">
          <table className="w-full text-sm">
            <caption className="sr-only">Diferenças do preset por escola/etapa</caption>
            <thead className="text-xs uppercase tracking-wider text-slate-500">
              <tr>
                <th scope="col" className="px-2 py-1 text-left">Escola/Etapa</th>
                <th scope="col" className="px-2 py-1 text-right">CAQi atual</th>
                <th scope="col" className="px-2 py-1 text-right">CAQi simulado</th>
                <th scope="col" className="px-2 py-1 text-right">Δ</th>
                <th scope="col" className="px-2 py-1 text-right">Δ %</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200">
              {resultado.diferencas.map((d) => {
                const sobe = parseFloat(d.deltaCaqi) >= 0;
                return (
                  <tr key={`${d.escolaId}|${d.etapaCodigo}`}>
                    <td className="px-2 py-1 font-medium">
                      {d.escolaId} · {d.etapaCodigo}
                    </td>
                    <td className="px-2 py-1 text-right tabular-nums">{fmt.money(d.caqiAtual)}</td>
                    <td className="px-2 py-1 text-right tabular-nums">{fmt.money(d.caqiSimulado)}</td>
                    <td className={`px-2 py-1 text-right tabular-nums font-semibold ${sobe ? 'text-warning' : 'text-success'}`}>
                      {sobe ? '+' : ''}{fmt.money(d.deltaCaqi)}
                    </td>
                    <td className={`px-2 py-1 text-right tabular-nums ${sobe ? 'text-warning' : 'text-success'}`}>
                      {sobe ? '+' : ''}{fmt.percent(d.pctDeltaCaqi)}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}
