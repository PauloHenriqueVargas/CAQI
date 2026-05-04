'use client';

import { signOut } from 'next-auth/react';

export function SignOutButton() {
  return (
    <button
      type="button"
      onClick={() => signOut({ callbackUrl: '/login' })}
      className="ml-auto rounded px-3 py-1 text-sm text-brand-fg/80 hover:bg-white/10 hover:text-brand-fg"
    >
      Sair
    </button>
  );
}
