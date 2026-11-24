"use client";

import { Children, useEffect, useRef, useState, type CSSProperties, type ReactNode } from "react";
import { chooseLocale, type Locale } from "@/i18n/locales";
import { useTheme } from "@/shared/theme";
import { cx } from "@/shared/ui/cx";
import { Icon } from "@/shared/ui/Icon";
import styles from "../landing.module.css";
import { useSeenOnce } from "./motion";

const FOCUS_RING = "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-focus";

/**
 * Danh sách tính năng: mỗi hàng là hình minh hoạ và lời giải thích. Hàng đang ở giữa màn hình sáng rõ, các hàng khác mờ đi,
 * nên người đọc luôn biết mình đang xem tính năng nào khi cuộn.
 */
export function FeatureRows({ children, className }: { children: ReactNode; className?: string }) {
  const listRef = useRef<HTMLOListElement>(null);
  const [active, setActive] = useState(0);
  useEffect(() => {
    const list = listRef.current;
    if (!list || typeof IntersectionObserver === "undefined") return;
    const rows = Array.from(list.children);
    const observer = new IntersectionObserver((entries) => {
      for (const entry of entries) {
        if (entry.isIntersecting) setActive(rows.indexOf(entry.target));
      }
    }, { rootMargin: "-42% 0px -42% 0px" });
    rows.forEach((row) => observer.observe(row));
    return () => observer.disconnect();
  }, []);
  return (
    <ol ref={listRef} className={className}>
      {Children.toArray(children).map((child, index) => (
        <li key={index} className={styles.panel} data-active={index === active ? "" : undefined}>
          {child}
        </li>
      ))}
    </ol>
  );
}

export type Pill = { label: string; tone: "orange" | "teal" | "blue" | "lilac" | "paper" };

const PILL_TONES: Record<Pill["tone"], string> = {
  orange: "bg-[#e8833a] text-[#1d1005]",
  teal: "bg-[#3fb5c9] text-[#04232a]",
  blue: "bg-[#9ab9df] text-[#12233a]",
  lilac: "bg-[#c6a6ec] text-[#22123a]",
  paper: "bg-[#eef2f7] text-[#18212d]",
};

// Chỗ đáp của từng nhãn: trái (%), cao từ đáy (px), góc nghiêng. Xếp thành đống thấp dần ra hai bên như đồ đổ ra bàn.
const SLOTS: [number, number, number][] = [
  [2, 0, -4], [21, 2, 6], [40, 0, -2], [58, 3, 9], [76, 0, -7],
  [8, 48, 14], [30, 52, -10], [49, 46, 4], [67, 50, -15],
  [16, 98, -6], [38, 104, 18], [58, 96, -8], [74, 112, 24],
  [26, 150, 8], [47, 156, -20], [64, 162, 10],
  [36, 204, -4], [52, 214, 30],
];

/**
 * Nhãn lợi ích rơi xuống và chất thành đống khi người đọc cuộn tới (như video tham chiếu). Mỗi nhãn là một mục của danh
 * sách thật, nên trình đọc màn hình và máy tìm kiếm đọc được đủ chữ; rê chuột lên nhãn thì nó nhấc nhẹ lên.
 */
export function PillPile({ pills, label }: { pills: Pill[]; label: string }) {
  const [ref, seen] = useSeenOnce<HTMLUListElement>(0.3);
  return (
    <ul ref={ref} aria-label={label} data-fall={seen ? "" : undefined} className={cx(styles.pile, "relative h-[300px] w-full sm:h-[320px]")}>
      {pills.slice(0, SLOTS.length).map((pill, index) => {
        const [x, y, r] = SLOTS[index]!;
        const style = {
          "--x": `${x}%`,
          "--y": `${y}px`,
          "--r": `${r}deg`,
          "--spin": `${(index % 2 ? 1 : -1) * (30 + (index * 17) % 50)}deg`,
          "--delay": `${(index % 6) * 0.09 + Math.floor(index / 6) * 0.18}s`,
        } as CSSProperties;
        return (
          <li
            key={pill.label}
            style={style}
            className={cx(
              styles.pill,
              "rounded-full px-4 py-2.5 text-[13px] font-bold whitespace-nowrap shadow-[0_10px_24px_rgb(0_0_0/0.35)] sm:px-5 sm:py-3 sm:text-[15px]",
              PILL_TONES[pill.tone],
            )}
          >
            {pill.label}
          </li>
        );
      })}
    </ul>
  );
}

/**
 * Thanh điều hướng dạng viên thuốc nổi ở đáy màn hình: nhảy tới từng phần của trang, mục đang xem được tô sáng. Ẩn khi còn
 * ở phần đầu trang để không che nút chính.
 */
export function FloatingNav({ items, label }: { items: { id: string; label: string }[]; label: string }) {
  const [active, setActive] = useState<string | null>(null);
  const [visible, setVisible] = useState(false);
  useEffect(() => {
    const onScroll = () => setVisible(window.scrollY > window.innerHeight * 0.6);
    onScroll();
    window.addEventListener("scroll", onScroll, { passive: true });
    if (typeof IntersectionObserver === "undefined") return () => window.removeEventListener("scroll", onScroll);
    const observer = new IntersectionObserver((entries) => {
      for (const entry of entries) if (entry.isIntersecting) setActive(entry.target.id);
    }, { rootMargin: "-45% 0px -50% 0px" });
    for (const { id } of items) {
      const section = document.getElementById(id);
      if (section) observer.observe(section);
    }
    return () => {
      observer.disconnect();
      window.removeEventListener("scroll", onScroll);
    };
  }, [items]);
  return (
    <nav
      aria-label={label}
      className={cx(
        "fixed inset-x-0 bottom-4 z-40 flex justify-center px-3 transition-[opacity,translate] duration-500 ease-[cubic-bezier(0.16,1,0.3,1)]",
        visible ? "translate-y-0 opacity-100" : "pointer-events-none translate-y-6 opacity-0",
      )}
    >
      <ul className="flex max-w-full items-center gap-0.5 overflow-x-auto rounded-full border border-white/10 bg-[#0d1624]/92 p-1.5 shadow-[0_18px_40px_rgb(0_0_0/0.35)] backdrop-blur-md">
        {items.map(({ id, label: text }) => (
          <li key={id}>
            <a
              href={`#${id}`}
              tabIndex={visible ? undefined : -1}
              aria-current={active === id ? "location" : undefined}
              className={cx(
                "block rounded-full px-3.5 py-2 text-[13px] font-semibold whitespace-nowrap transition-colors duration-200 sm:px-4",
                active === id ? "bg-[#eef2f7] text-[#18212d]" : "text-[#c5cfdd] hover:bg-white/10 hover:text-white",
                FOCUS_RING,
              )}
            >
              {text}
            </a>
          </li>
        ))}
      </ul>
    </nav>
  );
}

/**
 * Nút đổi sáng/tối của landing, dùng chung lựa chọn theme với cả ứng dụng (lưu trong localStorage). Các dải bảng vẽ tối
 * giữ nguyên màu; nút này đổi các phần sáng ở giữa trang.
 */
export function ThemeSwitch({ toLight, toDark }: { toLight: string; toDark: string }) {
  const [theme, toggle] = useTheme();
  const label = theme === "dark" ? toLight : toDark;
  return (
    <button
      type="button"
      onClick={toggle}
      aria-label={label}
      title={label}
      className={cx("inline-flex size-10 items-center justify-center rounded-full border border-white/15 text-[#c5cfdd] transition-colors duration-200 hover:bg-white/10 hover:text-white", FOCUS_RING)}
    >
      <Icon name={theme === "dark" ? "sun" : "moon"} size={18} />
    </button>
  );
}

/** Đổi ngôn ngữ ngay trên landing; lưu cookie rồi tải lại trang bằng ngôn ngữ mới. */
export function LanguageSwitch({ locale, label, options }: { locale: Locale; label: string; options: { code: Locale; short: string; name: string }[] }) {
  return (
    <div role="group" aria-label={label} className="flex items-center rounded-full border border-white/15 p-0.5">
      {options.map(({ code, short, name }) => (
        <button
          key={code}
          type="button"
          lang={code}
          aria-pressed={locale === code}
          aria-label={name}
          onClick={() => code !== locale && chooseLocale(code)}
          className={cx(
            "h-8 min-w-10 rounded-full px-2.5 text-[12px] font-bold tracking-wide transition-colors duration-200",
            locale === code ? "bg-white/90 text-[#132031]" : "text-[#c5cfdd] hover:text-white",
            FOCUS_RING,
          )}
        >
          {short}
        </button>
      ))}
    </div>
  );
}
