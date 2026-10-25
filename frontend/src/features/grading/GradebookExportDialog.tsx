"use client";
import { useState } from "react";
import { Dialog } from "@/shared/ui/Dialog";
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
  const [format, setFormat] = useState("csv");
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
      title="Xuất sổ điểm"
      onClose={onClose}
      dismissible={!busy}
      footer={
        <>
          <button disabled={busy} onClick={onClose}>
            Hủy
          </button>
          <button
            disabled={busy}
            onClick={() => void download()}
            className="ml-3 rounded bg-primary px-4 py-2 text-white"
          >
            {busy ? "Đang tạo tệp…" : "Xuất tệp"}
          </button>
        </>
      }
    >
      <label className="my-4 block">
        Định dạng{" "}
        <select
          value={format}
          onChange={(e) => setFormat(e.target.value)}
          className="rounded border p-2"
        >
          <option value="csv">CSV</option>
          <option value="xlsx">Excel (.xlsx)</option>
        </select>
      </label>
      <label>
        Phạm vi{" "}
        <select
          value={assignment}
          onChange={(e) => setAssignment(e.target.value)}
          className="rounded border p-2"
        >
          <option value="">Toàn bộ bài Graded</option>
          {assignments.map((a) => (
            <option key={a.id} value={a.id}>
              {a.title}
            </option>
          ))}
        </select>
      </label>
      <p className="mt-4">
        Chỉ xuất điểm đã chốt/công bố. Mục chưa nộp/chưa chốt được ghi rõ; không có điểm tổng,
        Practice hay Quiz.
      </p>
      <p className="mt-2 text-sm text-slate-600">
        Phạm vi xuất áp dụng cho toàn bộ danh sách sinh viên của lớp theo lựa chọn ở trên, không phụ
        thuộc bộ lọc tìm kiếm hay trang đang xem. Thao tác xuất được ghi audit; tệp không lưu lại
        trên hệ thống.
      </p>
      {error && <p role="alert">{error}</p>}
    </Dialog>
  );
}
