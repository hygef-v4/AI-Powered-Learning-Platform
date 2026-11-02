"use client";

import { useId, useState } from "react";
import { Alert, Button, Dialog, Pill, TableFrame, Td, Th } from "@/shared/ui";
import { importQuestions } from "../api";
import { errorMessage } from "../model/labels";
import type { ImportResult } from "../model/types";

const MAX_BYTES = 5 * 1024 * 1024;
export const TEMPLATE_HREF = "/templates/question-import.csv";

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
      setError("Tệp tối đa 5 MB.");
      return;
    }
    setBusy(true);
    try {
      setPreview(await importQuestions(subjectId, next, true));
    } catch (failure) {
      setError(errorMessage(failure));
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
      setError(errorMessage(failure));
    } finally {
      setBusy(false);
    }
  };

  return (
    <Dialog open onClose={onClose} dismissible={!busy} size="lg" icon="upload_file" title="Nhập câu hỏi từ tệp"
      footer={<>
        <Button variant="primary" disabled={busy || !preview || preview.valid === 0} onClick={() => void commit()} data-testid="question-import-commit">
          Nhập {preview?.valid ?? 0} câu hợp lệ
        </Button>
      </>}>
      <div className="flex flex-col gap-3 text-[13px]">
        <p className="text-body">
          Tệp .xlsx hoặc .csv theo mẫu: nội dung, loại, các lựa chọn, đáp án đúng, độ khó, thẻ. Tối đa 500 dòng, 5 MB.{" "}
          <a href={TEMPLATE_HREF} download className="font-semibold text-primary-ink hover:underline" data-testid="question-import-template">Tải tệp mẫu</a>
        </p>
        <label htmlFor={`${id}-file`} className="text-xs font-semibold text-body">Tệp câu hỏi</label>
        <input id={`${id}-file`} type="file" accept=".xlsx,.csv,text/csv" disabled={busy} data-testid="question-import-file"
          onChange={(event) => void choose(event.target.files?.[0] ?? null)} className="text-[13px]" />
        {busy && <p role="status" className="text-muted">Đang kiểm tệp…</p>}
        {error && <Alert tone="danger" role="alert">{error}</Alert>}
        {preview && (
          <>
            <div className="flex flex-wrap gap-2" data-testid="question-import-summary">
              <Pill>{preview.total} dòng</Pill>
              <Pill tone="success">{preview.valid} hợp lệ</Pill>
              <Pill tone={preview.invalid > 0 ? "danger" : "neutral"}>{preview.invalid} lỗi</Pill>
            </div>
            <p className="text-muted">Câu nhập vào ở trạng thái Nháp.</p>
            <TableFrame className="max-h-80 overflow-y-auto [&_table]:table-fixed">
              <colgroup><col className="w-[64px]" /><col /><col className="w-[110px]" /><col className="w-[170px]" /></colgroup><thead><tr><Th>Dòng</Th><Th>Câu hỏi</Th><Th>Số lựa chọn</Th><Th>Kết quả</Th></tr></thead>
              <tbody>
                {preview.rows.map((row) => (
                  <tr key={row.row}>
                    <Td>{row.row}</Td>
                    <Td className="max-w-80 truncate">{row.stem}</Td>
                    <Td>{row.optionCount}</Td>
                    <Td>{row.valid
                      ? <Pill tone="success">Hợp lệ</Pill>
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
