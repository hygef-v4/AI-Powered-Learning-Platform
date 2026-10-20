"use client";
import Link from "next/link";
import { useCallback, useState } from "react";
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
  return (
    <main className="space-y-5">
      <header className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <Link
            className="text-primary"
            onClick={(event) => {
              if (!confirmNavigation()) event.preventDefault();
            }}
            href={`/teaching/classes/${evaluation.classId}/assignments/${evaluation.assignmentId}/submissions`}
          >
            ← Bài nộp
          </Link>
          <h1 className="mt-2 text-2xl font-bold">Chấm bài · {statusLabels[evaluation.state]}</h1>
          <p>Điểm đã lưu: {points(evaluation.score, evaluation.maxScore)}</p>
        </div>
        <div className="flex flex-wrap gap-3">
          <button
            disabled={currentIndex <= 0 || busy}
            onClick={() => navigate(detail.navigationIds[currentIndex - 1]!)}
            className="rounded border px-3 py-2"
          >
            ← Bài trước
          </button>
          <button
            disabled={currentIndex < 0 || currentIndex >= detail.navigationIds.length - 1 || busy}
            onClick={() => navigate(detail.navigationIds[currentIndex + 1]!)}
            className="rounded border px-3 py-2"
          >
            Bài sau →
          </button>
          <button
            disabled={disabled}
            onClick={() => void save()}
            className="rounded bg-primary px-4 py-2 text-white disabled:opacity-40"
          >
            {busy ? "Đang xử lý…" : "Lưu nháp / thay đổi"}
          </button>
          <button
            disabled={disabled || dirty || evaluation.state !== "DRAFT"}
            onClick={() => setAction("finalize")}
            className="rounded border px-3 py-2"
          >
            Chốt điểm
          </button>
          <button
            disabled={disabled || dirty || evaluation.state !== "FINALIZED"}
            onClick={() => setAction("publish")}
            className="rounded border px-3 py-2"
          >
            Công bố điểm
          </button>
        </div>
      </header>
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
      <div className="grid gap-6 xl:grid-cols-2">
        <SubmissionViewer document={detail.document} evaluationId={evaluation.id} />
        <GradingForm
          rubrics={detail.rubrics}
          values={form}
          disabled={disabled}
          official={official}
          onChange={updateForm}
        >
          <AiProposalPanel
            evaluationId={evaluation.id}
            proposals={proposals}
            disabled={disabled || official}
            onRequest={(input) => void requestProposal(input)}
            onApply={applyProposal}
          />
        </GradingForm>
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
