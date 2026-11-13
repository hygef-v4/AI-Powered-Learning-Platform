"use client";

import type { ReactNode } from "react";
import { Button } from "./Button";
import { cx } from "./cx";
import { INK_ORANGE, INK_TEAL } from "./Logo";

/**
 * Logo bút chấm nghiên to, có chuyển động: `grinding` là bút mài vòng trong nghiên, mặt mực gợn theo (đang tải); `resting`
 * là bút nhấc khỏi nghiên rồi gác nằm trên mép nghiên (tải lỗi). Máy bật giảm chuyển động thì đứng yên ở tư thế cuối.
 */
export function InkMark({ state, size = 160, className }: { state: "grinding" | "resting"; size?: number; className?: string }) {
  const grinding = state === "grinding";
  return (
    <svg viewBox="0 0 64 64" width={size} height={size} aria-hidden="true" className={cx("shrink-0 overflow-visible", className)}>
      <rect x="9" y="41" width="46" height="13" rx="6.5" fill="none" stroke={INK_ORANGE} strokeWidth="3" />
      <ellipse
        cx="25"
        cy="47.5"
        rx="8"
        ry="2.6"
        fill={INK_TEAL}
        className={cx("origin-[25px_47.5px] [transform-box:view-box]", grinding ? "animate-[ink-ripple_1.6s_ease-in-out_infinite]" : "opacity-60")}
      />
      <g className={grinding ? "animate-[ink-grind_1.6s_ease-in-out_infinite] motion-reduce:animate-none" : undefined}>
        <g
          transform="translate(27 44) rotate(38)"
          className={cx(
            "origin-[0_0] [transform-box:view-box]",
            !grinding && "animate-[ink-rest_.7s_cubic-bezier(.16,1,.3,1)_forwards] motion-reduce:animate-none motion-reduce:[transform:translate(46px,37.5px)_rotate(-84deg)]",
          )}
        >
          <rect x="-2.6" y="-38" width="5.2" height="24" rx="2.6" fill={INK_ORANGE} />
          <rect x="-3.3" y="-15" width="6.6" height="3.4" rx="1" fill={INK_TEAL} />
          <path d="M-3.3-11.6C-3.3-5-1-1 0 1.5C1-1 3.3-5 3.3-11.6Z" fill={INK_TEAL} />
        </g>
      </g>
    </svg>
  );
}

/** Chiếm đúng vùng nội dung còn thấy dưới thanh trên (trừ lề trên dưới của `main`) để logo nằm giữa màn hình. */
const CENTER = "flex min-h-[calc(100dvh-3.5rem-6.5rem)] flex-1 flex-col items-center justify-center";

/** Cả trang đang tải: chỉ có bút mài nghiên ở giữa, không chữ (trình đọc màn hình vẫn đọc `label`). */
export function PageLoading({ label = "Đang tải…", className }: { label?: string; className?: string }) {
  return (
    <div role="status" className={cx(CENTER, className)}>
      <InkMark state="grinding" />
      <span className="sr-only">{label}</span>
    </div>
  );
}

/** Cả trang tải lỗi: bút gác lên nghiên, lời báo ngắn bên dưới và nút Tải lại trang (mặc định tải lại cả trang). */
export function PageLoadError({ children, onRetry, className }: { children: ReactNode; onRetry?: () => void; className?: string }) {
  return (
    <div role="alert" className={cx(CENTER, "gap-4 text-center", className)}>
      <InkMark state="resting" />
      <p className="max-w-sm text-[14px] leading-relaxed text-body">{children}</p>
      <Button variant="primary" onClick={onRetry ?? (() => window.location.reload())}>
        Tải lại trang
      </Button>
    </div>
  );
}
