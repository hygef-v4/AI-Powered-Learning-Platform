"use client";
import type { Checks, Rubric } from "./types";

/** Integer cents keep the preview aligned with the server's decimal checklist sum. */
export function checklistPoints(rubrics: Rubric[], checks: Checks): number {
  return rubrics.reduce((total, rubric) => total + rubric.criteria.reduce((sum, criterion) =>
    sum + criterion.items.reduce((points, item) => points + ((checks[rubric.rubricId] ?? []).includes(item.itemId)
      ? Math.round(Number(item.points) * 100) : 0), 0), 0), 0) / 100;
}

export function RubricChecklistPanel({ rubrics, checks, onChange, disabled = false, partTitles = {} }: {
  rubrics: Rubric[]; checks: Checks; onChange: (value: Checks) => void; disabled?: boolean;
  partTitles?: Record<string, string>;
}) {
  return <section aria-label="Chấm theo rubric">
    <h2 className="text-base font-bold text-ink">Chấm theo rubric</h2>
    <p className="mt-1 text-sm text-muted">Tick các mục bài làm đáp ứng. Điểm được cộng theo các mục đã chọn.</p>
    {!rubrics.length && <p className="mt-3 rounded-lg bg-shell p-3 text-sm text-muted">Bài chưa có rubric khả dụng. Hãy kiểm tra lại cấu hình bài tập.</p>}
    {rubrics.map((rubric, index) => <fieldset key={rubric.rubricId} disabled={disabled}
      className="mt-4 min-w-0 rounded-xl border border-line p-3 sm:p-4">
      <legend className="max-w-full px-1 text-sm font-semibold text-ink">{partTitles[rubric.partId] || `Phần ${index + 1}`}</legend>
      <p className="mb-3 text-sm font-bold text-primary">{checklistPoints([rubric], checks).toFixed(2)} / {Number(rubric.maxPoints).toFixed(2)} điểm</p>
      {rubric.criteria.map((criterion) => <div key={criterion.criterionId}>
        <h3 className="mb-2 text-sm font-semibold text-body">{criterion.title}</h3>
        {criterion.items.map((item) => {
          const checked = (checks[rubric.rubricId] ?? []).includes(item.itemId);
          return <label key={item.itemId} className={`my-2 flex cursor-pointer items-start gap-3 rounded-lg border p-3 text-sm leading-relaxed transition-colors focus-within:ring-2 focus-within:ring-primary/30 ${checked ? "border-primary/30 bg-primary/5" : "border-transparent bg-shell/60 hover:border-line"} ${disabled ? "cursor-default opacity-60" : ""}`}>
            <input type="checkbox" checked={checked} className="mt-1 size-4 shrink-0 accent-primary"
              onChange={(event) => {
                const selected = new Set(checks[rubric.rubricId] ?? []);
                if (event.target.checked) selected.add(item.itemId); else selected.delete(item.itemId);
                onChange({ ...checks, [rubric.rubricId]: [...selected] });
              }} />
            <span className="flex-1">{item.text}<span className="mt-1 block text-xs font-semibold text-primary">{Number(item.points).toFixed(2)} điểm</span></span>
          </label>;
        })}
      </div>)}
    </fieldset>)}
  </section>;
}
