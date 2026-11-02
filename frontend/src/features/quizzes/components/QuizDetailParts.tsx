"use client";

import { useId, useState } from "react";
import { Alert, Button, Dialog, Field, Icon, Pill, Section, Switch, TextInput } from "@/shared/ui";
import { formatPoints, TYPE_LABELS } from "../model/labels";
import { McqEditor } from "./McqEditor";
import { QuestionMetaPanel } from "./QuestionSidePanels";
import { emptyForm, formOf, toContent, validateForm, type FormErrors, type QuestionForm } from "../model/questionForm";
import type { LeavePenalty, QuestionContent, QuizDetailsInput, QuizItemView, QuizSettings } from "../model/types";

export type DraftItem = { item: QuizItemView; points: string };

const TEXTAREA = "w-full rounded-lg border border-line-strong px-3 py-2 text-[13px] text-ink focus-visible:outline-focus disabled:bg-shell";

/**
 * "Câu hỏi (n)": thứ tự, nguồn câu, điểm, nội dung và đáp án; sửa điểm, đổi thứ tự, bỏ câu khi quiz còn nháp. `onPropose`
 * (Quiz của lớp): giảng viên đề xuất câu riêng vào ngân hàng của môn (BR-U10-09).
 */
export function QuizItemList({ items, onChange, editable, onEditInline, onPropose }: {
  items: DraftItem[];
  onChange: (items: DraftItem[]) => void;
  editable: boolean;
  onEditInline: (item: QuizItemView) => void;
  onPropose?: (item: QuizItemView) => void;
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
                {inline && onPropose && <Button size="sm" variant="ghost" onClick={() => onPropose(draft.item)} data-testid={`quiz-item-propose-${index}`}>Đề xuất vào ngân hàng</Button>}
                <Button size="sm" variant="ghost" onClick={() => onChange(items.filter((_, i) => i !== index))} data-testid={`quiz-item-remove-${index}`}>Bỏ khỏi quiz</Button>
              </div>
            )}
          </li>
        );
      })}
    </ol>
  );
}


/** Popup soạn câu riêng của quiz (BR-U10-13) hoặc câu đề xuất (BR-U10-09), dùng McqEditor. */
export function InlineQuestionDialog({ initial, onClose, onSave, title }: {
  initial: Pick<QuizItemView, "question"> | null;
  onClose: () => void;
  onSave: (content: QuestionContent) => Promise<void>;
  title?: string;
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
    <Dialog open onClose={onClose} dismissible={!busy} size="lg" icon="quiz" title={title ?? (initial ? "Sửa câu riêng của quiz" : "Thêm câu riêng")}
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
  penaltyEnabled: boolean;
  freeLeaves: string;
  pointsPerLeave: string;
  maxPenalty: string;
  minAwaySeconds: string;
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
    penaltyEnabled: !!s.leavePenalty,
    freeLeaves: String(s.leavePenalty?.freeLeaves ?? 0),
    pointsPerLeave: s.leavePenalty ? String(s.leavePenalty.pointsPerLeave) : "",
    maxPenalty: s.leavePenalty?.maxPenalty === null || s.leavePenalty?.maxPenalty === undefined ? "" : String(s.leavePenalty.maxPenalty),
    minAwaySeconds: String(s.leavePenalty?.minAwaySeconds ?? 5),
  };
}

const POINTS = /^\d+([.,]\d{1,2})?$/;
const decimal = (value: string) => Number(value.trim().replace(",", "."));

/** Gợi ý trừ điểm khi rời trang (BR-U10-15): cần bật ghi nhận hoạt động. */
function penaltyInput(form: SettingsForm, errors: Record<string, string>): LeavePenalty | null {
  if (!form.activityMonitoringEnabled) errors.penalty = "Cần bật ghi nhận hoạt động thì mới gợi ý trừ điểm được";
  const free = Number(form.freeLeaves);
  if (!Number.isInteger(free) || free < 0 || free > 50) errors.freeLeaves = "Từ 0 đến 50 lần";
  if (!POINTS.test(form.pointsPerLeave.trim()) || decimal(form.pointsPerLeave) <= 0) errors.pointsPerLeave = "Lớn hơn 0, tối đa 2 chữ số thập phân";
  const hasMax = form.maxPenalty.trim() !== "";
  if (hasMax && (!POINTS.test(form.maxPenalty.trim()) || decimal(form.maxPenalty) <= 0)) errors.maxPenalty = "Lớn hơn 0 hoặc để trống";
  const minAway = Number(form.minAwaySeconds);
  if (!Number.isInteger(minAway) || minAway < 0 || minAway > 600) errors.minAwaySeconds = "Từ 0 đến 600 giây";
  return { freeLeaves: free, pointsPerLeave: decimal(form.pointsPerLeave), maxPenalty: hasMax ? decimal(form.maxPenalty) : null, minAwaySeconds: minAway };
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
  const penalty = form.penaltyEnabled ? penaltyInput(form, errors) : null;
  if (Object.keys(errors).length > 0) return { errors };
  const settings: QuizSettings = {
    shuffleQuestions: form.shuffleQuestions,
    shuffleOptions: form.shuffleOptions,
    timeLimitMinutes: time,
    maxAttempts: attempts,
    showScoreAfterSubmit: form.showScoreAfterSubmit,
    showCorrectAnswers: form.showCorrectAnswers,
    leavePenalty: penalty,
  };
  return {
    errors,
    input: {
      title: form.title.trim(),
      instructions: form.instructions.trim() || null,
      settings,
      activityMonitoringEnabled: form.activityMonitoringEnabled,
      activityNotice: form.activityNotice.trim() || null,
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
        {value.activityMonitoringEnabled && (
          <Switch checked={value.penaltyEnabled} onChange={(v) => set("penaltyEnabled", v)} label="Gợi ý trừ điểm khi rời trang" ariaLabel="Gợi ý trừ điểm khi rời trang" disabled={readOnly} />
        )}
        {errors.penalty && <p className="text-xs text-danger" role="alert">{errors.penalty}</p>}
        {value.activityMonitoringEnabled && value.penaltyEnabled && (
          <div className="grid grid-cols-2 gap-2" data-testid="quiz-settings-penalty">
            <Field label="Số lần được miễn" htmlFor={`${id}-free`} error={errors.freeLeaves}>
              <TextInput id={`${id}-free`} inputMode="numeric" value={value.freeLeaves} invalid={!!errors.freeLeaves} onChange={(e) => set("freeLeaves", e.target.value)} data-testid="quiz-settings-free-leaves" />
            </Field>
            <Field label="Điểm trừ mỗi lần" htmlFor={`${id}-per`} error={errors.pointsPerLeave}>
              <TextInput id={`${id}-per`} inputMode="decimal" value={value.pointsPerLeave} invalid={!!errors.pointsPerLeave} onChange={(e) => set("pointsPerLeave", e.target.value)} data-testid="quiz-settings-points-per-leave" />
            </Field>
            <Field label="Trừ tối đa" htmlFor={`${id}-max`} hint="Trống là không giới hạn" error={errors.maxPenalty}>
              <TextInput id={`${id}-max`} inputMode="decimal" value={value.maxPenalty} invalid={!!errors.maxPenalty} onChange={(e) => set("maxPenalty", e.target.value)} data-testid="quiz-settings-max-penalty" />
            </Field>
            <Field label="Chỉ tính khi rời từ" htmlFor={`${id}-min`} error={errors.minAwaySeconds}>
              <TextInput id={`${id}-min`} inputMode="numeric" suffix="giây" value={value.minAwaySeconds} invalid={!!errors.minAwaySeconds} onChange={(e) => set("minAwaySeconds", e.target.value)} data-testid="quiz-settings-min-away" />
            </Field>
            <p className="col-span-2 text-xs text-muted">Hệ thống không tự trừ điểm: chỉ tính mức gợi ý, giảng viên quyết định ở từng lượt làm. Tín hiệu do trình duyệt tự báo nên có thể sai hoặc bị chặn.</p>
          </div>
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
