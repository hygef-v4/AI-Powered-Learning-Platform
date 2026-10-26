// Kiểu dùng chung của các màn chưa đăng nhập (đăng nhập, kích hoạt, khôi phục mật khẩu).
// Màn sau đăng nhập dùng `@/shared/ui`; các màn này có khung riêng nên giữ kiểu ở đây.

/** Nhãn ô nhập. */
export const AUTH_LABEL = "text-xs font-semibold text-body";

/** Ô nhập cao 44px. */
export const AUTH_INPUT =
  "h-11 w-full rounded-lg border border-line-strong bg-surface px-3.5 text-[14px] text-ink outline-none transition-[border-color,box-shadow] duration-200 " +
  "placeholder:text-faint focus:border-primary focus:shadow-[0_0_0_3px_var(--c-primary-soft)] " +
  "read-only:bg-sunken read-only:text-body";

/** Nút chính rộng hết cột. */
export const AUTH_BUTTON =
  "flex h-11 w-full items-center justify-center gap-2 rounded-lg bg-primary text-sm font-semibold text-on-primary " +
  "transition-[background-color,transform] duration-200 hover:bg-primary-strong active:scale-[.98] disabled:cursor-not-allowed disabled:opacity-45";

/** Nút Đăng nhập, cùng kiểu nút chính. */
export const LOGIN_BUTTON = AUTH_BUTTON;

/** Chữ ghi chú dưới nút. */
export const AUTH_NOTE = "text-center text-xs leading-relaxed text-muted";
