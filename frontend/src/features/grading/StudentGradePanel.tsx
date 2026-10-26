"use client";
import { useCallback } from "react";
import { gradingApi } from "./api";
import { useResource } from "./useResource";
import { GradingFeedbackSummary } from "./StructuredFeedbackSection";
import { points } from "./types";

export function StudentGradePanel({ attemptId }: { attemptId: string }) {
  const load = useCallback(() => gradingApi.attempt(attemptId), [attemptId]);
  const { value, error } = useResource(load);

  const hasRubricItems = Boolean(value?.rubricItems && value.rubricItems.length > 0);
  const hasRubricScoreDiff = Boolean(
    value?.score != null &&
      value?.rubricScore != null &&
      Math.round(Number(value.score) * 100) !== Math.round(Number(value.rubricScore) * 100),
  );

  return (
    <section aria-label="Kết quả chính thức" className="rounded-xl border border-line bg-white p-4 text-sm leading-relaxed">
      <h3 className="text-base font-bold text-ink">Kết quả chính thức</h3>
      {error && <p role="alert" className="mt-2 text-danger">{error}</p>}
      {value?.score != null ? (
        <div className="mt-2 space-y-3">
          <div className="flex flex-wrap items-baseline justify-between gap-2">
            <p className="text-base font-semibold tabular-nums text-ink">
              {points(value.score, value.maxScore)} {value.updated && "· Đã cập nhật"}
            </p>
            {hasRubricScoreDiff && (
              <span className="text-xs text-muted">
                Điểm theo rubric: {Number(value.rubricScore).toFixed(2)} / {Number(value.maxScore ?? 0).toFixed(2)}
              </span>
            )}
          </div>
          {value.memberOverride && (
            <p className="rounded-lg border border-line bg-shell/60 px-3 py-2 text-xs text-muted">
              Điểm cá nhân đã được giảng viên điều chỉnh riêng; chi tiết nhận xét rubric dưới đây là nhận xét chung cho bài nộp của nhóm.
            </p>
          )}
          {value.structuredSummary && (
            <GradingFeedbackSummary
              summary={value.structuredSummary}
              title="Tóm tắt nhận xét (Làm tốt · Cần sửa · Gợi ý tiếp theo)"
            />
          )}
          {hasRubricItems && (
            <div className="space-y-2">
              <h4 className="text-xs font-bold uppercase tracking-wide text-body">
                Chi tiết điểm & nhận xét theo từng tiêu chí ({value.rubricItems!.length})
              </h4>
              {value.rubricItems!.map((item) => {
                const awarded = Number(item.awardedPoints);
                const max = Number(item.maxPoints);
                const isFull =
                  Number.isFinite(awarded) &&
                  Number.isFinite(max) &&
                  max > 0 &&
                  Math.round(awarded * 100) === Math.round(max * 100);
                const hasDetails = Boolean(item.evidence || item.reason || item.suggestion);
                const suggestionLabel = isFull
                  ? "Gợi ý thêm (không trừ điểm)"
                  : "Gợi ý cải thiện";
                return (
                  <details
                    key={`${item.rubricId}-${item.itemId}`}
                    className="rounded-lg border border-line bg-shell/30 p-2.5 text-xs"
                  >
                    <summary className="flex cursor-pointer flex-wrap items-center justify-between gap-2 font-semibold text-ink">
                      <span className="min-w-0 flex-1 break-words">
                        {item.criterionTitle ? `${item.criterionTitle}: ` : ""}
                        {item.itemText}
                      </span>
                      <span className="shrink-0 tabular-nums text-primary">
                        {awarded.toFixed(2)} / {max.toFixed(2)} điểm
                      </span>
                    </summary>
                    {hasDetails ? (
                      <div className="mt-2 space-y-1.5 border-t border-line-soft pt-2 text-ink">
                        {item.evidence && (
                          <p className="break-words">
                            <strong className="text-body">Minh chứng: </strong>
                            <span>{item.evidence}</span>
                          </p>
                        )}
                        {item.reason && (
                          <p className="break-words">
                            <strong className="text-body">Giải thích điểm: </strong>
                            <span>{item.reason}</span>
                          </p>
                        )}
                        {item.suggestion && (
                          <p className="break-words">
                            <strong className="text-body">{suggestionLabel}: </strong>
                            <span>{item.suggestion}</span>
                          </p>
                        )}
                      </div>
                    ) : (
                      <p className="mt-2 border-t border-line-soft pt-2 text-muted">
                        Chưa có nhận xét chi tiết cho tiêu chí này.
                      </p>
                    )}
                  </details>
                );
              })}
            </div>
          )}
          {value.feedback && (
            <div className="rounded-lg border border-line-soft bg-shell/20 p-2.5">
              <p className=" whitespace-pre-wrap text-ink">{value.feedback}</p>
            </div>
          )}
          <p className="text-xs text-muted">
            Công bố: {value.publishedAt ? new Date(value.publishedAt).toLocaleString("vi-VN") : "—"}
          </p>
        </div>
      ) : (
        !error && <p className="mt-2 text-muted">Chưa có điểm được công bố.</p>
      )}
    </section>
  );
}
