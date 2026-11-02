"use client";

import { useEffect, useState } from "react";
import { Alert, Button, cx, Dialog, Pill, Select, TableFrame, Td, TextInput, Th } from "@/shared/ui";
import { getBankQuestionsForQuiz, previewRandomItems } from "./api";
import { DIFFICULTY_LABELS, DIFFICULTY_TONES, errorMessage, TYPE_LABELS } from "./labels";
import type { Difficulty, QuestionPage, QuestionView } from "./types";

type Props = { quizId: string; freeSlots: number; onClose: () => void; onAdd: (questionIds: string[]) => Promise<void> };
type Tab = "manual" | "random";
type Row = { id: string; stem: string; type: QuestionView["type"]; difficulty: Difficulty; tags: string[] };

/**
 * Popup "Lấy câu hỏi từ ngân hàng" (BR-U10-14): chọn tay hoặc bốc ngẫu nhiên câu Đang dùng của môn, ẩn câu đã có
 * trong quiz. Chỉ là xem trước; lưu vào quiz mới ghim phiên bản.
 */
export function BankQuestionPicker({ quizId, freeSlots, onClose, onAdd }: Props) {
  const [tab, setTab] = useState<Tab>("manual");
  const [difficulty, setDifficulty] = useState<Difficulty | "">("");
  const [tag, setTag] = useState("");
  const [text, setText] = useState("");
  const [page, setPage] = useState<QuestionPage | null>(null);
  const [selected, setSelected] = useState<string[]>([]);
  const [count, setCount] = useState("5");
  const [random, setRandom] = useState<{ rows: Row[]; matching: number } | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (tab !== "manual") return;
    let active = true;
    const timer = setTimeout(() => {
      getBankQuestionsForQuiz(quizId, { difficulty: difficulty || undefined, tag: tag.trim() || undefined, q: text.trim() || undefined, size: 50 })
        .then((result) => {
          if (!active) return;
          setError(null);
          setPage(result);
        })
        .catch((failure: unknown) => active && setError(errorMessage(failure)));
    }, 250);
    return () => {
      active = false;
      clearTimeout(timer);
    };
  }, [quizId, tab, difficulty, tag, text]);

  const draw = async () => {
    const n = Number(count);
    if (!Number.isInteger(n) || n < 1 || n > freeSlots) {
      setError(`Số câu từ 1 đến ${freeSlots}.`);
      return;
    }
    setBusy(true);
    setError(null);
    try {
      const result = await previewRandomItems(quizId, n, difficulty || null, tag.trim() || null);
      setRandom({ rows: result.questions, matching: result.matching });
    } catch (failure) {
      setError(errorMessage(failure));
    } finally {
      setBusy(false);
    }
  };

  const ids = tab === "manual" ? selected : random?.rows.map((row) => row.id) ?? [];
  const tooMany = ids.length > freeSlots;
  const add = async () => {
    setBusy(true);
    setError(null);
    try {
      await onAdd(ids);
    } catch (failure) {
      setError(errorMessage(failure));
      setBusy(false);
    }
  };

  const toggle = (id: string) => setSelected((current) => (current.includes(id) ? current.filter((x) => x !== id) : [...current, id]));

  return (
    <Dialog open onClose={onClose} dismissible={!busy} size="lg" icon="help_center" title="Lấy câu hỏi từ ngân hàng"
      footer={<>
        <Button variant="primary" disabled={busy || ids.length === 0 || tooMany} onClick={() => void add()} data-testid="bank-picker-add">
          {tab === "manual" ? `Thêm ${ids.length} câu` : `Lưu ${ids.length} câu vào quiz`}
        </Button>
      </>}>
      <div className="flex flex-col gap-3 text-[13px]">
        <div role="tablist" aria-label="Cách lấy câu" className="flex gap-1">
          {([["manual", "Chọn tay"], ["random", "Bốc ngẫu nhiên"]] as const).map(([key, label]) => (
            <button key={key} type="button" role="tab" aria-selected={tab === key} onClick={() => { setTab(key); setError(null); }}
              data-testid={`bank-picker-tab-${key}`}
              className={cx("rounded-lg px-3 py-1.5 font-semibold", tab === key ? "bg-primary-soft text-primary-ink" : "text-body hover:bg-shell")}>
              {label}
            </button>
          ))}
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <Select aria-label="Độ khó" value={difficulty} onChange={(event) => setDifficulty(event.target.value as Difficulty | "")} data-testid="bank-picker-difficulty">
            <option value="">Mọi độ khó</option>
            {(Object.keys(DIFFICULTY_LABELS) as Difficulty[]).map((d) => <option key={d} value={d}>{DIFFICULTY_LABELS[d]}</option>)}
          </Select>
          <TextInput aria-label="Thẻ" placeholder="Thẻ" value={tag} onChange={(event) => setTag(event.target.value)} wrapperClassName="w-40" />
          {tab === "manual" && (
            <TextInput icon="search" aria-label="Tìm câu hỏi" placeholder="Tìm nội dung" value={text} onChange={(event) => setText(event.target.value)}
              wrapperClassName="min-w-48 flex-1" data-testid="bank-picker-search" />
          )}
          {tab === "random" && <>
            <TextInput aria-label="Số câu" inputMode="numeric" value={count} onChange={(event) => setCount(event.target.value)} wrapperClassName="w-24"
              data-testid="bank-picker-count" />
            <Button onClick={() => void draw()} disabled={busy} data-testid="bank-picker-draw">{random ? "Bốc lại" : "Bốc thử"}</Button>
          </>}
        </div>
        <p className="text-muted">Còn {freeSlots} chỗ trống trong quiz. {tab === "manual" ? `Đã chọn ${selected.length} câu.` : random ? `Có ${random.matching} câu phù hợp.` : ""}</p>
        {tooMany && <Alert tone="warning">Số câu chọn vượt chỗ trống của quiz.</Alert>}
        {error && <Alert tone="danger" role="alert">{error}</Alert>}
        <TableFrame className="max-h-80 overflow-y-auto [&_table]:min-w-[600px] [&_table]:table-fixed">
          <colgroup><col className="w-[64px]" /><col /><col className="w-[130px]" /><col className="w-[100px]" /><col className="w-[20%]" /></colgroup><thead><tr><Th>{tab === "manual" ? "Chọn" : ""}</Th><Th>Câu hỏi</Th><Th>Loại</Th><Th>Độ khó</Th><Th>Thẻ</Th></tr></thead>
          <tbody>
            {(tab === "manual" ? page?.items ?? [] : random?.rows ?? []).map((row) => (
              <tr key={row.id} data-testid="bank-picker-row">
                <Td>
                  {tab === "manual"
                    ? <input type="checkbox" checked={selected.includes(row.id)} onChange={() => toggle(row.id)} aria-label={`Chọn câu ${row.stem}`} />
                    : <Button size="sm" variant="ghost" onClick={() => setRandom((r) => r && { ...r, rows: r.rows.filter((x) => x.id !== row.id) })}>Bỏ</Button>}
                </Td>
                <Td className="max-w-80"><p className="line-clamp-2 whitespace-pre-wrap">{row.stem}</p></Td>
                <Td>{TYPE_LABELS[row.type]}</Td>
                <Td><Pill tone={DIFFICULTY_TONES[row.difficulty]}>{DIFFICULTY_LABELS[row.difficulty]}</Pill></Td>
                <Td className="text-xs text-muted">{row.tags.join(", ")}</Td>
              </tr>
            ))}
          </tbody>
        </TableFrame>
      </div>
    </Dialog>
  );
}
