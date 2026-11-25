import { INK_ORANGE, INK_TEAL } from "@/shared/ui/Logo";
import { ClassDiagram, ReviewSheet } from "./ReviewSheet";

type Translate = (key: string) => string;

/*
 * Hình minh hoạ cho từng tính năng, dựng bằng chính token màu và kiểu chữ của ứng dụng để người xem nhận ra màn hình thật.
 * Dữ liệu trong hình là ví dụ (tên lớp, tên bài, tên thành viên). Chỉ để minh hoạ nên trình đọc màn hình bỏ qua; nội dung
 * tính năng nằm ở phần chữ bên cạnh.
 */

const FRAME = "w-full overflow-hidden rounded-[28px] border border-line bg-surface shadow-[0_30px_70px_rgb(24_33_45/0.12),0_4px_12px_rgb(24_33_45/0.05)]";

function WindowBar({ title }: { title: string }) {
  return (
    <div className="flex items-center gap-3 border-b border-line bg-sunken px-5 py-3">
      <span className="flex gap-1.5" aria-hidden="true">
        <i className="size-2.5 rounded-full bg-line-strong" />
        <i className="size-2.5 rounded-full bg-line-strong" />
        <i className="size-2.5 rounded-full bg-line-strong" />
      </span>
      <span className="truncate text-[12px] font-semibold text-muted">{title}</span>
    </div>
  );
}

/** Giao bài: đề bài có rubric chấm, giao cho lớp kèm hạn nộp. */
export function AssignVisual({ tr }: { tr: Translate }) {
  const rubric = [
    { key: "actors", points: 3 },
    { key: "useCases", points: 4 },
    { key: "relations", points: 3 },
  ];
  return (
    <div className={FRAME} aria-hidden="true">
      <WindowBar title={tr("visual.assign.window")} />
      <div className="flex flex-col gap-5 p-6 sm:p-7">
        <div className="flex flex-wrap items-center gap-2">
          <span className="rounded-full bg-primary-soft px-3 py-1 text-[12px] font-semibold text-primary-ink">{tr("visual.assign.class")}</span>
          <span className="rounded-full bg-orange-soft px-3 py-1 text-[12px] font-semibold text-orange">{tr("visual.assign.deadline")}</span>
        </div>
        <p className="text-[19px] leading-snug font-extrabold tracking-[-0.01em] text-ink">{tr("visual.assign.title")}</p>
        <ul className="flex flex-col">
          {rubric.map(({ key, points }, index) => (
            <li key={key} className="flex items-center justify-between gap-4 border-t border-line py-3 text-[14px]">
              <span className="flex items-center gap-3 text-body">
                <span className="inline-flex size-6 items-center justify-center rounded-full bg-sunken text-[12px] font-bold text-muted">{index + 1}</span>
                {tr(`visual.assign.rubric.${key}`)}
              </span>
              <span className="font-bold text-ink tabular-nums">{points}đ</span>
            </li>
          ))}
        </ul>
        <div className="flex items-center justify-between gap-3">
          <span className="text-[12px] text-muted">{tr("visual.assign.students")}</span>
          <span className="rounded-full bg-primary px-5 py-2.5 text-[14px] font-bold text-on-primary">{tr("visual.assign.publish")}</span>
        </div>
      </div>
    </div>
  );
}

/** Vẽ sơ đồ: khung trình vẽ có thanh hình UML và sơ đồ lớp đang vẽ dở. */
export function DiagramVisual({ tr }: { tr: Translate }) {
  return (
    <div className={FRAME} aria-hidden="true">
      <WindowBar title={tr("visual.diagram.window")} />
      <div className="grid grid-cols-[56px_1fr]">
        <div className="flex flex-col items-center gap-3 border-r border-line bg-sunken py-5">
          <span className="h-6 w-8 rounded-[5px] border-2 border-line-strong" />
          <span className="size-7 rounded-full border-2 border-line-strong" />
          <svg viewBox="0 0 24 24" className="size-7 text-line-strong" fill="none" stroke="currentColor" strokeWidth="2.2">
            <path d="M12 4 L20 18 H4 Z" strokeLinejoin="round" />
          </svg>
          <svg viewBox="0 0 24 34" className="h-8 w-6 text-line-strong" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round">
            <circle cx="12" cy="6" r="4" />
            <path d="M12 10 V22 M4 14 H20 M12 22 L5 32 M12 22 L19 32" />
          </svg>
          <span className="h-0.5 w-7 rounded bg-line-strong" />
        </div>
        <div className="relative bg-[linear-gradient(var(--c-line-soft)_1px,transparent_1px),linear-gradient(90deg,var(--c-line-soft)_1px,transparent_1px)] bg-[size:20px_20px] p-5">
          <ClassDiagram className="h-auto w-full" />
          <span className="absolute right-5 bottom-4 rounded-full bg-surface px-3 py-1 text-[12px] font-semibold text-muted shadow-[var(--shadow-lift)]">
            {tr("visual.diagram.saved")}
          </span>
        </div>
      </div>
    </div>
  );
}

/** AI chấm điểm: phiếu rubric quen thuộc của landing, trên nền bảng vẽ tối. */
export function GradeVisual({ tr }: { tr: Translate }) {
  return (
    <div data-theme="dark" className="drafting-board w-full rounded-[28px] p-5 text-body shadow-[0_30px_70px_rgb(24_33_45/0.22)] sm:p-8" aria-hidden="true">
      <ReviewSheet tr={tr} />
    </div>
  );
}

/** AI tạo đề: giảng viên mô tả đề cần soạn, AI trả về bản nháp chờ duyệt. */
export function DraftVisual({ tr }: { tr: Translate }) {
  return (
    <div className={FRAME} aria-hidden="true">
      <WindowBar title={tr("visual.draft.window")} />
      <div className="flex flex-col gap-4 p-6 sm:p-7">
        <p className="ml-auto max-w-[85%] rounded-[20px] rounded-br-md bg-primary px-4 py-3 text-[14px] leading-6 text-on-primary">
          {tr("visual.draft.prompt")}
        </p>
        <div className="max-w-[92%] rounded-[20px] rounded-bl-md border border-line bg-sunken p-4">
          <p className="flex items-center gap-2 text-[12px] font-bold" style={{ color: INK_TEAL }}>
            <svg viewBox="0 0 16 16" className="size-3.5" fill="currentColor"><path d="M8 0l1.8 5.2L15 7l-5.2 1.8L8 14l-1.8-5.2L1 7l5.2-1.8z" /></svg>
            {tr("visual.draft.badge")}
          </p>
          <ol className="mt-3 flex flex-col gap-2 text-[14px] leading-6 text-body">
            {["q1", "q2", "q3"].map((key, index) => (
              <li key={key} className="flex gap-2">
                <span className="font-bold text-ink">{index + 1}.</span>
                {tr(`visual.draft.${key}`)}
              </li>
            ))}
          </ol>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <span className="rounded-full border border-line-strong px-3 py-1.5 text-[12px] font-semibold text-muted">{tr("visual.draft.status")}</span>
          <span className="rounded-full bg-ink px-4 py-1.5 text-[12px] font-bold text-surface">{tr("visual.draft.review")}</span>
        </div>
      </div>
    </div>
  );
}

/** Làm bài nhóm: tài liệu chung, mức đóng góp của từng thành viên, nộp chung một lần. */
export function GroupVisual({ tr }: { tr: Translate }) {
  const members = [
    { key: "lan", initials: "LA", color: INK_ORANGE, level: "high" },
    { key: "minh", initials: "MK", color: INK_TEAL, level: "high" },
    { key: "tu", initials: "TU", color: "#6f93c2", level: "fair" },
  ];
  return (
    <div className={FRAME} aria-hidden="true">
      <WindowBar title={tr("visual.group.window")} />
      <div className="flex flex-col gap-5 p-6 sm:p-7">
        <div className="flex items-center justify-between gap-3">
          <p className="text-[18px] font-extrabold tracking-[-0.01em] text-ink">{tr("visual.group.title")}</p>
          <span className="flex -space-x-2">
            {members.map(({ key, initials, color }) => (
              <span key={key} className="inline-flex size-9 items-center justify-center rounded-full border-2 border-surface text-[12px] font-bold text-white" style={{ background: color }}>
                {initials}
              </span>
            ))}
          </span>
        </div>
        <div className="rounded-2xl bg-sunken p-4 text-[14px] leading-6 text-body">
          <p className="font-semibold text-ink">{tr("visual.group.section")}</p>
          <p className="mt-1">
            {tr("visual.group.text")}
            <span className="ml-0.5 inline-block h-4 w-0.5 translate-y-0.5 animate-pulse" style={{ background: INK_ORANGE }} />
          </p>
          <span className="mt-2 inline-block rounded-md px-2 py-0.5 text-[11px] font-bold text-white" style={{ background: INK_ORANGE }}>
            {tr("visual.group.editing")}
          </span>
        </div>
        <ul className="flex flex-col gap-2.5">
          {members.map(({ key, initials, color, level }) => (
            <li key={key} className="flex items-center justify-between gap-3 text-[14px]">
              <span className="flex items-center gap-3 text-body">
                <span className="inline-flex size-7 items-center justify-center rounded-full text-[11px] font-bold text-white" style={{ background: color }}>{initials}</span>
                {tr(`visual.group.member.${key}`)}
              </span>
              <span className={level === "high" ? "rounded-full bg-success-soft px-3 py-1 text-[12px] font-semibold text-success" : "rounded-full bg-warning-soft px-3 py-1 text-[12px] font-semibold text-warning"}>
                {tr(`visual.group.level.${level}`)}
              </span>
            </li>
          ))}
        </ul>
        <span className="self-end rounded-full bg-primary px-5 py-2.5 text-[14px] font-bold text-on-primary">{tr("visual.group.submit")}</span>
      </div>
    </div>
  );
}
