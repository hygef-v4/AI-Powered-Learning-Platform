import type { Metadata } from "next";
import type { ReactNode } from "react";
import "@/features/register";
import "./globals.css";

export const metadata: Metadata = {
  title: "ALAP",
  description: "Nền tảng học tập có hỗ trợ AI",
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="vi">
      <body>{children}</body>
    </html>
  );
}
