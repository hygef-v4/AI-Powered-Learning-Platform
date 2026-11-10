"use client";

import { useEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { getPolicies, uploadFile } from "@/features/files/api";
import {
  acceptAttribute,
  checkFile,
  type PurposePolicy,
} from "@/features/files/model/purposes";
import { Button, Dialog, Field, Icon, IconButton, Pill, Select, showToast } from "@/shared/ui";
import { assignmentsApi } from "../api";
import { AiDraftPreview } from "./AiDraftPreview";
import { SearchPicker } from "./SearchPicker";
import { TemplateFileField } from "./TemplateFileField";
import { errorMessage, requestRef } from "../model/format";
import { templateFor } from "../model/templateStore";
import { templateText } from "../model/templateText";
import type { AiDraft, AiSourcePreview, GradingMode, Material } from "../model/types";

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
const MAX_SOURCE_FILES = 3;

type SourceUpload = {
  id: string;
  file: File;
  status: "UPLOADING" | "READING" | "READY" | "ERROR";
  percent: number;
  fileRef?: string;
  error?: string;
  preview?: AiSourcePreview;
};
/** Mẫu đề: DOCX nhập qua bộ nhập khung đề (tối đa 20 MB, không giới hạn 2 MB của tệp nguồn), gửi AI dạng chữ. */
type Template = { file: File; status: "READING" | "READY" | "ERROR"; text?: string; questionCount?: number; error?: string };

/**
 * t13b Tạo nháp bằng AI (F6; BR-U07-50, 51). Yêu cầu 2026-11-05: không chọn nguồn AI (hệ thống dùng API chung nếu hạn mức
 * còn đủ cho lượt này, không thì API cá nhân; API chung lỗi thì tự chuyển sang API cá nhân), tệp nguồn, tài liệu tham khảo
 * và định dạng đề trên một dòng; đề hợp lệ được ghi thẳng lên trang (Ctrl+Z quay lại), đề lỗi mới hiện xem trước.
 */
export function AiDraftDialog({
  assignmentId,
  initialDraftId,
  version,
  currentPartCount,
  currentTitle,
  currentInstructions,
  prepareApply,
  onClose,
  onApplied,
}: Props) {
  const [request, setRequest] = useState("");
  const [language, setLanguage] = useState<"VI" | "EN">("EN");
  const [questionCount, setQuestionCount] = useState("");
  const [materials, setMaterials] = useState<Material[]>([]);
  const [chosen, setChosen] = useState<string[]>([]);
  const [sourceFiles, setSourceFiles] = useState<SourceUpload[]>([]);
  const [template, setTemplate] = useState<Template | null>(null);

  const [sourcePolicy, setSourcePolicy] = useState<PurposePolicy | null>(null);
  const abortUploads = useRef(new Map<string, () => void>());
  const [draft, setDraft] = useState<AiDraft | null>(null);
  const [busy, setBusy] = useState(Boolean(initialDraftId));
  /** Đang gửi yêu cầu hoặc đang ghi đề lên trang: hiện popup chờ riêng (yêu cầu 2026-11-05). */
  const [phase, setPhase] = useState<"idle" | "sending" | "applying">("idle");
  // Lỗi hiện thông báo đỏ ở góc trên bên phải, không hiện trong popup (yêu cầu 2026-11-05).
  const setError = (message: string | null) => { if (message) showToast(message, "danger"); };
  const ref = useRef(requestRef());
  const applied = useRef(false);
  const awaiting = busy || draft?.status === "PENDING";
  const uploading = sourceFiles.some((item) => item.status !== "READY") || template?.status === "READING";
  const templateReady = template?.status === "READY";
  /** Đã chọn mẫu đề (kể cả đang đọc): ngôn ngữ và số câu hỏi khóa, lấy theo mẫu. Đọc lỗi thì mở lại. */
  const templateSet = template !== null && template.status !== "ERROR";

  useEffect(() => {
    if (!initialDraftId) return;
    const controller = new AbortController();
    assignmentsApi.aiDraft(assignmentId, initialDraftId, controller.signal).then(
      loaded => { if (!controller.signal.aborted) { setDraft(loaded); setBusy(false); } },
      error => { if (!controller.signal.aborted) { setError(errorMessage(error)); setBusy(false); } },
    );
    return () => controller.abort();
  }, [assignmentId, initialDraftId]);

  useEffect(() => {
    let active = true;
    const pending = abortUploads.current;
    getPolicies()
      .then((policies) => {
        if (active) setSourcePolicy(policies.find((policy) => policy.purpose === "ASSIGNMENT_SOURCE") ?? null);
      })
      .catch(() => {
        if (active) setError("Không tải được giới hạn tệp nguồn.");
      });
    return () => {
      active = false;
      pending.forEach((abort) => abort());
      pending.clear();
    };
  }, []);

  // Bài tạo kèm đề mẫu: mặc định dùng đề mẫu đó làm Mẫu đề (yêu cầu 2026-11-05).
  const templateLoaded = useRef(false);
  useEffect(() => {
    if (templateLoaded.current) return;
    templateLoaded.current = true;
    let active = true;
    void templateFor(assignmentId).then((file) => {
      if (active && file) void readTemplate(file);
    });
    return () => { active = false; };
    // readTemplate chỉ dùng setter; chạy một lần.
    // eslint-disable-next-line react-hooks/exhaustive-deps
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
      assignmentsApi.aiDraft(assignmentId, draft.draftId).then(setDraft, (e) => setError(errorMessage(e)));
    }, POLL_MS);
    return () => clearTimeout(timer);
  }, [assignmentId, draft]);


  const proposal = draft?.status === "READY" ? draft.proposal : null;
  const validation = proposal?.validation;
  const invalid = proposal !== null && (!validation || validation.status === "INVALID" || proposal.sections.length === 0);

  // Lỗi khi chạy: hệ thống đã tự thử lần lượt các khóa (trường rồi cá nhân) ở phía máy chủ; hết khóa mới báo lỗi. Quay về
  // popup cũ, giữ nguyên mọi thứ đã nhập, báo lỗi ở góc.
  useEffect(() => {
    if (draft?.status !== "FAILED") return;
    showToast(`${draft.safeError ?? "AI không trả kết quả."} Bạn vẫn soạn thủ công được.`, "danger");
    setDraft(null);
    ref.current = requestRef();
  }, [draft]);

  // Đề hợp lệ: ghi thẳng lên trang.
  useEffect(() => {
    if (!proposal || invalid || applied.current) return;
    applied.current = true;
    void apply();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [proposal, invalid]);

  function updateUpload(id: string, update: (item: SourceUpload) => SourceUpload) {
    setSourceFiles((files) => files.map((item) => (item.id === id ? update(item) : item)));
  }

  /** Đọc mẫu đề bằng bộ nhập DOCX; số câu hỏi là số mục ngoài cùng của mẫu. */
  async function readTemplate(file: File) {
    setTemplate({ file, status: "READING" });
    try {
      const preview = await assignmentsApi.importDocx(assignmentId, file);
      const { text, questionCount: count } = templateText(preview.document);
      if (!text.trim()) throw new Error("Mẫu đề không có nội dung đọc được.");
      setTemplate((current) => (current?.file === file ? { file, status: "READY", text, questionCount: count } : current));
    } catch (cause) {
      setTemplate((current) => (current?.file === file ? { file, status: "ERROR", error: errorMessage(cause) } : current));
    }
  }

  async function upload(file: File, id = crypto.randomUUID()) {
    if (!sourcePolicy) {
      setError("Chưa tải được chính sách tệp nguồn.");
      return;
    }
    const issue = checkFile(file, sourcePolicy);
    const start: SourceUpload = issue
      ? { id, file, status: "ERROR", percent: 0, error: issue }
      : { id, file, status: "UPLOADING", percent: 0 };
    setSourceFiles((files) => (files.some((item) => item.id === id)
      ? files.map((item) => (item.id === id ? start : item)) : [...files, start]));
    if (issue) return;
    const handle = uploadFile(file, "ASSIGNMENT_SOURCE", (percent) => updateUpload(id, (item) => ({ ...item, percent })));
    const controller = new AbortController();
    abortUploads.current.set(id, () => {
      handle.abort();
      controller.abort();
    });
    try {
      const result = await handle.promise;
      updateUpload(id, (item) => ({ ...item, status: "READING", percent: 100, fileRef: result.fileRef.value }));
      const previews = await assignmentsApi.previewAiSources(assignmentId, [result.fileRef.value], controller.signal);
      if (!previews[0]) throw new Error("Không đọc được nội dung tệp.");
      updateUpload(id, (item) => ({ ...item, status: "READY", preview: previews[0] }));
    } catch (cause) {
      updateUpload(id, (item) => ({
        ...item,
        status: "ERROR",
        error: cause instanceof Error ? cause.message : "Tải tệp lên không thành công",
      }));
    } finally {
      abortUploads.current.delete(id);
    }
  }

  function addSourceFiles(files: FileList | File[] | null) {
    if (!files) return;
    const remaining = MAX_SOURCE_FILES - sourceFiles.length;
    if (files.length > remaining) setError("Chỉ chọn tối đa 3 tệp nguồn.");
    Array.from(files).slice(0, Math.max(0, remaining)).forEach((file) => void upload(file));
  }

  function removeUpload(item: SourceUpload) {
    abortUploads.current.get(item.id)?.();
    abortUploads.current.delete(item.id);
    setSourceFiles((files) => files.filter((file) => file.id !== item.id));
  }

  async function submit(input: Parameters<typeof assignmentsApi.requestAiDraft>[1]) {
    setPhase("sending");
    try {
      setDraft(await assignmentsApi.requestAiDraft(assignmentId, input));
    } catch (e) {
      setError(errorMessage(e));
      ref.current = requestRef();
    } finally {
      setPhase("idle");
    }
  }

  async function send() {
    if (!configured || !hasReference) return;
    // Có mẫu đề: ngôn ngữ và số câu hỏi lấy theo mẫu (yêu cầu 2026-11-05).
    const count = templateReady
      ? (template.questionCount && template.questionCount <= 10 ? template.questionCount : undefined)
      : Number(questionCount);
    if (uploading) {
      setError("Hãy đợi tải xong hoặc gỡ các tệp bị lỗi trước khi tạo đề.");
      return;
    }
    if (draft?.status === "FAILED") ref.current = requestRef();
    applied.current = false;
    const input = {
      request,
      materialIds: chosen,
      sourceFileRefs: sourceFiles.flatMap((item) => (item.fileRef ? [item.fileRef] : [])),
      formatText: templateReady ? template.text : undefined,
      options: { profile: "AUTO" as const, language: templateReady ? "AUTO" as const : language, questionCount: count },
      requestRef: ref.current,
    };
    await submit(input);
  }

  async function apply() {
    if (!draft) return;
    setPhase("applying");
    try {
      const latestVersion = prepareApply ? await prepareApply() : version;
      await assignmentsApi.applyAiDraft(
        assignmentId,
        draft.draftId,
        latestVersion,
        !currentInstructions.trim(),
        !currentTitle.trim() || currentTitle.trim() === "Bài tập chưa đặt tên",
      );
      onApplied();
    } catch (e) {
      applied.current = false;
      setError(errorMessage(e));
      // Quay về popup cũ, giữ nguyên nội dung đã nhập; bản nháp lỗi bỏ đi để tạo lại được.
      void assignmentsApi.discardAiDraft(assignmentId, draft.draftId).catch(() => {});
      setDraft(null);
      ref.current = requestRef();
    } finally {
      setPhase("idle");
    }
  }

  async function revise() {
    if (!draft) return;
    setBusy(true);
    try {
      await assignmentsApi.discardAiDraft(assignmentId, draft.draftId);
      setDraft(null);
      ref.current = requestRef();
    } catch (cause) {
      setError(errorMessage(cause));
    } finally {
      setBusy(false);
    }
  }

  async function discard() {
    abortUploads.current.forEach((abort) => abort());
    abortUploads.current.clear();
    if (draft && (draft.status === "READY" || draft.status === "PENDING")) {
      await assignmentsApi.discardAiDraft(assignmentId, draft.draftId).catch(() => {});
    }
    onClose();
  }

  const showPreview = proposal !== null && invalid;
  const countNumber = Number(questionCount);
  const countValid = questionCount.trim() !== "" && Number.isInteger(countNumber) && countNumber >= 1 && countNumber <= 10;
  // Lỗi số câu hỏi hiện đỏ ngay dưới ô (yêu cầu 2026-11-05); ô trống thì chỉ khóa nút Tạo đề.
  const countError = questionCount.trim() !== "" && !countValid ? "Số câu hỏi từ 1 đến 10." : undefined;
  const hasReference = chosen.length > 0 || sourceFiles.length > 0;
  /** Cấu hình: có mẫu đề, hoặc nhập số câu hỏi (ngôn ngữ luôn có giá trị). */
  const configured = templateReady || countValid;
  const working = phase !== "idle" || draft?.status === "PENDING" || (busy && !draft) || (proposal !== null && !invalid);
  const accept = sourcePolicy ? acceptAttribute(sourcePolicy) : undefined;
  if (working) {
    // Popup chờ riêng: chỉ vòng xoay và chữ; popup soạn vẫn giữ nguyên trạng thái để quay về khi lỗi.
    return typeof document === "undefined" ? null : createPortal(
      <div className="fixed inset-0 z-[100] flex animate-fade items-center justify-center bg-[var(--c-scrim)] p-4 backdrop-blur-[2px]">
        <div role="alertdialog" aria-modal="true" aria-busy="true" aria-label="Đang tạo bản nháp" data-testid="ai-progress"
          className="flex animate-pop items-center gap-3 rounded-xl border border-line bg-surface px-6 py-5 shadow-[var(--shadow-float)]">
          <span aria-hidden="true"
            className="size-5 animate-spin rounded-full border-2 border-line-strong border-t-primary motion-reduce:animate-none" />
          <span className="text-[14px] font-semibold text-ink">Đang tạo bản nháp</span>
        </div>
      </div>,
      document.body,
    );
  }
  return (
    <Dialog
      open
      size="xl"
      icon="smart_toy"
      title="Tạo nháp bằng AI"
      onClose={() => void discard()}
      dismissible={!busy}
      footer={
        showPreview ? (
          <>
            <Button onClick={() => void discard()} disabled={busy}>Bỏ đề xuất</Button>
            <Button variant="primary" onClick={() => void revise()} disabled={busy}>Sửa yêu cầu và tạo lại</Button>
          </>
        ) : (
          <Button
            variant="primary"
            onClick={() => void send()}
            disabled={awaiting || proposal !== null || uploading || !configured || !hasReference}
            data-testid="ai-send"
          >
            Tạo đề
          </Button>
        )
      }
    >
      {!showPreview && (
        <>
          {/* Cấu hình (yêu cầu 2026-11-05): mẫu đề, hoặc ngôn ngữ và số câu hỏi; có mẫu đề thì hai ô dưới lấy theo mẫu. */}
          <section aria-labelledby="ai-config" className="flex flex-col gap-3">
            <p id="ai-config" className="text-xs font-semibold text-body">Cấu hình<span className="text-primary-ink"> *</span></p>
            <Field label="Mẫu đề">
              <TemplateFileField file={template?.file ?? null} disabled={awaiting} accept=".docx" testId="ai-format"
                status={template && <TemplateStatus template={template} onRetry={() => void readTemplate(template.file)} />}
                onChange={(file) => { if (file) void readTemplate(file); else setTemplate(null); }} />
            </Field>
            <div aria-hidden className="flex items-center gap-3 text-xs font-semibold text-muted">
              <span className="h-px flex-1 bg-line" />hoặc<span className="h-px flex-1 bg-line" />
            </div>
            <div className="grid gap-3 sm:grid-cols-2">
              <Field label="Ngôn ngữ" htmlFor="ai-language">
                <Select id="ai-language" aria-label="Ngôn ngữ" value={templateSet ? "AUTO" : language}
                  disabled={awaiting || templateSet} onChange={(e) => setLanguage(e.target.value as typeof language)}>
                  {templateSet && <option value="AUTO">Theo mẫu đề</option>}
                  <option value="EN">English</option>
                  <option value="VI">Tiếng Việt</option>
                </Select>
              </Field>
              <Field label="Số câu hỏi" htmlFor="ai-count" error={templateSet ? undefined : countError}>
                <input id="ai-count" aria-label="Số câu hỏi" type="number" min={1} max={10} step={1}
                  value={templateSet ? String(template.questionCount || "") : questionCount}
                  disabled={awaiting || templateSet} onChange={(e) => setQuestionCount(e.target.value)}
                  aria-invalid={!templateSet && countError ? true : undefined}
                  className={`w-full rounded-lg border px-3 py-2 text-[13px] disabled:cursor-not-allowed disabled:bg-sunken disabled:text-muted ${
                    !templateSet && countError ? "border-danger" : "border-line-strong"}`} />
              </Field>
            </div>
          </section>
          <Field label="Tài liệu tham khảo" htmlFor="ai-reference" required>
            <ReferencePicker materials={materials} chosen={chosen} uploads={sourceFiles} disabled={awaiting}
              canUpload={!!sourcePolicy && sourceFiles.length < MAX_SOURCE_FILES} accept={accept}
              onChoose={(id) => setChosen((c) => (c.includes(id) ? c : [...c, id].slice(0, 10)))}
              onUnchoose={(id) => setChosen((c) => c.filter((item) => item !== id))}
              onUpload={addSourceFiles} onRemoveUpload={removeUpload}
              onRetryUpload={(item) => void upload(item.file, item.id)} />
          </Field>
          <Field label="Yêu cầu" htmlFor="ai-request">
            <textarea id="ai-request" rows={4} maxLength={4000} value={request} disabled={awaiting}
              onChange={(e) => setRequest(e.target.value)}
              className="w-full rounded-lg border border-field px-3 py-2 text-[13px] outline-none focus:outline-2 focus:outline-focus" />
          </Field>
        </>
      )}
      {showPreview && proposal && (
        <div className="flex flex-col gap-4" data-testid="ai-proposal">
          {currentPartCount > 0 && <p className="text-xs text-muted">Bài chưa bị thay đổi.</p>}
          <AiDraftPreview proposal={proposal} />
        </div>
      )}
    </Dialog>
  );
}

function TemplateStatus({ template, onRetry }: { template: Template; onRetry: () => void }) {
  if (template.status === "READING") return <span role="status" className="text-xs text-muted">Đang đọc…</span>;
  if (template.status === "READY") {
    return <Pill tone="success">{template.questionCount ? `${template.questionCount} câu hỏi` : "Đã đọc"}</Pill>;
  }
  return <>
    <span className="text-xs text-danger">{template.error}</span>
    <Button size="sm" onClick={onRetry}>Thử lại</Button>
  </>;
}

/**
 * Tài liệu tham khảo (yêu cầu 2026-11-05): một ô gộp học liệu của bài và tệp tải lên, thêm được nhiều tài liệu. Ô tìm đứng
 * yên, gợi ý luôn ngay dưới ô tìm, dòng cuối của gợi ý là tải tài liệu lên; tài liệu đã chọn xếp dài dần xuống dưới ô tìm.
 */
function ReferencePicker({ materials, chosen, uploads, disabled, canUpload, accept, onChoose, onUnchoose, onUpload,
  onRemoveUpload, onRetryUpload }: {
  materials: Material[];
  chosen: string[];
  uploads: SourceUpload[];
  disabled: boolean;
  canUpload: boolean;
  accept?: string;
  onChoose: (materialId: string) => void;
  onUnchoose: (materialId: string) => void;
  onUpload: (files: FileList | null) => void;
  onRemoveUpload: (item: SourceUpload) => void;
  onRetryUpload: (item: SourceUpload) => void;
}) {
  const fileInput = useRef<HTMLInputElement>(null);
  const picked = chosen.flatMap((id) => materials.filter((m) => m.materialId === id));
  const chip = "inline-flex h-7 max-w-[16rem] items-center gap-1.5 rounded-md border border-line bg-sunken pl-2 pr-0.5 text-xs";
  return (
    <>
      {/* Tài liệu đã chọn xếp hàng ngay trong thanh tìm, dài thì xuống dòng (yêu cầu 2026-11-05). */}
      <SearchPicker id="ai-reference" label="Tài liệu tham khảo" disabled={disabled} optionIcon="menu_book" multiple
        options={materials.filter((m) => !chosen.includes(m.materialId)).map((m) => ({ id: m.materialId, label: m.title }))}
        emptyText={materials.length === 0 ? "Chưa có học liệu dùng được." : "Không tìm thấy học liệu."}
        onPick={onChoose}
        footer={{ label: `Tải tài liệu lên${uploads.length > 0 ? ` (${uploads.length}/${MAX_SOURCE_FILES})` : ""}`,
          icon: "upload_file", disabled: !canUpload, onPick: () => fileInput.current?.click() }}
        chips={(picked.length > 0 || uploads.length > 0) && (
          <ul className="contents" aria-label="Tài liệu đã chọn">
            {picked.map((m) => (
              <li key={m.materialId} className={chip}>
                <Icon name="menu_book" size={13} className="shrink-0 text-primary-ink" />
                <span className="min-w-0 truncate font-medium text-ink" title={m.title}>{m.title}</span>
                <IconButton icon="close" label={`Bỏ ${m.title}`} size={22} disabled={disabled} onClick={() => onUnchoose(m.materialId)} />
              </li>
            ))}
            {uploads.map((item) => (
              <li key={item.id} className={`${chip} ${item.status === "ERROR" ? "border-danger" : ""}`}
                title={item.status === "ERROR" ? item.error : item.file.name}>
                <Icon name="description" size={13} className="shrink-0 text-primary-ink" />
                <span className="min-w-0 truncate font-medium text-ink">{item.file.name}</span>
                {item.status === "UPLOADING" && <span role="status" className="shrink-0 text-muted">{item.percent}%</span>}
                {item.status === "READING" && <span role="status" className="shrink-0 text-muted">Đang đọc…</span>}
                {item.status === "ERROR" && (
                  <IconButton icon="refresh" label={`Thử lại ${item.file.name}`} size={22} onClick={() => onRetryUpload(item)} />
                )}
                <IconButton icon="close" label={`Gỡ ${item.file.name}`} size={22} onClick={() => onRemoveUpload(item)} />
              </li>
            ))}
          </ul>
        )} />
      <input ref={fileInput} aria-label="Tải tài liệu lên" type="file" multiple accept={accept} hidden
        onChange={(event) => { onUpload(event.target.files); event.target.value = ""; }} />
    </>
  );
}
