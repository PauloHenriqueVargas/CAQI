import type { Metadata } from 'next';

import { fetchPublic } from '@/lib/api-public';
import type { ContratoDto } from '@/lib/types';
import { fmt } from '@/lib/types';

export const dynamic = 'force-dynamic';

export const metadata: Metadata = {
  title: 'Contratos firmados',
  description:
    'Lista pública de contratos firmados pela administração — modalidade, fornecedor, ' +
    'valor global, data de assinatura. Lei 14.133/2021 + LRF art. 48 §1°.',
};

export default async function ContratosPublicos() {
  const contratos = (await fetchPublic<ContratoDto[]>('financeiro', '/api/public/transparencia/contratos')) ?? [];

  const total = contratos.reduce((acc, c) => acc + parseFloat(c.valorGlobal || '0'), 0);

  return (
    <div className="space-y-6">
      <header>
        <p className="text-xs font-semibold uppercase tracking-widest text-brand">
          Lei 14.133/2021 · Nova Lei de Licitações
        </p>
        <h1 className="text-2xl font-bold">Contratos firmados</h1>
        <p className="mt-1 max-w-3xl text-sm text-slate-600">
          {contratos.length === 0
            ? 'Nenhum contrato cadastrado.'
            : `${contratos.length} contrato(s) registrado(s) · valor global agregado ${fmt.money(total.toFixed(2))}.`}
        </p>
      </header>

      {contratos.length > 0 && (
        <div className="overflow-x-auto rounded-lg border border-slate-200 bg-white">
          <table className="w-full text-sm">
            <caption className="sr-only">Contratos firmados pela administração</caption>
            <thead className="bg-slate-50 text-xs uppercase tracking-wider text-slate-500">
              <tr>
                <th scope="col" className="px-4 py-2 text-left">ID</th>
                <th scope="col" className="px-4 py-2 text-left">Objeto</th>
                <th scope="col" className="px-4 py-2 text-left">Modalidade</th>
                <th scope="col" className="px-4 py-2 text-left">Assinatura</th>
                <th scope="col" className="px-4 py-2 text-right">Valor global</th>
                <th scope="col" className="px-4 py-2 text-left">PNCP</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {contratos.map((c) => (
                <tr key={c.id}>
                  <td className="px-4 py-2 tabular-nums text-slate-500">#{c.id}</td>
                  <td className="px-4 py-2">{c.objeto}</td>
                  <td className="px-4 py-2">
                    <span className="rounded bg-slate-100 px-2 py-0.5 text-xs font-medium uppercase tracking-wider text-slate-700">
                      {c.modalidade.replace(/_/g, ' ').toLowerCase()}
                    </span>
                  </td>
                  <td className="px-4 py-2">{fmt.date(c.dataAssinatura)}</td>
                  <td className="px-4 py-2 text-right tabular-nums">{fmt.money(c.valorGlobal)}</td>
                  <td className="px-4 py-2 font-mono text-xs">
                    {c.pncpId ?? <span className="text-slate-400">—</span>}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <section className="rounded border border-slate-200 bg-white p-6 text-sm text-slate-600">
        <h2 className="text-sm font-semibold uppercase tracking-wider text-slate-500">
          Sobre os contratos publicados
        </h2>
        <p className="mt-2">
          Apenas dados públicos por natureza são exibidos: identificação do contrato, objeto,
          modalidade de licitação (Lei 14.133/2021), valor global e identificador PNCP quando
          aplicável. CPF/dados pessoais de pessoa física não constam aqui (LGPD art. 11);
          dados sigilosos podem ser solicitados via SIC.
        </p>
      </section>
    </div>
  );
}
