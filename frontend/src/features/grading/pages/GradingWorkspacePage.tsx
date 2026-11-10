"use client";
import { useCallback, useEffect, useMemo, useRef, useState, type CSSProperties, type ReactNode } from "react";
import { createPortal } from "react-dom";
import { apiFetch } from "@/shared/api/client";
import {
  DocumentEditor,
  RubricNote,
  ToolbarButton,
  outlineOf,
  publishOutline,
  type DocumentComment,
  type RubricCriterion,
} from "@/shared/document";
import { Alert, Button, Icon, Pill, showErrorToast } from "@/shared/ui";
import { RubricChecklist } from "../components/RubricChecklistPanel";
import { GradingFeedbackSummary } from "../components/StructuredFeedbackSection";
import { calculateChecklistPoints } from "../model/rubricScores";
import { useSubmissionContext } from "../hooks/useSubmissionContext";
import { useSubmissionDirectory } from "../hooks/useSubmissionDirectory";
import { gradingApi } from "../api";
import { useResource } from "../hooks/useResource";
import { buildChecklistBindings } from "../components/GradingForm";
import { useGradingWorkspace } from "../hooks/useGradingWorkspace";
import { ActivityReview, GradeHistory } from "../components/GradingReviewPanels";
import { AiProposalPanel, buildAiRequest } from "../components/AiProposalPanel";
import { BatchActionDialog } from "../components/BatchActionDialog";
import { MemberContributionPanel } from "../components/MemberContributionPanel";
import { buildGradebookHref } from "../components/ClassProgressSection";
import { formatPoints, GRADE_STATUS_LABELS, type AiRequest, type Detail, type Proposal, type Rubric } from "../model/types";

/** Kết quả một lượt chấm AI cần xử lý một lần: áp đề xuất READY hoặc báo lỗi. */
type AiOutcome = { proposal?: Proposal; message?: string };
import { toEditorDocument, type SubmissionDocumentDto } from "../model/submissionDocument";
import { PageLoadError, PageLoading } from "@/shared/ui/PageStatus";
import type { NavigationFilters } from "../model/navigation";

export function GradingWorkspacePage({ evaluationId, proposalId, status, late }: { evaluationId: string; proposalId?: string } & NavigationFilters) {
  const load = useCallback(() => status !== undefined || late !== undefined
    ? gradingApi.loadEvaluation(evaluationId, { status, late }) : gradingApi.loadEvaluation(evaluationId), [evaluationId, status, late]);
  const resource = useResource(load);
  return resource.value ? (
    <Workspace key={`${evaluationId}:${status ?? ""}:${late ?? ""}`} initial={resource.value} refresh={resource.refresh} proposalId={proposalId} status={status} late={late} />
  ) : resource.error ? (
    <PageLoadError onRetry={() => void resource.refresh()}>{resource.error}</PageLoadError>
  ) : (
    <PageLoading label="Đang tải bài làm…" />
  );
}

/** Link tải có hạn cho từng ảnh trong bài nộp (U03 qua U09), lấy một lần khi mở bài. */
function useSubmissionImages(evaluationId: string, document: SubmissionDocumentDto): Record<string, string> {
  const [urls, setUrls] = useState<Record<string, string>>({});
  useEffect(() => {
    let active = true;
    const images = document.blocks.filter(
      (block): block is Extract<SubmissionDocumentDto["blocks"][number], { type: "IMAGE" }> =>
        block.type === "IMAGE",
    );
    void Promise.all(images.map(async (block): Promise<[string, string] | null> => {
      if (!block.content.fileId) return null;
      try {
        const token = await apiFetch<{ url: string }>(
          `/api/v1/evaluations/${evaluationId}/images/${encodeURIComponent(block.content.fileId)}`,
        );
        const parsed = new URL(token.url, window.location.origin);
        return ["https:", "http:"].includes(parsed.protocol) ? [block.id, parsed.href] : null;
      } catch {
        return null;
      }
    })).then((entries) => {
      if (active) setUrls(Object.fromEntries(entries.filter((entry): entry is [string, string] => entry !== null)));
    });
    return () => {
      active = false;
    };
  }, [evaluationId, document]);
  return urls;
}

function noteCriteria(rubric: Rubric): RubricCriterion[] {
  return rubric.criteria.map((criterion) => ({
    criterionId: criterion.criterionId,
    title: criterion.title,
    description: "",
    kind: "TEXT",
    items: criterion.items.map((item) => ({ itemId: item.itemId, text: item.text, points: Number(item.points) })),
  }));
}

/** Thanh tỉ lệ điểm đạt / điểm tối đa, giống trang lịch sử nộp bài. */
function Meter({ awarded, max }: { awarded: number | null; max: number | null }) {
  const ratio = awarded != null && max ? Math.min(1, Math.max(0, awarded / max)) : 0;
  return (
    <span aria-hidden className="mt-2.5 block h-1 overflow-hidden rounded-full bg-sunken">
      <span className="block h-full rounded-full bg-primary" style={{ width: `${ratio * 100}%` }} />
    </span>
  );
}

/** Thẻ tổng điểm ở lề phải, giống trang lịch sử nộp bài. */
function TotalCard({ total, maxScore }: { total: number; maxScore: number }) {
  return (
    <article className="rounded-xl border border-line bg-surface px-4 py-3.5 shadow-[var(--shadow-lift)]">
      <p className="text-xs font-medium text-muted">Tổng điểm</p>
      <p aria-label="Điểm tạm tính" className="mt-1 flex items-baseline gap-1.5 tabular-nums">
        <span className="font-display text-[30px] leading-none tracking-[-0.02em] text-ink">{total.toFixed(2)}</span>
        <span className="text-[13px] text-muted"> / {maxScore.toFixed(2)}</span>
      </p>
      <Meter awarded={total} max={maxScore} />
    </article>
  );
}

/**
 * Thanh trên cùng giống trang soạn đề (3 cột): tên sinh viên + trạng thái bên trái, chỗ đặt nút Cả trang / Từng
 * phần ở giữa (qua `viewSlot`), thao tác bên phải (không rớt dòng).
 */
function TopBar({ barRef, left, setViewSlot, actions }: {
  barRef: (element: HTMLElement | null) => void;
  left: ReactNode;
  setViewSlot: (element: HTMLElement | null) => void;
  actions?: ReactNode;
}) {
  return (
    <header ref={barRef} className="sticky top-14 z-30 -mx-4 -mt-8 flex items-center justify-between gap-3 border-b border-line bg-[var(--c-topbar)] px-4 py-2.5 backdrop-blur-md sm:-mx-8 sm:px-8 md:-mt-10">
      <div className="flex min-w-0 items-center gap-x-2.5 shrink">
        {left}
      </div>
      <div ref={setViewSlot} data-testid="view-slot" className="flex shrink-0 justify-center" />
      <div className="flex shrink-0 items-center justify-end gap-2 whitespace-nowrap">
        {actions}
      </div>
    </header>
  );
}

function Workspace({ initial, refresh, proposalId, status, late }: { initial: Detail; refresh: () => Promise<Detail>; proposalId?: string } & NavigationFilters) {
  const workspace = useGradingWorkspace(initial, refresh, { status, late });
  const {
    detail,
    evaluation,
    form,
    busy,
    error,
    dirty,
    proposals,
    proposalsLoaded,
    pollExhausted,
    disabled,
    official,
    currentIndex,
    updateForm,
    reload,
    navigate,
    confirmNavigation,
    save,
    requestProposal,
    resumePolling,
    applyProposal,
  } = workspace;
  const [action, setAction] = useState<"finalize" | "publish" | null>(null);
  const [aiOpen, setAiOpen] = useState(Boolean(proposalId));
  const [aiRequesting, setAiRequesting] = useState(false);
  const [generatingProposalId, setGeneratingProposalId] = useState<string | null>(null);
  const [aiOutcome, setAiOutcome] = useState<AiOutcome | null>(null);
  const [seenPollExhausted, setSeenPollExhausted] = useState(pollExhausted);
  const aiGenerating = aiRequesting || generatingProposalId !== null;
  const [reviewSelection, setReviewSelection] = useState(proposalId);
  if (reviewSelection !== proposalId) {
    setReviewSelection(proposalId);
    setAiOpen(Boolean(proposalId));
  }
  // Kết thúc lượt chấm AI theo dữ liệu mới nhất ngay khi render; effect bên dưới chỉ áp đề xuất và báo lỗi.
  const pollJustExhausted = pollExhausted && !seenPollExhausted;
  if (pollExhausted !== seenPollExhausted) setSeenPollExhausted(pollExhausted);
  if (pollJustExhausted || (aiGenerating && error)) {
    setAiRequesting(false);
    setGeneratingProposalId(null);
    setAiOutcome({ message: error || "Quá thời gian chờ phản hồi từ AI" });
  } else if (generatingProposalId) {
    const target = proposals.find((p) => p.id === generatingProposalId);
    if (target && ((target.status === "READY" && target.proposal) || target.status === "FAILED" || target.status === "REJECTED")) {
      setGeneratingProposalId(null);
      setAiOutcome(target.status === "READY" ? { proposal: target } : { message: target.safeError ?? undefined });
    }
  }
  const handledAiOutcome = useRef<AiOutcome | null>(null);
  useEffect(() => {
    if (!aiOutcome || handledAiOutcome.current === aiOutcome) return;
    handledAiOutcome.current = aiOutcome;
    const ready = aiOutcome.proposal;
    if (ready?.proposal) {
      applyProposal(
        ready.proposal.checks ?? {},
        ready.proposal.feedback ?? "",
        ready.id,
        ready.proposal.itemScoresByRubric ?? {},
        ready.proposal.structuredFeedback ?? null,
      );
    } else if (aiOutcome.message) {
      showErrorToast(aiOutcome.message);
    }
  }, [aiOutcome, applyProposal]);

  const handleAiGrade = async (input?: AiRequest) => {
    if (evaluation.kind === "MEMBER") {
      if (detail.groupEvaluationId && !busy) navigate(detail.groupEvaluationId);
      return;
    }
    if (disabled || official || busy || aiGenerating) return;
    setAiOpen(false);
    setAiRequesting(true);
    const created = await requestProposal(input ?? buildAiRequest());
    setAiRequesting(false);
    if (created) {
      if (created.status === "READY" && created.proposal) {
        applyProposal(
          created.proposal.checks ?? {},
          created.proposal.feedback ?? "",
          created.id,
          created.proposal.itemScoresByRubric ?? {},
          created.proposal.structuredFeedback ?? null,
        );
      } else if (created.status === "FAILED" || created.status === "REJECTED") {
        if (created.safeError) showErrorToast(created.safeError);
      } else {
        setGeneratingProposalId(created.id);
      }
    }
  };

  const handlePublish = async () => {
    if (busy || disabled) return;
    if (dirty) {
      const saved = await save(false);
      if (!saved) return;
    }
    setAction("publish");
  };
  useEffect(() => {
    if (aiOpen) (document.getElementById(`grading-proposal-${proposalId}`) ?? document.getElementById("grading-ai-review"))?.scrollIntoView?.({ block: "start" });
  }, [aiOpen, proposalId, proposals.length]);
  const [activePart, setActivePart] = useState<string | null>(null);
  const [studentSearch, setStudentSearch] = useState("");
  const context = useSubmissionContext(evaluation.classId, evaluation.assignmentId);
  const directory = useSubmissionDirectory(evaluation.classId, evaluation.assignmentId);
  const submitters = (directory.value ?? []).filter((row) => row.evaluationId === evaluation.id ||
    (evaluation.accountId && row.student.accountId === evaluation.accountId));
  const studentName = [...new Set(submitters.map((row) => row.student.name))].join(", ") ||
    (evaluation.kind === "GROUP_DOCUMENT" ? "Bài làm nhóm" : "Bài làm sinh viên");
  const studentEmail = [...new Set(submitters.map((row) => row.student.email))].join(", ") || "";
  const studentMatches = (directory.value ?? []).filter((row) => row.evaluationId &&
    `${row.student.name} ${row.student.email}`.toLocaleLowerCase("vi").includes(studentSearch.trim().toLocaleLowerCase("vi")));
  const paper = useMemo(() => toEditorDocument(detail.document), [detail.document]);
  const imageUrls = useSubmissionImages(evaluation.id, detail.document);
  // Rubric "Toàn bài" (mã ASSIGNMENT) không gắn với mục nào.
  const partTitles: Record<string, string> = { ASSIGNMENT: "Toàn bài", ...Object.fromEntries(detail.document.blocks.flatMap((block) =>
    block.type === "HEADING" && typeof block.content.text === "string" ? [[block.id, block.content.text]] : [])) };
  const validBlockIds = useMemo(() => {
    const ids = new Set<string>();
    for (const block of detail.document.blocks) {
      if (block.id) ids.add(block.id);
    }
    for (const rubric of detail.rubrics) {
      if (rubric.partId) ids.add(rubric.partId);
    }
    return ids;
  }, [detail.document.blocks, detail.rubrics]);
  const total = Math.min(Number(evaluation.maxScore), Math.max(0, calculateChecklistPoints(detail.rubrics, form.checks, form.itemScores) + Number(form.adjustment)));
  const checklist = buildChecklistBindings(form, disabled, updateForm);
  const blockIds = new Set(paper.blocks.map((block) => block.id));

  // --- Header dính trên đầu: thanh công cụ của trình soạn dính ngay dưới header. ---
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
  // Tự động lưu sau 1.5 giây khi có thay đổi (giống AssignmentFormPage). Tạm dừng khi đang mở so khớp đề xuất AI.
  useEffect(() => {
    if (!dirty || disabled || aiOpen) return;
    const timer = setTimeout(() => {
      void save(false);
    }, 1500);
    return () => clearTimeout(timer);
  }, [dirty, disabled, aiOpen, save, form.checks, form.itemScores, form.adjustment, form.feedback]);

  useEffect(() => {
    const keys = (event: KeyboardEvent) => {
      if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === "s") {
        event.preventDefault();
        void save(false);
      }
    };
    window.addEventListener("keydown", keys);
    return () => window.removeEventListener("keydown", keys);
  }, [save]);

  // Sidebar mục lục của bài nộp như trang tạo bài; nút quay lại "Chấm điểm" mở đúng bài tập trên sổ điểm lớp.
  useEffect(() => {
    const backHref = buildGradebookHref(evaluation.classId, { assignment: evaluation.assignmentId });
    publishOutline({
      assignmentId: evaluation.assignmentId,
      backHref,
      backLabel: "Chấm điểm",
      entries: outlineOf(paper.blocks),
      editable: false,
    });
  }, [evaluation.assignmentId, evaluation.classId, paper.blocks]);
  useEffect(() => () => publishOutline(null), []);

  // Dropdown chọn sinh viên mở bằng state (thay cho <details>), đóng khi click ra ngoài.
  const [studentDropdown, setStudentDropdown] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);
  useEffect(() => {
    if (!studentDropdown) return;
    function close(event: MouseEvent) {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) setStudentDropdown(false);
    }
    window.addEventListener("mousedown", close);
    return () => window.removeEventListener("mousedown", close);
  }, [studentDropdown]);

  const comments: DocumentComment[] = [
    {
      key: "total-card",
      anchorBlockId: null,
      content: <TotalCard total={total} maxScore={Number(evaluation.maxScore)} />,
    },
    ...detail.rubrics.map((rubric, index): DocumentComment => ({
      key: rubric.rubricId,
      // Rubric gắn với tiêu đề của phần; rubric cả bài (không có tiêu đề) nằm ở đầu tài liệu.
      anchorBlockId: blockIds.has(rubric.partId) ? rubric.partId : null,
      content: (
        <RubricNote title={partTitles[rubric.partId] || `Phần ${index + 1}`} criteria={noteCriteria(rubric)}
          active={activePart === rubric.rubricId} onActivate={() => setActivePart(rubric.rubricId)}
          badge={<span className="sr-only">Đạt {String(Number(calculateChecklistPoints([rubric], form.checks, form.itemScores).toFixed(2)))}</span>}
          editor={
            <RubricChecklist rubric={rubric} {...checklist}
              feedbackItems={form.structuredFeedback?.items}
              acknowledgedReviewItems={form.acknowledgedReviewItems} />
          } />
      ),
    })),
  ];
  const activeHeading = detail.rubrics.find((rubric) => rubric.rubricId === activePart)?.partId ?? null;

  const saveText = busy ? "Đang xử lý…" : dirty ? "Chưa lưu" : `Đã lưu ${formatPoints(evaluation.score, evaluation.maxScore)}`;

  const isGroupSubmission = evaluation.kind === "GROUP_DOCUMENT";
  const assignmentKind = isGroupSubmission ? "Nhóm" : "Cá nhân";
  const assignmentTitle = context.value?.title || "Bài tập";
  const memberNames = Object.fromEntries(
    (detail.memberActivities ?? []).map(({ student }) => [student.accountId, student.name]),
  );
  const sectionBylines: Record<string, string> = {};
  const unanchoredAuthors = new Set<string>();
  if (isGroupSubmission) {
    const headings = new Set(paper.blocks.filter((block) => block.type === "HEADING").map((block) => block.id));
    for (const { partId, accountId } of detail.sectionAuthors ?? []) {
      const name = memberNames[accountId] ?? "Thành viên";
      if (headings.has(partId)) {
        sectionBylines[partId] = `Người thực hiện: ${name}`;
      } else {
        unanchoredAuthors.add(name);
      }
    }
  }

  const header = (
    <div className="mb-5 flex flex-col gap-2 border-b border-line pb-4">
      <p className="text-[13px] text-muted" data-testid="assignment-kind">{assignmentKind}</p>
      {unanchoredAuthors.size > 0 && (
        <p className="text-xs text-muted">Người thực hiện: {[...unanchoredAuthors].join(", ")}</p>
      )}
      {form.feedback.trim() && (
        <section aria-label="Nhận xét chung" className="mt-2 text-sm leading-relaxed text-body">
          <h3 className="mb-1 font-semibold text-ink">Nhận xét chung</h3>
          <p className="whitespace-pre-wrap wrap-break-word">{form.feedback}</p>
        </section>
      )}
      <GradingFeedbackSummary summary={form.structuredFeedback?.summary}
        title="Điểm mạnh và điểm cần cải thiện" />
    </div>
  );

  return (
    <div data-wide-workspace className="flex flex-col gap-4" style={{ "--doc-toolbar-top": `${56 + headerHeight}px` } as CSSProperties}>
      <TopBar barRef={headerRef} setViewSlot={setViewSlot}
        left={<>
          <h1 className="min-w-0 truncate text-[15px] font-semibold text-ink" title={assignmentTitle}>{assignmentTitle}</h1>
          <h1 className="sr-only">{studentName}</h1>
          <Pill tone={evaluation.state === "PUBLISHED" ? "success" : (evaluation.state === "DRAFT" || evaluation.state === "FINALIZED") ? "info" : "neutral"} className="shrink-0">{GRADE_STATUS_LABELS[evaluation.state]}</Pill>
          <span data-testid="save-state" className="sr-only" role="status">{saveText}</span>
        </>}
        actions={<>
          <button type="button" className="sr-only" aria-hidden="false" disabled={disabled} onClick={() => void save(true)}>
            Lưu nháp / thay đổi
          </button>
          <button type="button" className="sr-only" aria-hidden="false" disabled={disabled || dirty || evaluation.state !== "DRAFT"} onClick={() => setAction("finalize")}>
            Chốt điểm
          </button>
          <Button
            size="sm"
            variant="primary"
            disabled={busy || Boolean(error) || !detail.current || evaluation.state === "PUBLISHED"}
            onClick={() => void handlePublish()}
          >
            Công bố điểm
          </Button>
        </>}
      />

      {!detail.current && <Alert tone="info">Đây là lần nộp cũ, chỉ có thể xem lại.</Alert>}
      {error && (
        <Alert tone="danger" role="alert" action={<Button size="sm" onClick={() => void reload()}>Tải lại phiên bản</Button>}>{error}</Alert>
      )}
      {!detail.rubrics.length && <Alert tone="warning">Bài chưa có rubric khả dụng.</Alert>}

      <DocumentEditor fullWidth matchCommentHeight document={paper} onChange={() => {}} mode="student" readOnly diagramEnabled={false}
        imageUrl={(blockId) => imageUrls[blockId] ?? null}
        emptyLeafLabel="Sinh viên chưa làm phần này."
        titleField={{ text: context.value?.title || "Bài làm đã nộp", readOnly: true, onText: () => {} }}
        header={header}
        sectionBylines={sectionBylines}
        comments={comments}
        activeComment={activePart}
        highlightBlockId={activeHeading}
        viewSlot={viewSlot}
        toolbarCenter={<>
          <ToolbarButton icon="chevron_left" title="Bài trước" aria-label="Bài trước"
            disabled={currentIndex <= 0 || busy}
            onClick={() => {
              if (confirmNavigation()) navigate(detail.navigationIds[currentIndex - 1]!);
            }} />
          <div ref={dropdownRef} className="relative">
            <button type="button" aria-label="Chọn sinh viên" aria-expanded={studentDropdown}
              onClick={() => setStudentDropdown((open) => !open)}
              className="flex max-w-[min(18rem,40vw)] cursor-pointer items-center gap-1.5 rounded-md border border-line bg-surface px-2.5 py-1 text-left transition-colors hover:bg-sunken">
              <span className="min-w-0 flex-1 truncate">
                <span className="block text-xs font-semibold leading-tight text-ink">{studentName}</span>
                {studentEmail && <span className="block text-xs leading-tight text-muted">{studentEmail}</span>}
              </span>
              <Icon name="unfold_more" size={14} className="shrink-0 text-muted" />
            </button>
            {studentDropdown && (
              <div className="absolute left-1/2 top-full z-20 mt-2 w-[min(22rem,calc(100vw-3rem))] -translate-x-1/2 rounded-xl border border-line bg-surface p-3 shadow-[var(--shadow-float)]">
                <label className="flex flex-col gap-2 text-sm font-semibold text-ink">
                  Tìm sinh viên
                  <input type="search" placeholder="Tên hoặc email" value={studentSearch} onChange={(event) => setStudentSearch(event.target.value)}
                    className="w-full rounded-lg border border-field px-3 py-2 text-sm font-normal" />
                </label>
                {directory.error ? <p role="alert" className="mt-3 text-sm text-danger">Không tải được danh sách. <button type="button" className="underline" onClick={() => void directory.refresh().catch(() => {})}>Thử lại</button></p>
                  : !directory.value ? <p role="status" className="mt-3 text-sm text-muted">Đang tải sinh viên…</p>
                    : <ul aria-label="Bài nộp sinh viên" className="mt-2 max-h-64 overflow-y-auto">
                      {studentMatches.map((row) => <li key={row.student.accountId}>
                        <button type="button" disabled={busy || row.evaluationId === evaluation.id} onClick={() => { navigate(row.evaluationId!, false); setStudentDropdown(false); }}
                          className="w-full cursor-pointer rounded-lg px-2 py-2 text-left text-sm hover:bg-sunken disabled:cursor-default disabled:opacity-50">
                          <span className="block font-semibold">{row.student.name}</span><span className="block text-xs text-muted">{row.student.email}</span>
                        </button>
                      </li>)}
                      {!studentMatches.length && <li className="py-3 text-sm text-muted">Không có bài nộp phù hợp.</li>}
                    </ul>}
              </div>
            )}
          </div>
          <ToolbarButton icon="chevron_right" title="Bài sau" aria-label="Bài sau"
            disabled={currentIndex < 0 || currentIndex >= detail.navigationIds.length - 1 || busy}
            onClick={() => {
              if (confirmNavigation()) navigate(detail.navigationIds[currentIndex + 1]!);
            }} />
        </>}
        toolbarExtra={<>
          {isGroupSubmission && detail.members.length > 0 && (
            <ToolbarButton
              icon="groups"
              title="Điểm thành viên"
              aria-label="Điểm thành viên"
              aria-controls="group-member-grades"
              onClick={() => {
                const panel = document.getElementById("group-member-grades");
                panel?.focus({ preventScroll: true });
                panel?.scrollIntoView?.({ block: "start" });
              }}
            >
              <span className="hidden xl:inline">Điểm thành viên</span>
            </ToolbarButton>
          )}
          <ToolbarButton
            icon="history"
            title="Xem đề xuất AI"
            aria-label="Xem đề xuất AI"
            aria-expanded={aiOpen}
            aria-controls={aiOpen ? "grading-ai-review" : undefined}
            onClick={() => setAiOpen((open) => !open)}
          >
            <span className="hidden md:inline">Xem đề xuất AI</span>
          </ToolbarButton>
          <ToolbarButton
            icon="smart_toy"
            title={evaluation.kind === "MEMBER" ? "Mở bài nộp nhóm để chấm bằng AI" : "Chấm bằng AI"}
            aria-label="Chấm bằng AI"
            aria-expanded={aiGenerating}
            disabled={evaluation.kind === "MEMBER"
              ? busy || !detail.groupEvaluationId
              : disabled || official || busy || aiGenerating}
            onClick={() => void handleAiGrade()}
          />
        </>} />

      {isGroupSubmission && (
        <div className="mt-6 flex flex-col gap-4">
          <MemberContributionPanel
            root={evaluation}
            members={detail.members}
            names={memberNames}
            onSaved={() => void reload()}
          />
        </div>
      )}
      <div className="mt-6 flex flex-col gap-3">
        <div className={detail.activityState === "DISABLED" ? "hidden" : undefined}>
          <ActivityReview detail={detail} />
        </div>
        <div className="hidden">
          <GradeHistory evaluation={evaluation} />
        </div>
      </div>
      {aiOpen && (
        <section id="grading-ai-review" aria-label="Chấm bằng AI" className="scroll-mt-52 border-t border-line pt-5">
          <div className="mb-4 flex items-center justify-between gap-3">
            <h2 className="text-lg font-semibold text-ink">Chấm bằng AI</h2>
            <Button size="sm" icon="close" onClick={() => setAiOpen(false)}>Đóng</Button>
          </div>
          <AiProposalPanel evaluationId={evaluation.id} proposals={proposals} proposalsLoaded={proposalsLoaded} selectedProposalId={proposalId} disabled={disabled || official}
            rubrics={detail.rubrics} currentValues={form} partTitles={partTitles} validBlockIds={validBlockIds}
            pollExhausted={pollExhausted} onRecheck={resumePolling}
            onRequest={(input) => void handleAiGrade(input)}
            onApply={(checks, feedback, id, itemScores, structuredFeedback) => {
              applyProposal(checks, feedback, id, itemScores, structuredFeedback);
              setAiOpen(false);
            }} />
        </section>
      )}
      {aiGenerating && typeof document !== "undefined" && createPortal(
        <div className="fixed inset-0 z-[100] flex animate-fade items-center justify-center bg-[var(--c-scrim)] p-4 backdrop-blur-[2px]">
          <div role="alertdialog" aria-modal="true" aria-busy="true" aria-label="Đang chấm bằng AI" data-testid="ai-progress"
            className="flex animate-pop items-center gap-3 rounded-xl border border-line bg-surface px-6 py-5 shadow-[var(--shadow-float)]">
            <span aria-hidden="true"
              className="size-5 animate-spin rounded-full border-2 border-line-strong border-t-primary motion-reduce:animate-none" />
            <span className="text-[14px] font-semibold text-ink">Đang chấm bằng AI…</span>
          </div>
        </div>,
        document.body,
      )}
      {action && (
        <BatchActionDialog
          action={action}
          items={[{ evaluationId: evaluation.id, version: evaluation.version }]}
          onClose={() => setAction(null)}
          onDone={() => void reload()}
        />
      )}
    </div>
  );
}
