"use client";
import { BackLink } from "@/shared/navigation/BackLink";
import { useCallback, useEffect, useMemo, useState } from "react";
import { apiFetch } from "@/shared/api/client";
import {
  DocumentEditor,
  RubricNote,
  ToolbarButton,
  type DocumentComment,
  type DocumentModel as EditorDocument,
  type RubricCriterion,
} from "@/shared/document";
import { Alert, Button, Icon, IconButton, Pill } from "@/shared/ui";
import { checklistPoints, RubricChecklist } from "../components/RubricChecklistPanel";
import { useSubmissionContext } from "../hooks/useSubmissionContext";
import { useSubmissionDirectory } from "../hooks/useSubmissionDirectory";
import { gradingApi } from "../api";
import { useResource } from "../hooks/useResource";
import { checklistBindings, GradingNotes, needsGradingNotes } from "../components/GradingForm";
import { useGradingWorkspace } from "../hooks/useGradingWorkspace";
import { ActivityReview, GradeHistory, GroupSectionAuthors } from "../components/GradingReviewPanels";
import { AiProposalPanel } from "../components/AiProposalPanel";
import { BatchActionDialog } from "../components/BatchActionDialog";
import { MemberContributionPanel } from "../components/MemberContributionPanel";
import { countUnresolvedReviews } from "../components/StructuredFeedbackSection";
import { points, statusLabels, type Detail, type DocumentModel, type Rubric } from "../model/types";
import { PageLoadError, PageLoading } from "@/shared/ui/PageStatus";

export function GradingWorkspacePage({ evaluationId, proposalId }: { evaluationId: string; proposalId?: string }) {
  const load = useCallback(() => gradingApi.detail(evaluationId), [evaluationId]);
  const resource = useResource(load);
  return resource.value ? (
    <Workspace key={evaluationId} initial={resource.value} refresh={resource.refresh} proposalId={proposalId} />
  ) : resource.error ? (
    <PageLoadError onRetry={() => void resource.refresh()}>{resource.error}</PageLoadError>
  ) : (
    <PageLoading label="Đang tải bài làm…" />
  );
}

/** Bài nộp hiện bằng trình soạn chỉ đọc như trang soạn bài; ảnh có fileId thì đánh dấu có tệp để lấy link. */
function editorDocument(document: DocumentModel): EditorDocument {
  return {
    blocks: document.blocks.map((block) => block.type === "IMAGE"
      ? { ...block, content: { ...block.content, hasFile: typeof block.content.fileId === "string" && block.content.fileId !== "" } }
      : block),
  } as unknown as EditorDocument;
}

/** Link tải có hạn cho từng ảnh trong bài nộp (U03 qua U09), lấy một lần khi mở bài. */
function useSubmissionImages(evaluationId: string, document: DocumentModel): Record<string, string> {
  const [urls, setUrls] = useState<Record<string, string>>({});
  useEffect(() => {
    let active = true;
    const images = document.blocks.filter((block) => block.type === "IMAGE" && typeof block.content.fileId === "string" && block.content.fileId);
    void Promise.all(images.map(async (block): Promise<[string, string] | null> => {
      try {
        const token = await apiFetch<{ url: string }>(
          `/api/v1/evaluations/${evaluationId}/images/${encodeURIComponent(String(block.content.fileId))}`,
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

function Workspace({ initial, refresh, proposalId }: { initial: Detail; refresh: () => Promise<Detail>; proposalId?: string }) {
  const workspace = useGradingWorkspace(initial, refresh);
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
  const [focusMode, setFocusMode] = useState(false);
  const [aiOpen, setAiOpen] = useState(Boolean(proposalId));
  const [reviewSelection, setReviewSelection] = useState(proposalId);
  if (reviewSelection !== proposalId) {
    setReviewSelection(proposalId);
    setAiOpen(Boolean(proposalId));
  }
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
  const studentMatches = (directory.value ?? []).filter((row) => row.evaluationId &&
    `${row.student.name} ${row.student.email}`.toLocaleLowerCase("vi").includes(studentSearch.trim().toLocaleLowerCase("vi")));
  const paper = useMemo(() => editorDocument(detail.document), [detail.document]);
  const imageUrls = useSubmissionImages(evaluation.id, detail.document);
  const partTitles = Object.fromEntries(detail.document.blocks.filter((block) => block.type === "HEADING" && typeof block.content.text === "string")
    .map((block) => [block.id, block.content.text as string]));
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
  const total = Math.min(Number(evaluation.maxScore), Math.max(0, checklistPoints(detail.rubrics, form.checks, form.itemScores) + Number(form.adjustment)));
  const unresolvedReviews = countUnresolvedReviews(detail.rubrics, form.checks, form.itemScores,
    form.structuredFeedback, form.reviewedItemScores, form.acknowledgedReviewItems).total;
  const checklist = { ...checklistBindings(form, disabled, updateForm), validBlockIds };
  const blockIds = new Set(paper.blocks.map((block) => block.id));
  const pending = proposals.some((proposal) => proposal.status === "PENDING");
  const ready = proposals[0]?.status === "READY";

  // Lề chỉ để điểm: mỗi phần một thẻ checklist gắn vào tiêu đề của phần; thẻ ghi chú ở đầu chỉ khi cần.
  const comments: DocumentComment[] = [
    ...(needsGradingNotes(detail.rubrics, form, official) ? [{
      key: "grading-notes",
      anchorBlockId: null,
      content: <GradingNotes rubrics={detail.rubrics} values={form} />,
    }] : []),
    ...detail.rubrics.map((rubric, index): DocumentComment => ({
      key: rubric.rubricId,
      // Rubric gắn với tiêu đề của phần; rubric cả bài (không có tiêu đề) nằm ở đầu tài liệu.
      anchorBlockId: blockIds.has(rubric.partId) ? rubric.partId : null,
      content: (
        <RubricNote title={partTitles[rubric.partId] || `Phần ${index + 1}`} criteria={noteCriteria(rubric)}
          active={activePart === rubric.rubricId} onActivate={() => setActivePart(rubric.rubricId)}
          badge={<Pill tone="primary">Đạt {String(Number(checklistPoints([rubric], form.checks, form.itemScores).toFixed(2)))}</Pill>}
          editor={<RubricChecklist rubric={rubric} {...checklist} />} />
      ),
    })),
  ];
  const activeHeading = detail.rubrics.find((rubric) => rubric.rubricId === activePart)?.partId ?? null;

  return (
    <main data-wide-workspace data-workspace-focus={focusMode} data-workspace-main="true" className="flex min-w-0 flex-col gap-5">
      <header className="flex flex-wrap items-end justify-between gap-x-6 gap-y-3">
        <div className="flex min-w-0 flex-1 flex-col gap-2.5">
          <BackLink className="inline-flex w-fit items-center gap-1.5 text-[12.5px] font-semibold text-muted transition-colors hover:text-ink"
            onClick={(event) => { if (!confirmNavigation()) event.preventDefault(); }}
            href={`/teaching/classes/${evaluation.classId}/assignments/${evaluation.assignmentId}/submissions`}>
            <Icon name="arrow_back" size={14} />Bài nộp
          </BackLink>
          <h1 className="font-display text-[28px] leading-[1.1] tracking-[-0.025em] text-ink">{studentName}</h1>
          {context.value?.title && <p className="text-sm text-muted">{context.value.title}</p>}
          <div className="flex flex-wrap items-center gap-2.5 text-[13px] text-muted">
            <Pill tone={evaluation.state === "PUBLISHED" ? "success" : evaluation.state === "FINALIZED" ? "info" : "neutral"}>{statusLabels[evaluation.state]}</Pill>
            {context.value && <span className="font-semibold text-ink">{context.value.ownerName}</span>}
          </div>
        </div>
        <Button size="sm" variant="ghost" icon={focusMode ? "close" : "visibility"} aria-pressed={focusMode} onClick={() => setFocusMode((value) => !value)}>
          {focusMode ? "Thoát tập trung" : "Chế độ tập trung"}
        </Button>
      </header>
      <div role="region" aria-label="Thao tác chấm bài"
        className="sticky top-16 z-30 flex flex-wrap items-center justify-between gap-x-6 gap-y-3 rounded-xl border border-line bg-[var(--c-topbar)] px-4 py-3 shadow-[var(--shadow-lift)] backdrop-blur-md">
        <div className="flex items-center gap-1">
          <IconButton icon="chevron_left" label="Bài trước" disabled={currentIndex <= 0 || busy} onClick={() => navigate(detail.navigationIds[currentIndex - 1]!)} />
          <span className="min-w-12 text-center font-mono text-xs text-muted">{Math.max(1, currentIndex + 1)} / {detail.navigationIds.length || 1}</span>
          <IconButton icon="chevron_right" label="Bài sau" disabled={currentIndex < 0 || currentIndex >= detail.navigationIds.length - 1 || busy} onClick={() => navigate(detail.navigationIds[currentIndex + 1]!)} />
          <details className="ml-2">
            <summary aria-label="Chọn sinh viên" title="Chọn sinh viên khác" className="max-w-[min(16rem,45vw)] cursor-pointer rounded-lg px-2 py-2 text-sm font-semibold break-words text-ink">{studentName}</summary>
            <div className="absolute left-0 top-full z-20 mt-2 w-[min(22rem,calc(100vw-3rem))] rounded-xl border border-line bg-surface p-3 shadow-[var(--shadow-float)]">
              <label className="flex flex-col gap-2 text-sm font-semibold text-ink">
                Tìm sinh viên
                <input type="search" placeholder="Tên hoặc email" value={studentSearch} onChange={(event) => setStudentSearch(event.target.value)}
                  className="w-full rounded-lg border border-field px-3 py-2 text-sm font-normal" />
              </label>
              {directory.error ? <p role="alert" className="mt-3 text-sm text-danger">Không tải được danh sách. <button type="button" className="underline" onClick={() => void directory.refresh().catch(() => {})}>Thử lại</button></p>
                : !directory.value ? <p role="status" className="mt-3 text-sm text-muted">Đang tải sinh viên…</p>
                  : <ul aria-label="Bài nộp sinh viên" className="mt-2 max-h-64 overflow-y-auto">
                    {studentMatches.map((row) => <li key={row.student.accountId}>
                      <button type="button" disabled={busy || row.evaluationId === evaluation.id} onClick={() => navigate(row.evaluationId!)}
                        className="w-full cursor-pointer rounded-lg px-2 py-2 text-left text-sm hover:bg-sunken disabled:cursor-default disabled:opacity-50">
                        <span className="block font-semibold">{row.student.name}</span><span className="block text-xs text-muted">{row.student.email}</span>
                      </button>
                    </li>)}
                    {!studentMatches.length && <li className="py-3 text-sm text-muted">Không có bài nộp phù hợp.</li>}
                  </ul>}
            </div>
          </details>
        </div>
        <div className="flex items-baseline gap-3">
          <output aria-label="Điểm tạm tính" className="font-display text-[26px] leading-none tracking-[-0.02em] text-ink tabular-nums">
            {total.toFixed(2)}<span className="text-[18px] text-muted"> / {Number(evaluation.maxScore).toFixed(2)}</span>
          </output>
          <p role="status" className={dirty ? "text-xs font-semibold text-warning" : "text-xs text-muted"}>
            {busy ? "Đang xử lý…" : dirty ? "Chưa lưu" : `Đã lưu ${points(evaluation.score, evaluation.maxScore)}`}
          </p>
          {Number(form.adjustment) !== 0 && <p className="text-xs text-muted">Điều chỉnh đã lưu: {form.adjustment} · {form.adjustmentReason}</p>}
        </div>
        <div className="flex flex-wrap items-center gap-1.5">
          <Button size="sm" variant="primary" icon="save" disabled={disabled} onClick={() => void save()}>Lưu nháp / thay đổi</Button>
          <Button size="sm" disabled={disabled || dirty || evaluation.state !== "DRAFT" || unresolvedReviews > 0} onClick={() => setAction("finalize")}>Chốt điểm</Button>
          <Button size="sm" disabled={busy || !detail.current || dirty || evaluation.state !== "FINALIZED"} onClick={() => setAction("publish")}>Công bố điểm</Button>
        </div>
      </div>
      {!detail.current && <Alert tone="info">Đây là lần nộp cũ, chỉ có thể xem lại.</Alert>}
      {official && <Alert tone="info">Điểm đã chốt. Bản chấm chỉ có thể xem, không thể chỉnh sửa.</Alert>}
      {error && (
        <Alert tone="danger" role="alert" action={<Button size="sm" onClick={() => void reload()}>Tải lại phiên bản</Button>}>{error}</Alert>
      )}
      {!detail.rubrics.length && <Alert tone="warning">Bài chưa có rubric khả dụng.</Alert>}
      <DocumentEditor fullWidth matchCommentHeight document={paper} onChange={() => {}} mode="student" readOnly diagramEnabled={false}
        imageUrl={(blockId) => imageUrls[blockId] ?? null}
        emptyLeafLabel="Sinh viên chưa làm phần này."
        header={<div className="mb-5 border-b border-line pb-4">
          <h2 className="font-display text-[28px] leading-tight tracking-[-0.02em] text-ink">{context.value?.title || "Bài làm đã nộp"}</h2>
          <p className="mt-1 text-[13px] text-muted">Bài làm đã nộp · chỉ đọc</p>
        </div>}
        comments={comments}
        activeComment={activePart}
        highlightBlockId={activeHeading}
        toolbarExtra={<>
          {pending && <Pill tone="warning">AI đang chấm…</Pill>}
          {!pending && ready && <Pill tone="success">Có đề xuất AI</Pill>}
          <ToolbarButton icon="smart_toy" title="Chấm bằng AI" aria-label="Chấm bằng AI" aria-expanded={aiOpen} aria-controls="grading-ai-review" onClick={() => setAiOpen((value) => !value)} />
        </>} />
      <GroupSectionAuthors detail={detail} />
      {evaluation.kind === "GROUP_DOCUMENT" && (
        <MemberContributionPanel
          root={evaluation}
          members={detail.members}
          names={Object.fromEntries(
            (detail.memberActivities ?? []).map((m) => [m.student.accountId, m.student.name]),
          )}
          onSaved={() => void reload()}
        />
      )}
      <ActivityReview detail={detail} />
      <GradeHistory evaluation={evaluation} />
      {aiOpen && (
        <section id="grading-ai-review" aria-label="Chấm bằng AI" className="scroll-mt-52 border-t border-line pt-5">
          <div className="mb-4 flex items-center justify-between gap-3">
            <h2 className="text-lg font-semibold text-ink">Chấm bằng AI</h2>
            <Button size="sm" icon="close" onClick={() => setAiOpen(false)}>Đóng</Button>
          </div>
          <AiProposalPanel evaluationId={evaluation.id} proposals={proposals} proposalsLoaded={proposalsLoaded} selectedProposalId={proposalId} disabled={disabled || official}
            rubrics={detail.rubrics} currentValues={form} partTitles={partTitles} validBlockIds={validBlockIds}
            pollExhausted={pollExhausted} onRecheck={resumePolling}
            onRequest={(input) => void requestProposal(input)}
            onApply={(checks, feedback, id, itemScores, structuredFeedback) => {
              applyProposal(checks, feedback, id, itemScores, structuredFeedback);
              setAiOpen(false);
            }} />
        </section>
      )}
      {action && (
        <BatchActionDialog
          action={action}
          items={[{ evaluationId: evaluation.id, version: evaluation.version }]}
          onClose={() => setAction(null)}
          onDone={() => void reload()}
        />
      )}
    </main>
  );
}
