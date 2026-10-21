"use client";

import { useEffect, useState } from "react";
import { Alert, Button, Dialog, Field, Pill } from "@/shared/ui";
import { assignmentsApi, type CreateInput } from "./api";
import { errorMessage } from "./format";
import { statusLabels, type AssignmentDetail, type CopySource, type GradingMode, type SubmissionMode } from "./types";

type Props = {
  /** Lớp đích (bài của lớp) hoặc môn (bài của môn). */
  target: { kind: "class"; classId: string; groupSupported: boolean } | { kind: "subject"; subjectId: string };
  onClose: () => void;
  /** {@code importDocx}: người soạn chọn tạo từ tệp DOCX, trang soạn mở sẵn hộp nhập DOCX. */
  onCreated: (assignment: AssignmentDetail, options: { importDocx: boolean }) => void;
};

type Method = "new" | "docx" | "copy";

function Choice<T extends string>({ name, value, options, onChange, disabled }: {
  name: string;
  value: T;
  options: { value: T; label: string; hint?: string; disabled?: boolean }[];
  onChange: (value: T) => void;
  disabled?: boolean;
}) {
  return (
    <div role="radiogroup" aria-label={name} className="grid grid-cols-2 gap-2">
      {options.map((option) => (
        <label key={option.value} className={`flex cursor-pointer flex-col gap-0.5 rounded-xl border px-3 py-2 text-[13px] ${value === option.value ? "border-primary bg-primary-soft" : "border-line-strong"} ${option.disabled || disabled ? "cursor-not-allowed opacity-50" : ""}`}>
          <span className="flex items-center gap-2 font-semibold text-ink">
            <input type="radio" name={name} checked={value === option.value} disabled={option.disabled || disabled}
              onChange={() => onChange(option.value)} />
            {option.label}
          </span>
          {option.hint && <span className="text-[11px] text-muted">{option.hint}</span>}
        </label>
      ))}
    </div>
  );
}

/**
 * t21b Tạo bài tập (F3, F13): tạo mới, tạo từ tệp DOCX hoặc copy bài lớp khác; hình thức và chế độ (BR-U07-01…03).
 * Không còn chọn có sơ đồ hay không: sơ đồ Draw.io luôn dùng được (yêu cầu 2026-10-21).
 */
export function CreateAssignmentDialog({ target, onClose, onCreated }: Props) {
  const [method, setMethod] = useState<Method>("new");
  const [submissionMode, setSubmissionMode] = useState<SubmissionMode>("INDIVIDUAL");
  const [gradingMode, setGradingMode] = useState<GradingMode>("GRADED");
  const [sources, setSources] = useState<CopySource[] | null>(null);
  const [sourceId, setSourceId] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const groupAllowed = target.kind === "class" && target.groupSupported;
  const classId = target.kind === "class" ? target.classId : null;

  useEffect(() => {
    if (method !== "copy" || !classId) return;
    const controller = new AbortController();
    assignmentsApi.copySources(classId, controller.signal).then(setSources, (e) => {
      if (!controller.signal.aborted) setError(errorMessage(e));
    });
    return () => controller.abort();
  }, [method, classId]);

  async function submit() {
    setBusy(true);
    setError(null);
    try {
      let created: AssignmentDetail;
      if (method === "copy" && target.kind === "class") {
        created = await assignmentsApi.copy(target.classId, sourceId);
      } else {
        const input: CreateInput = { submissionMode, gradingMode: submissionMode === "GROUP" ? "GRADED" : gradingMode, diagramEnabled: true };
        created = target.kind === "class" ? await assignmentsApi.createForClass(target.classId, input)
          : await assignmentsApi.createForSubject(target.subjectId, input);
      }
      onCreated(created, { importDocx: method === "docx" });
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }

  return (
    <Dialog open title="Tạo bài tập" icon="assignment" size="lg" onClose={onClose} dismissible={!busy}
      footer={<>
        <Button onClick={onClose} disabled={busy}>Hủy</Button>
        <Button variant="primary" onClick={() => void submit()} disabled={busy || (method === "copy" && !sourceId)} data-testid="create-assignment-submit">
          Tiếp tục
        </Button>
      </>}>
      {error && <Alert tone="danger" role="alert">{error}</Alert>}
      <Field label="Cách tạo">
        <Choice name="Cách tạo" value={method} onChange={setMethod} options={[
          { value: "new", label: "Tạo mới", hint: "Soạn trên trang tài liệu" },
          { value: "docx", label: "Từ tệp DOCX", hint: "Quét tệp Word thành các phần, sửa tiếp trên trang" },
          ...(target.kind === "class" ? [{ value: "copy" as const, label: "Copy từ lớp khác", hint: "Chọn bài ở lớp bạn dạy" }] : []),
        ]} />
      </Field>
      {method === "copy" && target.kind === "class" ? (
        <Field label="Bài để copy" hint="Bản copy là bản nháp mới; không sao lịch, nhóm, bài nộp.">
          {!sources ? <p role="status" className="text-xs text-muted">Đang tải…</p> : sources.length === 0 ? (
            <p className="text-xs text-muted">Không có lớp nào khác bạn dạy có bài tập.</p>
          ) : (
            <div className="flex max-h-64 flex-col gap-2 overflow-y-auto" data-testid="copy-sources">
              {sources.map((source) => (
                <fieldset key={source.classId} className="rounded-xl border border-line p-2">
                  <legend className="px-1 text-xs font-semibold text-body">{source.code} · {source.name}</legend>
                  {source.assignments.map((assignment) => (
                    <label key={assignment.id} className="flex items-center gap-2 py-1 text-[13px]">
                      <input type="radio" name="copy-source" checked={sourceId === assignment.id} onChange={() => setSourceId(assignment.id)} />
                      <span className="min-w-0 flex-1 truncate">{assignment.title}</span>
                      <Pill>{statusLabels[assignment.status]}</Pill>
                    </label>
                  ))}
                </fieldset>
              ))}
            </div>
          )}
        </Field>
      ) : (
        <>
          <Field label="Hình thức">
            <Choice name="Hình thức" value={submissionMode} onChange={(value) => { setSubmissionMode(value); if (value === "GROUP") setGradingMode("GRADED"); }} options={[
              { value: "INDIVIDUAL", label: "Cá nhân" },
              { value: "GROUP", label: "Nhóm", hint: groupAllowed ? "Theo nhóm cố định của lớp, trưởng nhóm nộp" : target.kind === "subject" ? "Bài của môn chỉ có cá nhân" : "Chưa dùng được bài nhóm", disabled: !groupAllowed },
            ]} />
          </Field>
          <Field label="Chế độ">
            <Choice name="Chế độ" value={gradingMode} onChange={setGradingMode} disabled={submissionMode === "GROUP"} options={[
              { value: "GRADED", label: "Graded", hint: "Vào sổ điểm" },
              { value: "PRACTICE", label: "Practice", hint: "Sinh viên nhận góp ý AI" },
            ]} />
          </Field>
          <p className="text-[11px] text-muted">Sơ đồ Draw.io luôn dùng được: chèn sơ đồ vào phần nào của đề cũng được, sinh viên vẽ thẳng vào đó.</p>
        </>
      )}
    </Dialog>
  );
}
