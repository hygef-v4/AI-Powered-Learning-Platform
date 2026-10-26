"use client";
import Link from "next/link";
import { useCallback, useMemo, useState } from "react";
import { Button, Pill } from "@/shared/ui";
import { checklistPoints } from "./RubricChecklistPanel";
import { useSubmissionContext } from "./useSubmissionContext";
import { gradingApi } from "./api";
import { useResource } from "./useResource";
import { GradingForm } from "./GradingForm";
import { useGradingWorkspace } from "./useGradingWorkspace";
import { ActivityReview, GradeHistory, GroupSectionAuthors } from "./GradingReviewPanels";
import { SubmissionViewer } from "./SubmissionViewer";
import { AiProposalPanel } from "./AiProposalPanel";
import { BatchActionDialog } from "./BatchActionDialog";
import { MemberContributionPanel } from "./MemberContributionPanel";
import { points, statusLabels, type Detail } from "./types";

export function GradingWorkspacePage({ evaluationId }: { evaluationId: string }) {
  const load = useCallback(() => gradingApi.detail(evaluationId), [evaluationId]);
  const resource = useResource(load);
  return resource.value ? (
    <Workspace key={evaluationId} initial={resource.value} refresh={resource.refresh} />
  ) : (
    <p role={resource.error ? "alert" : "status"}>{resource.error || "Đang tải bài làm…"}</p>
  );
}
function Workspace({ initial, refresh }: { initial: Detail; refresh: () => Promise<Detail> }) {
  const workspace = useGradingWorkspace(initial, refresh);
  const {
    detail,
    evaluation,
    form,
    busy,
    error,
    dirty,
    proposals,
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
  const context = useSubmissionContext(evaluation.classId, evaluation.assignmentId);
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
  const total = Math.min(Number(evaluation.maxScore), Math.max(0, checklistPoints(detail.rubrics, form.checks, form.itemScores) + (Number(form.adjustment) || 0)));
  return (
    <main data-workspace-focus={focusMode} data-workspace-main="true" className="min-w-0 space-y-3">
      <header className="flex flex-wrap items-start justify-between gap-2">
        <div className="min-w-0 flex-1">
          <Link className="text-sm font-medium text-primary" onClick={(event) => { if (!confirmNavigation()) event.preventDefault(); }}
            href={`/teaching/classes/${evaluation.classId}/assignments/${evaluation.assignmentId}/submissions`}>← Bài nộp</Link>
          <h1 className="mt-1 text-xl font-bold text-ink">{context.value?.title || "Chấm bài"}</h1>
          <div className="mt-1.5 flex flex-wrap items-center gap-2 text-sm text-muted">
            <Pill>{statusLabels[evaluation.state]}</Pill>
            {context.value && <span>{context.value.ownerName}</span>}
            <span>Bài {Math.max(1, currentIndex + 1)} / {detail.navigationIds.length || 1}</span>
            <details className="text-xs text-muted">
              <summary className="cursor-pointer font-medium text-primary">Quy trình chấm</summary>
              <p className="mt-1">1. Nhập điểm từng mục và nhận xét → 2. Lưu nháp → 3. Chốt → 4. Công bố để sinh viên xem điểm.{detail.navigationIds.length > 1 && " Lưu thành công sẽ chuyển sang bài tiếp theo nếu còn bài."}</p>
            </details>
          </div>
        </div>
        <Button size="sm" aria-pressed={focusMode} onClick={() => setFocusMode((value) => !value)}>{focusMode ? "Thoát tập trung" : "Chế độ tập trung"}</Button>
      </header>
      <div role="region" aria-label="Thao tác chấm bài" className="sticky top-16 z-20 flex flex-wrap items-center justify-between gap-2 rounded-xl border border-line bg-white px-3 py-2 shadow-sm">
        <div className="flex flex-wrap items-baseline gap-x-3 gap-y-0.5">
          <div className="flex items-baseline gap-1.5">
            <span className="text-xs font-medium text-muted">Điểm tạm tính</span>
            <output aria-label="Điểm tạm tính" className="text-lg font-bold tabular-nums text-primary">{total.toFixed(2)} / {Number(evaluation.maxScore).toFixed(2)}</output>
          </div>
          <p role="status" className="text-xs text-muted">{busy ? "Đang xử lý…" : dirty ? "Có thay đổi chưa lưu" : `Điểm đã lưu: ${points(evaluation.score, evaluation.maxScore)}`}</p>
        </div>
        <div className="flex flex-wrap items-center gap-1.5">
          <Button size="sm" disabled={currentIndex <= 0 || busy} onClick={() => navigate(detail.navigationIds[currentIndex - 1]!)}>← Bài trước</Button>
          <Button size="sm" disabled={currentIndex < 0 || currentIndex >= detail.navigationIds.length - 1 || busy} onClick={() => navigate(detail.navigationIds[currentIndex + 1]!)}>Bài sau →</Button>
          <Button size="sm" variant="primary" disabled={disabled} onClick={() => void save()}>Lưu nháp / thay đổi</Button>
          <Button size="sm" disabled={disabled || dirty || evaluation.state !== "DRAFT"} onClick={() => setAction("finalize")}>Chốt điểm</Button>
          <Button size="sm" disabled={disabled || dirty || evaluation.state !== "PUBLISHED" && evaluation.state !== "FINALIZED" ? true : evaluation.state !== "FINALIZED"} onClick={() => setAction("publish")}>Công bố điểm</Button>
        </div>
      </div>
      {!detail.current && <p>Đây là lần nộp cũ, chỉ có thể xem lại.</p>}
      {error && (
        <p role="alert" className="rounded border border-red-200 bg-red-50 p-4">
          {error} <button onClick={() => void reload()}>Tải lại phiên bản</button>
        </p>
      )}
      {dirty && (
        <p className="text-xs font-medium text-amber-700">
          Có thay đổi chưa lưu. Chốt/công bố chỉ áp dụng sau khi lưu.
        </p>
      )}
      <div data-grading-grid="true" className="grid min-w-0 items-start gap-4">
        <SubmissionViewer document={detail.document} evaluationId={evaluation.id} />
        <div className="min-w-0 space-y-3">
          <GradingForm rubrics={detail.rubrics} partTitles={partTitles} validBlockIds={validBlockIds} values={form} disabled={disabled} official={official} onChange={updateForm} />
          <AiProposalPanel evaluationId={evaluation.id} proposals={proposals} disabled={disabled || official}
            rubrics={detail.rubrics} currentValues={form} partTitles={partTitles} validBlockIds={validBlockIds}
            pollExhausted={pollExhausted} onRecheck={resumePolling}
            onRequest={(input) => void requestProposal(input)} onApply={applyProposal} />
        </div>
      </div>
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
