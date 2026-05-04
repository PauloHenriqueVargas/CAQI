import Link from 'next/link';

import { fetchService } from '@/lib/api-client';
import type { ContratoDto, FornecedorDto, MedicaoDto } from '@/lib/types';
import { fmt } from '@/lib/types';

export const dynamic = 'force-dynamic';

export default async function ContratoDetalhePage({ params }: { params: { id: string } }) {
  const [contrato, medicoes] = await Promise.all([
    fetchService<ContratoDto>('financeiro', `/api/v1/financeiro/contratos/${params.id}`),
    fetchService<MedicaoDto[]>('financeiro', `/api/v1/financeiro/contratos/${params.id}/medicoes`),
  ]);

  // Carrega fornecedor para mostrar nome (se existir)
  let fornecedor: FornecedorDto | null = null;
  try {
    fornecedor = await fetchService<FornecedorDto>(
      'financeiro',
      `/api/v1/financeiro/fornecedores/${contrato.fornecedorId}`,
    );
  } catch {
    /* fornecedor pode ter sido removido — segue sem */
  }

  const totalMedido = medicoes.reduce((sum, m) => sum + parseFloat(m.valorMedido), 0);
  const valorGlobal = parseFloat(contrato.valorGlobal);
  const saldoRestante = valorGlobal - totalMedido;
  const pctExecutado = valorGlobal > 0 ? (totalMedido / valorGlobal) * 100 : 0;

  return (
    <div className="space-y-6">
      <Link href="/contratos" className="text-sm text-brand hover:underline">← Contratos</Link>

      <header>
        <h1 className="text-2xl font-bold">Contrato #{contrato.id}</h1>
        <p className="text-sm text-slate-700">{contrato.objeto}</p>
        <p className="mt-1 text-xs text-slate-500">
          {contrato.modalidade.replaceAll('_', ' ')} · Assinado em {fmt.date(contrato.dataAssinatura)}
          {contrato.pncpId && <> · PNCP: <code className="font-mono">{contrato.pncpId}</code></>}
        </p>
      </header>

      <section className="grid gap-4 md:grid-cols-3">
        <Card label="Valor global" value={fmt.money(contrato.valorGlobal)} />
        <Card label="Total medido" value={fmt.money(totalMedido)} sub={`${pctExecutado.toFixed(1)}% executado`} />
        <Card
          label="Saldo restante"
          value={fmt.money(saldoRestante)}
          variant={saldoRestante < 0 ? 'danger' : saldoRestante < valorGlobal * 0.1 ? 'warn' : 'default'}
        />
      </section>

      {fornecedor && (
        <section className="rounded-lg border border-slate-200 bg-white p-4">
          <h2 className="text-sm font-semibold uppercase tracking-wider text-slate-500">Fornecedor</h2>
          <p className="mt-1 text-sm">
            <strong>{fornecedor.nome}</strong> · CNPJ {fornecedor.cnpj}
            {fornecedor.optanteSimples && (
              <span className="ml-2 rounded bg-warning/15 px-2 py-0.5 text-xs text-warning">Simples Nacional</span>
            )}
            {fornecedor.municipio && <span className="ml-2 text-slate-400">· {fornecedor.municipio}</span>}
          </p>
        </section>
      )}

      <section>
        <header className="mb-3 flex items-baseline justify-between">
          <h2 className="text-lg font-semibold">Medições</h2>
          <Link
            href={`/contratos/${contrato.id}/medicoes/nova`}
            className="rounded bg-brand px-4 py-2 text-sm font-semibold text-brand-fg hover:bg-brand/90"
          >
            + Registrar medição
          </Link>
        </header>

        {medicoes.length === 0 ? (
          <p className="rounded border border-slate-200 bg-white p-4 text-sm text-slate-500">
            Nenhuma medição registrada para este contrato.
          </p>
        ) : (
          <div className="overflow-x-auto rounded border border-slate-200 bg-white shadow-sm">
            <table className="w-full text-sm">
              <thead className="border-b border-slate-200 bg-slate-50 text-left text-xs uppercase tracking-wider text-slate-500">
                <tr>
                  <th className="px-4 py-2">ID</th>
                  <th className="px-4 py-2">Competência</th>
                  <th className="px-4 py-2">Nota fiscal</th>
                  <th className="px-4 py-2 text-right">Valor medido</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {medicoes.map((m) => (
                  <tr key={m.id}>
                    <td className="px-4 py-2 font-mono text-xs">#{m.id}</td>
                    <td className="px-4 py-2 tabular-nums">{m.competencia}</td>
                    <td className="px-4 py-2 font-mono text-xs">{m.notaFiscal ?? '—'}</td>
                    <td className="px-4 py-2 text-right tabular-nums">{fmt.money(m.valorMedido)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}

function Card({
  label,
  value,
  sub,
  variant = 'default',
}: {
  label: string;
  value: string;
  sub?: string;
  variant?: 'default' | 'warn' | 'danger';
}) {
  const accent = {
    default: 'border-slate-200',
    warn: 'border-warning',
    danger: 'border-danger',
  }[variant];
  return (
    <div className={`rounded-lg border-2 ${accent} bg-white p-5`}>
      <p className="text-sm font-semibold uppercase tracking-wider text-slate-500">{label}</p>
      <p className="mt-2 text-2xl font-bold tabular-nums">{value}</p>
      {sub && <p className="mt-1 text-xs text-slate-500">{sub}</p>}
    </div>
  );
}
