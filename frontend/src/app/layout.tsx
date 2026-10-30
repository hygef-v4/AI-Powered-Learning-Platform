import type { Metadata } from "next";
import type { ReactNode } from "react";
import { headers } from "next/headers";
import { connection } from "next/server";
import "@/features/register";
import { THEME_BOOT_SCRIPT } from "@/shared/themeBoot";
import { Toaster } from "@/shared/ui/Toast";
import { ClientRegistrations } from "./ClientRegistrations";
import "../shared/styles/globals.css";
import "../shared/styles/fonts.css";

export const metadata: Metadata = {
  title: "Nền tảng học tập",
  description: "Nền tảng học tập có hỗ trợ AI",
};

const PRELOAD_FONTS = [
  "/fonts/be-vietnam-pro/be-vietnam-pro-latin-400-normal.woff2",
  "/fonts/be-vietnam-pro/be-vietnam-pro-vietnamese-400-normal.woff2",
];

export default async function RootLayout({ children }: { children: ReactNode }) {
  // Per-request rendering keeps framework scripts aligned with the CSP nonce.
  await connection();
  const nonce = (await headers()).get("x-nonce") ?? undefined;
  return (
    <html lang="vi" suppressHydrationWarning>
      <head>
        {/* Đặt theme trước khi vẽ để không nháy sáng khi đang dùng giao diện tối. */}
        <script nonce={nonce} dangerouslySetInnerHTML={{ __html: THEME_BOOT_SCRIPT }} />
        {PRELOAD_FONTS.map((href) => (
          <link key={href} rel="preload" href={href} as="font" type="font/woff2" crossOrigin="anonymous" />
        ))}
      </head>
      <body>
        <ClientRegistrations />
        {children}
        <Toaster />
      </body>
    </html>
  );
}
