import type { ReactNode } from "react";

/** Con số lớn trong thẻ Usage. */
export function Big({ children }: { children: ReactNode }) {
  return <span className="text-[40px] leading-none font-bold tracking-[-0.02em] tabular-nums md:text-[48px]">{children}</span>;
}

/** Thẻ cam lớn: số token là phần nổi nhất. */
export function TokenCard({ title, action, className, children }: { title: string; action?: ReactNode; className?: string; children: ReactNode }) {
  return (
    <div className={`flex min-w-0 flex-col gap-4 rounded-2xl bg-primary p-6 text-on-primary ${className ?? ""}`}>
      <div className="flex items-center justify-between gap-3">
        <p className="text-sm font-semibold opacity-90">{title}</p>
        {action}
      </div>
      <div>{children}</div>
    </div>
  );
}

/** Thẻ trắng: một nhãn và một con số. */
export function CountCard({ title, value }: { title: string; value: ReactNode }) {
  return (
    <div className="flex min-w-0 flex-col justify-between gap-4 rounded-2xl border border-line bg-surface p-6">
      <p className="text-sm font-medium text-muted">{title}</p>
      <p className="text-[40px] leading-none font-bold text-ink tabular-nums">{value}</p>
    </div>
  );
}
