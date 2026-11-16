"use client";

import { type ReactNode, useEffect, useId, useState } from "react";
import { Alert, Button, Dialog, Field, Icon, Pill, Section, Switch, Textarea, TextInput } from "@/shared/ui";
import { formatPoints, TYPE_LABELS } from "../model/labels";
import { McqEditor } from "./McqEditor";
import { QuestionMetaPanel } from "./QuestionSidePanels";
import { emptyForm, formOf, toContent, validateForm, type FormErrors, type QuestionForm } from "../model/questionForm";
import type { LeavePenalty, QuestionContent, QuizDetailsInput, QuizItemView, QuizSettings } from "../model/types";
import { msg } from "@/i18n/msg";

export type DraftItem = { item: QuizItemView; points: string };


/**
 * "Câu hỏi (n)": thứ tự, nguồn câu, điểm, nội dung và đáp án; sửa điểm, đổi thứ tự, bỏ câu khi quiz còn nháp. `onPropose`
 * (Quiz của lớp): giảng viên đề xuất câu riêng vào ngân hàng của môn (BR-U10-09).
 */
export function QuizItemList({ items, onChange, editable, onEditInline, onPropose, editingId, editor }: {
  items: DraftItem[];
  onChange: (items: DraftItem[]) => void;
  editable: boolean;
  onEditInline: (item: QuizItemView) => void;
  onPropose?: (item: QuizItemView) => void;
  editingId?: string;
  editor?: ReactNode;
}) {
  const move = (index: number, delta: number) => {
    const next = [...items];
    const [moved] = next.splice(index, 1);
    if (!moved) return;
    next.splice(index + delta, 0, moved);
    onChange(next);
  };
  if (items.length === 0) return <p className="py-6 text-center text-[13px] text-muted">{msg("quizzes.components.QuizDetailParts.quizChuaCoCauHoi")}</p>;
  return (
    <ol className="flex flex-col gap-3" aria-label={msg("quizzes.components.QuizDetailParts.cauHoiCuaQuiz")}>
      {items.map((draft, index) => {
        const q = draft.item.question;
        const inline = draft.item.inline;
        if (q.id === editingId) return <li key={q.id} className="rounded-xl border border-line p-3" data-testid="quiz-item">
          <p className="mb-3 text-sm font-semibold text-ink">{msg("quizzes.components.QuizDetailParts.cau")}{index + 1}</p>{editor}
        </li>;
        return (
          <li key={q.id} className="rounded-xl border border-line p-3" data-testid="quiz-item">
            <div className="mb-2 flex flex-wrap items-center gap-2 text-xs text-muted">
              <span className="font-bold text-ink">{msg("quizzes.components.QuizDetailParts.cau")}{index + 1}</span>
              <Pill>{TYPE_LABELS[q.type]}</Pill>
              <span>{inline ? msg("quizzes.components.QuizDetailParts.cauRiengCuaQuiz") : msg("quizzes.components.QuizDetailParts.tuNganHangV", { versionNo: String(q.versionNo) })}</span>
              {q.status === "RETIRED" && <Pill tone="danger">{msg("quizzes.components.QuizDetailParts.daNgungTrongNganHang")}</Pill>}
              <span className="ml-auto flex items-center gap-1">
                <TextInput aria-label={msg("quizzes.components.QuizDetailParts.diemCau", { value: String(index + 1) })} inputMode="decimal" value={draft.points} disabled={!editable}
                  wrapperClassName="w-20" data-testid={`quiz-item-points-${index}`}
                  onChange={(event) => onChange(items.map((d, i) => (i === index ? { ...d, points: event.target.value } : d)))} />
                {msg("quizzes.components.QuizDetailParts.diem")}</span>
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
                <Button size="sm" variant="ghost" disabled={index === 0} onClick={() => move(index, -1)} aria-label={msg("quizzes.components.QuizDetailParts.duaCauLen", { value: String(index + 1) })}>{msg("quizzes.components.QuizDetailParts.len")}</Button>
                <Button size="sm" variant="ghost" disabled={index === items.length - 1} onClick={() => move(index, 1)} aria-label={msg("quizzes.components.QuizDetailParts.duaCauXuong", { value: String(index + 1) })}>{msg("quizzes.components.QuizDetailParts.xuong")}</Button>
                {inline && <Button size="sm" variant="ghost" onClick={() => onEditInline(draft.item)} data-testid={`quiz-item-edit-${index}`}>{msg("quizzes.components.QuizDetailParts.suaCau")}</Button>}
                {inline && onPropose && <Button size="sm" variant="ghost" onClick={() => onPropose(draft.item)} data-testid={`quiz-item-propose-${index}`}>{msg("quizzes.components.QuizDetailParts.deXuatVaoNganHang")}</Button>}
                <Button size="sm" variant="ghost" onClick={() => onChange(items.filter((_, i) => i !== index))} data-testid={`quiz-item-remove-${index}`}>{msg("quizzes.components.QuizDetailParts.boKhoiQuiz")}</Button>
              </div>
            )}
          </li>
        );
      })}
    </ol>
  );
}


/** Popup soạn câu riêng của quiz (BR-U10-13) hoặc câu đề xuất (BR-U10-09), dùng McqEditor. */
export function InlineQuestionDialog({ initial, onClose, onSave, title, inline = false, onDirtyChange }: {
  initial: Pick<QuizItemView, "question"> | null;
  onClose: () => void;
  onSave: (content: QuestionContent) => Promise<void>;
  title?: string;
  inline?: boolean;
  onDirtyChange?: (dirty: boolean) => void;
}) {
  const [form, setForm] = useState<QuestionForm>(() => (initial ? formOf(initial.question) : emptyForm()));
  const [errors, setErrors] = useState<FormErrors>({});
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  useEffect(() => {
    onDirtyChange?.(JSON.stringify(form) !== JSON.stringify(initial ? formOf(initial.question) : emptyForm()));
  }, [form, initial, onDirtyChange]);
  useEffect(() => () => onDirtyChange?.(false), [onDirtyChange]);
  const save = async () => {
    const found = validateForm(form);
    setErrors(found);
    if (Object.keys(found).length > 0) return;
    setBusy(true);
    setError(null);
    try {
      await onSave(toContent(form));
    } catch (failure) {
      setError(failure instanceof Error ? failure.message : msg("quizzes.components.QuizDetailParts.chuaLuuDuocCauHoi"));
      setBusy(false);
    }
  };
  const footer = <>
    {inline && <Button onClick={onClose} disabled={busy} aria-label={msg("quizzes.components.QuizDetailParts.huySoanCau")}>{msg("quizzes.components.QuizDetailParts.huy")}</Button>}
    <Button variant="primary" onClick={() => void save()} disabled={busy} data-testid="inline-question-save">{msg("quizzes.components.QuizDetailParts.luuCau")}</Button>
  </>;
  const body = (
      <div className="flex flex-col gap-4">
        <McqEditor value={form} onChange={setForm} errors={errors} readOnly={busy} />
        <QuestionMetaPanel value={form} onChange={setForm} errors={errors} />
        {error && <Alert tone="danger" role="alert">{error}</Alert>}
      </div>
  );
  if (inline) return <div className="flex min-w-0 flex-col gap-4" role="region" aria-label={initial ? msg("quizzes.components.QuizDetailParts.suaCauHoi") : msg("quizzes.components.QuizDetailParts.soanCauMoi")}>
    {body}<div className="flex justify-end gap-2 border-t border-line pt-3">{footer}</div>
  </div>;
  return <Dialog open onClose={onClose} dismissible={!busy} size="lg" icon="quiz"
    title={title ?? (initial ? msg("quizzes.components.QuizDetailParts.suaCauRiengCuaQuiz") : msg("quizzes.components.QuizDetailParts.themCauRieng"))} footer={footer}>{body}</Dialog>;
}

export type SettingsForm = {
  title: string;
  instructions: string;
  instructionsDocument?: QuizDetailsInput["instructionsDocument"];
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
    instructionsDocument: quiz.instructionsDocument,
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
  if (!form.activityMonitoringEnabled) errors.penalty = msg("quizzes.components.QuizDetailParts.canBatGhiNhanHoatDong");
  const free = Number(form.freeLeaves);
  if (!Number.isInteger(free) || free < 0 || free > 50) errors.freeLeaves = msg("quizzes.components.QuizDetailParts.tu0Den50Lan");
  if (!POINTS.test(form.pointsPerLeave.trim()) || decimal(form.pointsPerLeave) <= 0) errors.pointsPerLeave = msg("quizzes.components.QuizDetailParts.lonHon0ToiDa2");
  const hasMax = form.maxPenalty.trim() !== "";
  if (hasMax && (!POINTS.test(form.maxPenalty.trim()) || decimal(form.maxPenalty) <= 0)) errors.maxPenalty = msg("quizzes.components.QuizDetailParts.lonHon0HoacDeTrong");
  const minAway = Number(form.minAwaySeconds);
  if (!Number.isInteger(minAway) || minAway < 0 || minAway > 600) errors.minAwaySeconds = msg("quizzes.components.QuizDetailParts.tu0Den600Giay");
  return { freeLeaves: free, pointsPerLeave: decimal(form.pointsPerLeave), maxPenalty: hasMax ? decimal(form.maxPenalty) : null, minAwaySeconds: minAway };
}

/** Kiểm cấu hình theo BR-U10-15; trả lỗi theo trường hoặc dữ liệu gửi API. */
export function settingsInput(form: SettingsForm): { errors: Record<string, string>; input?: QuizDetailsInput } {
  const errors: Record<string, string> = {};
  if (!form.title.trim() || form.title.trim().length > 200) errors.title = msg("quizzes.components.QuizDetailParts.tenQuizTu1Den200");
  const time = form.timeLimit.trim() === "" ? null : Number(form.timeLimit);
  if (time !== null && (!Number.isInteger(time) || time < 1 || time > 300)) errors.timeLimit = msg("quizzes.components.QuizDetailParts.tu1Den300PhutHoac");
  const attempts = form.maxAttempts.trim() === "" ? null : Number(form.maxAttempts);
  if (attempts !== null && (!Number.isInteger(attempts) || attempts < 1 || attempts > 10)) errors.maxAttempts = msg("quizzes.components.QuizDetailParts.tu1Den10HoacDe");
  if (form.activityMonitoringEnabled && !form.activityNotice.trim()) errors.activityNotice = msg("quizzes.components.QuizDetailParts.canLoiBaoChoSinhVien");
  if (form.activityNotice.trim().length > 1000) errors.activityNotice = msg("quizzes.components.QuizDetailParts.loiBaoToiDa1000");
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
      instructionsDocument: form.instructionsDocument,
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
    <Section title={msg("quizzes.components.QuizDetailParts.cauHinh")} icon="tune">
      <fieldset disabled={readOnly} className="flex flex-col gap-3">
        <Field label={msg("quizzes.components.QuizDetailParts.thoiGianLamBai")} htmlFor={`${id}-time`} hint={msg("quizzes.components.QuizDetailParts.deTrongLaKhongGioiHan")} error={errors.timeLimit}>
          <TextInput id={`${id}-time`} inputMode="numeric" placeholder={msg("quizzes.components.QuizDetailParts.khongGioiHan")} suffix={msg("quizzes.components.QuizDetailParts.phut")} value={value.timeLimit} invalid={!!errors.timeLimit}
            onChange={(e) => set("timeLimit", e.target.value)} data-testid="quiz-settings-time" />
        </Field>
        <Field label={msg("quizzes.components.QuizDetailParts.soLuotToiDa")} htmlFor={`${id}-attempts`} hint={msg("quizzes.components.QuizDetailParts.deTrongLaKhongGioiHan")} error={errors.maxAttempts}>
          <TextInput id={`${id}-attempts`} inputMode="numeric" placeholder={msg("quizzes.components.QuizDetailParts.khongGioiHan")} value={value.maxAttempts} invalid={!!errors.maxAttempts}
            onChange={(e) => set("maxAttempts", e.target.value)} data-testid="quiz-settings-attempts" />
        </Field>
        <Switch checked={value.showScoreAfterSubmit} onChange={(v) => set("showScoreAfterSubmit", v)} label={msg("quizzes.components.QuizDetailParts.hienDiemSauKhiNop")} ariaLabel={msg("quizzes.components.QuizDetailParts.hienDiemSauKhiNop")} disabled={readOnly} />
        <Switch checked={value.showCorrectAnswers} onChange={(v) => set("showCorrectAnswers", v)} label={msg("quizzes.components.QuizDetailParts.hienDapAnSauKhiNop")} ariaLabel={msg("quizzes.components.QuizDetailParts.hienDapAnSauKhiNop")} disabled={readOnly} />
        <Switch checked={value.shuffleQuestions} onChange={(v) => set("shuffleQuestions", v)} label={msg("quizzes.components.QuizDetailParts.daoThuTuCau")} ariaLabel={msg("quizzes.components.QuizDetailParts.daoThuTuCau")} disabled={readOnly} />
        <Switch checked={value.shuffleOptions} onChange={(v) => set("shuffleOptions", v)} label={msg("quizzes.components.QuizDetailParts.daoThuTuLuaChon")} ariaLabel={msg("quizzes.components.QuizDetailParts.daoThuTuLuaChon")} disabled={readOnly} />
        <Switch checked={value.activityMonitoringEnabled} onChange={(v) => set("activityMonitoringEnabled", v)} label={msg("quizzes.components.QuizDetailParts.ghiNhanHoatDong")} ariaLabel={msg("quizzes.components.QuizDetailParts.ghiNhanHoatDong")}
          disabled={readOnly} />
        {value.activityMonitoringEnabled && (
          <Field label={msg("quizzes.components.QuizDetailParts.loiBaoChoSinhVien")} htmlFor={`${id}-notice`} required error={errors.activityNotice}
            hint={msg("quizzes.components.QuizDetailParts.sinhVienThayLoiBaoNay")}>
            <Textarea id={`${id}-notice`} rows={2} maxLength={1000} value={value.activityNotice}
              invalid={!!errors.activityNotice} onChange={(e) => set("activityNotice", e.target.value)}
              data-testid="quiz-settings-activity-notice" />
          </Field>
        )}
        {value.activityMonitoringEnabled && (
          <Switch checked={value.penaltyEnabled} onChange={(v) => set("penaltyEnabled", v)} label={msg("quizzes.components.QuizDetailParts.goiYTruDiemKhiRoi")} ariaLabel={msg("quizzes.components.QuizDetailParts.goiYTruDiemKhiRoi")} disabled={readOnly} />
        )}
        {errors.penalty && <p className="text-xs text-danger" role="alert">{errors.penalty}</p>}
        {value.activityMonitoringEnabled && value.penaltyEnabled && (
          <div className="grid grid-cols-2 gap-2" data-testid="quiz-settings-penalty">
            <Field label={msg("quizzes.components.QuizDetailParts.soLanDuocMien")} htmlFor={`${id}-free`} error={errors.freeLeaves}>
              <TextInput id={`${id}-free`} inputMode="numeric" value={value.freeLeaves} invalid={!!errors.freeLeaves} onChange={(e) => set("freeLeaves", e.target.value)} data-testid="quiz-settings-free-leaves" />
            </Field>
            <Field label={msg("quizzes.components.QuizDetailParts.diemTruMoiLan")} htmlFor={`${id}-per`} error={errors.pointsPerLeave}>
              <TextInput id={`${id}-per`} inputMode="decimal" value={value.pointsPerLeave} invalid={!!errors.pointsPerLeave} onChange={(e) => set("pointsPerLeave", e.target.value)} data-testid="quiz-settings-points-per-leave" />
            </Field>
            <Field label={msg("quizzes.components.QuizDetailParts.truToiDa")} htmlFor={`${id}-max`} hint={msg("quizzes.components.QuizDetailParts.trongLaKhongGioiHan")} error={errors.maxPenalty}>
              <TextInput id={`${id}-max`} inputMode="decimal" value={value.maxPenalty} invalid={!!errors.maxPenalty} onChange={(e) => set("maxPenalty", e.target.value)} data-testid="quiz-settings-max-penalty" />
            </Field>
            <Field label={msg("quizzes.components.QuizDetailParts.chiTinhKhiRoiTu")} htmlFor={`${id}-min`} error={errors.minAwaySeconds}>
              <TextInput id={`${id}-min`} inputMode="numeric" suffix={msg("quizzes.components.QuizDetailParts.giay")} value={value.minAwaySeconds} invalid={!!errors.minAwaySeconds} onChange={(e) => set("minAwaySeconds", e.target.value)} data-testid="quiz-settings-min-away" />
            </Field>
            <p className="col-span-2 text-xs text-muted">{msg("quizzes.components.QuizDetailParts.heThongKhongTuTruDiem")}</p>
          </div>
        )}
      </fieldset>
    </Section>
  );
}

/** Xem trước như sinh viên: câu và lựa chọn, không có đáp án (F9 bước 1). */
export function QuizPreviewDialog({ title, items, onClose }: { title: string; items: QuizItemView[]; onClose: () => void }) {
  return (
    <Dialog open onClose={onClose} size="lg" icon="visibility" title={msg("quizzes.components.QuizDetailParts.xemTruoc", { title: String(title) })}>
      <ol className="flex flex-col gap-3 text-[13px]" data-testid="quiz-preview">
        {items.map((item, index) => (
          <li key={item.question.id}>
            <p className="font-semibold">{msg("quizzes.components.QuizDetailParts.cau")}{index + 1} ({formatPoints(item.points)} {msg("quizzes.components.QuizDetailParts.diem2")}</p>
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
