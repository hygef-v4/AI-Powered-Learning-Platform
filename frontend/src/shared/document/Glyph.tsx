import type { ReactNode } from "react";

export type GlyphName = "undo" | "redo" | "alignLeft" | "alignCenter" | "alignRight" | "justify" | "bullets" | "numbers"
  | "indent" | "outdent" | "image" | "table" | "diagram" | "highlight";

const GLYPHS: Record<GlyphName, ReactNode> = {
  undo: <><path d="M9 14 4 9l5-5" /><path d="M4 9h10.5a5.5 5.5 0 0 1 0 11H11" /></>,
  redo: <><path d="m15 14 5-5-5-5" /><path d="M20 9H9.5a5.5 5.5 0 0 0 0 11H13" /></>,
  alignLeft: <path d="M4 6h16M4 10h10M4 14h16M4 18h10" />,
  alignCenter: <path d="M4 6h16M7 10h10M4 14h16M7 18h10" />,
  alignRight: <path d="M4 6h16M10 10h10M4 14h16M10 18h10" />,
  justify: <path d="M4 6h16M4 10h16M4 14h16M4 18h16" />,
  bullets: <><path d="M10 7h10M10 12h10M10 17h10" /><circle cx="5" cy="7" r="1" /><circle cx="5" cy="12" r="1" /><circle cx="5" cy="17" r="1" /></>,
  numbers: <path d="M10 7h10M10 12h10M10 17h10M4 5h1.5v4M4 9h3M4 15.5c.5-.8 2.5-.8 2.5.5S4 17.5 4 19h3" />,
  indent: <path d="M4 5h16M10 10h10M10 14h10M4 19h16M3 9l3 3-3 3" />,
  outdent: <path d="M4 5h16M10 10h10M10 14h10M4 19h16M6 9l-3 3 3 3" />,
  image: <><rect x="3" y="4" width="18" height="16" rx="2" /><circle cx="8.5" cy="9.5" r="1.5" /><path d="m21 16-5-5L6 20" /></>,
  table: <><rect x="3" y="4" width="18" height="16" rx="2" /><path d="M3 10h18M3 15h18M9 4v16M15 4v16" /></>,
  diagram: <><rect x="3" y="3" width="7" height="6" rx="1" /><rect x="14" y="15" width="7" height="6" rx="1" /><path d="M6.5 9v4.5a1.5 1.5 0 0 0 1.5 1.5h6" /></>,
  highlight: <><path d="m9 11-6 6v3h6l3-3" /><path d="m22 12-4.6 4.6a2 2 0 0 1-2.8 0l-5.2-5.2a2 2 0 0 1 0-2.8L14 4" /></>,
};

/** Ký hiệu nét mảnh trên thanh công cụ của các trình soạn (trình soạn tài liệu, trình soạn chữ có định dạng). */
export function Glyph({ name }: { name: GlyphName }) {
  return (
    <svg aria-hidden viewBox="0 0 24 24" width={18} height={18} fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round">
      {GLYPHS[name]}
    </svg>
  );
}
