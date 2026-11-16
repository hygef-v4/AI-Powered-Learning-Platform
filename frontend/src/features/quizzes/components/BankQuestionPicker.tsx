"use client";

import { useEffect, useState } from "react";
import { Alert, Button, Dialog, Pill, Segmented, Select, TableFrame, Td, TextInput, Th } from "@/shared/ui";
import { getBankQuestionsForQuiz, previewRandomItems } from "../api";
import { DIFFICULTY_LABELS, DIFFICULTY_TONES, describeError, TYPE_LABELS } from "../model/labels";
import type { Difficulty, QuestionPage, QuestionView } from "../model/types";
import { msg } from "@/i18n/msg";

type Props = { quizId: string; freeSlots: number; onClose: () => void; onAdd: (questionIds: string[]) => Promise<void>;
  inline?: boolean; onDirtyChange?: (dirty: boolean) => void };
type Tab = "manual" | "random";
type Row = { id: string; stem: string; type: QuestionView["type"]; difficulty: Difficulty; tags: string[] };

/**
 * Popup "Lấy câu hỏi từ ngân hàng" (BR-U10-14): chọn tay hoặc bốc ngẫu nhiên câu Đang dùng của môn, ẩn câu đã có
 * trong quiz. Chỉ là xem trước; lưu vào quiz mới ghim phiên bản.
 */
export function BankQuestionPicker({ quizId, freeSlots, onClose, onAdd, inline = false, onDirtyChange }: Props) {
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
        .catch((failure: unknown) => active && setError(describeError(failure)));
    }, 250);
    return () => {
      active = false;
      clearTimeout(timer);
    };
  }, [quizId, tab, difficulty, tag, text]);

  const draw = async () => {
    const n = Number(count);
    if (!Number.isInteger(n) || n < 1 || n > freeSlots) {
      setError(msg("quizzes.components.BankQuestionPicker.numberOfQuestionsFrom1", { freeSlots: String(freeSlots) }));
      return;
    }
    setBusy(true);
    setError(null);
    try {
      const result = await previewRandomItems(quizId, n, difficulty || null, tag.trim() || null);
      setRandom({ rows: result.questions, matching: result.matching });
    } catch (failure) {
      setError(describeError(failure));
    } finally {
      setBusy(false);
    }
  };

  const ids = tab === "manual" ? selected : random?.rows.map((row) => row.id) ?? [];
  const tooMany = ids.length > freeSlots;
  useEffect(() => { onDirtyChange?.(ids.length > 0); }, [ids.length, onDirtyChange]);
  useEffect(() => () => onDirtyChange?.(false), [onDirtyChange]);
  const addQuestions = async () => {
    setBusy(true);
    setError(null);
    try {
      await onAdd(ids);
    } catch (failure) {
      setError(describeError(failure));
      setBusy(false);
    }
  };

  const toggle = (id: string) => setSelected((current) => (current.includes(id) ? current.filter((x) => x !== id) : [...current, id]));

  const footer = <>
        {inline && <Button onClick={onClose} disabled={busy}>{msg("quizzes.components.BankQuestionPicker.clearSelection")}</Button>}
        <Button variant="primary" disabled={busy || ids.length === 0 || tooMany} onClick={() => void addQuestions()} data-testid="bank-picker-add">
          {tab === "manual" ? msg("quizzes.components.BankQuestionPicker.addQuestions", { length: String(ids.length) }) : msg("quizzes.components.BankQuestionPicker.saveQuestionsToTheQuiz", { length: String(ids.length) })}
        </Button>
      </>;
  const body = (
      <div className="flex flex-col gap-3 text-[13px]">
        <Segmented
          as="tab"
          ariaLabel={msg("quizzes.components.BankQuestionPicker.howToPickQuestions")}
          className="w-fit"
          value={tab}
          onChange={(value) => { setTab(value); setError(null); }}
          options={[
            { value: "manual", label: msg("quizzes.components.BankQuestionPicker.pickManually"), testId: "bank-picker-tab-manual" },
            { value: "random", label: msg("quizzes.components.BankQuestionPicker.drawRandomly"), testId: "bank-picker-tab-random" },
          ]}
        />
        <div className="flex flex-wrap items-center gap-2">
          <Select aria-label={msg("quizzes.components.BankQuestionPicker.difficulty")} value={difficulty} onChange={(event) => setDifficulty(event.target.value as Difficulty | "")} data-testid="bank-picker-difficulty">
            <option value="">{msg("quizzes.components.BankQuestionPicker.allDifficulties")}</option>
            {(Object.keys(DIFFICULTY_LABELS) as Difficulty[]).map((d) => <option key={d} value={d}>{DIFFICULTY_LABELS[d]}</option>)}
          </Select>
          <TextInput aria-label={msg("quizzes.components.BankQuestionPicker.tags")} placeholder={msg("quizzes.components.BankQuestionPicker.tags")} value={tag} onChange={(event) => setTag(event.target.value)} wrapperClassName="w-40" />
          {tab === "manual" && (
            <TextInput icon="search" aria-label={msg("quizzes.components.BankQuestionPicker.findQuestions")} placeholder={msg("quizzes.components.BankQuestionPicker.searchContent")} value={text} onChange={(event) => setText(event.target.value)}
              wrapperClassName="min-w-48 flex-1" data-testid="bank-picker-search" />
          )}
          {tab === "random" && <>
            <TextInput aria-label={msg("quizzes.components.BankQuestionPicker.questions")} inputMode="numeric" value={count} onChange={(event) => setCount(event.target.value)} wrapperClassName="w-24"
              data-testid="bank-picker-count" />
            <Button onClick={() => void draw()} disabled={busy} data-testid="bank-picker-draw">{random ? msg("quizzes.components.BankQuestionPicker.drawAgain") : msg("quizzes.components.BankQuestionPicker.tryADraw")}</Button>
          </>}
        </div>
        <p className="text-muted">{msg("quizzes.components.BankQuestionPicker.text")}{freeSlots} {msg("quizzes.components.BankQuestionPicker.freeSlotsInTheQuiz")}{tab === "manual" ? msg("quizzes.components.BankQuestionPicker.selectedQuestions", { length: String(selected.length) }) : random ? msg("quizzes.components.BankQuestionPicker.matchingQuestions", { matching: String(random.matching) }) : ""}</p>
        {tooMany && <Alert tone="warning">{msg("quizzes.components.BankQuestionPicker.theSelectionExceedsTheQuizs")}</Alert>}
        {error && <Alert tone="danger" role="alert">{error}</Alert>}
        <TableFrame className="max-h-80 overflow-y-auto [&_table]:min-w-[600px] [&_table]:table-fixed">
          <colgroup><col className="w-[64px]" /><col /><col className="w-[130px]" /><col className="w-[100px]" /><col className="w-[20%]" /></colgroup><thead><tr><Th>{tab === "manual" ? msg("quizzes.components.BankQuestionPicker.select") : ""}</Th><Th>{msg("quizzes.components.BankQuestionPicker.question")}</Th><Th>{msg("quizzes.components.BankQuestionPicker.type")}</Th><Th>{msg("quizzes.components.BankQuestionPicker.difficulty")}</Th><Th>{msg("quizzes.components.BankQuestionPicker.tags")}</Th></tr></thead>
          <tbody>
            {(tab === "manual" ? page?.items ?? [] : random?.rows ?? []).map((row) => (
              <tr key={row.id} data-testid="bank-picker-row">
                <Td>
                  {tab === "manual"
                    ? <input type="checkbox" checked={selected.includes(row.id)} onChange={() => toggle(row.id)} aria-label={msg("quizzes.components.BankQuestionPicker.selectQuestion", { stem: String(row.stem) })} />
                    : <Button size="sm" variant="ghost" onClick={() => setRandom((r) => r && { ...r, rows: r.rows.filter((x) => x.id !== row.id) })}>{msg("quizzes.components.BankQuestionPicker.remove")}</Button>}
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
  );
  if (inline) return <div role="region" aria-label={msg("quizzes.components.BankQuestionPicker.takeQuestionsFromTheBank")} className="flex min-w-0 flex-col gap-3">
    {body}<div className="flex justify-end gap-2 border-t border-line pt-3">{footer}</div>
  </div>;
  return <Dialog open onClose={onClose} dismissible={!busy} size="lg" icon="help_center" title={msg("quizzes.components.BankQuestionPicker.takeQuestionsFromTheBank")} footer={footer}>{body}</Dialog>;
}
