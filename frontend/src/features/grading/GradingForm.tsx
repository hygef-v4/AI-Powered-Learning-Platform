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
  partTitles?: Record<string, string>;
};

export function GradingForm({ rubrics, values, disabled, official, onChange, children, partTitles }: Props) {
  return (
    <div className="min-w-0 rounded-xl border border-line bg-white p-3.5 sm:p-4">
      <RubricChecklistPanel
        partTitles={partTitles}
        rubrics={rubrics}
        checks={values.checks}
        itemScores={values.itemScores}
        onScoresChange={(value) => onChange({ itemScores: value })}
        disabled={disabled}
        onChange={(value) => onChange({ checks: value })}
      />
      <div className="mt-3 flex flex-wrap items-center gap-3 text-sm">
        <label className="flex items-center gap-2 font-medium text-ink">
          Điều chỉnh (+/−)
          <input
            type="number"
            step="0.01"
            value={values.adjustment}
            disabled={disabled}
            onChange={(v) => onChange({ adjustment: v.target.value })}
            className="w-24 rounded-lg border border-line px-2 py-1.5 tabular-nums focus:border-primary focus:outline-none"
          />
        </label>
      </div>
      <label className="mt-2.5 block text-sm font-medium text-ink">
        Lý do điều chỉnh {Number(values.adjustment) !== 0 && "(bắt buộc)"}
        <input
          className="mt-1 block w-full rounded-lg border border-line px-2.5 py-1.5 text-sm font-normal focus:border-primary focus:outline-none"
          value={values.adjustmentReason}
          maxLength={2000}
          disabled={disabled}
          onChange={(v) => onChange({ adjustmentReason: v.target.value })}
        />
      </label>
      <label className="mt-2.5 block text-sm font-medium text-ink">
        Nhận xét
        <textarea
          className="mt-1 block min-h-24 w-full rounded-lg border border-line p-2.5 text-sm font-normal focus:border-primary focus:outline-none"
          value={values.feedback}
          maxLength={10000}
          disabled={disabled}
          onChange={(v) => onChange({ feedback: v.target.value })}
        />
      </label>
      {official && (
        <label className="mt-2.5 block text-sm font-medium text-ink">
          Lý do sửa điểm đã chốt/công bố (bắt buộc)
          <input
            className="mt-1 block w-full rounded-lg border border-line px-2.5 py-1.5 text-sm font-normal focus:border-primary focus:outline-none"
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
