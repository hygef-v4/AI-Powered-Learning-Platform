import type { ReactNode } from "react";
import { LogoMark } from "./Logo";

/** Màn chờ hoặc báo lỗi cả trang (tải phiên, chuyển trang): dấu 学 nhịp nhẹ ở giữa, một dòng chữ bên dưới. */
export function FullPageStatus({ tone = "muted", children }: { tone?: "muted" | "danger"; children: ReactNode }) {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-4 bg-shell">
      <LogoMark size={40} className={tone === "muted" ? "animate-pulse" : undefined} />
      <p role={tone === "danger" ? "alert" : "status"} className={tone === "danger" ? "text-sm text-danger" : "text-sm text-muted"}>
        {children}
      </p>
    </div>
  );
}
