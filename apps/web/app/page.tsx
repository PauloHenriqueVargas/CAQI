import { redirect } from 'next/navigation';

/**
 * Root: redireciona para o dashboard. Middleware decide entre dashboard
 * (autenticado) e /login (não autenticado).
 */
export default function HomePage() {
  redirect('/dashboard');
}
