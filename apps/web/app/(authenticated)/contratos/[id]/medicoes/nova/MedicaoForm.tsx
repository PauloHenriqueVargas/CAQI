'use client';

import { useRouter } from 'next/navigation';
import { FormEvent, useState } from 'react';

import { ErrorBanner, Field, FormCard, Select } from '@/components/Form';
import { TIPOS_SERVICO_RETENCAO, fmt } from '@/lib/types';
import type { MedicaoComRetencoesDto } from '@/lib/types';

export function MedicaoForm({
  contratoId,
  fornecedorOptanteSimples,
}: {
  contratoId: number;
  fornecedorOptanteSimples: boolean;
}) {
  const router = useRouter();
  const [resultado, setResultado] = useState<MedicaoComRetencoesDto | null>(null);
  const [erro, setErro] = useState<string | null>(null);
  const [carregando, setCarregando] = useState(false);

  async function handleSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setErro(null);
    setResultado(null);
    setCarregando(true);

    const fd = new FormData(e.currentTarget);
    const tipoServico = String(fd.get('tipoServico') ?? '').trim() || null;
    const aliquotaIss = String(fd.get('aliquotaIssMunicipal') ?? '').trim();
    const body = {
      contratoId,
      competencia: String(fd.get('competencia') ?? ''),
      valorMedido: String(fd.get('valorMedido') ?? ''),
      notaFiscal: String(fd.get('notaFiscal') ?? '').trim() || null,
      tipoServico,
      aliquotaIssMunicipal: aliquotaIss ? aliquotaIss : null,
    };

    try {
      const res = await fetch('/api/medicoes', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
      });
      const json = await res.json();
      if (!res.ok) {
        setErro(json.erro ?? `Erro ${res.status}`);
      } else {
        setResultado(json as MedicaoComRetencoesDto);
      }
    } catch (e) {
      setErro(e instanceof Error ? e.message : 'Erro desconhecido');
    } finally {
      setCarregando(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      <FormCard title="Dados da medição">
        <div className="grid gap-4 md:grid-cols-3">
          <Field
            label="Competência (YYYYMM)"
            name="competencia"
            required
            pattern="\d{6}"
            placeholder="202506"
            helper="Ex.: 202506 = junho/2026"
          />
          <Field
            label="Valor medido (R$)"
            name="valorMedido"
            type="number"
            step="0.01"
            min={0.01}
            required
            placeholder="10000.00"
          />
          <Field label="Nota fiscal" name="notaFiscal" placeholder="NF-12345" />
        </div>
      </FormCard>

      <FormCard
        title="Preview de retenções (opcional)"
        subtitle={
          fornecedorOptanteSimples
            ? 'Fornecedor é OPTANTE do Simples Nacional — apenas ISS será retido na fonte.'
            : 'Fornecedor não-optante — IRRF + INSS (se cessão MO) + PIS+COFINS+CSLL (se valor > R$ 215,05) + ISS.'
        }
      >
        <div className="grid gap-4 md:grid-cols-2">
          <Select
            label="Tipo de serviço"
            name="tipoServico"
            options={[
              { value: '', label: '— Sem preview —' },
              ...TIPOS_SERVICO_RETENCAO.map((t) => ({ value: t, label: t.replaceAll('_', ' ') })),
            ]}
            helper="LIMPEZA_CONSERVACAO, ENGENHARIA, VIGILANCIA, TRANSPORTE_CARGAS, MANUTENCAO_PREDIAL, OBRAS_CIVIS disparam INSS 11%."
          />
          <Field
            label="Alíquota ISS municipal (%)"
            name="aliquotaIssMunicipal"
            type="number"
            step="0.01"
            min={0}
            max={10}
            placeholder="5.0"
          />
        </div>
      </FormCard>

      <ErrorBanner message={erro} />

      <div className="flex items-center justify-end gap-3">
        <button
          type="submit"
          disabled={carregando}
          className="rounded bg-brand px-5 py-2 font-semibold text-brand-fg hover:bg-brand/90 disabled:opacity-60"
        >
          {carregando ? 'Registrando…' : 'Registrar medição'}
        </button>
      </div>

      {resultado && (
        <section className="rounded-lg border-2 border-success/40 bg-success/5 p-6">
          <p className="text-sm font-semibold uppercase tracking-wider text-success">MEDIÇÃO REGISTRADA</p>
          <p className="mt-1 text-sm">
            #{resultado.medicao.id} · Competência {resultado.medicao.competencia} ·{' '}
            <strong>{fmt.money(resultado.medicao.valorMedido)}</strong>
          </p>

          {resultado.retencoesPreview && (
            <div className="mt-5">
              <h3 className="text-sm font-semibold uppercase tracking-wider text-slate-700">
                Preview de retenções (não persistido)
              </h3>
              <div className="mt-3 grid grid-cols-2 gap-2 sm:grid-cols-4">
                <Linha label="Bruto" valor={resultado.retencoesPreview.valorBruto} />
                <Linha label="IRRF" valor={resultado.retencoesPreview.irrf} />
                <Linha label="INSS" valor={resultado.retencoesPreview.inss} />
                <Linha label="ISS" valor={resultado.retencoesPreview.iss} />
                <Linha label="PIS" valor={resultado.retencoesPreview.pis} />
                <Linha label="COFINS" valor={resultado.retencoesPreview.cofins} />
                <Linha label="CSLL" valor={resultado.retencoesPreview.csll} />
                <Linha label="DAS" valor={resultado.retencoesPreview.das} />
              </div>
              <div className="mt-4 grid gap-2 border-t border-slate-200 pt-4 sm:grid-cols-2">
                <p className="text-sm">
                  Total retido: <strong className="tabular-nums">{fmt.money(resultado.retencoesPreview.totalRetido)}</strong>
                </p>
                <p className="text-sm">
                  Líquido: <strong className="tabular-nums text-success">{fmt.money(resultado.retencoesPreview.valorLiquido)}</strong>
                </p>
              </div>

              <details className="mt-4">
                <summary className="cursor-pointer text-xs text-slate-500">
                  Memória de cálculo ({resultado.retencoesPreview.memoria.length} itens)
                </summary>
                <ul className="mt-2 space-y-1 text-xs text-slate-600">
                  {resultado.retencoesPreview.memoria.map((m, i) => (
                    <li key={i}>
                      <strong>{m.tributo}</strong> — {m.observacao ?? `${m.aliquota}% × ${fmt.money(m.base)} = ${fmt.money(m.valor)}`}
                      {m.baseLegal && <span className="ml-2 text-slate-400">({m.baseLegal})</span>}
                    </li>
                  ))}
                </ul>
              </details>
            </div>
          )}

          <div className="mt-5 flex gap-3">
            <button
              type="button"
              onClick={() => router.push(`/contratos/${contratoId}`)}
              className="rounded bg-brand px-4 py-2 text-sm font-semibold text-brand-fg hover:bg-brand/90"
            >
              Voltar ao contrato
            </button>
            <button
              type="button"
              onClick={() => { setResultado(null); router.refresh(); }}
              className="rounded border border-slate-300 px-4 py-2 text-sm hover:bg-slate-50"
            >
              Registrar outra
            </button>
          </div>
        </section>
      )}
    </form>
  );
}

function Linha({ label, valor }: { label: string; valor: string }) {
  const num = parseFloat(valor);
  return (
    <div className={`rounded border ${num > 0 ? 'border-warning/40 bg-warning/5' : 'border-slate-200 bg-white'} p-2`}>
      <p className="text-xs uppercase text-slate-500">{label}</p>
      <p className="font-mono text-sm tabular-nums">{fmt.money(valor)}</p>
    </div>
  );
}
