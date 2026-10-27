"use client";

import type { ReactNode } from "react";
import { useTheme } from "@/shared/theme";
import { Icon } from "@/shared/ui/Icon";
import { HankoMark, Logo } from "@/shared/ui/Logo";

type Variant = "login" | "flow";

/**
 * Khung các màn chưa đăng nhập: nền giấy ô li phủ cả màn, form nằm trong một thẻ cùng màu nền ở giữa, quanh thẻ là vài đồ dùng học
 * tập vẽ phẳng để lệch nhau (dấu 学, giấy nhớ, bút chì). Đồ trang trí chỉ hiện từ `sm`. `variant`, `aside` và
 * `tagline` giữ để tương thích.
 */
export function AuthLayout({
  toast,
  children,
}: {
  variant?: Variant;
  aside?: ReactNode;
  tagline?: boolean;
  /** Thông báo ngắn tự ẩn (`AuthToast`), đặt ngoài thẻ form. */
  toast?: ReactNode;
  children: ReactNode;
}) {
  const [theme, toggleTheme] = useTheme();
  return (
    <div className="paper-grid relative flex min-h-dvh flex-col overflow-hidden">
      {toast}
      <header className="flex items-center justify-between px-6 py-5 sm:px-10">
        <Logo />
        <button
          type="button"
          onClick={toggleTheme}
          aria-label={theme === "dark" ? "Chuyển giao diện sáng" : "Chuyển giao diện tối"}
          className="inline-flex size-10 items-center justify-center rounded-lg text-body transition-colors hover:bg-sunken hover:text-ink"
        >
          <Icon name={theme === "dark" ? "sun" : "moon"} size={17} />
        </button>
      </header>
      <main className="flex flex-1 items-center justify-center px-4 py-10 sm:py-16">
        <div className="relative w-full max-w-[480px]">
          <Decorations />
          <div className="relative animate-pop rounded-2xl border border-line-strong bg-shell px-6 py-8 shadow-[var(--shadow-float)] sm:px-9 sm:py-10">
            {children}
          </div>
        </div>
      </main>
      <footer className="px-6 pb-6 text-center text-xs text-muted">© 2026 Nền tảng học tập</footer>
    </div>
  );
}

/** Đồ dùng học tập quanh thẻ form; nằm dưới thẻ, không nhận chuột, trình đọc màn hình bỏ qua. */
function Decorations() {
  return (
    <div aria-hidden="true" className="pointer-events-none absolute inset-0 hidden sm:block">
      {/* Ba món để bừa như trên bàn học: lệch vị trí, nghiêng mỗi cái một kiểu, có món chui một phần dưới thẻ. */}
      {/* Dấu 学 đóng lệch, chạm mép phải phía trên */}
      <div className="absolute -top-9 -right-10 rotate-[14deg] lg:-right-16">
        <HankoMark size={76} />
      </div>

      {/* Giấy nhớ dán chéo bên trái, cao hơn giữa thẻ */}
      <svg viewBox="0 0 96 96" className="absolute top-[18%] -left-20 size-24 -rotate-[17deg] lg:-left-36 lg:size-28">
        <path d="M8 8 H88 V68 L68 88 H8 Z" className="fill-primary-soft stroke-line-strong" strokeWidth="1.5" />
        <path d="M68 88 V68 H88" className="fill-[var(--c-tier-3)] stroke-line-strong" strokeWidth="1.5" strokeLinejoin="round" />
        <path d="M22 32 H74 M22 46 H74 M22 60 H54" className="stroke-primary-ink" strokeWidth="3" strokeLinecap="round" opacity="0.55" />
      </svg>

      {/* Bút chì lăn dưới đáy thẻ, đầu nhọn ló ra bên trái */}
      <svg viewBox="0 0 160 40" className="absolute -bottom-10 -left-12 w-44 rotate-[9deg] lg:-left-24 lg:w-52">
        <rect x="26" y="8" width="104" height="24" rx="3" className="fill-primary" />
        <rect x="26" y="8" width="104" height="8" className="fill-primary-strong" opacity="0.6" />
        <rect x="130" y="8" width="10" height="24" className="fill-line-strong" />
        <rect x="140" y="8" width="14" height="24" rx="4" className="fill-danger-soft stroke-danger-line" strokeWidth="1.5" />
        <path d="M26 8 L4 20 L26 32 Z" className="fill-[var(--c-tier-2)] stroke-line-strong" strokeWidth="1.5" strokeLinejoin="round" />
        <path d="M11 16.2 L4 20 L11 23.8 Z" className="fill-ink" />
      </svg>
    </div>
  );
}
