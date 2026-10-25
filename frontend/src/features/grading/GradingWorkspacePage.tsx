"use client";
import Link from "next/link";
import { useCallback, useState } from "react";
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
    disabled,
    official,
    currentIndex,
    updateForm,
    reload,
    navigate,
    confirmNavigation,
    save,
    requestProposal,
    applyProposal,
  } = workspace;
  const [action, setAction] = useState<"finalize" | "publish" | null>(null);
  const [focusMode, setFocusMode] = useState(false);
  const context = useSubmissionContext(evaluation.classId, evaluation.assignmentId);
  const partTitles = Object.fromEntries(detail.document.blocks.filter((block) => block.type === "HEADING" && typeof block.content.text === "string")
    .map((block) => [block.id, block.content.text as string]));
  const total = Math.min(Number(evaluation.maxScore), Math.max(0, checklistPoints(detail.rubrics, form.checks) + (Number(form.adjustment) || 0)));
  return (
    <main data-workspace-focus={focusMode} className="min-w-0 space-y-4">
      <header className="flex flex-wrap items-start justify-between gap-3">
        <div className="min-w-0 flex-1">
          <Link className="text-sm font-medium text-primary" onClick={(event) => { if (!confirmNavigation()) event.preventDefault(); }}
            href={`/teaching/classes/${evaluation.classId}/assignments/${evaluation.assignmentId}/submissions`}>← Bài nộp</Link>
          <h1 className="mt-2 text-xl font-bold text-ink">{context.value?.title || "Chấm bài"}</h1>
          <div className="mt-2 flex flex-wrap items-center gap-2 text-sm text-muted">
            <Pill>{statusLabels[evaluation.state]}</Pill>
            {context.value && <span>{context.value.ownerName}</span>}
            <span>Bài {Math.max(1, currentIndex + 1)} / {detail.navigationIds.length || 1}</span>
          </div>
        </div>
        <Button size="sm" aria-pressed={focusMode} onClick={() => setFocusMode((value) => !value)}>{focusMode ? "Thoát tập trung" : "Chế độ tập trung"}</Button>
      </header>
      <div role="region" aria-label="Thao tác chấm bài" className="sticky top-16 z-20 flex flex-wrap items-center justify-between gap-3 rounded-xl border border-line bg-white px-4 py-3 shadow-sm">
        <div>
          <div className="flex flex-wrap items-baseline gap-2"><span className="text-sm text-muted">Điểm tạm tính</span>
            <output aria-label="Điểm tạm tính" className="text-xl font-bold text-primary">{total.toFixed(2)} / {Number(evaluation.maxScore).toFixed(2)}</output>
          </div>
          <p role="status" className="mt-1 text-xs text-muted">{busy ? "Đang xử lý…" : dirty ? "Có thay đổi chưa lưu" : `Điểm đã lưu: ${points(evaluation.score, evaluation.maxScore)}`}</p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <Button size="sm" disabled={currentIndex <= 0 || busy} onClick={() => navigate(detail.navigationIds[currentIndex - 1]!)}>← Bài trước</Button>
          <Button size="sm" disabled={currentIndex < 0 || currentIndex >= detail.navigationIds.length - 1 || busy} onClick={() => navigate(detail.navigationIds[currentIndex + 1]!)}>Bài sau →</Button>
          <Button size="sm" variant="primary" disabled={disabled} onClick={() => void save()}>Lưu nháp / thay đổi</Button>
          <Button size="sm" disabled={disabled || dirty || evaluation.state !== "DRAFT"} onClick={() => setAction("finalize")}>Chốt điểm</Button>
          <Button size="sm" disabled={disabled || dirty || evaluation.state !== "FINALIZED"} onClick={() => setAction("publish")}>Công bố điểm</Button>
        </div>
        <p className="basis-full text-xs text-muted">1. Tick rubric và nhận xét → 2. Lưu nháp → 3. Chốt → 4. Công bố để sinh viên xem điểm.{detail.navigationIds.length > 1 && " Lưu thành công sẽ chuyển sang bài tiếp theo nếu còn bài."}</p>
      </div>
      {!detail.current && <p>Đây là lần nộp cũ, chỉ có thể xem lại.</p>}
      {error && (
        <p role="alert" className="rounded border border-red-200 bg-red-50 p-4">
          {error} <button onClick={() => void reload()}>Tải lại phiên bản</button>
        </p>
      )}
      {dirty && (
        <p className="text-amber-700">
          Có thay đổi chưa lưu. Chốt/công bố chỉ áp dụng sau khi lưu.
        </p>
      )}
      <div className="grid min-w-0 items-start gap-4 xl:grid-cols-[minmax(0,1.15fr)_minmax(360px,1fr)]">
        <SubmissionViewer document={detail.document} evaluationId={evaluation.id} />
        <div className="min-w-0 space-y-4">
          <GradingForm rubrics={detail.rubrics} partTitles={partTitles} values={form} disabled={disabled} official={official} onChange={updateForm} />
          <AiProposalPanel evaluationId={evaluation.id} proposals={proposals} disabled={disabled || official}
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
