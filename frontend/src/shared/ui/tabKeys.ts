import type { KeyboardEvent } from "react";

/**
 * Phím mũi tên trái/phải, Home, End trên một hàng tab tự vẽ (`role="tablist"`): chọn tab kế bên và đưa focus theo. Gắn vào
 * `onKeyDown` của hàng tab; mỗi tab tự đặt `tabIndex` 0 khi đang chọn, -1 khi không.
 */
export function handleTabListKey(event: KeyboardEvent<HTMLElement>) {
  const tabs = [...event.currentTarget.querySelectorAll<HTMLElement>('[role="tab"]:not(:disabled)')];
  const current = tabs.indexOf(document.activeElement as HTMLElement);
  if (current < 0) return;
  const target = event.key === "ArrowRight" ? (current + 1) % tabs.length
    : event.key === "ArrowLeft" ? (current - 1 + tabs.length) % tabs.length
      : event.key === "Home" ? 0
        : event.key === "End" ? tabs.length - 1
          : -1;
  if (target < 0) return;
  event.preventDefault();
  tabs[target]!.click();
  tabs[target]!.focus();
}
