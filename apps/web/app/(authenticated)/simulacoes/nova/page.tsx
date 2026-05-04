import Link from 'next/link';

import { SimulacaoForm } from './SimulacaoForm';

export default function NovaSimulacaoPage() {
  return (
    <div className="max-w-5xl space-y-6">
      <Link href="/simulacoes" className="text-sm text-brand hover:underline">← Simulações</Link>

      <header>
        <h1 className="text-2xl font-bold">Nova simulação</h1>
        <p className="text-sm text-slate-500">
          Aplica overrides hipotéticos e exibe a comparação atual vs simulado.
          Sem persistência.
        </p>
      </header>

      <SimulacaoForm />
    </div>
  );
}
