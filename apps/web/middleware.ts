export { default } from 'next-auth/middleware';

export const config = {
  matcher: [
    /*
     * Protege tudo EXCETO:
     *   /login                  — formulário público
     *   /transparencia          — portal público (LAI / LRF art. 48-A)
     *   /api/auth               — handlers NextAuth
     *   /api/health             — health probe
     *   /sitemap.xml, /robots.txt — SEO públicos
     *   /_next, /favicon, etc.  — assets/SSR
     */
    '/((?!login|transparencia|api/auth|api/health|_next/static|_next/image|favicon.ico|robots.txt|sitemap.xml).*)',
  ],
};
