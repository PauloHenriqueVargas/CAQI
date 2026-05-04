import { ReactNode } from 'react';

export function Card({
  title,
  subtitle,
  children,
  variant = 'default',
}: {
  title?: string;
  subtitle?: string;
  children: ReactNode;
  variant?: 'default' | 'success' | 'warn' | 'danger';
}) {
  const accent = {
    default: 'border-slate-200',
    success: 'border-success',
    warn: 'border-warning',
    danger: 'border-danger',
  }[variant];

  return (
    <section className={`rounded-lg border-2 ${accent} bg-white p-5 shadow-sm`}>
      {title && <h3 className="text-sm font-semibold uppercase tracking-wider text-slate-500">{title}</h3>}
      {subtitle && <p className="mt-1 text-xs text-slate-400">{subtitle}</p>}
      <div className={title ? 'mt-3' : ''}>{children}</div>
    </section>
  );
}

export function StatNumber({ value, unit }: { value: string; unit?: string }) {
  return (
    <div>
      <span className="text-3xl font-bold tabular-nums">{value}</span>
      {unit && <span className="ml-2 text-sm text-slate-500">{unit}</span>}
    </div>
  );
}
