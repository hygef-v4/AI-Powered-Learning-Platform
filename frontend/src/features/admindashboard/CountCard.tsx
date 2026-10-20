import {Section} from "@/shared/ui/Section";

export const formatCount = (value: number) => new Intl.NumberFormat("vi-VN").format(value);

export function CountCard({title, value, detail, testId}: {title: string; value: number | null; detail?: string; testId: string}) {
  return <div data-testid={testId}>
    <Section title={title}>
      {value === null ? <p className="text-sm text-muted">Không khả dụng</p> : <>
        <p className="text-3xl font-bold tabular-nums text-ink">{formatCount(value)}</p>
        {detail && <p className="text-xs text-muted">{detail}</p>}
      </>}
    </Section>
  </div>;
}
