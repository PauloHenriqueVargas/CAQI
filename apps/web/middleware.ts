export { default } from 'next-auth/middleware';

export const config = {
  matcher: [
    /*
     * Protege tudo EXCETO:
     *   /login                  — formulário público
     *   /api/auth               — handlers NextAuth
     *   /api/health             — health probe
     *   /_next, /favicon, etc.  — assets/SSR
     */
    '/((?!login|api/auth|api/health|_next/static|_next/image|favicon.ico|robots.txt).*)',
  ],
};
