"use client";

import { useEffect, useState } from "react";
import { Alert, Button, Dialog, Field, Icon, Pill } from "@/shared/ui";
import { assignmentsApi, type CreateInput } from "./api";
import { Choice } from "./Choice";
import { errorMessage } from "./format";
import { statusLabels, type AssignmentDetail, type CopySource, type GradingMode, type SubmissionMode } from "./types";

type Props = {
  /** Lớp đích (bài của lớp) hoặc môn (bài của môn). */
  target: { kind: "class"; classId: string; groupSupported: boolean } | { kind: "subject"; subjectId: string };
  onClose: () => void;
  /** Bài đã tạo (đã nhập sẵn tệp DOCX nếu người soạn chọn tệp). */
  onCreated: (assignment: AssignmentDetail, startWithAi?: boolean) => void;
};

/** Ô chọn tệp DOCX (tùy chọn): chọn xong chỉ hiện tên tệp, bấm ✕ để bỏ. */
function DocxField({ file, onChange, disabled }: { file: File | null; onChange: (file: File | null) => void; disabled?: boolean }) {
  if (file) {
    return (
      <div className="flex items-center gap-2 rounded-xl border border-line-strong px-3 py-2 text-[13px]" data-testid="docx-file">
        <Icon name="description" size={18} className="text-primary" />
        <span className="min-w-0 flex-1 truncate text-ink" title={file.name}>{file.name}</span>
        <button type="button" aria-label="Bỏ tệp" disabled={disabled} onClick={() => onChange(null)}
          className="inline-flex size-6 items-center justify-center rounded-md text-muted hover:bg-shell hover:text-ink">
          <Icon name="close" size={16} />
        </button>
      </div>
    );
  }
  return (
    <label className="flex cursor-pointer items-center gap-2 rounded-xl border border-dashed border-line-strong px-3 py-2 text-[13px] text-muted hover:border-primary hover:text-primary">
      <Icon name="upload_file" size={18} />Tệp DOCX
      <input type="file" accept=".docx" className="sr-only" aria-label="Tệp DOCX" data-testid="create-docx" disabled={disabled}
        onChange={(event) => { onChange(event.target.files?.[0] ?? null); event.target.value = ""; }} />
    </label>
  );
}

/**
 * t21b Tạo bài tập (F3, F13): hình thức, chế độ (BR-U07-01…03) và một ô tệp DOCX tùy chọn; có tệp thì tạo xong nhập
 * luôn tệp vào khung rồi mới mở trang soạn (yêu cầu 2026-10-22, không còn bước xem trước nội dung tệp). Bài của lớp
 * còn copy được từ lớp khác. Không còn chọn có sơ đồ hay không: sơ đồ Draw.io luôn dùng được (yêu cầu 2026-10-21).
 */
export function CreateAssignmentDialog({ target, onClose, onCreated }: Props) {
  const [copying, setCopying] = useState(false);
  const [mode, setMode] = useState<"manual" | "import" | "ai">("import");
  const [file, setFile] = useState<File | null>(null);
  const [submissionMode, setSubmissionMode] = useState<SubmissionMode>("INDIVIDUAL");
  const [gradingMode, setGradingMode] = useState<GradingMode>("GRADED");
  const [sources, setSources] = useState<CopySource[] | null>(null);
  const [sourceId, setSourceId] = useState("");
  // Bài đã tạo mà nhập tệp chưa được: thử lại chỉ nhập lại tệp, không tạo thêm bài.
  const [created, setCreated] = useState<AssignmentDetail | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const groupAllowed = target.kind === "class" && target.groupSupported;
  const classId = target.kind === "class" ? target.classId : null;

  useEffect(() => {
    if (!copying || !classId) return;
    const controller = new AbortController();
    assignmentsApi.copySources(classId, controller.signal).then(setSources, (e) => {
      if (!controller.signal.aborted) setError(errorMessage(e));
    });
    return () => controller.abort();
  }, [copying, classId]);

  async function submit() {
    setBusy(true);
    setError(null);
    let assignment = created;
    try {
      if (!assignment) {
        if (copying && target.kind === "class") {
          assignment = await assignmentsApi.copy(target.classId, sourceId);
        } else {
          const input: CreateInput = { submissionMode, gradingMode: submissionMode === "GROUP" ? "GRADED" : gradingMode, diagramEnabled: true };
          assignment = target.kind === "class" ? await assignmentsApi.createForClass(target.classId, input)
            : await assignmentsApi.createForSubject(target.subjectId, input);
        }
      }
      if (file && !copying && mode === "import") {
        const made = assignment;
        setCreated(made);
        const imported = await assignmentsApi.importDocx(made.id, file);
        assignment = await assignmentsApi.saveSkeleton(made.id, made.version, imported.document, { fromDocx: true });
      }
      if (mode === "ai") onCreated(assignment, true);
      else onCreated(assignment);
    } catch (e) {
      setError(assignment ? `Đã tạo bài, chưa nhập tệp: ${errorMessage(e)}` : errorMessage(e));
    } finally {
      setBusy(false);
    }
  }

  return (
    <Dialog open title="Tạo bài tập" icon="assignment" size="lg" onClose={onClose} dismissible={!busy}
      footer={<>
        {classId && !created && (
          <button type="button" className="mr-auto text-xs font-semibold text-primary hover:underline disabled:opacity-50" disabled={busy}
            data-testid="create-copy-toggle" onClick={() => { setCopying((value) => !value); setError(null); }}>
            {copying ? "Tạo bài mới" : "Copy từ lớp khác"}
          </button>
        )}
        {created ? (
          <Button onClick={() => onCreated(created)} disabled={busy}>Mở bài</Button>
        ) : (
          <Button onClick={onClose} disabled={busy}>Hủy</Button>
        )}
        <Button variant="primary" onClick={() => void submit()} disabled={busy || (copying && !sourceId)} data-testid="create-assignment-submit">
          {created ? "Thử lại" : mode === "ai" ? "Tạo bài và mở AI" : mode === "import" && file ? "Tạo & nhập DOCX" : "Tạo bài nháp"}
        </Button>
      </>}>
      {error && <Alert tone="danger" role="alert">{error}</Alert>}
      {copying && target.kind === "class" ? (
        <Field label="Bài để copy">
          {!sources ? <p role="status" className="text-xs text-muted">Đang tải…</p> : sources.length === 0 ? (
            <p className="text-xs text-muted">Không có bài ở lớp khác.</p>
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
          <div role="tablist" aria-label="Cách soạn bài" className="flex border-b border-line">
            {([
              ["manual", "Soạn thủ công"], ["import", "Import từ file"], ["ai", "Soạn bằng AI"],
            ] as const).map(([value, label]) => <button key={value} type="button" role="tab" aria-selected={mode === value}
              onClick={() => setMode(value)} className={`border-b-2 px-3 py-2 text-[13px] ${mode === value ? "border-primary font-semibold text-primary" : "border-transparent text-muted"}`}>
              {label}
            </button>)}
          </div>
          {mode === "import" && <>
            <p className="text-xs text-muted">Tải DOCX để nhập nội dung vào trình soạn thảo sau khi tạo bài.</p>
            <DocxField file={file} onChange={setFile} disabled={busy} />
          </>}
          {mode === "manual" && <p className="text-xs text-muted">Tạo bài nháp trống để tự nhập câu hỏi, hướng dẫn và rubric.</p>}
          {mode === "ai" && <div className="rounded-lg bg-canvas p-3 text-xs text-body">
            Mở trình soạn bài để nhập yêu cầu; AI tự suy ra dạng SWR/SWD từ yêu cầu và tài liệu nguồn. Bạn vẫn chọn ngôn ngữ, số câu, tổng điểm và có thể tải tệp nguồn riêng.
          </div>}
          <Field label="Hình thức">
            <Choice name="Hình thức" value={submissionMode} disabled={!!created}
              onChange={(value) => { setSubmissionMode(value); if (value === "GROUP") setGradingMode("GRADED"); }} options={[
                { value: "INDIVIDUAL", label: "Cá nhân" },
                { value: "GROUP", label: "Nhóm", disabled: !groupAllowed, title: groupAllowed ? undefined : target.kind === "subject" ? "Bài của môn chỉ có cá nhân" : "Lớp chưa dùng được bài nhóm" },
              ]} />
          </Field>
          <Field label="Chế độ">
            <Choice name="Chế độ" value={gradingMode} onChange={setGradingMode} disabled={submissionMode === "GROUP" || !!created} options={[
              { value: "GRADED", label: "Graded" },
              { value: "PRACTICE", label: "Practice" },
            ]} />
          </Field>
        </>
      )}
    </Dialog>
  );
}
