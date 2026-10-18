import type { ReactNode } from "react";
import { Icon } from "@/shared/ui/Icon";

type Variant = "login" | "flow";

/**
 * Khung hai cột của các màn chưa đăng nhập theo design G21: form bên trái, khối giới thiệu gradient bên phải.
 * `login` theo artboard `Main` (cột trái nền trắng, gradient 155°); `flow` theo các bước kích hoạt và khôi phục
 * (`c02a`–`c04`: cả trang nền nhạt, gradient chéo xanh–tím, khẩu hiệu lớn). Màn hẹp hơn `lg` chỉ còn cột form.
 */
export function AuthLayout({
  variant = "flow",
  aside,
  tagline,
  children,
}: {
  variant?: Variant;
  /** Thẻ trắng ở góc trên khối giới thiệu. */
  aside?: ReactNode;
  /** Dòng phụ dưới khẩu hiệu (màn Đăng nhập và Khôi phục mật khẩu có, màn Kích hoạt không). */
  tagline?: boolean;
  children: ReactNode;
}) {
  const login = variant === "login";
  return (
    <div className={`flex min-h-screen flex-col ${login ? "bg-shell" : "bg-[#f8fafc]"}`}>
      <div className="flex flex-1">
        <div className={`flex w-full flex-col px-6 py-8 sm:px-12 lg:w-1/2 ${login ? "bg-white" : ""}`}>
          <Brand login={login} />
          <main className={`mx-auto flex w-full flex-1 flex-col justify-center py-10 ${login ? "max-w-[380px]" : "max-w-[440px]"}`}>
            {children}
          </main>
          {login && <Footer />}
        </div>
        <div className="hidden flex-1 p-6 lg:flex">
          <section
            className="relative flex w-full flex-col justify-between overflow-hidden rounded-[26px] p-10 text-white"
            style={{
              backgroundImage: login
                ? "linear-gradient(155deg, #1a31a8 0%, #2f45b5 55%, #e36d67 125%)"
                : "linear-gradient(to right bottom, #25399d, #2f45b5, #7b4282)",
            }}
          >
            <span aria-hidden="true" className="absolute -top-24 -right-24 size-80 rounded-full bg-white/[.03]" />
            <span aria-hidden="true" className="absolute bottom-24 -left-28 size-80 rounded-full border border-white/10" />
            <div className="relative flex justify-end">{aside}</div>
            <div className="relative max-w-[520px]">
              <h2
                className={
                  login
                    ? "text-[28px] leading-tight font-bold"
                    : "text-[40px] leading-[1.05] font-extrabold tracking-[-1px]"
                }
              >
                Học, nộp bài và nhận điểm — một nơi.
              </h2>
              {(login || tagline) && (
                <p className="mt-3 max-w-[440px] text-[13px] leading-relaxed text-white/80">
                  Hệ thống đào tạo số hiện đại tích hợp theo dõi tiến độ và đánh giá tự động chuẩn đại học.
                </p>
              )}
            </div>
          </section>
        </div>
      </div>
      {!login && (
        <div className="mx-6 border-t border-[#e2e8f0] py-4 sm:mx-12">
          <Footer />
        </div>
      )}
    </div>
  );
}

function Brand({ login }: { login: boolean }) {
  return (
    <div className="flex items-center gap-3">
      <span
        className={`flex items-center justify-center text-white ${
          login ? "size-9 rounded-lg bg-[#0e2a9e]" : "size-10 rounded-xl bg-primary"
        }`}
      >
        <Icon name="school" filled size={login ? 20 : 22} />
      </span>
      <span className={`font-bold text-ink ${login ? "text-base" : "text-xl tracking-[-0.5px]"}`}>Nền tảng học tập</span>
    </div>
  );
}

function Footer() {
  return <p className="text-xs text-[#64748b]">© 2026 Nền tảng học tập</p>;
}
