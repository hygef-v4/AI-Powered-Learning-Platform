// Trạng thái form soạn câu trắc nghiệm và kiểm tra ngay trên form theo BR-U10-02, 03.
// Server vẫn kiểm lại; form chỉ giúp người soạn thấy lỗi sớm.

import type { Difficulty, QuestionContent, QuestionOption, QuestionType, QuestionView } from "./types";

export const MIN_OPTIONS = 2;
export const MAX_OPTIONS = 6;
export const MAX_TEXT = 20_000;
export const MAX_TAGS = 10;

export type QuestionForm = {
  type: QuestionType;
  stem: string;
  options: QuestionOption[];
  correctOptionIds: string[];
  explanation: string;
  defaultPoints: string;
  difficulty: Difficulty;
  tagsText: string;
};

export type FormErrors = Partial<Record<"stem" | "options" | "correctOptionIds" | "defaultPoints" | "tags" | "explanation", string>>;

export function emptyForm(): QuestionForm {
  return {
    type: "MCQ_SINGLE",
    stem: "",
    options: [{ id: "1", text: "" }, { id: "2", text: "" }],
    correctOptionIds: [],
    explanation: "",
    defaultPoints: "1",
    difficulty: "MEDIUM",
    tagsText: "",
  };
}

export function formOf(question: QuestionView): QuestionForm {
  return {
    type: question.type,
    stem: question.stem,
    options: question.options.map((option) => ({ ...option })),
    correctOptionIds: [...question.correctOptionIds],
    explanation: question.explanation ?? "",
    defaultPoints: String(question.defaultPoints),
    difficulty: question.difficulty,
    tagsText: question.tags.join(", "),
  };
}

/** Mã lựa chọn mới: lớn hơn mọi mã số đang có, để mã không đổi khi xóa lựa chọn ở giữa. */
export function nextOptionId(options: QuestionOption[]): string {
  const max = options.reduce((acc, option) => Math.max(acc, Number(option.id) || 0), 0);
  return String(max + 1);
}

export function parseTags(text: string): string[] {
  const seen = new Set<string>();
  return text
    .split(/[,;]/)
    .map((tag) => tag.trim())
    .filter((tag) => tag && !seen.has(tag.toLowerCase()) && seen.add(tag.toLowerCase()));
}

/** Đổi loại câu sang một đáp án thì chỉ giữ đáp án đúng đầu tiên. */
export function withType(form: QuestionForm, type: QuestionType): QuestionForm {
  return { ...form, type, correctOptionIds: type === "MCQ_SINGLE" ? form.correctOptionIds.slice(0, 1) : form.correctOptionIds };
}

export function toggleCorrect(form: QuestionForm, optionId: string): QuestionForm {
  if (form.type === "MCQ_SINGLE") return { ...form, correctOptionIds: [optionId] };
  const selected = form.correctOptionIds.includes(optionId);
  return {
    ...form,
    correctOptionIds: selected ? form.correctOptionIds.filter((id) => id !== optionId) : [...form.correctOptionIds, optionId],
  };
}

export function validateForm(form: QuestionForm): FormErrors {
  const errors: FormErrors = {};
  if (!form.stem.trim()) errors.stem = "Nội dung câu hỏi không được để trống";
  else if ([...form.stem].length > MAX_TEXT) errors.stem = "Nội dung câu hỏi tối đa 20.000 ký tự";
  if ([...form.explanation].length > MAX_TEXT) errors.explanation = "Giải thích tối đa 20.000 ký tự";

  const texts = form.options.map((option) => option.text.trim());
  if (form.options.length < MIN_OPTIONS || form.options.length > MAX_OPTIONS) errors.options = "Cần 2–6 lựa chọn";
  else if (texts.some((text) => !text)) errors.options = "Lựa chọn không được để trống";
  else if (new Set(texts.map((text) => text.toLowerCase())).size !== texts.length) errors.options = "Các lựa chọn không được trùng nội dung";

  const ids = new Set(form.options.map((option) => option.id));
  const correct = form.correctOptionIds.filter((id) => ids.has(id));
  if (form.type === "MCQ_SINGLE" && correct.length !== 1) errors.correctOptionIds = "Chọn đúng 1 đáp án đúng";
  if (form.type === "MCQ_MULTI" && correct.length < 1) errors.correctOptionIds = "Chọn ít nhất 1 đáp án đúng";

  const points = Number(form.defaultPoints.replace(",", "."));
  if (!form.defaultPoints.trim() || !Number.isFinite(points) || points <= 0 || points > 9999.99
    || !/^\d+([.,]\d{1,2})?$/.test(form.defaultPoints.trim())) {
    errors.defaultPoints = "Điểm phải lớn hơn 0, tối đa 2 chữ số thập phân";
  }
  if (parseTags(form.tagsText).length > MAX_TAGS) errors.tags = "Tối đa 10 thẻ";
  return errors;
}

export function toContent(form: QuestionForm): QuestionContent {
  const ids = new Set(form.options.map((option) => option.id));
  return {
    type: form.type,
    stem: form.stem.trim(),
    options: form.options.map((option) => ({ id: option.id, text: option.text.trim() })),
    correctOptionIds: form.correctOptionIds.filter((id) => ids.has(id)),
    explanation: form.explanation.trim() || null,
    defaultPoints: Number(form.defaultPoints.replace(",", ".")),
    difficulty: form.difficulty,
    tags: parseTags(form.tagsText),
  };
}
