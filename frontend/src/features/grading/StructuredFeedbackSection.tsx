"use client";
import { useState } from "react";
import { Pill } from "@/shared/ui";
import { itemPoints, validItemScore } from "./RubricChecklistPanel";
import type {
  Checks,
  Decimal,
  ItemScores,
  Rubric,
  StructuredFeedback,
  StructuredFeedbackItem,
  StructuredFeedbackSummary,
} from "./types";

export function isItemAcknowledged(acknowledged: Checks | undefined, rubricId: string, itemId: string): boolean {
  return Boolean(acknowledged?.[rubricId]?.includes(itemId));
}

export function isScoreChangedAfterReview(
  feedbackItem: StructuredFeedbackItem | undefined | null,
  reviewedScore: Decimal | undefined,
  awardedPoints: number,
  acknowledged: boolean,
): boolean {
  if (!feedbackItem || acknowledged) return false;
  if (reviewedScore === undefined || reviewedScore === null || String(reviewedScore).trim() === "") {
    return false;
  }
  const rev = Number(reviewedScore);
  if (!Number.isFinite(rev) || !Number.isFinite(awardedPoints)) return false;
  return Math.round(rev * 100) !== Math.round(awardedPoints * 100);
}

export function countUnresolvedReviews(
  rubrics: Rubric[],
  checks: Checks,
  itemScores: ItemScores = {},
  structuredFeedback?: StructuredFeedback | null,
  reviewedItemScores: ItemScores = {},
  acknowledgedReviewItems: Checks = {},
): { unacknowledgedFlags: number; scoreMismatches: number; total: number } {
  if (!structuredFeedback?.items?.length) {
    return { unacknowledgedFlags: 0, scoreMismatches: 0, total: 0 };
  }
  let unacknowledgedFlags = 0;
  let scoreMismatches = 0;
  for (const fbItem of structuredFeedback.items) {
    const rubric = rubrics.find((r) => r.rubricId === fbItem.rubricId);
    const rubricItem = rubric?.criteria.flatMap((c) => c.items).find((i) => i.itemId === fbItem.itemId);
    const max = rubricItem ? Number(rubricItem.points) : 0;
    const rawAwarded = itemPoints(fbItem.rubricId, fbItem.itemId, max, checks, itemScores);
    const awarded = validItemScore(rawAwarded, max) ? Number(rawAwarded) : NaN;
    const ack = isItemAcknowledged(acknowledgedReviewItems, fbItem.rubricId, fbItem.itemId);
    if (fbItem.requiresReview && !ack) {
      unacknowledgedFlags += 1;
    }
    const revScore = reviewedItemScores?.[fbItem.rubricId]?.[fbItem.itemId];
    if (isScoreChangedAfterReview(fbItem, revScore, awarded, ack)) {
      scoreMismatches += 1;
    }
  }
  return {
    unacknowledgedFlags,
    scoreMismatches,
    total: unacknowledgedFlags + scoreMismatches,
  };
}

function cleanSuggestionText(suggestion: string, isFullMarks: boolean): string {
  if (!isFullMarks) return suggestion;
  return suggestion.replace(/^Gợi ý thêm\s*\(không trừ điểm\)\s*:\s*/i, "");
}

function parseSummaryLines(value: string): string[] {
  return value
    .split("\n")
    .map((line) => line.trim())
    .filter((line) => line.length > 0)
    .slice(0, 5)
    .map((line) => line.slice(0, 500));
}

export function GradingFeedbackSummary({
  summary,
  title = "Tóm tắt nhận xét cấu trúc",
  editable = false,
  disabled = false,
  onChange,
  onClear,
}: {
  summary?: StructuredFeedbackSummary | null;
  title?: string;
  editable?: boolean;
  disabled?: boolean;
  onChange?: (next: StructuredFeedbackSummary) => void;
  onClear?: () => void;
}) {
  if (!summary) return null;
  const hasAny =
    summary.strengths.length > 0 || summary.improvements.length > 0;
  if (!editable && !hasAny) return null;

  return (
    <section
      aria-label={title}
      className="mt-2.5 rounded-xl border border-line bg-shell/40 p-3 text-xs leading-relaxed"
    >
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h3 className="text-xs font-bold text-ink">{title}</h3>
        {editable && onClear && (
          <button
            type="button"
            disabled={disabled}
            onClick={onClear}
            className="rounded border border-line-strong bg-white px-2 py-1 text-xs font-medium text-danger hover:bg-danger-soft disabled:opacity-50"
          >
            Xóa nhận xét cấu trúc
          </button>
        )}
      </div>
      {editable && onChange ? (
        <div className="mt-2 grid gap-2.5 sm:grid-cols-2">
          <label className="block font-medium text-ink">
            Làm tốt (mỗi dòng 1 ý, tối đa 5)
            <textarea
              aria-label="Làm tốt"
              disabled={disabled}
              rows={3}
              value={summary.strengths.join("\n")}
              onChange={(e) =>
                onChange({ ...summary, strengths: parseSummaryLines(e.target.value) })
              }
              className="mt-1 block w-full rounded-lg border border-line bg-white p-2 text-xs font-normal text-ink focus:border-primary focus:outline-none"
            />
          </label>
          <label className="block font-medium text-ink">
            Cần sửa (mỗi dòng 1 ý, tối đa 5)
            <textarea
              aria-label="Cần sửa"
              disabled={disabled}
              rows={3}
              value={summary.improvements.join("\n")}
              onChange={(e) =>
                onChange({ ...summary, improvements: parseSummaryLines(e.target.value) })
              }
              className="mt-1 block w-full rounded-lg border border-line bg-white p-2 text-xs font-normal text-ink focus:border-primary focus:outline-none"
            />
          </label>
        </div>
      ) : (
        <div className={`mt-2 grid gap-2.5 ${summary.strengths.length > 0 && summary.improvements.length > 0 ? "sm:grid-cols-2" : ""}`}>
          {summary.strengths.length > 0 && (
            <div className="rounded-lg border border-line-soft bg-white p-2.5">
              <p className="font-semibold text-success">✓ Làm tốt</p>
              <ul className="mt-1 list-disc space-y-1 pl-4 text-ink">
                {summary.strengths.map((entry, idx) => (
                  <li key={idx} className="break-words">{entry}</li>
                ))}
              </ul>
            </div>
          )}
          {summary.improvements.length > 0 && (
            <div className="rounded-lg border border-line-soft bg-white p-2.5">
              <p className="font-semibold text-warning">! Cần sửa</p>
              <ul className="mt-1 list-disc space-y-1 pl-4 text-ink">
                {summary.improvements.map((entry, idx) => (
                  <li key={idx} className="break-words">{entry}</li>
                ))}
              </ul>
            </div>
          )}
        </div>
      )}
    </section>
  );
}

export function RubricItemFeedback({
  rubricId,
  itemId,
  itemText,
  awardedPoints,
  maxPoints,
  feedbackItem,
  reviewedScore,
  acknowledged = false,
  validBlockIds,
  editable = false,
  disabled = false,
  onChangeItem,
  onToggleAcknowledge,
}: {
  rubricId: string;
  itemId: string;
  itemText: string;
  awardedPoints: number;
  maxPoints: number;
  feedbackItem?: StructuredFeedbackItem | null;
  reviewedScore?: Decimal;
  acknowledged?: boolean;
  validBlockIds?: Set<string>;
  editable?: boolean;
  disabled?: boolean;
  onChangeItem?: (updated: StructuredFeedbackItem) => void;
  onToggleAcknowledge?: (acknowledged: boolean) => void;
}) {
  const [editing, setEditing] = useState(false);

  if (!feedbackItem) {
    if (!editable || !onChangeItem) return null;
    return (
      <div className="mt-1.5 border-t border-line-soft pt-1.5 text-xs">
        <button
          type="button"
          disabled={disabled}
          onClick={() => {
            setEditing(true);
            onChangeItem({
              rubricId,
              itemId,
              evidence: "",
              reason: "Đạt yêu cầu theo tiêu chí.",
              suggestion: null,
              evidenceBlockId: null,
              requiresReview: false,
              reviewReason: null,
            });
          }}
          className="font-medium text-primary underline underline-offset-2 hover:text-ink disabled:opacity-50"
        >
          + Thêm nhận xét chi tiết cho mục này
        </button>
      </div>
    );
  }

  const isFullMarks =
    Number.isFinite(awardedPoints) &&
    Number.isFinite(maxPoints) &&
    maxPoints > 0 &&
    Math.round(awardedPoints * 100) === Math.round(maxPoints * 100);
  const needsAiReview = Boolean(feedbackItem.requiresReview && !acknowledged);
  const scoreMismatch = isScoreChangedAfterReview(
    feedbackItem,
    reviewedScore,
    awardedPoints,
    acknowledged,
  );
  const needsAttention = needsAiReview || scoreMismatch;
  const showAckControl = editable && Boolean(onToggleAcknowledge) && (feedbackItem.requiresReview || scoreMismatch || acknowledged);

  const blockId = feedbackItem.evidenceBlockId?.trim() || null;
  const canNavigateToBlock = Boolean(blockId && validBlockIds?.has(blockId));
  const suggestionLabel = isFullMarks ? "Gợi ý thêm (không trừ điểm)" : "Cách cải thiện";

  return (
    <details
      open={needsAttention || editing}
      className={`mt-2 rounded-lg border p-2.5 text-xs leading-relaxed ${
        needsAttention
          ? "border-warning bg-warning-soft/50 text-ink"
          : "border-line-soft bg-white text-ink"
      }`}
    >
      <summary className="flex cursor-pointer flex-wrap items-center justify-between gap-2 font-semibold text-ink">
        <span className="inline-flex flex-wrap items-center gap-1.5">
          <span>Chi tiết nhận xét & minh chứng</span>
          {needsAiReview && (
            <Pill tone="warning">
              <span aria-hidden="true">⚠</span>
              <span>Cần giảng viên kiểm tra</span>
            </Pill>
          )}
          {scoreMismatch && (
            <Pill tone="warning">
              <span aria-hidden="true">⚠</span>
              <span>Điểm đã đổi, cần xác nhận giải thích</span>
            </Pill>
          )}
          {!needsAttention && acknowledged && (
            <Pill tone="success">
              <span aria-hidden="true">✓</span>
              <span>Đã xác nhận</span>
            </Pill>
          )}
        </span>
      </summary>

      <div className="mt-2 space-y-2 border-t border-line-soft pt-2">
        {feedbackItem.requiresReview && (
          <div
            role="note"
            className="rounded-md border border-warning bg-white px-2.5 py-2 text-xs text-ink"
          >
            <p className="font-semibold text-warning">
              ⚠ Cần giảng viên kiểm tra thủ công:{" "}
              <span className="font-normal text-ink">
                {feedbackItem.reviewReason ||
                  "Minh chứng chưa đọc rõ hoặc có hướng thiết kế thay thế hợp lệ."}
              </span>
            </p>
          </div>
        )}

        {scoreMismatch && (
          <div
            role="alert"
            className="rounded-md border border-warning bg-white px-2.5 py-2 text-xs text-ink"
          >
            <p className="font-semibold text-warning">
              ⚠ Điểm mục này đã thay đổi sau khi tạo nhận xét ({Number(reviewedScore).toFixed(2)} →{" "}
              {Number.isFinite(awardedPoints) ? awardedPoints.toFixed(2) : "—"}).
            </p>
            <p className="mt-0.5 text-muted">
              Hãy sửa lại giải thích bên dưới hoặc đánh dấu xác nhận nếu giải thích hiện tại vẫn phù hợp trước khi chốt điểm.
            </p>
          </div>
        )}

        {editable && onChangeItem ? (
          <div className="space-y-2">
            <label className="block font-medium text-ink">
              Minh chứng từ bài làm
              <textarea
                aria-label={`Minh chứng: ${itemText}`}
                disabled={disabled}
                rows={2}
                maxLength={1000}
                value={feedbackItem.evidence ?? ""}
                onChange={(e) =>
                  onChangeItem({ ...feedbackItem, evidence: e.target.value || null })
                }
                className="mt-1 block w-full rounded border border-line bg-white p-1.5 text-xs font-normal text-ink focus:border-primary focus:outline-none"
              />
            </label>
            <label className="block font-medium text-ink">
              Giải thích điểm (bắt buộc)
              <textarea
                aria-label={`Giải thích điểm: ${itemText}`}
                disabled={disabled}
                rows={2}
                maxLength={1000}
                value={feedbackItem.reason}
                onChange={(e) =>
                  onChangeItem({ ...feedbackItem, reason: e.target.value })
                }
                className="mt-1 block w-full rounded border border-line bg-white p-1.5 text-xs font-normal text-ink focus:border-primary focus:outline-none"
              />
            </label>
            <label className="block font-medium text-ink">
              {suggestionLabel}
              <textarea
                aria-label={`Gợi ý: ${itemText}`}
                disabled={disabled}
                rows={2}
                maxLength={1000}
                value={feedbackItem.suggestion ?? ""}
                onChange={(e) =>
                  onChangeItem({ ...feedbackItem, suggestion: e.target.value || null })
                }
                className="mt-1 block w-full rounded border border-line bg-white p-1.5 text-xs font-normal text-ink focus:border-primary focus:outline-none"
              />
            </label>
          </div>
        ) : (
          <div className="space-y-1.5">
            {feedbackItem.evidence && (
              <p className="break-words">
                <strong className="text-body">Minh chứng: </strong>
                <span>{feedbackItem.evidence}</span>
              </p>
            )}
            <p className="break-words">
              <strong className="text-body">Giải thích điểm: </strong>
              <span>{feedbackItem.reason}</span>
            </p>
            {feedbackItem.suggestion && (
              <p className="break-words">
                <strong className="text-body">{suggestionLabel}: </strong>
                <span>{cleanSuggestionText(feedbackItem.suggestion, isFullMarks)}</span>
              </p>
            )}
          </div>
        )}

        {blockId && (
          <div className="text-xs">
            {canNavigateToBlock ? (
              <a
                href={`#submission-block-${blockId}`}
                className="font-semibold text-primary underline underline-offset-2 hover:text-ink"
              >
                Đi tới đoạn bài làm ({blockId})
              </a>
            ) : (
              <span className="text-muted">
                Tham chiếu đoạn: {blockId} (hiển thị dạng văn bản, không có neo trực tiếp)
              </span>
            )}
          </div>
        )}

        {showAckControl && onToggleAcknowledge && (
          <label className="mt-1 flex cursor-pointer items-center gap-2 rounded border border-line bg-shell/60 px-2.5 py-1.5 font-medium text-ink">
            <input
              type="checkbox"
              disabled={disabled}
              checked={acknowledged}
              aria-label={`Đã kiểm tra và xác nhận giải thích: ${itemText}`}
              onChange={(e) => onToggleAcknowledge(e.target.checked)}
              className="h-4 w-4 rounded border-line-strong text-primary focus:ring-primary"
            />
            <span>Đã kiểm tra và xác nhận giải thích này phù hợp với điểm số</span>
          </label>
        )}
      </div>
    </details>
  );
}
