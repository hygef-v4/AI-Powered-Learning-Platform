import type { ReactNode } from "react";
import { Icon } from "@/shared/ui/Icon";

/**
 * Thẻ trắng ở góc trên khối giới thiệu (design `Main`, `c02*`, `c03*`, `c04`): icon khiên, tiêu đề, dòng phụ,
 * rồi một đoạn chữ (`text`) hoặc các gạch đầu dòng (`points`).
 */
export function InfoCard({
  title,
  subtitle,
  points,
  text,
}: {
  title: string;
  subtitle: string;
  points?: string[];
  text?: ReactNode;
}) {
  return (
    <div className="w-[300px] rounded-2xl border border-white/40 bg-white/95 p-4 text-ink shadow-[0_18px_40px_rgba(15,23,42,.18)]">
      <div className="flex items-start gap-3">
        <span className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-[#eff6ff] text-primary">
          <Icon name="verified_user" size={20} />
        </span>
        <div className="min-w-0">
          <p className="text-sm font-bold">{title}</p>
          <p className="text-xs text-[#64748b]">{subtitle}</p>
        </div>
      </div>
      <div className="mt-3 border-t border-[#e2e8f0] pt-3 text-xs leading-relaxed text-[#475569]">
        {text}
        {points && (
          <ul className="space-y-1.5">
            {points.map((point) => (
              <li key={point} className="flex items-start gap-2">
                <span aria-hidden="true" className="mt-[5px] size-1.5 shrink-0 rounded-full bg-[#4f46e5]" />
                {point}
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}
