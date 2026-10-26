import Link from "next/link";
import { Fragment, type ReactNode } from "react";
import { Icon } from "./Icon";

export type Crumb = { label: string; href?: string };

type Props = {
  title: ReactNode;
  subtitle?: ReactNode;
  breadcrumb?: Crumb[];
  backHref?: string;
  /** Nút thao tác bên phải. */
  actions?: ReactNode;
};

/** Đầu trang: đường dẫn nhỏ phía trên, tiêu đề chữ có chân, một dòng mô tả, thao tác căn đáy bên phải. */
export function PageHeader({ title, subtitle, breadcrumb, backHref, actions }: Props) {
  const trail = breadcrumb?.slice(0, -1) ?? [];
  return (
    <header className="flex animate-rise flex-wrap items-end justify-between gap-x-6 gap-y-4 pb-1">
      <div className="flex min-w-0 flex-[1_1_22rem] flex-col gap-2.5">
        {(backHref || trail.length > 0) && (
          <nav aria-label="Đường dẫn" className="flex flex-wrap items-center gap-1 text-[12.5px] font-semibold text-muted">
            {backHref && (
              <Link
                href={backHref}
                aria-label="Quay lại"
                className="-ml-1 inline-flex size-6 items-center justify-center rounded-md transition-colors hover:bg-sunken hover:text-ink"
              >
                <Icon name="arrow_back" size={14} />
              </Link>
            )}
            {trail.map((crumb, index) => (
              <Fragment key={`${index}-${crumb.label}`}>
                {index > 0 && <Icon name="chevron_right" size={11} className="text-faint" />}
                {crumb.href ? (
                  <Link href={crumb.href} className="transition-colors hover:text-ink">
                    {crumb.label}
                  </Link>
                ) : (
                  <span>{crumb.label}</span>
                )}
              </Fragment>
            ))}
          </nav>
        )}
        <h1 className="flex flex-wrap items-center gap-3 font-display text-[34px] leading-[1.1] tracking-[-0.025em] text-ink sm:text-[40px]">
          {title}
        </h1>
        {subtitle && <p className="text-[13.5px] text-muted">{subtitle}</p>}
      </div>
      {actions && <div className="flex flex-wrap items-center gap-2">{actions}</div>}
    </header>
  );
}
