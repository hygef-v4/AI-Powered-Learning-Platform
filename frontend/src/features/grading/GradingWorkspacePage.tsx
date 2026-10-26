"use client";
import Link from "next/link";
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
import { Button, Dialog, Pill } from "@/shared/ui";
import { checklistPoints, RubricChecklist } from "./RubricChecklistPanel";
import { useSubmissionContext } from "./useSubmissionContext";
import { gradingApi } from "./api";
import { useResource } from "./useResource";
import { checklistBindings, GradingNotes } from "./GradingForm";
import { useGradingWorkspace } from "./useGradingWorkspace";
import { ActivityReview, GradeHistory, GroupSectionAuthors } from "./GradingReviewPanels";
import { AiProposalPanel } from "./AiProposalPanel";
import { BatchActionDialog } from "./BatchActionDialog";
import { MemberContributionPanel } from "./MemberContributionPanel";
import { points, statusLabels, type Detail, type DocumentModel, type Rubric } from "./types";

export function GradingWorkspacePage({ evaluationId }: { evaluationId: string }) {
  const load = useCallback(() => gradingApi.detail(evaluationId), [evaluationId]);
  const resource = useResource(load);
  return resource.value ? (
    <Workspace key={evaluationId} initial={resource.value} refresh={resource.refresh} />
  ) : (
    <p role={resource.error ? "alert" : "status"}>{resource.error || "Đang tải bài làm…"}</p>
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
  const [aiOpen, setAiOpen] = useState(false);
  const [activePart, setActivePart] = useState<string | null>(null);
  const context = useSubmissionContext(evaluation.classId, evaluation.assignmentId);
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
  const total = Math.min(Number(evaluation.maxScore), Math.max(0, checklistPoints(detail.rubrics, form.checks, form.itemScores)));
  const checklist = { ...checklistBindings(form, disabled, updateForm), validBlockIds };
  const blockIds = new Set(paper.blocks.map((block) => block.id));
  const pending = proposals.some((proposal) => proposal.status === "PENDING");
  const ready = proposals[0]?.status === "READY";

  const comments: DocumentComment[] = [
    {
      key: "grading-notes",
      anchorBlockId: null,
      content: <GradingNotes rubrics={detail.rubrics} values={form} disabled={disabled} official={official} onChange={updateForm} />,
    },
    ...detail.rubrics.map((rubric, index): DocumentComment => ({
      key: rubric.rubricId,
      // Rubric gắn với tiêu đề của phần; rubric cả bài (không có tiêu đề) nằm ở đầu tài liệu.
      anchorBlockId: blockIds.has(rubric.partId) ? rubric.partId : null,
      content: (
        <RubricNote title={partTitles[rubric.partId] || `Phần ${index + 1}`} criteria={noteCriteria(rubric)}
          active={activePart === rubric.rubricId} onActivate={() => setActivePart(rubric.rubricId)}
          badge={<Pill tone="primary">Đạt {checklistPoints([rubric], form.checks, form.itemScores).toFixed(2)}</Pill>}
          editor={<RubricChecklist rubric={rubric} {...checklist} />} />
      ),
    })),
  ];
  const activeHeading = detail.rubrics.find((rubric) => rubric.rubricId === activePart)?.partId ?? null;

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
              <p className="mt-1">1. Tích mục đạt trong checklist cạnh từng phần và nhận xét → 2. Lưu nháp → 3. Chốt → 4. Công bố để sinh viên xem điểm.{detail.navigationIds.length > 1 && " Lưu thành công sẽ chuyển sang bài tiếp theo nếu còn bài."}</p>
            </details>
          </div>
        </div>
        <Button size="sm" aria-pressed={focusMode} onClick={() => setFocusMode((value) => !value)}>{focusMode ? "Thoát tập trung" : "Chế độ tập trung"}</Button>
      </header>
      <div role="region" aria-label="Thao tác chấm bài" className="flex flex-wrap items-center justify-between gap-2 rounded-xl border border-line bg-white px-3 py-2 shadow-sm">
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
          <Button size="sm" disabled={disabled || dirty || evaluation.state !== "FINALIZED"} onClick={() => setAction("publish")}>Công bố điểm</Button>
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
      {!detail.rubrics.length && <p className="rounded-lg bg-shell p-3 text-sm text-muted">Bài chưa có rubric khả dụng. Hãy kiểm tra lại cấu hình bài tập.</p>}
      <DocumentEditor document={paper} onChange={() => {}} mode="student" readOnly diagramEnabled={false}
        imageUrl={(blockId) => imageUrls[blockId] ?? null}
        emptyLeafLabel="Sinh viên chưa làm phần này."
        header={<div className="mb-5 border-b border-line pb-4">
          <h2 className="text-[26px] leading-tight font-bold text-ink">{context.value?.title || "Bài làm đã nộp"}</h2>
          <p className="mt-1 text-sm text-muted">Bài làm đã nộp · chỉ đọc</p>
        </div>}
        comments={comments}
        activeComment={activePart}
        highlightBlockId={activeHeading}
        toolbarExtra={<>
          {pending && <Pill tone="warning">AI đang chấm…</Pill>}
          {!pending && ready && <Pill tone="success">Có đề xuất AI</Pill>}
          <ToolbarButton icon="smart_toy" title="Chấm bằng AI" aria-label="Chấm bằng AI" onClick={() => setAiOpen(true)} />
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
        <Dialog open size="xl" icon="smart_toy" title="Chấm bằng AI" onClose={() => setAiOpen(false)}>
          <AiProposalPanel evaluationId={evaluation.id} proposals={proposals} disabled={disabled || official}
            rubrics={detail.rubrics} currentValues={form} partTitles={partTitles} validBlockIds={validBlockIds}
            pollExhausted={pollExhausted} onRecheck={resumePolling}
            onRequest={(input) => void requestProposal(input)}
            onApply={(...args) => {
              applyProposal(...args);
              setAiOpen(false);
            }} />
        </Dialog>
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
