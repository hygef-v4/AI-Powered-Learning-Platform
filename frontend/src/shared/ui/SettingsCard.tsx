import type { ReactNode } from "react";
import { cx } from "./cx";
import { InfoTip } from "./InfoTip";

/** Một thẻ cài đặt duy nhất; các nhóm nằm trong thẻ, ngăn bằng đường kẻ. */
export function SettingsCard({ children, className }: { children: ReactNode; className?: string }) {
  return (
    <div className={cx("flex flex-col rounded-2xl border border-line bg-surface px-5 sm:px-6", className)}>{children}</div>
  );
}

/** Nhóm trong thẻ cài đặt: tiêu đề nhỏ, thao tác của nhóm bên phải, không có dòng mô tả. */
export function SettingsGroup({ id, title, action, children }: { id?: string; title: string; action?: ReactNode; children: ReactNode }) {
  return (
    <section id={id} className="flex scroll-mt-4 flex-col border-t border-line py-4 first:border-t-0">
      <div className="flex min-h-8 items-center justify-between gap-3 pb-1">
        <h2 className="text-[15px] font-semibold text-ink">{title}</h2>
        {action}
      </div>
      {children}
    </section>
  );
}

type LineProps = {
  label: ReactNode;
  htmlFor?: string;
  /** Lời giải thích, chỉ hiện khi rê chuột vào chữ i. */
  info?: string;
  control: ReactNode;
  /** Ô bên phải ô nhập, giữ chỗ cố định để các ô nhập thẳng hàng (nút Lưu hiện khi sửa). */
  action?: ReactNode;
  error?: string | null;
  muted?: boolean;
};

/** Một dòng cài đặt kiểu màn cài đặt game: tên bên trái, ô nhập bên phải trên cùng một dòng. */
export function SettingLine({ label, htmlFor, info, control, action, error, muted }: LineProps) {
  const name = typeof label === "string" ? label : "";
  return (
    <div className="grid grid-cols-1 items-center gap-x-6 gap-y-2 py-2.5 sm:grid-cols-[minmax(0,1fr)_auto]">
      <div className={cx("flex min-w-0 items-center gap-1.5 text-sm font-medium", muted ? "text-muted" : "text-ink")}>
        {htmlFor ? <label htmlFor={htmlFor}>{label}</label> : <span>{label}</span>}
        {info && <InfoTip text={info} label={name} />}
      </div>
      <div className="flex items-center gap-2 sm:justify-end">
        {control}
        <span className="flex w-16 shrink-0 justify-end">{action}</span>
      </div>
      {error && (
        <p role="alert" className="text-xs text-danger sm:col-span-2 sm:text-right sm:pr-[72px]">
          {error}
        </p>
      )}
    </div>
  );
}
