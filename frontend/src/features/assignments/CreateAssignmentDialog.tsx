"use client";

import { useRef, useState } from "react";
import { Alert, Button, Dialog, Field, Icon, IconButton, Select, Switch } from "@/shared/ui";
import { assignmentsApi, type CreateInput } from "./api";
import { Choice } from "./Choice";
import { errorMessage } from "./format";
import { type AssignmentDetail, type CopySource, type GradingMode, type SubmissionMode } from "./types";

type Props = {
  /** Lớp đích (bài của lớp) hoặc môn (bài của môn). */
  target: { kind: "class"; classId: string; groupSupported: boolean } | { kind: "subject"; subjectId: string };
  onClose: () => void;
  /** Bài đã tạo (đã nhập sẵn tệp DOCX nếu người soạn chọn tệp); `startWithAi` khi bật Soạn bằng AI. */
  onCreated: (assignment: AssignmentDetail, startWithAi?: boolean) => void;
};

/** Ô chọn tệp DOCX: kéo thả hoặc bấm để chọn; chọn xong chỉ hiện tên tệp và nút bỏ. */
function DocxField({ file, onChange, disabled }: { file: File | null; onChange: (file: File | null) => void; disabled?: boolean }) {
  const [over, setOver] = useState(false);
  if (file) {
    return (
      <div className="flex animate-fade items-center gap-3 rounded-[10px] border border-line bg-sunken px-3.5 py-3 text-[13.5px]" data-testid="docx-file">
        <Icon name="description" size={18} className="text-primary-ink" />
        <span className="min-w-0 flex-1 truncate font-semibold text-ink" title={file.name}>{file.name}</span>
        <IconButton icon="close" label="Bỏ tệp" size={28} disabled={disabled} onClick={() => onChange(null)} />
      </div>
    );
  }
  return (
    <label
      onDragOver={(event) => { event.preventDefault(); setOver(true); }}
      onDragLeave={() => setOver(false)}
      onDrop={(event) => {
        event.preventDefault();
        setOver(false);
        const dropped = event.dataTransfer.files[0];
        if (dropped && !disabled) onChange(dropped);
      }}
      className={`flex cursor-pointer items-center gap-3 rounded-[10px] border-[1.5px] border-dashed px-3.5 py-3.5 text-[13.5px] font-semibold transition-colors duration-200 ${
        over ? "border-primary bg-primary-soft text-ink" : "border-line-strong text-body hover:border-primary hover:bg-sunken"
      }`}
    >
      <Icon name="upload_file" size={18} className="text-primary-ink" />
      Tệp DOCX
      <input type="file" accept=".docx" className="sr-only" aria-label="Tệp DOCX" data-testid="create-docx" disabled={disabled}
        onChange={(event) => { onChange(event.target.files?.[0] ?? null); event.target.value = ""; }} />
    </label>
  );
}

/**
 * t21b Tạo bài tập (F3, F13) gộp một form: tệp DOCX (tùy chọn, nhập luôn vào khung sau khi tạo), copy bài từ lớp khác
 * (chỉ bài của lớp), hình thức, chế độ và công tắc Soạn bằng AI. Không còn tách tab theo cách soạn; nút tạo đổi nhãn
 * theo các trường đã điền. Sơ đồ Draw.io luôn dùng được (yêu cầu 2026-10-21).
 */
export function CreateAssignmentDialog({ target, onClose, onCreated }: Props) {
  const [file, setFile] = useState<File | null>(null);
  const [withAi, setWithAi] = useState(false);
  const [submissionMode, setSubmissionMode] = useState<SubmissionMode>("INDIVIDUAL");
  const [gradingMode, setGradingMode] = useState<GradingMode>("GRADED");
  const [sources, setSources] = useState<CopySource[] | null>(null);
  const [sourceId, setSourceId] = useState("");
  // Bài đã tạo mà nhập tệp chưa được: thử lại chỉ nhập lại tệp, không tạo thêm bài.
  const [created, setCreated] = useState<AssignmentDetail | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const loadingSources = useRef(false);
  const groupAllowed = target.kind === "class" && target.groupSupported;
  const classId = target.kind === "class" ? target.classId : null;
  const copying = !!sourceId;
  const locked = busy || !!created;

  function loadSources() {
    if (!classId || sources || loadingSources.current) return;
    loadingSources.current = true;
    assignmentsApi.copySources(classId).then(setSources, (e) => setError(errorMessage(e))).finally(() => { loadingSources.current = false; });
  }

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
      if (file && !copying) {
        const made = assignment;
        setCreated(made);
        const imported = await assignmentsApi.importDocx(made.id, file);
        assignment = await assignmentsApi.saveSkeleton(made.id, made.version, imported.document, { fromDocx: true });
      }
      if (withAi && !copying) onCreated(assignment, true);
      else onCreated(assignment);
    } catch (e) {
      setError(assignment ? `Đã tạo bài, chưa nhập tệp: ${errorMessage(e)}` : errorMessage(e));
    } finally {
      setBusy(false);
    }
  }

  const label = created ? "Thử lại" : copying ? "Copy bài" : withAi ? "Tạo và mở AI" : file ? "Tạo và nhập DOCX" : "Tạo bài nháp";

  return (
    <Dialog open title="Bài tập mới" size="lg" onClose={onClose} dismissible={!busy}
      footer={<>
        {created && <Button onClick={() => onCreated(created)} disabled={busy}>Mở bài</Button>}
        <Button variant="primary" onClick={() => void submit()} disabled={busy} data-testid="create-assignment-submit">
          {label}
          {!busy && <Icon name="arrow_forward" size={15} />}
        </Button>
      </>}>
      {error && <Alert tone="danger" role="alert">{error}</Alert>}
      <Field label="Nội dung ban đầu">
        <div className={`flex flex-col gap-2.5 transition-opacity ${copying ? "opacity-45" : ""}`}>
          <DocxField file={file} onChange={setFile} disabled={locked || copying} />
        </div>
        {classId && (
          <Select aria-label="Copy bài từ lớp khác" value={sourceId} disabled={locked} data-testid="copy-sources"
            onFocus={loadSources} onMouseDown={loadSources} onChange={(event) => setSourceId(event.target.value)}>
            <option value="">{sources && sources.length === 0 ? "Không có bài ở lớp khác" : "Copy từ lớp khác"}</option>
            {sources?.map((source) => (
              <optgroup key={source.classId} label={`${source.code} · ${source.name}`}>
                {source.assignments.map((assignment) => <option key={assignment.id} value={assignment.id}>{assignment.title}</option>)}
              </optgroup>
            ))}
          </Select>
        )}
      </Field>
      <div className={`grid grid-cols-1 gap-4 transition-opacity sm:grid-cols-2 ${copying ? "pointer-events-none opacity-45" : ""}`}>
        <Field label="Hình thức">
          <Choice name="Hình thức" value={submissionMode} disabled={locked || copying}
            onChange={(value) => { setSubmissionMode(value); if (value === "GROUP") setGradingMode("GRADED"); }} options={[
              { value: "INDIVIDUAL", label: "Cá nhân" },
              { value: "GROUP", label: "Nhóm", disabled: !groupAllowed, title: groupAllowed ? undefined : target.kind === "subject" ? "Bài của môn chỉ có cá nhân" : "Lớp chưa dùng được bài nhóm" },
            ]} />
        </Field>
        <Field label="Chế độ">
          <Choice name="Chế độ" value={gradingMode} onChange={setGradingMode} disabled={submissionMode === "GROUP" || locked || copying} options={[
            { value: "GRADED", label: "Tính điểm" },
            { value: "PRACTICE", label: "Luyện tập" },
          ]} />
        </Field>
      </div>
      <div className={`flex items-center justify-between border-t border-line pt-4 ${copying ? "opacity-45" : ""}`}>
        <span className="text-[13.5px] font-semibold text-ink">Soạn bằng AI</span>
        <Switch checked={withAi} onChange={setWithAi} label="" ariaLabel="Soạn bằng AI" disabled={locked || copying} />
      </div>
    </Dialog>
  );
}
