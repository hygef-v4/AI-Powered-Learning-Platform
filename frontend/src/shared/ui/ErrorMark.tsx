import { cx } from "./cx";
import { INK_ORANGE, INK_TEAL } from "./Logo";

export type ErrorKind = "not-found" | "unavailable" | "offline" | "generic";

/** Ngòi bút lông ở gốc tọa độ (đầu ngòi tại 0,1.5), cán dựng thẳng lên trên; xoay, dời bằng `transform`. */
function Brush({ transform, outline = false }: { transform: string; outline?: boolean }) {
  const handle = outline ? { fill: "none", stroke: INK_ORANGE, strokeWidth: 1.6, strokeDasharray: "3 2.4" } : { fill: INK_ORANGE };
  const head = outline ? { fill: "none", stroke: INK_TEAL, strokeWidth: 1.6, strokeDasharray: "3 2.4" } : { fill: INK_TEAL };
  return (
    <g transform={transform}>
      <rect x="-2.6" y="-38" width="5.2" height="24" rx="2.6" {...handle} />
      <rect x="-3.3" y="-15" width="6.6" height="3.4" rx="1" {...head} />
      <path d="M-3.3-11.6C-3.3-5-1-1 0 1.5C1-1 3.3-5 3.3-11.6Z" {...head} />
    </g>
  );
}

function Inkstone({ pool = true }: { pool?: boolean }) {
  return (
    <>
      <rect x="9" y="41" width="46" height="13" rx="6.5" fill="none" stroke={INK_ORANGE} strokeWidth="3" />
      {pool && <ellipse cx="25" cy="47.5" rx="8" ry="2.6" fill={INK_TEAL} />}
    </>
  );
}

/**
 * Hình riêng cho từng trang lỗi, cùng bộ bút và nghiên của logo:
 * - `not-found`: bút thất lạc, chỉ còn nét đứt chỗ bút từng chấm;
 * - `unavailable`: bút dựng chờ trong nghiên, ba chấm cam nhấp nháy bên cạnh;
 * - `offline`: bút treo cao, đường mực xuống nghiên bị đứt quãng;
 * - `generic`: bút gác trên nghiên, mực đổ loang ra ngoài.
 */
export function ErrorMark({ kind, size = 176, className }: { kind: ErrorKind; size?: number; className?: string }) {
  return (
    <svg viewBox="0 0 64 64" width={size} height={size} aria-hidden="true" className={cx("shrink-0 overflow-visible", className)}>
      {kind === "not-found" && (
        <>
          <Inkstone />
          <Brush transform="translate(30 37) rotate(30)" outline />
        </>
      )}
      {kind === "unavailable" && (
        <>
          <Inkstone />
          <Brush transform="translate(25 46)" />
          <g fill={INK_ORANGE}>
            <circle cx="38" cy="24" r="2.2" className="animate-pulse" />
            <circle cx="45" cy="24" r="2.2" className="animate-pulse [animation-delay:.25s]" />
            <circle cx="52" cy="24" r="2.2" className="animate-pulse [animation-delay:.5s]" />
          </g>
        </>
      )}
      {kind === "offline" && (
        <>
          <Inkstone />
          <Brush transform="translate(32 28) rotate(30)" />
          <g fill="none" stroke={INK_TEAL} strokeWidth="1.8" strokeLinecap="round">
            <path d="M30.6 31.6 29.2 35" />
            <path d="M27.6 38.6 26.4 42" />
          </g>
          <path d="M22 33.5l4.5 3.6M26.5 33.5 22 37.1" fill="none" stroke={INK_ORANGE} strokeWidth="1.8" strokeLinecap="round" />
        </>
      )}
      {kind === "generic" && (
        <>
          <Inkstone pool={false} />
          <ellipse cx="29" cy="47.5" rx="4.5" ry="1.8" fill={INK_TEAL} />
          {/* Mực tràn qua mép trái rồi loang dưới đáy */}
          <path d="M11 45.5C7.5 47 6.5 51 7.5 55.5" fill="none" stroke={INK_TEAL} strokeWidth="2.6" strokeLinecap="round" />
          <ellipse cx="13" cy="58.3" rx="9" ry="2.2" fill={INK_TEAL} />
          <circle cx="24.5" cy="59" r="1.3" fill={INK_TEAL} />
          <Brush transform="translate(46 37.5) rotate(-84)" />
        </>
      )}
    </svg>
  );
}
