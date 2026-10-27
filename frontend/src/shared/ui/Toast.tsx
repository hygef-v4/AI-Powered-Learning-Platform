"use client";

import { useEffect, useState } from "react";
import { cx } from "./cx";
import { Icon } from "./Icon";

export type ToastTone = "success" | "warning" | "danger";
type ToastItem = { id: number; text: string; tone: ToastTone };

const VISIBLE_MS = 4000;
const listeners = new Set<(item: ToastItem) => void>();
let nextId = 1;

/** Hiện một câu ngắn nổi ở góc phải, ngay dưới thanh trên cùng, tự ẩn sau vài giây; giữ qua lần chuyển trang. */
export function showToast(text: string, tone: ToastTone = "success") {
  const item = { id: nextId++, text, tone };
  listeners.forEach((listener) => listener(item));
}

/** Nơi vẽ thông báo nổi; đặt một lần ở layout gốc. */
export function Toaster() {
  const [items, setItems] = useState<ToastItem[]>([]);

  useEffect(() => {
    const timers = new Set<ReturnType<typeof setTimeout>>();
    const listener = (item: ToastItem) => {
      setItems((current) => [...current, item]);
      const timer = setTimeout(() => {
        setItems((current) => current.filter((t) => t.id !== item.id));
        timers.delete(timer);
      }, VISIBLE_MS);
      timers.add(timer);
    };
    listeners.add(listener);
    return () => {
      listeners.delete(listener);
      timers.forEach(clearTimeout);
    };
  }, []);

  return <ToastStack items={items} className="top-[68px]" />;
}

/** Chồng thông báo ở góc phải; dùng chung cho màn sau đăng nhập (dưới thanh trên) và màn chưa đăng nhập. */
export function ToastStack({ items, className }: { items: ToastItem[]; className?: string }) {
  return (
    <div aria-live="polite" className={cx("pointer-events-none fixed right-4 z-[70] flex flex-col items-end gap-2 md:right-6", className)}>
      {items.map((item) => (
        <p
          key={item.id}
          role="status"
          className={cx(
            "flex animate-slide-in items-center gap-2 rounded-xl border bg-surface px-4 py-2.5 text-[13px] font-semibold shadow-[var(--shadow-float)]",
            item.tone === "success" && "border-success-soft text-success",
            item.tone === "warning" && "border-warning-soft text-warning",
            item.tone === "danger" && "border-danger-soft text-danger",
          )}
        >
          <Icon name={item.tone === "success" ? "check_circle" : item.tone === "danger" ? "error" : "warning"} size={16} />
          {item.text}
        </p>
      ))}
    </div>
  );
}
