import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: {
    default: "Sistema CAQ/CAQi",
    template: "%s · Sistema CAQ/CAQi",
  },
  description:
    "Gestão do Custo Aluno Qualidade (CAQ/CAQi) com conformidade legal nativa.",
  robots: { index: false, follow: false }, // ambiente proprietário
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="pt-BR" className="h-full">
      <body className="min-h-full bg-slate-50 text-slate-900 antialiased">
        {children}
      </body>
    </html>
  );
}
