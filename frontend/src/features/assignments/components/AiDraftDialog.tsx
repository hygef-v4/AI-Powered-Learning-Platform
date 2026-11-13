"use client";

import { useEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { Button, Dialog, Field, Icon, IconButton, Pill, Select, showErrorToast } from "@/shared/ui";
import { assignmentsApi } from "../api";
import { AiDraftPreview } from "./AiDraftPreview";
import { SearchPicker } from "./SearchPicker";
import { errorMessage, requestRef } from "../model/format";
import { MAX_SOURCE_FILES, readTemplateFile, useSourceUploads, type SourceUpload, type TemplateFile } from "./sourceUploads";
import type { AiDraft, GradingMode, Material } from "../model/types";

export type { SourceUpload };
import { SourceImagePreview } from "./SourceImagePreview";
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
  const [targetPoints, setTargetPoints] = useState(DEFAULT_TARGET_POINTS);
  const [materials, setMaterials] = useState<Material[]>([]);
  const [chosen, setChosen] = useState<string[]>([]);
  const [template, setTemplate] = useState<Template | null>(null);
  /** Đề mẫu chọn từ bài của môn (yêu cầu 2026-11-09); tệp tải lên thay thế lựa chọn này. */
  const [templateId, setTemplateId] = useState("");
  const [templates, setTemplates] = useState<{ assignmentId: string; title: string }[]>([]);
  const templateInput = useRef<HTMLInputElement>(null);

  const [draft, setDraft] = useState<AiDraft | null>(null);
  const [busy, setBusy] = useState(Boolean(initialDraftId));
  /** Đang gửi yêu cầu hoặc đang ghi đề lên trang: hiện popup chờ riêng (yêu cầu 2026-11-05). */
  const [phase, setPhase] = useState<"idle" | "sending" | "applying">("idle");
  // Lỗi hiện thông báo đỏ ở góc trên bên phải, không hiện trong popup (yêu cầu 2026-11-05).
  const setError = (message: string | null) => { if (message) showErrorToast(message); };
  const sources = useSourceUploads(assignmentId, showErrorToast);
  const ref = useRef(requestRef());
  const applied = useRef(false);
  const awaiting = busy || draft?.status === "PENDING";
  const uploading = sources.pending || template?.status === "READING";
  const templateReady = template?.status === "READY";

  /**
   * Kết quả AI về. Lỗi khi chạy: hệ thống đã tự thử lần lượt các khóa (trường rồi cá nhân) ở phía máy chủ, hết khóa mới báo
   * lỗi; quay về popup soạn, giữ nguyên mọi thứ đã nhập, báo lỗi ở góc.
   */
  function receive(next: AiDraft) {
    if (next.status !== "FAILED") {
      setDraft(next);
      return;
    }
    showErrorToast(`${next.safeError ?? "AI không trả kết quả."} Bạn vẫn soạn thủ công được.`);
    setDraft(null);
    ref.current = requestRef();
  }

  useEffect(() => {
    if (!initialDraftId) return;
    const controller = new AbortController();
    assignmentsApi.aiDraft(assignmentId, initialDraftId, controller.signal).then(
      loaded => { if (!controller.signal.aborted) { receive(loaded); setBusy(false); } },
      error => { if (!controller.signal.aborted) { setError(errorMessage(error)); setBusy(false); } },
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
      assignmentsApi.aiDraft(assignmentId, draft.draftId).then(receive, (e) => setError(errorMessage(e)));
    }, POLL_MS);
    return () => clearTimeout(timer);
  }, [assignmentId, draft]);


  const proposal = draft?.status === "READY" ? draft.proposal : null;
  const validation = proposal?.validation;
  const invalid = proposal !== null && (!validation || validation.status === "INVALID" || proposal.sections.length === 0);

  /** Đọc mẫu đề bằng bộ nhập DOCX; số câu hỏi là số mục ngoài cùng của mẫu. */
  async function readTemplate(file: File) {
    setTemplate({ file, status: "READING" });
    const read = await readTemplateFile(assignmentId, file);
    setTemplate((current) => (current?.file === file ? read : current));
  }

  async function submit(input: Parameters<typeof assignmentsApi.requestAiDraft>[1]) {
    setPhase("sending");
    try {
      receive(await assignmentsApi.requestAiDraft(assignmentId, input));
    } catch (e) {
      setError(errorMessage(e));
      ref.current = requestRef();
    } finally {
      setPhase("idle");
    }
  }

  // Không trường nào bắt buộc (yêu cầu 2026-11-09): bỏ trống thì AI tự quyết; nút Tạo đề không bị khóa.
  async function send() {
    if (countError || pointsError) {
      setError(countError ?? pointsError ?? null);
      return;
    }
    const count = countValid ? countNumber : undefined;
    if (uploading) {
      setError("Hãy đợi tải xong hoặc gỡ các tệp bị lỗi trước khi tạo đề.");
      return;
    }
    applied.current = false;
    const input = {
      request,
      materialIds: chosen,
      sourceFileRefs: sources.fileRefs,
      formatText: templateReady ? template.text : undefined,
      templateAssignmentId: template ? undefined : templateId || undefined,
      // Ngôn ngữ theo ô Ngôn ngữ, ưu tiên hơn ngôn ngữ ghi trong yêu cầu hay đề mẫu.
      options: { profile: "AUTO" as const, language, questionCount: count, targetPoints: pointsValid ? pointsNumber : undefined },
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

  // Đề hợp lệ: ghi thẳng lên trang.
  useEffect(() => {
    if (!proposal || invalid || applied.current) return;
    applied.current = true;
    void apply();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [proposal, invalid]);

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
    sources.clear();
    if (draft && (draft.status === "READY" || draft.status === "PENDING")) {
      await assignmentsApi.discardAiDraft(assignmentId, draft.draftId).catch(() => {});
    }
    onClose();
  }

  const showPreview = proposal !== null && invalid;
  const countNumber = Number(questionCount);
  const countValid = questionCount.trim() !== "" && Number.isInteger(countNumber) && countNumber >= 1 && countNumber <= 10;
  // Số câu hỏi sai hiện đỏ ngay dưới ô; ô trống thì AI tự quyết.
  const countError = questionCount.trim() !== "" && !countValid ? "Số câu hỏi từ 1 đến 10." : undefined;
  // Tổng điểm như luật của server: lớn hơn 0, tối đa 1.000, không quá 2 chữ số thập phân; ô trống thì AI tự chia điểm.
  const pointsNumber = Number(targetPoints);
  const pointsValid = targetPoints.trim() !== "" && Number.isFinite(pointsNumber) && pointsNumber > 0 && pointsNumber <= 1000
    && Math.round(pointsNumber * 100) === pointsNumber * 100;
  const pointsError = targetPoints.trim() !== "" && !pointsValid ? "Tổng điểm lớn hơn 0, tối đa 1.000, tối đa 2 chữ số thập phân." : undefined;
  const working = phase !== "idle" || draft?.status === "PENDING" || (busy && !draft) || (proposal !== null && !invalid);
  // Popup chờ riêng: popup soạn vẫn giữ nguyên trạng thái để quay về khi lỗi.
  if (working) return <AiProgress />;
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
            disabled={awaiting || proposal !== null}
            data-testid="ai-send"
          >
            Tạo đề
          </Button>
        )
      }
    >
      {!showPreview && (
        <>
          {/* Lời dẫn cho người mới dùng (yêu cầu 2026-11-10): ô nào cũng không bắt buộc, đề tạo xong ghi thẳng lên trang. */}
          <p className="text-[13px] leading-relaxed text-muted" data-testid="ai-intro">
            Mô tả đề bạn cần, AI soạn câu hỏi kèm rubric chấm điểm rồi ghi thẳng lên trang; nhấn Ctrl+Z để hoàn tác. Các ô dưới
            đây đều không bắt buộc.
          </p>
          {/* Mẫu đề và tài liệu tham khảo chỉ thêm ngữ cảnh, không bắt buộc (yêu cầu 2026-11-09). */}
          <Field label="Mẫu đề" htmlFor="ai-template">
            <Select id="ai-template" aria-label="Mẫu đề" disabled={awaiting} data-testid="ai-template"
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
              <option value="">Không dùng đề mẫu</option>
              {template && <option value="__file__">{template.file.name}</option>}
              {templates.length > 0 && (
                <optgroup label="Đề mẫu của môn">
                  {templates.map((item) => <option key={item.assignmentId} value={item.assignmentId}>{item.title}</option>)}
                </optgroup>
              )}
              <option value="__upload__">Tải đề mẫu lên…</option>
            </Select>
            <input ref={templateInput} type="file" accept=".docx" hidden aria-label="Tải đề mẫu lên" data-testid="ai-format"
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
            <Field label="Ngôn ngữ" htmlFor="ai-language">
              <Select id="ai-language" aria-label="Ngôn ngữ" value={language} disabled={awaiting}
                onChange={(e) => setLanguage(e.target.value as typeof language)}>
                <option value="EN">English</option>
                <option value="VI">Tiếng Việt</option>
              </Select>
            </Field>
            <Field label="Số câu hỏi" htmlFor="ai-count" error={countError}>
              <input id="ai-count" aria-label="Số câu hỏi" type="number" min={1} max={10} step={1} value={questionCount}
                placeholder="AI tự chọn" disabled={awaiting} onChange={(e) => setQuestionCount(e.target.value)}
                aria-invalid={countError ? true : undefined}
                className={`w-full rounded-lg border px-3 py-2 text-[13px] disabled:cursor-not-allowed disabled:bg-sunken disabled:text-muted ${
                  countError ? "border-danger" : "border-line-strong"}`} />
            </Field>
            <Field label="Tổng điểm" htmlFor="ai-points" error={pointsError}>
              <input id="ai-points" aria-label="Tổng điểm" type="number" min={0.01} max={1000} step={0.5} value={targetPoints}
                placeholder="AI tự chia" disabled={awaiting} onChange={(e) => setTargetPoints(e.target.value)}
                aria-invalid={pointsError ? true : undefined} data-testid="ai-points"
                className={`w-full rounded-lg border px-3 py-2 text-[13px] disabled:cursor-not-allowed disabled:bg-sunken disabled:text-muted ${
                  pointsError ? "border-danger" : "border-line-strong"}`} />
            </Field>
          </div>
          <Field label="Tài liệu tham khảo" htmlFor="ai-reference">
            <ReferencePicker materials={materials} chosen={chosen} uploads={sources.uploads} disabled={awaiting}
              canUpload={sources.canUpload} accept={sources.accept}
              onChoose={(id) => setChosen((c) => (c.includes(id) ? c : [...c, id].slice(0, 10)))}
              onUnchoose={(id) => setChosen((c) => c.filter((item) => item !== id))}
              onUpload={sources.add} onRemoveUpload={sources.remove} onRetryUpload={sources.retry} />
          </Field>
          <Field label="Yêu cầu" htmlFor="ai-request">
            <textarea id="ai-request" rows={4} maxLength={4000} value={request} disabled={awaiting}
              placeholder="Ví dụ: Đề phân tích thiết kế hệ thống quản lý thư viện, có vẽ sơ đồ use case và sơ đồ lớp, độ khó trung bình."
              onChange={(e) => setRequest(e.target.value)}
              className="w-full rounded-lg border border-field px-3 py-2 text-[13px] outline-none focus:outline-2 focus:outline-focus" />
          </Field>
        </>
      )}
      {showPreview && proposal && (
        <div className="flex flex-col gap-4" data-testid="ai-proposal">
          {currentPartCount > 0 && <p className="text-xs text-muted">Bài chưa bị thay đổi.</p>}
          <AiDraftPreview proposal={proposal} targetPoints={pointsValid ? pointsNumber : undefined} />
        </div>
      )}
    </Dialog>
  );
}

/** Popup chờ khi AI đang soạn hay đang ghi đề: chỉ vòng xoay và chữ. */
export function AiProgress() {
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
  onUpload: (files: FileList | null) => void;
  onRemoveUpload: (item: SourceUpload) => void;
  onRetryUpload: (item: SourceUpload) => void;
}) {
  const fileInput = useRef<HTMLInputElement>(null);
  const picked = chosen.flatMap((id) => materials.filter((m) => m.materialId === id));
  // Tên tài liệu cắt ngắn để một dòng chứa được khoảng 4 tài liệu (yêu cầu 2026-11-10); rê chuột xem đủ tên.
  const chip = "inline-flex h-7 max-w-[8.5rem] items-center gap-1.5 rounded-md border border-line bg-sunken pl-2 pr-0.5 text-xs";
  return (
    <>
      {/* Tài liệu đã chọn xếp hàng ngay trong thanh tìm, dài thì xuống dòng (yêu cầu 2026-11-05). */}
      <SearchPicker id={id} label="Tài liệu tham khảo" disabled={disabled} optionIcon="menu_book" multiple
        placeholder="Chọn học liệu của môn hoặc tải tệp lên để AI bám theo"
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
      {uploads.some(item => item.file.type.startsWith("image/")) && <div className="flex flex-wrap gap-2">
        {uploads.filter(item => item.file.type.startsWith("image/")).map(item => <SourceImagePreview key={item.id} file={item.file} />)}
      </div>}
      <input ref={fileInput} aria-label="Tải tài liệu lên" type="file" multiple accept={accept} hidden
        onChange={(event) => { onUpload(event.target.files); event.target.value = ""; }} />
    </>
  );
}
