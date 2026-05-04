'use client';

import { FormEvent, useState } from 'react';

import { Field, FormCard, ErrorBanner } from '@/components/Form';
import { fmt } from '@/lib/types';
import type { Money } from '@/lib/types';

interface DiferencaItemDto {
  escolaId: string;
  etapaCodigo: string;
  caqiAtual: Money;
  caqiSimulado: Money;
  deltaCaqi: Money;
  caqAtual: Money;
  caqSimulado: Money;
  deltaCaq: Money;
  pctDeltaCaqi: Money;
}

interface SimulacaoResultadoDto {
  ano: number;
  diferencas: DiferencaItemDto[];
}

const ANO_PADRAO = new Date().getFullYear();

export function SimulacaoForm() {
  const [resultado, setResultado] = useState<SimulacaoResultadoDto | null>(null);
  const [erro, setErro] = useState<string | null>(null);
  const [carregando, setCarregando] = useState(false);

  async function handleSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setErro(null);
    setCarregando(true);
    setResultado(null);

    const fd = new FormData(e.currentTarget);
    const etapaCodigo = String(fd.get('etapa') ?? '').trim().toUpperCase();
    const alunosOverride = fd.get('alunosPorTurma');
    const insumoOverride = String(fd.get('insumoCodigo') ?? '').trim().toUpperCase();
    const multiplierOverride = fd.get('custoMultiplier');

    const cenario = {
      ano: Number(fd.get('ano')),
      escolas: [String(fd.get('escolaId') ?? '').trim()],
      etapas: [etapaCodigo],
      alunosPorTurma: alunosOverride ? { [etapaCodigo]: Number(alunosOverride) } : {},
      qtdPadraoInsumos: {},
      custoMultiplierInsumos:
        insumoOverride && multiplierOverride
          ? { [insumoOverride]: Number(multiplierOverride) }
          : {},
    };

    try {
      const res = await fetch('/api/simular', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(cenario),
      });
      const body = await res.json();
      if (!res.ok) {
        setErro(body.erro ?? `Erro ${res.status}`);
      } else {
        setResultado(body as SimulacaoResultadoDto);
      }
    } catch (e2) {
      setErro(e2 instanceof Error ? e2.message : 'Erro desconhecido');
    } finally {
      setCarregando(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      <FormCard title="Cenário" subtitle="Pelo menos um override deve ser informado para a simulação fazer sentido.">
        <div className="grid gap-4 md:grid-cols-3">
          <Field label="Ano" name="ano" type="number" required defaultValue={ANO_PADRAO} />
          <Field label="Escola (ID)" name="escolaId" required defaultValue="1" />
          <Field label="Etapa (código)" name="etapa" required defaultValue="EF1" />
        </div>
      </FormCard>

      <FormCard title="Override de alunos por turma" subtitle="Substitui o ParametroEtapa.alunosPorTurma da etapa selecionada.">
        <Field
          label="Alunos por turma (vazio = sem override)"
          name="alunosPorTurma"
          type="number"
          min={1}
          max={50}
          placeholder="ex.: 20"
          helper="Apenas afeta insumos do tipo por_turma (ex.: PES-001)."
        />
      </FormCard>

      <FormCard title="Override de custo unitário (multiplicador)" subtitle="Ex.: piso salarial +25% → multiplicador 1.25.">
        <div className="grid gap-4 md:grid-cols-2">
          <Field label="Código do insumo" name="insumoCodigo" placeholder="PES-001" />
          <Field label="Multiplicador" name="custoMultiplier" type="number" step="0.01" min={0.1} max={5} placeholder="1.25" />
        </div>
      </FormCard>

      <ErrorBanner message={erro} />

      <div className="flex items-center justify-end gap-3">
        <button
          type="submit"
          disabled={carregando}
          className="rounded bg-brand px-5 py-2 font-semibold text-brand-fg hover:bg-brand/90 disabled:opacity-60"
        >
          {carregando ? 'Simulando…' : 'Simular'}
        </button>
      </div>

      {resultado && (
        <section className="rounded-lg border-2 border-brand/30 bg-white p-6 shadow-sm">
          <h2 className="text-lg font-semibold">Resultado</h2>
          <p className="text-sm text-slate-500">Ano {resultado.ano} · {resultado.diferencas.length} item(ns)</p>

          <div className="mt-4 overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="border-b border-slate-200 bg-slate-50 text-left text-xs uppercase tracking-wider text-slate-500">
                <tr>
                  <th className="px-3 py-2">Escola/Etapa</th>
                  <th className="px-3 py-2 text-right">CAQi atual</th>
                  <th className="px-3 py-2 text-right">CAQi simulado</th>
                  <th className="px-3 py-2 text-right">Δ CAQi</th>
                  <th className="px-3 py-2 text-right">Δ %</th>
                  <th className="px-3 py-2 text-right">CAQ atual</th>
                  <th className="px-3 py-2 text-right">CAQ simulado</th>
                  <th className="px-3 py-2 text-right">Δ CAQ</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {resultado.diferencas.map((d) => (
                  <tr key={`${d.escolaId}|${d.etapaCodigo}`}>
                    <td className="px-3 py-2 font-medium">
                      {d.escolaId} · {d.etapaCodigo}
                    </td>
                    <td className="px-3 py-2 text-right tabular-nums">{fmt.money(d.caqiAtual)}</td>
                    <td className="px-3 py-2 text-right tabular-nums">{fmt.money(d.caqiSimulado)}</td>
                    <td className={`px-3 py-2 text-right tabular-nums font-semibold ${parseFloat(d.deltaCaqi) >= 0 ? 'text-warning' : 'text-success'}`}>
                      {parseFloat(d.deltaCaqi) >= 0 ? '+' : ''}{fmt.money(d.deltaCaqi)}
                    </td>
                    <td className={`px-3 py-2 text-right tabular-nums ${parseFloat(d.pctDeltaCaqi) >= 0 ? 'text-warning' : 'text-success'}`}>
                      {parseFloat(d.pctDeltaCaqi) >= 0 ? '+' : ''}{fmt.percent(d.pctDeltaCaqi)}
                    </td>
                    <td className="px-3 py-2 text-right tabular-nums">{fmt.money(d.caqAtual)}</td>
                    <td className="px-3 py-2 text-right tabular-nums">{fmt.money(d.caqSimulado)}</td>
                    <td className={`px-3 py-2 text-right tabular-nums ${parseFloat(d.deltaCaq) >= 0 ? 'text-warning' : 'text-success'}`}>
                      {parseFloat(d.deltaCaq) >= 0 ? '+' : ''}{fmt.money(d.deltaCaq)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}
    </form>
  );
}
