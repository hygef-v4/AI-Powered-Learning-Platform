import type { Metadata } from "next";
import { headers } from "next/headers";
import type { ReactNode } from "react";
import "./globals.css";

export const metadata: Metadata = {
  title: "Nền tảng học tập AI",
  description: "AI-Powered Learning Platform",
};

export default async function RootLayout({ children }: { children: ReactNode }) {
  // Đọc header để trang render theo từng request: nonce CSP (src/proxy.ts) khác nhau mỗi request.
  await headers();
  return (
    <html lang="vi">
      <body>{children}</body>
    </html>
  );
}
