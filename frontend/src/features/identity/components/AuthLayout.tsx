"use client";

import type { ReactNode } from "react";
import { useTheme } from "@/shared/theme";
import { Icon } from "@/shared/ui/Icon";
import { Logo, LogoMark } from "@/shared/ui/Logo";

type Variant = "login" | "flow";

/**
 * Khung các màn chưa đăng nhập: cột form bên trái; bên phải là một tấm tranh tĩnh (dấu 学 lớn, câu khẩu hiệu chữ có
 * chân, lưới kẻ mảnh như trang vở). Màn hẹp hơn `lg` chỉ còn cột form. `aside` và `tagline` giữ để tương thích, giao
 * diện tối giản không vẽ thẻ giới thiệu.
 */
export function AuthLayout({
  children,
}: {
  variant?: Variant;
  aside?: ReactNode;
  tagline?: boolean;
  children: ReactNode;
}) {
  const [theme, toggleTheme] = useTheme();
  return (
    <div className="flex min-h-screen bg-shell">
      <div className="flex w-full flex-col px-6 py-7 sm:px-12 lg:w-[46%]">
        <div className="flex items-center justify-between">
          <Logo />
          <button
            type="button"
            onClick={toggleTheme}
            aria-label={theme === "dark" ? "Chuyển giao diện sáng" : "Chuyển giao diện tối"}
            className="inline-flex size-9 items-center justify-center rounded-lg text-body transition-colors hover:bg-sunken hover:text-ink"
          >
            <Icon name={theme === "dark" ? "sun" : "moon"} size={17} />
          </button>
        </div>
        <main className="mx-auto flex w-full max-w-[380px] flex-1 animate-rise flex-col justify-center py-12">{children}</main>
        <p className="text-xs text-muted">© 2026 Nền tảng học tập</p>
      </div>
      <div className="hidden flex-1 p-4 lg:flex">
        <section
          aria-hidden="true"
          className="relative flex w-full flex-col justify-end overflow-hidden rounded-2xl border border-line bg-surface p-14"
          style={{
            backgroundImage:
              "linear-gradient(var(--c-line-soft) 1px, transparent 1px), linear-gradient(90deg, var(--c-line-soft) 1px, transparent 1px)",
            backgroundSize: "32px 32px",
          }}
        >
          <div className="absolute top-14 right-14 animate-[pop_0.8s_cubic-bezier(.16,1,.3,1)_backwards] [animation-delay:150ms]">
            <LogoMark size={148} className="-rotate-6 drop-shadow-[0_10px_24px_rgb(232_131_58/0.25)]" />
          </div>
          <p className="relative max-w-[520px] font-display text-[52px] leading-[1.05] tracking-[-0.03em] text-ink">
            Học, nộp bài và nhận điểm <i className="text-primary-ink">một nơi.</i>
          </p>
        </section>
      </div>
    </div>
  );
}
