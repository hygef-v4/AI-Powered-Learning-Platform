// Kiểu dùng chung của các màn chưa đăng nhập, theo số đo của design G21 (`Main`, `c02a`–`c04`).
// Màn sau đăng nhập dùng `@/shared/ui`; các màn này có khung riêng nên giữ kiểu ở đây.

/** Nhãn ô nhập: chữ in hoa nhỏ, đậm. */
export const AUTH_LABEL = "text-[11px] font-bold tracking-[.06em] text-[#64748b] uppercase";

/** Ô nhập cao 44px, bo 12px. */
export const AUTH_INPUT =
  "h-11 w-full rounded-xl border border-[#d9dee8] bg-white px-3.5 text-[13px] text-ink outline-none " +
  "placeholder:text-[#94a3b8] focus:border-primary focus:ring-2 focus:ring-primary/15 " +
  "read-only:bg-[#f1f5f9] read-only:text-[#475569]";

/** Nút chính rộng hết cột, bo 12px. */
export const AUTH_BUTTON =
  "flex h-12 w-full items-center justify-center gap-2 rounded-xl bg-primary-strong text-sm font-semibold text-white " +
  "transition-colors hover:bg-[#1d2c80] disabled:cursor-not-allowed disabled:opacity-45";

/** Nút Đăng nhập dùng màu riêng của artboard `Main`. */
export const LOGIN_BUTTON =
  "flex h-12 w-full items-center justify-center rounded-xl bg-[#0e2a9e] text-sm font-semibold text-white " +
  "transition-colors hover:bg-[#0b2180] disabled:cursor-not-allowed disabled:opacity-60";

/** Chữ ghi chú dưới nút. */
export const AUTH_NOTE = "text-center text-xs leading-relaxed text-[#94a3b8]";
