// Không có "use client": layout (server) nhúng chuỗi này vào <head>.

export const THEME_KEY = "theme";

export const DARK_SCHEME_QUERY = "(prefers-color-scheme: dark)";

/** Chạy trong <head> trước khi vẽ: theme đã chọn; chưa chọn thì theo cài đặt sáng/tối của hệ điều hành. */
export const THEME_BOOT_SCRIPT =
  `try{var t=localStorage.getItem("${THEME_KEY}");` +
  `if(t!=="dark"&&t!=="light")t=window.matchMedia&&matchMedia("${DARK_SCHEME_QUERY}").matches?"dark":"light";` +
  `document.documentElement.dataset.theme=t}catch(e){document.documentElement.dataset.theme="light"}`;
