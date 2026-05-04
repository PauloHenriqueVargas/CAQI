import Link from 'next/link';

import { fetchService } from '@/lib/api-client';
import type { FornecedorDto } from '@/lib/types';

import { ContratoForm } from './ContratoForm';

export default async function NovoContratoPage() {
  const fornecedores = await fetchService<FornecedorDto[]>(
    'financeiro',
    '/api/v1/financeiro/fornecedores',
  );

  return (
    <div className="max-w-3xl space-y-6">
      <Link href="/contratos" className="text-sm text-brand hover:underline">← Contratos</Link>
      <header>
        <h1 className="text-2xl font-bold">Novo contrato</h1>
        <p className="text-sm text-slate-500">
          Após criação, registre medições para cada competência. Lançamento de despesa
          relativa ao contrato é separado — ver <code>/despesas/nova</code> (em sprint futura).
        </p>
      </header>
      <ContratoForm fornecedores={fornecedores} />
    </div>
  );
}
