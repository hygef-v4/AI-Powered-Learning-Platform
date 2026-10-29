import Link from "next/link";
import type { ReactNode } from "react";
import { cx } from "./cx";
import { Icon } from "./Icon";

/** Ô số liệu: con số chữ đậm, nhãn nhỏ in hoa, một dòng phụ; có `href` thì cả ô bấm được. */
export function StatTile({
  label,
  value,
  meta,
  href,
  className,
  testId,
}: {
  label: ReactNode;
  value: ReactNode;
  meta?: ReactNode;
  href?: string;
  className?: string;
  testId?: string;
}) {
  const body = (
    <>
      <span className="text-[10.5px] font-semibold tracking-[.08em] text-muted uppercase">{label}</span>
      <span className="font-display text-[32px] leading-none tracking-[-0.02em] text-ink">{value}</span>
      {meta && <span className="text-[12.5px] text-muted">{meta}</span>}
      {href && (
        <Icon name="arrow_forward" size={15} className="absolute top-5 right-5 text-muted transition-transform duration-200 group-hover:translate-x-0.5 group-hover:text-ink" />
      )}
    </>
  );
  const box = cx(
    "group relative flex min-w-0 flex-col gap-2.5 rounded-xl border border-line bg-surface p-5",
    href && "transition-[border-color,box-shadow] duration-200 hover:border-line-strong hover:shadow-[var(--shadow-lift)]",
    className,
  );
  return href ? (
    <Link href={href} className={box} data-testid={testId}>
      {body}
    </Link>
  ) : (
    <div className={box} data-testid={testId}>
      {body}
    </div>
  );
}
