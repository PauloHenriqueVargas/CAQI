import Link from 'next/link';

import { CalculoForm } from './CalculoForm';

export default function NovoCalculoPage() {
  return (
    <div className="max-w-2xl space-y-6">
      <Link href="/calculos" className="text-sm text-brand hover:underline">← Cálculos</Link>

      <header>
        <h1 className="text-2xl font-bold">Executar cálculo CAQ/CAQi</h1>
        <p className="text-sm text-slate-500">
          Dispara o motor do <code>caq-engine-svc</code>. O cálculo é persistido em <code>calculo_caq</code>
          {' '}e dispara um evento RabbitMQ consumido pelo <code>caq-compliance-svc</code> (auditoria).
        </p>
      </header>

      <CalculoForm />
    </div>
  );
}
