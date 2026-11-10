"use client";

import { useEffect, useId, useMemo, useState } from "react";
import { Alert, Button, Dialog, Pill, Section, Select, TextInput } from "@/shared/ui";
import { getBankTags, previewPaper, saveDrawRules } from "../api";
import { DIFFICULTY_LABELS, formatPoints } from "../model/labels";
import type { BankTag, Difficulty, PaperPreview, QuizDetail, QuizPaperMode } from "../model/types";

type Row = { key: number; tag: string; difficulty: Difficulty | ""; questionCount: string; pointsEach: string };

const POINTS = /^\d+([.,]\d{1,2})?$/;
const decimal = (value: string) => Number(value.trim().replace(",", "."));
const validRow = (r: Row) => r.tag.trim() !== "" && Number.isInteger(Number(r.questionCount)) && Number(r.questionCount) >= 1
  && POINTS.test(r.pointsEach.trim()) && decimal(r.pointsEach) > 0;
const rowsOf = (detail: QuizDetail): Row[] => detail.drawRules.map((r) => ({ key: r.position, tag: r.tag, difficulty: r.difficulty ?? "",
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
  const [rows, setRows] = useState<Row[]>(() => rowsOf(detail));
  const [tags, setTags] = useState<BankTag[] | null>(null);
  const [busy, setBusy] = useState(false);
  const [preview, setPreview] = useState<PaperPreview | null>(null);
  const [nextKey, setNextKey] = useState(1000);

  const quizId = detail.quiz.id;
  const saved = useMemo(() => JSON.stringify([detail.quiz.paperMode, rowsOf(detail).map(({ tag, difficulty, questionCount, pointsEach }) => [tag, difficulty, questionCount, pointsEach])]), [detail]);
  const current = JSON.stringify([mode, rows.map(({ tag, difficulty, questionCount, pointsEach }) => [tag, difficulty, questionCount, pointsEach])]);
  const dirty = saved !== current;

  useEffect(() => {
    if (!editable || mode !== "GENERATED") return;
    const controller = new AbortController();
    getBankTags(quizId, controller.signal).then(setTags).catch(() => undefined);
    return () => controller.abort();
  }, [editable, mode, quizId]);

  // Số câu trong kho khớp thẻ và độ khó của quy tắc; null khi chưa tải được danh sách thẻ.
  const available = (row: Row) => {
    if (!tags) return null;
    const found = tags.find((t) => t.tag.toLowerCase() === row.tag.trim().toLowerCase());
    if (!found) return 0;
    return row.difficulty ? found.byDifficulty[row.difficulty] ?? 0 : found.activeCount;
  };
  const update = (key: number, patch: Partial<Row>) => setRows((list) => list.map((r) => (r.key === key ? { ...r, ...patch } : r)));
  const total = rows.reduce((sum, r) => sum + (Number(r.questionCount) || 0), 0);
  const totalPoints = rows.reduce((sum, r) => sum + (Number(r.questionCount) || 0) * (decimal(r.pointsEach) || 0), 0);
  const invalid = mode === "GENERATED" && (rows.length === 0 || rows.some((r) => !validRow(r)));

  const save = async () => {
    setBusy(true);
    try {
      onSaved(await saveDrawRules(quizId, mode, rows.map((r) => ({ tag: r.tag.trim(), difficulty: r.difficulty || null,
        questionCount: Number(r.questionCount), pointsEach: decimal(r.pointsEach) })), detail.quiz.version));
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
    <Section title="Cách lập đề" icon="tune" actions={editable && dirty
      ? <Button size="sm" variant="primary" disabled={busy || disabled || invalid} onClick={() => void save()} data-testid="draw-rules-save">Lưu cách lập đề</Button>
      : undefined}>
      <fieldset disabled={!editable || busy} className="flex flex-col gap-3" data-testid="draw-rules">
        <Select aria-label="Cách lập đề" value={mode} onChange={(event) => setMode(event.target.value as QuizPaperMode)} data-testid="draw-rules-mode">
          <option value="FIXED">Đề cố định: mọi lượt cùng danh sách câu</option>
          <option value="GENERATED">Sinh đề mới cho mỗi lượt làm</option>
        </Select>
        {mode === "GENERATED" && <>
          <datalist id={`${id}-tags`}>{tags?.map((t) => <option key={t.tag} value={t.tag}>{t.activeCount} câu</option>)}</datalist>
          <ul className="flex flex-col gap-2">
            {rows.map((row, index) => {
              const hasTag = row.tag.trim() !== "";
              const count = available(row);
              const level = row.difficulty ? ` mức ${DIFFICULTY_LABELS[row.difficulty]}` : "";
              const requested = Number(row.questionCount);
              const short = hasTag && count !== null && Number.isInteger(requested) && requested > count;
              return (
                <li key={row.key} className="flex flex-col gap-1 rounded-lg border border-line p-2" data-testid="draw-rule">
                  <TextInput aria-label={`Thẻ của quy tắc ${index + 1}`} placeholder="Thẻ, ví dụ Chương 1" list={`${id}-tags`} value={row.tag}
                    onChange={(event) => update(row.key, { tag: event.target.value })} data-testid={`draw-rule-tag-${index}`} />
                  <div className="grid grid-cols-3 gap-1">
                    <Select aria-label={`Độ khó của quy tắc ${index + 1}`} value={row.difficulty} onChange={(event) => update(row.key, { difficulty: event.target.value as Difficulty | "" })}>
                      <option value="">Mọi độ khó</option>
                      {(Object.keys(DIFFICULTY_LABELS) as Difficulty[]).map((d) => <option key={d} value={d}>{DIFFICULTY_LABELS[d]}</option>)}
                    </Select>
                    <TextInput aria-label={`Số câu của quy tắc ${index + 1}`} inputMode="numeric" suffix="câu" value={row.questionCount}
                      onChange={(event) => update(row.key, { questionCount: event.target.value })} data-testid={`draw-rule-count-${index}`} />
                    <TextInput aria-label={`Điểm mỗi câu của quy tắc ${index + 1}`} inputMode="decimal" suffix="điểm" value={row.pointsEach}
                      onChange={(event) => update(row.key, { pointsEach: event.target.value })} data-testid={`draw-rule-points-${index}`} />
                  </div>
                  {short && <p role="status" className="text-xs font-medium text-warning" data-testid={`draw-rule-shortage-${index}`}>Cần {requested} câu, kho chỉ có {count} câu; chưa phát hành được.</p>}
                  <div className="flex items-center justify-between gap-2 text-xs text-muted">
                    <span>{!hasTag ? "Chọn thẻ để xem số câu trong kho" : count === null ? "" : count === 0 ? `Kho chưa có câu${level} mang thẻ này` : `Kho có ${count} câu${level} đang dùng mang thẻ này`}</span>
                    {editable && <Button size="sm" variant="ghost" onClick={() => setRows((list) => list.filter((r) => r.key !== row.key))} aria-label={`Bỏ quy tắc ${index + 1}`}>Bỏ</Button>}
                  </div>
                </li>
              );
            })}
          </ul>
          {editable && (
            <Button size="sm" icon="add" onClick={() => { setRows((list) => [...list, { key: nextKey, tag: "", difficulty: "", questionCount: "1", pointsEach: "1" }]); setNextKey((k) => k + 1); }}
              data-testid="draw-rule-add">Thêm quy tắc</Button>
          )}
          <p className="text-[13px] text-body" data-testid="draw-rules-total">Mỗi lượt: {total} câu · {formatPoints(totalPoints)} điểm</p>
          <p className="text-xs text-muted">Danh sách câu bên dưới không dùng khi sinh đề. Câu lấy từ ngân hàng của môn; kho không đủ câu thì chưa phát hành được.</p>
          {!dirty && rows.length > 0 && (
            <Button size="sm" icon="visibility" onClick={() => void openPreview()} data-testid="draw-rules-preview">Xem thử đề mẫu</Button>
          )}
        </>}
      </fieldset>
      {preview && (
        <Dialog open size="lg" onClose={() => setPreview(null)} icon="visibility" title="Đề mẫu" footer={<Button onClick={() => setPreview(null)}>Đóng</Button>}>
          <div className="flex flex-col gap-3 text-[13px]" data-testid="paper-preview">
            {preview.shortages.length > 0 && (
              <Alert tone="warning" title="Kho chưa đủ câu">
                <ul className="list-disc pl-4">{preview.shortages.map((s) => <li key={s.position}>Quy tắc {s.position} (thẻ {s.tag}): cần {s.required} câu, kho chỉ cấp được {s.available} câu.</li>)}</ul>
              </Alert>
            )}
            <p className="text-muted">Mỗi lượt làm sẽ nhận một đề khác. Đề này chỉ để xem, không được lưu. Tổng {formatPoints(preview.maxScore)} điểm.</p>
            <ol className="flex flex-col gap-3">
              {preview.items.map((item, index) => (
                <li key={item.question.id}>
                  <p className="flex items-center gap-2 font-semibold">Câu {index + 1} ({formatPoints(item.points)} điểm) <Pill>Quy tắc {item.rulePosition}</Pill></p>
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
