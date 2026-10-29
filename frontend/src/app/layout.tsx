import type { Metadata } from "next";
import { Be_Vietnam_Pro } from "next/font/google";
import type { ReactNode } from "react";
import "@/features/register";
import { ClientRegistrations } from "./ClientRegistrations";
import "./globals.css";

// Font tự host khi build (next/font), không tải từ Google lúc chạy.
const beVietnam = Be_Vietnam_Pro({
  subsets: ["latin", "vietnamese"],
  weight: ["400", "500", "600", "700", "800"],
  variable: "--font-be-vietnam",
  display: "swap",
});

export const metadata: Metadata = {
  title: "Nền tảng học tập",
  description: "Nền tảng học tập có hỗ trợ AI",
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="vi" className={beVietnam.variable}>
      <body>
        <ClientRegistrations />
        {children}
      </body>
    </html>
  );
}
