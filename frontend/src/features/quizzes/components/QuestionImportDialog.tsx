"use client";

import { useId, useState } from "react";
import { Alert, Button, Dialog, Pill, TableFrame, Td, Th } from "@/shared/ui";
import { importQuestions } from "../api";
import { describeError } from "../model/labels";
import type { ImportResult } from "../model/types";
import { msg } from "@/i18n/msg";

const MAX_BYTES = 5 * 1024 * 1024;
export const TEMPLATE_HREF = "/templates/question-import.xlsx";

type Props = { subjectId: string; onClose: () => void; onImported: (created: number) => void };

/**
 * Popup "Nhập từ tệp" (UC 47, BR-U10-07): chọn tệp → kiểm từng dòng (dryRun) → nhập các dòng hợp lệ thành câu nháp.
 */
export function QuestionImportDialog({ subjectId, onClose, onImported }: Props) {
  const id = useId();
  const [file, setFile] = useState<File | null>(null);
  const [preview, setPreview] = useState<ImportResult | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const choose = async (next: File | null) => {
    setFile(next);
    setPreview(null);
    setError(null);
    if (!next) return;
    if (next.size > MAX_BYTES) {
      setError(msg("quizzes.components.QuestionImportDialog.filesCanBeAtMost"));
      return;
    }
    setBusy(true);
    try {
      setPreview(await importQuestions(subjectId, next, true));
    } catch (failure) {
      setError(describeError(failure));
    } finally {
      setBusy(false);
    }
  };

  const commit = async () => {
    if (!file) return;
    setBusy(true);
    setError(null);
    try {
      const result = await importQuestions(subjectId, file, false);
      onImported(result.created);
    } catch (failure) {
      setError(describeError(failure));
    } finally {
      setBusy(false);
    }
  };

  return (
    <Dialog open onClose={onClose} dismissible={!busy} size="lg" icon="upload_file" title={msg("quizzes.components.QuestionImportDialog.importQuestionsFromExcel")}
      footer={<>
        <Button variant="primary" disabled={busy || !preview || preview.valid === 0} onClick={() => void commit()} data-testid="question-import-commit">
          {msg("quizzes.components.QuestionImportDialog.import")}{preview?.valid ?? 0} {msg("quizzes.components.QuestionImportDialog.validQuestions")}</Button>
      </>}>
      <div className="flex flex-col gap-3 text-[13px]">
        <p className="text-body">
          {msg("quizzes.components.QuestionImportDialog.excelXlsxFileFollowingThe")}{" "}
          <a href={TEMPLATE_HREF} download className="font-semibold text-primary-ink hover:underline" data-testid="question-import-template">{msg("quizzes.components.QuestionImportDialog.downloadTemplate")}</a>
        </p>
        <label htmlFor={`${id}-file`} className="text-xs font-semibold text-body">{msg("quizzes.components.QuestionImportDialog.questionFile")}</label>
        <input id={`${id}-file`} type="file" accept=".xlsx" disabled={busy} data-testid="question-import-file"
          onChange={(event) => void choose(event.target.files?.[0] ?? null)} className="text-[13px]" />
        {busy && <p role="status" className="text-muted">{msg("quizzes.components.QuestionImportDialog.checkingFile")}</p>}
        {error && <Alert tone="danger" role="alert">{error}</Alert>}
        {preview && (
          <>
            <div className="flex flex-wrap gap-2" data-testid="question-import-summary">
              <Pill>{preview.total} {msg("quizzes.components.QuestionImportDialog.rows")}</Pill>
              <Pill tone="success">{preview.valid} {msg("quizzes.components.QuestionImportDialog.valid")}</Pill>
              <Pill tone={preview.invalid > 0 ? "danger" : "neutral"}>{preview.invalid} {msg("quizzes.components.QuestionImportDialog.errors")}</Pill>
            </div>
            <p className="text-muted">{msg("quizzes.components.QuestionImportDialog.importedQuestionsAreInDraft")}</p>
            <TableFrame className="max-h-80 overflow-y-auto [&_table]:table-fixed">
              <colgroup><col className="w-[64px]" /><col /><col className="w-[110px]" /><col className="w-[170px]" /></colgroup><thead><tr><Th>{msg("quizzes.components.QuestionImportDialog.row")}</Th><Th>{msg("quizzes.components.QuestionImportDialog.question")}</Th><Th>{msg("quizzes.components.QuestionImportDialog.options")}</Th><Th>{msg("quizzes.components.QuestionImportDialog.result")}</Th></tr></thead>
              <tbody>
                {preview.rows.map((row) => (
                  <tr key={row.row}>
                    <Td>{row.row}</Td>
                    <Td className="max-w-80 truncate">{row.stem}</Td>
                    <Td>{row.optionCount}</Td>
                    <Td>{row.valid
                      ? <Pill tone="success">{msg("quizzes.components.QuestionImportDialog.valid2")}</Pill>
                      : <span className="text-danger">{row.reasons.map((reason) => reason.message).join("; ")}</span>}</Td>
                  </tr>
                ))}
              </tbody>
            </TableFrame>
          </>
        )}
      </div>
    </Dialog>
  );
}
