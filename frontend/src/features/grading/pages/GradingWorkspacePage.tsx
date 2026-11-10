"use client";
import { useCallback, useEffect, useMemo, useRef, useState, type CSSProperties } from "react";
import {
  DocumentEditor,
  RubricNote,
  ToolbarButton,
  outlineOf,
  publishOutline,
  type DocumentComment,
} from "@/shared/document";
import { Alert, Button, Pill, showToast } from "@/shared/ui";
import { RubricChecklist } from "../components/RubricChecklistPanel";
import { calculateChecklistPoints } from "../model/rubricScores";
import { useSubmissionContext } from "../hooks/useSubmissionContext";
import { useSubmissionDirectory } from "../hooks/useSubmissionDirectory";
import { gradingApi } from "../api";
import { useResource } from "../hooks/useResource";
import { buildChecklistBindings } from "../components/GradingForm";
import { useGradingWorkspace } from "../hooks/useGradingWorkspace";
import { ActivityReview } from "../components/GradingReviewPanels";
import { AiProposalPanel, buildAiRequest } from "../components/AiProposalPanel";
import { BatchActionDialog } from "../components/BatchActionDialog";
import { MemberContributionPanel } from "../components/MemberContributionPanel";
import { buildGradebookHref } from "../components/ClassProgressSection";
import {
  AiProgressModal,
  GradingHeaderSection,
  StudentPickerDropdown,
  TopBar,
  TotalCard,
  noteCriteria,
  useSubmissionImages,
} from "../components/GradingWorkspaceHeader";
import { formatPoints, GRADE_STATUS_LABELS, type AiRequest, type Detail, type Proposal } from "../model/types";
import { toEditorDocument } from "../model/submissionDocument";
import { PageLoadError, PageLoading } from "@/shared/ui/PageStatus";
import type { NavigationFilters } from "../model/navigation";

/** Kết quả một lượt chấm AI cần xử lý một lần: áp đề xuất READY hoặc báo lỗi. */
type AiOutcome = { proposal?: Proposal; message?: string };

export function GradingWorkspacePage({
  evaluationId,
  proposalId,
  status,
  late,
}: { evaluationId: string; proposalId?: string } & NavigationFilters) {
  const load = useCallback(
    () =>
      status !== undefined || late !== undefined
        ? gradingApi.loadEvaluation(evaluationId, { status, late })
        : gradingApi.loadEvaluation(evaluationId),
    [evaluationId, status, late],
  );
  const resource = useResource(load);
  return resource.value ? (
    <Workspace
      key={`${evaluationId}:${status ?? ""}:${late ?? ""}`}
      initial={resource.value}
      refresh={resource.refresh}
      proposalId={proposalId}
      status={status}
      late={late}
    />
  ) : resource.error ? (
    <PageLoadError onRetry={() => void resource.refresh()}>{resource.error}</PageLoadError>
  ) : (
    <PageLoading label="Đang tải bài làm…" />
  );
}

function Workspace({
  initial,
  refresh,
  proposalId,
  status,
  late,
}: { initial: Detail; refresh: () => Promise<Detail>; proposalId?: string } & NavigationFilters) {
  const workspace = useGradingWorkspace(initial, refresh, { status, late });
  const {
    detail,
    evaluation,
    form,
    busy,
    saving,
    error,
    dirty,
    aiAutoApplied,
    proposals,
    proposalsLoaded,
    pollExhausted,
    disabled,
    official,
    currentIndex,
    updateForm,
    setItemScore,
    setFeedback,
    setAdjustment,
    setChangeReason,
    updateStructuredSummary,
    updateStructuredItem,
    toggleAcknowledgedReviewItem,
    reload,
    navigate,
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
  const [appliedAiOutcome, setAppliedAiOutcome] = useState<AiOutcome | null>(null);
  if (aiOutcome?.proposal?.proposal && appliedAiOutcome !== aiOutcome) {
    const ready = aiOutcome.proposal;
    setAppliedAiOutcome(aiOutcome);
    applyProposal(ready.proposal!.checks ?? {}, ready.proposal!.feedback ?? "", ready.id,
      ready.proposal!.itemScoresByRubric ?? {}, ready.proposal!.structuredFeedback ?? null);
    setAiOpen(false);
  }
  useEffect(() => {
    if (aiOutcome?.message) showToast(aiOutcome.message, "danger");
  }, [aiOutcome]);

  const handleAiGrade = async (input?: AiRequest) => {
    if (evaluation.kind === "MEMBER") {
      if (detail.groupEvaluationId && !busy && !saving) navigate(detail.groupEvaluationId);
      return;
    }
    if (disabled || official || busy || saving || aiGenerating) return;
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
        if (created.safeError) showToast(created.safeError, "danger");
      } else {
        setGeneratingProposalId(created.id);
      }
    }
  };

  const handlePublish = async () => {
    if (busy || saving || disabled) return;
    if (dirty) {
      const saved = await save(false);
      if (!saved) return;
    }
    setAction("publish");
  };

  useEffect(() => {
    if (aiOpen) {
      (document.getElementById(`grading-proposal-${proposalId}`) ?? document.getElementById("grading-ai-review"))?.scrollIntoView?.({ block: "start" });
    }
  }, [aiOpen, proposalId, proposals.length]);

  const [activePart, setActivePart] = useState<string | null>(null);
  const context = useSubmissionContext(evaluation.classId, evaluation.assignmentId);
  const directory = useSubmissionDirectory(evaluation.classId, evaluation.assignmentId);
  const submitters = (directory.value ?? []).filter(
    (row) => row.evaluationId === evaluation.id || (evaluation.accountId && row.student.accountId === evaluation.accountId),
  );
  const studentName =
    [...new Set(submitters.map((row) => row.student.name))].join(", ") ||
    (evaluation.kind === "GROUP_DOCUMENT" ? "Bài làm nhóm" : "Bài làm sinh viên");
  const studentEmail = [...new Set(submitters.map((row) => row.student.email))].join(", ") || "";
  const paper = useMemo(() => toEditorDocument(detail.document), [detail.document]);
  const imageUrls = useSubmissionImages(evaluation.id, detail.document);
  const partTitles: Record<string, string> = {
    ASSIGNMENT: "Toàn bài",
    ...Object.fromEntries(
      detail.document.blocks.flatMap((block) =>
        block.type === "HEADING" && typeof block.content.text === "string" ? [[block.id, block.content.text]] : [],
      ),
    ),
  };
  const validBlockIds = useMemo(() => {
    const ids = new Set<string>();
    for (const block of detail.document.blocks) {
      if (block.id) ids.add(block.id);
    }
    const rawJson = (detail as { submittedDocumentJson?: string }).submittedDocumentJson;
    if (typeof rawJson === "string" && rawJson.trim()) {
      try {
        const parsed = JSON.parse(rawJson) as { blocks?: { id?: string }[] };
        for (const b of parsed.blocks ?? []) {
          if (b.id) ids.add(b.id);
        }
      } catch {
        // Ignore invalid fallback JSON
      }
    }
    for (const rubric of detail.rubrics) {
      if (rubric.partId) ids.add(rubric.partId);
    }
    return ids;
  }, [detail]);

  const total = Math.min(
    Number(evaluation.maxScore),
    Math.max(0, calculateChecklistPoints(detail.rubrics, form.checks, form.itemScores) + Number(form.adjustment)),
  );
  const checklist = buildChecklistBindings(form, disabled, updateForm);
  const blockIds = new Set(paper.blocks.map((block) => block.id));

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

  const saveDraftRef = useRef(save);
  useEffect(() => { saveDraftRef.current = save; }, [save]);
  useEffect(() => {
    if (!dirty || aiAutoApplied || disabled || busy || saving || aiOpen || error) return;
    const timer = setTimeout(() => {
      void saveDraftRef.current(false, { background: true });
    }, 30000);
    return () => clearTimeout(timer);
  }, [dirty, aiAutoApplied, disabled, busy, saving, aiOpen, error, form]);

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

  const comments: DocumentComment[] = [
    {
      key: "total-card",
      anchorBlockId: null,
      content: <TotalCard total={total} maxScore={Number(evaluation.maxScore)} />,
    },
    ...detail.rubrics.map((rubric, index): DocumentComment => ({
      key: rubric.rubricId,
      anchorBlockId: blockIds.has(rubric.partId) ? rubric.partId : null,
      content: (
        <RubricNote
          title={partTitles[rubric.partId] || `Phần ${index + 1}`}
          criteria={noteCriteria(rubric)}
          active={activePart === rubric.rubricId}
          onActivate={() => setActivePart(rubric.rubricId)}
          badge={
            <span className="sr-only">
              Đạt {String(Number(calculateChecklistPoints([rubric], form.checks, form.itemScores).toFixed(2)))}
            </span>
          }
          editor={
            <RubricChecklist
              rubric={rubric}
              {...checklist}
              validBlockIds={validBlockIds}
              onSetItemScore={setItemScore}
              onChangeStructuredItem={updateStructuredItem}
              onToggleAcknowledge={toggleAcknowledgedReviewItem}
              feedbackItems={form.structuredFeedback?.items}
              acknowledgedReviewItems={form.acknowledgedReviewItems}
            />
          }
        />
      ),
    })),
  ];
  const activeHeading = detail.rubrics.find((rubric) => rubric.rubricId === activePart)?.partId ?? null;
  const saveText = saving ? "Đang lưu…" : busy ? "Đang xử lý…" : dirty ? "Chưa lưu" : `Đã lưu ${formatPoints(evaluation.score, evaluation.maxScore)}`;
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
      if (headings.has(partId)) sectionBylines[partId] = `Người thực hiện: ${name}`;
      else unanchoredAuthors.add(name);
    }
  }

  return (
    <div data-wide-workspace className="flex flex-col gap-4" style={{ "--doc-toolbar-top": `${56 + headerHeight}px` } as CSSProperties}>
      <TopBar
        barRef={headerRef}
        setViewSlot={setViewSlot}
        left={
          <>
            <h1 className="min-w-0 truncate text-[15px] font-semibold text-ink" title={assignmentTitle}>{assignmentTitle}</h1>
            <h1 className="sr-only">{studentName}</h1>
            <Pill tone={evaluation.state === "PUBLISHED" ? "success" : evaluation.state === "DRAFT" || evaluation.state === "FINALIZED" ? "info" : "neutral"} className="shrink-0">
              {GRADE_STATUS_LABELS[evaluation.state]}
            </Pill>
            <span data-testid="save-state" className="sr-only" role="status">{saveText}</span>
          </>
        }
        actions={
          <>
            <Button size="sm" variant="secondary" disabled={disabled || saving} onClick={() => void save(true)}>
              Lưu nháp / thay đổi
            </Button>
            <Button size="sm" variant="secondary" disabled={disabled || saving || dirty || evaluation.state !== "DRAFT"} onClick={() => setAction("finalize")}>
              Chốt điểm
            </Button>
            <Button
              size="sm"
              variant="primary"
              disabled={busy || saving || Boolean(error) || !detail.current || evaluation.state === "PUBLISHED"}
              onClick={() => void handlePublish()}
            >
              Công bố điểm
            </Button>
          </>
        }
      />

      {!detail.current && <Alert tone="info">Đây là lần nộp cũ, chỉ có thể xem lại.</Alert>}
      {error && (
        <Alert tone="danger" role="alert" action={<Button size="sm" onClick={() => void reload()}>Tải lại phiên bản</Button>}>{error}</Alert>
      )}
      {!detail.rubrics.length && <Alert tone="warning">Bài chưa có rubric khả dụng.</Alert>}

      <DocumentEditor
        fullWidth
        matchCommentHeight
        document={paper}
        onChange={() => {}}
        mode="student"
        readOnly
        diagramEnabled={false}
        imageUrl={(blockId) => imageUrls[blockId] ?? null}
        emptyLeafLabel="Sinh viên chưa làm phần này."
        titleField={{ text: context.value?.title || "Bài làm đã nộp", readOnly: true, onText: () => {} }}
        header={
          <GradingHeaderSection
            assignmentKind={assignmentKind}
            unanchoredAuthors={[...unanchoredAuthors]}
            form={form}
            disabled={disabled}
            setFeedback={setFeedback}
            setAdjustment={setAdjustment}
            setChangeReason={setChangeReason}
            updateStructuredSummary={updateStructuredSummary}
          />
        }
        sectionBylines={sectionBylines}
        comments={comments}
        activeComment={activePart}
        highlightBlockId={activeHeading}
        viewSlot={viewSlot}
        toolbarCenter={
          <>
            <ToolbarButton
              icon="chevron_left"
              title="Bài trước"
              aria-label="Bài trước"
              disabled={currentIndex <= 0 || busy || saving}
              onClick={() => navigate(detail.navigationIds[currentIndex - 1]!)}
            />
            <StudentPickerDropdown
              evaluationId={evaluation.id}
              busy={busy || saving}
              studentName={studentName}
              studentEmail={studentEmail}
              directory={directory}
              onSelect={(targetId) => navigate(targetId, false)}
            />
            <ToolbarButton
              icon="chevron_right"
              title="Bài sau"
              aria-label="Bài sau"
              disabled={currentIndex < 0 || currentIndex >= detail.navigationIds.length - 1 || busy || saving}
              onClick={() => navigate(detail.navigationIds[currentIndex + 1]!)}
            />
          </>
        }
        toolbarExtra={
          <>
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
              className="h-9! gap-2! px-3! text-sm! font-semibold!"
              aria-expanded={aiGenerating}
              disabled={evaluation.kind === "MEMBER" ? busy || saving || !detail.groupEvaluationId : disabled || official || busy || saving || aiGenerating}
              onClick={() => void handleAiGrade()}
            >
              Chấm bằng AI
            </ToolbarButton>
          </>
        }
      />

      {isGroupSubmission && (
        <div className="mt-6 flex flex-col gap-4">
          <MemberContributionPanel root={evaluation} members={detail.members} names={memberNames} onSaved={() => void reload()} />
        </div>
      )}
      <div className={detail.activityState === "DISABLED" ? "hidden" : "mt-6"}>
        <ActivityReview detail={detail} />
      </div>
      {aiOpen && (
        <section id="grading-ai-review" aria-label="Chấm bằng AI" className="scroll-mt-52 border-t border-line pt-5">
          <div className="mb-4 flex items-center justify-between gap-3">
            <h2 className="text-lg font-semibold text-ink">Chấm bằng AI</h2>
            <Button size="sm" icon="close" onClick={() => setAiOpen(false)}>Đóng</Button>
          </div>
          <AiProposalPanel
            evaluationId={evaluation.id}
            proposals={proposals}
            proposalsLoaded={proposalsLoaded}
            selectedProposalId={proposalId}
            disabled={disabled || official}
            rubrics={detail.rubrics}
            currentValues={form}
            partTitles={partTitles}
            validBlockIds={validBlockIds}
            pollExhausted={pollExhausted}
            onRecheck={resumePolling}
            onRequest={(input) => void handleAiGrade(input)}
            onApply={(checks, feedback, id, itemScores, structuredFeedback) => {
              applyProposal(checks, feedback, id, itemScores, structuredFeedback);
              setAiOpen(false);
            }}
          />
        </section>
      )}
      <AiProgressModal open={aiGenerating} />
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
