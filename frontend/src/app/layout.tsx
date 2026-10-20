import type { Metadata } from "next";
import type { ReactNode } from "react";
import "@/features/register";
import { ClientRegistrations } from "./ClientRegistrations";
import "./globals.css";
import "./fonts.css";

export const metadata: Metadata = {
  title: "Nền tảng học tập",
  description: "Nền tảng học tập có hỗ trợ AI",
};

export default function RootLayout({ children }: { children: ReactNode }) {
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
