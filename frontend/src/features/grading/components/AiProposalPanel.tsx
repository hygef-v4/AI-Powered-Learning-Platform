"use client";
import { useState } from "react";
import { Card, Pill, type Tone } from "@/shared/ui";
import { describeAiFailure } from "@/shared/api/errorMessages";
import { formatDateTime, type SourceChoice } from "@/shared/ai";
import { calculateChecklistPoints, hasInvalidItemScores, resolveItemPoints, isValidItemScore } from "../model/rubricScores";
import { GradingFeedbackSummary, RubricItemFeedback } from "./StructuredFeedbackSection";
import type {
  AiRequest,
  Checks,
  GradingFormValues,
  ItemScores,
  Proposal,
  Rubric,
  StructuredFeedback,
} from "../model/types";
import { msg } from "@/i18n/msg";

/** Yêu cầu AI: không chọn nguồn thì hệ thống tự chọn (API chung nếu đủ hạn mức, không thì khóa cá nhân; 2026-11-05). */
export function buildAiRequest(choice?: SourceChoice): AiRequest {
  const reqRef = typeof crypto !== "undefined" && "randomUUID" in crypto
    ? crypto.randomUUID()
    : `${Date.now()}-${Math.random().toString(36).substring(2, 9)}`;
  return {
    ...(choice ? { source: choice.source } : {}),
    ...(choice?.source === "PERSONAL" && choice.provider ? { provider: choice.provider } : {}),
    requestRef: reqRef,
  };
}

const STATUS_META: Record<string, { label: string; tone: Tone; icon: string }> = {
  PENDING: { label: msg("grading.components.AiProposalPanel.waitingToBeProcessed"), tone: "warning", icon: "⏳" },
  READY: { label: msg("grading.components.AiProposalPanel.readyToReview"), tone: "success", icon: "✓" },
  FAILED: { label: msg("grading.components.AiProposalPanel.couldNotCreateAProposal"), tone: "danger", icon: "✕" },
  STALE: { label: msg("grading.components.AiProposalPanel.theProposalIsOutdated"), tone: "neutral", icon: "•" },
  REJECTED: { label: msg("grading.components.AiProposalPanel.theProposalIsInvalid"), tone: "danger", icon: "✕" },
};

// Đổi trạng thái tác vụ thành nhãn của bước xử lý đề xuất.
function describePendingStage(taskState?: string | null): string {
  if (taskState === "READY") return msg("grading.components.AiProposalPanel.aiResultReceivedProcessingThe");
  if (taskState === "RUNNING") return msg("grading.components.AiProposalPanel.theAiIsGrading");
  return msg("grading.components.AiProposalPanel.waitingToBeProcessed");
}

// Giải thích giai đoạn agent đang chạy hoặc kết quả đang được hệ thống xử lý.
function describePendingStageDetail(taskState?: string | null): string {
  if (taskState === "READY") {
    return msg("grading.components.AiProposalPanel.theResultFromTheAi");
  }
  if (taskState === "RUNNING") {
    return msg("grading.components.AiProposalPanel.theAiIsReadingThe");
  }
  return msg("grading.components.AiProposalPanel.theAiGradingRequestIs");
}

// Hiển thị đề xuất đang chờ hoặc có nội dung, kể cả mục được mở từ link MCP.
export function AiProposalPanel({
  proposals,
  selectedProposalId,
  proposalsLoaded = true,
  onApply,
  onRequest,
  disabled = false,
  rubrics = [],
  currentValues,
  partTitles = {},
  validBlockIds,
  pollExhausted = false,
  onRecheck,
}: {
  evaluationId: string;
  proposals: Proposal[];
  selectedProposalId?: string;
  proposalsLoaded?: boolean;
  onApply: (
    checks: Checks,
    feedback: string,
    id: string,
    itemScores?: ItemScores,
    structuredFeedback?: StructuredFeedback | null,
  ) => void;
  onRequest: (input: AiRequest) => void;
  disabled?: boolean;
  rubrics?: Rubric[];
  currentValues?: GradingFormValues;
  partTitles?: Record<string, string>;
  validBlockIds?: Set<string>;
  pollExhausted?: boolean;
  onRecheck?: () => void;
}) {
  const [expandedOlder, setExpandedOlder] = useState<Record<string, boolean>>({});
  const visibleProposals = proposals.filter((p) => p.status === "PENDING" || (p.proposal && p.status !== "FAILED" && p.status !== "REJECTED"));
  const latestFailure = proposals[0]?.status === "FAILED" || proposals[0]?.status === "REJECTED" ? proposals[0] : null;

  // Dựng bản so sánh điểm và phản hồi để giảng viên cân nhắc trước khi áp dụng.
  function renderProposalContent(p: Proposal, isLatest: boolean) {
    const aiChecks = p.proposal?.checks ?? {};
    const aiScores = p.proposal?.itemScoresByRubric ?? {};
    const aiStructured = p.proposal?.structuredFeedback ?? null;
    const hasComparison = Boolean(isLatest && p.proposal && rubrics.length > 0 && currentValues);
    const hasInvalidCurrent = Boolean(currentValues && hasInvalidItemScores(rubrics, currentValues.itemScores));
    const currentSubtotal = currentValues ? calculateChecklistPoints(rubrics, currentValues.checks, currentValues.itemScores) : 0;
    const aiTotal = p.proposal ? Number(p.proposal.score) : 0;
    const totalDiff = !hasInvalidCurrent && Number.isFinite(aiTotal) ? Math.round((aiTotal - currentSubtotal) * 100) / 100 : 0;
    const reviewFlagCount = aiStructured?.items?.filter((item) => item.requiresReview).length ?? 0;

    return (
      <>
        {p.provider && <p className="mt-1 text-xs text-muted">{p.provider}{p.model && ` · ${p.model}`}</p>}
        {p.status === "PENDING" && !pollExhausted && (
          <p role="status" className="mt-2 text-muted">
            {describePendingStageDetail(p.taskState)}
          </p>
        )}
        {p.status === "PENDING" && pollExhausted && (
          <div role="status" className="mt-2 rounded-lg border border-warning bg-warning-soft/60 p-3 text-xs text-ink">
            <p className="font-semibold">{msg("grading.components.AiProposalPanel.theAiResponseIsTaking")}</p>
            <p className="mt-1 text-muted">
              {msg("grading.components.AiProposalPanel.theCurrentRequestIsStill")}</p>
            {onRecheck && (
              <button
                type="button"
                onClick={onRecheck}
                className="mt-2 rounded-lg border border-primary bg-surface px-3 py-1.5 text-xs font-semibold text-primary-ink hover:bg-primary-soft"
              >
                {msg("grading.components.AiProposalPanel.checkAgain")}</button>
            )}
          </div>
        )}
        {p.status === "STALE" && <p className="mt-2 text-muted">{msg("grading.components.AiProposalPanel.theSubmissionOrRubricHas")}</p>}
        {p.proposal && <>
          <div className="mt-2 flex flex-wrap items-baseline justify-between gap-2 rounded-lg border border-line bg-surface px-3 py-2">
            <div>
              <span className="text-xs font-medium text-muted">{msg("grading.components.AiProposalPanel.aiProposedScore")}</span>
              <strong className="text-base font-bold tabular-nums text-success">{aiTotal.toFixed(2)}</strong>
            </div>
            {hasComparison && (
              <div className="flex flex-wrap items-center gap-2 text-xs">
                {hasInvalidCurrent ? (
                  <span className="font-semibold text-danger">{msg("grading.components.AiProposalPanel.theEnteredScoreIsInvalid")}</span>
                ) : (
                  <>
                    <span className="font-medium tabular-nums text-body">{msg("grading.components.AiProposalPanel.currentScore")}{currentSubtotal.toFixed(2)}</span>
                    <span className="font-medium tabular-nums text-body">{msg("grading.components.AiProposalPanel.aiProposal")}{aiTotal.toFixed(2)}</span>
                    {totalDiff !== 0 ? (
                      <Pill tone="warning">{msg("grading.components.AiProposalPanel.difference")}{totalDiff > 0 ? `+${totalDiff.toFixed(2)}` : totalDiff.toFixed(2)}</Pill>
                    ) : (
                      <Pill tone="success">{msg("grading.components.AiProposalPanel.matchesRubricPoints")}</Pill>
                    )}
                  </>
                )}
              </div>
            )}
          </div>
          {reviewFlagCount > 0 && (
            <div role="note" className="mt-2 rounded-lg border border-warning bg-warning-soft/60 px-3 py-2 text-xs text-ink">
              <strong className="text-warning">{msg("grading.components.AiProposalPanel.text")}{reviewFlagCount} {msg("grading.components.AiProposalPanel.itemsFlaggedByTheAi")}</strong>{" "}
              {msg("grading.components.AiProposalPanel.forExampleEvidenceThatCould")}</div>
          )}
          {aiStructured && (
            <GradingFeedbackSummary
              summary={aiStructured.summary}
              title={msg("grading.components.AiProposalPanel.summaryOfStructuredAiFeedback")}
            />
          )}
          {hasComparison && currentValues && (
            <div className="mt-2 space-y-1.5 rounded-lg border border-line bg-surface p-2.5 text-xs">
              <p className="font-semibold text-ink">{msg("grading.components.AiProposalPanel.comparisonByPartRubricItem")}</p>
              {rubrics.map((rubric, rIdx) => {
                const secTitle = partTitles[rubric.partId] || msg("grading.components.AiProposalPanel.part", { value: String(rIdx + 1) });
                const hasAiRubric = Object.prototype.hasOwnProperty.call(aiScores, rubric.rubricId) || Object.prototype.hasOwnProperty.call(aiChecks, rubric.rubricId);
                const secAi = hasAiRubric ? calculateChecklistPoints([rubric], aiChecks, aiScores).toFixed(2) : msg("grading.components.AiProposalPanel.noDataYet");
                const secCurInvalid = hasInvalidItemScores([rubric], currentValues.itemScores);
                const secCur = secCurInvalid ? msg("grading.components.AiProposalPanel.invalid") : calculateChecklistPoints([rubric], currentValues.checks, currentValues.itemScores).toFixed(2);
                return (
                  <div key={rubric.rubricId} className="rounded-lg border border-line bg-shell p-3">
                    <div className="flex flex-wrap items-center justify-between gap-1 font-semibold text-ink">
                      <span>{secTitle}</span>
                      <span className="tabular-nums text-body">
                        {msg("grading.components.AiProposalPanel.current")}{secCur} · AI: {secAi} / {Number(rubric.maxPoints).toFixed(2)}
                      </span>
                    </div>
                    <div className="mt-1.5 space-y-1">
                      {rubric.criteria.flatMap((c) => c.items).map((item) => {
                        const max = Number(item.points);
                        const curRaw = resolveItemPoints(rubric.rubricId, item.itemId, max, currentValues.checks, currentValues.itemScores);
                        const curValid = isValidItemScore(curRaw, max);
                        const hasAiItem = Boolean(
                          (aiScores[rubric.rubricId] && Object.prototype.hasOwnProperty.call(aiScores[rubric.rubricId], item.itemId)) ||
                          Object.prototype.hasOwnProperty.call(aiChecks, rubric.rubricId),
                        );
                        const aiVal = hasAiItem ? Number(resolveItemPoints(rubric.rubricId, item.itemId, max, aiChecks, aiScores)) : null;
                        const itemDiff = curValid && aiVal !== null ? Math.round((aiVal - Number(curRaw)) * 100) / 100 : 0;
                        const changed = !curValid || (aiVal !== null && itemDiff !== 0);
                        const fbItem = aiStructured?.items?.find(
                          (entry) => entry.rubricId === rubric.rubricId && entry.itemId === item.itemId,
                        );
                        return (
                          <div
                            key={item.itemId}
                            className={`space-y-0.5 rounded px-2 py-1.5 ${
                              changed ? "bg-warning-soft/60 text-ink" : "text-muted"
                            }`}
                          >
                            <p className="break-words leading-snug">{item.text}</p>
                            <div className="flex flex-wrap items-center justify-between gap-2 font-medium tabular-nums">
                              <span>
                                {msg("grading.components.AiProposalPanel.current")}{curValid ? Number(curRaw).toFixed(2) : msg("grading.components.AiProposalPanel.invalid")} · AI:{" "}
                                {aiVal !== null && Number.isFinite(aiVal) ? aiVal.toFixed(2) : msg("grading.components.AiProposalPanel.noDataYet")} / {max.toFixed(2)}
                              </span>
                              {curValid && aiVal !== null && itemDiff !== 0 && (
                                <span className="font-semibold text-warning">
                                  {itemDiff > 0 ? `+${itemDiff.toFixed(2)}` : itemDiff.toFixed(2)}
                                </span>
                              )}
                            </div>
                            {fbItem && (
                              <RubricItemFeedback
                                rubricId={rubric.rubricId}
                                itemId={item.itemId}
                                itemText={item.text}
                                awardedPoints={aiVal ?? 0}
                                maxPoints={max}
                                feedbackItem={fbItem}
                                validBlockIds={validBlockIds}
                                editable={false}
                              />
                            )}
                          </div>
                        );
                      })}
                    </div>
                  </div>
                );
              })}
            </div>
          )}
          {p.proposal.feedback && (
            <details open={!aiStructured && p.proposal.feedback.length <= 280} className="mt-2 rounded-lg border border-line-soft bg-surface p-2.5">
              <summary className="cursor-pointer text-xs font-semibold text-body">{msg("grading.components.AiProposalPanel.overallAiFeedback")}</summary>
              <p className="mt-1.5 whitespace-pre-wrap leading-relaxed text-ink">{p.proposal.feedback}</p>
            </details>
          )}
          {!!p.proposal.sections?.length && <details className="mt-2 rounded-lg border border-line-soft bg-surface p-2.5">
            <summary className="cursor-pointer text-xs font-semibold text-primary-ink">{msg("grading.components.AiProposalPanel.feedbackByPart")}{p.proposal.sections.length})</summary>
            {p.proposal.sections.map((section, sectionIndex) => (
              <p key={section.partId} className="mt-2 text-xs leading-relaxed">
                <strong>{partTitles[section.partId] || msg("grading.components.AiProposalPanel.part", { value: String(sectionIndex + 1) })}:</strong> {section.feedback}
              </p>
            ))}
          </details>}
        </>}
        <div className="mt-3 flex flex-wrap items-center gap-2">
          <button
            type="button"
            className="rounded-lg border border-primary bg-primary-soft px-3 py-1.5 text-xs font-semibold text-primary-ink hover:bg-primary hover:text-on-primary disabled:cursor-not-allowed disabled:opacity-40"
            disabled={disabled || p.status !== "READY" || !p.proposal}
            onClick={() => {
              if (!p.proposal) return;
              if (p.proposal.structuredFeedback) {
                onApply(
                  p.proposal.checks,
                  p.proposal.feedback ?? "",
                  p.id,
                  p.proposal.itemScoresByRubric,
                  p.proposal.structuredFeedback,
                );
              } else if (p.proposal.itemScoresByRubric) {
                onApply(p.proposal.checks, p.proposal.feedback ?? "", p.id, p.proposal.itemScoresByRubric);
              } else {
                onApply(p.proposal.checks, p.proposal.feedback ?? "", p.id);
              }
            }}
          >
            {msg("grading.components.AiProposalPanel.applyToDraft")}</button>
          <span className="text-xs text-muted">{msg("grading.components.AiProposalPanel.onlyPreTicksTheChecklist")}</span>
        </div>
      </>
    );
  }

  return (
    <Card as="section" padding="none" className="p-3.5 sm:p-4" aria-label={msg("grading.components.AiProposalPanel.aiProposal2")}>
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div>
          <h2 className="text-base font-bold text-ink">{msg("grading.components.AiProposalPanel.aiProposal2")}</h2>
          <p className="mt-0.5 text-xs leading-relaxed text-muted">{msg("grading.components.AiProposalPanel.reviewTheProposalAndSave")}</p>
        </div>
        <button
          type="button"
          className="rounded-lg bg-primary px-3 py-1.5 text-xs font-semibold text-on-primary disabled:opacity-40"
          disabled={disabled || proposals.some((proposal) => proposal.status === "PENDING")}
          onClick={() => { if (!disabled) onRequest(buildAiRequest()); }}
        >
          {msg("grading.components.AiProposalPanel.requestANewProposal")}</button>
      </div>
      {latestFailure && <div role="alert" className="mt-3 rounded-lg bg-danger/5 p-3 text-sm">
        <p className="font-semibold text-danger">{msg("grading.components.AiProposalPanel.couldNotCreateAProposal")}</p>
        <p className="mt-1">{latestFailure.safeError && !/^AI task (failed|unavailable or failed)$/.test(latestFailure.safeError)
          ? describeAiFailure(latestFailure.safeError) : msg("grading.components.AiProposalPanel.theAiTaskFailedCheck")}</p>
        <p className="mt-1 text-muted">{msg("grading.components.AiProposalPanel.youCanStillGradeManually")}</p>
        <a href="/ai/connection" className="mt-2 inline-block font-semibold text-primary-ink underline underline-offset-2">{msg("grading.components.AiProposalPanel.checkAiConnection")}</a>
      </div>}
      {visibleProposals.length === 0 && <p className="mt-3 text-sm text-muted">{msg("grading.components.AiProposalPanel.noProposalYetYouCan")}</p>}
      {selectedProposalId && proposalsLoaded && !proposals.some((p) => p.id === selectedProposalId) && (
        <p role="alert" className="mt-3 text-sm text-danger">{msg("grading.components.AiProposalPanel.theProposalFromTheLink")}</p>
      )}
      {visibleProposals.map((p, index) => {
        const baseMeta = STATUS_META[p.status] ?? { label: msg("grading.components.AiProposalPanel.notAvailableYet"), tone: "neutral" as Tone, icon: "•" };
        const meta = p.status === "PENDING" ? { ...baseMeta, label: describePendingStage(p.taskState) } : baseMeta;
        const sourceLabel = msg("grading.components.AiProposalPanel.run", { versionNo: String(p.versionNo), value: String(p.origin === "MCP" ? (p.agentLabel ?? "Agent") : p.source === "PERSONAL" ? msg("grading.components.AiProposalPanel.personalApi") : msg("grading.components.AiProposalPanel.schoolApi")) });
        const createdAt = p.createdAt && Number.isFinite(Date.parse(p.createdAt))
          ? <time dateTime={p.createdAt} className="block text-xs font-normal text-muted">{msg("grading.components.AiProposalPanel.createdAt")}{formatDateTime(p.createdAt)}</time>
          : null;
        const keepExpanded = p.id === selectedProposalId || index === 0 || p.status === "PENDING";
        if (!keepExpanded) {
          const isOpen = !!expandedOlder[p.id];
          return (
            <article key={p.id} className="mt-2.5 rounded-lg border border-line bg-shell/40 p-2.5 text-sm">
              <button
                type="button"
                aria-expanded={isOpen}
                onClick={() => setExpandedOlder((prev) => ({ ...prev, [p.id]: !prev[p.id] }))}
                className="flex w-full cursor-pointer flex-wrap items-center justify-between gap-2 text-left"
              >
                <span className="inline-flex items-center gap-2">
                  <span aria-hidden="true" className="text-xs text-muted">{isOpen ? "▾" : "▸"}</span>
                  <Pill tone={meta.tone}>
                    <span aria-hidden="true">{meta.icon}</span>
                    <span>{meta.label}</span>
                  </Pill>
                  <span className="text-xs text-muted">{msg("grading.components.AiProposalPanel.previousProposals")}</span>
                </span>
                <span className="text-xs text-muted">
                  {sourceLabel}
                  {p.proposal ? msg("grading.components.AiProposalPanel.points", { value: String(Number(p.proposal.score).toFixed(2)) }) : ""}
                  {createdAt}
                </span>
              </button>
              <div hidden={!isOpen} className="mt-2 border-t border-line pt-2">
                {renderProposalContent(p, false)}
              </div>
            </article>
          );
        }
        return (
          <article key={p.id} id={`grading-proposal-${p.id}`} className="mt-3 scroll-mt-52 rounded-lg border border-line bg-shell/40 p-3 text-sm">
            {p.id === selectedProposalId && <p className="mb-2 text-xs font-semibold text-primary-ink">{msg("grading.components.AiProposalPanel.proposalFromLink")}</p>}
            <div className="flex flex-wrap items-center justify-between gap-2">
              <Pill tone={meta.tone}>
                <span aria-hidden="true">{meta.icon}</span>
                <strong>{meta.label}</strong>
              </Pill>
              <span className="text-xs text-muted">{sourceLabel}{createdAt}</span>
            </div>
            {index > 0 && <p className="mt-1 text-xs text-muted">{msg("grading.components.AiProposalPanel.previousProposals")}</p>}
            {renderProposalContent(p, index === 0)}
          </article>
        );
      })}
    </Card>
  );
}
