import Link from 'next/link';

import { fetchService } from '@/lib/api-client';
import type { ContratoDto, FornecedorDto } from '@/lib/types';

import { MedicaoForm } from './MedicaoForm';

export default async function NovaMedicaoPage({ params }: { params: { id: string } }) {
  const contrato = await fetchService<ContratoDto>(
    'financeiro',
    `/api/v1/financeiro/contratos/${params.id}`,
  );
  let fornecedor: FornecedorDto | null = null;
  try {
    fornecedor = await fetchService<FornecedorDto>(
      'financeiro',
      `/api/v1/financeiro/fornecedores/${contrato.fornecedorId}`,
    );
  } catch {
    /* segue sem */
  }

  return (
    <div className="max-w-4xl space-y-6">
      <Link href={`/contratos/${contrato.id}`} className="text-sm text-brand hover:underline">
        ← Contrato #{contrato.id}
      </Link>
      <header>
        <h1 className="text-2xl font-bold">Registrar medição</h1>
        <p className="text-sm text-slate-700">
          Contrato #{contrato.id} · {contrato.objeto}
        </p>
        {fornecedor && (
          <p className="text-xs text-slate-500">
            Fornecedor: <strong>{fornecedor.nome}</strong> · CNPJ {fornecedor.cnpj}
            {fornecedor.optanteSimples && ' · Optante Simples Nacional'}
          </p>
        )}
      </header>

      <MedicaoForm
        contratoId={contrato.id}
        fornecedorOptanteSimples={fornecedor?.optanteSimples ?? false}
      />
    </div>
  );
}
