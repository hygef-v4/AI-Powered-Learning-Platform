"use client";
import type { Checks, Rubric } from "./types";

export function RubricChecklistPanel({
  rubrics,
  checks,
  onChange,
  disabled = false,
}: {
  rubrics: Rubric[];
  checks: Checks;
  onChange: (value: Checks) => void;
  disabled?: boolean;
}) {
  return (
    <section>
      <h2>Chấm theo rubric</h2>
      {rubrics.map((r) => (
        <fieldset
          key={r.rubricId}
          disabled={disabled}
          className="my-4 rounded-xl border border-line p-4"
        >
          <legend>
            {r.partId} · tối đa {Number(r.maxPoints).toFixed(2)} điểm
          </legend>
          {r.criteria.map((c) => (
            <div key={c.criterionId}>
              <h3 className="my-2 font-semibold">{c.title}</h3>
              {c.items.map((i) => (
                <label key={i.itemId} className="my-2 flex items-start gap-3">
                  <input
                    type="checkbox"
                    checked={(checks[r.rubricId] ?? []).includes(i.itemId)}
                    onChange={(event) => {
                      const selected = new Set(checks[r.rubricId] ?? []);
                      if (event.target.checked) selected.add(i.itemId);
                      else selected.delete(i.itemId);
                      onChange({ ...checks, [r.rubricId]: [...selected] });
                    }}
                  />
                  <span>
                    {i.text} <strong>({Number(i.points).toFixed(2)} điểm)</strong>
                  </span>
                </label>
              ))}
            </div>
          ))}
        </fieldset>
      ))}
    </section>
  );
}
