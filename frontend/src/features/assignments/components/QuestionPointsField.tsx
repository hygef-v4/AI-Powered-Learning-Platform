"use client";

import { Field, TextInput } from "@/shared/ui";
import { LOCALE, msg } from "@/i18n/msg";

export function QuestionPointsField({ id, value, onChange, points, disabled }: {
  id: string; value: string; onChange: (value: string) => void; points: number[] | null; disabled: boolean;
}) {
  const invalid = points === null;
  const total = points?.reduce((cents, point) => cents + Math.round(point * 100), 0) ?? 0;
  return (
    <Field label={msg("assignments.components.QuestionPointsField.label")} htmlFor={id}
      error={invalid ? msg("assignments.components.QuestionPointsField.invalid") : undefined}>
      <TextInput id={id} value={value} onChange={(event) => onChange(event.target.value)}
        disabled={disabled} invalid={invalid} maxLength={100} aria-describedby={`${id}-help`} />
      <p id={`${id}-help`} className="text-xs text-muted">
        {points?.length ? msg("assignments.components.QuestionPointsField.summary", {
          count: String(points.length), total: (total / 100).toLocaleString(LOCALE),
        }) : msg("assignments.components.QuestionPointsField.help")}
      </p>
    </Field>
  );
}
