import type { Metadata } from "next";
import { IBM_Plex_Mono, Space_Grotesk } from "next/font/google";
import "./globals.css";

const display = Space_Grotesk({
  subsets: ["latin"],
  variable: "--font-display",
});

const mono = IBM_Plex_Mono({
  subsets: ["latin"],
  weight: ["400", "500", "600"],
  variable: "--font-mono",
});

export const metadata: Metadata = {
  title: "SIFAP 2.0",
  description: "Painel moderno do SIFAP com backend Spring Boot e frontend Next.js 15.",
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="pt-BR">
      <body className={`${display.variable} ${mono.variable}`} style={{ fontFamily: "var(--font-display)" }}>
        <div className="shell">
          <header className="topbar">
            <div className="brand">
              <span className="eyebrow">SIFAP 2.0</span>
              <h1 className="title">Orquestra de Beneficios Sociais</h1>
            </div>
            <nav className="nav">
              <a href="/">Visao geral</a>
              <a href="/programas">Programas</a>
              <a href="/pagamentos">Simulador</a>
            </nav>
          </header>
          {children}
        </div>
      </body>
    </html>
  );
}