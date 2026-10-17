import type { ReactNode } from "react";
import { BrandLogo } from "./BrandLogo";

/**
 * Khung hai cột của các màn xác thực theo mock-up: form bên trái, panel gradient xanh-tím bên phải với khẩu hiệu.
 * Màn hẹp hơn `lg` chỉ còn cột form.
 */
export function AuthLayout({ children, aside }: { children: ReactNode; aside?: ReactNode }) {
  return (
    <div className="flex min-h-screen bg-slate-50">
      <div className="flex w-full flex-col bg-white px-6 py-6 sm:px-12 lg:w-[45%]">
        <BrandLogo />
        <main className="mx-auto flex w-full max-w-sm flex-1 flex-col justify-center py-10">{children}</main>
        <footer className="border-t border-slate-100 pt-4 text-xs text-slate-500">© 2026 Nền tảng học tập</footer>
      </div>
      <div className="hidden flex-1 p-4 lg:flex">
        <section className="relative flex w-full flex-col justify-between overflow-hidden rounded-2xl bg-linear-to-br from-[#2f3fb8] via-[#3d3fa6] to-[#8e4f8f] p-10 text-white">
          <div className="flex justify-end">{aside}</div>
          <div className="max-w-md">
            <p className="text-3xl leading-tight font-bold">Học, nộp bài và nhận điểm — một nơi.</p>
            <p className="mt-3 text-sm text-white/80">
              Hệ thống đào tạo số hiện đại tích hợp theo dõi tiến độ và đánh giá tự động chuẩn đại học.
            </p>
          </div>
        </section>
      </div>
    </div>
  );
}
