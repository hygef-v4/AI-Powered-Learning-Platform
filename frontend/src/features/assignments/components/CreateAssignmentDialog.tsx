"use client";

import { useRef, useState } from "react";
import { Alert, Button, Dialog, Field, Icon, IconButton } from "@/shared/ui";
import { SearchPicker } from "./SearchPicker";
import { TemplateFileField } from "./TemplateFileField";
import { assignmentsApi, type CreateInput } from "../api";
import { Choice } from "./Choice";
import { errorMessage } from "../model/format";
import { rememberTemplate } from "../model/templateStore";
import { type AssignmentDetail, type CopySource } from "../model/types";

type Props = {
  /** Lớp đích (bài của lớp) hoặc môn (bài của môn). */
  target: { kind: "class"; classId: string; groupSupported: boolean } | { kind: "subject"; subjectId: string };
  onClose: () => void;
  /** Bài đã tạo (đã nhập sẵn tệp DOCX nếu người soạn chọn tệp); `startWithAi` khi bật Soạn bằng AI. */
  onCreated: (assignment: AssignmentDetail) => void;
};

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
  const picked = sources?.flatMap((source) => source.assignments).find((item) => item.id === sourceId);
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
        // Đề mẫu vừa tải là Mẫu đề mặc định khi soạn bằng AI (copy từ bài có sẵn thì không).
        await rememberTemplate(made.id, file).catch(() => {});
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
        <TemplateFileField file={file} disabled={locked} onChange={(next) => { setFile(next); if (next) setSourceId(""); }} />
        {!file && (
          <div aria-hidden className="flex items-center gap-3 text-xs font-semibold text-muted">
            <span className="h-px flex-1 bg-line" />hoặc<span className="h-px flex-1 bg-line" />
          </div>
        )}
        {!file && (
          <SearchPicker id="copy-sources" label="Copy bài có sẵn" placeholder="Copy từ bài có sẵn" disabled={locked}
            optionIcon="assignment" loading={sources === null} emptyText="Không có bài phù hợp." onOpen={loadSources}
            options={(sources ?? []).flatMap((source) => source.assignments).filter((item) => item.id !== sourceId)
              .map((item) => ({ id: item.id, label: item.title }))}
            onPick={setSourceId} />
        )}
        {!file && picked && (
          <div className="flex animate-fade items-center gap-3 rounded-[10px] border border-line bg-sunken px-3.5 py-3 text-[13.5px]"
            data-testid="copy-source">
            <Icon name="assignment" size={18} className="text-primary-ink" />
            <span className="min-w-0 flex-1 truncate font-semibold text-ink" title={picked.title}>{picked.title}</span>
            <IconButton icon="close" label="Bỏ bài đã chọn" size={28} disabled={locked} onClick={() => setSourceId("")} />
          </div>
        )}
      </Field>
    </Dialog>
  );
}
