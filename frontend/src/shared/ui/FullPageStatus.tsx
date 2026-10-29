import type { ReactNode } from "react";
import { InkMark } from "./PageStatus";

/**
 * Màn chờ hoặc báo lỗi cả trang trước khi có khung (tải phiên, chuyển trang): đang chờ thì bút mài nghiên ở giữa, không chữ
 * (chữ chỉ cho trình đọc màn hình); lỗi thì bút gác lên nghiên, lời báo bên dưới.
 */
export function FullPageStatus({ tone = "muted", children }: { tone?: "muted" | "danger"; children: ReactNode }) {
  const failed = tone === "danger";
  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-4 bg-shell">
      <InkMark state={failed ? "resting" : "grinding"} />
      <p role={failed ? "alert" : "status"} className={failed ? "max-w-sm text-center text-[14px] text-body" : "sr-only"}>
        {children}
      </p>
    </div>
  );
}
