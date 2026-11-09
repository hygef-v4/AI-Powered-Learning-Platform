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
  rubricHalfWritten,
  rubricPointsMissing,
  rubricTotal,
  sectionsOf,
  type DocumentBlock,
  type DocumentComment,
  type DocumentModel,
  type DocumentPart,
  type RubricCriterion,
  type ValidationIssue,
} from "@/shared/document";
import { Alert, Button, Dialog, Field, Menu, MenuItem, Pill, showToast } from "@/shared/ui";
import { AiDraftDialog } from "../components/AiDraftDialog";
import { assignmentsApi, docxUrl, imageUrlFor } from "../api";
import { AssignmentSettingsPanel, settingsOf, type Settings } from "../components/AssignmentSettingsPanel";
import { errorMessage, formatTime, problemCode, problemIssues } from "../model/format";
import { RubricEditor, cleanCriteria, rubricPointsMalformed } from "../components/RubricEditor";
import { ScheduleForm } from "../components/ScheduleForm";
import { statusLabels, statusTones, submissionLabels, type AssignmentDetail, type RubricDetail } from "../model/types";
import { PageLoadError, PageLoading } from "@/shared/ui/PageStatus";
import { assignmentsReturn, outlineOf, publishOutline } from "../model/outline";

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
/** Phần mới có trong {@code parts} (chưa có trong {@code before}) mà trang còn giữ tiêu chí: cần lưu lại rubric đó. */
export function reappearedParts(before: Set<string>, parts: DocumentPart[], criteria: Record<string, RubricCriterion[]>): string[] {
  return parts.filter((part) => !before.has(part.partId) && (criteria[part.partId]?.length ?? 0) > 0).map((part) => part.partId);
}

/**
 * Rubric "Toàn bài" (yêu cầu 2026-11-04): tiêu chí chấm cả bài, không gắn với mục nào, luôn có ở bài có mục (có thể trống);
 * thẻ của nó đứng đầu và ghi tổng điểm cả bài. Bài không có mục thì phần duy nhất đã là cả bài.
 */
export const WHOLE_PART: DocumentPart = { partId: "ASSIGNMENT", headingBlockId: null, ancestorHeadingIds: [], title: "Toàn bài" };
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
export const RUBRIC_ZERO = "Tổng điểm của rubric phải lớn hơn 0";
export const RUBRIC_HALF = "Có mục chấm thiếu nội dung hoặc thiếu điểm";

/**
 * Thông báo khi xếp lịch mà bài còn thiếu (yêu cầu 2026-11-05): nói rõ thiếu gì, không ghi chung chung. Cả bài chưa có điểm
 * thì chỉ ghi "Bài làm chưa có rubric chấm điểm"; rubric lỗi trên thẻ thì báo số thẻ viền đỏ; lỗi trong đề thì báo chỗ đánh
 * dấu đỏ; lỗi không gắn được vào đâu (cài đặt, nhóm…) ghi nguyên văn.
 */
export function missingText(issues: ValidationIssue[]): string {
  const parts: string[] = [];
  if (issues.some((issue) => issue.code === "TITLE_REQUIRED")) parts.push("Bài cần tiêu đề");
  if (issues.some((issue) => issue.code === RUBRIC_MISSING)) parts.push("Bài làm chưa có rubric chấm điểm");
  const cards = issues.filter((issue) => issue.code.startsWith("RUBRIC") && issue.code !== RUBRIC_MISSING);
  if (cards.length > 0) parts.push(`${cards.length} rubric bị lỗi điểm`);
  if (issues.some((issue) => issue.blockId && !issue.code.startsWith("RUBRIC"))) parts.push("Đề còn chỗ lỗi");
  issues.filter((issue) => !issue.blockId && issue.code !== "TITLE_REQUIRED" && !issue.code.startsWith("RUBRIC"))
    .forEach((issue) => parts.push(issue.message));
  return parts.join("; ");
}

/** Lỗi rubric hiện trên thẻ của {@code part}: thẻ có tiêu đề theo tiêu đề, thẻ Toàn bài theo mã RUBRIC_WHOLE…. */
export function cardIssues(issues: ValidationIssue[], part: DocumentPart): ValidationIssue[] {
  return issues.filter((issue) => issue.code.startsWith("RUBRIC") && issue.code !== RUBRIC_MISSING && (part.headingBlockId
    ? issue.blockId === part.headingBlockId
    : !issue.blockId && issue.code.startsWith(WHOLE_ISSUE)));
}

/**
 * Lỗi rubric khi xếp lịch (yêu cầu 2026-11-05, cùng luật với server): cả bài tổng điểm bằng 0 thì chỉ báo "Bài làm chưa có
 * rubric chấm điểm", không tô đỏ thẻ nào; còn lại thẻ có mục chấm mà tổng không quá 0, hoặc mục ghi dở, thì viền đỏ và ghi
 * rõ lý do; thẻ chỉ có dòng trống bỏ qua.
 */
export function rubricIssues(cards: DocumentPart[], criteria: Record<string, RubricCriterion[]>): ValidationIssue[] {
  const total = cards.reduce((sum, part) => sum + rubricTotal(criteria[part.partId] ?? []), 0);
  if (total <= 0) return [{ code: RUBRIC_MISSING, blockId: null, message: "Bài làm chưa có rubric chấm điểm" }];
  return cards.flatMap((part) => {
    const list = criteria[part.partId] ?? [];
    if (!rubricPointsMissing(list)) return [];
    const code = part.headingBlockId ? "RUBRIC_POINTS_MISSING" : "RUBRIC_WHOLE_POINTS_MISSING";
    const reasons = [...(rubricHalfWritten(list) ? [RUBRIC_HALF] : []), ...(rubricTotal(list) <= 0 ? [RUBRIC_ZERO] : [])];
    return reasons.map((message) => ({ code, blockId: part.headingBlockId, message }));
  });
}

/** Thông báo của trang soạn nổi ở góc trên bên phải (yêu cầu 2026-11-05), không chen lên thanh công cụ. */
function notify(text: string, tone: "danger" | "success") {
  showToast(text, tone);
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
  /** Đề AI vừa ghi thẳng lên trang: bản trước đó vào lịch sử của trình soạn để Ctrl+Z quay lại (yêu cầu 2026-11-05). */
  const [aiEdit, setAiEdit] = useState<{ id: number; before: DocumentModel } | null>(null);
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
  const [dialog, setDialog] = useState<"ai" | "retire" | "delete" | "settings" | "schedule" | null>(null);
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
  const dirty = useRef({ details: false, skeleton: false, rubrics: new Set<string>() });
  const queue = useRef<Promise<unknown>>(Promise.resolve());
  const conflictRef = useRef(false);
  const fetched = useRef({ epoch: -1, parts: new Set<string>() });

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
        const before = new Set((detailRef.current?.parts ?? []).map((part) => part.partId));
        next = await assignmentsApi.saveSkeleton(id, version.current, values.skeleton);
        version.current = next.version;
        // Mục thành phần chấm lại (ví dụ xóa mục con vừa thêm): server tạo rubric trống mới, trang vẫn giữ tiêu chí cũ. Lưu lại
        // tiêu chí cũ lên server để thẻ trên trang và server khớp nhau, không báo thiếu rubric khi phát hành (yêu cầu 2026-11-04).
        reappearedParts(before, next.parts, values.criteria).forEach((partId) => work.rubrics.add(partId));
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
          if (rubricPointsMalformed(partCriteria)) {
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
        notify(`Chưa lưu: ${POINTS_INVALID}`, "danger");
        return false;
      }
      setSavedAt(new Date().toISOString());
      setIssues((current) => current.filter((issue) => issue.code.startsWith("RUBRIC")));
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
        notify(`Chưa lưu: ${errorMessage(error)}`, "danger");
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
    // Sửa thẻ: bỏ lỗi của thẻ đó và báo chung "chưa có rubric" (có thể vừa được bổ sung).
    setIssues((current) => {
      const own = new Set(cardIssues(current, part));
      return current.filter((issue) => !own.has(issue) && issue.code !== RUBRIC_MISSING);
    });
    changed();
  }

  /** Lưu bản đang sửa rồi mới gọi thao tác (duyệt, nhân bản…), nối vào hàng ghi để dùng đúng version. */
  async function act(action: () => Promise<AssignmentDetail | void>, success: string | null, after?: () => void) {
    if (!(await save())) return;
    try {
      const result = await chain(queue, action);
      if (result) apply(result, false);
      setIssues([]);
      if (success) notify(success, "success");
      after?.();
    } catch (error) {
      if (problemCode(error) === VERSION_CONFLICT) {
        conflictRef.current = true;
        setConflict(true);
        return;
      }
      setIssues(problemIssues(error));
      notify(errorMessage(error), "danger");
    }
  }

  async function exportDocx(what: "skeleton" | "rubric") {
    if (await save()) window.location.assign(docxUrl(id, what));
  }

  /** Xem trước (tùy chọn): lưu bản đang sửa rồi xem đề như sinh viên thấy khi làm bài (không có rubric). */
  async function openPreview() {
    if (await save()) {
      setPreview(true);
      window.scrollTo({ top: 0 });
    }
  }

  /** Bài còn thiếu mà trang tự thấy được (tên bài, rubric các phần); luật đầy đủ server kiểm khi phát hành. */
  function localMissing(): ValidationIssue[] {
    const found: ValidationIssue[] = [];
    if (!title.trim()) found.push({ code: "TITLE_REQUIRED", blockId: null, message: "Bài cần tiêu đề" });
    const cards = hasWholePart(parts) ? [WHOLE_PART, ...parts] : parts;
    found.push(...rubricIssues(cards, criteria));
    return found;
  }

  /** Bài còn thiếu: về trang soạn, chỗ thiếu tô đỏ, báo chung chung. */
  function showMissing(found: ValidationIssue[]) {
    setPreview(false);
    setIssues(found);
    notify(missingText(found), "danger");
  }

  /**
   * "Xếp lịch" ngay trên trang soạn hoặc trang xem trước (yêu cầu 2026-10-22, bỏ trạng thái Đã duyệt): lưu, kiểm sơ bộ
   * tên bài và rubric rồi mở lịch; phát hành thẳng từ bản nháp, server kiểm đủ luật duyệt (BR-U07-32) khi phát hành.
   */
  async function openSchedule() {
    // Xếp lịch: bỏ luôn các dòng mục chấm trống trên thẻ (server cũng không lưu chúng).
    const cleaned = Object.fromEntries(Object.entries(latest.current.criteria).map(([partId, list]) => [partId, cleanCriteria(list)]));
    latest.current = { ...latest.current, criteria: cleaned };
    setCriteria(cleaned);
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

  // Sidebar của bài: các phần (Đề bài, Câu hỏi) theo khung đang soạn, lối về trang Bài tập của lớp / môn.
  useEffect(() => {
    if (!detail) return;
    // Lối về đúng tab, bộ lọc của trang danh sách đã mở bài này (Nháp, Đang mở, Đã đóng…).
    const backHref = assignmentsReturn(assignmentsHref(detail, params.get("classId") ?? detail.classId));
    const editable = detail.permissions.canEditContent && !preview;
    publishOutline({ assignmentId: detail.id, backHref, backLabel: "Bài tập", entries: outlineOf(skeleton.blocks), editable });
  }, [detail, skeleton, params, preview]);
  useEffect(() => () => publishOutline(null), []);

  if (loadError) {
    return <PageLoadError onRetry={() => void reload()}>{errorMessage(loadError)}</PageLoadError>;
  }
  if (!detail || !settings) return <PageLoading label="Đang tải bài tập…" />;

  const classId = params.get("classId") ?? detail.classId;
  // Xóa bản nháp xong về trang Bài tập (tab Bài tập của lớp, hoặc Bài tập của môn), như lối về ở sidebar.
  const backHref = assignmentsHref(detail, classId);
  const permissions = detail.permissions;
  const editable = permissions.canEditContent && !preview;
  const rubricItemCount = Object.values(criteria).reduce((sum, list) => sum + list.reduce((partSum, criterion) => partSum + criterion.items.length, 0), 0);
  const active = parts.find((part) => part.partId === activePart);
  const titleMissing = issues.some((issue) => issue.code === "TITLE_REQUIRED");
  const saveText = !permissions.canEditContent ? "Chỉ xem" : saving ? "Đang lưu…" : conflict ? "Chưa lưu"
    : unsaved ? "Chưa lưu" : savedAt ? `Đã lưu lúc ${formatTime(savedAt)}` : "Đã lưu";

  // Thẻ rubric ghi số mục như trong trang: "1.2. Tên", mục chưa đặt tên chỉ có số (yêu cầu 2026-11-04).
  const numberOf = new Map(outlineOf(skeleton.blocks).map((entry) => [entry.id, entry.number]));
  const rubricTitle = (part: DocumentPart) => {
    const name = part.title.split(" › ").at(-1) ?? part.title;
    const number = part.headingBlockId ? numberOf.get(part.headingBlockId) : undefined;
    return number && name !== number ? `${number} ${name}` : name;
  };
  // Thẻ Toàn bài đứng đầu, ghi tổng điểm cả bài (mọi mục và toàn bài).
  const whole = hasWholePart(parts);
  const cardParts = whole ? [WHOLE_PART, ...parts] : parts;
  const totalPoints = cardParts.reduce((sum, part) => sum + rubricTotal(criteria[part.partId] ?? []), 0);
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
        {rubricLoadError ? "Chưa tải được tiêu chí chấm." : "Đang tải tiêu chí chấm…"}
      </p> : (
        <RubricNote title={isWhole ? part.title : rubricTitle(part)} criteria={list} active={activePart === part.partId} onActivate={() => setActivePart(part.partId)}
          total={isWhole ? totalPoints : undefined} editOnActive
          issues={cardIssues(issues, part).map((issue) => issue.message)}
          editor={editable && !locked
            ? <RubricEditor criteria={list} onChange={(value) => editRubric(part, value)} />
            : undefined} />
      ),
    };
  });

  const header = (
    <div className="mb-5 flex flex-col gap-2 border-b border-line pb-4">
      {/* Tên bài do trình soạn vẽ ngay trên dòng này (định dạng được, lưu trong khung đề). */}
      {/* Dòng cuối chỉ ghi loại bài (yêu cầu 2026-11-02): Luyện tập, hoặc Cá nhân / Nhóm. */}
      <p className="text-[13px] text-muted" data-testid="assignment-kind">
        {settings.gradingMode === "PRACTICE" ? "Luyện tập" : submissionLabels[detail.submissionMode]}
      </p>
    </div>
  );

  return (
    <div data-wide-workspace className="flex flex-col gap-4" style={{ "--doc-toolbar-top": `${56 + headerHeight}px` } as CSSProperties}>
      <header ref={headerRef} className="sticky top-14 z-30 -mx-4 -mt-8 grid grid-cols-[minmax(0,1fr)_auto_minmax(0,1fr)] items-center gap-x-4 gap-y-2 border-b border-line bg-[var(--c-topbar)] px-4 py-3 backdrop-blur-md sm:-mx-8 sm:px-8 md:-mt-10">
        {/* Quay lại nằm ở sidebar của bài; đầu trang chỉ còn tên bài (đổi theo ô tên đang sửa) và trạng thái. */}
        <div className="flex min-w-0 flex-1 items-center gap-x-3">
          <h1 className="min-w-0 truncate text-[15px] font-semibold text-ink" title={title || detail.title}>{title || detail.title || "Bài chưa đặt tên"}</h1>
          <Pill tone={statusTones[detail.status]}>{statusLabels[detail.status]}</Pill>
          <span data-testid="save-state" className="sr-only" role="status">{saveText}</span>
        </div>
        <div ref={setViewSlot} data-testid="view-slot" className="flex justify-center" />
        <div className="flex flex-wrap items-center justify-end gap-2">
          <Menu label="" icon="more_horiz" ariaLabel="Thao tác khác" align="right">
            <MenuItem icon="download" onClick={() => void exportDocx("skeleton")}>Xuất DOCX</MenuItem>
            <MenuItem icon="download" onClick={() => void exportDocx("rubric")}>Xuất rubric</MenuItem>
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
            <Button icon="edit" data-testid="preview-back" onClick={() => setPreview(false)}>Chỉnh sửa</Button>
          ) : (
            <Button icon="visibility" data-testid="assignment-preview" onClick={() => void openPreview()}>Xem trước</Button>
          )}
          {permissions.canPublish ? (
            <Button variant="primary" icon="calendar_month" data-testid="assignment-schedule" onClick={() => void openSchedule()}>Xếp lịch</Button>
          ) : permissions.canChangeSchedule && (
            <Button variant="primary" icon="calendar_month" data-testid="assignment-change-schedule" onClick={() => setDialog("schedule")}>Đổi lịch</Button>
          )}
        </div>
      </header>

      {conflict && (
        <Alert tone="warning" role="alert" action={<Button size="sm" onClick={() => void reload()}>Tải lại</Button>}>
          Bài đã thay đổi ở nơi khác, bản đang sửa chưa lưu
        </Alert>
      )}
      {detail.gradingMode === "GRADED" && detail.maxAiGradingRubricItems !== undefined
        && rubricItemCount > detail.maxAiGradingRubricItems && (
        <Alert tone="warning" role="status">
          Rubric có {rubricItemCount} mục, vượt giới hạn {detail.maxAiGradingRubricItems} mục cho một lượt chấm AI. Bạn vẫn có thể soạn và chấm thủ công.
        </Alert>
      )}
      {rubricLoadError && !preview && <Alert tone="warning" role="alert" action={<Button size="sm" onClick={() => {
        setRubricLoadError(null);
        setRubricEpoch((value) => value + 1);
      }}>Tải lại tiêu chí</Button>}>Không tải được tiêu chí chấm: {rubricLoadError}</Alert>}
      {permissions.ownerArchived && <Alert tone="info">{detail.scope === "CLASS" ? "Lớp" : "Môn"} đã lưu trữ · chỉ xem</Alert>}
      {!permissions.canManage && <Alert tone="info">Bài của môn · chỉ xem</Alert>}
      {detail.status === "RETIRED" && detail.retiredReason && <Alert tone="warning" title="Đã ngưng giao">{detail.retiredReason}</Alert>}

      <DocumentEditor fullWidth document={skeleton} mode={preview ? "student" : "author"} diagramEnabled readOnly={!editable}
        imageUrl={imageUrlFor(detail.id)} issues={preview ? [] : issues.filter((issue) => !issue.code.startsWith("RUBRIC"))}
        uploadImage={async (file) => (await uploadFile(file, "DOCUMENT_IMAGE", () => {}).promise).fileRef.value}
        onChange={editSkeleton}
        externalEdit={aiEdit}
        titleField={{ text: title, placeholder: "Tên bài tập", invalid: titleMissing, readOnly: !editable, onText: (value) => editTitle(value.slice(0, 200)) }}
        header={header}
        comments={preview ? undefined : comments}
        activeComment={activePart}
        highlightBlockId={preview ? null : active?.headingBlockId ?? null}
        onFocusBlock={(blockId) => { const part = sections.get(blockId); if (part) setActivePart(part); }}
        viewSlot={viewSlot}
        toolbarExtra={preview ? undefined : <>
          <ToolbarButton icon="tune" title="Cài đặt bài" aria-label="Cài đặt bài" onClick={() => setDialog("settings")} />
        </>} />

      {dialog === "settings" && (
        <Dialog open size="lg" title="Cài đặt bài" onClose={() => setDialog(null)}>
          <AssignmentSettingsPanel detail={detail} classId={classId} value={settings} readOnly={!permissions.canEditContent} onChange={editSettings} />
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
                notify(next.status === "OPEN" ? "Đã phát hành" : "Đã lên lịch", "success");
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
              notify("Đã lưu lịch", "success");
            }} />
          )}
        </Dialog>
      )}
      {dialog === "ai" && proposalId && detail.permissions.canEditContent && detail.status === "DRAFT" && (
        <AiDraftDialog assignmentId={id} initialDraftId={proposalId} version={detail.version} currentPartCount={detail.parts.filter((p) => p.headingBlockId).length}
          currentTitle={title} currentInstructions={instructions} gradingMode={detail.gradingMode}
          prepareApply={async () => {
            if (!await save()) throw new Error("Không áp dụng được vì còn nội dung chưa lưu. Kiểm tra trạng thái lưu rồi thử lại.");
            return version.current;
          }}
          onClose={() => setDialog(null)} onApplied={async () => {
            const values = latest.current;
            const before = { ...values.skeleton, title: values.skeleton.title ?? { runs: [{ text: values.title }] } };
            setDialog(null);
            await reload();
            setAiEdit((current) => ({ id: (current?.id ?? 0) + 1, before }));
            notify("Đã ghi đề AI lên trang. Nhấn Ctrl+Z để quay lại bản trước.", "success");
          }} />
      )}
      {dialog === "retire" && (
        <RetireDialog onClose={() => setDialog(null)}
          onConfirm={async (reason) => {
            apply(await chain(queue, () => assignmentsApi.retire(id, version.current, reason)), false);
            setDialog(null);
            notify("Đã ngưng giao", "success");
          }} />
      )}
      {dialog === "delete" && (
        <Dialog open title="Xóa bản nháp?" icon="warning" tone="danger" onClose={() => setDialog(null)}
          footer={<>
            <Button variant="danger" data-testid="assignment-delete-confirm" onClick={() => void act(async () => {
              await assignmentsApi.remove(id, version.current);
              router.push(assignmentsReturn(backHref));
            }, null)}>Xóa</Button>
          </>}>
          <p>Bạn có chắc muốn xóa bản nháp này không?</p>
        </Dialog>
      )}
    </div>
  );
}

/** Trang Bài tập chứa bài: tab Bài tập của lớp, bài chung của môn thì trang Bài tập của môn. */
function assignmentsHref(detail: { scope: string; subjectId: string }, classId: string | null | undefined) {
  if (detail.scope === "SUBJECT" && !classId) return `/manager/assignments?subjectId=${encodeURIComponent(detail.subjectId)}`;
  return classId ? `/teaching/classes/${encodeURIComponent(classId)}?tab=evals` : "/teaching/classes";
}

function RetireDialog({ onClose, onConfirm }: { onClose: () => void; onConfirm: (reason: string) => Promise<void> }) {
  const [reason, setReason] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  return (
    <Dialog open title="Ngưng giao bài" icon="block" tone="danger" onClose={onClose} dismissible={!busy}
      footer={<>
        <Button variant="danger" disabled={busy || !reason.trim()} data-testid="retire-confirm" onClick={async () => {
          setBusy(true);
          try { await onConfirm(reason.trim()); } catch (e) { setError(errorMessage(e)); } finally { setBusy(false); }
        }}>Ngưng giao</Button>
      </>}>
      {error && <Alert tone="danger" role="alert">{error}</Alert>}
      <p>Bạn có chắc muốn ngưng giao bài này không?</p>
      <Field label="Lý do" htmlFor="retire-reason" required>
        <textarea id="retire-reason" rows={3} maxLength={500} value={reason} onChange={(e) => setReason(e.target.value)}
          className="w-full rounded-lg border border-field px-3 py-2 text-[13px] outline-none focus:outline-2 focus:outline-focus" />
      </Field>
    </Dialog>
  );
}
