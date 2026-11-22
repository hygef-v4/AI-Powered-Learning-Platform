import { INK_ORANGE, INK_TEAL } from "@/shared/ui/Logo";

/**
 * Hình minh họa của landing: hai lớp con kế thừa một lớp cha (mũi tên tổng quát hóa UML); lớp màu cam đã được chấm
 * điểm. Chữ trong hình là tên lớp mẫu, không cần dịch. Trình đọc màn hình bỏ qua (nội dung đã có ở chữ bên cạnh).
 */
export function DiagramIllustration({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 420 320" aria-hidden="true" className={className} fontFamily="var(--font-mono, ui-monospace, monospace)">
      {/* Lớp cha */}
      <g transform="translate(135 20)">
        <rect width="150" height="92" rx="8" fill="var(--c-surface)" stroke="var(--c-line-strong)" strokeWidth="2" />
        <rect width="150" height="30" rx="8" fill="var(--c-primary-soft)" />
        <rect y="22" width="150" height="8" fill="var(--c-primary-soft)" />
        <line x1="0" y1="30" x2="150" y2="30" stroke="var(--c-line-strong)" strokeWidth="2" />
        <text x="75" y="20" textAnchor="middle" fontSize="13" fontWeight="700" fill="var(--c-ink)">Person</text>
        <text x="12" y="52" fontSize="11" fill="var(--c-body)">- name: String</text>
        <text x="12" y="70" fontSize="11" fill="var(--c-body)">- email: String</text>
        <text x="12" y="86" fontSize="11" fill="var(--c-muted)">+ login(): void</text>
      </g>

      {/* Đường kế thừa và đầu mũi tên tam giác rỗng */}
      <path d="M110 200 V170 H310 V200 M210 170 V128" fill="none" stroke="var(--c-body)" strokeWidth="2" />
      <path d="M210 113 L199 129 H221 Z" fill="var(--c-surface)" stroke="var(--c-body)" strokeWidth="2" strokeLinejoin="round" />

      {/* Lớp con bên trái */}
      <g transform="translate(35 200)">
        <rect width="150" height="74" rx="8" fill="var(--c-surface)" stroke="var(--c-line-strong)" strokeWidth="2" />
        <line x1="0" y1="30" x2="150" y2="30" stroke="var(--c-line-strong)" strokeWidth="2" />
        <text x="75" y="20" textAnchor="middle" fontSize="13" fontWeight="700" fill="var(--c-ink)">Lecturer</text>
        <text x="12" y="52" fontSize="11" fill="var(--c-body)">- staffId: String</text>
        <text x="12" y="68" fontSize="11" fill="var(--c-muted)">+ grade(): Score</text>
      </g>

      {/* Lớp con bên phải: bài đã chấm, viền cam */}
      <g transform="translate(235 200)">
        <rect width="150" height="74" rx="8" fill="var(--c-surface)" stroke={INK_ORANGE} strokeWidth="2.5" />
        <line x1="0" y1="30" x2="150" y2="30" stroke={INK_ORANGE} strokeWidth="2" />
        <text x="75" y="20" textAnchor="middle" fontSize="13" fontWeight="700" fill="var(--c-ink)">Student</text>
        <text x="12" y="52" fontSize="11" fill="var(--c-body)">- studentId: String</text>
        <text x="12" y="68" fontSize="11" fill="var(--c-muted)">+ submit(): void</text>
      </g>

      {/* Huy hiệu điểm */}
      <g transform="translate(362 192)">
        <circle r="26" fill={INK_ORANGE} />
        <text y="6" textAnchor="middle" fontSize="16" fontWeight="700" fill="#fff">9.5</text>
      </g>

      {/* Dấu tích đã duyệt */}
      <g transform="translate(60 52)">
        <circle r="18" fill={INK_TEAL} />
        <path d="M-8 0 L-2 6 L9 -6" fill="none" stroke="#fff" strokeWidth="3.5" strokeLinecap="round" strokeLinejoin="round" />
      </g>
    </svg>
  );
}
