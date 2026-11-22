"use client";

import { Field, TextInput } from "@/shared/ui";
import { msg } from "@/i18n/msg";

export function QuestionCountField({ id, value, onChange, disabled, invalid }: {
  id: string; value: string; onChange: (value: string) => void; disabled: boolean; invalid: boolean;
}) {
  return (
    <Field label={msg("assignments.components.AiDraftDialog.numberOfQuestions")} htmlFor={id}
      error={invalid ? msg("assignments.components.AiDraftDialog.theNumberOfQuestionsMust") : undefined}>
      <TextInput id={id} type="number" min={1} max={10} step={1} value={value}
        placeholder={msg("assignments.components.AiDraftDialog.aiDecides")} disabled={disabled} invalid={invalid}
        aria-describedby={disabled ? undefined : `${id}-help`}
        onChange={(event) => onChange(event.target.value)} />
      {!disabled && <p id={`${id}-help`} className="text-xs text-muted">{msg("assignments.components.QuestionCountField.help")}</p>}
    </Field>
  );
}
