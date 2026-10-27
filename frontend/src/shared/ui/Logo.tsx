import { cx } from "./cx";

// Màu cố định, không đổi khi đổi sáng tối: cam và xanh lơ đậm vừa đủ đọc rõ trên cả nền trắng lẫn nền xanh đêm.
export const INK_ORANGE = "#e8833a";
export const INK_TEAL = "#2b9bb0";

/**
 * Bút lông chấm vào nghiên: cán bút và viền nghiên màu cam, mực trong nghiên và đầu bút màu xanh lơ. Không có ô nền, không có
 * chữ; giữ nguyên hình và màu ở cả hai theme.
 */
export function LogoMark({ size = 28, className }: { size?: number; className?: string }) {
  return (
    <svg viewBox="0 0 64 64" width={size} height={size} aria-hidden="true" className={cx("shrink-0", className)}>
      <rect x="9" y="41" width="46" height="13" rx="6.5" fill="none" stroke={INK_ORANGE} strokeWidth="3" />
      <ellipse cx="25" cy="47.5" rx="8" ry="2.6" fill={INK_TEAL} />
      <g transform="translate(27 44) rotate(38)">
        <rect x="-2.6" y="-38" width="5.2" height="24" rx="2.6" fill={INK_ORANGE} />
        <rect x="-3.3" y="-15" width="6.6" height="3.4" rx="1" fill={INK_TEAL} />
        <path d="M-3.3-11.6C-3.3-5-1-1 0 1.5C1-1 3.3-5 3.3-11.6Z" fill={INK_TEAL} />
      </g>
    </svg>
  );
}

/**
 * Bút chấm nghiên vẽ một màu (`currentColor`) làm hình chìm to sau vùng nội dung: viền nghiên và cán bút là nét, mực và
 * ngòi tô kín, cùng một màu.
 */
export function LogoWatermark({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 64 64" aria-hidden="true" className={className}>
      <rect x="9" y="41" width="46" height="13" rx="6.5" fill="none" stroke="currentColor" strokeWidth="2.4" />
      <ellipse cx="25" cy="47.5" rx="8" ry="2.6" fill="currentColor" />
      <g transform="translate(27 44) rotate(38)" fill="currentColor">
        <rect x="-2.6" y="-38" width="5.2" height="24" rx="2.6" />
        <rect x="-3.3" y="-15" width="6.6" height="3.4" rx="1" />
        <path d="M-3.3-11.6C-3.3-5-1-1 0 1.5C1-1 3.3-5 3.3-11.6Z" />
      </g>
    </svg>
  );
}

/** Con dấu hanko khắc chữ 学 (học), chỉ còn làm đồ trang trí ở các màn đăng nhập; màu dấu theo theme. */
export function HankoMark({ size = 28, className }: { size?: number; className?: string }) {
  return (
    <svg viewBox="0 0 64 64" width={size} height={size} aria-hidden="true" className={cx("shrink-0", className)}>
      <rect x="4" y="4" width="56" height="56" rx="12" className="fill-[#e8833a] dark:fill-primary" />
      <g fill="none" strokeWidth="4.6" strokeLinecap="round" strokeLinejoin="round" className="stroke-[#fff8f1] dark:stroke-on-primary">
        <path d="M21.5 12.5 24 17.5" />
        <path d="M32 11v6" />
        <path d="M42.5 12.5 40 17.5" />
        <path d="M14.5 26v-4.5h35V26" />
        <path d="M22 28.5h19L33 35" />
        <path d="M14 41.5h36" />
        <path d="M32 35v15q0 4-4.5 2" />
      </g>
    </svg>
  );
}

/** Logo đầy đủ: bút chấm nghiên + tên nền tảng; chữ "học tập" nằm trên nền màu nhấn của theme. */
export function Logo({ className }: { className?: string }) {
  return (
    <span className={cx("inline-flex items-center gap-2.5 text-ink", className)}>
      <LogoMark />
      <span className="font-display text-[17px] leading-none tracking-[-0.02em] whitespace-nowrap">
        Nền tảng <i className="rounded-md bg-[#e8833a] px-1.5 pb-0.5 text-[#fff8f1] dark:bg-primary dark:text-on-primary">học tập</i>
      </span>
    </span>
  );
}
