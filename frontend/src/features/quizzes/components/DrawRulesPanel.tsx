"use client";

import { useEffect, useId, useMemo, useState } from "react";
import { Alert, Button, Dialog, Pill, Section, Select, TextInput } from "@/shared/ui";
import { getBankTags, previewPaper, saveDrawRules } from "../api";
import { DIFFICULTY_LABELS, formatPoints } from "../model/labels";
import type { BankTag, Difficulty, PaperPreview, QuizDetail, QuizPaperMode } from "../model/types";
import { msg } from "@/i18n/msg";

type Row = { key: number; tag: string; difficulty: Difficulty | ""; questionCount: string; pointsEach: string };

const POINTS = /^\d+([.,]\d{1,2})?$/;
const parseDecimal = (value: string) => Number(value.trim().replace(",", "."));
const isValidRow = (r: Row) => r.tag.trim() !== "" && Number.isInteger(Number(r.questionCount)) && Number(r.questionCount) >= 1
  && POINTS.test(r.pointsEach.trim()) && parseDecimal(r.pointsEach) > 0;
const buildRows = (detail: QuizDetail): Row[] => detail.drawRules.map((r) => ({ key: r.position, tag: r.tag, difficulty: r.difficulty ?? "",
  questionCount: String(r.questionCount), pointsEach: String(r.pointsEach) }));

/**
 * "Cách lập đề" (BR-U10-17): đề cố định, hoặc mỗi lượt làm sinh một đề mới theo quy tắc "thẻ nào lấy bao nhiêu câu" từ
 * câu đang dùng của ngân hàng môn. Lưu riêng với danh sách câu; phát hành chặn nếu kho không đủ câu, còn cảnh báo ở từng
 * quy tắc chỉ là ước tính vì một câu nhiều thẻ có thể được hai quy tắc cùng cần. Trang cha đặt `key` theo version của Quiz
 * để nhận lại dữ liệu mới sau khi lưu.
 */
export function DrawRulesPanel({ detail, editable, disabled, onSaved, onError }: {
  detail: QuizDetail;
  editable: boolean;
  disabled: boolean;
  onSaved: (next: QuizDetail) => void;
  onError: (failure: unknown) => void;
}) {
  const id = useId();
  const [mode, setMode] = useState<QuizPaperMode>(detail.quiz.paperMode);
  const [rows, setRows] = useState<Row[]>(() => buildRows(detail));
  const [tags, setTags] = useState<BankTag[] | null>(null);
  const [busy, setBusy] = useState(false);
  const [preview, setPreview] = useState<PaperPreview | null>(null);
  const [nextKey, setNextKey] = useState(1000);

  const quizId = detail.quiz.id;
  const saved = useMemo(() => JSON.stringify([detail.quiz.paperMode, buildRows(detail).map(({ tag, difficulty, questionCount, pointsEach }) => [tag, difficulty, questionCount, pointsEach])]), [detail]);
  const current = JSON.stringify([mode, rows.map(({ tag, difficulty, questionCount, pointsEach }) => [tag, difficulty, questionCount, pointsEach])]);
  const dirty = saved !== current;

  useEffect(() => {
    if (!editable || mode !== "GENERATED") return;
    const controller = new AbortController();
    getBankTags(quizId, controller.signal).then(setTags).catch(() => undefined);
    return () => controller.abort();
  }, [editable, mode, quizId]);

  // Số câu trong kho khớp thẻ và độ khó của quy tắc; null khi chưa tải được danh sách thẻ.
  const countAvailable = (row: Row) => {
    if (!tags) return null;
    const found = tags.find((t) => t.tag.toLowerCase() === row.tag.trim().toLowerCase());
    if (!found) return 0;
    return row.difficulty ? found.byDifficulty[row.difficulty] ?? 0 : found.activeCount;
  };
  const updateRow = (key: number, patch: Partial<Row>) => setRows((list) => list.map((r) => (r.key === key ? { ...r, ...patch } : r)));
  const total = rows.reduce((sum, r) => sum + (Number(r.questionCount) || 0), 0);
  const totalPoints = rows.reduce((sum, r) => sum + (Number(r.questionCount) || 0) * (parseDecimal(r.pointsEach) || 0), 0);
  const invalid = mode === "GENERATED" && (rows.length === 0 || rows.some((r) => !isValidRow(r)));

  const saveRules = async () => {
    setBusy(true);
    try {
      onSaved(await saveDrawRules(quizId, mode, rows.map((r) => ({ tag: r.tag.trim(), difficulty: r.difficulty || null,
        questionCount: Number(r.questionCount), pointsEach: parseDecimal(r.pointsEach) })), detail.quiz.version));
    } catch (failure) {
      onError(failure);
    } finally {
      setBusy(false);
    }
  };

  const openPreview = async () => {
    setBusy(true);
    try {
      setPreview(await previewPaper(quizId));
    } catch (failure) {
      onError(failure);
    } finally {
      setBusy(false);
    }
  };

  return (
    <Section title={msg("quizzes.components.DrawRulesPanel.howTheQuizIsBuilt")} icon="tune" actions={editable && dirty
      ? <Button size="sm" variant="primary" disabled={busy || disabled || invalid} onClick={() => void saveRules()} data-testid="draw-rules-save">{msg("quizzes.components.DrawRulesPanel.saveBuildMethod")}</Button>
      : undefined}>
      <fieldset disabled={!editable || busy} className="flex flex-col gap-3" data-testid="draw-rules">
        <Select aria-label={msg("quizzes.components.DrawRulesPanel.howTheQuizIsBuilt")} value={mode} onChange={(event) => setMode(event.target.value as QuizPaperMode)} data-testid="draw-rules-mode">
          <option value="FIXED">{msg("quizzes.components.DrawRulesPanel.fixedQuizEveryAttemptHas")}</option>
          <option value="GENERATED">{msg("quizzes.components.DrawRulesPanel.generateANewQuizFor")}</option>
        </Select>
        {mode === "GENERATED" && <>
          <datalist id={`${id}-tags`}>{tags?.map((t) => <option key={t.tag} value={t.tag}>{t.activeCount} {msg("quizzes.components.DrawRulesPanel.questions")}</option>)}</datalist>
          <ul className="flex flex-col gap-2">
            {rows.map((row, index) => {
              const hasTag = row.tag.trim() !== "";
              const count = countAvailable(row);
              const level = row.difficulty ? msg("quizzes.components.DrawRulesPanel.atLevel", { value: String(DIFFICULTY_LABELS[row.difficulty]) }) : "";
              const requested = Number(row.questionCount);
              const short = hasTag && count !== null && Number.isInteger(requested) && requested > count;
              return (
                <li key={row.key} className="flex flex-col gap-1 rounded-lg border border-line p-2" data-testid="draw-rule">
                  <TextInput aria-label={msg("quizzes.components.DrawRulesPanel.tagsOfRule", { value: String(index + 1) })} placeholder={msg("quizzes.components.DrawRulesPanel.tagEGChapter1")} list={`${id}-tags`} value={row.tag}
                    onChange={(event) => updateRow(row.key, { tag: event.target.value })} data-testid={`draw-rule-tag-${index}`} />
                  <div className="grid grid-cols-3 gap-1">
                    <Select aria-label={msg("quizzes.components.DrawRulesPanel.difficultyOfRule", { value: String(index + 1) })} value={row.difficulty} onChange={(event) => updateRow(row.key, { difficulty: event.target.value as Difficulty | "" })}>
                      <option value="">{msg("quizzes.components.DrawRulesPanel.allDifficulties")}</option>
                      {(Object.keys(DIFFICULTY_LABELS) as Difficulty[]).map((d) => <option key={d} value={d}>{DIFFICULTY_LABELS[d]}</option>)}
                    </Select>
                    <TextInput aria-label={msg("quizzes.components.DrawRulesPanel.questionsOfRule", { value: String(index + 1) })} inputMode="numeric" suffix={msg("quizzes.components.DrawRulesPanel.questions2")} value={row.questionCount}
                      onChange={(event) => updateRow(row.key, { questionCount: event.target.value })} data-testid={`draw-rule-count-${index}`} />
                    <TextInput aria-label={msg("quizzes.components.DrawRulesPanel.pointsPerQuestionOfRule", { value: String(index + 1) })} inputMode="decimal" suffix={msg("quizzes.components.DrawRulesPanel.points")} value={row.pointsEach}
                      onChange={(event) => updateRow(row.key, { pointsEach: event.target.value })} data-testid={`draw-rule-points-${index}`} />
                  </div>
                  {short && <p role="status" className="text-xs font-medium text-warning" data-testid={`draw-rule-shortage-${index}`}>{msg("quizzes.components.DrawRulesPanel.needs")}{requested} {msg("quizzes.components.DrawRulesPanel.questionsTheBankOnlyHas")}{count} {msg("quizzes.components.DrawRulesPanel.questionsCannotPublishYet")}</p>}
                  <div className="flex items-center justify-between gap-2 text-xs text-muted">
                    <span>{!hasTag ? msg("quizzes.components.DrawRulesPanel.chooseTagsToSeeThe") : count === null ? "" : count === 0 ? msg("quizzes.components.DrawRulesPanel.theBankHasNoQuestions", { level: String(level) }) : msg("quizzes.components.DrawRulesPanel.theBankHasActiveQuestions", { count: String(count), level: String(level) })}</span>
                    {editable && <Button size="sm" variant="ghost" onClick={() => setRows((list) => list.filter((r) => r.key !== row.key))} aria-label={msg("quizzes.components.DrawRulesPanel.removeRule", { value: String(index + 1) })}>{msg("quizzes.components.DrawRulesPanel.remove")}</Button>}
                  </div>
                </li>
              );
            })}
          </ul>
          {editable && (
            <Button size="sm" icon="add" onClick={() => { setRows((list) => [...list, { key: nextKey, tag: "", difficulty: "", questionCount: "1", pointsEach: "1" }]); setNextKey((k) => k + 1); }}
              data-testid="draw-rule-add">{msg("quizzes.components.DrawRulesPanel.addRule")}</Button>
          )}
          <p className="text-[13px] text-body" data-testid="draw-rules-total">{msg("quizzes.components.DrawRulesPanel.eachAttempt")}{total} {msg("quizzes.components.DrawRulesPanel.questions3")}{formatPoints(totalPoints)} {msg("quizzes.components.DrawRulesPanel.points2")}</p>
          <p className="text-xs text-muted">{msg("quizzes.components.DrawRulesPanel.theQuestionListBelowIs")}</p>
          {!dirty && rows.length > 0 && (
            <Button size="sm" icon="visibility" onClick={() => void openPreview()} data-testid="draw-rules-preview">{msg("quizzes.components.DrawRulesPanel.previewASampleQuiz")}</Button>
          )}
        </>}
      </fieldset>
      {preview && (
        <Dialog open size="lg" onClose={() => setPreview(null)} icon="visibility" title={msg("quizzes.components.DrawRulesPanel.sampleQuiz")}>
          <div className="flex flex-col gap-3 text-[13px]" data-testid="paper-preview">
            {preview.shortages.length > 0 && (
              <Alert tone="warning" title={msg("quizzes.components.DrawRulesPanel.theBankDoesNotHave")}>
                <ul className="list-disc pl-4">{preview.shortages.map((s) => <li key={s.position}>{msg("quizzes.components.DrawRulesPanel.rule")}{s.position} {msg("quizzes.components.DrawRulesPanel.tag")}{s.tag}{msg("quizzes.components.DrawRulesPanel.needs2")}{s.required} {msg("quizzes.components.DrawRulesPanel.questionsTheBankCanOnly")}{s.available} {msg("quizzes.components.DrawRulesPanel.questions4")}</li>)}</ul>
              </Alert>
            )}
            <p className="text-muted">{msg("quizzes.components.DrawRulesPanel.eachAttemptGetsADifferent")}{formatPoints(preview.maxScore)} {msg("quizzes.components.DrawRulesPanel.points3")}</p>
            <ol className="flex flex-col gap-3">
              {preview.items.map((item, index) => (
                <li key={item.question.id}>
                  <p className="flex items-center gap-2 font-semibold">{msg("quizzes.components.DrawRulesPanel.question")}{index + 1} ({formatPoints(item.points)} {msg("quizzes.components.DrawRulesPanel.points4")}<Pill>{msg("quizzes.components.DrawRulesPanel.rule")}{item.rulePosition}</Pill></p>
                  <p className="whitespace-pre-wrap">{item.question.stem}</p>
                  <ul className="mt-1 list-[upper-alpha] pl-6">{item.question.options.map((o) => <li key={o.id}>{o.text}</li>)}</ul>
                </li>
              ))}
            </ol>
          </div>
        </Dialog>
      )}
    </Section>
  );
}
