"use client";

import { useId } from "react";
import { Button, cx, Field, Icon, TextInput } from "@/shared/ui";
import { TYPE_LABELS } from "./labels";
import { MAX_OPTIONS, MIN_OPTIONS, nextOptionId, toggleCorrect, withType, type FormErrors, type QuestionForm } from "./questionForm";
import type { QuestionType } from "./types";

const TEXTAREA = "w-full rounded-[9px] border border-line-strong px-3 py-2 text-[13px] text-ink focus-visible:outline-primary disabled:bg-shell";

type Props = {
  value: QuestionForm;
  onChange: (value: QuestionForm) => void;
  errors: FormErrors;
  readOnly?: boolean;
};

/**
 * Soạn câu trắc nghiệm (BR-U10-02, 03): loại câu, nội dung, 2–6 lựa chọn kèm đáp án đúng, điểm mặc định, giải thích.
 * Dùng ở Question Detail và (giai đoạn 2) câu riêng của Quiz.
 */
export function McqEditor({ value, onChange, errors, readOnly = false }: Props) {
  const id = useId();
  const single = value.type === "MCQ_SINGLE";

  const setOption = (optionId: string, text: string) =>
    onChange({ ...value, options: value.options.map((option) => (option.id === optionId ? { ...option, text } : option)) });
  const removeOption = (optionId: string) =>
    onChange({
      ...value,
      options: value.options.filter((option) => option.id !== optionId),
      correctOptionIds: value.correctOptionIds.filter((correct) => correct !== optionId),
    });
  const addOption = () => onChange({ ...value, options: [...value.options, { id: nextOptionId(value.options), text: "" }] });

  return (
    <fieldset disabled={readOnly} className="flex min-w-0 flex-col gap-4" data-testid="mcq-editor">
      <div role="radiogroup" aria-label="Loại câu hỏi" className="flex flex-wrap gap-2">
        {(Object.keys(TYPE_LABELS) as QuestionType[]).map((type) => (
          <button key={type} type="button" role="radio" aria-checked={value.type === type}
            onClick={() => onChange(withType(value, type))} data-testid={`mcq-editor-type-${type}`}
            className={cx("rounded-full border px-3 py-1 text-xs font-semibold",
              value.type === type ? "border-primary/30 bg-primary-soft text-primary" : "border-line-strong bg-white text-body hover:bg-shell")}>
            {TYPE_LABELS[type]}
          </button>
        ))}
      </div>

      <Field label="Nội dung câu hỏi" htmlFor={`${id}-stem`} required error={errors.stem}>
        <textarea id={`${id}-stem`} value={value.stem} rows={4} aria-invalid={!!errors.stem}
          onChange={(event) => onChange({ ...value, stem: event.target.value })} className={TEXTAREA} data-testid="mcq-editor-stem" />
      </Field>

      <div className="flex flex-col gap-2">
        <p className="text-xs font-semibold text-body">
          Các lựa chọn ({MIN_OPTIONS}–{MAX_OPTIONS}) <span className="font-normal text-muted">· {single ? "chọn 1 đáp án đúng" : "chọn các đáp án đúng"}</span>
        </p>
        <ul className="flex flex-col gap-2" aria-label="Các lựa chọn">
          {value.options.map((option, index) => {
            const correct = value.correctOptionIds.includes(option.id);
            return (
              <li key={option.id} className="flex items-center gap-2">
                <label className="flex items-center gap-1.5 text-xs text-muted">
                  <input type={single ? "radio" : "checkbox"} name={`${id}-correct`} checked={correct}
                    onChange={() => onChange(toggleCorrect(value, option.id))}
                    aria-label={`Lựa chọn ${index + 1} là đáp án đúng`} data-testid={`mcq-editor-correct-${index}`} />
                  {String.fromCharCode(65 + index)}
                </label>
                <TextInput value={option.text} onChange={(event) => setOption(option.id, event.target.value)}
                  aria-label={`Nội dung lựa chọn ${index + 1}`} wrapperClassName="flex-1" invalid={!!errors.options}
                  data-testid={`mcq-editor-option-${index}`} />
                <Button size="sm" variant="ghost" aria-label={`Xóa lựa chọn ${index + 1}`} disabled={value.options.length <= MIN_OPTIONS}
                  onClick={() => removeOption(option.id)} data-testid={`mcq-editor-remove-${index}`}>
                  <Icon name="close" size={16} />
                </Button>
              </li>
            );
          })}
        </ul>
        {!readOnly && (
          <div>
            <Button size="sm" icon="add" onClick={addOption} disabled={value.options.length >= MAX_OPTIONS} data-testid="mcq-editor-add-option">
              Thêm lựa chọn
            </Button>
          </div>
        )}
        {errors.options && <p role="alert" className="text-[11px] text-danger">{errors.options}</p>}
        {errors.correctOptionIds && <p role="alert" className="text-[11px] text-danger">{errors.correctOptionIds}</p>}
      </div>

      <Field label="Điểm (mặc định)" htmlFor={`${id}-points`} required error={errors.defaultPoints}>
        <TextInput id={`${id}-points`} inputMode="decimal" value={value.defaultPoints} wrapperClassName="max-w-40"
          invalid={!!errors.defaultPoints} onChange={(event) => onChange({ ...value, defaultPoints: event.target.value })}
          data-testid="mcq-editor-points" />
      </Field>

      <Field label="Giải thích (tùy chọn)" htmlFor={`${id}-explanation`} error={errors.explanation}>
        <textarea id={`${id}-explanation`} value={value.explanation} rows={3}
          onChange={(event) => onChange({ ...value, explanation: event.target.value })} className={TEXTAREA}
          data-testid="mcq-editor-explanation" />
      </Field>
    </fieldset>
  );
}
