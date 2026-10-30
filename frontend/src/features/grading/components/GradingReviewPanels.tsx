import { activityLabels, type Detail, type Evaluation } from "../model/types";

export function GroupSectionAuthors({ detail }: { detail: Detail }) {
  return (
    <>
      {detail.evaluation.kind === "GROUP_DOCUMENT" && detail.sectionAuthors?.length > 0 && (
        <section className="rounded-xl border p-4">
          <h2 className="font-bold">Người thực hiện từng phần</h2>
          {detail.sectionAuthors.map((author, i) => {
            const block = detail.document.blocks.find((b) => b.id === author.partId);
            const name =
              detail.memberActivities?.find((m) => m.student.accountId === author.accountId)
                ?.student.name ?? "Thành viên";
            return (
              <p key={i}>
                {typeof block?.content.text === "string" ? block.content.text : "Phần tài liệu"}:{" "}
                {name}
              </p>
            );
          })}
        </section>
      )}
    </>
  );
}

export function ActivityReview({ detail }: { detail: Detail }) {
  const hasData = Boolean(detail.activity || (detail.memberActivities && detail.memberActivities.length > 0));
  return (
    <details open={hasData} className="rounded-xl border border-line bg-surface px-4 py-3 text-sm">
      <summary className="flex cursor-pointer flex-wrap items-center justify-between gap-2">
        <h2 className="inline text-sm font-bold text-ink">Hoạt động khi làm bài</h2>
        <span className="text-xs text-muted">{activityLabels[detail.activityState] ?? detail.activityState}</span>
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
              {m.student.name}: {activityLabels[m.state] ?? m.state}
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
    </details>
  );
}

export function GradeHistory({ evaluation }: { evaluation: Evaluation }) {
  const hasChanges = evaluation.changes.length > 0;
  return (
    <details open={hasChanges} className="rounded-xl border border-line bg-surface px-4 py-3 text-sm">
      <summary className="flex cursor-pointer flex-wrap items-center justify-between gap-2">
        <h2 className="inline text-sm font-bold text-ink">Lịch sử sửa điểm</h2>
        <span className="text-xs text-muted">
          {hasChanges ? `${evaluation.changes.length} lần thay đổi` : "Chưa có thay đổi điểm chính thức."}
        </span>
      </summary>
      {hasChanges && (
        <div className="mt-2 space-y-1 border-t border-line-soft pt-2">
          {evaluation.changes.map((h, i) => (
            <p key={i}>
              {new Date(h.at).toLocaleString("vi-VN")} · {h.before} → {h.after} · {h.reason}
            </p>
          ))}
        </div>
      )}
    </details>
  );
}
