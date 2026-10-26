// Không có "use client": layout (server) nhúng chuỗi này vào <head>.

export const THEME_KEY = "theme";

/** Chạy trong <head> trước khi vẽ: theme đã chọn, không có thì theo hệ điều hành. */
export const THEME_BOOT_SCRIPT = `try{var t=localStorage.getItem("${THEME_KEY}");if(t!=="light"&&t!=="dark")t=matchMedia("(prefers-color-scheme: dark)").matches?"dark":"light";document.documentElement.dataset.theme=t}catch(e){}`;
