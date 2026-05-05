import type { Metadata } from 'next';
import Link from 'next/link';

const NAV = [
  { href: '/transparencia',                label: 'Visão geral' },
  { href: '/transparencia/fundeb',         label: 'Fundeb / MDE' },
  { href: '/transparencia/calculos',       label: 'Cálculos CAQ' },
  { href: '/transparencia/contratos',      label: 'Contratos' },
  { href: '/transparencia/despesas',       label: 'Despesas' },
  { href: '/transparencia/notificacoes',   label: 'Notificações' },
] as const;

export const metadata: Metadata = {
  title: { default: 'Transparência', template: '%s · Transparência · CAQ/CAQi' },
  description:
    'Portal público de transparência ativa em educação municipal — execução do Fundeb/MDE, ' +
    'cálculo do Custo Aluno Qualidade (CAQ/CAQi), contratos, despesas e notificações de compliance. ' +
    'Lei 12.527/2011 (LAI) · LRF art. 48-A · Lei 14.113/2020.',
  // Sobrescreve o robots noindex do RootLayout — esta árvore É indexável.
  robots: { index: true, follow: true },
};

export default function TransparenciaLayout({ children }: { children: React.ReactNode }) {
  const municipio = process.env.CAQI_MUNICIPIO_NOME ?? 'Município Exemplo';

  return (
    <div className="min-h-screen flex flex-col">
      <a
        href="#conteudo"
        className="sr-only focus:not-sr-only focus:absolute focus:left-2 focus:top-2 focus:rounded focus:bg-brand focus:px-3 focus:py-2 focus:text-brand-fg"
      >
        Pular para o conteúdo
      </a>

      <header className="bg-brand text-brand-fg shadow">
        <div className="container mx-auto flex flex-wrap items-center gap-x-6 gap-y-2 px-6 py-4">
          <Link href="/transparencia" className="text-lg font-bold tracking-tight">
            Transparência · {municipio}
          </Link>
          <nav aria-label="Seções do portal de transparência" className="flex flex-wrap gap-x-5 gap-y-2">
            {NAV.map((it) => (
              <Link
                key={it.href}
                href={it.href}
                className="rounded px-2 py-1 text-sm text-brand-fg/85 transition hover:bg-white/10 hover:text-brand-fg focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-fg"
              >
                {it.label}
              </Link>
            ))}
          </nav>
          <Link
            href="/login"
            className="ml-auto rounded border border-brand-fg/30 px-3 py-1 text-xs font-semibold text-brand-fg/85 hover:bg-white/10"
          >
            Acesso administrativo
          </Link>
        </div>
      </header>

      <main id="conteudo" tabIndex={-1} className="container mx-auto flex-1 px-6 py-8">
        {children}
      </main>

      <footer className="border-t border-slate-200 bg-white">
        <div className="container mx-auto space-y-3 px-6 py-8 text-xs text-slate-600">
          <p className="font-semibold uppercase tracking-wider text-slate-700">
            Transparência ativa — Lei 12.527/2011 · LRF art. 48-A
          </p>
          <p>
            Os dados aqui publicados são de natureza pública, atualizados conforme a execução
            orçamentária do município. Eventuais inconsistências podem ser questionadas via
            Sistema de Informações ao Cidadão (SIC) do município ou ao Conselho Municipal de
            Educação (CME) e CACS-Fundeb.
          </p>
          <p>
            Conformidade: CF/88 · LDB · PNE · Lei 14.113/2020 (Fundeb) · LRF · LAI · SIAFIC ·
            Lei 14.133/2021 · LGPD. Dados pessoais não são publicados (LGPD art. 11).
          </p>
          <p className="text-slate-400">Cache server-side: 5 min · Conteúdo destinado a controle social.</p>
        </div>
      </footer>
    </div>
  );
}
