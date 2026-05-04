import Link from 'next/link';

import { fetchService } from '@/lib/api-client';
import type { FornecedorDto } from '@/lib/types';

export const dynamic = 'force-dynamic';

export default async function FornecedoresPage() {
  const fornecedores = await fetchService<FornecedorDto[]>(
    'financeiro',
    '/api/v1/financeiro/fornecedores',
  );

  return (
    <div>
      <header className="mb-6 flex items-baseline justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold">Fornecedores</h1>
          <p className="text-sm text-slate-500">
            {fornecedores.length === 0
              ? 'Nenhum fornecedor cadastrado.'
              : `${fornecedores.length} fornecedores · regime define retenção tributária na fonte.`}
          </p>
        </div>
        <Link
          href="/fornecedores/novo"
          className="rounded bg-brand px-4 py-2 text-sm font-semibold text-brand-fg hover:bg-brand/90"
        >
          + Novo fornecedor
        </Link>
      </header>

      {fornecedores.length > 0 && (
        <div className="overflow-x-auto rounded border border-slate-200 bg-white shadow-sm">
          <table className="w-full text-sm">
            <thead className="border-b border-slate-200 bg-slate-50 text-left text-xs uppercase tracking-wider text-slate-500">
              <tr>
                <th className="px-4 py-2">ID</th>
                <th className="px-4 py-2">CNPJ</th>
                <th className="px-4 py-2">Nome</th>
                <th className="px-4 py-2">Município</th>
                <th className="px-4 py-2">Regime</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {fornecedores.map((f) => (
                <tr key={f.id} className="hover:bg-slate-50">
                  <td className="px-4 py-2 font-mono text-xs">#{f.id}</td>
                  <td className="px-4 py-2 font-mono">{f.cnpj}</td>
                  <td className="px-4 py-2 font-medium">{f.nome}</td>
                  <td className="px-4 py-2 text-slate-500">{f.municipio ?? '—'}</td>
                  <td className="px-4 py-2">
                    {f.optanteSimples ? (
                      <span className="rounded bg-warning/15 px-2 py-0.5 text-xs font-semibold text-warning">
                        Simples Nacional
                      </span>
                    ) : (
                      <span className="rounded bg-slate-100 px-2 py-0.5 text-xs text-slate-600">
                        Não-optante
                      </span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
