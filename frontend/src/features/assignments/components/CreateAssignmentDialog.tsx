"use client";

import { useEffect, useRef, useState } from "react";
import { getPolicies, uploadFile } from "@/features/files/api";
import { acceptAttribute, checkFile, type PurposePolicy } from "@/features/files/model/purposes";
import { AiSourcePicker, type SourceChoice } from "@/shared/ai";
import { Alert, Button, Dialog, Field, Icon, IconButton, Select, Textarea, TextInput } from "@/shared/ui";
import { AiProgress, ReferencePicker, type SourceUpload } from "./AiDraftDialog";
import { AiRequestLength, MAX_AI_REQUEST_LENGTH } from "./AiRequestLength";
import { describeAiFailure } from "@/shared/api/errorMessages";
import { SearchPicker } from "./SearchPicker";
import { TemplateFileField } from "./TemplateFileField";
import { checkImportFiles, IMPORT_ACCEPT, importKindOf } from "../model/importFiles";
import { savePendingImport } from "../model/pendingImports";
import { assignmentsApi, type CreateInput } from "../api";
import { Choice, ChoiceHint as Hint } from "./Choice";
import { describeError, parseQuestionCount, parseQuestionPoints, requestRef, requiresAiDraftReview } from "../model/format";
import { QuestionCountField } from "./QuestionCountField";
import { QuestionPointsField } from "./QuestionPointsField";
import { extractTemplateText } from "../model/templateText";
import { type AiDraft, type AssignmentDetail, type CopySource, type Material } from "../model/types";
import { msg } from "@/i18n/msg";
import { formatDate } from "@/shared/dateTime";
import { LOCALES, LOCALE_NAMES } from "@/i18n/locales";

type Props = {
  /** Lớp đích (bài của lớp) hoặc môn (đề mẫu của môn). */
  target: { kind: "class"; classId: string; groupSupported: boolean } | { kind: "subject"; subjectId: string };
  onClose: () => void;
  onCreated: (assignment: AssignmentDetail, proposalId?: string) => void;
};

/** Cách tạo đề (yêu cầu 2026-11-10): đề mới, copy đề có sẵn, hoặc để AI soạn. */
type Method = "NEW" | "EXISTING" | "AI";
type Kind = "INDIVIDUAL" | "GROUP" | "PRACTICE";

const TITLE_MAX = 200;
const POLL_MS = 2000;
const MAX_SOURCE_FILES = 3;
/** Tổng mặc định khi giảng viên dùng bố cục đề mẫu hoặc soạn đề mới. */
const AI_TARGET_POINTS = 10;

/** Một câu ngắn dưới mỗi lựa chọn (yêu cầu 2026-11-10). */
const METHOD_HINTS: Record<Method, string> = {
  NEW: msg("assignments.components.CreateAssignmentDialog.writeFromScratchYouCan"),
  EXISTING: msg("assignments.components.CreateAssignmentDialog.copyAnExistingAssignmentAnd"),
  AI: msg("assignments.components.CreateAssignmentDialog.theAiWritesTheAssignment"),
};
const KIND_HINTS: Record<Kind, string> = {
  INDIVIDUAL: msg("assignments.components.CreateAssignmentDialog.eachStudentSubmitsTheirOwn"),
  GROUP: msg("assignments.components.CreateAssignmentDialog.eachGroupSubmitsOneShared"),
  PRACTICE: msg("assignments.components.CreateAssignmentDialog.selfPracticeNotGraded"),
};


const wait = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

/**
 * t21b Tạo bài tập (F3, F13), một popup: Tiêu đề bắt buộc ở trên cùng, rồi Loại bài (Cá nhân, Nhóm, Luyện tập) dùng cho
 * cả ba cách tạo, rồi Cách tạo (yêu cầu 2026-11-10; 2026-11-12: Loại đứng trên Cách tạo; 2026-11-25: mặc định là Đề mới):
 * - Đề có sẵn: copy đề mẫu của môn hay bài ở lớp mình dạy; bài copy theo Loại đã chọn (chọn bài nguồn thì Loại nhảy theo
 *   bài đó, vẫn đổi được).
 * - Đề mới: Mẫu đề là tệp DOCX (nhập luôn vào khung), không copy.
 * - Tạo từ AI: Mẫu đề, Tài liệu tham khảo và Yêu cầu ngay trong popup này; xác nhận nguồn trước khi tạo.
 *   AI lỗi thì giữ bài đã tạo để thử lại; đóng popup thì bỏ bài.
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
  const [pickingSource, setPickingSource] = useState(false);
  const [aiTaskId, setAiTaskId] = useState<string | null>(null);
  const [aiTemplateId, setAiTemplateId] = useState("");
  const [aiTemplateFile, setAiTemplateFile] = useState<File | null>(null);
  const [preserveTemplatePoints, setPreserveTemplatePoints] = useState(false);
  const [questionCountText, setQuestionCountText] = useState("");
  const [questionPointsText, setQuestionPointsText] = useState("");
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
  const requestTooLong = request.length > MAX_AI_REQUEST_LENGTH;
  // Đề có sẵn: đề mẫu của môn đứng đầu, rồi bài của các lớp mình dạy (thứ tự server trả). Bài của lớp ghi thêm `mã lớp · học kỳ`
  // để phân biệt lớp cùng mã ở các học kỳ (D-U04-05). Ngày tạo hiện bên phải để phân biệt bài cùng tên (yêu cầu 2026-11-27).
  const candidates = (sources ?? []).flatMap((source) => source.assignments);
  const options = (sources ?? []).flatMap((source) => source.assignments.map((item) => ({
    id: item.id, label: source.kind === "CLASS" && source.term ? `${item.title} · ${source.code} · ${source.term}` : item.title,
    hint: item.createdAt ? formatDate(item.createdAt) : undefined,
  }))).filter((option) => option.id !== sourceId);
  const picked = candidates.find((item) => item.id === sourceId);
  const subjectTemplates = (sources ?? []).filter((source) => source.kind === "SUBJECT").flatMap((source) => source.assignments);
  const templateName = aiTemplateFile?.name ?? subjectTemplates.find((item) => item.id === aiTemplateId)?.title;
  const canPreserveTemplatePoints = !!aiTemplateId && !aiTemplateFile && kind !== "PRACTICE";
  const keepTemplatePoints = canPreserveTemplatePoints && preserveTemplatePoints;
  const showQuestionPoints = kind !== "PRACTICE" && !keepTemplatePoints;
  const questionPoints = showQuestionPoints ? parseQuestionPoints(questionPointsText) : [];
  const questionCount = keepTemplatePoints || questionPoints?.length ? undefined : parseQuestionCount(questionCountText);
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
      .then(setSources, (e) => setError(describeError(e)))
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

  /** Chọn bài nguồn: Loại nhảy theo bài đó (bài nhóm mà lớp chưa dùng được nhóm thì giữ loại đang chọn). */
  function pickSource(id: string) {
    setSourceId(id);
    setError(null);
    const source = candidates.find((item) => item.id === id);
    if (!source) return;
    const next: Kind = source.gradingMode === "PRACTICE" ? "PRACTICE" : source.submissionMode === "GROUP" ? "GROUP" : "INDIVIDUAL";
    if (next !== "GROUP" || groupAllowed) setKind(next);
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
      updateUpload(id, (item) => ({ ...item, status: "ERROR", error: cause instanceof Error ? cause.message : msg("assignments.components.CreateAssignmentDialog.uploadFailed") }));
    } finally {
      abortUploads.current.delete(id);
    }
  }

  function addUploads(files: FileList | null) {
    if (!files) return;
    const remaining = MAX_SOURCE_FILES - uploads.length;
    if (files.length > remaining) setError(msg("assignments.components.CreateAssignmentDialog.chooseAtMost3Source"));
    Array.from(files).slice(0, Math.max(0, remaining)).forEach((item) => void upload(item));
  }

  function removeUpload(item: SourceUpload) {
    abortUploads.current.get(item.id)?.();
    abortUploads.current.delete(item.id);
    setUploads((list) => list.filter((upload) => upload.id !== item.id));
  }

  /** AI soạn trên bài vừa tạo rồi mở đề xuất cần giảng viên duyệt. */
  async function draftWithAi(assignment: AssignmentDetail, choice: SourceChoice): Promise<void> {
    let formatText: string | undefined;
    if (aiTemplateFile) {
      const imported = await assignmentsApi.importDocx(assignment.id, aiTemplateFile);
      formatText = extractTemplateText(imported.document).text;
      if (!formatText.trim()) throw new Error(msg("assignments.components.CreateAssignmentDialog.theTemplateHasNoReadable"));
    }
    let draft: AiDraft = await assignmentsApi.requestAiDraft(assignment.id, {
      request: request.trim(),
      materialIds: chosen,
      sourceFileRefs: uploads.flatMap((item) => (item.fileRef ? [item.fileRef] : [])),
      formatText,
      templateAssignmentId: aiTemplateFile ? undefined : aiTemplateId || undefined,
      preserveTemplateQuestionPoints: keepTemplatePoints || undefined,
      source: choice.source,
      provider: choice.source === "PERSONAL" ? choice.provider ?? undefined : undefined,
      options: { profile: "AUTO", language,
        targetPoints: keepTemplatePoints || questionPoints?.length ? undefined : AI_TARGET_POINTS,
        ...(questionCount ? { questionCount } : {}),
        ...(questionPoints?.length ? { questionPoints } : {}) },
      requestRef: requestRef(),
    });
    while (draft.status === "PENDING") {
      setAiTaskId(draft.taskId);
      await wait(POLL_MS);
      if (!alive.current) throw new Error(msg("assignments.components.CreateAssignmentDialog.thePopupWasClosed"));
      draft = await assignmentsApi.aiDraft(assignment.id, draft.draftId);
    }
    if (draft.status !== "READY" || !draft.proposal) {
      throw new Error(draft.safeError ? describeAiFailure(draft.safeError)
        : msg("assignments.components.CreateAssignmentDialog.theAiReturnedNoResult"));
    }
    const validation = draft.proposal.validation;
    if (!validation || validation.status === "INVALID" || draft.proposal.sections.length === 0) {
      await assignmentsApi.discardAiDraft(assignment.id, draft.draftId).catch(() => {});
      const reason = validation?.issues.find((issue) => issue.severity === "ERROR")?.message;
      throw new Error(reason ? msg("assignments.components.CreateAssignmentDialog.theAiCouldNotProduce", { reason: String(reason) }) : msg("assignments.components.CreateAssignmentDialog.theAiCouldNotProduce2"));
    }
    if (aiTemplateId || formatText || requiresAiDraftReview(draft)) {
      onCreated(assignment, draft.draftId);
      return;
    }
    onCreated(await assignmentsApi.applyAiDraft(assignment.id, draft.draftId, assignment.version, true, false));
  }

  async function submit(choice?: SourceChoice) {
    if (ai && requestTooLong) return;
    const name = title.trim();
    if (!created && !aiCreated && !name) {
      setTitleError(msg("assignments.components.CreateAssignmentDialog.enterAnAssignmentTitle"));
      return;
    }
    if (existing && !sourceId) {
      setError(msg("assignments.components.CreateAssignmentDialog.chooseAnExistingAssignmentTo"));
      return;
    }
    if (ai && uploads.some((item) => item.status !== "READY")) {
      setError(msg("assignments.components.CreateAssignmentDialog.waitForUploadsToFinish"));
      return;
    }
    if (ai && questionPoints === null) {
      setError(msg("assignments.components.QuestionPointsField.invalid"));
      return;
    }
    if (ai && questionCount === null) {
      setError(msg("assignments.components.AiDraftDialog.theNumberOfQuestionsMust"));
      return;
    }
    if (ai && !choice) {
      setError(null);
      setPickingSource(true);
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
          const { submissionMode, gradingMode } = createInput(name);
          const modes = { submissionMode, gradingMode };
          assignment = target.kind === "class" ? await assignmentsApi.copy(target.classId, sourceId, name, modes)
            : await assignmentsApi.copyToSubject(target.subjectId, sourceId, name, modes);
        } else {
          assignment = target.kind === "class" ? await assignmentsApi.createForClass(target.classId, createInput(name))
            : await assignmentsApi.createForSubject(target.subjectId, createInput(name));
        }
      }
      if (ai && choice) {
        setAiCreated(assignment);
        setAiRunning(true);
        try {
          await draftWithAi(assignment, choice);
        } finally {
          setAiRunning(false);
          setAiTaskId(null);
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
      setError(ai ? describeError(e, msg("assignments.components.CreateAssignmentDialog.theAiReturnedNoResult"))
        : assignment ? msg("assignments.components.CreateAssignmentDialog.assignmentCreatedFileNotImported", { value: String(describeError(e)) }) : describeError(e));
    } finally {
      setBusy(false);
    }
  }

  /** Đóng popup khi AI chưa ghi được đề: bỏ bài trống vừa tạo để không sót bản nháp rỗng. */
  async function close() {
    if (aiCreated) await assignmentsApi.remove(aiCreated.id, aiCreated.version).catch(() => {});
    onClose();
  }

  if (aiRunning) return <AiProgress taskId={aiTaskId} />;
  if (pickingSource) return <AiSourcePicker open requireConfirmation task={title.trim()}
    onCancel={() => setPickingSource(false)}
    onSubmit={(choice) => { setPickingSource(false); void submit(choice); }} />;

  return (
    <Dialog open title={isClass ? msg("assignments.components.CreateAssignmentDialog.newAssignment") : msg("assignments.components.CreateAssignmentDialog.newTemplate")} size="lg" onClose={() => void close()} dismissible={!busy}
      footer={<>
        {created && <Button onClick={() => onCreated(created)} disabled={busy}>{msg("assignments.components.CreateAssignmentDialog.openAssignment")}</Button>}
        <Button variant="primary" onClick={() => void submit()} loading={busy} disabled={ai && requestTooLong} data-testid="create-assignment-submit">
          {created || aiCreated ? msg("assignments.components.CreateAssignmentDialog.retry") : msg("assignments.components.CreateAssignmentDialog.create")}
        </Button>
      </>}>
      {error && <Alert tone="danger" role="alert">{error}</Alert>}
      <Field label={msg("assignments.components.CreateAssignmentDialog.title")} htmlFor="create-assignment-title" required error={titleError}>
        <TextInput id="create-assignment-title" value={title} maxLength={TITLE_MAX} disabled={setupLocked} invalid={!!titleError}
          data-testid="create-assignment-title"
          onChange={(e) => { setTitle(e.target.value); if (e.target.value.trim()) setTitleError(null); }} />
      </Field>
      <Field label={msg("assignments.components.CreateAssignmentDialog.type")}>
        <Choice name={msg("assignments.components.CreateAssignmentDialog.type")} value={kind} disabled={setupLocked} onChange={setKind} options={[
          { value: "INDIVIDUAL", label: msg("assignments.components.CreateAssignmentDialog.individual") },
          { value: "GROUP", label: msg("assignments.components.CreateAssignmentDialog.group"), disabled: !groupAllowed, title: groupAllowed ? undefined : msg("assignments.components.CreateAssignmentDialog.thisClassCannotUseGroup") },
          { value: "PRACTICE", label: msg("assignments.components.CreateAssignmentDialog.practice") },
        ]} />
        <Hint key={kind}>{KIND_HINTS[kind]}</Hint>
      </Field>
      <Field label={msg("assignments.components.CreateAssignmentDialog.howToCreate")}>
        <Choice name={msg("assignments.components.CreateAssignmentDialog.howToCreate")} value={method} disabled={setupLocked} onChange={changeMethod} options={[
          { value: "EXISTING", label: msg("assignments.components.CreateAssignmentDialog.existingAssignment") },
          { value: "NEW", label: msg("assignments.components.CreateAssignmentDialog.newAssignment2") },
          { value: "AI", label: msg("assignments.components.CreateAssignmentDialog.createWithAi") },
        ]} />
        <Hint key={method}>{METHOD_HINTS[method]}</Hint>
      </Field>
      {existing ? (
        <Field label={msg("assignments.components.CreateAssignmentDialog.existingAssignment")} required>
          <SearchPicker id="copy-sources" label={msg("assignments.components.CreateAssignmentDialog.existingAssignment")} placeholder={msg("assignments.components.CreateAssignmentDialog.searchByAssignmentName")} disabled={locked}
            optionIcon="assignment" loading={sources === null} emptyText={msg("assignments.components.CreateAssignmentDialog.noAssignmentsToCopyYet")} onOpen={loadSources}
            options={options} onPick={pickSource} />
          {picked && (
            <div className="flex animate-fade items-center gap-3 rounded-[10px] border border-line bg-sunken px-3.5 py-3 text-[13.5px]"
              data-testid="copy-source">
              <Icon name="assignment" size={18} className="text-primary-ink" />
              <span className="min-w-0 flex-1 truncate font-semibold text-ink" title={picked.title}>{picked.title}</span>
              <IconButton icon="close" label={msg("assignments.components.CreateAssignmentDialog.clearSelectedAssignment")} size={28} disabled={locked} onClick={() => setSourceId("")} />
            </div>
          )}
        </Field>
      ) : (
        <>
          {method === "NEW" && (
            <Field label={msg("assignments.components.CreateAssignmentDialog.template")}>
              <TemplateFileField file={file} files={templateFiles} disabled={locked} accept={IMPORT_ACCEPT}
                label={msg("assignments.components.CreateAssignmentDialog.uploadAnOldAssignmentAs")} onChange={(next) => setTemplateFiles(next ? [next] : [])}
                onFilesChange={setTemplateFiles} />
            </Field>
          )}
          {ai && (
            <>
              {/* Mẫu đề, Tài liệu tham khảo, Yêu cầu: mỗi ô một dòng (yêu cầu 2026-11-10). */}
              {/* Ngôn ngữ bên trái Mẫu đề trên cùng một dòng (yêu cầu 2026-11-10). */}
              <div className="grid gap-3 sm:grid-cols-[9rem_minmax(0,1fr)]">
                <Field label={msg("assignments.components.CreateAssignmentDialog.language")} htmlFor="ai-language">
                  <Select id="ai-language" aria-label={msg("assignments.components.CreateAssignmentDialog.language")} value={language} disabled={busy}
                    onChange={(e) => setLanguage(e.target.value as "VI" | "EN")}>
                    {LOCALES.map((code) => <option key={code} value={code.toUpperCase()} lang={code}>{LOCALE_NAMES[code]}</option>)}
                  </Select>
                </Field>
                {/* Mẫu đề như ô copy bài cũ: tìm đề mẫu của môn, dòng cuối là tải DOCX lên; đã chọn thì tên hiện ngay trong ô. */}
                <Field label={msg("assignments.components.CreateAssignmentDialog.template")} htmlFor="ai-template">
                  <SearchPicker id="ai-template" label={msg("assignments.components.CreateAssignmentDialog.template")} placeholder={msg("assignments.components.CreateAssignmentDialog.chooseATemplateOrUpload")} disabled={busy}
                    optionIcon="assignment" loading={sources === null} emptyText={msg("assignments.components.CreateAssignmentDialog.theSubjectHasNoTemplates")} onOpen={loadSources}
                    options={subjectTemplates.filter((item) => item.id !== aiTemplateId).map((item) => ({ id: item.id, label: item.title, hint: item.createdAt ? formatDate(item.createdAt) : undefined }))}
                    onPick={(id) => { setAiTemplateFile(null); setAiTemplateId(id); setPreserveTemplatePoints(false); }}
                    selected={templateName ? { label: templateName, clearLabel: msg("assignments.components.CreateAssignmentDialog.removeTemplate"),
                      onClear: () => { setAiTemplateId(""); setAiTemplateFile(null); setPreserveTemplatePoints(false); } } : undefined}
                    footer={{ label: msg("assignments.components.CreateAssignmentDialog.uploadATemplate"), icon: "upload_file", onPick: () => templateInput.current?.click() }} />
                  <input ref={templateInput} type="file" accept=".docx" hidden aria-label={msg("assignments.components.CreateAssignmentDialog.uploadATemplate")} data-testid="ai-format"
                    onChange={(event) => {
                      const templateFile = event.target.files?.[0];
                      event.target.value = "";
                      if (!templateFile) return;
                      setAiTemplateId("");
                      setAiTemplateFile(templateFile);
                      setPreserveTemplatePoints(false);
                    }} />
                </Field>
              </div>
              {canPreserveTemplatePoints && (
                <label className="flex items-center gap-2 text-sm">
                  <input type="checkbox" checked={keepTemplatePoints} disabled={busy}
                    onChange={(event) => setPreserveTemplatePoints(event.target.checked)}
                    data-testid="create-assignment-preserve-template-points" />
                  {msg("assignments.components.CreateAssignmentDialog.preserveTemplatePoints")}
                </label>
              )}
              {!keepTemplatePoints && (
                <div className="grid gap-3 sm:grid-cols-2">
                  <QuestionCountField id="create-ai-question-count"
                    value={questionPoints?.length ? String(questionPoints.length) : questionCountText}
                    onChange={setQuestionCountText} invalid={questionCount === null}
                    disabled={busy || Boolean(questionPoints?.length)} />
                  {showQuestionPoints && (
                    <QuestionPointsField id="create-ai-question-points" value={questionPointsText}
                      onChange={setQuestionPointsText} points={questionPoints} disabled={busy} />
                  )}
                </div>
              )}
              <Field label={msg("assignments.components.CreateAssignmentDialog.references")} htmlFor="ai-reference">
                <ReferencePicker materials={materials ?? []} chosen={chosen} uploads={uploads} disabled={busy}
                  canUpload={!!sourcePolicy && uploads.length < MAX_SOURCE_FILES}
                  accept={sourcePolicy ? acceptAttribute(sourcePolicy) : undefined}
                  onChoose={(id) => setChosen((list) => (list.includes(id) ? list : [...list, id].slice(0, 10)))}
                  onUnchoose={(id) => setChosen((list) => list.filter((item) => item !== id))}
                  onUpload={addUploads} onRemoveUpload={removeUpload} onRetryUpload={(item) => void upload(item.file, item.id)} />
              </Field>
              <Field label={msg("assignments.components.CreateAssignmentDialog.request")} htmlFor="ai-request">
                <Textarea id="ai-request" rows={2} value={request} disabled={busy}
                  invalid={requestTooLong} aria-describedby="create-ai-request-length"
                  placeholder={msg("assignments.components.CreateAssignmentDialog.describeTheAssignmentYouNeed")}
                  onChange={(e) => setRequest(e.target.value)} />
                <AiRequestLength id="create-ai-request-length" length={request.length} />
              </Field>
            </>
          )}
        </>
      )}
    </Dialog>
  );
}
