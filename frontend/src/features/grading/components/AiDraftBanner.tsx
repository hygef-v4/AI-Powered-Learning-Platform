"use client";
import { Alert, Button } from "@/shared/ui";
import { msg } from "@/i18n/msg";
import type { Proposal } from "../model/types";

/** Thanh trên form chấm điểm: AI đã điền đề xuất (Giữ/Hoàn tác) hoặc đang chờ giáo viên đồng ý điền (Áp dụng/Bỏ qua). */
export function AiDraftBanner({
  proposal,
  maxScore,
  offered,
  onApply,
  onSkip,
  onKeep,
  onUndo,
  onDetails,
}: {
  proposal: Proposal;
  maxScore: number | string;
  offered: boolean;
  onApply: () => void;
  onSkip: () => void;
  onKeep: () => void;
  onUndo: () => void;
  onDetails: () => void;
}) {
  const score = Number(proposal.proposal?.score ?? 0).toFixed(2);
  const max = Number(maxScore).toFixed(2);
  const reviewCount = proposal.proposal?.structuredFeedback?.items?.filter((item) => item.requiresReview).length ?? 0;
  return (
    <Alert
      tone="info"
      role="status"
      action={
        <div className="flex flex-wrap items-center gap-2">
          {offered ? (
            <>
              <Button size="sm" variant="primary" onClick={onApply}>{msg("grading.components.AiDraftBanner.apply")}</Button>
              <Button size="sm" onClick={onSkip}>{msg("grading.components.AiDraftBanner.skip")}</Button>
            </>
          ) : (
            <>
              <Button size="sm" variant="primary" onClick={onKeep}>{msg("grading.components.AiDraftBanner.keep")}</Button>
              <Button size="sm" onClick={onUndo}>{msg("grading.components.AiDraftBanner.undo")}</Button>
            </>
          )}
          <Button size="sm" variant="ghost" onClick={onDetails}>{msg("grading.components.AiDraftBanner.details")}</Button>
        </div>
      }
    >
      {msg(offered ? "grading.components.AiDraftBanner.offered" : "grading.components.AiDraftBanner.applied", { score, max })}
      {reviewCount > 0 && ` ${msg("grading.components.AiDraftBanner.reviewItems", { count: String(reviewCount) })}`}
    </Alert>
  );
}
