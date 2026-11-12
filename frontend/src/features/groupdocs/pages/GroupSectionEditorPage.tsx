"use client";

import { useCallback, useEffect, useMemo, useRef, useState, type CSSProperties } from "react";
import { useRouter } from "next/navigation";
import { uploadFile } from "@/features/files/api";
import { formatDateTime } from "@/features/groups/model/format";
import { ApiError } from "@/shared/api/client";
import { useActivityTracker, type ActivityEventInput } from "@/shared/activity/useActivityTracker";
import { DocumentEditor, DocxImportDialog } from "@/shared/document";
import { outlineOf, publishOutline } from "@/shared/document/outline";
import { Alert, Button, ButtonLink, Pill, showToast } from "@/shared/ui";
import { PageLoadError, PageLoading } from "@/shared/ui/PageStatus";
import { finishSection, getWorkspace, groupDocImageUrl, previewSectionDocx, saveDraft, sendActivity } from "../api";
import { useClock } from "../hooks/useClock";
import { useGroupDocStream } from "../hooks/useGroupDocStream";
import { describeDeadline, formatRemaining } from "../model/deadline";
import { assembleEditorDocument, assembleGroupDocument, splitDrafts } from "../model/document";
import type { DocumentBlock, DocumentModel, SectionView, WorkspaceView } from "../model/types";

export const AUTOSAVE_MS = 10_000;
const NOOP = () => {};

type Drafts = Record<string, DocumentBlock[]>;

function problemMessage(e: unknown, fallback: string): string {
  if (e instanceof ApiError) {
    if (e.problem.status === 409) return e.problem.detail ?? "Dữ liệu vừa thay đổi, hãy tải lại.";
    return e.problem.detail ?? fallback;
  }
  return fallback;
}

function heldBy(view: WorkspaceView): SectionView[] {
  return view.sections.filter((s) => s.status === "CLAIMED" && s.holderId === view.viewerId);
}

function sameBlocks(a: DocumentBlock[] | undefined, b: DocumentBlock[] | undefined): boolean {
  return !!a && !!b && a.length === b.length && a.every((block, i) => block === b[i]);
}

/**
 * Trang soạn bài nhóm (s18b, BR-U12-34), thay trình soạn che kín trang: mọi mục mình đang giữ trên một trang, mở ở chế độ
 * Từng phần; hướng dẫn của giảng viên nằm ngay trong mục (chỉ xem), phần chung của đề (không thuộc mục nào) chỉ xem, mục của
 * người khác không hiện. Sidebar là mục lục của chính các phần đó. Mỗi mục tự lưu 10 giây sau lần sửa cuối kèm `version`
 * riêng; nút Nhập DOCX và Xong nằm ngang tiêu đề mục. Mất mục (bị nhả, giao lại) thì mục rời trang và báo; hết hạn thì chỉ xem.
 */
export function GroupSectionEditorPage({ documentId, focusPart = null }: { documentId: string; focusPart?: string | null }) {
  const router = useRouter();
  const workspaceHref = `/group-docs/${encodeURIComponent(documentId)}`;
  const [view, setView] = useState<WorkspaceView | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [drafts, setDrafts] = useState<Drafts>({});
  const [dirty, setDirty] = useState<string[]>([]);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [busy, setBusy] = useState<string | null>(null);
  const [importing, setImporting] = useState<SectionView | null>(null);
  const [viewSlot, setViewSlot] = useState<HTMLElement | null>(null);
  const [headerHeight, setHeaderHeight] = useState(64);
  const [headerObserver, setHeaderObserver] = useState<ResizeObserver | null>(null);
  const headerRef = useCallback((element: HTMLElement | null) => {
    if (!element || typeof ResizeObserver === "undefined") return;
    const observer = new ResizeObserver(() => setHeaderHeight(element.offsetHeight));
    observer.observe(element);
    setHeaderObserver(observer);
  }, []);
  useEffect(() => () => headerObserver?.disconnect(), [headerObserver]);

  // Bản mới nhất cho các lần lưu chạy nền (hẹn giờ, rời trang).
  const latest = useRef<{ drafts: Drafts; dirty: string[]; versions: Record<string, number> }>({ drafts: {}, dirty: [], versions: {} });
  const held = useRef(new Map<string, string>());
  const finished = useRef(new Set<string>());
  useEffect(() => {
    latest.current.drafts = drafts;
    latest.current.dirty = dirty;
  }, [drafts, dirty]);

  // Bản xem mới: thêm mục vừa nhận, giữ nguyên bài đang viết của mục còn giữ, bỏ mục đã mất và báo.
  const applyView = useCallback((loaded: WorkspaceView) => {
    const mine = heldBy(loaded);
    const ids = new Set(mine.map((s) => s.id));
    const lost = [...held.current].filter(([id]) => !ids.has(id) && !finished.current.has(id)).map(([, title]) => `“${title}”`);
    held.current = new Map(mine.map((s) => [s.id, s.title]));
    for (const s of mine) latest.current.versions[s.id] ??= s.version;
    setDrafts((prev) => Object.fromEntries(mine.map((s) => [s.id, prev[s.id] ?? s.draftBlocks ?? s.publishedBlocks ?? []])));
    setDirty((prev) => prev.filter((id) => ids.has(id)));
    if (lost.length > 0) {
      setNotice(`Bạn không còn giữ mục ${lost.join(", ")} (đã được nhả hoặc giao cho người khác); phần chưa lưu không được giữ.`);
    }
    setView(loaded);
  }, []);

  const reload = useCallback(
    () => getWorkspace(documentId).then(applyView, (e: unknown) => {
      setLoadError(e instanceof ApiError && e.problem.status === 404 ? "Không tìm thấy tài liệu nhóm." : "Không tải được tài liệu nhóm.");
    }),
    [documentId, applyView],
  );
  useEffect(() => {
    void reload();
  }, [reload]);
  useGroupDocStream(view ? documentId : null, () => void reload(), () => void reload());
  const now = useClock();

  const editable = !!view && view.status === "IN_PROGRESS" && view.window !== "CLOSED";
  const send = useCallback(
    (events: ActivityEventInput[], options: { keepalive: boolean }) => sendActivity(documentId, events, options.keepalive),
    [documentId],
  );
  useActivityTracker({ enabled: !!view && view.activityMonitoring && view.role.member && editable, send });

  /** Lưu các mục có thay đổi (hoặc chỉ một mục); mục nào lưu xong mà không sửa thêm thì hết "chưa lưu". */
  const save = useCallback(async (only?: string) => {
    const ids = latest.current.dirty.filter((id) => !only || id === only);
    if (ids.length === 0) return true;
    setSaving(true);
    let ok = true;
    for (const id of ids) {
      const blocks = latest.current.drafts[id];
      if (!blocks) continue;
      try {
        const saved = await saveDraft(documentId, id, latest.current.versions[id] ?? 0, blocks);
        latest.current.versions[id] = saved.version;
        setDirty((prev) => (latest.current.drafts[id] === blocks ? prev.filter((d) => d !== id) : prev));
        setError(null);
      } catch (e) {
        setError(problemMessage(e, "Không lưu được bản nháp."));
        ok = false;
      }
    }
    setSaving(false);
    return ok;
  }, [documentId]);

  useEffect(() => {
    if (dirty.length === 0) return;
    const timer = setTimeout(() => void save(), AUTOSAVE_MS);
    return () => clearTimeout(timer);
  }, [dirty, drafts, save]);

  useEffect(() => {
    if (dirty.length === 0) return;
    const handleBeforeUnload = (event: BeforeUnloadEvent) => {
      event.preventDefault();
      event.returnValue = "";
    };
    window.addEventListener("beforeunload", handleBeforeUnload);
    return () => window.removeEventListener("beforeunload", handleBeforeUnload);
  }, [dirty]);
  // Rời trang trong app (sidebar, nút quay lại): lưu nốt phần chưa lưu.
  useEffect(() => () => void save(), [save]);

  const editorDocument = useMemo(() => (view ? assembleEditorDocument(view.sharedBlocks, view.sections, drafts) : null), [view, drafts]);
  // Số thứ tự như trang tài liệu nhóm (cả đề, kể cả mục của người khác), không đánh lại theo các phần đang hiện.
  const numbers = useMemo(
    () => (view ? Object.fromEntries(outlineOf(assembleGroupDocument(view.sharedBlocks, view.sections).blocks).map((e) => [e.id, e.number])) : {}),
    [view],
  );
  // Các mục mình giữ: có ô Bài làm, viết được khi còn hạn (hết hạn thì cả trang chỉ đọc).
  const writable = useMemo(() => new Set(view ? heldBy(view).map((s) => s.partId) : []), [view]);

  // Sidebar: mục lục của chính các phần trên trang, lối về tài liệu nhóm.
  const headings = editorDocument?.blocks.filter((b) => b.type === "HEADING") ?? [];
  const headingKey = headings.map((b) => b.id).join(",");
  useEffect(() => {
    if (!view || !editorDocument) return;
    const entries = outlineOf(editorDocument.blocks).map((entry) => {
      const number = numbers[entry.id] ?? entry.number;
      const title = number && entry.title.startsWith(number) ? entry.title.slice(number.length).trim() : entry.title;
      return { ...entry, number, title };
    });
    publishOutline({ assignmentId: view.assignmentId, backHref: workspaceHref, backLabel: "Tài liệu nhóm", entries });
    // Chỉ đăng lại khi đổi các phần, không đăng lại mỗi lần gõ.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [view, headingKey, workspaceHref, numbers]);
  useEffect(() => () => publishOutline(null), []);

  // Mở từ "Soạn tiếp" của một mục: chuyển tới đúng mục đó.
  const opened = useRef(false);
  useEffect(() => {
    if (!editorDocument || opened.current || !focusPart) return;
    opened.current = true;
    requestAnimationFrame(() => window.dispatchEvent(new CustomEvent("document:show-part", { detail: { headingId: focusPart } })));
  }, [editorDocument, focusPart]);

  const edit = (next: DocumentModel) => {
    if (!view) return;
    const split = splitDrafts(next, view.sections, new Set(Object.keys(drafts)));
    const changed = Object.keys(split).filter((id) => !sameBlocks(split[id], drafts[id]));
    if (changed.length === 0) return;
    setDrafts((prev) => ({ ...prev, ...Object.fromEntries(changed.map((id) => [id, split[id]!])) }));
    setDirty((prev) => [...new Set([...prev, ...changed])]);
  };

  const done = async (s: SectionView) => {
    setBusy(s.id);
    if (!(await save(s.id))) {
      setBusy(null);
      return;
    }
    try {
      await finishSection(documentId, s.id);
      finished.current.add(s.id);
      showToast(`Đã Xong mục “${s.title}”`);
      await reload();
      if (held.current.size === 0) router.push(workspaceHref);
    } catch (e) {
      setError(problemMessage(e, "Không đánh dấu Xong được."));
    }
    setBusy(null);
  };

  // Xong mọi mục đang giữ một lần: lưu phần chưa lưu, đăng từng mục, rồi về tài liệu nhóm.
  const finishAll = async () => {
    if (!view) return;
    setBusy("all");
    if (!(await save())) {
      setBusy(null);
      return;
    }
    try {
      for (const s of heldBy(view)) {
        await finishSection(documentId, s.id);
        finished.current.add(s.id);
      }
      showToast("Đã hoàn thành tất cả các mục");
      router.push(workspaceHref);
    } catch (e) {
      setError(problemMessage(e, "Không đánh dấu Xong được."));
      void reload();
      setBusy(null);
    }
  };

  if (loadError && !view) return <PageLoadError>{loadError}</PageLoadError>;
  if (!view || !editorDocument) return <PageLoading label="Đang mở trang soạn…" />;

  const mine = heldBy(view);
  const title = view.assignmentTitle ?? "Bài nhóm";
  const deadline = describeDeadline(view, now);
  const cutoff = view.window === "LATE" ? view.lateUntil : view.closesAt;
  const left = editable ? formatRemaining(cutoff, now) : null;
  const actions = Object.fromEntries(mine.map((s) => [s.partId, (
    <div key={s.id} role="group" aria-label={s.title} className="flex items-center gap-1.5">
      <Button size="sm" variant="ghost" icon="upload_file" disabled={!editable || busy !== null} onClick={() => setImporting(s)}>Nhập DOCX</Button>
      <Button size="sm" variant="primary" icon="check" disabled={!editable || busy !== null} onClick={() => void done(s)}>Xong</Button>
    </div>
  )]));
  const header = (
    <div className="mb-5 flex flex-col gap-2 border-b border-line pb-4">
      <p className="text-[13px] text-muted" data-testid="assignment-kind">Nhóm</p>
    </div>
  );

  return (
    <div data-wide-workspace className="flex flex-col gap-4" style={{ "--doc-toolbar-top": `${56 + headerHeight}px` } as CSSProperties}>
      <header ref={headerRef} className="sticky top-14 z-30 main-bleed -mt-8 grid grid-cols-[minmax(0,1fr)_auto_minmax(0,1fr)] items-center gap-x-4 gap-y-2 border-b border-line bg-[var(--c-topbar)] py-3 backdrop-blur-md md:-mt-10">
        <div className="flex min-w-0 flex-1 items-center gap-x-3">
          <h1 className="min-w-0 truncate text-[15px] font-semibold text-ink" title={title}>{title}</h1>
          {left && <span title={`Hạn nộp: ${formatDateTime(cutoff!)}`}><Pill tone="warning">Còn {left}</Pill></span>}
        </div>
        <div ref={setViewSlot} data-testid="view-slot" className="flex justify-center" />
        <div className="flex items-center justify-end">
          {editable && mine.length > 0 && (
            <Button variant="primary" icon="check_circle" disabled={busy !== null || saving} onClick={() => void finishAll()}>
              Hoàn thành tất cả
            </Button>
          )}
        </div>
      </header>

      {error && <Alert tone="danger" role="alert">{error}</Alert>}
      {notice && <Alert tone="warning">{notice}</Alert>}
      {!editable && <Alert tone="info">Đã hết hạn nộp; tài liệu chỉ còn xem.</Alert>}
      {deadline && editable && <Alert tone={deadline.tone}>{deadline.title} Nên bấm Xong trước hạn.</Alert>}

      {mine.length === 0 ? (
        <div className="flex flex-col items-start gap-3 rounded-xl border border-line bg-surface p-6">
          <p className="text-[13.5px] text-body">Bạn chưa giữ mục nào để soạn.</p>
          <ButtonLink href={workspaceHref}>Về tài liệu nhóm</ButtonLink>
        </div>
      ) : (
        <DocumentEditor fullWidth document={editorDocument} onChange={edit} mode="student" readOnly={!editable} answerGuidance
          diagramEnabled={view.diagramEnabled} imageUrl={groupDocImageUrl(documentId)}
          uploadImage={async (file) => (await uploadFile(file, "DOCUMENT_IMAGE", () => {}).promise).fileRef.value}
          titleField={{ text: title, readOnly: true, onText: NOOP }}
          header={header}
          sectionActions={actions}
          sectionNumbers={numbers}
          writableParts={writable}
          initialView="step"
          viewSlot={viewSlot} />
      )}

      {importing && (
        <DocxImportDialog
          allowReplace={false}
          title={`Nhập DOCX vào mục “${importing.title}”`}
          preview={(file) => previewSectionDocx(documentId, importing.id, file)}
          onClose={() => setImporting(null)}
          onImport={(imported) => {
            const id = importing.id;
            setDrafts((prev) => ({ ...prev, [id]: [...(prev[id] ?? []), ...imported.blocks] }));
            setDirty((prev) => [...new Set([...prev, id])]);
          }}
        />
      )}
    </div>
  );
}
