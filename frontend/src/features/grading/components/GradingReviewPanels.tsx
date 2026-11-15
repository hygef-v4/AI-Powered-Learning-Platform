import { ACTIVITY_LABELS, type Detail } from "../model/types";
import { Card } from "@/shared/ui";

export function ActivityReview({ detail }: { detail: Detail }) {
  const hasData = Boolean(detail.activity || (detail.memberActivities && detail.memberActivities.length > 0));
  return (
    <Card as="details" padding="none" className="px-4 py-3 text-sm" open={hasData}>
      <summary className="flex cursor-pointer flex-wrap items-center justify-between gap-2">
        <h2 className="inline text-sm font-bold text-ink">Hoạt động khi làm bài</h2>
        <span className="text-xs text-muted">{ACTIVITY_LABELS[detail.activityState] ?? detail.activityState}</span>
      </summary>
      <div className="mt-2 space-y-1.5 border-t border-line-soft pt-2">
        {detail.activity && (
          <p>
            {detail.activity.awayCount} lần rời trang · {detail.activity.unknownGaps} khoảng thiếu dữ
            liệu. {detail.activity.reliabilityNote}
          </p>
        )}
        <div>
          {detail.memberActivities?.map((m) => (
            <p key={m.student.accountId}>
              {m.student.name}: {ACTIVITY_LABELS[m.state] ?? m.state}
              {m.activity
                ? ` — ${m.activity.awayCount} lần rời trang, ${m.activity.unknownGaps} khoảng thiếu dữ liệu`
                : ""}
            </p>
          ))}
        </div>
        <p className="text-xs text-muted">
          Chỉ để tham khảo; nền tảng không kết luận gian lận hoặc tự thay đổi điểm.
        </p>
      </div>
    </Card>
  );
}
