import { cx } from "./cx";

/** Con dấu hanko khắc chữ 学 (học); màu dấu theo theme. */
export function LogoMark({ size = 28, className }: { size?: number; className?: string }) {
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

/** Logo đầy đủ: dấu + tên nền tảng chữ có chân. */
export function Logo({ className }: { className?: string }) {
  return (
    <span className={cx("inline-flex items-center gap-2.5 text-ink", className)}>
      <LogoMark />
      <span className="font-display text-[19px] leading-none tracking-[-0.02em] whitespace-nowrap">
        Nền tảng <i>học tập</i>
      </span>
    </span>
  );
}
