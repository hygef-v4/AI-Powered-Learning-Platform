"use client";
import { useState } from "react";
import { Alert, Button, Dialog, Field, Segmented, Select } from "@/shared/ui";
import { gradingApi } from "./api";
import { message } from "./useResource";
export function GradebookExportDialog({
  classId,
  assignments,
  onClose,
}: {
  classId: string;
  assignments: { id: string; title: string }[];
  onClose: () => void;
}) {
  const [format, setFormat] = useState("xlsx");
  const [assignment, setAssignment] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  async function download() {
    setBusy(true);
    try {
      const blob = await gradingApi.export(classId, format, assignment || undefined);
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement("a");
      anchor.href = url;
      anchor.download = `gradebook-${new Date().toISOString().slice(0, 10)}.${format}`;
      document.body.appendChild(anchor);
      anchor.click();
      anchor.remove();
      setTimeout(() => URL.revokeObjectURL(url), 1000);
      onClose();
    } catch (e) {
      setError(message(e));
    } finally {
      setBusy(false);
    }
  }
  return (
    <Dialog
      open
      title="Xuất bảng điểm"
      onClose={onClose}
      dismissible={!busy}
      footer={
        <>
          <Button variant="primary" icon="download" disabled={busy} onClick={() => void download()}>
            {busy ? "Đang tạo tệp…" : "Xuất tệp"}
          </Button>
        </>
      }
    >
      <Field label="Định dạng">
        <Segmented ariaLabel="Định dạng" value={format} onChange={setFormat} options={[{ value: "xlsx", label: "Excel" }, { value: "csv", label: "CSV" }]} />
      </Field>
      <label className="flex flex-col gap-1.5 text-xs font-semibold text-body">
        Phạm vi
        <Select
          value={assignment}
          onChange={(e) => setAssignment(e.target.value)}
        >
          <option value="">Toàn bộ bài tính điểm</option>
          {assignments.map((a) => (
            <option key={a.id} value={a.id}>
              {a.title}
            </option>
          ))}
        </Select>
      </label>
      {error && <Alert tone="danger" role="alert">{error}</Alert>}
    </Dialog>
  );
}
