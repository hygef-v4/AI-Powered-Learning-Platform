"use client";

import { useCallback, useEffect, useMemo, useRef, useState, type CSSProperties } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { uploadFile } from "@/features/files/api";
import { ApiError } from "@/shared/api/client";
import {
  DocumentEditor,
  RubricNote,
  ToolbarButton,
  computeParts,
  isRubricHalfWritten,
  isRubricPointsMissing,
  sumRubricTotal,
  numberSections,
  type DocumentBlock,
  type DocumentComment,
  type DocumentModel,
  type DocumentPart,
  type RubricCriterion,
  type ValidationIssue,
} from "@/shared/document";
import { Alert, Button, Dialog, Field, Menu, MenuItem, Pill, showErrorToast, showToast, Textarea } from "@/shared/ui";
import { AiDraftDialog } from "../components/AiDraftDialog";
import { AiReviseChat } from "../components/AiReviseChat";
import { ExamImportDialog } from "../components/ExamImportDialog";
import { clearPendingImport, peekPendingImport } from "../model/pendingImports";
import { assignmentsApi, downloadDocx, buildImageUrl } from "../api";
import { AssignmentSettingsPanel, readSettings, type Settings } from "../components/AssignmentSettingsPanel";
import { describeError, formatTime, readProblemCode, readProblemIssues } from "../model/format";
import { RubricEditor, cleanCriteria, isRubricPointsMalformed } from "../components/RubricEditor";
import { ScheduleForm } from "../components/ScheduleForm";
import { statusLabels, statusTones, submissionLabels, type AssignmentDetail, type RubricDetail } from "../model/types";
import { PageLoadError, PageLoading } from "@/shared/ui/PageStatus";
import { resolveAssignmentsReturn, outlineOf, publishOutline } from "../model/outline";
import { WorkspaceTopBar } from "@/shared/layout/WorkspaceTopBar";
import { msg } from "@/i18n/msg";

const AUTOSAVE_MS = 1500;
const VERSION_CONFLICT = "ASSIGNMENT_VERSION_CONFLICT";
const NOT_READY = "ASSIGNMENT_NOT_READY";
const POINTS_INVALID = msg("assignments.pages.AssignmentFormPage.diemMoiMucToiDa2");

type Local = {
  title: string;
  instructions: string;
  settings: Settings | null;
  skeleton: DocumentModel;
  criteria: Record<string, RubricCriterion[]>;
};

/** Sau khi lưu: ảnh vừa tải lên đã được gắn (server trả {@code hasFile}); bản đang sửa bỏ {@code fileRef} để không gắn lại. */
/** Phần mới có trong {@code parts} (chưa có trong {@code before}) mà trang còn giữ tiêu chí: cần lưu lại rubric đó. */
export function findReappearedParts(before: Set<string>, parts: DocumentPart[], criteria: Record<string, RubricCriterion[]>): string[] {
  return parts.filter((part) => !before.has(part.partId) && (criteria[part.partId]?.length ?? 0) > 0).map((part) => part.partId);
}

/** Hướng dẫn và tiêu chí đi cùng một bản đề; {@code criteria} trống khi rubric của bản đó chưa tải xong. */
type AuthoringSnapshot = { instructions: string; criteria: Record<string, RubricCriterion[]> | null };

/**
 * Tiêu chí cần ghi lại khi Ctrl+Z, Ctrl+Y đưa đề về {@code parts} của một bản đã chụp: mọi phần của bản đó mà trang đã
 * tải rubric, kể cả phần đang trống (AI có thể vừa điền). Phần chưa tải thì giữ nguyên.
 */
export function listRestoredCriteria(
  parts: DocumentPart[],
  snapshot: Record<string, RubricCriterion[]>,
): [string, RubricCriterion[]][] {
  const partIds = [...(hasWholePart(parts) ? [WHOLE_PART.partId] : []), ...parts.map((part) => part.partId)];
  return partIds.flatMap((partId): [string, RubricCriterion[]][] => {
    const criteria = snapshot[partId];
    return criteria ? [[partId, criteria]] : [];
  });
}

/**
 * Rubric "Toàn bài" (yêu cầu 2026-11-04): tiêu chí chấm cả bài, không gắn với mục nào, luôn có ở bài có mục (có thể trống);
 * thẻ của nó đứng đầu và ghi tổng điểm cả bài. Bài không có mục thì phần duy nhất đã là cả bài.
 */
export const WHOLE_PART: DocumentPart = { partId: "ASSIGNMENT", headingBlockId: null, ancestorHeadingIds: [], title: msg("assignments.pages.AssignmentFormPage.toanBai") };
export function hasWholePart(parts: DocumentPart[]): boolean {
  return !(parts.length === 1 && parts[0]!.headingBlockId === null);
}

export function mergeSaved(local: DocumentModel, saved: DocumentModel): DocumentModel {
  const persisted = new Set(saved.blocks.filter((b) => b.type === "IMAGE" && b.content.hasFile).map((b) => b.id));
  return {
    blocks: local.blocks.map((block): DocumentBlock => block.type === "IMAGE" && block.content.fileRef && persisted.has(block.id)
      ? { ...block, content: { ...block.content, fileRef: undefined, hasFile: true } }
      : block),
  };
}

const RUBRIC_MISSING = "RUBRIC_MISSING";
/** Lỗi của thẻ Toàn bài, hoặc phần duy nhất của bài không có mục: không gắn tiêu đề nào. */
const WHOLE_ISSUE = "RUBRIC_WHOLE";
export const RUBRIC_ZERO = msg("assignments.pages.AssignmentFormPage.tongDiemCuaRubricPhaiLon");
export const RUBRIC_HALF = msg("assignments.pages.AssignmentFormPage.coMucChamThieuNoiDung");

/**
 * Thông báo khi xếp lịch mà bài còn thiếu (yêu cầu 2026-11-05): nói rõ thiếu gì, không ghi chung chung. Cả bài chưa có điểm
 * thì chỉ ghi "Bài làm chưa có rubric chấm điểm"; rubric lỗi trên thẻ thì báo số thẻ viền đỏ; lỗi trong đề thì báo chỗ đánh
 * dấu đỏ; lỗi không gắn được vào đâu (cài đặt, nhóm…) ghi nguyên văn.
 */
export function describeMissing(issues: ValidationIssue[]): string {
  const parts: string[] = [];
  if (issues.some((issue) => issue.code === "TITLE_REQUIRED")) parts.push(msg("assignments.pages.AssignmentFormPage.baiCanTieuDe"));
  if (issues.some((issue) => issue.code === RUBRIC_MISSING)) parts.push(msg("assignments.pages.AssignmentFormPage.baiLamChuaCoRubricCham"));
  const cards = issues.filter((issue) => issue.code.startsWith("RUBRIC") && issue.code !== RUBRIC_MISSING);
  if (cards.length > 0) parts.push(msg("assignments.pages.AssignmentFormPage.rubricBiLoiDiem", { length: String(cards.length) }));
  if (issues.some((issue) => issue.blockId && !issue.code.startsWith("RUBRIC"))) parts.push(msg("assignments.pages.AssignmentFormPage.deConChoLoi"));
  issues.filter((issue) => !issue.blockId && issue.code !== "TITLE_REQUIRED" && !issue.code.startsWith("RUBRIC"))
    .forEach((issue) => parts.push(issue.message));
  return parts.join("; ");
}

/** Lỗi rubric hiện trên thẻ của {@code part}: thẻ có tiêu đề theo tiêu đề, thẻ Toàn bài theo mã RUBRIC_WHOLE…. */
export function filterCardIssues(issues: ValidationIssue[], part: DocumentPart): ValidationIssue[] {
  return issues.filter((issue) => issue.code.startsWith("RUBRIC") && issue.code !== RUBRIC_MISSING && (part.headingBlockId
    ? issue.blockId === part.headingBlockId
    : !issue.blockId && issue.code.startsWith(WHOLE_ISSUE)));
}

/**
 * Lỗi rubric khi xếp lịch (yêu cầu 2026-11-05, cùng luật với server): cả bài tổng điểm bằng 0 thì chỉ báo "Bài làm chưa có
 * rubric chấm điểm", không tô đỏ thẻ nào; còn lại thẻ có mục chấm mà tổng không quá 0, hoặc mục ghi dở, thì viền đỏ và ghi
 * rõ lý do; thẻ chỉ có dòng trống bỏ qua.
 */
export function listRubricIssues(cards: DocumentPart[], criteria: Record<string, RubricCriterion[]>): ValidationIssue[] {
  const total = cards.reduce((sum, part) => sum + sumRubricTotal(criteria[part.partId] ?? []), 0);
  if (total <= 0) return [{ code: RUBRIC_MISSING, blockId: null, message: msg("assignments.pages.AssignmentFormPage.baiLamChuaCoRubricCham") }];
  return cards.flatMap((part) => {
    const list = criteria[part.partId] ?? [];
    if (!isRubricPointsMissing(list)) return [];
    const code = part.headingBlockId ? "RUBRIC_POINTS_MISSING" : "RUBRIC_WHOLE_POINTS_MISSING";
    const reasons = [...(isRubricHalfWritten(list) ? [RUBRIC_HALF] : []), ...(sumRubricTotal(list) <= 0 ? [RUBRIC_ZERO] : [])];
    return reasons.map((message) => ({ code, blockId: part.headingBlockId, message }));
  });
}

/**
 * Thông báo của trang soạn nổi ở góc trên bên phải (yêu cầu 2026-11-05), không chen lên thanh công cụ. Trang này chỉ dùng
 * màu đỏ cho lỗi (chưa lưu được, thiếu nội dung), nên câu màu đỏ ở lại tới khi người dùng đóng.
 */
function notify(text: string, tone: "danger" | "success") {
  if (tone === "danger") showErrorToast(text);
  else showToast(text, tone);
}

/** Chạy {@code task} sau mọi lệnh ghi đang chờ (lưu, duyệt…) để lệnh nào cũng gửi đúng version của bài. */
function chainTask<T>(queue: { current: Promise<unknown> }, task: () => Promise<T>): Promise<T> {
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
  /** Đề AI vừa ghi thẳng lên trang: bản trước đó vào lịch sử của trình soạn để Ctrl+Z quay lại (yêu cầu 2026-11-05). */
  const [aiEdit, setAiEdit] = useState<{ id: number; before: DocumentModel } | null>(null);
  /**
   * AI ghi cả rubric và hướng dẫn, còn lịch sử của trình soạn chỉ giữ khung đề: chụp hai phần đó của bản trước AI và,
   * lúc hoàn tác, của bản AI, để Ctrl+Z và Ctrl+Y đổi cả ba cùng lúc.
   */
  const aiRevision = useRef<{
    before: DocumentModel; beforeState: AuthoringSnapshot; after: DocumentModel | null; afterState: AuthoringSnapshot | null;
  } | null>(null);
  const loadedRubricEpoch = useRef(-1);
  const [rubrics, setRubrics] = useState<Record<string, RubricDetail>>({});
  const [rubricLoadError, setRubricLoadError] = useState<string | null>(null);
  const [rubricEpoch, setRubricEpoch] = useState(0);
  const [tick, setTick] = useState(0);
  const [unsaved, setUnsaved] = useState(false);
  const [saving, setSaving] = useState(false);
  const [savedAt, setSavedAt] = useState<string | null>(null);
  const [conflict, setConflict] = useState(false);
  const [issues, setIssues] = useState<ValidationIssue[]>([]);
  const [activePart, setActivePart] = useState<string | null>(null);
  const proposalId = params.get("proposalId") ?? undefined;
  const [dialog, setDialog] = useState<"ai" | "import" | "retire" | "close" | "delete" | "settings" | "schedule" | null>(null);
  const [closing, setClosing] = useState(false);
  /** Tệp đề cũ chọn ở popup tạo bài: mở popup nhập đề và chuyển đổi ngay khi trang tải xong (BR-U07-87). */
  const [importFiles, setImportFiles] = useState<File[] | undefined>(() => {
    const pending = peekPendingImport(id);
    return pending.length > 0 ? pending : undefined;
  });
  const openedPendingImport = useRef(false);
  const openedMcpProposal = useRef<string | null>(null);
  const [preview, setPreview] = useState(false);
  // Header dính trên đầu: nút chọn cách đọc nằm giữa header; thanh công cụ của trình soạn dính ngay dưới header (yêu cầu 2026-11-04).
  const [viewSlot, setViewSlot] = useState<HTMLElement | null>(null);
  const [headerHeight, setHeaderHeight] = useState(64);
  const headerObserver = useRef<ResizeObserver | null>(null);
  const headerRef = useCallback((element: HTMLElement | null) => {
    headerObserver.current?.disconnect();
    headerObserver.current = null;
    if (!element || typeof ResizeObserver === "undefined") return;
    const observer = new ResizeObserver(() => setHeaderHeight(element.offsetHeight));
    observer.observe(element);
    headerObserver.current = observer;
  }, []);
  const version = useRef(0);
  const detailRef = useRef<AssignmentDetail | null>(null);
  // Bản mới nhất người dùng đã gõ, cập nhật ngay trong lúc sửa để lần lưu đang chờ không gửi bản cũ.
  const latest = useRef<Local>({ title, instructions, settings, skeleton, criteria });
  const exporting = useRef(false);
  const dirty = useRef({ details: false, skeleton: false, rubrics: new Set<string>() });
  const queue = useRef<Promise<unknown>>(Promise.resolve());
  const conflictRef = useRef(false);
  const fetched = useRef({ epoch: -1, parts: new Set<string>() });

  const apply = useCallback((next: AssignmentDetail, replaceLocal: boolean) => {
    setDetail(next);
    detailRef.current = next;
    version.current = next.version;
    if (!replaceLocal) return;
    const nextSettings = readSettings(next);
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
    if (!importFiles || detail?.id !== id || !detail.permissions.canEditContent) return;
    if (openedPendingImport.current) return;
    openedPendingImport.current = true;
    clearPendingImport(id);
    setDialog("import");
  }, [detail, id, importFiles]);

  // Tạo đề bằng AI chỉ có ở popup tạo bài (yêu cầu 2026-11-10); trang soạn chỉ mở popup AI để duyệt đề xuất do agent MCP gửi.
  useEffect(() => {
    if (!proposalId || detail?.id !== id || !detail.permissions.canEditContent || detail.status !== "DRAFT") return;
    const key = `${id}:${proposalId}`;
    if (openedMcpProposal.current === key) return;
    openedMcpProposal.current = key;
    setDialog("ai");
  }, [detail, id, proposalId]);

  // Rubric của các mục đã có trên server; mục mới chỉ tải phần chưa có, rubric đang sửa dở giữ bản đang sửa.
  const partsKey = detail ? [...(hasWholePart(detail.parts) ? [WHOLE_PART.partId] : []), ...detail.parts.map((part) => part.partId)].join("|") : "";
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
      loadedRubricEpoch.current = rubricEpoch;
      setRubricLoadError(null);
      setRubrics((current) => ({ ...current, ...Object.fromEntries(list.map((rubric) => [rubric.partId, rubric])) }));
      const fresh = Object.fromEntries(list.filter((rubric) => !dirty.current.rubrics.has(rubric.partId)).map((rubric) => [rubric.partId, rubric.criteria]));
      latest.current = { ...latest.current, criteria: { ...latest.current.criteria, ...fresh } };
      setCriteria((current) => ({ ...current, ...fresh }));
    }, (error) => {
      done = true;
      missing.forEach((partId) => fetched.current.parts.delete(partId));
      if (!controller.signal.aborted) setRubricLoadError(describeError(error));
    });
    return () => {
      controller.abort();
      if (!done) missing.forEach((partId) => fetched.current.parts.delete(partId));
    };
  }, [id, partsKey, rubricEpoch]);

  const save = useCallback((): Promise<boolean> => chainTask(queue, async () => {
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
        const before = new Set((detailRef.current?.parts ?? []).map((part) => part.partId));
        next = await assignmentsApi.saveSkeleton(id, version.current, values.skeleton);
        version.current = next.version;
        // Mục thành phần chấm lại (ví dụ xóa mục con vừa thêm): server tạo rubric trống mới, trang vẫn giữ tiêu chí cũ. Lưu lại
        // tiêu chí cũ lên server để thẻ trên trang và server khớp nhau, không báo thiếu rubric khi phát hành (yêu cầu 2026-11-04).
        findReappearedParts(before, next.parts, values.criteria).forEach((partId) => work.rubrics.add(partId));
        const saved = next.skeleton;
        latest.current = { ...latest.current, skeleton: mergeSaved(latest.current.skeleton, saved) };
        setSkeleton((current) => mergeSaved(current, saved));
      }
      if (work.rubrics.size > 0) {
        const serverParts = (next ?? detailRef.current)?.parts ?? [];
        const onServer = new Set([...serverParts.map((part) => part.partId), ...(hasWholePart(serverParts) ? [WHOLE_PART.partId] : [])]);
        const localParts = computeParts(latest.current.skeleton);
        const local = new Set([...localParts.map((part) => part.partId), ...(hasWholePart(localParts) ? [WHOLE_PART.partId] : [])]);
        let changed = false;
        for (const partId of work.rubrics) {
          if (!onServer.has(partId)) {
            // Mục vừa thêm, khung chưa lưu được: giữ lại để lưu sau khung; mục đã bị xóa thì bỏ.
            if (local.has(partId)) dirty.current.rubrics.add(partId);
            continue;
          }
          const partCriteria = latest.current.criteria[partId] ?? [];
          if (isRubricPointsMalformed(partCriteria)) {
            // Điểm quá 2 chữ số thập phân: không làm tròn, không gửi; giữ chưa lưu tới khi sửa (BR-U07-21).
            dirty.current.rubrics.add(partId);
            const headingBlockId = localParts.find((part) => part.partId === partId)?.headingBlockId ?? null;
            malformed.push({ code: headingBlockId ? "RUBRIC_POINTS_INVALID" : "RUBRIC_WHOLE_POINTS_INVALID",
              blockId: headingBlockId, message: POINTS_INVALID });
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
        setIssues((current) => [...current.filter((issue) => !issue.code.endsWith("POINTS_INVALID")), ...malformed]);
        notify(msg("assignments.pages.AssignmentFormPage.chuaLuu", { POINTS_INVALID: String(POINTS_INVALID) }), "danger");
        return false;
      }
      setSavedAt(new Date().toISOString());
      setIssues((current) => current.filter((issue) => issue.code.startsWith("RUBRIC")));
      return true;
    } catch (error) {
      if (next) apply(next, false);
      const transient = !(error instanceof ApiError) || error.problem.status >= 500;
      if (readProblemCode(error) === VERSION_CONFLICT || transient) {
        // Giữ đánh dấu chưa lưu: lỗi mạng thì lần lưu sau gửi lại; xung đột thì chờ người dùng tải lại.
        dirty.current.details ||= work.details;
        dirty.current.skeleton ||= work.skeleton;
        work.rubrics.forEach((partId) => dirty.current.rubrics.add(partId));
      }
      if (readProblemCode(error) === VERSION_CONFLICT) {
        conflictRef.current = true;
        setConflict(true);
      } else {
        setIssues(readProblemIssues(error));
        notify(msg("assignments.pages.AssignmentFormPage.chuaLuu2", { value: String(describeError(error)) }), "danger");
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
    const handleShortcutKeys = (event: KeyboardEvent) => {
      if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === "s") {
        event.preventDefault();
        void save();
      }
    };
    const leave = (event: BeforeUnloadEvent) => {
      const pending = dirty.current;
      if (pending.details || pending.skeleton || pending.rubrics.size > 0) event.preventDefault();
    };
    window.addEventListener("keydown", handleShortcutKeys);
    window.addEventListener("beforeunload", leave);
    return () => {
      window.removeEventListener("keydown", handleShortcutKeys);
      window.removeEventListener("beforeunload", leave);
    };
  }, [save]);

  const reload = useCallback(() => chainTask(queue, async () => {
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

  /** Lưu trước khi AI đọc hoặc ghi đè đề; trả version mới nhất. */
  const saveForAi = useCallback(async () => {
    if (!await save()) throw new Error(msg("assignments.pages.AssignmentFormPage.khongApDungDuocViCon"));
    return version.current;
  }, [save]);

  /** Đề AI đã ghi trên máy chủ: tải lại, giữ bản trước để Ctrl+Z quay lại được. */
  const showAiResult = useCallback(async (message: string) => {
    const values = latest.current;
    const before = { ...values.skeleton, title: values.skeleton.title ?? { runs: [{ text: values.title }] } };
    await reload();
    aiRevision.current = {
      before, beforeState: { instructions: values.instructions, criteria: values.criteria }, after: null, afterState: null,
    };
    setAiEdit((current) => ({ id: (current?.id ?? 0) + 1, before }));
    notify(message, "success");
  }, [reload]);

  const parts = useMemo(() => computeParts(skeleton), [skeleton]);
  const sections = useMemo(() => numberSections(skeleton, parts), [skeleton, parts]);

  function markChanged() {
    setUnsaved(true);
    setTick((value) => value + 1);
    // Sửa bài đã hoàn tất thì bài về nháp (server cũng đổi khi lưu): nút lại là "Hoàn tất" (yêu cầu 2026-11-11).
    const current = detailRef.current;
    if (current?.status === "REVIEWED") {
      const draft: AssignmentDetail = { ...current, status: "DRAFT",
        permissions: { ...current.permissions, canReview: true, canPublish: false } };
      detailRef.current = draft;
      setDetail(draft);
    }
  }
  function editTitle(value: string) {
    setTitle(value);
    setIssues((current) => current.filter((issue) => issue.code !== "TITLE_REQUIRED"));
    latest.current = { ...latest.current, title: value };
    dirty.current.details = true;
    markChanged();
  }
  function editSettings(value: Settings) {
    setSettings(value);
    latest.current = { ...latest.current, settings: value };
    dirty.current.details = true;
    markChanged();
  }
  /** Đưa hướng dẫn và tiêu chí về bản đã chụp, đánh dấu chưa lưu để lần lưu tới ghi lên máy chủ sau khung đề. */
  function restoreAuthoring(document: DocumentModel, snapshot: AuthoringSnapshot) {
    if (snapshot.instructions !== latest.current.instructions) {
      setInstructions(snapshot.instructions);
      latest.current = { ...latest.current, instructions: snapshot.instructions };
      dirty.current.details = true;
    }
    if (!snapshot.criteria) return;
    const restored = Object.fromEntries(listRestoredCriteria(computeParts(document), snapshot.criteria));
    setCriteria((current) => ({ ...current, ...restored }));
    latest.current = { ...latest.current, criteria: { ...latest.current.criteria, ...restored } };
    Object.keys(restored).forEach((partId) => dirty.current.rubrics.add(partId));
    setIssues((current) => current.filter((issue) => !issue.code.startsWith("RUBRIC")));
  }
  function editSkeleton(value: DocumentModel) {
    // Hoàn tác, làm lại trả đúng đối tượng đề đã vào lịch sử của trình soạn, nên so sánh theo tham chiếu.
    const revision = aiRevision.current;
    if (revision && value === revision.before) {
      revision.after = skeleton;
      revision.afterState = {
        instructions: latest.current.instructions,
        criteria: loadedRubricEpoch.current === rubricEpoch ? latest.current.criteria : null,
      };
      restoreAuthoring(value, revision.beforeState);
    } else if (revision?.afterState && value === revision.after) {
      restoreAuthoring(value, revision.afterState);
    }
    setSkeleton(value);
    latest.current = { ...latest.current, skeleton: value };
    dirty.current.skeleton = true;
    markChanged();
  }
  function editRubric(part: DocumentPart, value: RubricCriterion[]) {
    setCriteria((current) => ({ ...current, [part.partId]: value }));
    latest.current = { ...latest.current, criteria: { ...latest.current.criteria, [part.partId]: value } };
    dirty.current.rubrics.add(part.partId);
    // Sửa thẻ: bỏ lỗi của thẻ đó và báo chung "chưa có rubric" (có thể vừa được bổ sung).
    setIssues((current) => {
      const own = new Set(filterCardIssues(current, part));
      return current.filter((issue) => !own.has(issue) && issue.code !== RUBRIC_MISSING);
    });
    markChanged();
  }

  /** Lưu bản đang sửa rồi mới gọi thao tác (duyệt, nhân bản…), nối vào hàng ghi để dùng đúng version. */
  async function runAction(action: () => Promise<AssignmentDetail | void>, success: string | null, after?: () => void) {
    if (!(await save())) return;
    try {
      const result = await chainTask(queue, action);
      if (result) apply(result, false);
      setIssues([]);
      if (success) notify(success, "success");
      after?.();
    } catch (error) {
      if (readProblemCode(error) === VERSION_CONFLICT) {
        conflictRef.current = true;
        setConflict(true);
        return;
      }
      setIssues(readProblemIssues(error));
      notify(describeError(error), "danger");
    }
  }

  /** Lưu bản đang sửa rồi tải tệp về máy; đang tải thì không tải chồng, lỗi thì báo ở góc trên. */
  async function exportDocx(what: "skeleton" | "rubric") {
    if (exporting.current) return;
    exporting.current = true;
    try {
      if (!(await save())) return;
      const name = (latest.current.title.trim() || "bai-tap").replace(/[\\/:*?"<>|\r\n]+/g, " ");
      await downloadDocx(id, what, `${what === "rubric" ? "Rubric - " : ""}${name}.docx`);
    } catch (error) {
      notify(msg("assignments.pages.AssignmentFormPage.khongTaiDuocTep", { value: String(what === "rubric" ? "rubric" : msg("assignments.pages.AssignmentFormPage.de")), value2: String(describeError(error)) }), "danger");
    } finally {
      exporting.current = false;
    }
  }

  /** Xem trước (tùy chọn): lưu bản đang sửa rồi xem đề như sinh viên thấy khi làm bài (không có rubric). */
  async function openPreview() {
    if (await save()) {
      setPreview(true);
      window.scrollTo({ top: 0 });
    }
  }

  /** Bài còn thiếu mà trang tự thấy được (tên bài, rubric các phần); luật đầy đủ server kiểm khi phát hành. */
  function listLocalMissing(): ValidationIssue[] {
    const found: ValidationIssue[] = [];
    if (!title.trim()) found.push({ code: "TITLE_REQUIRED", blockId: null, message: msg("assignments.pages.AssignmentFormPage.baiCanTieuDe") });
    const cards = hasWholePart(parts) ? [WHOLE_PART, ...parts] : parts;
    found.push(...listRubricIssues(cards, criteria));
    return found;
  }

  /** Bài còn thiếu: về trang soạn, chỗ thiếu tô đỏ, báo chung chung. */
  function showMissing(found: ValidationIssue[]) {
    setPreview(false);
    setIssues(found);
    notify(describeMissing(found), "danger");
  }

  /** Lưu bản mới nhất, bỏ dòng mục chấm trống, kiểm sơ bộ tên bài và rubric; còn thiếu thì báo và trả false. */
  async function saveAndCheck(): Promise<boolean> {
    const cleaned = Object.fromEntries(Object.entries(latest.current.criteria).map(([partId, list]) => [partId, cleanCriteria(list)]));
    latest.current = { ...latest.current, criteria: cleaned };
    setCriteria(cleaned);
    if (!(await save())) {
      setPreview(false);
      return false;
    }
    const found = listLocalMissing();
    if (found.length > 0) {
      showMissing(found);
      return false;
    }
    return true;
  }

  /**
   * "Hoàn tất" (yêu cầu 2026-11-11): lưu bản mới nhất, server kiểm đủ luật duyệt (BR-U07-32) rồi chuyển bài sang Đã hoàn
   * tất; lúc đó nút đổi thành "Xếp lịch". Vẫn sửa được; sửa thì bài về nháp, phải hoàn tất lại mới giao.
   */
  async function finalizeAssignment() {
    if (!(await saveAndCheck())) return;
    try {
      const next = await chainTask(queue, () => assignmentsApi.review(id, version.current));
      // Không hiện thông báo nổi: nút đổi thành "Xếp lịch" và nhãn trạng thái thành "Hoàn tất" là đủ.
      apply(next, false);
    } catch (error) {
      const serverIssues = readProblemIssues(error);
      if (serverIssues.length > 0) showMissing(serverIssues);
      else notify(describeError(error), "danger");
    }
  }

  /** "Xếp lịch": chỉ có ở bài đã hoàn tất; lưu, kiểm rồi mở lịch, server kiểm lại khi phát hành. */
  async function openSchedule() {
    if (!(await saveAndCheck())) return;
    setDialog("schedule");
  }

  // Sidebar của bài: các phần (Đề bài, Câu hỏi) theo khung đang soạn, lối về trang Bài tập của lớp / môn.
  useEffect(() => {
    if (!detail) return;
    // Lối về đúng tab, bộ lọc của trang danh sách đã mở bài này (Nháp, Đang mở, Đã đóng…).
    const backHref = resolveAssignmentsReturn(buildAssignmentsHref(detail, params.get("classId") ?? detail.classId));
    const editable = detail.permissions.canEditContent && !preview;
    publishOutline({ assignmentId: detail.id, backHref, backLabel: msg("assignments.pages.AssignmentFormPage.baiTap"), entries: outlineOf(skeleton.blocks), editable });
  }, [detail, skeleton, params, preview]);
  useEffect(() => () => publishOutline(null), []);

  if (loadError) {
    return <PageLoadError onRetry={() => void reload()}>{describeError(loadError)}</PageLoadError>;
  }
  if (!detail || !settings) return <PageLoading label={msg("assignments.pages.AssignmentFormPage.dangTaiBaiTap")} />;

  const classId = params.get("classId") ?? detail.classId;
  // Xóa bản nháp xong về trang Bài tập (tab Bài tập của lớp, hoặc Bài tập của môn), như lối về ở sidebar.
  const backHref = buildAssignmentsHref(detail, classId);
  const permissions = detail.permissions;
  const canClose = detail.scope === "CLASS" && detail.status === "OPEN"
    && permissions.canRetire && !permissions.ownerArchived;
  const editable = permissions.canEditContent && !preview;
  // Nhập đề thay khung nên chỉ khi bài chưa phát hành (BR-U07-87).
  const canImport = permissions.canEditContent && (detail.status === "DRAFT" || detail.status === "REVIEWED");
  const rubricItemCount = Object.values(criteria).reduce((sum, list) => sum + list.reduce((partSum, criterion) => partSum + criterion.items.length, 0), 0);
  const active = parts.find((part) => part.partId === activePart);
  const titleMissing = issues.some((issue) => issue.code === "TITLE_REQUIRED");
  const saveText = !permissions.canEditContent ? msg("assignments.pages.AssignmentFormPage.chiXem") : saving ? msg("assignments.pages.AssignmentFormPage.dangLuu") : conflict ? msg("assignments.pages.AssignmentFormPage.chuaLuu3")
    : unsaved ? msg("assignments.pages.AssignmentFormPage.chuaLuu3") : savedAt ? msg("assignments.pages.AssignmentFormPage.daLuuLuc", { value: String(formatTime(savedAt)) }) : msg("assignments.pages.AssignmentFormPage.daLuu");

  // Thẻ rubric ghi số mục như trong trang: "1.2. Tên", mục chưa đặt tên chỉ có số (yêu cầu 2026-11-04).
  const numberOf = new Map(outlineOf(skeleton.blocks).map((entry) => [entry.id, entry.number]));
  const resolveRubricTitle = (part: DocumentPart) => {
    const name = part.title.split(" › ").at(-1) ?? part.title;
    const number = part.headingBlockId ? numberOf.get(part.headingBlockId) : undefined;
    return number && name !== number ? `${number} ${name}` : name;
  };
  // Thẻ Toàn bài đứng đầu, ghi tổng điểm cả bài (mọi mục và toàn bài).
  const whole = hasWholePart(parts);
  const cardParts = whole ? [WHOLE_PART, ...parts] : parts;
  const totalPoints = cardParts.reduce((sum, part) => sum + sumRubricTotal(criteria[part.partId] ?? []), 0);
  const comments: DocumentComment[] = cardParts.map((part) => {
    const isWhole = part.partId === WHOLE_PART.partId;
    const list = criteria[part.partId] ?? [];
    const locked = !!rubrics[part.partId]?.locked;
    const awaitingRubric = (isWhole ? hasWholePart(detail.parts) : detail.parts.some((saved) => saved.partId === part.partId))
      && !Object.hasOwn(criteria, part.partId);
    return {
      key: part.partId,
      anchorBlockId: part.headingBlockId,
      content: awaitingRubric ? <p role="status" className="rounded-lg border border-line bg-surface p-3 text-xs text-muted">
        {rubricLoadError ? msg("assignments.pages.AssignmentFormPage.chuaTaiDuocTieuChiCham") : msg("assignments.pages.AssignmentFormPage.dangTaiTieuChiCham")}
      </p> : (
        <RubricNote title={isWhole ? part.title : resolveRubricTitle(part)} criteria={list} active={activePart === part.partId} onActivate={() => setActivePart(part.partId)}
          total={isWhole ? totalPoints : undefined} editOnActive
          issues={filterCardIssues(issues, part).map((issue) => issue.message)}
          editor={editable && !locked
            ? <RubricEditor criteria={list} onChange={(value) => editRubric(part, value)} />
            : undefined} />
      ),
    };
  });

  // Khung chat sửa đề bằng AI đứng trên thẻ Toàn bài (yêu cầu 2026-11-09).

  const header = (
    <div className="mb-5 flex flex-col gap-2 border-b border-line pb-4">
      {/* Tên bài do trình soạn vẽ ngay trên dòng này (định dạng được, lưu trong khung đề). */}
      {/* Dòng cuối chỉ ghi loại bài (yêu cầu 2026-11-02): Luyện tập, hoặc Cá nhân / Nhóm. */}
      <p className="text-[13px] text-muted" data-testid="assignment-kind">
        {settings.gradingMode === "PRACTICE" ? msg("assignments.pages.AssignmentFormPage.luyenTap") : submissionLabels[detail.submissionMode]}
      </p>
    </div>
  );

  return (
    <div data-wide-workspace className="flex flex-col gap-4" style={{ "--doc-toolbar-top": `${56 + headerHeight}px` } as CSSProperties}>
      {/* Quay lại nằm ở sidebar của bài; đầu trang chỉ còn tên bài (đổi theo ô tên đang sửa) và trạng thái. */}
      <WorkspaceTopBar barRef={headerRef} title={title || detail.title || msg("assignments.pages.AssignmentFormPage.baiChuaDatTen")} setViewSlot={setViewSlot}
        status={<>
          <Pill tone={statusTones[detail.status]}>{statusLabels[detail.status]}</Pill>
          <span data-testid="save-state" className="sr-only" role="status">{saveText}</span>
        </>}
        actions={<>
          <Menu label="" icon="more_horiz" ariaLabel={msg("assignments.pages.AssignmentFormPage.thaoTacKhac")} align="right">
            {canImport && (
              <MenuItem icon="upload_file" onClick={() => { setImportFiles(undefined); setDialog("import"); }}>{msg("assignments.pages.AssignmentFormPage.nhapDeTuTep")}</MenuItem>
            )}
            <MenuItem icon="download" onClick={() => void exportDocx("skeleton")}>{msg("assignments.pages.AssignmentFormPage.taiDeDocx")}</MenuItem>
            <MenuItem icon="download" onClick={() => void exportDocx("rubric")}>{msg("assignments.pages.AssignmentFormPage.taiRubricDocx")}</MenuItem>
            {permissions.canCreateVersion && (
              <MenuItem icon="history" onClick={() => void runAction(async () => {
                const copy = await assignmentsApi.newVersion(id);
                router.push(`/assignments/${copy.id}${classId ? `?classId=${classId}` : ""}`);
              }, null)}>{msg("assignments.pages.AssignmentFormPage.taoVersionMoi")}</MenuItem>
            )}
            {permissions.canRetire && <MenuItem icon="block" tone="danger" onClick={() => setDialog("retire")}>{msg("assignments.pages.AssignmentFormPage.ngungGiao")}</MenuItem>}
            {permissions.canDelete && <MenuItem icon="close" tone="danger" onClick={() => setDialog("delete")}>{msg("assignments.pages.AssignmentFormPage.xoaBanNhap")}</MenuItem>}
          </Menu>
          {preview ? (
            <Button icon="edit" data-testid="preview-back" onClick={() => setPreview(false)}>{msg("assignments.pages.AssignmentFormPage.chinhSua")}</Button>
          ) : (
            <Button icon="visibility" data-testid="assignment-preview" onClick={() => void openPreview()}>{msg("assignments.pages.AssignmentFormPage.xemTruoc")}</Button>
          )}
          {canClose && <Button icon="lock" data-testid="assignment-close" disabled={closing || conflict}
            onClick={() => setDialog("close")}>{msg("assignments.pages.AssignmentFormPage.dongNhanBai")}</Button>}
          {permissions.canReview ? (
            <Button variant="primary" icon="check_circle" data-testid="assignment-finalize" onClick={() => void finalizeAssignment()}>{msg("assignments.pages.AssignmentFormPage.hoanTat")}</Button>
          ) : permissions.canPublish ? (
            <Button variant="primary" icon="calendar_month" data-testid="assignment-schedule" onClick={() => void openSchedule()}>{msg("assignments.pages.AssignmentFormPage.xepLich")}</Button>
          ) : permissions.canChangeSchedule && (
            <Button variant="primary" icon="calendar_month" data-testid="assignment-change-schedule" onClick={() => setDialog("schedule")}>{msg("assignments.pages.AssignmentFormPage.doiLich")}</Button>
          )}
        </>} />

      {conflict && (
        <Alert tone="warning" role="alert" action={<Button size="sm" onClick={() => void reload()}>{msg("assignments.pages.AssignmentFormPage.taiLai")}</Button>}>
          {msg("assignments.pages.AssignmentFormPage.baiDaThayDoiONoi")}</Alert>
      )}
      {detail.gradingMode === "GRADED" && detail.maxAiGradingRubricItems !== undefined
        && rubricItemCount > detail.maxAiGradingRubricItems && (
        <Alert tone="warning" role="status">
          {msg("assignments.pages.AssignmentFormPage.rubricCo")}{rubricItemCount} {msg("assignments.pages.AssignmentFormPage.mucVuotGioiHan")}{detail.maxAiGradingRubricItems} {msg("assignments.pages.AssignmentFormPage.mucChoMotLuotChamAi")}</Alert>
      )}
      {rubricLoadError && !preview && <Alert tone="warning" role="alert" action={<Button size="sm" onClick={() => {
        setRubricLoadError(null);
        setRubricEpoch((value) => value + 1);
      }}>{msg("assignments.pages.AssignmentFormPage.taiLaiTieuChi")}</Button>}>{msg("assignments.pages.AssignmentFormPage.khongTaiDuocTieuChiCham")}{rubricLoadError}</Alert>}
      {permissions.ownerArchived && <Alert tone="info">{detail.scope === "CLASS" ? msg("assignments.pages.AssignmentFormPage.lop") : msg("assignments.pages.AssignmentFormPage.mon")} {msg("assignments.pages.AssignmentFormPage.daLuuTruChiXem")}</Alert>}
      {!permissions.canManage && <Alert tone="info">{msg("assignments.pages.AssignmentFormPage.baiCuaMonChiXem")}</Alert>}
      {detail.status === "RETIRED" && detail.retiredReason && <Alert tone="warning" title={msg("assignments.pages.AssignmentFormPage.daNgungGiao")}>{detail.retiredReason}</Alert>}

      <DocumentEditor fullWidth document={skeleton} mode={preview ? "student" : "author"} diagramEnabled readOnly={!editable}
        imageUrl={buildImageUrl(detail.id)} issues={preview ? [] : issues.filter((issue) => !issue.code.startsWith("RUBRIC"))}
        uploadImage={async (file) => (await uploadFile(file, "DOCUMENT_IMAGE", () => {}).promise).fileRef.value}
        onChange={editSkeleton}
        externalEdit={aiEdit}
        titleField={{ text: title, placeholder: msg("assignments.pages.AssignmentFormPage.tenBaiTap"), invalid: titleMissing, readOnly: !editable, onText: (value) => editTitle(value.slice(0, 200)) }}
        header={header}
        comments={preview ? undefined : comments}
        activeComment={activePart}
        highlightBlockId={preview ? null : active?.headingBlockId ?? null}
        onFocusBlock={(blockId) => { const part = sections.get(blockId); if (part) setActivePart(part); }}
        viewSlot={viewSlot}
        toolbarExtra={preview ? undefined : <>
          <ToolbarButton icon="tune" title={msg("assignments.pages.AssignmentFormPage.caiDatBai")} aria-label={msg("assignments.pages.AssignmentFormPage.caiDatBai")} onClick={() => setDialog("settings")} />
        </>} />

      {dialog === "settings" && (
        <Dialog open size="lg" title={msg("assignments.pages.AssignmentFormPage.caiDatBai")} onClose={() => setDialog(null)}>
          <AssignmentSettingsPanel detail={detail} classId={classId} value={settings} readOnly={!permissions.canEditContent} onChange={editSettings} />
        </Dialog>
      )}
      {dialog === "schedule" && (
        <Dialog open size="lg" icon="calendar_month" title={permissions.canPublish ? msg("assignments.pages.AssignmentFormPage.xepLich") : msg("assignments.pages.AssignmentFormPage.doiLich")} onClose={() => setDialog(null)}>
          {permissions.canPublish ? (
            <ScheduleForm assignment={detail} mode="publish" onSubmit={async (input) => {
              try {
                const next = await chainTask(queue, () => assignmentsApi.publish(id, version.current, input));
                apply(next, false);
                // Phát hành khóa rubric trên server: lấy lại để thẻ hiện "Đã khóa" và thôi cho sửa.
                setRubricEpoch((epoch) => epoch + 1);
                setDialog(null);
                setIssues([]);
                notify(next.status === "OPEN" ? msg("assignments.pages.AssignmentFormPage.daPhatHanh") : msg("assignments.pages.AssignmentFormPage.daLenLich"), "success");
              } catch (error) {
                if (readProblemCode(error) !== NOT_READY) throw error;
                setDialog(null);
                showMissing(readProblemIssues(error));
              }
            }} />
          ) : permissions.canChangeSchedule && (
            <ScheduleForm assignment={detail} mode="change" onSubmit={async (input) => {
              apply(await chainTask(queue, () => assignmentsApi.changeSchedule(id, version.current, input)), false);
              setDialog(null);
              notify(msg("assignments.pages.AssignmentFormPage.daLuuLich"), "success");
            }} />
          )}
        </Dialog>
      )}
      {dialog === "ai" && proposalId && detail.permissions.canEditContent && detail.status === "DRAFT" && (
        <AiDraftDialog assignmentId={id} initialDraftId={proposalId} version={detail.version} currentPartCount={detail.parts.filter((p) => p.headingBlockId).length}
          currentTitle={title} currentInstructions={instructions} gradingMode={detail.gradingMode}
          prepareApply={saveForAi}
          onClose={() => setDialog(null)} onApplied={async () => {
            setDialog(null);
            await showAiResult(msg("assignments.pages.AssignmentFormPage.daGhiDeAiLenTrang"));
          }} />
      )}
      {dialog === "import" && canImport && (
        <ExamImportDialog assignmentId={id} currentSkeleton={skeleton} prepare={saveForAi} initialFiles={importFiles}
          onClose={() => setDialog(null)} onApplied={async (message) => {
            setDialog(null);
            await showAiResult(message);
          }} />
      )}
      {/* Trợ lý AI sửa đề: popup nổi ở góc phải dưới, bấm để mở / đóng (yêu cầu 2026-11-12). */}
      {editable && (detail.status === "DRAFT" || detail.status === "REVIEWED") && !preview && (
        <AiReviseChat assignmentId={id} prepare={saveForAi}
          onApplied={() => showAiResult(msg("assignments.pages.AssignmentFormPage.daSuaDeBangAiNhan"))} />
      )}
      {dialog === "retire" && (
        <RetireDialog onClose={() => setDialog(null)}
          onConfirm={async (reason) => {
            apply(await chainTask(queue, () => assignmentsApi.retire(id, version.current, reason)), false);
            setDialog(null);
            notify(msg("assignments.pages.AssignmentFormPage.daNgungGiao"), "success");
          }} />
      )}
      {dialog === "close" && canClose && (
        <Dialog open title={msg("assignments.pages.AssignmentFormPage.dongNhanBai2")} icon="lock" onClose={() => setDialog(null)} dismissible={!closing}
          footer={<Button variant="primary" data-testid="assignment-close-confirm" disabled={closing || conflict}
            onClick={async () => {
              setClosing(true);
              try {
                await runAction(() => assignmentsApi.closeAssignment(id, version.current), msg("assignments.pages.AssignmentFormPage.daDongNhanBai"), () => setDialog(null));
              } finally {
                setClosing(false);
              }
            }}>{msg("assignments.pages.AssignmentFormPage.dongNhanBai")}</Button>}>
          <p>{msg("assignments.pages.AssignmentFormPage.sinhVienKhongTheSuaHoac")}</p>
        </Dialog>
      )}
      {dialog === "delete" && (
        <Dialog open title={msg("assignments.pages.AssignmentFormPage.xoaBanNhap2")} icon="warning" tone="danger" onClose={() => setDialog(null)}
          footer={<>
            <Button variant="danger" data-testid="assignment-delete-confirm" onClick={() => void runAction(async () => {
              await assignmentsApi.remove(id, version.current);
              router.push(resolveAssignmentsReturn(backHref));
            }, null)}>{msg("assignments.pages.AssignmentFormPage.xoa")}</Button>
          </>}>
          <p>{msg("assignments.pages.AssignmentFormPage.banCoChacMuonXoaBan")}</p>
        </Dialog>
      )}
    </div>
  );
}

/** Trang Bài tập chứa bài: tab Bài tập của lớp, bài chung của môn thì trang Bài tập của môn. */
function buildAssignmentsHref(detail: { scope: string; subjectId: string }, classId: string | null | undefined) {
  if (detail.scope === "SUBJECT" && !classId) return `/manager/assignments?subjectId=${encodeURIComponent(detail.subjectId)}`;
  return classId ? `/teaching/classes/${encodeURIComponent(classId)}?tab=evals` : "/teaching/classes";
}

function RetireDialog({ onClose, onConfirm }: { onClose: () => void; onConfirm: (reason: string) => Promise<void> }) {
  const [reason, setReason] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  return (
    <Dialog open title={msg("assignments.pages.AssignmentFormPage.ngungGiaoBai")} icon="block" tone="danger" onClose={onClose} dismissible={!busy}
      footer={<>
        <Button variant="danger" disabled={busy || !reason.trim()} data-testid="retire-confirm" onClick={async () => {
          setBusy(true);
          try { await onConfirm(reason.trim()); } catch (e) { setError(describeError(e)); } finally { setBusy(false); }
        }}>{msg("assignments.pages.AssignmentFormPage.ngungGiao")}</Button>
      </>}>
      {error && <Alert tone="danger" role="alert">{error}</Alert>}
      <p>{msg("assignments.pages.AssignmentFormPage.banCoChacMuonNgungGiao")}</p>
      <Field label={msg("assignments.pages.AssignmentFormPage.lyDo")} htmlFor="retire-reason" required>
        <Textarea id="retire-reason" rows={3} maxLength={500} value={reason} onChange={(e) => setReason(e.target.value)} />
      </Field>
    </Dialog>
  );
}
