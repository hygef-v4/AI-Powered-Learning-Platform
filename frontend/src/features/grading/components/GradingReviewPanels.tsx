import { formatStudentLabel } from "@/shared/studentCode";
import { ActivityTimeline } from "@/shared/activity/ActivityTimeline";
import { ACTIVITY_LABELS, type Detail } from "../model/types";
import { Card } from "@/shared/ui";
import { msg } from "@/i18n/msg";

// Hiện dữ liệu hoạt động cá nhân hoặc nhóm để hỗ trợ đối chiếu lúc chấm.
export function ActivityReview({ detail }: { detail: Detail }) {
  const hasData = Boolean(detail.activity || (detail.memberActivities && detail.memberActivities.length > 0));
  return (
    <Card as="details" padding="none" className="px-4 py-3 text-sm" open={hasData}>
      <summary className="flex cursor-pointer flex-wrap items-center justify-between gap-2">
        <h2 className="inline text-sm font-bold text-ink">{msg("grading.components.GradingReviewPanels.activityWhileWorking")}</h2>
        <span className="text-xs text-muted">{ACTIVITY_LABELS[detail.activityState] ?? detail.activityState}</span>
      </summary>
      <div className="mt-2 space-y-1.5 border-t border-line-soft pt-2">
        {detail.activity?.policyState === "AVAILABLE" && (
          <>
            <ActivityTimeline summary={detail.activity} />
            <p>{detail.activity.unknownGaps} {msg("grading.components.GradingReviewPanels.gapsInTheData")}</p>
          </>
        )}
        <div>
          {detail.memberActivities?.map((m) => (
            <p key={m.student.accountId}>
              {formatStudentLabel(m.student)}: {ACTIVITY_LABELS[m.state] ?? m.state}
              {m.activity
                ? msg("grading.components.GradingReviewPanels.leftThePageTimesGaps", { awayCount: String(m.activity.awayCount), unknownGaps: String(m.activity.unknownGaps) })
                : ""}
            </p>
          ))}
        </div>
        <p className="text-xs text-muted">
          {msg("grading.components.GradingReviewPanels.forReferenceOnlyThePlatform")}</p>
      </div>
    </Card>
  );
}
