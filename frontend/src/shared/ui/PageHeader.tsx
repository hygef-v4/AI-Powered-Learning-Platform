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

/** Đầu trang: nút quay lại, tiêu đề, đường dẫn hoặc mô tả ngắn, thao tác; màn hẹp thì hàng nút xuống dưới tiêu đề. */
export function PageHeader({ title, subtitle, breadcrumb, backHref, actions }: Props) {
  return (
    <div className="flex flex-wrap items-center justify-between gap-x-2 gap-y-3">
      <div className="flex min-w-0 flex-[1_1_20rem] items-center gap-3">
        {backHref && (
          <Link
            href={backHref}
            aria-label="Quay lại"
            className="inline-flex size-[38px] shrink-0 items-center justify-center rounded-[11px] border border-line-strong bg-white text-body hover:bg-shell"
          >
            <Icon name="arrow_back" />
          </Link>
        )}
        <div className="min-w-0">
          <h1 className="flex items-center gap-2.5 text-2xl leading-[1.3] font-bold text-ink">{title}</h1>
          {breadcrumb && breadcrumb.length > 0 && (
            <nav aria-label="Đường dẫn" className="mt-[3px] text-xs">
              {breadcrumb.map((crumb, index) => {
                const last = index === breadcrumb.length - 1;
                return (
                  <Fragment key={`${index}-${crumb.label}`}>
                    {index > 0 && <span className="mx-1 text-faint">›</span>}
                    {crumb.href && !last ? (
                      <Link href={crumb.href} className="text-muted hover:text-ink">
                        {crumb.label}
                      </Link>
                    ) : (
                      <span className={last ? "font-semibold text-body" : "text-muted"}>{crumb.label}</span>
                    )}
                  </Fragment>
                );
              })}
            </nav>
          )}
          {subtitle && <p className="mt-1 text-[13px] text-muted">{subtitle}</p>}
        </div>
      </div>
      {actions && <div className="flex flex-wrap items-center gap-2">{actions}</div>}
    </div>
  );
}
