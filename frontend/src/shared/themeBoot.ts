// Không có "use client": layout (server) nhúng chuỗi này vào <head>.

export const THEME_KEY = "theme";

/** Chạy trong <head> trước khi vẽ: theme đã chọn, chưa chọn thì là giao diện sáng. */
export const THEME_BOOT_SCRIPT = `try{var t=localStorage.getItem("${THEME_KEY}");if(t!=="dark")t="light";document.documentElement.dataset.theme=t}catch(e){}`;
