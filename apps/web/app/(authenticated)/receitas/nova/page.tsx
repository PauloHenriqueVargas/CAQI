import Link from 'next/link';

import { ReceitaForm } from './ReceitaForm';

export default function NovaReceitaPage() {
  return (
    <div className="max-w-2xl space-y-6">
      <Link href="/receitas" className="text-sm text-brand hover:underline">← Receitas</Link>
      <header>
        <h1 className="text-2xl font-bold">Nova receita</h1>
        <p className="text-sm text-slate-500">
          Lançamento agregado por competência (YYYYMM). Cada origem entra em diferentes
          bases de cálculo das vinculações constitucionais e legais.
        </p>
      </header>
      <ReceitaForm />
    </div>
  );
}
