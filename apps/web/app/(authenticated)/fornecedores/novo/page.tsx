import Link from 'next/link';

import { FornecedorForm } from './FornecedorForm';

export default function NovoFornecedorPage() {
  return (
    <div className="max-w-2xl space-y-6">
      <Link href="/fornecedores" className="text-sm text-brand hover:underline">← Fornecedores</Link>
      <header>
        <h1 className="text-2xl font-bold">Novo fornecedor</h1>
        <p className="text-sm text-slate-500">
          Cadastro de pessoa jurídica para uso em contratos e despesas. A flag{' '}
          <code>optanteSimples</code> é fonte única da verdade — o sistema usa esse
          valor (não o que vier na requisição) ao calcular retenções.
        </p>
      </header>
      <FornecedorForm />
    </div>
  );
}
