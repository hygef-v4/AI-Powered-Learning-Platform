"use client";

import { useRef, useState } from "react";
import { Alert, Button, Dialog, Field, Icon, IconButton, Select } from "@/shared/ui";
import { assignmentsApi, type CreateInput } from "../api";
import { Choice } from "./Choice";
import { errorMessage } from "../model/format";
import { type AssignmentDetail, type CopySource } from "../model/types";

type Props = {
  /** Lớp đích (bài của lớp) hoặc môn (bài của môn). */
  target: { kind: "class"; classId: string; groupSupported: boolean } | { kind: "subject"; subjectId: string };
  onClose: () => void;
  /** Bài đã tạo (đã nhập sẵn tệp DOCX nếu người soạn chọn tệp); `startWithAi` khi bật Soạn bằng AI. */
  onCreated: (assignment: AssignmentDetail) => void;
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
      Upload đề mẫu
      <input type="file" accept=".docx" className="sr-only" aria-label="Upload đề mẫu" data-testid="create-docx" disabled={disabled}
        onChange={(event) => { onChange(event.target.files?.[0] ?? null); event.target.value = ""; }} />
    </label>
  );
}

/**
 * t21b Tạo bài tập (F3, F13) gộp một form: Upload đề mẫu DOCX (tùy chọn, nhập luôn vào khung sau khi tạo) hoặc copy bài có
 * sẵn: bài khác của cùng môn hoặc bài ở lớp khác mình dạy (đã tải tệp thì ẩn ô copy và tạo theo tệp) và Loại bài (Cá nhân, Nhóm, Luyện tập: hình thức và chế độ gộp làm một như trên bảng Bài tập). Nút xác
 * nhận chỉ ghi Tạo. Sơ đồ Draw.io luôn dùng được (yêu cầu 2026-10-21).
 */
export function CreateAssignmentDialog({ target, onClose, onCreated }: Props) {
  const [file, setFile] = useState<File | null>(null);
  const [kind, setKind] = useState<"INDIVIDUAL" | "GROUP" | "PRACTICE">("INDIVIDUAL");
  const [sources, setSources] = useState<CopySource[] | null>(null);
  const [sourceId, setSourceId] = useState("");
  // Bài đã tạo mà nhập tệp chưa được: thử lại chỉ nhập lại tệp, không tạo thêm bài.
  const [created, setCreated] = useState<AssignmentDetail | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const loadingSources = useRef(false);
  const groupAllowed = target.kind === "class" && target.groupSupported;
  const copying = !!sourceId;
  const locked = busy || !!created;

  function loadSources() {
    if (sources || loadingSources.current) return;
    loadingSources.current = true;
    (target.kind === "class" ? assignmentsApi.copySources(target.classId) : assignmentsApi.subjectCopySources(target.subjectId)).then(setSources, (e) => setError(errorMessage(e))).finally(() => { loadingSources.current = false; });
  }

  async function submit() {
    setBusy(true);
    setError(null);
    let assignment = created;
    try {
      if (!assignment) {
        if (copying) {
          assignment = target.kind === "class" ? await assignmentsApi.copy(target.classId, sourceId)
            : await assignmentsApi.copyToSubject(target.subjectId, sourceId);
        } else {
          const input: CreateInput = {
            submissionMode: kind === "GROUP" ? "GROUP" : "INDIVIDUAL",
            gradingMode: kind === "PRACTICE" ? "PRACTICE" : "GRADED",
            diagramEnabled: true,
          };
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
      onCreated(assignment);
    } catch (e) {
      setError(assignment ? `Đã tạo bài, chưa nhập tệp: ${errorMessage(e)}` : errorMessage(e));
    } finally {
      setBusy(false);
    }
  }


  return (
    <Dialog open title="Bài tập mới" size="lg" onClose={onClose} dismissible={!busy}
      footer={<>
        {created && <Button onClick={() => onCreated(created)} disabled={busy}>Mở bài</Button>}
        <Button variant="primary" onClick={() => void submit()} disabled={busy} data-testid="create-assignment-submit">
          {created ? "Thử lại" : "Tạo"}
        </Button>
      </>}>
      {error && <Alert tone="danger" role="alert">{error}</Alert>}
      <div className={`transition-opacity ${copying ? "pointer-events-none opacity-45" : ""}`}>
        <Field label="Loại">
          {/* Bài của môn chỉ có cá nhân và luyện tập nên không hiện loại Nhóm. */}
          <Choice name="Loại" value={kind} disabled={locked || copying} onChange={setKind} options={[
            { value: "INDIVIDUAL", label: "Cá nhân" },
            ...(target.kind === "subject" ? [] : [{ value: "GROUP" as const, label: "Nhóm", disabled: !groupAllowed, title: groupAllowed ? undefined : "Lớp chưa dùng được bài nhóm" }]),
            { value: "PRACTICE", label: "Luyện tập" },
          ]} />
        </Field>
      </div>
      <Field label="Mẫu đề">
        {/* Tải tệp lên thì bỏ bài đã chọn copy và ẩn ô copy: bài tạo theo tệp vừa tải. */}
        <DocxField file={file} disabled={locked} onChange={(next) => { setFile(next); if (next) setSourceId(""); }} />
        {!file && (
          <div aria-hidden className="flex items-center gap-3 text-xs font-semibold text-muted">
            <span className="h-px flex-1 bg-line" />hoặc<span className="h-px flex-1 bg-line" />
          </div>
        )}
        {!file && (
          <Select aria-label="Copy bài có sẵn" value={sourceId} disabled={locked} data-testid="copy-sources"
            onFocus={loadSources} onMouseDown={loadSources} onChange={(event) => setSourceId(event.target.value)}>
            <option value="">Copy từ bài có sẵn</option>
            {sources?.map((source) => (
              <optgroup key={source.classId} label={source.kind === "SUBJECT" ? `Cùng môn · ${source.code} · ${source.name}`
                : `Lớp ${source.code} · ${source.name}`}>
                {source.assignments.map((assignment) => <option key={assignment.id} value={assignment.id}>{assignment.title}</option>)}
              </optgroup>
            ))}
          </Select>
        )}
      </Field>
    </Dialog>
  );
}
