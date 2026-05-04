import Link from 'next/link';

import { SignOutButton } from '@/components/SignOutButton';

const NAV_ITEMS = [
  { href: '/dashboard',     label: 'Dashboard' },
  { href: '/calculos',      label: 'Cálculos CAQ' },
  { href: '/simulacoes',    label: 'Simulações' },
  { href: '/receitas',      label: 'Receitas' },
  { href: '/despesas',      label: 'Despesas' },
  { href: '/contratos',     label: 'Contratos' },
  { href: '/fornecedores',  label: 'Fornecedores' },
  { href: '/fundeb',        label: 'Fundeb / MDE' },
  { href: '/notificacoes',  label: 'Notificações' },
  { href: '/profile/mfa',   label: 'MFA' },
];

export default function AuthenticatedLayout({ children }: { children: React.ReactNode }) {
  const municipio = process.env.CAQI_MUNICIPIO_NOME ?? 'Município Exemplo';
  return (
    <div className="min-h-screen">
      <header className="bg-brand text-brand-fg shadow">
        <div className="container mx-auto flex flex-wrap items-center gap-x-6 gap-y-2 px-6 py-3">
          <Link href="/dashboard" className="text-lg font-bold tracking-tight">
            CAQ/CAQi · {municipio}
          </Link>
          <nav className="flex flex-wrap gap-x-5 gap-y-2">
            {NAV_ITEMS.map((it) => (
              <Link
                key={it.href}
                href={it.href}
                className="rounded px-2 py-1 text-sm text-brand-fg/85 transition hover:bg-white/10 hover:text-brand-fg"
              >
                {it.label}
              </Link>
            ))}
          </nav>
          <SignOutButton />
        </div>
      </header>

      <main className="container mx-auto px-6 py-8">{children}</main>

      <footer className="container mx-auto px-6 py-8 text-xs text-slate-400">
        Conformidade: CF/88 · LDB · PNE · Lei 14.113/2020 · LRF · LAI · SIAFIC · Lei 14.133/2021 · LGPD
      </footer>
    </div>
  );
}
