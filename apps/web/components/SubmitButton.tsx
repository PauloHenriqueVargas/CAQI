'use client';

import { useFormStatus } from 'react-dom';
import { ReactNode } from 'react';

/**
 * Botão de submit que reflete o pending state da Server Action.
 * Usar dentro de <form action={serverAction}>.
 */
export function SubmitButton({ children, variant = 'primary' }: { children: ReactNode; variant?: 'primary' | 'secondary' }) {
  const { pending } = useFormStatus();

  const base = 'rounded px-5 py-2 font-semibold disabled:opacity-60 disabled:cursor-not-allowed';
  const styles =
    variant === 'primary'
      ? `${base} bg-brand text-brand-fg hover:bg-brand/90`
      : `${base} border border-slate-300 bg-white hover:bg-slate-50`;

  return (
    <button type="submit" disabled={pending} className={styles}>
      {pending ? 'Enviando…' : children}
    </button>
  );
}
