"use client";

import { useEffect, useRef, useState, type CSSProperties, type ElementType, type ReactNode } from "react";
import { cx } from "@/shared/ui/cx";
import styles from "../landing.module.css";

/** `true` từ lần đầu phần tử lọt vào khung nhìn (một chiều, không tắt lại khi cuộn đi). */
export function useSeenOnce<T extends Element>(threshold = 0.35) {
  const ref = useRef<T>(null);
  const [seen, setSeen] = useState(false);
  useEffect(() => {
    const node = ref.current;
    // Không có IntersectionObserver thì giữ trạng thái tĩnh: nội dung vẫn hiện đủ, chỉ không có hiệu ứng.
    if (!node || seen || typeof IntersectionObserver === "undefined") return;
    const observer = new IntersectionObserver(([entry]) => {
      if (entry?.isIntersecting) {
        setSeen(true);
        observer.disconnect();
      }
    }, { threshold });
    observer.observe(node);
    return () => observer.disconnect();
  }, [seen, threshold]);
  return [ref, seen] as const;
}

/**
 * Vùng có vật thể bóng: ghi vị trí con trỏ so với tâm vùng (-1..1) vào biến CSS `--mx`, `--my` để các vật thể nghiêng
 * nhẹ theo chiều sâu riêng (`--depth`). Không theo con trỏ khi người dùng tắt chuyển động hoặc dùng màn hình cảm ứng.
 */
export function PointerScene({ className, children }: { className?: string; children: ReactNode }) {
  const ref = useRef<HTMLDivElement>(null);
  useEffect(() => {
    const node = ref.current;
    if (!node) return;
    if (!window.matchMedia || window.matchMedia("(prefers-reduced-motion: reduce), (pointer: coarse)").matches) return;
    let frame = 0;
    const move = (event: PointerEvent) => {
      cancelAnimationFrame(frame);
      frame = requestAnimationFrame(() => {
        const box = node.getBoundingClientRect();
        node.style.setProperty("--mx", (((event.clientX - box.left) / box.width) * 2 - 1).toFixed(3));
        node.style.setProperty("--my", (((event.clientY - box.top) / box.height) * 2 - 1).toFixed(3));
      });
    };
    const leave = () => {
      node.style.setProperty("--mx", "0");
      node.style.setProperty("--my", "0");
    };
    node.addEventListener("pointermove", move);
    node.addEventListener("pointerleave", leave);
    return () => {
      cancelAnimationFrame(frame);
      node.removeEventListener("pointermove", move);
      node.removeEventListener("pointerleave", leave);
    };
  }, []);
  return (
    <div ref={ref} className={cx(styles.scene, className)}>
      {children}
    </div>
  );
}

/**
 * Câu lớn chuyển dần từ màu mờ sang màu chữ đậm theo từng từ khi người đọc cuộn qua, như giảng viên đọc tới đâu sáng tới
 * đó. Toàn bộ câu vẫn có sẵn trong HTML cho máy tìm kiếm và trình đọc màn hình; tắt chuyển động thì hiện đậm ngay.
 */
export function InkReveal({
  text,
  as: Tag = "p",
  id,
  className,
  off = "var(--c-line-strong)",
  on = "var(--c-ink)",
}: {
  text: string;
  as?: ElementType;
  id?: string;
  className?: string;
  off?: string;
  on?: string;
}) {
  const ref = useRef<HTMLElement>(null);
  const [lit, setLit] = useState(0);
  const words = text.split(" ");
  useEffect(() => {
    const node = ref.current;
    if (!node) return;
    let frame = 0;
    if (window.matchMedia?.("(prefers-reduced-motion: reduce)").matches) {
      frame = requestAnimationFrame(() => setLit(words.length));
      return () => cancelAnimationFrame(frame);
    }
    const update = () => {
      const box = node.getBoundingClientRect();
      const view = window.innerHeight;
      const progress = (view * 0.88 - box.top) / (box.height + view * 0.3);
      setLit(Math.round(Math.min(1, Math.max(0, progress)) * words.length));
    };
    const schedule = () => {
      cancelAnimationFrame(frame);
      frame = requestAnimationFrame(update);
    };
    update();
    window.addEventListener("scroll", schedule, { passive: true });
    window.addEventListener("resize", schedule);
    return () => {
      cancelAnimationFrame(frame);
      window.removeEventListener("scroll", schedule);
      window.removeEventListener("resize", schedule);
    };
  }, [words.length]);
  return (
    <Tag ref={ref} id={id} className={className} style={{ "--ink-off": off, "--ink-on": on } as CSSProperties}>
      {words.map((word, index) => (
        <span key={index} className={styles.inkWord} data-on={index < lit ? "" : undefined}>
          {word}
          {index < words.length - 1 ? " " : ""}
        </span>
      ))}
    </Tag>
  );
}

export type DoodleKind = "underline" | "circle" | "heart" | "sun" | "star" | "flower" | "swirl" | "arrow";

// Mỗi nét vẽ tay là một hoặc vài đường; `pathLength=1` để chung một nhịp vẽ bất kể độ dài nét.
const DOODLES: Record<DoodleKind, { viewBox: string; paths: string[] }> = {
  underline: { viewBox: "0 0 300 24", paths: ["M4 16 C60 6 120 4 180 9 C220 12 260 14 296 8"] },
  circle: { viewBox: "0 0 300 110", paths: ["M152 8 C70 6 10 26 12 56 C14 92 92 104 166 100 C240 96 292 78 288 48 C284 18 214 4 120 14"] },
  heart: { viewBox: "0 0 100 90", paths: ["M50 82 C20 60 6 44 8 28 C10 12 30 6 42 18 C46 22 48 26 50 30 C52 24 58 14 70 10 C86 6 96 20 92 34 C88 52 70 66 50 82 Z"] },
  sun: {
    viewBox: "0 0 120 120",
    paths: [
      "M60 40 C72 40 80 50 80 60 C80 72 70 80 60 80 C48 80 40 70 40 60 C40 50 48 40 60 40 Z",
      "M60 8 V26 M60 94 V112 M8 60 H26 M94 60 H112 M22 22 L34 34 M86 86 L98 98 M98 22 L86 34 M34 86 L22 98",
    ],
  },
  star: { viewBox: "0 0 120 120", paths: ["M60 10 L72 46 L110 46 L80 70 L92 108 L60 84 L28 108 L40 70 L10 46 L48 46 Z"] },
  flower: {
    viewBox: "0 0 120 140",
    paths: [
      "M60 50 C50 30 62 14 72 24 C82 34 70 48 60 50 C80 44 94 56 84 68 C74 78 62 66 60 50 C62 72 48 82 40 70 C32 58 46 48 60 50 C40 54 28 40 38 30 C48 20 60 34 60 50 Z",
      "M60 56 C58 84 62 108 56 134 M58 104 C70 96 80 98 88 90",
    ],
  },
  swirl: { viewBox: "0 0 160 80", paths: ["M6 60 C30 20 60 14 70 34 C80 54 50 62 52 44 C54 24 96 16 116 34 C130 46 140 56 154 52"] },
  arrow: { viewBox: "0 0 140 60", paths: ["M6 46 C40 10 86 8 126 26", "M108 12 L128 27 L106 38"] },
};

/** Nét vẽ tay (gạch chân, khoanh tròn, trái tim…) tự vẽ ra khi lọt vào khung nhìn. Chỉ để trang trí. */
export function Doodle({
  kind,
  className,
  color = "currentColor",
  width = 3,
  delay = 0,
}: {
  kind: DoodleKind;
  className?: string;
  color?: string;
  width?: number;
  delay?: number;
}) {
  const [ref, seen] = useSeenOnce<SVGSVGElement>(0.6);
  const doodle = DOODLES[kind];
  return (
    <svg
      ref={ref}
      viewBox={doodle.viewBox}
      aria-hidden="true"
      className={cx(styles.doodle, className)}
      data-drawn={seen ? "" : undefined}
      fill="none"
      stroke={color}
      strokeWidth={width}
      strokeLinecap="round"
      strokeLinejoin="round"
      style={{ "--delay": `${delay}s` } as CSSProperties}
      preserveAspectRatio="none"
    >
      {/* Giãn theo khung chữ được bao (none), nét giữ đều độ dày nhờ non-scaling-stroke. */}
      {doodle.paths.map((d) => (
        <path key={d} d={d} pathLength={1} vectorEffect="non-scaling-stroke" />
      ))}
    </svg>
  );
}
