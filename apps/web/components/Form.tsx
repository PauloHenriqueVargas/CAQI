import { ReactNode } from 'react';

/**
 * Componentes de formulário reutilizáveis. Server-safe (sem hooks).
 * Estilo: Tailwind básico — usar com server actions ou client forms.
 */

export function Field({
  label,
  name,
  type = 'text',
  required,
  defaultValue,
  placeholder,
  pattern,
  step,
  min,
  max,
  helper,
}: {
  label: string;
  name: string;
  type?: 'text' | 'number' | 'date';
  required?: boolean;
  defaultValue?: string | number;
  placeholder?: string;
  pattern?: string;
  step?: string;
  min?: number | string;
  max?: number | string;
  helper?: string;
}) {
  return (
    <label className="block">
      <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">
        {label}
        {required && <span aria-label="obrigatório" className="ml-1 text-danger">*</span>}
      </span>
      <input
        name={name}
        type={type}
        required={required}
        defaultValue={defaultValue}
        placeholder={placeholder}
        pattern={pattern}
        step={step}
        min={min}
        max={max}
        className="mt-1 block w-full rounded border border-slate-300 px-3 py-2 focus:border-brand focus:outline-none focus:ring-1 focus:ring-brand"
      />
      {helper && <p className="mt-1 text-xs text-slate-500">{helper}</p>}
    </label>
  );
}

export function Select({
  label,
  name,
  options,
  required,
  defaultValue,
  helper,
}: {
  label: string;
  name: string;
  options: Array<{ value: string; label: string }>;
  required?: boolean;
  defaultValue?: string;
  helper?: string;
}) {
  return (
    <label className="block">
      <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">
        {label}
        {required && <span aria-label="obrigatório" className="ml-1 text-danger">*</span>}
      </span>
      <select
        name={name}
        required={required}
        defaultValue={defaultValue}
        className="mt-1 block w-full rounded border border-slate-300 px-3 py-2 focus:border-brand focus:outline-none focus:ring-1 focus:ring-brand"
      >
        {options.map((o) => (
          <option key={o.value} value={o.value}>
            {o.label}
          </option>
        ))}
      </select>
      {helper && <p className="mt-1 text-xs text-slate-500">{helper}</p>}
    </label>
  );
}

export function FormCard({ title, subtitle, children }: { title: string; subtitle?: string; children: ReactNode }) {
  return (
    <section className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
      <h2 className="text-lg font-semibold">{title}</h2>
      {subtitle && <p className="mt-1 text-sm text-slate-500">{subtitle}</p>}
      <div className="mt-5 space-y-4">{children}</div>
    </section>
  );
}

export function ErrorBanner({ message }: { message: string | null | undefined }) {
  if (!message) return null;
  return (
    <p role="alert" className="rounded border border-danger/30 bg-danger/5 p-3 text-sm text-danger">
      {message}
    </p>
  );
}
