"use client";

import { useEffect, useRef, useState, type KeyboardEvent, type ReactNode } from "react";
import { cx } from "@/shared/ui";

/** Một chuỗi số trên biểu đồ: tên (chú thích, tooltip) và màu (token CSS). */
export type Series = { name: string; color: string };

const NUMBER = new Intl.NumberFormat("vi-VN");
const COMPACT = new Intl.NumberFormat("vi-VN", { notation: "compact", maximumFractionDigits: 1 });
export const formatNumber = (value: number) => NUMBER.format(value);

/**
 * Bấm vào biểu đồ thì hiện số trên các phần của nó, bấm ra ngoài thì ẩn. Bàn phím: Enter hoặc Space trên biểu đồ để hiện,
 * Esc để ẩn.
 */
export function Reveal({ label, className, children }: { label: string; className?: string; children: (shown: boolean) => ReactNode }) {
  const ref = useRef<HTMLDivElement>(null);
  const [shown, setShown] = useState(false);
  useEffect(() => {
    if (!shown) return;
    const hide = (event: PointerEvent) => { if (!ref.current?.contains(event.target as Node)) setShown(false); };
    document.addEventListener("pointerdown", hide);
    return () => document.removeEventListener("pointerdown", hide);
  }, [shown]);
  const onKeyDown = (event: KeyboardEvent) => {
    if (event.key === "Enter" || event.key === " ") { event.preventDefault(); setShown(true); }
    if (event.key === "Escape") setShown(false);
  };
  return (
    <div ref={ref} role="button" tabIndex={0} aria-pressed={shown} aria-label={`${label}; bấm để ${shown ? "giữ" : "hiện"} số`}
      onClick={() => setShown(true)} onKeyDown={onKeyDown}
      className={cx("cursor-pointer rounded-lg outline-none focus-visible:ring-2 focus-visible:ring-focus/40", className)}>
      {children(shown)}
    </div>
  );
}

/** Bề rộng thật của khung để vẽ SVG theo pixel (chữ không bị co giãn); chưa đo được thì lấy `fallback`. */
function useWidth(fallback: number) {
  const ref = useRef<HTMLDivElement>(null);
  const [width, setWidth] = useState(fallback);
  useEffect(() => {
    const node = ref.current;
    if (!node || typeof ResizeObserver === "undefined") return;
    const observer = new ResizeObserver(([entry]) => { if (entry && entry.contentRect.width > 0) setWidth(entry.contentRect.width); });
    observer.observe(node);
    return () => observer.disconnect();
  }, []);
  return [ref, width] as const;
}

/** Chú thích: ô màu nhỏ và tên, chữ màu chữ phụ (không tô theo màu chuỗi). */
export function Legend({ series, className }: { series: Series[]; className?: string }) {
  return (
    <ul className={cx("flex flex-wrap gap-x-3.5 gap-y-1 text-xs text-muted", className)}>
      {series.map((s) => (
        <li key={s.name} className="flex items-center gap-1.5">
          <span aria-hidden className="size-2.5 rounded-[3px]" style={{ background: s.color }} />{s.name}
        </li>
      ))}
    </ul>
  );
}

/** Cột chỉ bo góc trên (đầu dữ liệu), đáy vuông tại trục. */
function topRounded(x: number, y: number, w: number, h: number, r: number) {
  const rr = Math.min(r, w / 2, h);
  return `M${x},${y + h}V${y + rr}Q${x},${y} ${x + rr},${y}H${x + w - rr}Q${x + w},${y} ${x + w},${y + rr}V${y + h}Z`;
}

function niceMax(value: number) {
  if (value <= 4) return 4;
  const step = 10 ** Math.floor(Math.log10(value));
  const nice = [1, 2, 2.5, 5, 10].map((m) => m * step).find((m) => m * 4 >= value) ?? step * 10;
  return nice * 4;
}

/**
 * Cột chồng theo ngày: mỗi cột một ngày, các chuỗi xếp chồng theo thứ tự cố định, khe 2px màu nền giữa các phần, đầu cột
 * bo 4px. Rê chuột lên cột thì sáng nền cột kèm tooltip; `shown` thì ghi tổng trên đầu cột và số trong từng phần đủ cao.
 */
export function StackedColumns({ days, series, shown, height = 200 }: {
  days: { label: string; values: number[] }[];
  series: Series[];
  shown: boolean;
  height?: number;
}) {
  const [ref, width] = useWidth(520);
  const [hover, setHover] = useState<number | null>(null);
  const left = 28, right = 4, top = 18, bottom = 22;
  const plotH = height - top - bottom;
  const max = niceMax(Math.max(1, ...days.map((d) => d.values.reduce((a, b) => a + b, 0))));
  const band = (width - left - right) / Math.max(1, days.length);
  const barW = Math.min(24, band * 0.5);
  const y = (v: number) => top + plotH - (v / max) * plotH;
  const ticks = [0, max / 4, max / 2, (3 * max) / 4, max];
  return (
    <div ref={ref} className="w-full">
      <svg width={width} height={height} className="block overflow-visible" onMouseLeave={() => setHover(null)}>
        {ticks.map((t) => (
          <g key={t}>
            <line x1={left} x2={width - right} y1={y(t)} y2={y(t)} stroke="var(--c-line)" strokeWidth={1} />
            <text x={left - 6} y={y(t)} textAnchor="end" dominantBaseline="middle" fontSize={10} fill="var(--c-faint)" className="tabular-nums">{t}</text>
          </g>
        ))}
        {days.map((day, i) => {
          const cx0 = left + band * i + band / 2;
          const total = day.values.reduce((a, b) => a + b, 0);
          // Các phần khác 0 từ dưới lên, khe 2px giữa hai phần liền nhau.
          const parts: { v: number; si: number; yTop: number; h: number }[] = [];
          const nonZero = day.values.map((v, si) => ({ v, si })).filter((p) => p.v > 0);
          for (let pi = 0, cursor = y(0); pi < nonZero.length; pi++) {
            const p = nonZero[pi]!;
            const h = Math.max(2, (p.v / max) * plotH - (pi < nonZero.length - 1 ? 2 : 0));
            parts.push({ ...p, yTop: cursor - h, h });
            cursor = cursor - h - 2;
          }
          return (
            <g key={day.label} onMouseEnter={() => setHover(i)}>
              <title>{`${day.label}: ${series.map((s, si) => `${s.name} ${day.values[si] ?? 0}`).join(", ")}`}</title>
              <rect x={left + band * i} y={top} width={band} height={plotH} fill={hover === i ? "var(--c-line-soft)" : "transparent"} rx={6} />
              <g className="animate-grow-y motion-reduce:animate-none"
                style={{ transformOrigin: `${cx0}px ${y(0)}px`, animationDelay: `${120 + i * 60}ms` }}>
              {parts.map(({ v, si, yTop, h }, pi) => {
                const p = { v, si };
                const isTop = pi === parts.length - 1;
                const fill = series[si]!.color;
                return (
                  <g key={p.si}>
                    {isTop ? <path d={topRounded(cx0 - barW / 2, yTop, barW, h, 4)} fill={fill} />
                      : <rect x={cx0 - barW / 2} y={yTop} width={barW} height={h} fill={fill} />}
                    {shown && h >= 14 && parts.length > 1 && (
                      <text x={cx0} y={yTop + h / 2} textAnchor="middle" dominantBaseline="middle" fontSize={10} fontWeight={600} fill="var(--c-surface)">{p.v}</text>
                    )}
                  </g>
                );
              })}
              </g>
              {shown && total > 0 && (
                <text x={cx0} y={y(total) - 6} textAnchor="middle" fontSize={11} fontWeight={600} fill="var(--c-ink)" className="tabular-nums">{total}</text>
              )}
              <text x={cx0} y={height - 6} textAnchor="middle" fontSize={10} fill="var(--c-faint)">{day.label}</text>
            </g>
          );
        })}
        <line x1={left} x2={width - right} y1={y(0)} y2={y(0)} stroke="var(--c-line-strong)" strokeWidth={1} />
      </svg>
    </div>
  );
}

/**
 * Biểu đồ tròn rỗng giữa. Phần quá nhỏ (ví dụ chỉ một quản trị viên) vẫn vẽ tối thiểu 4,5% vòng để còn thấy; số thật hiện
 * ở tooltip và khi `shown` (ghi ngoài vòng, đúng giữa cung).
 */
export function Donut({ items, shown, size = 132 }: { items: (Series & { value: number })[]; shown: boolean; size?: number }) {
  const [hover, setHover] = useState<string | null>(null);
  const total = items.reduce((sum, item) => sum + item.value, 0);
  const visible = items.filter((item) => item.value > 0);
  const drawn = visible.map((item) => Math.max(item.value, total * 0.045));
  const drawnTotal = drawn.reduce((a, b) => a + b, 0);
  const c = size / 2, r = size / 2 - 22, thick = 16;
  const arcs: { item: Series & { value: number }; start: number; end: number; mid: number }[] = [];
  for (let i = 0, angle = -Math.PI / 2; i < visible.length; i++) {
    const sweep = (drawn[i]! / drawnTotal) * Math.PI * 2;
    arcs.push({ item: visible[i]!, start: angle, end: angle + sweep, mid: angle + sweep / 2 });
    angle += sweep;
  }
  const point = (a: number, radius: number) => [c + radius * Math.cos(a), c + radius * Math.sin(a)] as const;
  const gap = visible.length > 1 ? 0.035 : 0;
  const drawDelay = (i: number) => `${150 + i * 140}ms`;
  return (
    <svg width={size} height={size} className="block shrink-0 overflow-visible" onMouseLeave={() => setHover(null)}>
      {total === 0 && <circle cx={c} cy={c} r={r} fill="none" stroke="var(--c-line)" strokeWidth={thick} />}
      {arcs.map(({ item, start, end, mid }, index) => {
        const s = start + gap / 2, e = Math.max(s + 0.001, end - gap / 2);
        const [x1, y1] = point(s, r), [x2, y2] = point(e, r);
        const large = e - s > Math.PI ? 1 : 0;
        const [lx, ly] = point(mid, r + thick / 2 + 9);
        const d = visible.length === 1 ? `M${c},${c - r}A${r},${r} 0 1 1 ${c - 0.01},${c - r}` : `M${x1},${y1}A${r},${r} 0 ${large} 1 ${x2},${y2}`;
        return (
          <g key={item.name} onMouseEnter={() => setHover(item.name)}>
            <title>{`${item.name}: ${formatNumber(item.value)}`}</title>
            <path d={d} fill="none" stroke={item.color} strokeWidth={hover === item.name ? thick + 5 : thick}
              pathLength={1} strokeDasharray="1" style={{ animationDelay: drawDelay(index) }}
              className="animate-draw transition-[stroke-width] duration-200 ease-out motion-reduce:animate-none motion-reduce:transition-none" />
            {shown && (
              <text x={lx} y={ly} textAnchor={Math.cos(mid) > 0.2 ? "start" : Math.cos(mid) < -0.2 ? "end" : "middle"}
                dominantBaseline="middle" fontSize={11} fontWeight={600} fill="var(--c-ink)" className="tabular-nums">{formatNumber(item.value)}</text>
            )}
          </g>
        );
      })}
    </svg>
  );
}

/** Thanh tỷ lệ theo trạng thái; phần nhỏ vẫn rộng tối thiểu 7% để thấy; `shown` thì ghi số trong từng phần. */
export function SegmentBar({ items, shown }: { items: (Series & { value: number })[]; shown: boolean }) {
  const [hover, setHover] = useState<string | null>(null);
  const total = items.reduce((sum, item) => sum + item.value, 0);
  const visible = items.filter((item) => item.value > 0);
  if (total === 0) return <div className="h-4 rounded-[4px] bg-sunken" />;
  return (
    <div className="flex h-4 origin-left animate-grow-x gap-0.5 [animation-delay:200ms] motion-reduce:animate-none" onMouseLeave={() => setHover(null)}>
      {visible.map((item, i) => (
        <span key={item.name} title={`${item.name}: ${formatNumber(item.value)}`} onMouseEnter={() => setHover(item.name)}
          className={cx("flex min-w-0 items-center justify-center text-[10px] font-semibold text-surface tabular-nums",
            "transition-transform duration-200 ease-out motion-reduce:transition-none",
            hover === item.name && "scale-y-[1.35]",
            i === 0 && "rounded-l-[4px]", i === visible.length - 1 && "rounded-r-[4px]")}
          style={{ background: item.color, flexGrow: Math.max(item.value, total * 0.07), flexBasis: 0 }}>
          {shown && formatNumber(item.value)}
        </span>
      ))}
    </div>
  );
}

/** Đường theo ngày, vùng dưới tô nhạt; rê chuột ra tooltip từng ngày; `shown` thì ghi số gọn trên từng điểm. */
export function LineChart({ points, color, shown, height = 110 }: {
  points: { label: string; value: number }[];
  color: string;
  shown: boolean;
  height?: number;
}) {
  const [ref, width] = useWidth(260);
  const [hover, setHover] = useState<number | null>(null);
  const padX = 14, top = 18, bottom = 18;
  const max = Math.max(1, ...points.map((p) => p.value));
  const step = points.length > 1 ? (width - padX * 2) / (points.length - 1) : 0;
  const xy = points.map((p, i) => [padX + step * i, top + (height - top - bottom) * (1 - p.value / max)] as const);
  const line = xy.map(([x, y], i) => `${i ? "L" : "M"}${x},${y}`).join("");
  const base = height - bottom;
  return (
    <div ref={ref} className="w-full">
      <svg width={width} height={height} className="block overflow-visible" onMouseLeave={() => setHover(null)}>
        <line x1={padX} x2={width - padX} y1={base} y2={base} stroke="var(--c-line)" strokeWidth={1} />
        {xy.length > 1 && <path d={`${line}L${xy[xy.length - 1]![0]},${base}L${xy[0]![0]},${base}Z`} fill={color} opacity={0.1}
          className="animate-fade [animation-delay:700ms] [animation-duration:500ms] motion-reduce:animate-none" />}
        <path d={line} fill="none" stroke={color} strokeWidth={2} strokeLinejoin="round" strokeLinecap="round"
          pathLength={1} strokeDasharray="1" className="animate-draw [animation-delay:150ms] motion-reduce:animate-none" />
        {hover !== null && xy[hover] && <line x1={xy[hover][0]} x2={xy[hover][0]} y1={top - 6} y2={base} stroke="var(--c-line-strong)" strokeWidth={1} />}
        {points.map((p, i) => (
          <g key={p.label} onMouseEnter={() => setHover(i)}>
            <title>{`${p.label}: ${formatNumber(p.value)}`}</title>
            <rect x={xy[i]![0] - step / 2} y={0} width={Math.max(step, 16)} height={height} fill="transparent" />
            <circle cx={xy[i]![0]} cy={xy[i]![1]} r={hover === i ? 4.5 : 3} fill={color} stroke="var(--c-surface)" strokeWidth={2}
              style={{ animationDelay: `${300 + i * 90}ms` }} className="animate-fade motion-reduce:animate-none" />
            {shown && (
              <text x={xy[i]![0]} y={xy[i]![1] - 9} textAnchor="middle" fontSize={10} fontWeight={600} fill="var(--c-ink)" className="tabular-nums">
                {COMPACT.format(p.value)}
              </text>
            )}
            <text x={xy[i]![0]} y={height - 3} textAnchor="middle" fontSize={10} fill="var(--c-faint)">{p.label}</text>
          </g>
        ))}
      </svg>
    </div>
  );
}
