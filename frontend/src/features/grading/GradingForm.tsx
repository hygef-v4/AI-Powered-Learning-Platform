"use client";
import type { ReactNode } from "react";
import { RubricChecklistPanel } from "./RubricChecklistPanel";
import type { GradingFormValues, Rubric } from "./types";

type Props = {
  rubrics: Rubric[];
  values: GradingFormValues;
  disabled: boolean;
  official: boolean;
  onChange: (patch: Partial<GradingFormValues>) => void;
  children?: ReactNode;
};

export function GradingForm({ rubrics, values, disabled, official, onChange, children }: Props) {
  return (
    <div className="rounded-xl border border-line bg-white p-5">
      <RubricChecklistPanel
        rubrics={rubrics}
        checks={values.checks}
        disabled={disabled}
        onChange={(value) => onChange({ checks: value })}
      />
      <label className="my-3 block">
        Điều chỉnh (+/−)
        <input
          type="number"
          step="0.01"
          value={values.adjustment}
          disabled={disabled}
          onChange={(v) => onChange({ adjustment: v.target.value })}
          className="ml-3 rounded border p-2"
        />
      </label>
      <label className="my-3 block">
        Lý do điều chỉnh {Number(values.adjustment) !== 0 && "(bắt buộc)"}
        <input
          className="mt-1 block w-full rounded border p-2"
          value={values.adjustmentReason}
          maxLength={2000}
          disabled={disabled}
          onChange={(v) => onChange({ adjustmentReason: v.target.value })}
        />
      </label>
      <label className="my-3 block">
        Nhận xét
        <textarea
          className="mt-1 block min-h-28 w-full rounded border p-2"
          value={values.feedback}
          maxLength={10000}
          disabled={disabled}
          onChange={(v) => onChange({ feedback: v.target.value })}
        />
      </label>
      {official && (
        <label className="block">
          Lý do sửa điểm đã chốt/công bố (bắt buộc)
          <input
            className="mt-1 block w-full rounded border p-2"
            value={values.changeReason}
            maxLength={2000}
            onChange={(v) => onChange({ changeReason: v.target.value })}
          />
        </label>
      )}
      {children}
    </div>
  );
}
