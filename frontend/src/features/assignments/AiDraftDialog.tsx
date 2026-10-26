"use client";

import { useEffect, useRef, useState } from "react";
import { AiTaskStatus } from "@/shared/ai/AiTaskStatus";
import { getPolicies, uploadFile } from "@/features/files/api";
import {
  acceptAttribute,
  checkFile,
  type PurposePolicy,
} from "@/features/files/purposes";
import { Alert, Button, Dialog, Field, Pill, Select } from "@/shared/ui";
import { assignmentsApi } from "./api";
import { AiDraftPreview } from "./AiDraftPreview";
import { errorMessage, requestRef } from "./format";
import type { AiDraft, AiSourcePreview, GradingMode, Material } from "./types";

type Props = {
  assignmentId: string;
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
type SourceSummary = {
  fileName: string;
  byteSize: number;
  extractedChars: number;
  truncated: boolean;
};

/** t13b Tạo nháp bằng AI (F6; BR-U07-50, 51): đề xuất xem trước, Áp dụng thay khung, Bỏ thì bài không đổi. */
export function AiDraftDialog({
  assignmentId,
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
  const [language, setLanguage] = useState<"VI" | "EN">("VI");
  const [questionCount, setQuestionCount] = useState("3");
  const [targetPoints, setTargetPoints] = useState("10");
  const [materials, setMaterials] = useState<Material[]>([]);
  const [chosen, setChosen] = useState<string[]>([]);
  const [sourceFiles, setSourceFiles] = useState<SourceUpload[]>([]);
  const [sourcePolicy, setSourcePolicy] = useState<PurposePolicy | null>(null);
  const abortUploads = useRef(new Map<string, () => void>());
  const [source, setSource] = useState<"SHARED" | "PERSONAL">("SHARED");
  const [provider, setProvider] = useState("GEMINI");
  const [draft, setDraft] = useState<AiDraft | null>(null);
  const [replaceInstructions, setReplaceInstructions] = useState(
    !currentInstructions.trim(),
  );
  const [replaceTitle, setReplaceTitle] = useState(
    !currentTitle.trim() || currentTitle.trim() === "Bài tập chưa đặt tên",
  );
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [startedAt, setStartedAt] = useState<number | null>(null);
  const [now, setNow] = useState(() => Date.now());
  const ref = useRef(requestRef());
  const awaiting = busy || draft?.status === "PENDING";

  useEffect(() => {
    let active = true;
    const uploads = abortUploads.current;
    getPolicies()
      .then((policies) => {
        if (active)
          setSourcePolicy(
            policies.find((policy) => policy.purpose === "ASSIGNMENT_SOURCE") ??
              null,
          );
      })
      .catch(() => {
        if (active) setError("Không tải được giới hạn tệp nguồn.");
      });
    return () => {
      active = false;
      uploads.forEach((abort) => abort());
      uploads.clear();
    };
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    assignmentsApi.materials(assignmentId, controller.signal).then(
      (list) => {
        if (!controller.signal.aborted)
          setMaterials(list.filter((m) => m.hasText));
      },
      () => {},
    );
    return () => controller.abort();
  }, [assignmentId]);

  useEffect(() => {
    if (!draft || draft.status !== "PENDING") return;
    const timer = setTimeout(() => {
      assignmentsApi
        .aiDraft(assignmentId, draft.draftId)
        .then(setDraft, (e) => setError(errorMessage(e)));
    }, POLL_MS);
    return () => clearTimeout(timer);
  }, [assignmentId, draft]);

  useEffect(() => {
    if (!awaiting || startedAt === null) return;
    const timer = window.setInterval(() => setNow(Date.now()), 1000);
    return () => window.clearInterval(timer);
  }, [awaiting, startedAt]);

  function updateSource(
    id: string,
    update: (item: SourceUpload) => SourceUpload,
  ) {
    setSourceFiles((files) =>
      files.map((item) => (item.id === id ? update(item) : item)),
    );
  }

  async function uploadSource(file: File, id = crypto.randomUUID()) {
    if (!sourcePolicy) {
      setError("Chưa tải được chính sách tệp nguồn.");
      return;
    }
    const issue = checkFile(file, sourcePolicy);
    if (issue) {
      setSourceFiles((files) => [
        ...files,
        { id, file, status: "ERROR", percent: 0, error: issue },
      ]);
      return;
    }
    setSourceFiles((files) =>
      files.some((item) => item.id === id)
        ? files.map((item) =>
            item.id === id
              ? { ...item, status: "UPLOADING", percent: 0, error: undefined }
              : item,
          )
        : [...files, { id, file, status: "UPLOADING", percent: 0 }],
    );
    const handle = uploadFile(file, "ASSIGNMENT_SOURCE", (percent) =>
      updateSource(id, (item) => ({ ...item, percent })),
    );
    const controller = new AbortController();
    abortUploads.current.set(id, () => {
      handle.abort();
      controller.abort();
    });
    try {
      const result = await handle.promise;
      updateSource(id, (item) => ({
        ...item,
        status: "READING",
        percent: 100,
        fileRef: result.fileRef.value,
      }));
      const previews = await assignmentsApi.previewAiSources(
        assignmentId,
        [result.fileRef.value],
        controller.signal,
      );
      if (!previews[0]) throw new Error("Không đọc được nội dung tệp nguồn.");
      updateSource(id, (item) => ({
        ...item,
        status: "READY",
        preview: previews[0],
      }));
    } catch (cause) {
      updateSource(id, (item) => ({
        ...item,
        status: "ERROR",
        error:
          cause instanceof Error
            ? cause.message
            : "Tải tệp lên không thành công",
      }));
    } finally {
      abortUploads.current.delete(id);
    }
  }

  function addSourceFiles(files: FileList | null) {
    if (!files) return;
    const remaining = MAX_SOURCE_FILES - sourceFiles.length;
    if (files.length > remaining) setError("Chỉ chọn tối đa 3 tệp nguồn.");
    Array.from(files)
      .slice(0, Math.max(0, remaining))
      .forEach((file) => void uploadSource(file));
  }

  function removeSource(id: string) {
    abortUploads.current.get(id)?.();
    abortUploads.current.delete(id);
    setSourceFiles((files) => files.filter((item) => item.id !== id));
  }

  async function send() {
    const count = Number(questionCount);
    const target = Number(targetPoints);
    if (
      !Number.isInteger(count) ||
      count < 1 ||
      count > 10 ||
      !Number.isFinite(target) ||
      target <= 0 ||
      target > 1000 ||
      !/^\d+(\.\d{1,2})?$/.test(targetPoints)
    ) {
      setError(
        "Số câu phải từ 1 đến 10; tổng điểm phải lớn hơn 0, tối đa 1.000 và có không quá 2 chữ số thập phân.",
      );
      return;
    }
    if (sourceFiles.some((item) => item.status !== "READY")) {
      setError(
        "Hãy đợi tải xong hoặc gỡ các tệp nguồn bị lỗi trước khi tạo đề.",
      );
      return;
    }
    if (draft?.status === "FAILED") ref.current = requestRef();
    setBusy(true);
    setError(null);
    setStartedAt(Date.now());
    setNow(Date.now());
    try {
      setDraft(
        await assignmentsApi.requestAiDraft(assignmentId, {
          request,
          materialIds: chosen,
          sourceFileRefs: sourceFiles.flatMap((item) =>
            item.fileRef ? [item.fileRef] : [],
          ),
          options: {
            profile: "AUTO",
            language,
            questionCount: count,
            targetPoints: target,
          },
          source,
          provider: source === "PERSONAL" ? provider : undefined,
          requestRef: ref.current,
        }),
      );
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }

  async function apply() {
    if (!draft) return;
    setBusy(true);
    setError(null);
    try {
      const latestVersion = prepareApply ? await prepareApply() : version;
      await assignmentsApi.applyAiDraft(
        assignmentId,
        draft.draftId,
        latestVersion,
        replaceInstructions,
        replaceTitle,
      );
      onApplied();
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }

  async function revise() {
    if (!draft) return;
    setBusy(true);
    try {
      await assignmentsApi.discardAiDraft(assignmentId, draft.draftId);
      setDraft(null);
      setError(null);
      setStartedAt(null);
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
      await assignmentsApi
        .discardAiDraft(assignmentId, draft.draftId)
        .catch(() => {});
    }
    onClose();
  }

  const proposal = draft?.status === "READY" ? draft.proposal : null;
  const ready = proposal !== null;
  const validation = proposal?.validation;
  const provenance = draft?.provenance ?? {};
  const sourceSummaries = (
    Array.isArray(provenance.sourceFiles) ? provenance.sourceFiles : []
  ).filter(
    (item): item is SourceSummary =>
      typeof item === "object" &&
      item !== null &&
      "fileName" in item &&
      typeof item.fileName === "string",
  );
  const invalid = ready && (!validation || validation.status === "INVALID" || proposal.sections.length === 0);
  const elapsed =
    startedAt === null ? 0 : Math.max(0, Math.floor((now - startedAt) / 1000));
  return (
    <Dialog
      open
      size="xl"
      icon="smart_toy"
      title="Tạo nháp bằng AI"
      onClose={() => void discard()}
      dismissible={!busy}
      footer={
        ready ? (
          <>
            <Button onClick={() => void discard()} disabled={busy}>
              Bỏ
            </Button>
            {invalid && <Button onClick={() => void revise()} disabled={busy}>Sửa yêu cầu và tạo lại</Button>}
            <Button
              variant="primary"
              onClick={() => void apply()}
              disabled={busy || invalid}
              data-testid="ai-apply"
            >
              {currentPartCount > 0
                ? "Thay nội dung bài tập"
                : "Thêm vào bài tập"}
            </Button>
          </>
        ) : (
          <>
            <Button onClick={() => void discard()} disabled={busy}>
              Hủy
            </Button>
            <Button
              variant="primary"
              onClick={() => void send()}
              disabled={
                awaiting ||
                sourceFiles.some((item) => item.status !== "READY") ||
                (!request.trim() &&
                  chosen.length === 0 &&
                  sourceFiles.length === 0)
              }
              data-testid="ai-send"
            >
              {draft?.status === "FAILED" ? "Thử yêu cầu mới" : "Tạo đề xuất"}
            </Button>
          </>
        )
      }
    >
      {error && (
        <Alert tone="danger" role="alert">
          {error}
        </Alert>
      )}
      {!ready && (
        <>
          <div className="grid gap-3 sm:grid-cols-2">
            <Field label="Ngôn ngữ đề" htmlFor="ai-language">
              <Select
                id="ai-language"
                aria-label="Ngôn ngữ đề"
                value={language}
                disabled={awaiting}
                onChange={(e) => setLanguage(e.target.value as typeof language)}
              >
                <option value="VI">Tiếng Việt</option>
                <option value="EN">English</option>
              </Select>
            </Field>
            <Field
              label="Số yêu cầu chính"
              htmlFor="ai-count"
              hint="Từ 1 đến 10; mỗi yêu cầu chính có thể gồm nhiều ý nhỏ."
            >
              <input
                id="ai-count"
                aria-label="Số yêu cầu chính"
                type="number"
                min={1}
                max={10}
                step={1}
                value={questionCount}
                disabled={awaiting}
                onChange={(e) => setQuestionCount(e.target.value)}
                className="w-full rounded-lg border border-line-strong px-3 py-2 text-[13px]"
              />
            </Field>
            <Field
              label="Tổng điểm mục tiêu"
              htmlFor="ai-total"
              hint="Điểm rubric của các câu con phải cộng đúng tổng này."
            >
              <input
                id="ai-total"
                aria-label="Tổng điểm mục tiêu"
                type="number"
                min={0.01}
                max={1000}
                step={0.01}
                value={targetPoints}
                disabled={awaiting}
                onChange={(e) => setTargetPoints(e.target.value)}
                className="w-full rounded-lg border border-line-strong px-3 py-2 text-[13px]"
              />
            </Field>
          </div>
          <Field
            label="Yêu cầu"
            htmlFor="ai-request"
            hint="Nhập chủ đề, bối cảnh và yêu cầu riêng; hoặc chỉ chọn tài liệu nguồn bên dưới."
          >
            <textarea
              id="ai-request"
              rows={4}
              maxLength={4000}
              value={request}
              disabled={awaiting}
              onChange={(e) => setRequest(e.target.value)}
              className="w-full rounded-lg border border-field px-3 py-2 text-[13px] outline-none focus:outline-2 focus:outline-focus"
            />
          </Field>
          <Field
            label="Tệp nguồn riêng cho AI"
            hint={
              sourcePolicy
                ? `Tối đa 3 tệp · ${sourcePolicy.allowedTypes.join(", ")} · ${sourcePolicy.maxSizeMb} MB mỗi tệp · tổng chữ trích tối đa 30.000 ký tự. PDF scan chưa hỗ trợ.`
                : "Đang tải giới hạn tệp…"
            }
          >
            <input
              aria-label="Tệp nguồn riêng cho AI"
              type="file"
              multiple
              accept={sourcePolicy ? acceptAttribute(sourcePolicy) : undefined}
              disabled={
                !sourcePolicy ||
                sourceFiles.length >= MAX_SOURCE_FILES ||
                awaiting
              }
              onChange={(event) => {
                addSourceFiles(event.target.files);
                event.target.value = "";
              }}
              className="w-full text-xs file:mr-3 file:rounded-lg file:border file:border-line-strong file:bg-surface file:px-3 file:py-2"
            />
            {sourceFiles.length > 0 && (
              <ul
                className="mt-2 flex flex-col gap-2"
                aria-label="Tệp nguồn đã chọn"
              >
                {sourceFiles.map((item) => (
                  <li
                    key={item.id}
                    className="flex flex-col gap-2 rounded-lg border border-line px-3 py-2 text-xs"
                  >
                    <div className="flex items-center gap-2">
                      <span
                        className="min-w-0 flex-1 truncate"
                        title={item.file.name}
                      >
                        {item.file.name} ·{" "}
                        {(item.file.size / 1024 / 1024).toFixed(2)} MB
                      </span>
                      {item.status === "UPLOADING" && (
                        <span role="status">Đang tải {item.percent}%</span>
                      )}
                      {item.status === "READING" && (
                        <span role="status">Đang đọc nội dung…</span>
                      )}
                      {item.status === "READY" && (
                        <Pill tone="success">
                          Đã đọc · {item.preview?.extractedChars} ký tự
                        </Pill>
                      )}
                      {item.status === "ERROR" && (
                        <span className="text-danger">{item.error}</span>
                      )}
                      {item.status === "ERROR" && (
                        <Button
                          size="sm"
                          onClick={() => void uploadSource(item.file, item.id)}
                        >
                          Thử lại
                        </Button>
                      )}
                      <Button
                        size="sm"
                        aria-label={`Gỡ ${item.file.name}`}
                        onClick={() => removeSource(item.id)}
                      >
                        Gỡ
                      </Button>
                      {item.status === "UPLOADING" && (
                        <div className="h-1 w-16 overflow-hidden rounded bg-shell">
                          <div
                            className="h-full bg-primary"
                            style={{ width: `${item.percent}%` }}
                          />
                        </div>
                      )}
                    </div>
                    {item.preview && item.status === "READY" && (
                      <details>
                        <summary className="cursor-pointer text-primary-ink">
                          Xem chữ đã đọc
                          {item.preview.truncated ? " · đã rút gọn" : ""}
                        </summary>
                        <pre className="mt-2 max-h-36 overflow-auto whitespace-pre-wrap rounded bg-canvas p-2 font-sans text-muted">
                          {item.preview.previewText}
                        </pre>
                      </details>
                    )}
                  </li>
                ))}
              </ul>
            )}
            {sourceFiles.reduce(
              (total, item) => total + (item.preview?.extractedChars ?? 0),
              0,
            ) > 30000 && (
              <p className="mt-2 text-xs text-warning">
                Tổng chữ vượt 30.000 ký tự. Khi tạo đề, nguồn được rút gọn theo
                thứ tự đã chọn.
              </p>
            )}
          </Field>
          <Field
            label="Tài liệu tham khảo"
            hint="Chỉ học liệu đã trích được chữ."
          >
            {materials.length === 0 ? (
              <p className="text-xs text-muted">Chưa có học liệu dùng được.</p>
            ) : (
              <div className="flex max-h-32 flex-col gap-1 overflow-y-auto">
                {materials.map((m) => (
                  <label
                    key={m.materialId}
                    className="flex items-center gap-2 text-[13px]"
                  >
                    <input
                      type="checkbox"
                      checked={chosen.includes(m.materialId)}
                      onChange={(e) =>
                        setChosen((c) =>
                          e.target.checked
                            ? [...c, m.materialId].slice(0, 10)
                            : c.filter((id) => id !== m.materialId),
                        )
                      }
                    />
                    {m.title}
                  </label>
                ))}
              </div>
            )}
          </Field>
          <Field label="Nguồn AI">
            <div className="flex flex-wrap items-center gap-2">
              <Select
                aria-label="Nguồn AI"
                value={source}
                disabled={awaiting}
                wrapperClassName="w-56"
                onChange={(e) =>
                  setSource(e.target.value as "SHARED" | "PERSONAL")
                }
              >
                <option value="SHARED">API chung (quota giảng viên)</option>
                <option value="PERSONAL">API cá nhân</option>
              </Select>
              {source === "PERSONAL" && (
                <Select
                  aria-label="Nhà cung cấp"
                  value={provider}
                  disabled={awaiting}
                  wrapperClassName="w-36"
                  onChange={(e) => setProvider(e.target.value)}
                >
                  <option value="GEMINI">Gemini</option>
                  <option value="OPENAI">OpenAI</option>
                  <option value="CLAUDE">Claude</option>
                </Select>
              )}
            </div>
          </Field>
          {awaiting && (
            <div
              role="status"
              aria-busy="true"
              className="flex items-center gap-2 rounded-lg bg-canvas p-3 text-sm text-muted"
              data-testid="ai-progress"
            >
              <span
                aria-hidden="true"
                className="size-4 animate-spin rounded-full border-2 border-line-strong border-t-primary motion-reduce:animate-none"
              />
              {busy
                ? "Đang gửi yêu cầu và đọc nguồn…"
                : `AI đang soạn đề · chế độ ${gradingMode}`}{" "}
              {elapsed > 0 && <span>{elapsed} giây</span>}
            </div>
          )}
          {draft?.taskId && <AiTaskStatus taskId={draft.taskId} />}
          {draft?.status === "FAILED" && (
            <Alert tone="warning">
              {draft.safeError ?? "AI không trả kết quả."} Bạn vẫn soạn thủ công
              được.
            </Alert>
          )}
        </>
      )}
      {proposal && (
        <div className="flex flex-col gap-5" data-testid="ai-proposal">
          <AiDraftPreview proposal={proposal} targetPoints={Number(targetPoints)} />
          <section className="space-y-3 rounded-xl border border-line p-5" aria-label="Nội dung sẽ áp dụng">
            <h3 className="text-[15px] font-bold text-ink">Nội dung sẽ áp dụng</h3>
            {currentPartCount > 0 && <Alert tone="warning">Sẽ thay toàn bộ câu hỏi và rubric hiện tại ({currentPartCount} mục).</Alert>}
            {proposal.title && <label className="flex items-center gap-2 text-sm"><input type="checkbox" className="accent-[var(--c-primary)]" checked={replaceTitle} onChange={(e) => setReplaceTitle(e.target.checked)} />Thay tiêu đề bài bằng tiêu đề đề xuất</label>}
            {proposal.instructions && <label className="flex items-center gap-2 text-sm"><input type="checkbox" className="accent-[var(--c-primary)]" checked={replaceInstructions} onChange={(e) => setReplaceInstructions(e.target.checked)} />{currentInstructions.trim() ? "Thay hướng dẫn làm bài hiện tại" : "Thêm hướng dẫn làm bài do AI tạo"}</label>}
            {proposal.instructions && currentInstructions.trim() && <details className="rounded-lg bg-canvas p-3 text-xs">
              <summary className="cursor-pointer font-semibold">Xem hướng dẫn hiện tại để đối chiếu</summary>
              <p className="mt-2 whitespace-pre-wrap">{currentInstructions}</p>
            </details>}
          </section>
          {sourceSummaries.length > 0 && <section className="rounded-xl border border-line p-4" aria-label="Nguồn đã đọc">
            <h3 className="text-sm font-semibold">Nguồn đã đọc</h3>
            <ul className="mt-2 space-y-1 text-xs text-muted">{sourceSummaries.map((item) => <li key={item.fileName}>{item.fileName} · {item.extractedChars} ký tự{item.truncated ? " · đã rút gọn trong giới hạn 30.000 ký tự" : ""}</li>)}</ul>
          </section>}
          <p className="flex flex-wrap items-center gap-1 text-xs text-muted">
            Nguồn: <Pill>{String(provenance.origin ?? "AI")}</Pill>
            {typeof provenance.provider === "string" && <Pill>{provenance.provider}</Pill>}
            {typeof provenance.model === "string" && <Pill>{provenance.model}</Pill>}
            {typeof provenance.agentLabel === "string" && <Pill>{provenance.agentLabel}</Pill>}
          </p>
        </div>
      )}
    </Dialog>
  );
}
