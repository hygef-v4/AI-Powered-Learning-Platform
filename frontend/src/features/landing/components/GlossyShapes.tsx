import { INK_ORANGE, INK_TEAL } from "@/shared/ui/Logo";

/**
 * Các vật thể "nhựa bóng" của landing, vẽ bằng SVG từ đúng bộ màu của logo (cam, xanh lơ) cùng xanh primary và tím accent
 * của theme. Mỗi hình là một đồ vật của môn thiết kế phần mềm: cây bút chấm nghiên trong logo, vòng nghiên mực, tam giác
 * kế thừa UML, khối lớp ba ngăn, ngôi sao điểm thưởng và viên bi mực. Chỉ để trang trí nên trình đọc màn hình bỏ qua.
 *
 * Độ bóng gồm ba lớp: thân tô gradient chéo (sáng ở góc trên trái, tối ở góc dưới phải), một vệt sáng trắng mờ dọc mép
 * trên và một đốm phản chiếu nhỏ. `uid` phân biệt id gradient khi một hình xuất hiện nhiều lần trên trang.
 */

export type GlossyTone = "orange" | "teal" | "blue" | "lilac";

const TONES: Record<GlossyTone, [string, string, string]> = {
  orange: ["#ffc08f", INK_ORANGE, "#9c4513"],
  teal: ["#8be3ef", INK_TEAL, "#0f5866"],
  blue: ["#c4daf5", "#6f93c2", "#2a4a73"],
  lilac: ["#ecdcfb", "#a884d6", "#583887"],
};

function Body({ id, tone }: { id: string; tone: GlossyTone }) {
  const [light, mid, deep] = TONES[tone];
  return (
    <>
      <linearGradient id={`${id}-body`} x1="0.1" y1="0" x2="0.9" y2="1">
        <stop offset="0" stopColor={light} />
        <stop offset="0.45" stopColor={mid} />
        <stop offset="1" stopColor={deep} />
      </linearGradient>
      <radialGradient id={`${id}-shine`} cx="0.32" cy="0.22" r="0.55">
        <stop offset="0" stopColor="#ffffff" stopOpacity="0.75" />
        <stop offset="0.35" stopColor="#ffffff" stopOpacity="0.18" />
        <stop offset="1" stopColor="#ffffff" stopOpacity="0" />
      </radialGradient>
    </>
  );
}

type ShapeProps = { uid: string; tone: GlossyTone; className?: string };

/** Cây bút của logo: cán tròn, khâu kim loại và ngòi xanh lơ. */
export function GlossyPen({ uid, className }: Omit<ShapeProps, "tone">) {
  return (
    <svg viewBox="0 0 120 260" aria-hidden="true" className={className}>
      <defs>
        <Body id={`${uid}-a`} tone="orange" />
        <Body id={`${uid}-b`} tone="teal" />
      </defs>
      <rect x="30" y="8" width="60" height="164" rx="30" fill={`url(#${uid}-a-body)`} />
      <rect x="30" y="8" width="60" height="164" rx="30" fill={`url(#${uid}-a-shine)`} />
      <rect x="40" y="22" width="10" height="120" rx="5" fill="#ffffff" opacity="0.45" />
      <rect x="24" y="166" width="72" height="22" rx="6" fill={`url(#${uid}-b-body)`} />
      <path d="M26 192 C26 214 50 236 60 254 C70 236 94 214 94 192 Z" fill={`url(#${uid}-b-body)`} />
      <path d="M26 192 C26 214 50 236 60 254 C70 236 94 214 94 192 Z" fill={`url(#${uid}-b-shine)`} />
      <ellipse cx="44" cy="200" rx="6" ry="10" fill="#ffffff" opacity="0.5" />
    </svg>
  );
}

/** Vòng nghiên mực: khung chữ nhật bo tròn, rỗng giữa. */
export function GlossyRing({ uid, tone, className }: ShapeProps) {
  return (
    <svg viewBox="0 0 240 160" aria-hidden="true" className={className}>
      <defs>
        <Body id={uid} tone={tone} />
      </defs>
      <rect x="24" y="24" width="192" height="112" rx="56" fill="none" stroke={`url(#${uid}-body)`} strokeWidth="38" />
      <rect x="24" y="24" width="192" height="112" rx="56" fill="none" stroke={`url(#${uid}-shine)`} strokeWidth="38" />
      <path d="M58 16 H170" stroke="#ffffff" strokeOpacity="0.55" strokeWidth="7" strokeLinecap="round" />
      <circle cx="196" cy="34" r="5" fill="#ffffff" opacity="0.7" />
    </svg>
  );
}

/** Tam giác rỗng của quan hệ tổng quát hóa (kế thừa) trong sơ đồ lớp. */
export function GlossyTriangle({ uid, tone, className }: ShapeProps) {
  return (
    <svg viewBox="0 0 220 200" aria-hidden="true" className={className}>
      <defs>
        <Body id={uid} tone={tone} />
      </defs>
      <path d="M110 26 L194 170 H26 Z" fill="none" stroke={`url(#${uid}-body)`} strokeWidth="32" strokeLinejoin="round" />
      <path d="M110 26 L194 170 H26 Z" fill="none" stroke={`url(#${uid}-shine)`} strokeWidth="32" strokeLinejoin="round" />
      <path d="M96 40 L58 104" stroke="#ffffff" strokeOpacity="0.55" strokeWidth="7" strokeLinecap="round" />
    </svg>
  );
}

/** Khối lớp UML ba ngăn: tên, thuộc tính, phương thức. */
export function GlossyClassBlock({ uid, tone, className }: ShapeProps) {
  return (
    <svg viewBox="0 0 200 220" aria-hidden="true" className={className}>
      <defs>
        <Body id={uid} tone={tone} />
      </defs>
      <rect x="14" y="14" width="172" height="192" rx="34" fill={`url(#${uid}-body)`} />
      <rect x="14" y="14" width="172" height="192" rx="34" fill={`url(#${uid}-shine)`} />
      <path d="M22 70 H178 M22 140 H178" stroke="#000000" strokeOpacity="0.16" strokeWidth="6" />
      <path d="M22 76 H178 M22 146 H178" stroke="#ffffff" strokeOpacity="0.35" strokeWidth="3" />
      <rect x="40" y="34" width="70" height="14" rx="7" fill="#ffffff" opacity="0.5" />
    </svg>
  );
}

/** Ngôi sao năm cánh bo tròn, như con dấu điểm thưởng. */
export function GlossyStar({ uid, tone, className }: ShapeProps) {
  return (
    <svg viewBox="0 0 220 220" aria-hidden="true" className={className}>
      <defs>
        <Body id={uid} tone={tone} />
      </defs>
      <path
        d="M110 18 C120 18 124 30 130 52 L138 78 L166 78 C192 78 204 82 206 92 C208 102 198 110 178 124 L156 140 L164 166 C172 190 172 202 164 206 C156 212 144 204 124 190 L110 180 L96 190 C76 204 64 212 56 206 C48 202 48 190 56 166 L64 140 L42 124 C22 110 12 102 14 92 C16 82 28 78 54 78 L82 78 L90 52 C96 30 100 18 110 18 Z"
        fill={`url(#${uid}-body)`}
      />
      <path
        d="M110 18 C120 18 124 30 130 52 L138 78 L166 78 C192 78 204 82 206 92 C208 102 198 110 178 124 L156 140 L164 166 C172 190 172 202 164 206 C156 212 144 204 124 190 L110 180 L96 190 C76 204 64 212 56 206 C48 202 48 190 56 166 L64 140 L42 124 C22 110 12 102 14 92 C16 82 28 78 54 78 L82 78 L90 52 C96 30 100 18 110 18 Z"
        fill={`url(#${uid}-shine)`}
      />
      <ellipse cx="96" cy="62" rx="9" ry="16" fill="#ffffff" opacity="0.5" transform="rotate(20 96 62)" />
    </svg>
  );
}

/** Viên bi mực. */
export function GlossyDrop({ uid, tone, className }: ShapeProps) {
  return (
    <svg viewBox="0 0 120 120" aria-hidden="true" className={className}>
      <defs>
        <Body id={uid} tone={tone} />
      </defs>
      <circle cx="60" cy="60" r="50" fill={`url(#${uid}-body)`} />
      <circle cx="60" cy="60" r="50" fill={`url(#${uid}-shine)`} />
      <ellipse cx="42" cy="36" rx="12" ry="8" fill="#ffffff" opacity="0.6" transform="rotate(-30 42 36)" />
    </svg>
  );
}
