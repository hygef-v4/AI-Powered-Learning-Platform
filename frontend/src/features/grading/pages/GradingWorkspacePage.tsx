"use client";
import { formatStudentLabel } from "@/shared/studentCode";
import { useCallback, useEffect, useMemo, useRef, useState, type CSSProperties, type ReactNode } from "react";
import {
  DocumentEditor,
  RubricNote,
  ToolbarButton,
  outlineOf,
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
import { buildChecklistBindings } from "../components/GradingForm";
import { useGradingWorkspace } from "../hooks/useGradingWorkspace";
import { ActivityReview } from "../components/GradingReviewPanels";
import { AiProposalPanel, buildAiRequest } from "../components/AiProposalPanel";
import { BatchActionDialog } from "../components/BatchActionDialog";
import { MemberContributionPanel } from "../components/MemberContributionPanel";
import { GroupGradingPanel } from "../components/GroupGradingPanel";
import { buildGradebookHref } from "../components/ClassProgressSection";
import {
  GradingHeaderSection,
  StudentPickerDropdown,
  TopBar,
  TotalCard,
  noteCriteria,
  useSubmissionImages,
} from "../components/GradingWorkspaceHeader";
import { formatPoints, GRADE_STATUS_LABELS, type AiRequest, type Detail } from "../model/types";
import { toEditorDocument } from "../model/submissionDocument";
import { PageLoadError, PageLoading } from "@/shared/ui/PageStatus";
import type { NavigationFilters } from "../model/navigation";
import { setGradingAiLabel } from "../model/gradingAiTasks";
import { msg } from "@/i18n/msg";

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
    <PageLoading label={msg("grading.pages.GradingWorkspacePage.dangTaiBaiLam")} />
  );
}

function Workspace({
  initial,
  refresh,
  proposalId,
  status,
  late,
}: { initial: Detail; refresh: () => Promise<Detail>; proposalId?: string } & NavigationFilters) {
  const [membersDirty, setMembersDirty] = useState(false);
  const [groupMode, setGroupMode] = useState<"rubric" | "members">("rubric");
  const [groupPanelOpen, setGroupPanelOpen] = useState(false);
  const [selectedMember, setSelectedMember] = useState<string | null>(null);
  const [memberPart, setMemberPart] = useState<string | null>(null);
  const workspace = useGradingWorkspace(initial, refresh, { status, late }, membersDirty);
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
  const [action, setAction] = useState<"publish" | null>(null);
  const [aiOpen, setAiOpen] = useState(Boolean(proposalId));
  const [aiRequesting, setAiRequesting] = useState(false);
  const aiGenerating = aiRequesting || proposals.some((proposal) => proposal.status === "PENDING");
  const [reviewSelection, setReviewSelection] = useState(proposalId);
  if (reviewSelection !== proposalId) {
    setReviewSelection(proposalId);
    setAiOpen(Boolean(proposalId));
  }

  const handleAiGrade = async (input?: AiRequest) => {
    if (disabled || busy || saving) return;
    if (evaluation.kind === "MEMBER") {
      if (detail.groupEvaluationId && !busy && !saving) navigate(detail.groupEvaluationId);
      return;
    }
    if (disabled || busy || saving || aiGenerating) return;
    setAiOpen(false);
    setAiRequesting(true);
    const created = await requestProposal(input ?? buildAiRequest());
    setAiRequesting(false);
    if (created) setGradingAiLabel(evaluation.id, `${assignmentTitle} — ${studentName}`);
    if (created?.status === "FAILED" || created?.status === "REJECTED") {
      if (created.safeError) showErrorToast(created.safeError);
    } else if (created) {
      showToast(created.status === "READY"
        ? msg("grading.pages.GradingWorkspacePage.daCoDeXuatAiMoi")
        : msg("grading.pages.GradingWorkspacePage.daGuiChamAiBanCo"));
    }
  };

  const handlePublish = async () => {
    if (busy || saving || disabled) return;
    if (membersDirty) {
      showErrorToast(msg("grading.pages.GradingWorkspacePage.banCoDanhGiaThanhVien"));
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
    (evaluation.kind === "GROUP_DOCUMENT" ? msg("grading.pages.GradingWorkspacePage.baiLamNhom") : msg("grading.pages.GradingWorkspacePage.baiLamSinhVien"));
  const studentEmail = [...new Set(submitters.map((row) => row.student.email))].join(", ") || "";
  const studentCode = [...new Set(submitters.map((row) => row.student.studentCode).filter(Boolean))].join(", ");
  const paper = useMemo(() => toEditorDocument(detail.document), [detail.document]);
  const imageUrls = useSubmissionImages(evaluation.id, detail.document);
  const partTitles: Record<string, string> = {
    ASSIGNMENT: msg("grading.pages.GradingWorkspacePage.toanBai"),
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
      backLabel: msg("grading.pages.GradingWorkspacePage.chamDiem"),
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
          title={partTitles[rubric.partId] || msg("grading.pages.GradingWorkspacePage.phan", { value: String(index + 1) })}
          criteria={noteCriteria(rubric)}
          active={activePart === rubric.rubricId}
          onActivate={() => setActivePart(rubric.rubricId)}
          badge={
            <span className="sr-only">
              {msg("grading.pages.GradingWorkspacePage.dat")}{String(Number(calculateChecklistPoints([rubric], form.checks, form.itemScores).toFixed(2)))}
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
  const activeHeading = evaluation.kind === "GROUP_DOCUMENT" && groupMode === "members"
    ? memberPart : detail.rubrics.find((rubric) => rubric.rubricId === activePart)?.partId ?? null;
  const saveText = saving ? msg("grading.pages.GradingWorkspacePage.dangLuu") : busy ? msg("grading.pages.GradingWorkspacePage.dangXuLy") : dirty ? msg("grading.pages.GradingWorkspacePage.chuaLuu") : msg("grading.pages.GradingWorkspacePage.daLuu", { value: String(formatPoints(evaluation.score, evaluation.maxScore)) });
  const isGroupSubmission = evaluation.kind === "GROUP_DOCUMENT";
  const assignmentKind = isGroupSubmission ? msg("grading.pages.GradingWorkspacePage.nhom") : msg("grading.pages.GradingWorkspacePage.caNhan");
  const assignmentTitle = context.value?.title || msg("grading.pages.GradingWorkspacePage.baiTap");
  useEffect(() => {
    setGradingAiLabel(evaluation.id, `${assignmentTitle} — ${studentName}`);
  }, [evaluation.id, assignmentTitle, studentName]);
  const memberNames = Object.fromEntries(
    (detail.memberActivities ?? []).map(({ student }) => [student.accountId, formatStudentLabel(student)]),
  );
  const sectionBylines: Record<string, string> = {};
  const sectionActions: Record<string, ReactNode> = {};
  const memberSections: Record<string, { partId: string; title: string }[]> = {};
  const unanchoredAuthors = new Set<string>();
  function openMember(accountId?: string) {
    if (accountId) { setSelectedMember(accountId); setMemberPart(null); }
    setGroupMode("members");
    setGroupPanelOpen(true);
  }
  if (isGroupSubmission) {
    const headings = new Set(paper.blocks.filter((block) => block.type === "HEADING").map((block) => block.id));
    const authors = new Map<string, Set<string>>();
    for (const { partId, accountId } of detail.sectionAuthors ?? []) {
      const name = memberNames[accountId] ?? msg("grading.pages.GradingWorkspacePage.thanhVien");
      if (headings.has(partId)) {
        const accounts = authors.get(partId) ?? new Set<string>();
        accounts.add(accountId);
        authors.set(partId, accounts);
        const parts = memberSections[accountId] ?? [];
        if (!parts.some(part => part.partId === partId)) parts.push({ partId, title: partTitles[partId] ?? msg("grading.pages.GradingWorkspacePage.phanBaiLam") });
        memberSections[accountId] = parts;
      } else unanchoredAuthors.add(name);
    }
    for (const [partId, accounts] of authors) {
      sectionBylines[partId] = msg("grading.pages.GradingWorkspacePage.nguoiThucHien", { value: String([...accounts].map(id => memberNames[id] ?? msg("grading.pages.GradingWorkspacePage.thanhVien")).join(", ")) });
      sectionActions[partId] = <div className="flex max-w-40 flex-wrap justify-end gap-1">
        {[...accounts].filter(accountId => detail.members.some(member => member.accountId === accountId)).map(accountId => (
          <Button key={accountId} size="sm" variant="ghost" className="max-w-full" onClick={() => openMember(accountId)}>
            <span className="truncate">{msg("grading.pages.GradingWorkspacePage.danhGia")}{memberNames[accountId] ?? msg("grading.pages.GradingWorkspacePage.thanhVien2")}</span>
          </Button>
        ))}
      </div>;
    }
  }

  return (
    <div data-wide-workspace className="flex flex-col gap-4" style={{ "--doc-toolbar-top": `${56 + headerHeight}px` } as CSSProperties}>
      {workspace.confirmDialog}
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
            <Button size="sm" variant="secondary" disabled={disabled || saving} onClick={() => void save(false)}>
              {msg("grading.pages.GradingWorkspacePage.luuThayDoi")}</Button>
            <Button
              size="sm"
              variant="primary"
              disabled={busy || saving || Boolean(error) || !detail.current || detail.editable === false || evaluation.state === "PUBLISHED"}
              onClick={() => void handlePublish()}
            >
              {msg("grading.pages.GradingWorkspacePage.congBoDiem")}</Button>
          </>
        }
      />

      {!detail.current && <Alert tone="info">{msg("grading.pages.GradingWorkspacePage.dayLaLanNopCuChi")}</Alert>}
      {detail.editable === false && detail.readOnlyReason && <Alert tone="info">{detail.readOnlyReason}</Alert>}
      {error && (
        <Alert tone="danger" role="alert" action={<Button size="sm" onClick={() => void reload()}>{msg("grading.pages.GradingWorkspacePage.taiLaiPhienBan")}</Button>}>{error}</Alert>
      )}
      {!detail.rubrics.length && <Alert tone="warning">{msg("grading.pages.GradingWorkspacePage.baiChuaCoRubricKhaDung")}</Alert>}

      {evaluation.state === "PUBLISHED" && <p className="text-xs text-muted">
        {msg("grading.pages.GradingWorkspacePage.thayDoiSeHienThiCho")}</p>}
      <DocumentEditor
        fullWidth
        matchCommentHeight
        document={paper}
        onChange={() => {}}
        mode="student"
        readOnly
        diagramEnabled={false}
        imageUrl={(blockId) => imageUrls[blockId] ?? null}
        emptyLeafLabel={msg("grading.pages.GradingWorkspacePage.sinhVienChuaLamPhanNay")}
        titleField={{ text: context.value?.title || msg("grading.pages.GradingWorkspacePage.baiLamDaNop"), readOnly: true, onText: () => {} }}
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
        sectionActions={sectionActions}
        comments={comments}
        sidePanel={isGroupSubmission ? (
          <GroupGradingPanel mode={groupMode} onModeChange={(mode) => { setGroupMode(mode); setMemberPart(null); }}
            open={groupPanelOpen} onClose={() => setGroupPanelOpen(false)}
            selectionKey={selectedMember}
            rubric={comments.map(comment => <div key={comment.key}>{comment.content}</div>)}>
            <MemberContributionPanel key={evaluation.submissionHash} root={evaluation}
              editable={detail.editable !== false && detail.current} members={detail.members} names={memberNames}
              selectedAccountId={selectedMember} onSelectMember={(accountId) => { setSelectedMember(accountId); setMemberPart(null); }}
              sections={memberSections} onShowPart={(partId) => {
                setMemberPart(partId);
                setGroupPanelOpen(false);
                window.dispatchEvent(new CustomEvent("document:show-part", { detail: { headingId: partId } }));
              }} onDirtyChange={setMembersDirty} onSaved={() => void reload()} />
          </GroupGradingPanel>
        ) : undefined}
        activeComment={activePart}
        highlightBlockId={activeHeading}
        viewSlot={viewSlot}
        toolbarCenter={
          <>
            <ToolbarButton
              icon="chevron_left"
              title={msg("grading.pages.GradingWorkspacePage.baiTruoc")}
              aria-label={msg("grading.pages.GradingWorkspacePage.baiTruoc")}
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
              title={msg("grading.pages.GradingWorkspacePage.baiSau")}
              aria-label={msg("grading.pages.GradingWorkspacePage.baiSau")}
              disabled={currentIndex < 0 || currentIndex >= detail.navigationIds.length - 1 || busy || saving}
              onClick={() => navigate(detail.navigationIds[currentIndex + 1]!)}
            />
          </>
        }
        toolbarExtra={
          // Ba nút cách nhau rõ, không dính vào nhau.
          <div className="ml-2 flex items-center gap-2">
            {isGroupSubmission && detail.members.length > 0 && (
              <>
              <ToolbarButton icon="description" title={msg("grading.pages.GradingWorkspacePage.rubricNhom")} aria-label={msg("grading.pages.GradingWorkspacePage.rubricNhom")} className="xl:hidden"
                onClick={() => { setGroupMode("rubric"); setMemberPart(null); setGroupPanelOpen(true); }} />
              <ToolbarButton
                icon="groups"
                title={msg("grading.pages.GradingWorkspacePage.diemThanhVien")}
                aria-label={msg("grading.pages.GradingWorkspacePage.diemThanhVien")}
                aria-controls="group-member-grades"
                onClick={() => openMember()}
              >
                <span className="hidden xl:inline">{msg("grading.pages.GradingWorkspacePage.diemThanhVien")}</span>
              </ToolbarButton>
              </>
            )}
            <ToolbarButton
              icon="history"
              title={msg("grading.pages.GradingWorkspacePage.xemDeXuatAi")}
              aria-label={msg("grading.pages.GradingWorkspacePage.xemDeXuatAi")}
              aria-expanded={aiOpen}
              aria-controls={aiOpen ? "grading-ai-review" : undefined}
              onClick={() => setAiOpen((open) => !open)}
            >
              <span className="hidden md:inline">{msg("grading.pages.GradingWorkspacePage.xemDeXuatAi")}</span>
            </ToolbarButton>
            <ToolbarButton
              icon="smart_toy"
              title={evaluation.kind === "MEMBER" ? msg("grading.pages.GradingWorkspacePage.moBaiNopNhomDeCham") : msg("grading.pages.GradingWorkspacePage.chamBangAi")}
              aria-label={msg("grading.pages.GradingWorkspacePage.chamBangAi")}
              className="h-9! gap-2! px-3! text-sm! font-semibold!"
              aria-busy={aiGenerating}
              disabled={disabled || busy || saving || (evaluation.kind === "MEMBER" ? !detail.groupEvaluationId : aiGenerating)}
              onClick={() => void handleAiGrade()}
            >
              {aiGenerating ? msg("grading.pages.GradingWorkspacePage.aiDangCham") : msg("grading.pages.GradingWorkspacePage.chamBangAi")}
            </ToolbarButton>
          </div>
        }
      />

      <div className={detail.activityState === "DISABLED" ? "hidden" : "mt-6"}>
        <ActivityReview detail={detail} />
      </div>
      {aiOpen && (
        <section id="grading-ai-review" aria-label={msg("grading.pages.GradingWorkspacePage.chamBangAi")} className="scroll-mt-52 border-t border-line pt-5">
          <div className="mb-4 flex items-center justify-between gap-3">
            <h2 className="text-lg font-semibold text-ink">{msg("grading.pages.GradingWorkspacePage.chamBangAi")}</h2>
            <Button size="sm" icon="close" onClick={() => setAiOpen(false)}>{msg("grading.pages.GradingWorkspacePage.dong")}</Button>
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
              applyProposal(checks, feedback, id, itemScores, structuredFeedback);
              setAiOpen(false);
            }}
          />
        </section>
      )}
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
