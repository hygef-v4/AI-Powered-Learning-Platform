import { activityLabels, type Detail, type Evaluation } from "./types";

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
  return (
    <section className="rounded-xl border bg-white p-5">
      <h2 className="font-bold">Hoạt động khi làm bài</h2>
      <p>{activityLabels[detail.activityState] ?? detail.activityState}</p>
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
      <p className="text-sm text-slate-600">
        Chỉ để tham khảo; nền tảng không kết luận gian lận hoặc tự thay đổi điểm.
      </p>
    </section>
  );
}

export function GradeHistory({ evaluation }: { evaluation: Evaluation }) {
  return (
    <section className="rounded-xl border bg-white p-5">
      <h2 className="font-bold">Lịch sử sửa điểm</h2>
      {evaluation.changes.length ? (
        evaluation.changes.map((h, i) => (
          <p key={i}>
            {new Date(h.at).toLocaleString("vi-VN")} · {h.before} → {h.after} · {h.reason}
          </p>
        ))
      ) : (
        <p>Chưa có thay đổi điểm chính thức.</p>
      )}
    </section>
  );
}
