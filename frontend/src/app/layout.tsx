import type { Metadata } from "next";
import type { ReactNode } from "react";
import { connection } from "next/server";
import "@/features/register";
import { ClientRegistrations } from "./ClientRegistrations";
import "./globals.css";
import "./fonts.css";

export const metadata: Metadata = {
  title: "Nền tảng học tập",
  description: "Nền tảng học tập có hỗ trợ AI",
};

export default async function RootLayout({ children }: { children: ReactNode }) {
  // CSP nonces must be attached during request rendering, never static generation.
  await connection();
  return (
    <html lang="vi" className="font-be-vietnam">
      <head>
        {[400, 500, 600, 700, 800].flatMap((weight) =>
          ["latin", "vietnamese"].map((subset) => (
            <link
              key={`${weight}-${subset}`}
              rel="preload"
              href={`/fonts/be-vietnam-pro/be-vietnam-pro-${weight}-${subset}.woff2`}
              as="font"
              type="font/woff2"
              crossOrigin="anonymous"
            />
          )),
        )}
      </head>
      <body>
        <ClientRegistrations />
        {children}
      </body>
    </html>
  );
}
