"use client";

import { studentAssignmentFeedback } from "@/features/assignmentwork/slots";
import { SlotOutlet } from "@/shared/slots/SlotOutlet";
import { Pill, Section, cx } from "@/shared/ui";
import { useMyGroupDoc } from "../hooks/useMyGroupDoc";
import { SECTION_STATUS, isOwnSection, memberName, sectionPerson } from "../model/status";

/**
 * Thẻ Tiến độ trên Assignment Detail bài nhóm, cùng dáng thẻ Kết quả: số mục Xong / tổng, từng mục với người nhận và trạng
 * thái; mục của mình tô nền. Nhóm đã nộp (chưa sửa gì sau đó) thì thay bằng thẻ Kết quả (U09) nếu đã có điểm công bố.
 */
export function GroupProgressCard({ assignmentId, classId }: { assignmentId: string; classId: string }) {
  const { view } = useMyGroupDoc(assignmentId);
  if (!view) return null;
  const partTitles = Object.fromEntries(view.sections.map((s) => [s.partId, s.title]));
  const result = view.lastSubmission && <SlotOutlet slot={studentAssignmentFeedback} props={{ assignmentId, classId, partTitles }} />;
  // Đã nộp và chưa sửa gì sau đó: chỉ còn thẻ Kết quả; sửa sau khi nộp thì bài lại Đang làm, hiện lại Tiến độ.
  if (view.lastSubmission && !view.lastSubmission.changedSince) return result;
  const done = view.sections.filter((s) => s.status === "DONE").length;
  return (
    <>
    <Section title="Tiến độ">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-stretch">
        <div className="flex shrink-0 flex-col justify-center rounded-xl bg-shell px-6 py-4 text-center">
          <span className="text-xs text-muted">Mục xong</span>
          <span className="mt-1 font-display text-[34px] leading-none tracking-[-0.02em] text-ink tabular-nums">{done}</span>
          <span className="mt-1 text-xs text-muted tabular-nums">/ {view.sections.length}</span>
        </div>
        {view.sections.length > 0 && (
          <ul className="flex min-w-0 flex-1 flex-col" aria-label="Các mục">
            {view.sections.map((s) => {
              const own = isOwnSection(s, view.viewerId);
              return (
                <li key={s.id} data-own={own || undefined}
                  className={cx("flex items-center justify-between gap-4 border-b py-2.5 last:border-b-0",
                    own ? "-mx-2 rounded-lg border-transparent bg-primary-soft px-2" : "border-line")}>
                  <span className="min-w-0 truncate text-[13.5px] text-ink">{s.title}</span>
                  <span className="flex shrink-0 items-center gap-3">
                    <span className="text-[13px] text-muted">{memberName(view, sectionPerson(s))}</span>
                    <Pill tone={SECTION_STATUS[s.status].tone}>{SECTION_STATUS[s.status].text}</Pill>
                  </span>
                </li>
              );
            })}
          </ul>
        )}
      </div>
    </Section>
    {result}
    </>
  );
}
