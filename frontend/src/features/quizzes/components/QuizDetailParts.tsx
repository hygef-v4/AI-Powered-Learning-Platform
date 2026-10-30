"use client";

import { useId, useState } from "react";
import { Alert, Button, Dialog, Field, Icon, Pill, Section, Switch, TextInput } from "@/shared/ui";
import { formatPoints, TYPE_LABELS } from "../model/labels";
import { McqEditor } from "./McqEditor";
import { QuestionMetaPanel } from "./QuestionSidePanels";
import { emptyForm, formOf, toContent, validateForm, type FormErrors, type QuestionForm } from "../model/questionForm";
import type { QuestionContent, QuizDetailsInput, QuizItemView, QuizSettings } from "../model/types";

export type DraftItem = { item: QuizItemView; points: string };

const TEXTAREA = "w-full rounded-lg border border-line-strong px-3 py-2 text-[13px] text-ink focus-visible:outline-focus disabled:bg-shell";

/** "Câu hỏi (n)": thứ tự, nguồn câu, điểm, nội dung và đáp án; sửa điểm, đổi thứ tự, bỏ câu khi quiz còn nháp. */
export function QuizItemList({ items, onChange, editable, onEditInline }: {
  items: DraftItem[];
  onChange: (items: DraftItem[]) => void;
  editable: boolean;
  onEditInline: (item: QuizItemView) => void;
}) {
  const move = (index: number, delta: number) => {
    const next = [...items];
    const [moved] = next.splice(index, 1);
    if (!moved) return;
    next.splice(index + delta, 0, moved);
    onChange(next);
  };
  if (items.length === 0) return <p className="py-6 text-center text-[13px] text-muted">Quiz chưa có câu hỏi.</p>;
  return (
    <ol className="flex flex-col gap-3" aria-label="Câu hỏi của quiz">
      {items.map((draft, index) => {
        const q = draft.item.question;
        const inline = draft.item.inline;
        return (
          <li key={q.id} className="rounded-xl border border-line p-3" data-testid="quiz-item">
            <div className="mb-2 flex flex-wrap items-center gap-2 text-xs text-muted">
              <span className="font-bold text-ink">Câu {index + 1}</span>
              <Pill>{TYPE_LABELS[q.type]}</Pill>
              <span>{inline ? "Câu riêng của quiz" : `Từ ngân hàng · v${q.versionNo}`}</span>
              {q.status === "RETIRED" && <Pill tone="danger">Đã ngưng trong ngân hàng</Pill>}
              <span className="ml-auto flex items-center gap-1">
                <TextInput aria-label={`Điểm câu ${index + 1}`} inputMode="decimal" value={draft.points} disabled={!editable}
                  wrapperClassName="w-20" data-testid={`quiz-item-points-${index}`}
                  onChange={(event) => onChange(items.map((d, i) => (i === index ? { ...d, points: event.target.value } : d)))} />
                điểm
              </span>
            </div>
            <p className="whitespace-pre-wrap text-[13px] text-ink">{q.stem}</p>
            <ul className="mt-2 grid gap-1 text-[13px]">
              {q.options.map((option) => (
                <li key={option.id} className="flex items-center gap-1.5">
                  {q.correctOptionIds.includes(option.id)
                    ? <Icon name="check_circle" size={16} />
                    : <span className="inline-block w-4" aria-hidden="true" />}
                  <span className={q.correctOptionIds.includes(option.id) ? "font-semibold text-success" : "text-body"}>{option.text}</span>
                </li>
              ))}
            </ul>
            {editable && (
              <div className="mt-2 flex flex-wrap gap-1">
                <Button size="sm" variant="ghost" disabled={index === 0} onClick={() => move(index, -1)} aria-label={`Đưa câu ${index + 1} lên`}>Lên</Button>
                <Button size="sm" variant="ghost" disabled={index === items.length - 1} onClick={() => move(index, 1)} aria-label={`Đưa câu ${index + 1} xuống`}>Xuống</Button>
                {inline && <Button size="sm" variant="ghost" onClick={() => onEditInline(draft.item)} data-testid={`quiz-item-edit-${index}`}>Sửa câu</Button>}
                <Button size="sm" variant="ghost" onClick={() => onChange(items.filter((_, i) => i !== index))} data-testid={`quiz-item-remove-${index}`}>Bỏ khỏi quiz</Button>
              </div>
            )}
          </li>
        );
      })}
    </ol>
  );
}


/** Popup soạn câu riêng của quiz (BR-U10-13), dùng McqEditor. */
export function InlineQuestionDialog({ initial, onClose, onSave }: {
  initial: QuizItemView | null;
  onClose: () => void;
  onSave: (content: QuestionContent) => Promise<void>;
}) {
  const [form, setForm] = useState<QuestionForm>(() => (initial ? formOf(initial.question) : emptyForm()));
  const [errors, setErrors] = useState<FormErrors>({});
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const save = async () => {
    const found = validateForm(form);
    setErrors(found);
    if (Object.keys(found).length > 0) return;
    setBusy(true);
    setError(null);
    try {
      await onSave(toContent(form));
    } catch (failure) {
      setError(failure instanceof Error ? failure.message : "Chưa lưu được câu hỏi.");
      setBusy(false);
    }
  };
  return (
    <Dialog open onClose={onClose} dismissible={!busy} size="lg" icon="quiz" title={initial ? "Sửa câu riêng của quiz" : "Thêm câu riêng"}
      footer={<>
        <Button variant="primary" onClick={() => void save()} disabled={busy} data-testid="inline-question-save">Lưu câu</Button>
      </>}>
      <div className="flex flex-col gap-4">
        <McqEditor value={form} onChange={setForm} errors={errors} />
        <QuestionMetaPanel value={form} onChange={setForm} errors={errors} />
        {error && <Alert tone="danger" role="alert">{error}</Alert>}
      </div>
    </Dialog>
  );
}

export type SettingsForm = {
  title: string;
  instructions: string;
  timeLimit: string;
  maxAttempts: string;
  shuffleQuestions: boolean;
  shuffleOptions: boolean;
  showScoreAfterSubmit: boolean;
  showCorrectAnswers: boolean;
  activityMonitoringEnabled: boolean;
  activityNotice: string;
  materialIds: string[];
};

export function settingsFormOf(quiz: QuizDetailsInput): SettingsForm {
  const s = quiz.settings;
  return {
    title: quiz.title,
    instructions: quiz.instructions ?? "",
    timeLimit: s.timeLimitMinutes === null ? "" : String(s.timeLimitMinutes),
    maxAttempts: s.maxAttempts === null ? "" : String(s.maxAttempts),
    shuffleQuestions: s.shuffleQuestions,
    shuffleOptions: s.shuffleOptions,
    showScoreAfterSubmit: s.showScoreAfterSubmit,
    showCorrectAnswers: s.showCorrectAnswers,
    activityMonitoringEnabled: quiz.activityMonitoringEnabled,
    activityNotice: quiz.activityNotice ?? "",
    materialIds: [...quiz.referenceMaterialIds],
  };
}

/** Kiểm cấu hình theo BR-U10-15; trả lỗi theo trường hoặc dữ liệu gửi API. */
export function settingsInput(form: SettingsForm): { errors: Record<string, string>; input?: QuizDetailsInput } {
  const errors: Record<string, string> = {};
  if (!form.title.trim() || form.title.trim().length > 200) errors.title = "Tên quiz từ 1 đến 200 ký tự";
  const time = form.timeLimit.trim() === "" ? null : Number(form.timeLimit);
  if (time !== null && (!Number.isInteger(time) || time < 1 || time > 300)) errors.timeLimit = "Từ 1 đến 300 phút hoặc để trống";
  const attempts = form.maxAttempts.trim() === "" ? null : Number(form.maxAttempts);
  if (attempts !== null && (!Number.isInteger(attempts) || attempts < 1 || attempts > 10)) errors.maxAttempts = "Từ 1 đến 10 hoặc để trống";
  if (form.activityMonitoringEnabled && !form.activityNotice.trim()) errors.activityNotice = "Cần lời báo cho sinh viên khi bật ghi nhận hoạt động";
  if (form.activityNotice.trim().length > 1000) errors.activityNotice = "Lời báo tối đa 1.000 ký tự";
  if (Object.keys(errors).length > 0) return { errors };
  const settings: QuizSettings = {
    shuffleQuestions: form.shuffleQuestions,
    shuffleOptions: form.shuffleOptions,
    timeLimitMinutes: time,
    maxAttempts: attempts,
    showScoreAfterSubmit: form.showScoreAfterSubmit,
    showCorrectAnswers: form.showCorrectAnswers,
  };
  return {
    errors,
    input: {
      title: form.title.trim(),
      instructions: form.instructions.trim() || null,
      settings,
      activityMonitoringEnabled: form.activityMonitoringEnabled,
      activityNotice: form.activityNotice.trim() || null,
      referenceMaterialIds: form.materialIds,
    },
  };
}

/** "Cấu hình" (BR-U10-15); tên và mô tả sửa ở popup Sửa. Ghi nhận hoạt động luôn bật được, dữ liệu giữ cố định 1 ngày. */
export function QuizSettingsPanel({ value, onChange, errors, readOnly }: {
  value: SettingsForm;
  onChange: (value: SettingsForm) => void;
  errors: Record<string, string>;
  readOnly: boolean;
}) {
  const id = useId();
  const set = <K extends keyof SettingsForm>(key: K, v: SettingsForm[K]) => onChange({ ...value, [key]: v });
  return (
    <Section title="Cấu hình" icon="tune">
      <fieldset disabled={readOnly} className="flex flex-col gap-3">
        <Field label="Thời gian làm bài" htmlFor={`${id}-time`} hint="Để trống là không giới hạn" error={errors.timeLimit}>
          <TextInput id={`${id}-time`} inputMode="numeric" placeholder="Không giới hạn" suffix="phút" value={value.timeLimit} invalid={!!errors.timeLimit}
            onChange={(e) => set("timeLimit", e.target.value)} data-testid="quiz-settings-time" />
        </Field>
        <Field label="Số lượt tối đa" htmlFor={`${id}-attempts`} hint="Để trống là không giới hạn" error={errors.maxAttempts}>
          <TextInput id={`${id}-attempts`} inputMode="numeric" placeholder="Không giới hạn" value={value.maxAttempts} invalid={!!errors.maxAttempts}
            onChange={(e) => set("maxAttempts", e.target.value)} data-testid="quiz-settings-attempts" />
        </Field>
        <Switch checked={value.showScoreAfterSubmit} onChange={(v) => set("showScoreAfterSubmit", v)} label="Hiện điểm sau khi nộp" ariaLabel="Hiện điểm sau khi nộp" disabled={readOnly} />
        <Switch checked={value.showCorrectAnswers} onChange={(v) => set("showCorrectAnswers", v)} label="Hiện đáp án sau khi nộp" ariaLabel="Hiện đáp án sau khi nộp" disabled={readOnly} />
        <Switch checked={value.shuffleQuestions} onChange={(v) => set("shuffleQuestions", v)} label="Đảo thứ tự câu" ariaLabel="Đảo thứ tự câu" disabled={readOnly} />
        <Switch checked={value.shuffleOptions} onChange={(v) => set("shuffleOptions", v)} label="Đảo thứ tự lựa chọn" ariaLabel="Đảo thứ tự lựa chọn" disabled={readOnly} />
        <Switch checked={value.activityMonitoringEnabled} onChange={(v) => set("activityMonitoringEnabled", v)} label="Ghi nhận hoạt động" ariaLabel="Ghi nhận hoạt động"
          disabled={readOnly} />
        {value.activityMonitoringEnabled && (
          <Field label="Lời báo cho sinh viên" htmlFor={`${id}-notice`} required error={errors.activityNotice}
            hint="Sinh viên thấy lời báo này trước và trong khi làm bài.">
            <textarea id={`${id}-notice`} rows={2} maxLength={1000} value={value.activityNotice} className={TEXTAREA}
              aria-invalid={!!errors.activityNotice} onChange={(e) => set("activityNotice", e.target.value)}
              data-testid="quiz-settings-activity-notice" />
          </Field>
        )}
      </fieldset>
    </Section>
  );
}

/** Xem trước như sinh viên: câu và lựa chọn, không có đáp án (F9 bước 1). */
export function QuizPreviewDialog({ title, items, onClose }: { title: string; items: QuizItemView[]; onClose: () => void }) {
  return (
    <Dialog open onClose={onClose} size="lg" icon="visibility" title={`Xem trước: ${title}`} footer={<Button onClick={onClose}>Đóng</Button>}>
      <ol className="flex flex-col gap-3 text-[13px]" data-testid="quiz-preview">
        {items.map((item, index) => (
          <li key={item.question.id}>
            <p className="font-semibold">Câu {index + 1} ({formatPoints(item.points)} điểm)</p>
            <p className="whitespace-pre-wrap">{item.question.stem}</p>
            <ul className="mt-1 list-[upper-alpha] pl-6">
              {item.question.options.map((option) => <li key={option.id}>{option.text}</li>)}
            </ul>
          </li>
        ))}
      </ol>
    </Dialog>
  );
}
