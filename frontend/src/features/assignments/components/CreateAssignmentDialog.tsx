"use client";

import { useEffect, useRef, useState } from "react";
import { getPolicies, uploadFile } from "@/features/files/api";
import { acceptAttribute, checkFile, type PurposePolicy } from "@/features/files/model/purposes";
import { Alert, Button, Dialog, Field, Icon, IconButton, Select, TextInput } from "@/shared/ui";
import { AiProgress, ReferencePicker, type SourceUpload } from "./AiDraftDialog";
import { SearchPicker } from "./SearchPicker";
import { TemplateFileField } from "./TemplateFileField";
import { checkImportFiles, IMPORT_ACCEPT, importKindOf } from "../model/importFiles";
import { savePendingImport } from "../model/pendingImports";
import { assignmentsApi, type CreateInput } from "../api";
import { Choice, ChoiceHint as Hint } from "./Choice";
import { errorMessage, requestRef } from "../model/format";
import { templateText } from "../model/templateText";
import { type AiDraft, type AssignmentDetail, type CopySource, type Material } from "../model/types";

type Props = {
  /** Lớp đích (bài của lớp) hoặc môn (đề mẫu của môn). */
  target: { kind: "class"; classId: string; groupSupported: boolean } | { kind: "subject"; subjectId: string };
  onClose: () => void;
  /** Bài đã tạo (đã nhập sẵn tệp DOCX, hoặc đã ghi đề AI lên trang). */
  onCreated: (assignment: AssignmentDetail) => void;
};

/** Cách tạo đề (yêu cầu 2026-11-10): đề mới, copy đề có sẵn, hoặc để AI soạn. */
type Method = "NEW" | "EXISTING" | "AI";
type Kind = "INDIVIDUAL" | "GROUP" | "PRACTICE";

const TITLE_MAX = 200;
const POLL_MS = 2000;
const MAX_SOURCE_FILES = 3;
/** Đề AI luôn tổng 10 điểm (yêu cầu 2026-11-10); số câu để AI tự quyết cho popup gọn. */
const AI_TARGET_POINTS = 10;

/** Một câu ngắn dưới mỗi lựa chọn (yêu cầu 2026-11-10). */
const METHOD_HINTS: Record<Method, string> = {
  NEW: "Soạn từ đầu, có thể tải DOCX, ảnh hoặc PDF đề cũ làm khung.",
  EXISTING: "Copy một đề có sẵn rồi sửa tiếp.",
  AI: "AI soạn đề kèm rubric, tổng 10 điểm.",
};
const KIND_HINTS: Record<Kind, string> = {
  INDIVIDUAL: "Mỗi sinh viên nộp một bài.",
  GROUP: "Mỗi nhóm nộp chung một bài.",
  PRACTICE: "Tự luyện, không tính điểm.",
};


const wait = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

/**
 * t21b Tạo bài tập (F3, F13), một popup: Tiêu đề bắt buộc ở trên cùng, rồi Cách tạo (yêu cầu 2026-11-10):
 * - Đề mới: Loại bài (Cá nhân, Nhóm, Luyện tập) và Mẫu đề là tệp DOCX (nhập luôn vào khung), không copy.
 * - Đề có sẵn: copy đề mẫu của môn hay bài ở lớp mình dạy (bài copy giữ loại của bài nguồn).
 * - Tạo từ AI: Loại, Mẫu đề, Tài liệu tham khảo và Yêu cầu ngay trong popup này; bấm Tạo thì tạo bài, AI soạn rồi ghi đề
 *   lên trang. AI chỉ có ở bước tạo, không có trong trang soạn. AI lỗi thì giữ bài đã tạo để thử lại; đóng popup thì bỏ bài.
 * Bài của môn chỉ là đề mẫu, không giao cho lớp.
 */
export function CreateAssignmentDialog({ target, onClose, onCreated }: Props) {
  const [title, setTitle] = useState("");
  const [titleError, setTitleError] = useState<string | null>(null);
  const [method, setMethod] = useState<Method>("NEW");
  /** Mẫu đề của Đề mới: 1 DOCX (nhập ngay), hoặc ảnh/PDF đề cũ (trang soạn bài nhờ AI đọc, BR-U07-87). */
  const [templateFiles, setTemplateFiles] = useState<File[]>([]);
  const file = templateFiles[0] ?? null;
  const [kind, setKind] = useState<Kind>("INDIVIDUAL");
  const [sources, setSources] = useState<CopySource[] | null>(null);
  const [sourceId, setSourceId] = useState("");
  // Bài đã tạo mà nhập tệp chưa được: thử lại chỉ nhập lại tệp, không tạo thêm bài.
  const [created, setCreated] = useState<AssignmentDetail | null>(null);
  // Tạo từ AI: bài đã tạo mà AI chưa ghi được đề; thử lại dùng lại bài này.
  const [aiCreated, setAiCreated] = useState<AssignmentDetail | null>(null);
  const [aiRunning, setAiRunning] = useState(false);
  const [aiTemplateId, setAiTemplateId] = useState("");
  const [aiTemplateFile, setAiTemplateFile] = useState<File | null>(null);
  const [materials, setMaterials] = useState<Material[] | null>(null);
  const [chosen, setChosen] = useState<string[]>([]);
  const [uploads, setUploads] = useState<SourceUpload[]>([]);
  const [sourcePolicy, setSourcePolicy] = useState<PurposePolicy | null>(null);
  const [request, setRequest] = useState("");
  /** Ngôn ngữ của đề AI, ưu tiên hơn ngôn ngữ ghi trong yêu cầu, mẫu đề hay tài liệu (yêu cầu 2026-11-10). */
  const [language, setLanguage] = useState<"VI" | "EN">("EN");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const loadingSources = useRef(false);
  const templateInput = useRef<HTMLInputElement>(null);
  const abortUploads = useRef(new Map<string, () => void>());
  const alive = useRef(true);
  const isClass = target.kind === "class";
  // Đề mẫu của môn có cả bài nhóm (yêu cầu 2026-11-09); ở lớp cần lớp dùng được bài nhóm.
  const groupAllowed = !isClass || target.groupSupported;
  const existing = method === "EXISTING";
  const ai = method === "AI";
  // Đề có sẵn: đề mẫu của môn đứng đầu, rồi bài của các lớp mình dạy (thứ tự server trả).
  const candidates = (sources ?? []).flatMap((source) => source.assignments);
  const options = candidates.filter((item) => item.id !== sourceId).map((item) => ({ id: item.id, label: item.title }));
  const picked = candidates.find((item) => item.id === sourceId);
  const subjectTemplates = (sources ?? []).filter((source) => source.kind === "SUBJECT").flatMap((source) => source.assignments);
  const templateName = aiTemplateFile?.name ?? subjectTemplates.find((item) => item.id === aiTemplateId)?.title;
  const locked = busy || !!created;
  // Bài đã tạo cho AI: không đổi tiêu đề, cách tạo, loại nữa; vẫn sửa được mẫu đề, tài liệu, yêu cầu để thử lại.
  const setupLocked = locked || !!aiCreated;

  useEffect(() => {
    const pending = abortUploads.current;
    alive.current = true;
    return () => {
      alive.current = false;
      pending.forEach((abort) => abort());
      pending.clear();
    };
  }, []);

  function loadSources() {
    if (sources || loadingSources.current) return;
    loadingSources.current = true;
    (target.kind === "class" ? assignmentsApi.copySources(target.classId) : assignmentsApi.subjectCopySources(target.subjectId))
      .then(setSources, (e) => setError(errorMessage(e)))
      .finally(() => { loadingSources.current = false; });
  }

  /** Lần đầu chọn Tạo từ AI: tải đề mẫu của môn, học liệu và giới hạn tệp nguồn. */
  function loadAiInputs() {
    loadSources();
    if (materials === null) {
      (target.kind === "class" ? assignmentsApi.classMaterials(target.classId) : assignmentsApi.subjectMaterials(target.subjectId))
        .then((list) => setMaterials(list.filter((m) => m.hasText)), () => setMaterials([]));
    }
    if (!sourcePolicy) {
      getPolicies().then((policies) => setSourcePolicy(policies.find((p) => p.purpose === "ASSIGNMENT_SOURCE") ?? null), () => {});
    }
  }

  function changeMethod(next: Method) {
    setMethod(next);
    setSourceId("");
    setTemplateFiles([]);
    setError(null);
    if (next === "AI") loadAiInputs();
  }

  function createInput(name: string): CreateInput {
    return {
      title: name,
      submissionMode: kind === "GROUP" ? "GROUP" : "INDIVIDUAL",
      gradingMode: kind === "PRACTICE" ? "PRACTICE" : "GRADED",
      diagramEnabled: true,
    };
  }

  function updateUpload(id: string, update: (item: SourceUpload) => SourceUpload) {
    setUploads((list) => list.map((item) => (item.id === id ? update(item) : item)));
  }

  /** Tệp nguồn tải lên trước khi có bài: server đọc nội dung khi AI soạn. */
  async function upload(source: File, id = crypto.randomUUID()) {
    if (!sourcePolicy) return;
    const issue = checkFile(source, sourcePolicy);
    const start: SourceUpload = issue ? { id, file: source, status: "ERROR", percent: 0, error: issue }
      : { id, file: source, status: "UPLOADING", percent: 0 };
    setUploads((list) => (list.some((item) => item.id === id) ? list.map((item) => (item.id === id ? start : item)) : [...list, start]));
    if (issue) return;
    const handle = uploadFile(source, "ASSIGNMENT_SOURCE", (percent) => updateUpload(id, (item) => ({ ...item, percent })));
    abortUploads.current.set(id, handle.abort);
    try {
      const result = await handle.promise;
      updateUpload(id, (item) => ({ ...item, status: "READY", percent: 100, fileRef: result.fileRef.value }));
    } catch (cause) {
      updateUpload(id, (item) => ({ ...item, status: "ERROR", error: cause instanceof Error ? cause.message : "Tải tệp lên không thành công" }));
    } finally {
      abortUploads.current.delete(id);
    }
  }

  function addUploads(files: FileList | null) {
    if (!files) return;
    const remaining = MAX_SOURCE_FILES - uploads.length;
    if (files.length > remaining) setError("Chỉ chọn tối đa 3 tệp nguồn.");
    Array.from(files).slice(0, Math.max(0, remaining)).forEach((item) => void upload(item));
  }

  function removeUpload(item: SourceUpload) {
    abortUploads.current.get(item.id)?.();
    abortUploads.current.delete(item.id);
    setUploads((list) => list.filter((upload) => upload.id !== item.id));
  }

  /** AI soạn trên bài vừa tạo, chờ xong rồi ghi đề lên trang; đề lỗi thì bỏ bản nháp và báo lý do. */
  async function draftWithAi(assignment: AssignmentDetail): Promise<AssignmentDetail> {
    let formatText: string | undefined;
    if (aiTemplateFile) {
      const imported = await assignmentsApi.importDocx(assignment.id, aiTemplateFile);
      formatText = templateText(imported.document).text;
      if (!formatText.trim()) throw new Error("Mẫu đề không có nội dung đọc được.");
    }
    let draft: AiDraft = await assignmentsApi.requestAiDraft(assignment.id, {
      request: request.trim(),
      materialIds: chosen,
      sourceFileRefs: uploads.flatMap((item) => (item.fileRef ? [item.fileRef] : [])),
      formatText,
      templateAssignmentId: aiTemplateFile ? undefined : aiTemplateId || undefined,
      options: { profile: "AUTO", language, targetPoints: AI_TARGET_POINTS },
      requestRef: requestRef(),
    });
    while (draft.status === "PENDING") {
      await wait(POLL_MS);
      if (!alive.current) throw new Error("Đã đóng popup.");
      draft = await assignmentsApi.aiDraft(assignment.id, draft.draftId);
    }
    if (draft.status !== "READY" || !draft.proposal) throw new Error(draft.safeError ?? "AI không trả kết quả.");
    const validation = draft.proposal.validation;
    if (!validation || validation.status === "INVALID" || draft.proposal.sections.length === 0) {
      await assignmentsApi.discardAiDraft(assignment.id, draft.draftId).catch(() => {});
      const reason = validation?.issues.find((issue) => issue.severity === "ERROR")?.message;
      throw new Error(reason ? `AI chưa soạn được đề hợp lệ: ${reason}` : "AI chưa soạn được đề hợp lệ.");
    }
    return assignmentsApi.applyAiDraft(assignment.id, draft.draftId, assignment.version, true, false);
  }

  async function submit() {
    const name = title.trim();
    if (!created && !aiCreated && !name) {
      setTitleError("Nhập tiêu đề bài tập.");
      return;
    }
    if (existing && !sourceId) {
      setError("Chọn một đề có sẵn để copy.");
      return;
    }
    if (ai && uploads.some((item) => item.status !== "READY")) {
      setError("Hãy đợi tải xong hoặc gỡ các tệp bị lỗi.");
      return;
    }
    if (method === "NEW" && file && importKindOf(file) !== "DOCX") {
      const issue = checkImportFiles(templateFiles, null);
      if (issue) {
        setError(issue);
        return;
      }
    }
    setBusy(true);
    setError(null);
    let assignment = created ?? aiCreated;
    try {
      if (!assignment) {
        if (existing) {
          // Tiêu đề không lấy theo bài nguồn: dùng tiêu đề người dùng nhập (yêu cầu 2026-11-09).
          assignment = target.kind === "class" ? await assignmentsApi.copy(target.classId, sourceId, name)
            : await assignmentsApi.copyToSubject(target.subjectId, sourceId, name);
        } else {
          assignment = target.kind === "class" ? await assignmentsApi.createForClass(target.classId, createInput(name))
            : await assignmentsApi.createForSubject(target.subjectId, createInput(name));
        }
      }
      if (ai) {
        setAiCreated(assignment);
        setAiRunning(true);
        try {
          onCreated(await draftWithAi(assignment));
        } finally {
          setAiRunning(false);
        }
        return;
      }
      if (file && importKindOf(file) !== "DOCX") {
        // Ảnh/PDF: trang soạn bài mở popup nhập đề và nhờ AI đọc ngay (BR-U07-87).
        savePendingImport(assignment.id, templateFiles);
      } else if (file) {
        const made = assignment;
        setCreated(made);
        const imported = await assignmentsApi.importDocx(made.id, file);
        assignment = await assignmentsApi.saveSkeleton(made.id, made.version, imported.document, { fromDocx: true });
      }
      onCreated(assignment);
    } catch (e) {
      setError(ai ? errorMessage(e, "AI không trả kết quả.")
        : assignment ? `Đã tạo bài, chưa nhập tệp: ${errorMessage(e)}` : errorMessage(e));
    } finally {
      setBusy(false);
    }
  }

  /** Đóng popup khi AI chưa ghi được đề: bỏ bài trống vừa tạo để không sót bản nháp rỗng. */
  async function close() {
    if (aiCreated) await assignmentsApi.remove(aiCreated.id, aiCreated.version).catch(() => {});
    onClose();
  }

  if (aiRunning) return <AiProgress />;

  return (
    <Dialog open title={isClass ? "Bài tập mới" : "Đề mẫu mới"} size="lg" onClose={() => void close()} dismissible={!busy}
      footer={<>
        {created && <Button onClick={() => onCreated(created)} disabled={busy}>Mở bài</Button>}
        <Button variant="primary" onClick={() => void submit()} disabled={busy} data-testid="create-assignment-submit">
          {created || aiCreated ? "Thử lại" : "Tạo"}
        </Button>
      </>}>
      {error && <Alert tone="danger" role="alert">{error}</Alert>}
      <Field label="Tiêu đề" htmlFor="create-assignment-title" required error={titleError}>
        <TextInput id="create-assignment-title" value={title} maxLength={TITLE_MAX} disabled={setupLocked} invalid={!!titleError}
          data-testid="create-assignment-title"
          onChange={(e) => { setTitle(e.target.value); if (e.target.value.trim()) setTitleError(null); }} />
      </Field>
      <Field label="Cách tạo">
        <Choice name="Cách tạo" value={method} disabled={setupLocked} onChange={changeMethod} options={[
          { value: "NEW", label: "Đề mới" },
          { value: "EXISTING", label: "Đề có sẵn" },
          { value: "AI", label: "Tạo từ AI" },
        ]} />
        <Hint key={method}>{METHOD_HINTS[method]}</Hint>
      </Field>
      {existing ? (
        <Field label="Đề có sẵn" required>
          <SearchPicker id="copy-sources" label="Đề có sẵn" placeholder="Tìm theo tên bài" disabled={locked}
            optionIcon="assignment" loading={sources === null} emptyText="Chưa có đề nào để copy." onOpen={loadSources}
            options={options} onPick={(id) => { setSourceId(id); setError(null); }} />
          {picked && (
            <div className="flex animate-fade items-center gap-3 rounded-[10px] border border-line bg-sunken px-3.5 py-3 text-[13.5px]"
              data-testid="copy-source">
              <Icon name="assignment" size={18} className="text-primary-ink" />
              <span className="min-w-0 flex-1 truncate font-semibold text-ink" title={picked.title}>{picked.title}</span>
              <IconButton icon="close" label="Bỏ bài đã chọn" size={28} disabled={locked} onClick={() => setSourceId("")} />
            </div>
          )}
        </Field>
      ) : (
        <>
          <Field label="Loại">
            <Choice name="Loại" value={kind} disabled={setupLocked} onChange={setKind} options={[
              { value: "INDIVIDUAL", label: "Cá nhân" },
              { value: "GROUP", label: "Nhóm", disabled: !groupAllowed, title: groupAllowed ? undefined : "Lớp chưa dùng được bài nhóm" },
              { value: "PRACTICE", label: "Luyện tập" },
            ]} />
            <Hint key={kind}>{KIND_HINTS[kind]}</Hint>
          </Field>
          {method === "NEW" && (
            <Field label="Mẫu đề">
              <TemplateFileField file={file} files={templateFiles} disabled={locked} accept={IMPORT_ACCEPT}
                label="Tải DOCX, ảnh hoặc PDF đề cũ" onChange={(next) => setTemplateFiles(next ? [next] : [])}
                onFilesChange={setTemplateFiles} />
            </Field>
          )}
          {ai && (
            <>
              {/* Mẫu đề, Tài liệu tham khảo, Yêu cầu: mỗi ô một dòng (yêu cầu 2026-11-10). */}
              {/* Ngôn ngữ bên trái Mẫu đề trên cùng một dòng (yêu cầu 2026-11-10). */}
              <div className="grid gap-3 sm:grid-cols-[9rem_minmax(0,1fr)]">
                <Field label="Ngôn ngữ" htmlFor="ai-language">
                  <Select id="ai-language" aria-label="Ngôn ngữ" value={language} disabled={busy}
                    onChange={(e) => setLanguage(e.target.value as "VI" | "EN")}>
                    <option value="EN">English</option>
                    <option value="VI">Tiếng Việt</option>
                  </Select>
                </Field>
                {/* Mẫu đề như ô copy bài cũ: tìm đề mẫu của môn, dòng cuối là tải DOCX lên; đã chọn thì tên hiện ngay trong ô. */}
                <Field label="Mẫu đề" htmlFor="ai-template">
                  <SearchPicker id="ai-template" label="Mẫu đề" placeholder="Chọn đề mẫu hoặc tải DOCX" disabled={busy}
                    optionIcon="assignment" loading={sources === null} emptyText="Môn chưa có đề mẫu." onOpen={loadSources}
                    options={subjectTemplates.filter((item) => item.id !== aiTemplateId).map((item) => ({ id: item.id, label: item.title }))}
                    onPick={(id) => { setAiTemplateFile(null); setAiTemplateId(id); }}
                    selected={templateName ? { label: templateName, clearLabel: "Bỏ mẫu đề",
                      onClear: () => { setAiTemplateId(""); setAiTemplateFile(null); } } : undefined}
                    footer={{ label: "Tải đề mẫu lên", icon: "upload_file", onPick: () => templateInput.current?.click() }} />
                  <input ref={templateInput} type="file" accept=".docx" hidden aria-label="Tải đề mẫu lên" data-testid="ai-format"
                    onChange={(event) => {
                      const templateFile = event.target.files?.[0];
                      event.target.value = "";
                      if (!templateFile) return;
                      setAiTemplateId("");
                      setAiTemplateFile(templateFile);
                    }} />
                </Field>
              </div>
              <Field label="Tài liệu tham khảo" htmlFor="ai-reference">
                <ReferencePicker materials={materials ?? []} chosen={chosen} uploads={uploads} disabled={busy}
                  canUpload={!!sourcePolicy && uploads.length < MAX_SOURCE_FILES}
                  accept={sourcePolicy ? acceptAttribute(sourcePolicy) : undefined}
                  onChoose={(id) => setChosen((list) => (list.includes(id) ? list : [...list, id].slice(0, 10)))}
                  onUnchoose={(id) => setChosen((list) => list.filter((item) => item !== id))}
                  onUpload={addUploads} onRemoveUpload={removeUpload} onRetryUpload={(item) => void upload(item.file, item.id)} />
              </Field>
              <Field label="Yêu cầu" htmlFor="ai-request">
                <textarea id="ai-request" rows={2} maxLength={4000} value={request} disabled={busy}
                  placeholder="Mô tả đề bạn cần: chủ đề, nội dung, độ khó…"
                  onChange={(e) => setRequest(e.target.value)}
                  className="w-full resize-y rounded-lg border border-field px-3 py-2 text-[13px] outline-none placeholder:text-muted focus:outline-2 focus:outline-focus" />
              </Field>
            </>
          )}
        </>
      )}
    </Dialog>
  );
}
