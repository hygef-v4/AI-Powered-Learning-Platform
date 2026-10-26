"use client";
import { useState } from "react";
import { Pill, type Tone } from "@/shared/ui";
import { AiSourcePicker, type SourceChoice } from "@/shared/ai";
import { checklistPoints, invalidItemScores, itemPoints, validItemScore } from "./RubricChecklistPanel";
import { GradingFeedbackSummary, RubricItemFeedback } from "./StructuredFeedbackSection";
import type {
  AiRequest,
  Checks,
  GradingFormValues,
  ItemScores,
  Proposal,
  Rubric,
  StructuredFeedback,
} from "./types";

/** Preserve the U09 request DTO; shared provider choice remains implicit as before. */
export function aiRequest(choice: SourceChoice): AiRequest {
  return {
    source: choice.source,
    ...(choice.source === "PERSONAL" && choice.provider ? { provider: choice.provider } : {}),
    requestRef: crypto.randomUUID(),
  };
}

const STATUS_META: Record<string, { label: string; tone: Tone; icon: string }> = {
  PENDING: { label: "Đang chờ xử lý", tone: "warning", icon: "⏳" },
  READY: { label: "Sẵn sàng để xem", tone: "success", icon: "✓" },
  FAILED: { label: "Không tạo được đề xuất", tone: "danger", icon: "✕" },
  STALE: { label: "Đề xuất đã cũ", tone: "neutral", icon: "•" },
  REJECTED: { label: "Đề xuất không hợp lệ", tone: "danger", icon: "✕" },
};

function pendingStageLabel(taskState?: string | null): string {
  if (taskState === "READY") return "Đã nhận kết quả AI, đang xử lý phản hồi";
  if (taskState === "RUNNING") return "AI đang chấm bài";
  return "Đang chờ xử lý";
}

function pendingStageDetail(taskState?: string | null): string {
  if (taskState === "READY") {
    return "Đã nhận kết quả từ nhà cung cấp AI và đang đối chiếu với rubric. Bạn vẫn có thể chấm thủ công trong lúc chờ.";
  }
  if (taskState === "RUNNING") {
    return "AI đang đọc bài làm và rubric để tạo đề xuất. Bạn vẫn có thể chấm thủ công trong lúc chờ.";
  }
  return "Yêu cầu chấm AI đang xếp hàng xử lý. Bạn vẫn có thể chấm thủ công trong lúc chờ.";
}

export function AiProposalPanel({
  proposals,
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
  const [choosingSource, setChoosingSource] = useState(false);
  const [expandedOlder, setExpandedOlder] = useState<Record<string, boolean>>({});

  function renderProposalContent(p: Proposal, isLatest: boolean) {
    const aiChecks = p.proposal?.checks ?? {};
    const aiScores = p.proposal?.itemScoresByRubric ?? {};
    const aiStructured = p.proposal?.structuredFeedback ?? null;
    const hasComparison = Boolean(isLatest && p.proposal && rubrics.length > 0 && currentValues);
    const hasInvalidCurrent = Boolean(currentValues && invalidItemScores(rubrics, currentValues.itemScores));
    const currentSubtotal = currentValues ? checklistPoints(rubrics, currentValues.checks, currentValues.itemScores) : 0;
    const aiTotal = p.proposal ? Number(p.proposal.score) : 0;
    const totalDiff = !hasInvalidCurrent && Number.isFinite(aiTotal) ? Math.round((aiTotal - currentSubtotal) * 100) / 100 : 0;
    const reviewFlagCount = aiStructured?.items?.filter((item) => item.requiresReview).length ?? 0;

    return (
      <>
        {p.provider && <p className="mt-1 text-xs text-muted">{p.provider}{p.model && ` · ${p.model}`}</p>}
        {p.status === "PENDING" && !pollExhausted && (
          <p role="status" className="mt-2 text-muted">
            {pendingStageDetail(p.taskState)}
          </p>
        )}
        {p.status === "PENDING" && pollExhausted && (
          <div role="status" className="mt-2 rounded-lg border border-warning bg-warning-soft/60 p-3 text-xs text-ink">
            <p className="font-semibold">Phản hồi AI đang mất nhiều thời gian hơn dự kiến.</p>
            <p className="mt-1 text-muted">
              Yêu cầu hiện tại vẫn đang được xử lý. Bạn có thể kiểm tra lại trạng thái của yêu cầu này mà không gửi yêu cầu AI mới, hoặc tiếp tục chấm thủ công.
            </p>
            {onRecheck && (
              <button
                type="button"
                onClick={onRecheck}
                className="mt-2 rounded-lg border border-primary bg-white px-3 py-1.5 text-xs font-semibold text-primary hover:bg-primary-soft"
              >
                Kiểm tra lại
              </button>
            )}
          </div>
        )}
        {(p.status === "FAILED" || p.status === "REJECTED") && <div className="mt-2 rounded-lg bg-danger/5 p-3">
          <p>{p.safeError && !/^AI task (failed|unavailable or failed)$/.test(p.safeError) ? p.safeError : "Tác vụ AI không thành công. Kiểm tra kết nối hoặc thử lại bằng nguồn AI đã cấu hình."}</p>
          <p className="mt-1 text-muted">Bạn vẫn có thể chấm thủ công theo rubric. Điểm chưa bị thay đổi.</p>
          <a href="/ai/connection" className="mt-2 inline-block font-semibold text-primary underline underline-offset-2">Kiểm tra kết nối AI</a>
        </div>}
        {p.status === "STALE" && <p className="mt-2 text-muted">Bài nộp hoặc rubric đã thay đổi. Hãy yêu cầu một đề xuất mới.</p>}
        {p.proposal && <>
          <div className="mt-2 flex flex-wrap items-baseline justify-between gap-2 rounded-lg border border-line bg-white px-3 py-2">
            <div>
              <span className="text-xs font-medium text-muted">Điểm AI đề xuất: </span>
              <strong className="text-base font-bold tabular-nums text-success">{aiTotal.toFixed(2)}</strong>
            </div>
            {hasComparison && (
              <div className="flex flex-wrap items-center gap-2 text-xs">
                {hasInvalidCurrent ? (
                  <span className="font-semibold text-danger">Điểm đang nhập chưa hợp lệ (kiểm tra mục báo lỗi)</span>
                ) : (
                  <>
                    <span className="font-medium tabular-nums text-body">Điểm đang chấm: {currentSubtotal.toFixed(2)}</span>
                    <span className="font-medium tabular-nums text-body">AI đề xuất: {aiTotal.toFixed(2)}</span>
                    {totalDiff !== 0 ? (
                      <Pill tone="warning">Lệch {totalDiff > 0 ? `+${totalDiff.toFixed(2)}` : totalDiff.toFixed(2)}</Pill>
                    ) : (
                      <Pill tone="success">Khớp điểm rubric</Pill>
                    )}
                  </>
                )}
              </div>
            )}
          </div>
          {reviewFlagCount > 0 && (
            <div role="note" className="mt-2 rounded-lg border border-warning bg-warning-soft/60 px-3 py-2 text-xs text-ink">
              <strong className="text-warning">⚠ Có {reviewFlagCount} mục AI đánh dấu cần giảng viên kiểm tra thủ công</strong>{" "}
              (ví dụ: minh chứng chưa đọc rõ hoặc thiết kế thay thế hợp lệ).
            </div>
          )}
          {aiStructured && (
            <GradingFeedbackSummary
              summary={aiStructured.summary}
              title="Tóm tắt nhận xét cấu trúc từ AI"
            />
          )}
          {hasComparison && currentValues && (
            <div className="mt-2 space-y-1.5 rounded-lg border border-line bg-white p-2.5 text-xs">
              <p className="font-semibold text-ink">So sánh theo từng phần & mục rubric</p>
              {rubrics.map((rubric, rIdx) => {
                const secTitle = partTitles[rubric.partId] || `Phần ${rIdx + 1}`;
                const hasAiRubric = Object.prototype.hasOwnProperty.call(aiScores, rubric.rubricId) || Object.prototype.hasOwnProperty.call(aiChecks, rubric.rubricId);
                const secAi = hasAiRubric ? checklistPoints([rubric], aiChecks, aiScores).toFixed(2) : "Chưa có dữ liệu";
                const secCurInvalid = invalidItemScores([rubric], currentValues.itemScores);
                const secCur = secCurInvalid ? "Chưa hợp lệ" : checklistPoints([rubric], currentValues.checks, currentValues.itemScores).toFixed(2);
                return (
                  <div key={rubric.rubricId} className="rounded border border-line-soft bg-shell/40 p-2">
                    <div className="flex flex-wrap items-center justify-between gap-1 font-semibold text-ink">
                      <span>{secTitle}</span>
                      <span className="tabular-nums text-body">
                        Đang chấm: {secCur} · AI: {secAi} / {Number(rubric.maxPoints).toFixed(2)}
                      </span>
                    </div>
                    <div className="mt-1.5 space-y-1">
                      {rubric.criteria.flatMap((c) => c.items).map((item) => {
                        const max = Number(item.points);
                        const curRaw = itemPoints(rubric.rubricId, item.itemId, max, currentValues.checks, currentValues.itemScores);
                        const curValid = validItemScore(curRaw, max);
                        const hasAiItem = Boolean(
                          (aiScores[rubric.rubricId] && Object.prototype.hasOwnProperty.call(aiScores[rubric.rubricId], item.itemId)) ||
                          Object.prototype.hasOwnProperty.call(aiChecks, rubric.rubricId),
                        );
                        const aiVal = hasAiItem ? Number(itemPoints(rubric.rubricId, item.itemId, max, aiChecks, aiScores)) : null;
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
                                Đang chấm: {curValid ? Number(curRaw).toFixed(2) : "Chưa hợp lệ"} · AI:{" "}
                                {aiVal !== null && Number.isFinite(aiVal) ? aiVal.toFixed(2) : "Chưa có dữ liệu"} / {max.toFixed(2)}
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
            <details open={!aiStructured && p.proposal.feedback.length <= 280} className="mt-2 rounded-lg border border-line-soft bg-white p-2.5">
              <summary className="cursor-pointer text-xs font-semibold text-body">Nhận xét tổng hợp từ AI</summary>
              <p className="mt-1.5 whitespace-pre-wrap leading-relaxed text-ink">{p.proposal.feedback}</p>
            </details>
          )}
          {!!p.proposal.sections?.length && <details className="mt-2 rounded-lg border border-line-soft bg-white p-2.5">
            <summary className="cursor-pointer text-xs font-semibold text-primary">Nhận xét từng phần ({p.proposal.sections.length})</summary>
            {p.proposal.sections.map((section, sectionIndex) => (
              <p key={section.partId} className="mt-2 text-xs leading-relaxed">
                <strong>{partTitles[section.partId] || `Phần ${sectionIndex + 1}`}:</strong> {section.feedback}
              </p>
            ))}
          </details>}
        </>}
        <div className="mt-3 flex flex-wrap items-center gap-2">
          <button
            type="button"
            className="rounded-lg border border-primary bg-primary-soft px-3 py-1.5 text-xs font-semibold text-primary hover:bg-primary hover:text-white disabled:cursor-not-allowed disabled:opacity-40"
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
            Áp dụng vào bản nháp
          </button>
          <span className="text-xs text-muted">Chỉ tích sẵn checklist và điền nhận xét; bạn có thể sửa lại trước khi lưu.</span>
        </div>
      </>
    );
  }

  return (
    <section aria-label="Đề xuất AI" className="rounded-xl border border-line bg-white p-3.5 sm:p-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div>
          <h2 className="text-base font-bold text-ink">Đề xuất AI</h2>
          <p className="mt-0.5 text-xs leading-relaxed text-muted">AI chỉ đề xuất. Bạn kiểm tra rubric và lưu nháp trước khi chốt.</p>
        </div>
        <button
          type="button"
          className="rounded-lg bg-primary px-3 py-1.5 text-xs font-semibold text-white disabled:opacity-40"
          disabled={disabled || proposals.some((proposal) => proposal.status === "PENDING")}
          onClick={() => setChoosingSource(true)}
        >
          Yêu cầu đề xuất mới
        </button>
      </div>
      {choosingSource && (
        <AiSourcePicker
          open
          task="Đề xuất chấm bài đang xem"
          busy={disabled}
          onCancel={() => setChoosingSource(false)}
          onSubmit={(choice) => {
            if (disabled) return;
            setChoosingSource(false);
            onRequest(aiRequest(choice));
          }}
        />
      )}
      {proposals.length === 0 && <p className="mt-3 text-sm text-muted">Chưa có đề xuất. Bạn có thể tự chấm theo rubric hoặc yêu cầu AI hỗ trợ.</p>}
      {proposals.map((p, index) => {
        const baseMeta = STATUS_META[p.status] ?? { label: "Chưa khả dụng", tone: "neutral" as Tone, icon: "•" };
        const meta = p.status === "PENDING" ? { ...baseMeta, label: pendingStageLabel(p.taskState) } : baseMeta;
        const sourceLabel = `Lần ${p.versionNo} · ${p.origin === "MCP" ? (p.agentLabel ?? "Agent") : p.source === "PERSONAL" ? "API cá nhân" : "API trường"}`;
        const keepExpanded = index === 0 || p.status === "PENDING" || p.status === "FAILED" || p.status === "REJECTED";
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
                  <span className="text-xs text-muted">Đề xuất trước đó</span>
                </span>
                <span className="text-xs text-muted">
                  {sourceLabel}
                  {p.proposal ? ` · ${Number(p.proposal.score).toFixed(2)} điểm` : ""}
                </span>
              </button>
              <div hidden={!isOpen} className="mt-2 border-t border-line pt-2">
                {renderProposalContent(p, false)}
              </div>
            </article>
          );
        }
        return (
          <article key={p.id} className="mt-3 rounded-lg border border-line bg-shell/40 p-3 text-sm">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <Pill tone={meta.tone}>
                <span aria-hidden="true">{meta.icon}</span>
                <strong>{meta.label}</strong>
              </Pill>
              <span className="text-xs text-muted">{sourceLabel}</span>
            </div>
            {index > 0 && <p className="mt-1 text-xs text-muted">Đề xuất trước đó</p>}
            {renderProposalContent(p, index === 0)}
          </article>
        );
      })}
    </section>
  );
}
