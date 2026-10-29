"use client";

import Link from "next/link";
import { useId } from "react";
import { Field, Pill, Section, Select, TextInput } from "@/shared/ui";
import { DIFFICULTY_LABELS, formatDateTime, STATUS_LABELS, STATUS_TONES } from "./labels";
import type { FormErrors, QuestionForm } from "./questionForm";
import type { Difficulty, QuestionDetail } from "./types";

/** "Phân loại": độ khó và thẻ (tối đa 10). */
export function QuestionMetaPanel({ value, onChange, errors, readOnly = false }: {
  value: QuestionForm;
  onChange: (value: QuestionForm) => void;
  errors: FormErrors;
  readOnly?: boolean;
}) {
  const id = useId();
  return (
    <Section title="Phân loại" icon="tune">
      <fieldset disabled={readOnly} className="flex flex-col gap-3">
        <Field label="Độ khó" htmlFor={`${id}-difficulty`}>
          <Select id={`${id}-difficulty`} value={value.difficulty} data-testid="question-meta-difficulty"
            onChange={(event) => onChange({ ...value, difficulty: event.target.value as Difficulty })}>
            {(Object.keys(DIFFICULTY_LABELS) as Difficulty[]).map((difficulty) => (
              <option key={difficulty} value={difficulty}>{DIFFICULTY_LABELS[difficulty]}</option>
            ))}
          </Select>
        </Field>
        <Field label="Thẻ" htmlFor={`${id}-tags`} hint="Cách nhau bằng dấu phẩy, tối đa 10 thẻ" error={errors.tags}>
          <TextInput id={`${id}-tags`} placeholder="uml, sơ đồ lớp" value={value.tagsText} invalid={!!errors.tags} data-testid="question-meta-tags"
            onChange={(event) => onChange({ ...value, tagsText: event.target.value })} />
        </Field>
      </fieldset>
    </Section>
  );
}

/** "Trạng thái": trạng thái, phiên bản, số quiz đang dùng và lịch sử phiên bản (BR-U10-06). */
export function QuestionVersionPanel({ detail }: { detail: QuestionDetail }) {
  const { question, versions, usedInQuizzes, draftId } = detail;
  return (
    <Section title="Trạng thái" icon="history">
      <dl className="grid grid-cols-[auto_1fr] gap-x-3 gap-y-1.5 text-[13px]">
        <dt className="text-muted">Trạng thái</dt>
        <dd><Pill tone={STATUS_TONES[question.status]}>{STATUS_LABELS[question.status]}</Pill></dd>
        <dt className="text-muted">Phiên bản</dt>
        <dd>v{question.versionNo} · {formatDateTime(question.updatedAt)}</dd>
        <dt className="text-muted">Quiz đang dùng</dt>
        <dd data-testid="question-version-used">{usedInQuizzes}</dd>
      </dl>
      {draftId && (
        <p className="mt-3 text-[13px]">
          <Link href={`/manager/questions/${draftId}`} className="font-semibold text-primary-ink hover:underline" data-testid="question-version-draft-link">
            Mở bản nháp mới của câu này
          </Link>
        </p>
      )}
      {versions.length > 1 && (
        <ol className="mt-3 flex flex-col gap-1 border-t border-line-soft pt-3 text-[13px]" aria-label="Lịch sử phiên bản">
          {versions.map((version) => (
            <li key={version.id} className="flex items-center justify-between gap-2">
              {version.id === question.id
                ? <span className="font-semibold">v{version.versionNo} (đang xem)</span>
                : <Link href={`/manager/questions/${version.id}`} className="text-primary-ink hover:underline">v{version.versionNo}</Link>}
              <Pill tone={STATUS_TONES[version.status]}>{STATUS_LABELS[version.status]}</Pill>
            </li>
          ))}
        </ol>
      )}
    </Section>
  );
}
