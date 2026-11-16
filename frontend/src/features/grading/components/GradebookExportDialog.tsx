"use client";
import { useState } from "react";
import { Alert, Button, Dialog, Field, Segmented, Select } from "@/shared/ui";
import { gradingApi } from "../api";
import { formatErrorMessage } from "../model/format";
import { msg } from "@/i18n/msg";
export function GradebookExportDialog({
  classId,
  assignments,
  initialAssignmentId = "",
  onClose,
}: {
  classId: string;
  assignments: { id: string; title: string }[];
  initialAssignmentId?: string;
  onClose: () => void;
}) {
  const [format, setFormat] = useState("xlsx");
  const [assignment, setAssignment] = useState(initialAssignmentId);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  async function handleDownload() {
    setBusy(true);
    try {
      const { blob, filename } = await gradingApi.exportGrades(classId, format, assignment || undefined);
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement("a");
      anchor.href = url;
      anchor.download = filename || `${assignments.find((item) => item.id === assignment)?.title || msg("grading.components.GradebookExportDialog.bangDiemTongHop")}.${format}`;
      document.body.appendChild(anchor);
      anchor.click();
      anchor.remove();
      setTimeout(() => URL.revokeObjectURL(url), 1000);
      onClose();
    } catch (e) {
      setError(formatErrorMessage(e));
    } finally {
      setBusy(false);
    }
  }
  return (
    <Dialog
      open
      title={msg("grading.components.GradebookExportDialog.xuatBangDiem")}
      onClose={onClose}
      dismissible={!busy}
      footer={
        <>
          <Button variant="primary" icon="download" disabled={busy} onClick={() => void handleDownload()}>
            {busy ? msg("grading.components.GradebookExportDialog.dangTaoTep") : msg("grading.components.GradebookExportDialog.xuatTep")}
          </Button>
        </>
      }
    >
      <Field label={msg("grading.components.GradebookExportDialog.dinhDang")}>
        <Segmented ariaLabel={msg("grading.components.GradebookExportDialog.dinhDang")} value={format} onChange={setFormat} options={[{ value: "xlsx", label: "Excel" }, { value: "csv", label: "CSV" }]} />
      </Field>
      <label className="flex flex-col gap-1.5 text-xs font-semibold text-body">
        {msg("grading.components.GradebookExportDialog.phamVi")}<Select
          value={assignment}
          onChange={(e) => setAssignment(e.target.value)}
        >
          <option value="">{msg("grading.components.GradebookExportDialog.toanBoBaiTinhDiem")}</option>
          {assignments.map((a) => (
            <option key={a.id} value={a.id}>
              {a.title}
            </option>
          ))}
        </Select>
      </label>
      <p className="text-xs text-muted">{msg("grading.components.GradebookExportDialog.xuatTatCaSinhVienTrong")}</p>
      {error && <Alert tone="danger" role="alert">{error}</Alert>}
    </Dialog>
  );
}
