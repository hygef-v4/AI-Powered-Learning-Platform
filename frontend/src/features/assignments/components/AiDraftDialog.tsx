"use client";

import { useEffect, useRef, useState } from "react";
import { AiSourcePicker, AiTaskStatus, type SourceChoice } from "@/shared/ai";
import { Alert, BusyOverlay, Button, Dialog, Field, FileDropZone, Icon, IconButton, Pill, Select, showErrorToast, Textarea } from "@/shared/ui";
import { assignmentsApi } from "../api";
import { AiDraftPreview } from "./AiDraftPreview";
import { SearchPicker } from "./SearchPicker";
import { describeError, parseQuestionCount, parseQuestionPoints, requestRef, requiresAiDraftReview } from "../model/format";
import { AiRequestLength, MAX_AI_REQUEST_LENGTH } from "./AiRequestLength";
import { describeAiFailure } from "@/shared/api/errorMessages";
import { QuestionCountField } from "./QuestionCountField";
import { QuestionPointsField } from "./QuestionPointsField";
import { MAX_SOURCE_FILES, readTemplateFile, useSourceUploads, type SourceUpload, type TemplateFile } from "./sourceUploads";
import type { AiDraft, GradingMode, Material } from "../model/types";

export type { SourceUpload };
import { SourceImagePreview } from "./SourceImagePreview";
import { msg } from "@/i18n/msg";
import { LOCALES, LOCALE_NAMES } from "@/i18n/locales";
type Template = TemplateFile;

type Props = {
  assignmentId: string;
  initialDraftId?: string;
  version: number;
  currentPartCount: number;
  currentTitle: string;
  currentInstructions: string;
  gradingMode: GradingMode;
  prepareApply?: () => Promise<number>;
  onClose: () => void;
  onApplied: () => void;
};

const POLL_MS = 2000;
/** Tổng điểm mặc định của đề AI (yêu cầu 2026-11-10); AI chia điểm rubric cho đủ con số này. */
const DEFAULT_TARGET_POINTS = "10";

/**
 * Tạo và xem lại bản nháp AI trước khi áp dụng vào đề.
 */
export function AiDraftDialog({
  assignmentId,
  initialDraftId,
  version,
  currentPartCount,
  currentTitle,
  currentInstructions,
  gradingMode,
  prepareApply,
  onClose,
  onApplied,
}: Props) {
  const [request, setRequest] = useState("");
  const requestTooLong = request.length > MAX_AI_REQUEST_LENGTH;
  const [language, setLanguage] = useState<"VI" | "EN">("EN");
  const [questionCount, setQuestionCount] = useState("");
  const [targetPoints, setTargetPoints] = useState(DEFAULT_TARGET_POINTS);
  const [questionPointsText, setQuestionPointsText] = useState("");
  const [materials, setMaterials] = useState<Material[]>([]);
  const [chosen, setChosen] = useState<string[]>([]);
  const [template, setTemplate] = useState<Template | null>(null);
  /** Đề mẫu chọn từ bài của môn (yêu cầu 2026-11-09); tệp tải lên thay thế lựa chọn này. */
  const [templateId, setTemplateId] = useState("");
  const [templates, setTemplates] = useState<{ assignmentId: string; title: string }[]>([]);
  const templateInput = useRef<HTMLInputElement>(null);

  const [draft, setDraft] = useState<AiDraft | null>(null);
  const [pickingSource, setPickingSource] = useState(false);
  const [busy, setBusy] = useState(Boolean(initialDraftId));
  /** Đang gửi yêu cầu hoặc đang ghi đề lên trang: hiện popup chờ riêng (yêu cầu 2026-11-05). */
  const [phase, setPhase] = useState<"idle" | "sending" | "applying">("idle");
  // Lỗi hiện thông báo đỏ ở góc trên bên phải, không hiện trong popup (yêu cầu 2026-11-05).
  const setError = (message: string | null) => { if (message) showErrorToast(message); };
  const sources = useSourceUploads(assignmentId, showErrorToast);
  const ref = useRef(requestRef());
  const applied = useRef(false);
  const [autoApplyFailed, setAutoApplyFailed] = useState(false);
  const awaiting = busy || draft?.status === "PENDING";
  const uploading = sources.pending || template?.status === "READING";
  const templateReady = template?.status === "READY";

  // Cập nhật bản nháp đang hiển thị và làm mới mã yêu cầu sau trạng thái FAILED.
  function receive(next: AiDraft) {
    setDraft(next);
    if (next.status === "FAILED") ref.current = requestRef();
  }

  useEffect(() => {
    if (!initialDraftId) return;
    const controller = new AbortController();
    assignmentsApi.aiDraft(assignmentId, initialDraftId, controller.signal).then(
      loaded => { if (!controller.signal.aborted) { receive(loaded); setBusy(false); } },
      error => { if (!controller.signal.aborted) { setError(describeError(error)); setBusy(false); } },
    );
    return () => controller.abort();
  }, [assignmentId, initialDraftId]);

  useEffect(() => {
    const controller = new AbortController();
    assignmentsApi.listAiTemplates(assignmentId, controller.signal).then(
      (list) => { if (!controller.signal.aborted) setTemplates(list); },
      () => {},
    );
    return () => controller.abort();
  }, [assignmentId]);

  useEffect(() => {
    const controller = new AbortController();
    assignmentsApi.materials(assignmentId, controller.signal).then(
      (list) => {
        if (!controller.signal.aborted) setMaterials(list.filter((m) => m.hasText));
      },
      () => {},
    );
    return () => controller.abort();
  }, [assignmentId]);

  useEffect(() => {
    if (!draft || draft.status !== "PENDING") return;
    const timer = setTimeout(() => {
      assignmentsApi.aiDraft(assignmentId, draft.draftId).then(receive, (e) => setError(describeError(e)));
    }, POLL_MS);
    return () => clearTimeout(timer);
  }, [assignmentId, draft]);


  const proposal = draft?.status === "READY" ? draft.proposal : null;
  const validation = proposal?.validation;
  const invalid = proposal !== null && (!validation || validation.status === "INVALID" || proposal.sections.length === 0);
  const linkedMcpDraft = draft?.origin === "MCP" && draft.draftId === initialDraftId;
  const staleMcpDraft = linkedMcpDraft && draft.baseVersion !== version;
  const canAutoApply = proposal !== null && !invalid && !requiresAiDraftReview(draft)
    && (draft?.origin !== "MCP"
      || (linkedMcpDraft && validation?.status === "VALID" && !staleMcpDraft && !autoApplyFailed));

  useEffect(() => {
    if (linkedMcpDraft && draft.status === "APPLIED") onClose();
  }, [draft, linkedMcpDraft, onClose]);

  /** Đọc mẫu đề bằng bộ nhập DOCX; số câu hỏi là số mục ngoài cùng của mẫu. */
  async function readTemplate(file: File) {
    setTemplate({ file, status: "READING" });
    const read = await readTemplateFile(assignmentId, file);
    setTemplate((current) => (current?.file === file ? read : current));
  }

  // Gửi yêu cầu sinh nháp, cập nhật trạng thái chờ và cho phép thử lại sau lỗi.
  async function submit(input: Parameters<typeof assignmentsApi.requestAiDraft>[1]) {
    setPhase("sending");
    try {
      receive(await assignmentsApi.requestAiDraft(assignmentId, input));
    } catch (e) {
      setError(describeError(e));
      ref.current = requestRef();
    } finally {
      setPhase("idle");
    }
  }

  // Không trường nào bắt buộc (yêu cầu 2026-11-09): bỏ trống thì AI tự quyết; nút Tạo đề không bị khóa.
  // Kiểm số câu, thang điểm và tệp đính kèm trước khi dựng yêu cầu tạo nháp.
  async function sendRequest(choice?: SourceChoice) {
    if (requestTooLong) return;
    if (countError || pointsError || questionPoints === null) {
      setError(countError ?? pointsError ?? msg("assignments.components.QuestionPointsField.invalid"));
      return;
    }
    if (uploading) {
      setError(msg("assignments.components.AiDraftDialog.waitForUploadsToFinish"));
      return;
    }
    if (!choice) {
      setPickingSource(true);
      return;
    }
    applied.current = false;
    const input = {
      request,
      materialIds: chosen,
      sourceFileRefs: sources.fileRefs,
      formatText: templateReady ? template.text : undefined,
      templateAssignmentId: template ? undefined : templateId || undefined,
      source: choice.source,
      provider: choice.source === "PERSONAL" ? choice.provider ?? undefined : undefined,
      // Ngôn ngữ theo ô Ngôn ngữ, ưu tiên hơn ngôn ngữ ghi trong yêu cầu hay đề mẫu.
      options: { profile: "AUTO" as const, language,
        questionCount: count ?? undefined,
        targetPoints: !hasQuestionPoints && pointsValid ? pointsNumber : undefined,
        ...(questionPoints.length ? { questionPoints } : {}) },
      requestRef: ref.current,
    };
    await submit(input);
  }

  // Chặn thao tác trùng và chốt version dùng để áp dụng đề xuất vào bài.
  async function applyDraft() {
    if (!draft || invalid || staleMcpDraft || applied.current) return;
    applied.current = true;
    setPhase("applying");
    try {
      const latestVersion = prepareApply ? await prepareApply() : version;
      await assignmentsApi.applyAiDraft(
        assignmentId,
        draft.draftId,
        linkedMcpDraft ? draft.baseVersion : latestVersion,
        !currentInstructions.trim() || (linkedMcpDraft && Boolean(draft.proposal?.instructions.trim())),
        (linkedMcpDraft && Boolean(draft.proposal?.title?.trim()))
          || !currentTitle.trim()
          || currentTitle.trim() === msg("assignments.components.AiDraftDialog.untitledAssignment"),
      );
      onApplied();
    } catch (e) {
      applied.current = false;
      setError(describeError(e));
      if (linkedMcpDraft) {
        setAutoApplyFailed(true);
        return;
      }
      // Quay về popup cũ, giữ nguyên nội dung đã nhập; bản nháp lỗi bỏ đi để tạo lại được.
      void assignmentsApi.discardAiDraft(assignmentId, draft.draftId).catch(() => {});
      setDraft(null);
      ref.current = requestRef();
    } finally {
      setPhase("idle");
    }
  }

  useEffect(() => {
    if (!canAutoApply) return;
    const timer = setTimeout(() => void applyDraft(), 0);
    return () => clearTimeout(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [proposal, canAutoApply]);

  // Bỏ đề xuất hiện tại để chuẩn bị lượt sinh đề xuất mới.
  async function revise() {
    if (!draft) return;
    setBusy(true);
    try {
      await assignmentsApi.discardAiDraft(assignmentId, draft.draftId);
      setDraft(null);
      ref.current = requestRef();
    } catch (cause) {
      setError(describeError(cause));
    } finally {
      setBusy(false);
    }
  }

  // Xóa nguồn đã chọn, thử bỏ bản nháp đang chờ hoặc sẵn sàng rồi đóng hộp thoại.
  async function discard() {
    sources.clear();
    if (draft && (draft.status === "READY" || draft.status === "PENDING")) {
      await assignmentsApi.discardAiDraft(assignmentId, draft.draftId).catch(() => {});
    }
    onClose();
  }

  const showPreview = proposal !== null && !canAutoApply;
  const questionPoints = gradingMode !== "PRACTICE" ? parseQuestionPoints(questionPointsText) : [];
  const hasQuestionPoints = Boolean(questionPoints?.length);
  const count = hasQuestionPoints ? undefined : parseQuestionCount(questionCount);
  // Số câu hỏi sai hiện đỏ ngay dưới ô; ô trống thì AI tự quyết.
  const countError = count === null
    ? msg("assignments.components.AiDraftDialog.theNumberOfQuestionsMust") : undefined;
  // Tổng điểm như luật của server: lớn hơn 0, tối đa 1.000, không quá 2 chữ số thập phân; ô trống thì AI tự chia điểm.
  const pointsNumber = Number(targetPoints);
  const pointsValid = targetPoints.trim() !== "" && Number.isFinite(pointsNumber) && pointsNumber > 0 && pointsNumber <= 1000
    && Math.round(pointsNumber * 100) === pointsNumber * 100;
  const pointsError = !hasQuestionPoints && targetPoints.trim() !== "" && !pointsValid
    ? msg("assignments.components.AiDraftDialog.totalPointsMustBeGreater") : undefined;
  if (linkedMcpDraft && draft.status === "APPLIED") return null;
  const working = phase !== "idle" || draft?.status === "PENDING" || (busy && !draft) || canAutoApply;
  // Popup chờ riêng: popup soạn vẫn giữ nguyên trạng thái để quay về khi lỗi.
  if (working) return <AiProgress taskId={draft?.status === "PENDING" ? draft.taskId : null} />;
  if (pickingSource) return <AiSourcePicker open requireConfirmation
    task={currentTitle || msg("assignments.components.AiDraftDialog.draftWithAi")}
    onCancel={() => setPickingSource(false)}
    onSubmit={(choice) => { setPickingSource(false); void sendRequest(choice); }} />;
  return (
    <Dialog
      open
      size="xl"
      icon="smart_toy"
      title={msg("assignments.components.AiDraftDialog.draftWithAi")}
      onClose={() => void discard()}
      dismissible={!busy}
      footer={
        showPreview ? (
          <>
            <Button onClick={() => void discard()} disabled={busy}>{msg("assignments.components.AiDraftDialog.discardProposal")}</Button>
            <Button variant={invalid ? "primary" : "secondary"} onClick={() => void revise()} disabled={busy}>{msg("assignments.components.AiDraftDialog.editRequestAndRegenerate")}</Button>
            {!invalid && <Button variant="primary" onClick={() => void applyDraft()} disabled={busy || staleMcpDraft}
              data-testid="ai-apply-proposal">{msg("assignments.components.AiDraftDialog.applyProposal")}</Button>}
          </>
        ) : (
          <Button
            variant="primary"
            onClick={() => void sendRequest()}
            disabled={awaiting || proposal !== null || requestTooLong}
            data-testid="ai-send"
          >
            {msg("assignments.components.AiDraftDialog.createAssignment")}</Button>
        )
      }
    >
      {staleMcpDraft && showPreview && <Alert tone="warning" role="alert">
        {msg("assignments.components.AiDraftDialog.linkedDraftStale")}
      </Alert>}
      {!showPreview && (
        <>
          {draft?.status === "FAILED" && <Alert tone="danger" role="alert">
            <p className="font-semibold">{msg("assignments.components.AiDraftDialog.draftFailed")}</p>
            <p>{msg("assignments.components.AiDraftDialog.youCanStillWriteIt", {
              value: draft.safeError ? describeAiFailure(draft.safeError) : msg("assignments.components.AiDraftDialog.theAiReturnedNoResult"),
            })}</p>
          </Alert>}
          <p className="text-[13px] leading-relaxed text-muted" data-testid="ai-intro">
            {msg("assignments.components.AiDraftDialog.describeTheAssignmentYouNeed")}</p>
          {/* Mẫu đề và tài liệu tham khảo chỉ thêm ngữ cảnh, không bắt buộc (yêu cầu 2026-11-09). */}
          <Field label={msg("assignments.components.AiDraftDialog.template")} htmlFor="ai-template">
            <FileDropZone accept=".docx" disabled={awaiting} label={msg("assignments.components.AiDraftDialog.dropTemplateHere")}
              onFiles={([file]) => { if (!file) return; setTemplateId(""); void readTemplate(file); }}>
            <Select id="ai-template" aria-label={msg("assignments.components.AiDraftDialog.template")} disabled={awaiting} data-testid="ai-template"
              value={template ? "__file__" : templateId}
              onChange={(e) => {
                const value = e.target.value;
                if (value === "__upload__") templateInput.current?.click();
                else if (value === "__file__") return;
                else {
                  setTemplate(null);
                  setTemplateId(value);
                }
              }}>
              <option value="">{msg("assignments.components.AiDraftDialog.noTemplate")}</option>
              {template && <option value="__file__">{template.file.name}</option>}
              {templates.length > 0 && (
                <optgroup label={msg("assignments.components.AiDraftDialog.subjectTemplates")}>
                  {templates.map((item) => <option key={item.assignmentId} value={item.assignmentId}>{item.title}</option>)}
                </optgroup>
              )}
              <option value="__upload__">{msg("assignments.components.AiDraftDialog.uploadATemplate")}</option>
            </Select>
            </FileDropZone>
            <input ref={templateInput} type="file" accept=".docx" hidden aria-label={msg("assignments.components.AiDraftDialog.uploadATemplate2")} data-testid="ai-format"
              onChange={(event) => {
                const file = event.target.files?.[0];
                event.target.value = "";
                if (!file) return;
                setTemplateId("");
                void readTemplate(file);
              }} />
            {template && template.status !== "READY" && (
              <span className="flex items-center gap-2"><TemplateStatus template={template} onRetry={() => void readTemplate(template.file)} /></span>
            )}
          </Field>
          <div className="grid gap-3 sm:grid-cols-3">
            <Field label={msg("assignments.components.AiDraftDialog.language")} htmlFor="ai-language">
              <Select id="ai-language" aria-label={msg("assignments.components.AiDraftDialog.language")} value={language} disabled={awaiting}
                onChange={(e) => setLanguage(e.target.value as typeof language)}>
                {LOCALES.map((code) => <option key={code} value={code.toUpperCase()} lang={code}>{LOCALE_NAMES[code]}</option>)}
              </Select>
            </Field>
            <QuestionCountField id="ai-count" value={questionPoints?.length ? String(questionPoints.length) : questionCount}
              onChange={setQuestionCount} disabled={awaiting || hasQuestionPoints} invalid={count === null} />
            <Field label={msg("assignments.components.AiDraftDialog.totalPoints")} htmlFor="ai-points" error={pointsError}>
              <input id="ai-points" aria-label={msg("assignments.components.AiDraftDialog.totalPoints")} type="number" min={0.01} max={1000} step={0.5}
                value={questionPoints?.length ? questionPoints.reduce((cents, point) => cents + Math.round(point * 100), 0) / 100 : targetPoints}
                placeholder={msg("assignments.components.AiDraftDialog.aiDistributes")} disabled={awaiting || hasQuestionPoints} onChange={(e) => setTargetPoints(e.target.value)}
                aria-invalid={pointsError ? true : undefined} data-testid="ai-points"
                className={`w-full rounded-lg border px-3 py-2 text-[13px] disabled:cursor-not-allowed disabled:bg-sunken disabled:text-muted ${
                  pointsError ? "border-danger" : "border-line-strong"}`} />
            </Field>
          </div>
          <Field label={msg("assignments.components.AiDraftDialog.references")} htmlFor="ai-reference">
            <ReferencePicker materials={materials} chosen={chosen} uploads={sources.uploads} disabled={awaiting}
              canUpload={sources.canUpload} accept={sources.accept}
              onChoose={(id) => setChosen((c) => (c.includes(id) ? c : [...c, id].slice(0, 10)))}
              onUnchoose={(id) => setChosen((c) => c.filter((item) => item !== id))}
              onUpload={sources.add} onRemoveUpload={sources.remove} onRetryUpload={sources.retry} />
          </Field>
          {gradingMode !== "PRACTICE" && (
            <QuestionPointsField id="review-ai-question-points" value={questionPointsText}
              onChange={setQuestionPointsText} points={questionPoints} disabled={awaiting} />
          )}
          <Field label={msg("assignments.components.AiDraftDialog.request")} htmlFor="ai-request">
            <Textarea id="ai-request" rows={4} value={request} disabled={awaiting}
              invalid={requestTooLong} aria-describedby="draft-ai-request-length"
              placeholder={msg("assignments.components.AiDraftDialog.exampleAnAnalysisAndDesign")}
              onChange={(e) => setRequest(e.target.value)} />
            <AiRequestLength id="draft-ai-request-length" length={request.length} />
          </Field>
        </>
      )}
      {showPreview && proposal && (
        <div className="flex flex-col gap-4" data-testid="ai-proposal">
          {currentPartCount > 0 && <p className="text-xs text-muted">{msg("assignments.components.AiDraftDialog.theAssignmentHasNotBeen")}</p>}
          {draft?.provenance.templateReviewRequired === true && (
            <p className="text-sm text-muted">{msg("assignments.components.AiDraftDialog.reviewTemplateDraft")}</p>
          )}
          {typeof draft?.provenance.model === "string" && (
            <p className="text-sm text-muted">{msg("shared.ai.AiSourcePicker.model", {
              model: draft.provenance.model,
            })}</p>
          )}
          <AiDraftPreview proposal={proposal}
            targetPoints={!initialDraftId && !hasQuestionPoints && pointsValid ? pointsNumber : undefined} />
        </div>
      )}
    </Dialog>
  );
}

/** Popup chờ khi AI đang soạn hay đang ghi đề. */
export function AiProgress({ taskId }: { taskId?: string | null }) {
  return (
    <BusyOverlay label={msg("assignments.components.AiDraftDialog.creatingDraft")} spinner testId="ai-progress">
      <span className="text-[14px] font-semibold text-ink">{msg("assignments.components.AiDraftDialog.creatingDraft")}</span>
      {taskId && <AiTaskStatus key={taskId} taskId={taskId} cancellable={false} intervalMs={500} />}
    </BusyOverlay>
  );
}

// Hiện trạng thái đọc mẫu hoặc nút thử lại cho tệp mẫu lỗi.
function TemplateStatus({ template, onRetry }: { template: Template; onRetry: () => void }) {
  if (template.status === "READING") return <span role="status" className="text-xs text-muted">{msg("assignments.components.AiDraftDialog.reading")}</span>;
  if (template.status === "READY") {
    return <Pill tone="success">{template.questionCount ? msg("assignments.components.AiDraftDialog.questions", { questionCount: String(template.questionCount) }) : msg("assignments.components.AiDraftDialog.read")}</Pill>;
  }
  return <>
    <span className="text-xs text-danger">{template.error}</span>
    <Button size="sm" onClick={onRetry}>{msg("assignments.components.AiDraftDialog.retry")}</Button>
  </>;
}

/**
 * Tài liệu tham khảo (yêu cầu 2026-11-05): một ô gộp học liệu của bài và tệp tải lên, thêm được nhiều tài liệu. Ô tìm đứng
 * yên, gợi ý luôn ngay dưới ô tìm, dòng cuối của gợi ý là tải tài liệu lên; tài liệu đã chọn xếp dài dần xuống dưới ô tìm.
 */
export function ReferencePicker({ id = "ai-reference", materials, chosen, uploads, disabled, canUpload, accept, onChoose,
  onUnchoose, onUpload, onRemoveUpload, onRetryUpload }: {
  /** Id của ô tìm; đổi khi một trang có hai ô chọn tài liệu. */
  id?: string;
  materials: Material[];
  chosen: string[];
  uploads: SourceUpload[];
  disabled: boolean;
  canUpload: boolean;
  accept?: string;
  onChoose: (materialId: string) => void;
  onUnchoose: (materialId: string) => void;
  onUpload: (files: FileList | File[] | null) => void;
  onRemoveUpload: (item: SourceUpload) => void;
  onRetryUpload: (item: SourceUpload) => void;
}) {
  const fileInput = useRef<HTMLInputElement>(null);
  const picked = chosen.flatMap((id) => materials.filter((m) => m.materialId === id));
  // Tên tài liệu cắt ngắn để một dòng chứa được khoảng 4 tài liệu (yêu cầu 2026-11-10); rê chuột xem đủ tên.
  const chip = "inline-flex h-7 max-w-[8.5rem] items-center gap-1.5 rounded-md border border-line bg-sunken pl-2 pr-0.5 text-xs";
  return (
    <>
      {/* Kéo tệp vào ô tài liệu để tải lên (yêu cầu 2026-11-27). */}
      <FileDropZone accept={accept} multiple disabled={disabled || !canUpload} label={msg("assignments.components.AiDraftDialog.dropDocumentsHere")}
        onFiles={onUpload}>
      {/* Tài liệu đã chọn xếp hàng ngay trong thanh tìm, dài thì xuống dòng (yêu cầu 2026-11-05). */}
      <SearchPicker id={id} label={msg("assignments.components.AiDraftDialog.references")} disabled={disabled} optionIcon="menu_book" multiple
        placeholder={msg("assignments.components.AiDraftDialog.chooseSubjectMaterialsOrUpload")}
        options={materials.filter((m) => !chosen.includes(m.materialId)).map((m) => ({ id: m.materialId, label: m.title }))}
        emptyText={materials.length === 0 ? msg("assignments.components.AiDraftDialog.noUsableMaterialsYet") : msg("assignments.components.AiDraftDialog.noMaterialsFound")}
        onPick={onChoose}
        footer={{ label: msg("assignments.components.AiDraftDialog.uploadDocuments", { value: String(uploads.length > 0 ? ` (${uploads.length}/${MAX_SOURCE_FILES})` : "") }),
          icon: "upload_file", disabled: !canUpload, /* Mở hộp chọn tệp mẫu qua input ẩn. */ onPick: () => fileInput.current?.click() }}
        chips={(picked.length > 0 || uploads.length > 0) && (
          <ul className="contents" aria-label={msg("assignments.components.AiDraftDialog.selectedDocuments")}>
            {picked.map((m) => (
              <li key={m.materialId} className={chip}>
                <Icon name="menu_book" size={13} className="shrink-0 text-primary-ink" />
                <span className="min-w-0 truncate font-medium text-ink" title={m.title}>{m.title}</span>
                <IconButton icon="close" label={msg("assignments.components.AiDraftDialog.remove", { title: String(m.title) })} size={22} disabled={disabled} onClick={() => onUnchoose(m.materialId)} />
              </li>
            ))}
            {uploads.map((item) => (
              <li key={item.id} className={`${chip} ${item.status === "ERROR" ? "border-danger" : ""}`}
                title={item.status === "ERROR" ? item.error : item.file.name}>
                <Icon name="description" size={13} className="shrink-0 text-primary-ink" />
                <span className="min-w-0 truncate font-medium text-ink">{item.file.name}</span>
                {item.status === "UPLOADING" && <span role="status" className="shrink-0 text-muted">{item.percent}%</span>}
                {item.status === "READING" && <span role="status" className="shrink-0 text-muted">{msg("assignments.components.AiDraftDialog.reading")}</span>}
                {item.status === "ERROR" && (
                  <IconButton icon="refresh" label={msg("assignments.components.AiDraftDialog.retry2", { name: String(item.file.name) })} size={22} onClick={() => onRetryUpload(item)} />
                )}
                <IconButton icon="close" label={msg("assignments.components.AiDraftDialog.remove2", { name: String(item.file.name) })} size={22} onClick={() => onRemoveUpload(item)} />
              </li>
            ))}
          </ul>
        )} />
      </FileDropZone>
      {uploads.some(item => item.file.type.startsWith("image/")) && <div className="flex flex-wrap gap-2">
        {uploads.filter(item => item.file.type.startsWith("image/")).map(item => <SourceImagePreview key={item.id} file={item.file} />)}
      </div>}
      <input ref={fileInput} aria-label={msg("assignments.components.AiDraftDialog.uploadDocuments2")} type="file" multiple accept={accept} hidden
        onChange={(event) => { onUpload(event.target.files); event.target.value = ""; }} />
    </>
  );
}
