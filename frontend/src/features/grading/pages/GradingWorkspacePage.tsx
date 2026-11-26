"use client";
import { formatStudentLabel } from "@/shared/studentCode";
import { describeAiFailure } from "@/shared/api/errorMessages";
import {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
  type CSSProperties,
  type ReactNode,
} from "react";
import {
  DocumentEditor,
  RubricNote,
  ToolbarButton,
  buildOutline,
  publishOutline,
  type DocumentComment,
} from "@/shared/document";
import { Alert, Button, Pill, showErrorToast, showToast } from "@/shared/ui";
import { RubricChecklist } from "../components/RubricChecklistPanel";
import { calculateChecklistPoints } from "../model/rubricScores";
import { useSubmissionContext } from "../hooks/useSubmissionContext";
import { useSubmissionDirectory } from "../hooks/useSubmissionDirectory";
import { gradingApi } from "../api";
import { useResource } from "../hooks/useResource";
import {
  buildChecklistBindings,
  listAiFilledItems,
} from "../components/GradingForm";
import { useGradingWorkspace } from "../hooks/useGradingWorkspace";
import { ActivityReview } from "../components/GradingReviewPanels";
import { AiProposalPanel, buildAiRequest } from "../components/AiProposalPanel";
import { AiDraftBanner } from "../components/AiDraftBanner";
import { BatchActionDialog } from "../components/BatchActionDialog";
import { MemberContributionPanel } from "../components/MemberContributionPanel";
import { GroupGradingPanel } from "../components/GroupGradingPanel";
import { buildGradebookHref } from "../components/ClassProgressSection";
import {
  GradingHeaderSection,
  StudentPickerDropdown,
  TopBar,
  TotalCard,
  listNoteCriteria,
  useSubmissionImages,
} from "../components/GradingWorkspaceHeader";
import {
  formatPoints,
  GRADE_STATUS_LABELS,
  type AiRequest,
  type Detail,
  type Proposal,
} from "../model/types";
import { convertToEditorDocument } from "../model/submissionDocument";
import { PageLoadError, PageLoading } from "@/shared/ui/PageStatus";
import type { NavigationFilters } from "../model/navigation";
import { setGradingAiLabel } from "../model/gradingAiTasks";
import { formatErrorMessage } from "../model/format";
import { msg } from "@/i18n/msg";

// Nạp bản chấm theo URL trước khi khởi tạo workspace theo ID và bộ lọc.
export function GradingWorkspacePage({
  evaluationId,
  proposalId,
  status,
  late,
}: { evaluationId: string; proposalId?: string } & NavigationFilters) {
  // Đọc bản chấm kèm bộ lọc của hàng chờ khi URL có trạng thái hoặc cờ nộp muộn.
  const load = useCallback(
    () =>
      status !== undefined || late !== undefined
        ? gradingApi.loadEvaluation(evaluationId, { status, late }) // load evaluation with filters if provided
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
    <PageLoadError onRetry={() => void resource.refresh()}>
      {resource.error}
    </PageLoadError>
  ) : (
    <PageLoading
      label={msg("grading.pages.GradingWorkspacePage.loadingWork")}
    />
  );
}

// Điều phối bài nộp, form rubric, đề xuất AI và điểm thành viên trong cùng màn hình.
function Workspace({
  initial,
  refresh,
  proposalId,
  status,
  late,
}: {
  initial: Detail;
  refresh: () => Promise<Detail>;
  proposalId?: string;
} & NavigationFilters) {
  const [membersDirty, setMembersDirty] = useState(false);
  const [groupMode, setGroupMode] = useState<"rubric" | "members">("rubric");
  const [groupPanelOpen, setGroupPanelOpen] = useState(false);
  const [selectedMember, setSelectedMember] = useState<string | null>(null);
  const [memberPart, setMemberPart] = useState<string | null>(null);
  const workspace = useGradingWorkspace(
    initial,
    refresh,
    { status, late },
    membersDirty,
  );
  const {
    detail,
    evaluation,
    form,
    busy,
    saving,
    error,
    dirty,
    aiAutoApplied,
    hasAiBackup,
    proposals,
    proposalsLoaded,
    pollExhausted,
    disabled,
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
    discardProposal,
    keepProposal,
  } = workspace;
  const [action, setAction] = useState<"publish" | null>(null);
  // Đề xuất do giáo viên vừa yêu cầu ở trang này: READY thì tự điền vào form (hoặc chờ đồng ý nếu form đang có chỉnh sửa chưa lưu).
  const awaitedProposalId = useRef<string | null>(null);
  const [offeredProposalId, setOfferedProposalId] = useState<string | null>(
    null,
  );
  // Link từ MCP mở phần duyệt đề xuất. Giảng viên vẫn chọn áp dụng hoặc chỉnh điểm.
  const [aiOpen, setAiOpen] = useState(Boolean(proposalId));
  const [aiRequesting, setAiRequesting] = useState(false);
  const aiGenerating =
    aiRequesting || proposals.some((proposal) => proposal.status === "PENDING");
  const [reviewSelection, setReviewSelection] = useState(proposalId);
  if (reviewSelection !== proposalId) {
    setReviewSelection(proposalId);
    setAiOpen(Boolean(proposalId));
  }

  // Xin đề xuất cho bản chấm hiện tại hoặc chuyển sang bản chính khi đang xem thành viên.
  const handleAiGrade = async (input?: AiRequest) => {  // Ai chấm bài
    if (disabled || busy || saving) return;
    if (evaluation.kind === "MEMBER") {
      if (detail.groupEvaluationId && !busy && !saving)
        navigate(detail.groupEvaluationId);
      return;
    }
    if (disabled || busy || saving || aiGenerating) return;
    setAiOpen(false);
    setAiRequesting(true);
    const created = await requestProposal(input ?? buildAiRequest());
    if (created) awaitedProposalId.current = created.id;
    setAiRequesting(false);
    if (created)
      setGradingAiLabel(evaluation.id, `${assignmentTitle} — ${studentName}`);
    if (created?.status === "FAILED" || created?.status === "REJECTED") {
      if (created.safeError) showErrorToast(describeAiFailure(created.safeError));
    } else if (created) {
      showToast(
        created.status === "READY"
          ? msg("grading.pages.GradingWorkspacePage.aNewAiProposalIs")
          : msg("grading.pages.GradingWorkspacePage.aiGradingSentYouCan"),
      );
    }
  };

  // Lấy checklist, điểm và phản hồi của đề xuất đã có nội dung để điền vào form.
  function applyReadyProposal(proposal: Proposal) {
    if (!proposal.proposal) return;
    const { checks, feedback, itemScoresByRubric, structuredFeedback } =
      proposal.proposal;
    applyProposal(
      checks,
      feedback ?? "",
      proposal.id,
      itemScoresByRubric,
      structuredFeedback,
    );
  }

  // Chỉ xử lý tự điền cho đề xuất được yêu cầu từ màn hình hiện tại sau khi có kết quả.
  function settleAwaitedProposal() {
    const awaited = proposals.find(
      (proposal) => proposal.id === awaitedProposalId.current,
    );
    if (!awaited || awaited.status === "PENDING" || disabled) return;
    awaitedProposalId.current = null;
    if (awaited.status !== "READY" || !awaited.proposal) return;
    if (dirty) setOfferedProposalId(awaited.id);
    else applyReadyProposal(awaited);
  }

  // Chạy mỗi lần render để dùng form mới nhất; ref chặn xử lý lặp.
  useEffect(settleAwaitedProposal);

  const offeredProposal = proposals.find(
    (proposal) => proposal.id === offeredProposalId && proposal.proposal,
  );
  const draftProposal =
    offeredProposal ??
    (hasAiBackup
      ? proposals.find(
          (proposal) =>
            proposal.id === form.usedProposalId && proposal.proposal,
        )
      : undefined);

  const aiFilledItems = listAiFilledItems(
    detail.rubrics,
    form,
    hasAiBackup ? draftProposal : undefined,
  );
  const isFeedbackAiFilled =
    hasAiBackup &&
    Boolean(draftProposal?.proposal?.feedback) &&
    form.feedback === draftProposal?.proposal?.feedback;

  // Chỉ task còn QUEUED mới hủy được; RUNNING đã gửi tới nhà cung cấp, tự kết thúc khi hết thời gian chờ.
  const queuedProposal = proposals.find(
    (proposal) =>
      proposal.status === "PENDING" &&
      proposal.taskId &&
      proposal.taskState === "QUEUED",
  );
  // Hủy tác vụ của đề xuất đang chờ và cập nhật lại trạng thái hiển thị.
  const handleCancelAi = async () => {
    if (!queuedProposal?.taskId) return;
    try {
      await gradingApi.cancelTask(queuedProposal.taskId);
      showToast(
        msg("grading.pages.GradingWorkspacePage.aiGradingRequestCancelled"),
      );
    } catch (error) {
      showErrorToast(formatErrorMessage(error));
    }
    resumePolling();
  };

  // Chặn công bố khi điểm thành viên chưa lưu và xử lý lưu bản chấm trước khi xác nhận.
  const handlePublish = async () => {
    if (busy || saving || disabled) return;
    if (membersDirty) {
      showErrorToast(
        msg(
          "grading.pages.GradingWorkspacePage.youHaveUnsavedMemberAssessments",
        ),
      );
      return;
    }
    if (dirty) {
      const saved = await save(false);
      if (!saved) return;
    }
    setAction("publish");
  };

  useEffect(() => {
    if (aiOpen) {
      (
        document.getElementById(`grading-proposal-${proposalId}`) ??
        document.getElementById("grading-ai-review")
      )?.scrollIntoView?.({ block: "start" });
    }
  }, [aiOpen, proposalId, proposals.length]);

  const [activePart, setActivePart] = useState<string | null>(null);
  const context = useSubmissionContext(
    evaluation.classId,
    evaluation.assignmentId,
  );
  const directory = useSubmissionDirectory(
    evaluation.classId,
    evaluation.assignmentId,
  );
  const submitters = (directory.value ?? []).filter(
    (row) =>
      row.evaluationId === evaluation.id ||
      (evaluation.accountId && row.student.accountId === evaluation.accountId),
  );
  const studentName =
    [...new Set(submitters.map((row) => row.student.name))].join(", ") ||
    (evaluation.kind === "GROUP_DOCUMENT"
      ? msg("grading.pages.GradingWorkspacePage.groupWork")
      : msg("grading.pages.GradingWorkspacePage.studentWork"));
  const studentEmail =
    [...new Set(submitters.map((row) => row.student.email))].join(", ") || "";
  const studentCode = [
    ...new Set(
      submitters.map((row) => row.student.studentCode).filter(Boolean),
    ),
  ].join(", ");
  const paper = useMemo(
    () => convertToEditorDocument(detail.document),
    [detail.document],
  );
  const imageUrls = useSubmissionImages(evaluation.id, detail.document);
  const partTitles: Record<string, string> = {
    ASSIGNMENT: msg("grading.pages.GradingWorkspacePage.wholeAssignment"),
    ...Object.fromEntries(
      detail.document.blocks.flatMap((block) =>
        block.type === "HEADING" && typeof block.content.text === "string"
          ? [[block.id, block.content.text]]
          : [],
      ),
    ),
  };
  const validBlockIds = useMemo(() => {
    const ids = new Set<string>();
    for (const block of detail.document.blocks) {
      if (block.id) ids.add(block.id);
    }
    const rawJson = (detail as { submittedDocumentJson?: string })
      .submittedDocumentJson;
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
    Math.max(
      0,
      calculateChecklistPoints(detail.rubrics, form.checks, form.itemScores) +
        Number(form.adjustment),
    ),
  );
  const checklist = buildChecklistBindings(form, disabled, updateForm);
  const blockIds = new Set(paper.blocks.map((block) => block.id));

  const [viewSlot, setViewSlot] = useState<HTMLElement | null>(null);
  const [headerHeight, setHeaderHeight] = useState(64);
  const headerObserver = useRef<ResizeObserver | null>(null);
  // Đo chiều cao thanh thao tác để căn workspace và tháo observer khi phần tử đổi.
  const headerRef = useCallback((element: HTMLElement | null) => {
    headerObserver.current?.disconnect();
    headerObserver.current = null;
    if (!element || typeof ResizeObserver === "undefined") return;
    const observer = new ResizeObserver(() =>
      setHeaderHeight(element.offsetHeight),
    );
    observer.observe(element);
    headerObserver.current = observer;
  }, []);

  const saveDraftRef = useRef(save);
  useEffect(() => {
    saveDraftRef.current = save;
  }, [save]);
  useEffect(() => {
    if (
      !dirty ||
      aiAutoApplied ||
      disabled ||
      busy ||
      saving ||
      aiOpen ||
      error
    )
      return;
    const timer = setTimeout(() => {
      void saveDraftRef.current(false, { background: true });
    }, 30000);
    return () => clearTimeout(timer);
  }, [dirty, aiAutoApplied, disabled, busy, saving, aiOpen, error, form]);

  useEffect(() => {
    // Đổi Ctrl/Cmd+S thành thao tác lưu bản chấm hiện tại.
    const handleShortcutKeys = (event: KeyboardEvent) => {
      if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === "s") {
        event.preventDefault();
        void save(false);
      }
    };
    window.addEventListener("keydown", handleShortcutKeys);
    return () => window.removeEventListener("keydown", handleShortcutKeys);
  }, [save]);

  useEffect(() => {
    const backHref = buildGradebookHref(evaluation.classId, {
      assignment: evaluation.assignmentId,
    });
    publishOutline({
      assignmentId: evaluation.assignmentId,
      backHref,
      backLabel: msg("grading.pages.GradingWorkspacePage.grading"),
      entries: buildOutline(paper.blocks),
      editable: false,
    });
  }, [evaluation.assignmentId, evaluation.classId, paper.blocks]);
  useEffect(() => () => publishOutline(null), []);

  const comments: DocumentComment[] = [
    {
      key: "total-card",
      anchorBlockId: null,
      content: (
        <TotalCard total={total} maxScore={Number(evaluation.maxScore)} />
      ),
    },
    ...detail.rubrics.map(
      (rubric, index): DocumentComment => ({
        key: rubric.rubricId,
        anchorBlockId: blockIds.has(rubric.partId) ? rubric.partId : null,
        content: (
          <RubricNote
            title={
              partTitles[rubric.partId] ||
              msg("grading.pages.GradingWorkspacePage.part", {
                value: String(index + 1),
              })
            }
            criteria={listNoteCriteria(rubric)}
            active={activePart === rubric.rubricId}
            onActivate={() => setActivePart(rubric.rubricId)}
            badge={
              <span className="sr-only">
                {msg("grading.pages.GradingWorkspacePage.score")}
                {String(
                  Number(
                    calculateChecklistPoints(
                      [rubric],
                      form.checks,
                      form.itemScores,
                    ).toFixed(2),
                  ),
                )}
              </span>
            }
            editor={
              <RubricChecklist
                rubric={rubric}
                {...checklist}
                aiFilledItems={aiFilledItems}
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
      }),
    ),
  ];
  const activeHeading =
    evaluation.kind === "GROUP_DOCUMENT" && groupMode === "members"
      ? memberPart
      : (detail.rubrics.find((rubric) => rubric.rubricId === activePart)
          ?.partId ?? null);
  const saveText = saving
    ? msg("grading.pages.GradingWorkspacePage.saving")
    : busy
      ? msg("grading.pages.GradingWorkspacePage.processing")
      : dirty
        ? msg("grading.pages.GradingWorkspacePage.notSaved")
        : msg("grading.pages.GradingWorkspacePage.saved", {
            value: String(formatPoints(evaluation.score, evaluation.maxScore)),
          });
  const isGroupSubmission = evaluation.kind === "GROUP_DOCUMENT";
  const assignmentKind = isGroupSubmission
    ? msg("grading.pages.GradingWorkspacePage.group")
    : msg("grading.pages.GradingWorkspacePage.individual");
  const assignmentTitle =
    context.value?.title ||
    msg("grading.pages.GradingWorkspacePage.assignment");
  useEffect(() => {
    setGradingAiLabel(evaluation.id, `${assignmentTitle} — ${studentName}`);
  }, [evaluation.id, assignmentTitle, studentName]);
  const memberNames = Object.fromEntries(
    (detail.memberActivities ?? []).map(({ student }) => [
      student.accountId,
      formatStudentLabel(student),
    ]),
  );
  const sectionBylines: Record<string, string> = {};
  const sectionActions: Record<string, ReactNode> = {};
  const memberSections: Record<string, { partId: string; title: string }[]> =
    {};
  const unanchoredAuthors = new Set<string>();
  // Chuyển panel sang thành viên và bỏ mục đang chọn khi nhảy sang tài khoản khác.
  function openMember(accountId?: string) {
    if (accountId) {
      setSelectedMember(accountId);
      setMemberPart(null);
    }
    setGroupMode("members");
    setGroupPanelOpen(true);
  }
  if (isGroupSubmission) {
    const headings = new Set(
      paper.blocks
        .filter((block) => block.type === "HEADING")
        .map((block) => block.id),
    );
    const authors = new Map<string, Set<string>>();
    for (const { partId, accountId } of detail.sectionAuthors ?? []) {
      const name =
        memberNames[accountId] ??
        msg("grading.pages.GradingWorkspacePage.member");
      if (headings.has(partId)) {
        const accounts = authors.get(partId) ?? new Set<string>();
        accounts.add(accountId);
        authors.set(partId, accounts);
        const parts = memberSections[accountId] ?? [];
        if (!parts.some((part) => part.partId === partId))
          parts.push({
            partId,
            title:
              partTitles[partId] ??
              msg("grading.pages.GradingWorkspacePage.partOfTheWork"),
          });
        memberSections[accountId] = parts;
      } else unanchoredAuthors.add(name);
    }
    for (const [partId, accounts] of authors) {
      sectionBylines[partId] = msg(
        "grading.pages.GradingWorkspacePage.doneBy",
        {
          value: String(
            [...accounts]
              .map(
                (id) =>
                  memberNames[id] ??
                  msg("grading.pages.GradingWorkspacePage.member"),
              )
              .join(", "),
          ),
        },
      );
      sectionActions[partId] = (
        <div className="flex max-w-40 flex-wrap justify-end gap-1">
          {[...accounts]
            .filter((accountId) =>
              detail.members.some((member) => member.accountId === accountId),
            )
            .map((accountId) => (
              <Button
                key={accountId}
                size="sm"
                variant="ghost"
                className="max-w-full"
                onClick={() => openMember(accountId)}
              >
                <span className="truncate">
                  {msg("grading.pages.GradingWorkspacePage.assess")}
                  {memberNames[accountId] ??
                    msg("grading.pages.GradingWorkspacePage.members")}
                </span>
              </Button>
            ))}
        </div>
      );
    }
  }

  return (
    <div
      data-wide-workspace
      className="flex flex-col gap-4"
      style={{ "--doc-toolbar-top": `${56 + headerHeight}px` } as CSSProperties}
    >
      {workspace.confirmDialog}
      <TopBar
        barRef={headerRef}
        setViewSlot={setViewSlot}
        left={
          <>
            <h1
              className="min-w-0 truncate text-[15px] font-semibold text-ink"
              title={assignmentTitle}
            >
              {assignmentTitle}
            </h1>
            <h1 className="sr-only">{studentName}</h1>
            <Pill
              tone={
                evaluation.state === "PUBLISHED"
                  ? "success"
                  : evaluation.state === "DRAFT" ||
                      evaluation.state === "FINALIZED"
                    ? "info"
                    : "neutral"
              }
              className="shrink-0"
            >
              {GRADE_STATUS_LABELS[evaluation.state]}
            </Pill>
            <span data-testid="save-state" className="sr-only" role="status">
              {saveText}
            </span>
          </>
        }
        actions={
          <>
            <Button
              size="sm"
              variant="secondary"
              disabled={disabled || saving}
              onClick={() => void save(false)}
            >
              {msg("grading.pages.GradingWorkspacePage.saveChanges")}
            </Button>
            <Button
              size="sm"
              variant="primary"
              disabled={
                busy ||
                saving ||
                Boolean(error) ||
                !detail.current ||
                detail.editable === false ||
                evaluation.state === "PUBLISHED"
              }
              onClick={() => void handlePublish()}
            >
              {msg("grading.pages.GradingWorkspacePage.publishGrade")}
            </Button>
          </>
        }
      />

      {!detail.current && (
        <Alert tone="info">
          {msg("grading.pages.GradingWorkspacePage.thisIsAnOldSubmission")}
        </Alert>
      )}
      {detail.editable === false && detail.readOnlyReason && (
        <Alert tone="info">{detail.readOnlyReason}</Alert>
      )}
      {error && (
        <Alert
          tone="danger"
          role="alert"
          action={
            <Button size="sm" onClick={() => void reload()}>
              {msg("grading.pages.GradingWorkspacePage.reloadVersion")}
            </Button>
          }
        >
          {error}
        </Alert>
      )}
      {!detail.rubrics.length && (
        <Alert tone="warning">
          {msg("grading.pages.GradingWorkspacePage.theAssignmentHasNoUsable")}
        </Alert>
      )}

      {draftProposal && (
        <AiDraftBanner
          proposal={draftProposal}
          maxScore={evaluation.maxScore}
          offered={Boolean(offeredProposal)}
          onApply={() => {
            applyReadyProposal(draftProposal);
            setOfferedProposalId(null);
          }}
          onSkip={() => setOfferedProposalId(null)}
          onKeep={keepProposal}
          onUndo={discardProposal}
          onDetails={() => setAiOpen(true)}
        />
      )}
      {aiOpen && (
        <section
          id="grading-ai-review"
          aria-label={msg("grading.pages.GradingWorkspacePage.gradeWithAi")}
          className="scroll-mt-52 border-b border-line pb-5"
        >
          <div className="mb-4 flex items-center justify-between gap-3">
            <h2 className="text-lg font-semibold text-ink">
              {msg("grading.pages.GradingWorkspacePage.gradeWithAi")}
            </h2>
            <Button size="sm" icon="close" onClick={() => setAiOpen(false)}>
              {msg("grading.pages.GradingWorkspacePage.close")}
            </Button>
          </div>
          <AiProposalPanel
            evaluationId={evaluation.id}
            proposals={proposals}
            proposalsLoaded={proposalsLoaded}
            selectedProposalId={proposalId}
            disabled={disabled}
            rubrics={detail.rubrics}
            currentValues={form}
            partTitles={partTitles}
            validBlockIds={validBlockIds}
            pollExhausted={pollExhausted}
            onRecheck={resumePolling}
            onRequest={(input) => void handleAiGrade(input)}
            onApply={(checks, feedback, id, itemScores, structuredFeedback) => {
              applyProposal(
                checks,
                feedback,
                id,
                itemScores,
                structuredFeedback,
              );
              setOfferedProposalId(null);
              setAiOpen(false);
            }}
          />
        </section>
      )}
      {evaluation.state === "PUBLISHED" && (
        <p className="text-xs text-muted">
          {msg("grading.pages.GradingWorkspacePage.changesWillBeVisibleTo")}
        </p>
      )}
      <DocumentEditor
        fullWidth
        matchCommentHeight
        document={paper}
        onChange={() => {}}
        mode="student"
        readOnly
        diagramEnabled={false}
        imageUrl={(blockId) => imageUrls[blockId] ?? null}
        emptyLeafLabel={msg(
          "grading.pages.GradingWorkspacePage.theStudentDidNotDo",
        )}
        titleField={{
          text:
            context.value?.title ||
            msg("grading.pages.GradingWorkspacePage.submittedWork"),
          readOnly: true,
          // Giữ callback theo hợp đồng thanh tiêu đề vì nhãn bản chấm ở đây chỉ để đọc.
          onText: () => {},
        }}
        header={
          <GradingHeaderSection
            assignmentKind={assignmentKind}
            unanchoredAuthors={[...unanchoredAuthors]}
            form={form}
            aiFilledFeedback={isFeedbackAiFilled}
            disabled={disabled}
            setFeedback={setFeedback}
            setAdjustment={setAdjustment}
            setChangeReason={setChangeReason}
            updateStructuredSummary={updateStructuredSummary}
          />
        }
        sectionBylines={sectionBylines}
        sectionActions={sectionActions}
        comments={comments}
        sidePanel={
          isGroupSubmission ? (
            <GroupGradingPanel
              mode={groupMode}
              onModeChange={(mode) => {
                setGroupMode(mode);
                setMemberPart(null);
              }}
              open={groupPanelOpen}
              onClose={() => setGroupPanelOpen(false)}
              selectionKey={selectedMember}
              rubric={comments.map((comment) => (
                <div key={comment.key}>{comment.content}</div>
              ))}
            >
              <MemberContributionPanel
                key={evaluation.submissionHash}
                root={evaluation}
                editable={detail.editable !== false && detail.current}
                members={detail.members}
                names={memberNames}
                selectedAccountId={selectedMember}
                onSelectMember={(accountId) => {
                  setSelectedMember(accountId);
                  setMemberPart(null);
                }}
                sections={memberSections}
                onShowPart={(partId) => {
                  setMemberPart(partId);
                  setGroupPanelOpen(false);
                  window.dispatchEvent(
                    new CustomEvent("document:show-part", {
                      detail: { headingId: partId },
                    }),
                  );
                }}
                onDirtyChange={setMembersDirty}
                onSaved={() => void reload()}
              />
            </GroupGradingPanel>
          ) : undefined
        }
        activeComment={activePart}
        highlightBlockId={activeHeading}
        viewSlot={viewSlot}
        toolbarCenter={
          <>
            <ToolbarButton
              icon="chevron_left"
              title={msg(
                "grading.pages.GradingWorkspacePage.previousSubmission",
              )}
              aria-label={msg(
                "grading.pages.GradingWorkspacePage.previousSubmission",
              )}
              disabled={currentIndex <= 0 || busy || saving}
              onClick={() => navigate(detail.navigationIds[currentIndex - 1]!)}
            />
            <StudentPickerDropdown
              evaluationId={evaluation.id}
              busy={busy || saving}
              studentName={studentName}
              studentEmail={studentEmail}
              studentCode={studentCode}
              directory={directory}
              onSelect={(targetId) => navigate(targetId, false)}
            />
            <ToolbarButton
              icon="chevron_right"
              title={msg("grading.pages.GradingWorkspacePage.nextSubmission")}
              aria-label={msg(
                "grading.pages.GradingWorkspacePage.nextSubmission",
              )}
              disabled={
                currentIndex < 0 ||
                currentIndex >= detail.navigationIds.length - 1 ||
                busy ||
                saving
              }
              onClick={() => navigate(detail.navigationIds[currentIndex + 1]!)}
            />
          </>
        }
        toolbarExtra={
          // Ba nút cách nhau rõ, không dính vào nhau.
          <div className="ml-2 flex items-center gap-2">
            {isGroupSubmission && detail.members.length > 0 && (
              <>
                <ToolbarButton
                  icon="description"
                  title={msg("grading.pages.GradingWorkspacePage.groupRubric")}
                  aria-label={msg(
                    "grading.pages.GradingWorkspacePage.groupRubric",
                  )}
                  className="xl:hidden"
                  onClick={() => {
                    setGroupMode("rubric");
                    setMemberPart(null);
                    setGroupPanelOpen(true);
                  }}
                />
                <ToolbarButton
                  icon="groups"
                  title={msg("grading.pages.GradingWorkspacePage.memberScores")}
                  aria-label={msg(
                    "grading.pages.GradingWorkspacePage.memberScores",
                  )}
                  aria-controls="group-member-grades"
                  onClick={() => openMember()}
                >
                  <span className="hidden xl:inline">
                    {msg("grading.pages.GradingWorkspacePage.memberScores")}
                  </span>
                </ToolbarButton>
              </>
            )}
            <ToolbarButton
              icon="history"
              title={msg("grading.pages.GradingWorkspacePage.viewAiProposal")}
              aria-label={msg(
                "grading.pages.GradingWorkspacePage.viewAiProposal",
              )}
              aria-expanded={aiOpen}
              aria-controls={aiOpen ? "grading-ai-review" : undefined}
              onClick={() => setAiOpen((open) => !open)}
            >
              <span className="hidden md:inline">
                {msg("grading.pages.GradingWorkspacePage.viewAiProposal")}
              </span>
            </ToolbarButton>
            {queuedProposal && (
              <ToolbarButton
                icon="close"
                title={msg(
                  "grading.pages.GradingWorkspacePage.cancelAiGrading",
                )}
                aria-label={msg(
                  "grading.pages.GradingWorkspacePage.cancelAiGrading",
                )}
                onClick={() => void handleCancelAi()}
              >
                <span className="hidden md:inline">
                  {msg("grading.pages.GradingWorkspacePage.cancelAiGrading")}
                </span>
              </ToolbarButton>
            )}
            <ToolbarButton
              icon="smart_toy"
              title={
                evaluation.kind === "MEMBER"
                  ? msg(
                      "grading.pages.GradingWorkspacePage.openTheGroupSubmissionTo",
                    )
                  : msg("grading.pages.GradingWorkspacePage.gradeWithAi")
              }
              aria-label={msg("grading.pages.GradingWorkspacePage.gradeWithAi")}
              className="h-9! gap-2! px-3! text-sm! font-semibold!"
              aria-busy={aiGenerating}
              disabled={
                disabled ||
                busy ||
                saving ||
                (evaluation.kind === "MEMBER"
                  ? !detail.groupEvaluationId
                  : aiGenerating)
              }
              onClick={() => void handleAiGrade()}
            >
              {aiGenerating
                ? msg("grading.pages.GradingWorkspacePage.theAiIsGrading")
                : msg("grading.pages.GradingWorkspacePage.gradeWithAi")}
            </ToolbarButton>
          </div>
        }
      />

      <div className="mt-6">
        <ActivityReview detail={detail} />
      </div>
      {action && !disabled && (
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
