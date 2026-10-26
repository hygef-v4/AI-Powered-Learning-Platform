"use client";

import { useEffect } from "react";
import { cx } from "@/shared/ui/cx";
import { Icon } from "@/shared/ui/Icon";

export type AuthNotice = { id: number; text: string; tone: "success" | "warning" };

const VISIBLE_MS = 4000;

/** Thông báo một câu ngắn ở đầu màn chưa đăng nhập, tự ẩn sau vài giây; đổi `id` để hiện lại cùng một câu. */
export function AuthToast({ notice, onDone }: { notice: AuthNotice | null; onDone: () => void }) {
  useEffect(() => {
    if (!notice) return;
    const timer = setTimeout(onDone, VISIBLE_MS);
    return () => clearTimeout(timer);
  }, [notice, onDone]);

  if (!notice) return null;
  return (
    <div className="pointer-events-none fixed inset-x-0 top-5 z-50 flex justify-center px-4">
      <p
        key={notice.id}
        role="status"
        className={cx(
          "flex animate-pop items-center gap-2 rounded-full border bg-surface px-4 py-2 text-[13px] font-semibold shadow-[var(--shadow-float)]",
          notice.tone === "success" ? "border-success-soft text-success" : "border-warning-soft text-warning",
        )}
      >
        <Icon name={notice.tone === "success" ? "check_circle" : "warning"} size={16} />
        {notice.text}
      </p>
    </div>
  );
}
