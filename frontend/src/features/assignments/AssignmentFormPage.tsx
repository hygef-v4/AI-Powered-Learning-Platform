"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { uploadFile } from "@/features/files/api";
import { ApiError } from "@/shared/api/client";
import {
  AutoTextarea,
  DocumentEditor,
  RubricNote,
  ToolbarButton,
  computeParts,
  diagramTypeLabels,
  formatPoints,
  rubricComplete,
  rubricTotal,
  sectionsOf,
  type DiagramType,
  type DocumentBlock,
  type DocumentComment,
  type DocumentModel,
  type DocumentPart,
  type RubricCriterion,
  type ValidationIssue,
} from "@/shared/document";
import { Alert, Button, Dialog, Field, Menu, MenuItem, PageHeader, Pill, cx } from "@/shared/ui";
import { AiDraftDialog } from "./AiDraftDialog";
import { assignmentsApi, docxUrl, imageUrlFor } from "./api";
import { AssignmentSettingsPanel, settingsOf, type Settings } from "./AssignmentSettingsPanel";
import { errorMessage, formatTime, problemCode, problemIssues } from "./format";
import { RubricEditor, cleanCriteria, rubricPointsMalformed } from "./RubricEditor";
import { ScheduleForm } from "./ScheduleForm";
import { gradingLabels, statusLabels, statusTones, submissionLabels, type AssignmentDetail, type RubricDetail } from "./types";

const AUTOSAVE_MS = 1500;
const VERSION_CONFLICT = "ASSIGNMENT_VERSION_CONFLICT";
const NOT_READY = "ASSIGNMENT_NOT_READY";
const POINTS_INVALID = "Điểm mỗi mục tối đa 2 chữ số thập phân";

type Local = {
  title: string;
  instructions: string;
  settings: Settings | null;
  skeleton: DocumentModel;
  criteria: Record<string, RubricCriterion[]>;
};

/** Sau khi lưu: ảnh vừa tải lên đã được gắn (server trả {@code hasFile}); bản đang sửa bỏ {@code fileRef} để không gắn lại. */
export function mergeSaved(local: DocumentModel, saved: DocumentModel): DocumentModel {
  const persisted = new Set(saved.blocks.filter((b) => b.type === "IMAGE" && b.content.hasFile).map((b) => b.id));
  return {
    blocks: local.blocks.map((block): DocumentBlock => block.type === "IMAGE" && block.content.fileRef && persisted.has(block.id)
      ? { ...block, content: { ...block.content, fileRef: undefined, hasFile: true } }
      : block),
  };
}

/**
 * Báo chung chung khi xếp lịch mà bài còn thiếu (yêu cầu 2026-10-22): chỉ ghi trạng thái; chỗ thiếu tô đỏ ngay trong bài
 * (block, thẻ rubric, tên bài), chỉ lỗi không gắn được vào đâu (cài đặt, nhóm…) mới ghi kèm.
 */
export function missingText(issues: ValidationIssue[]): string {
  const loose = issues.filter((issue) => !issue.blockId && issue.code !== "TITLE_REQUIRED" && !issue.code.startsWith("RUBRIC"));
  return ["Bài còn chỗ chưa đủ", ...loose.map((issue) => issue.message)].join("; ");
}

/** Chạy {@code task} sau mọi lệnh ghi đang chờ (lưu, duyệt…) để lệnh nào cũng gửi đúng version của bài. */
function chain<T>(queue: { current: Promise<unknown> }, task: () => Promise<T>): Promise<T> {
  const run = queue.current.then(task, task);
  queue.current = run.catch(() => undefined);
  return run;
}

/**
 * #26 Assignment Form (UC 35, 36): một trang kiểu Google Docs, sửa khung tài liệu và rubric cùng lúc. Rubric atomic của
 * mỗi mục (tiêu đề lá của cây tiêu đề) nằm ở lề phải, bắt đầu ngang tiêu đề và dính trên màn hình suốt mục đó; tự lưu
 * sau 1,5 giây. Thứ tự (yêu cầu 2026-10-21, 2026-10-22): soạn đề và rubric → (tùy chọn) xem trước như sinh viên → xếp
 * lịch; nút "Xếp lịch" có ngay trên trang soạn, phát hành thẳng từ bản nháp (không còn trạng thái Đã duyệt).
 */
export function AssignmentFormPage({ id }: { id: string }) {
  const router = useRouter();
  const params = useSearchParams();
  const [detail, setDetail] = useState<AssignmentDetail | null>(null);
  const [loadError, setLoadError] = useState<unknown>(null);
  const [title, setTitle] = useState("");
  const [instructions, setInstructions] = useState("");
  const [settings, setSettings] = useState<Settings | null>(null);
  const [skeleton, setSkeleton] = useState<DocumentModel>({ blocks: [] });
  const [criteria, setCriteria] = useState<Record<string, RubricCriterion[]>>({});
  const [rubrics, setRubrics] = useState<Record<string, RubricDetail>>({});
  const [rubricLoadError, setRubricLoadError] = useState<string | null>(null);
  const [rubricEpoch, setRubricEpoch] = useState(0);
  const [tick, setTick] = useState(0);
  const [unsaved, setUnsaved] = useState(false);
  const [saving, setSaving] = useState(false);
  const [savedAt, setSavedAt] = useState<string | null>(null);
  const [conflict, setConflict] = useState(false);
  const [issues, setIssues] = useState<ValidationIssue[]>([]);
  const [message, setMessage] = useState<{ tone: "danger" | "success"; text: string } | null>(null);
  const [activePart, setActivePart] = useState<string | null>(null);
  const [dialog, setDialog] = useState<"ai" | "retire" | "delete" | "settings" | "schedule" | null>(null);
  const [preview, setPreview] = useState(false);
  const version = useRef(0);
  const detailRef = useRef<AssignmentDetail | null>(null);
  // Bản mới nhất người dùng đã gõ, cập nhật ngay trong lúc sửa để lần lưu đang chờ không gửi bản cũ.
  const latest = useRef<Local>({ title, instructions, settings, skeleton, criteria });
  const dirty = useRef({ details: false, skeleton: false, rubrics: new Set<string>() });
  const queue = useRef<Promise<unknown>>(Promise.resolve());
  const conflictRef = useRef(false);
  const fetched = useRef({ epoch: -1, parts: new Set<string>() });
  const startedAiFromCreate = useRef(false);

  const apply = useCallback((next: AssignmentDetail, replaceLocal: boolean) => {
    setDetail(next);
    detailRef.current = next;
    version.current = next.version;
    if (!replaceLocal) return;
    const nextSettings = settingsOf(next);
    setTitle(next.title);
    setInstructions(next.instructions);
    setSettings(nextSettings);
    setSkeleton(next.skeleton);
    latest.current = { ...latest.current, title: next.title, instructions: next.instructions, settings: nextSettings, skeleton: next.skeleton };
    dirty.current = { ...dirty.current, details: false, skeleton: false };
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    assignmentsApi.detail(id, controller.signal).then(
      (next) => { if (!controller.signal.aborted) apply(next, true); },
      (error) => { if (!controller.signal.aborted) setLoadError(error); },
    );
    return () => controller.abort();
  }, [apply, id]);

  useEffect(() => {
    if (params.get("startAI") !== "1" || !detail?.permissions.canEditContent || startedAiFromCreate.current) return;
    startedAiFromCreate.current = true;
    setDialog("ai");
    const query = new URLSearchParams(params.toString());
    query.delete("startAI");
    const suffix = query.toString();
    router.replace(`/assignments/${encodeURIComponent(id)}${suffix ? `?${suffix}` : ""}`, { scroll: false });
  }, [detail, id, params, router]);

  // Rubric của các mục đã có trên server; mục mới chỉ tải phần chưa có, rubric đang sửa dở giữ bản đang sửa.
  const partsKey = detail?.parts.map((part) => part.partId).join("|") ?? "";
  useEffect(() => {
    if (fetched.current.epoch !== rubricEpoch) fetched.current = { epoch: rubricEpoch, parts: new Set() };
    const missing = partsKey.split("|").filter((partId) => partId && !fetched.current.parts.has(partId));
    if (missing.length === 0) return;
    missing.forEach((partId) => fetched.current.parts.add(partId));
    const controller = new AbortController();
    let done = false;
    Promise.all(missing.map((partId) => assignmentsApi.rubric(id, partId, controller.signal))).then((list) => {
      done = true;
      if (controller.signal.aborted) return;
      setRubricLoadError(null);
      setRubrics((current) => ({ ...current, ...Object.fromEntries(list.map((rubric) => [rubric.partId, rubric])) }));
      const fresh = Object.fromEntries(list.filter((rubric) => !dirty.current.rubrics.has(rubric.partId)).map((rubric) => [rubric.partId, rubric.criteria]));
      latest.current = { ...latest.current, criteria: { ...latest.current.criteria, ...fresh } };
      setCriteria((current) => ({ ...current, ...fresh }));
    }, (error) => {
      done = true;
      missing.forEach((partId) => fetched.current.parts.delete(partId));
      if (!controller.signal.aborted) setRubricLoadError(errorMessage(error));
    });
    return () => {
      controller.abort();
      if (!done) missing.forEach((partId) => fetched.current.parts.delete(partId));
    };
  }, [id, partsKey, rubricEpoch]);

  const save = useCallback((): Promise<boolean> => chain(queue, async () => {
    const work = dirty.current;
    if (conflictRef.current) return false;
    if (!work.details && !work.skeleton && work.rubrics.size === 0) return true;
    dirty.current = { details: false, skeleton: false, rubrics: new Set() };
    const values = latest.current;
    let next: AssignmentDetail | null = null;
    const malformed: ValidationIssue[] = [];
    setSaving(true);
    try {
      if (work.details && values.settings) {
        const s = values.settings;
        next = await assignmentsApi.updateDetails(id, version.current, {
          title: values.title, instructions: values.instructions, gradingMode: s.gradingMode, diagramEnabled: s.diagramEnabled,
          requiredDiagrams: s.requiredDiagrams, activityMonitoringEnabled: s.activityMonitoringEnabled,
          activityNotice: s.activityNotice, referenceMaterialIds: s.referenceMaterialIds,
        });
        version.current = next.version;
      }
      if (work.skeleton) {
        next = await assignmentsApi.saveSkeleton(id, version.current, values.skeleton);
        version.current = next.version;
        const saved = next.skeleton;
        latest.current = { ...latest.current, skeleton: mergeSaved(latest.current.skeleton, saved) };
        setSkeleton((current) => mergeSaved(current, saved));
      }
      if (work.rubrics.size > 0) {
        const onServer = new Set((next ?? detailRef.current)?.parts.map((part) => part.partId));
        const localParts = computeParts(latest.current.skeleton);
        const local = new Set(localParts.map((part) => part.partId));
        let changed = false;
        for (const partId of work.rubrics) {
          if (!onServer.has(partId)) {
            // Mục vừa thêm, khung chưa lưu được: giữ lại để lưu sau khung; mục đã bị xóa thì bỏ.
            if (local.has(partId)) dirty.current.rubrics.add(partId);
            continue;
          }
          const partCriteria = latest.current.criteria[partId] ?? [];
          if (rubricPointsMalformed(partCriteria)) {
            // Điểm quá 2 chữ số thập phân: không làm tròn, không gửi; giữ chưa lưu tới khi sửa (BR-U07-21).
            dirty.current.rubrics.add(partId);
            const headingBlockId = localParts.find((part) => part.partId === partId)?.headingBlockId ?? null;
            malformed.push({ code: "RUBRIC_POINTS_INVALID", blockId: headingBlockId, message: POINTS_INVALID });
            continue;
          }
          const saved = await assignmentsApi.saveRubric(id, partId, version.current, cleanCriteria(partCriteria));
          version.current = saved.assignmentVersion;
          setRubrics((current) => ({ ...current, [partId]: saved }));
          changed = true;
        }
        if (changed) {
          // Lưu rubric đổi trạng thái bài (đã duyệt về nháp) và điểm của mục: lấy lại bài.
          next = await assignmentsApi.detail(id);
          version.current = next.version;
        }
      }
      if (next) apply(next, false);
      if (malformed.length > 0) {
        setIssues((current) => [...current.filter((issue) => issue.code !== "RUBRIC_POINTS_INVALID"), ...malformed]);
        setMessage({ tone: "danger", text: `Chưa lưu: ${POINTS_INVALID}` });
        return false;
      }
      setSavedAt(new Date().toISOString());
      setIssues((current) => current.filter((issue) => issue.code.startsWith("RUBRIC")));
      setMessage((current) => (current?.tone === "danger" ? null : current));
      return true;
    } catch (error) {
      if (next) apply(next, false);
      const transient = !(error instanceof ApiError) || error.problem.status >= 500;
      if (problemCode(error) === VERSION_CONFLICT || transient) {
        // Giữ đánh dấu chưa lưu: lỗi mạng thì lần lưu sau gửi lại; xung đột thì chờ người dùng tải lại.
        dirty.current.details ||= work.details;
        dirty.current.skeleton ||= work.skeleton;
        work.rubrics.forEach((partId) => dirty.current.rubrics.add(partId));
      }
      if (problemCode(error) === VERSION_CONFLICT) {
        conflictRef.current = true;
        setConflict(true);
      } else {
        setIssues(problemIssues(error));
        setMessage({ tone: "danger", text: `Chưa lưu: ${errorMessage(error)}` });
      }
      return false;
    } finally {
      setSaving(false);
      setUnsaved(dirty.current.details || dirty.current.skeleton || dirty.current.rubrics.size > 0);
    }
  }), [apply, id]);

  useEffect(() => {
    if (tick === 0) return;
    const timer = setTimeout(() => void save(), AUTOSAVE_MS);
    return () => clearTimeout(timer);
  }, [tick, save]);

  useEffect(() => {
    const keys = (event: KeyboardEvent) => {
      if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === "s") {
        event.preventDefault();
        void save();
      }
    };
    const leave = (event: BeforeUnloadEvent) => {
      const pending = dirty.current;
      if (pending.details || pending.skeleton || pending.rubrics.size > 0) event.preventDefault();
    };
    window.addEventListener("keydown", keys);
    window.addEventListener("beforeunload", leave);
    return () => {
      window.removeEventListener("keydown", keys);
      window.removeEventListener("beforeunload", leave);
    };
  }, [save]);

  const reload = useCallback(() => chain(queue, async () => {
    try {
      const next = await assignmentsApi.detail(id);
      conflictRef.current = false;
      dirty.current = { details: false, skeleton: false, rubrics: new Set() };
      apply(next, true);
      setConflict(false);
      setIssues([]);
      setLoadError(null);
      setUnsaved(false);
      setRubricEpoch((epoch) => epoch + 1);
    } catch (error) {
      setLoadError(error);
    }
  }), [apply, id]);

  const parts = useMemo(() => computeParts(skeleton), [skeleton]);
  const sections = useMemo(() => sectionsOf(skeleton, parts), [skeleton, parts]);

  function changed() {
    setUnsaved(true);
    setTick((value) => value + 1);
  }
  function editTitle(value: string) {
    setTitle(value);
    setIssues((current) => current.filter((issue) => issue.code !== "TITLE_REQUIRED"));
    latest.current = { ...latest.current, title: value };
    dirty.current.details = true;
    changed();
  }
  function editInstructions(value: string) {
    setInstructions(value);
    latest.current = { ...latest.current, instructions: value };
    dirty.current.details = true;
    changed();
  }
  function editSettings(value: Settings) {
    setSettings(value);
    latest.current = { ...latest.current, settings: value };
    dirty.current.details = true;
    changed();
  }
  function editSkeleton(value: DocumentModel) {
    setSkeleton(value);
    latest.current = { ...latest.current, skeleton: value };
    dirty.current.skeleton = true;
    changed();
  }
  function editRubric(part: DocumentPart, value: RubricCriterion[]) {
    setCriteria((current) => ({ ...current, [part.partId]: value }));
    latest.current = { ...latest.current, criteria: { ...latest.current.criteria, [part.partId]: value } };
    dirty.current.rubrics.add(part.partId);
    setIssues((current) => current.filter((issue) => !(issue.code.startsWith("RUBRIC") && issue.blockId === part.headingBlockId)));
    changed();
  }

  /** Lưu bản đang sửa rồi mới gọi thao tác (duyệt, nhân bản…), nối vào hàng ghi để dùng đúng version. */
  async function act(action: () => Promise<AssignmentDetail | void>, success: string | null, after?: () => void) {
    setMessage(null);
    if (!(await save())) return;
    try {
      const result = await chain(queue, action);
      if (result) apply(result, false);
      setIssues([]);
      if (success) setMessage({ tone: "success", text: success });
      after?.();
    } catch (error) {
      if (problemCode(error) === VERSION_CONFLICT) {
        conflictRef.current = true;
        setConflict(true);
        return;
      }
      setIssues(problemIssues(error));
      setMessage({ tone: "danger", text: errorMessage(error) });
    }
  }

  async function exportDocx(what: "skeleton" | "rubric") {
    if (await save()) window.location.assign(docxUrl(id, what));
  }

  /** Xem trước (tùy chọn): lưu bản đang sửa rồi xem đề như sinh viên thấy khi làm bài (không có rubric). */
  async function openPreview() {
    setMessage(null);
    if (await save()) {
      setPreview(true);
      window.scrollTo({ top: 0 });
    }
  }

  /** Bài còn thiếu mà trang tự thấy được (tên bài, rubric các phần); luật đầy đủ server kiểm khi phát hành. */
  function localMissing(): ValidationIssue[] {
    const found: ValidationIssue[] = [];
    if (!title.trim()) found.push({ code: "TITLE_REQUIRED", blockId: null, message: "Bài cần tiêu đề" });
    for (const part of parts) {
      if (!rubricComplete(criteria[part.partId] ?? [])) found.push({ code: "RUBRIC_INCOMPLETE", blockId: part.headingBlockId, message: "Rubric chưa đủ" });
    }
    return found;
  }

  /** Bài còn thiếu: về trang soạn, chỗ thiếu tô đỏ, báo chung chung. */
  function showMissing(found: ValidationIssue[]) {
    setPreview(false);
    setIssues(found);
    setMessage({ tone: "danger", text: missingText(found) });
  }

  /**
   * "Xếp lịch" ngay trên trang soạn hoặc trang xem trước (yêu cầu 2026-10-22, bỏ trạng thái Đã duyệt): lưu, kiểm sơ bộ
   * tên bài và rubric rồi mở lịch; phát hành thẳng từ bản nháp, server kiểm đủ luật duyệt (BR-U07-32) khi phát hành.
   */
  async function openSchedule() {
    setMessage(null);
    if (!(await save())) {
      setPreview(false);
      return;
    }
    const found = localMissing();
    if (found.length > 0) {
      showMissing(found);
      return;
    }
    setDialog("schedule");
  }

  if (loadError) {
    return <Alert tone="danger" role="alert" action={<Button size="sm" onClick={() => void reload()}>Thử lại</Button>}>{errorMessage(loadError)}</Alert>;
  }
  if (!detail || !settings) return <p role="status" className="text-sm text-muted">Đang tải bài tập…</p>;

  const classId = params.get("classId") ?? detail.classId;
  const backHref = detail.scope === "SUBJECT" && !classId ? `/manager/assignments?subjectId=${encodeURIComponent(detail.subjectId)}`
    : classId ? `/teaching/classes/${encodeURIComponent(classId)}` : "/teaching/classes";
  const permissions = detail.permissions;
  const editable = permissions.canEditContent && !preview;
  const missing = parts.filter((part) => !rubricComplete(criteria[part.partId] ?? [])).length;
  const totalPoints = parts.reduce((sum, part) => sum + rubricTotal(criteria[part.partId] ?? []), 0);
  const waitingForRubrics = detail.parts.some((part) => !Object.hasOwn(criteria, part.partId));
  const topLevelQuestionCount = skeleton.blocks.filter((block) => block.type === "HEADING" && block.content.style === "H1").length;
  const rubricCriterionCount = Object.values(criteria).reduce((sum, list) => sum + list.length, 0);
  const rubricItemCount = Object.values(criteria).reduce((sum, list) => sum + list.reduce((partSum, criterion) => partSum + criterion.items.length, 0), 0);
  const active = parts.find((part) => part.partId === activePart);
  const titleMissing = issues.some((issue) => issue.code === "TITLE_REQUIRED");
  const required = Object.entries(settings.requiredDiagrams) as [DiagramType, number][];
  const saveText = !permissions.canEditContent ? "Chỉ xem" : saving ? "Đang lưu…" : conflict ? "Chưa lưu"
    : unsaved ? "Chưa lưu" : savedAt ? `Đã lưu lúc ${formatTime(savedAt)}` : "Đã lưu";

  const comments: DocumentComment[] = parts.map((part) => {
    const list = criteria[part.partId] ?? [];
    const locked = !!rubrics[part.partId]?.locked;
    const awaitingRubric = detail.parts.some((saved) => saved.partId === part.partId) && !Object.hasOwn(criteria, part.partId);
    return {
      key: part.partId,
      anchorBlockId: part.headingBlockId,
      content: awaitingRubric ? <p role="status" className="rounded-lg border border-line bg-white p-3 text-xs text-muted">
        {rubricLoadError ? "Chưa tải được tiêu chí chấm." : "Đang tải tiêu chí chấm…"}
      </p> : (
        <RubricNote title={part.title} criteria={list} active={activePart === part.partId} onActivate={() => setActivePart(part.partId)}
          issues={issues.filter((issue) => issue.code.startsWith("RUBRIC") && issue.blockId === part.headingBlockId).map((issue) => issue.message)}
          badge={locked ? <Pill>Đã khóa</Pill> : undefined}
          editor={editable && !locked
            ? <RubricEditor criteria={list} onChange={(value) => editRubric(part, value)} />
            : undefined} />
      ),
    };
  });

  const header = (
    <div className="mb-5 flex flex-col gap-2 border-b border-line pb-4">
      {editable ? (
        <AutoTextarea id="assignment-title" aria-label="Tên bài" value={title} maxLength={200} placeholder="Tên bài tập"
          aria-invalid={titleMissing || undefined}
          className={cx("text-[26px] leading-tight font-bold text-ink", titleMissing && "rounded-md bg-danger-soft")}
          onKeyDown={(event) => { if (event.key === "Enter") event.preventDefault(); }}
          onChange={(event) => editTitle(event.target.value.replace(/\n/g, " "))} />
      ) : <h2 className="text-[26px] leading-tight font-bold text-ink">{title || "Bài chưa đặt tên"}</h2>}
      {editable ? (
        <Field label="Hướng dẫn làm và nộp bài" htmlFor="assignment-instructions" hint="Quy định áp dụng cho toàn bài: thời lượng, cách trình bày và tệp cần nộp.">
          <AutoTextarea id="assignment-instructions" aria-label="Hướng dẫn làm và nộp bài" value={instructions} maxLength={20000}
            placeholder="Ví dụ: Làm cá nhân trong 90 phút; nộp báo cáo PDF và tệp sơ đồ."
            className="text-sm leading-[1.65] text-body" onChange={(event) => editInstructions(event.target.value)} />
        </Field>
      ) : instructions && <p className="text-sm leading-[1.65] whitespace-pre-wrap text-body">{instructions}</p>}
      <p className="flex flex-wrap items-center gap-1.5 text-xs text-muted">
        <Pill>{submissionLabels[detail.submissionMode]}</Pill>
        <Pill tone={settings.gradingMode === "GRADED" ? "info" : "accent"}>{gradingLabels[settings.gradingMode]}</Pill>
        <Pill>{topLevelQuestionCount} câu · {parts.length} mục chấm</Pill>
        {!preview && (waitingForRubrics ? <Pill>{rubricLoadError ? "Chưa tải được điểm" : "Đang tải tiêu chí chấm…"}</Pill>
          : <><Pill>{rubricCriterionCount} tiêu chí · {rubricItemCount} mục điểm</Pill><Pill tone={missing > 0 ? "warning" : "neutral"}>{formatPoints(totalPoints)} điểm</Pill></>)}
        {!preview && !waitingForRubrics && missing > 0 && <Pill tone="warning">{missing} mục cần bổ sung rubric</Pill>}
        {required.map(([type, count]) => <Pill key={type}>{count} {diagramTypeLabels[type]}</Pill>)}
      </p>
    </div>
  );

  return (
    <div className="flex flex-col gap-3">
      <PageHeader backHref={backHref}
        title={<>{title || detail.title || "Bài chưa đặt tên"}<Pill tone={statusTones[detail.status]}>{statusLabels[detail.status]}</Pill></>}
        breadcrumb={[{ label: detail.ownerName || (detail.scope === "SUBJECT" ? "Bài của môn" : "Bài của lớp") }, { label: `Version ${detail.versionNo}` }]}
        subtitle={<span data-testid="save-state">{saveText}</span>}
        actions={<>
          <Menu label="Khác" align="right">
            <MenuItem icon="download" onClick={() => void exportDocx("skeleton")}>Xuất DOCX</MenuItem>
            <MenuItem icon="download" onClick={() => void exportDocx("rubric")}>Xuất rubric</MenuItem>
            {permissions.canClone && (
              <MenuItem icon="add" onClick={() => void act(async () => {
                const copy = await assignmentsApi.clone(id);
                router.push(`/assignments/${copy.id}${classId ? `?classId=${classId}` : ""}`);
              }, null)}>Nhân bản</MenuItem>
            )}
            {permissions.canCreateVersion && (
              <MenuItem icon="history" onClick={() => void act(async () => {
                const copy = await assignmentsApi.newVersion(id);
                router.push(`/assignments/${copy.id}${classId ? `?classId=${classId}` : ""}`);
              }, null)}>Tạo version mới</MenuItem>
            )}
            {permissions.canRetire && <MenuItem icon="block" tone="danger" onClick={() => setDialog("retire")}>Ngưng giao</MenuItem>}
            {permissions.canDelete && <MenuItem icon="close" tone="danger" onClick={() => setDialog("delete")}>Xóa bản nháp</MenuItem>}
          </Menu>
          {preview ? (
            <Button icon="arrow_back" data-testid="preview-back" onClick={() => setPreview(false)}>Quay lại</Button>
          ) : (
            <Button icon="visibility" data-testid="assignment-preview" onClick={() => void openPreview()}>Xem trước</Button>
          )}
          {permissions.canPublish ? (
            <Button variant="primary" icon="calendar_month" data-testid="assignment-schedule" onClick={() => void openSchedule()}>Xếp lịch</Button>
          ) : permissions.canChangeSchedule && (
            <Button variant="primary" icon="calendar_month" data-testid="assignment-change-schedule" onClick={() => setDialog("schedule")}>Đổi lịch</Button>
          )}
        </>} />

      {conflict && (
        <Alert tone="warning" role="alert" action={<Button size="sm" onClick={() => void reload()}>Tải lại</Button>}>
          Bài đã thay đổi ở nơi khác, bản đang sửa chưa lưu
        </Alert>
      )}
      {message && (
        <Alert tone={message.tone} role={message.tone === "danger" ? "alert" : "status"}>{message.text}</Alert>
      )}
      {rubricLoadError && !preview && <Alert tone="warning" role="alert" action={<Button size="sm" onClick={() => {
        setRubricLoadError(null);
        setRubricEpoch((value) => value + 1);
      }}>Tải lại tiêu chí</Button>}>Không tải được tiêu chí chấm: {rubricLoadError}</Alert>}
      {permissions.ownerArchived && <Alert tone="info">{detail.scope === "CLASS" ? "Lớp" : "Môn"} đã lưu trữ · chỉ xem</Alert>}
      {!permissions.canManage && <Alert tone="info">Bài của môn · chỉ xem</Alert>}
      {detail.status === "RETIRED" && detail.retiredReason && <Alert tone="warning" title="Đã ngưng giao">{detail.retiredReason}</Alert>}

      <DocumentEditor document={skeleton} mode={preview ? "student" : "author"} diagramEnabled readOnly={!editable}
        imageUrl={imageUrlFor(detail.id)} issues={preview ? [] : issues.filter((issue) => !issue.code.startsWith("RUBRIC"))}
        uploadImage={async (file) => (await uploadFile(file, "DOCUMENT_IMAGE", () => {}).promise).fileRef.value}
        onChange={editSkeleton}
        header={header}
        comments={preview ? undefined : comments}
        activeComment={activePart}
        highlightBlockId={preview ? null : active?.headingBlockId ?? null}
        onFocusBlock={(blockId) => { const part = sections.get(blockId); if (part) setActivePart(part); }}
        toolbarExtra={preview ? undefined : <>
          {editable && detail.status === "DRAFT" && <ToolbarButton icon="smart_toy" title="Soạn bằng AI" aria-label="Soạn bằng AI" onClick={() => setDialog("ai")} />}
          <ToolbarButton icon="tune" title="Cài đặt bài" aria-label="Cài đặt bài" onClick={() => setDialog("settings")} />
        </>} />

      {dialog === "settings" && (
        <Dialog open size="lg" icon="tune" title="Cài đặt bài" onClose={() => setDialog(null)}
          footer={<Button variant="primary" onClick={() => setDialog(null)}>Xong</Button>}>
          <AssignmentSettingsPanel detail={detail} value={settings} readOnly={!permissions.canEditContent} onChange={editSettings} />
        </Dialog>
      )}
      {dialog === "schedule" && (
        <Dialog open size="lg" icon="calendar_month" title={permissions.canPublish ? "Xếp lịch" : "Đổi lịch"} onClose={() => setDialog(null)}>
          {permissions.canPublish ? (
            <ScheduleForm assignment={detail} mode="publish" onSubmit={async (input) => {
              try {
                const next = await chain(queue, () => assignmentsApi.publish(id, version.current, input));
                apply(next, false);
                // Phát hành khóa rubric trên server: lấy lại để thẻ hiện "Đã khóa" và thôi cho sửa.
                setRubricEpoch((epoch) => epoch + 1);
                setDialog(null);
                setIssues([]);
                setMessage({ tone: "success", text: next.status === "OPEN" ? "Đã phát hành" : "Đã lên lịch" });
              } catch (error) {
                if (problemCode(error) !== NOT_READY) throw error;
                setDialog(null);
                showMissing(problemIssues(error));
              }
            }} />
          ) : permissions.canChangeSchedule && (
            <ScheduleForm assignment={detail} mode="change" onSubmit={async (input) => {
              apply(await chain(queue, () => assignmentsApi.changeSchedule(id, version.current, input)), false);
              setDialog(null);
              setMessage({ tone: "success", text: "Đã lưu lịch" });
            }} />
          )}
        </Dialog>
      )}
      {dialog === "ai" && (
        <AiDraftDialog assignmentId={id} version={detail.version} currentPartCount={detail.parts.filter((p) => p.headingBlockId).length}
          currentTitle={title} currentInstructions={instructions} gradingMode={detail.gradingMode}
          prepareApply={async () => {
            if (!await save()) throw new Error("Không áp dụng được vì còn nội dung chưa lưu. Kiểm tra trạng thái lưu rồi thử lại.");
            return version.current;
          }}
          onClose={() => setDialog(null)} onApplied={() => { setDialog(null); void reload(); }} />
      )}
      {dialog === "retire" && (
        <RetireDialog onClose={() => setDialog(null)}
          onConfirm={async (reason) => {
            apply(await chain(queue, () => assignmentsApi.retire(id, version.current, reason)), false);
            setDialog(null);
            setMessage({ tone: "success", text: "Đã ngưng giao" });
          }} />
      )}
      {dialog === "delete" && (
        <Dialog open title="Xóa bản nháp?" icon="warning" tone="danger" onClose={() => setDialog(null)}
          footer={<>
            <Button onClick={() => setDialog(null)}>Hủy</Button>
            <Button variant="danger" data-testid="assignment-delete-confirm" onClick={() => void act(async () => {
              await assignmentsApi.remove(id, version.current);
              router.push(backHref);
            }, null)}>Xóa</Button>
          </>}>
          Bài, rubric và đề xuất AI của bài bị xóa hẳn.
        </Dialog>
      )}
    </div>
  );
}

function RetireDialog({ onClose, onConfirm }: { onClose: () => void; onConfirm: (reason: string) => Promise<void> }) {
  const [reason, setReason] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  return (
    <Dialog open title="Ngưng giao bài" icon="block" tone="danger" onClose={onClose} dismissible={!busy}
      footer={<>
        <Button onClick={onClose} disabled={busy}>Hủy</Button>
        <Button variant="danger" disabled={busy || !reason.trim()} data-testid="retire-confirm" onClick={async () => {
          setBusy(true);
          try { await onConfirm(reason.trim()); } catch (e) { setError(errorMessage(e)); } finally { setBusy(false); }
        }}>Ngưng giao</Button>
      </>}>
      {error && <Alert tone="danger" role="alert">{error}</Alert>}
      <p>Sinh viên không bắt đầu, không nộp thêm; bài đang làm được tự nộp; bài đã nộp giữ nguyên.</p>
      <Field label="Lý do" htmlFor="retire-reason" required>
        <textarea id="retire-reason" rows={3} maxLength={500} value={reason} onChange={(e) => setReason(e.target.value)}
          className="w-full rounded-[9px] border border-line-strong px-3 py-2 text-[13px] outline-none focus:outline-2 focus:outline-primary" />
      </Field>
    </Dialog>
  );
}
