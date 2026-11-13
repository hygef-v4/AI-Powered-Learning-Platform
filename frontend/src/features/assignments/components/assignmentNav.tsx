"use client";

import { useEffect, useState } from "react";
import { FADE_LABEL, type NavContext, type NavLocation } from "@/shared/layout/navigation";
import { BackLink } from "@/shared/navigation/BackLink";
import { Icon, cx } from "@/shared/ui";
import { MAX_SECTION_DEPTH, SectionNumber } from "@/shared/document";
import { addPart, showPart, useAssignmentOutline } from "../model/outline";

const WHEN_OPEN = "md:group-data-[collapsed]/sidebar:hidden";
const WHEN_COLLAPSED = "hidden md:group-data-[collapsed]/sidebar:block";

/** Trang soạn một bài tập (`/assignments/{id}`) hoặc trang chấm bài (`/teaching/evaluations/{id}`): sidebar là các phần của bài thay cho menu chung. */
export function assignmentContext({ pathname }: NavLocation): NavContext | null {
  const match = /^\/assignments\/([^/]+)$/.exec(pathname) ?? /^\/teaching\/evaluations\/([^/]+)$/.exec(pathname);
  if (!match) return null;
  return { key: `assignment:${match[1]}`, label: "Các phần của bài", header: <AssignmentNav />, items: [] };
}

/**
 * Trang làm bài (`/attempts/{id}`) và lịch sử nộp bài (`/my-assignments/{id}/history`) của sinh viên: cùng sidebar mục lục
 * với trang soạn, xem trước của giảng viên, chỉ không có nút thêm mục (yêu cầu 2026-11-05).
 */
export function studentAssignmentContext({ pathname }: NavLocation): NavContext | null {
  const match = /^\/attempts\/([^/]+)$/.exec(pathname) ?? /^\/my-assignments\/([^/]+)\/history$/.exec(pathname);
  if (!match) return null;
  return { key: `assignment-work:${match[1]}`, label: "Các phần của bài", header: <AssignmentNav />, items: [] };
}

/** Nút "+" nhỏ trên sidebar: thêm mục con (hàng của mục) hoặc mục ngoài cùng (header Mục lục). */
function AddButton({ label, onClick }: { label: string; onClick: () => void }) {
  return (
    <button type="button" aria-label={label} title={label} onClick={onClick}
      className={cx(WHEN_OPEN, "flex size-6 shrink-0 items-center justify-center rounded-md text-muted transition-colors hover:bg-primary-soft hover:text-primary-ink")}>
      <Icon name="add" size={15} />
    </button>
  );
}

/**
 * Sidebar của bài, cùng kiểu sidebar học liệu: lối về trang Bài tập ở đầu, rồi thẻ trắng có header Mục lục (nút "+" thêm mục
 * ngoài cùng) và các mục trên một dòng: mục ngoài cùng in đậm, mục con chữ thường lùi vào theo tầng, đoạn văn không ghi; mỗi
 * hàng có "+" thêm mục con (tới tầng I.1.a.aa.). Thu gọn sidebar thì chỉ còn số (I., I.1.). Tài liệu nhóm (U12) dùng lại.
 */
export function AssignmentNav() {
  const outline = useAssignmentOutline();
  const [activeId, setActiveId] = useState<string | null>(null);
  // Sáng theo mục trình soạn đang hiện (cuộn tới hay đang mở ở chế độ Từng phần).
  useEffect(() => {
    const follow = (event: Event) => setActiveId((event as CustomEvent<{ headingId: string | null }>).detail?.headingId ?? null);
    window.addEventListener("document:current-part", follow);
    return () => window.removeEventListener("document:current-part", follow);
  }, []);
  return (
    <div className="flex min-h-0 flex-1 flex-col">
      <div className="mb-3 shrink-0 border-b border-line pb-3.5">
        <BackLink href={outline?.backHref ?? "/teaching/classes"} title={outline?.backLabel ?? "Bài tập"}
          className="inline-flex max-w-full items-center gap-2.5 rounded-md px-2.5 py-1 text-xs font-semibold whitespace-nowrap text-muted transition-colors hover:text-ink">
          <Icon name="arrow_back" size={17} className="shrink-0" />
          <span className={FADE_LABEL}>{outline?.backLabel ?? "Bài tập"}</span>
        </BackLink>
      </div>
      <nav aria-label="Các phần của bài" className="flex min-h-0 flex-1 flex-col overflow-x-hidden overflow-y-auto">
        {!outline && <span className={cx("px-2.5 py-2 text-[13px] text-muted", FADE_LABEL)}>Đang tải…</span>}
        {outline && (
          <div className="flex flex-col rounded-xl border border-line bg-surface p-1 shadow-xs">
          <div className={cx(WHEN_OPEN, "flex items-center justify-between gap-2 border-b border-line py-1 pr-1 pl-2.5")}>
            <span className="text-xs font-semibold tracking-[.04em] text-muted uppercase">Câu hỏi</span>
            {outline.editable && <AddButton label="Thêm câu hỏi" onClick={() => addPart(null)} />}
          </div>
          {outline.entries.length === 0 && <span className={cx("px-2.5 py-2 text-[12.5px] text-faint", FADE_LABEL)}>Bài chưa có câu hỏi</span>}
          {outline.entries.length > 0 && (
          <ol className="flex flex-col gap-0.5 pt-1" data-testid="assignment-outline">
            {outline.entries.map((entry) => {
              const problem = entry.depth === 1;
              // Văn bản ngoài mọi mục: mấy chữ đầu rồi "…", chữ thường, không có "+" (yêu cầu 2026-11-04).
              const loose = entry.depth === 0;
              // Như trong trang: số rồi tiêu đề; mục chưa đặt tên chỉ có số.
              const title = loose ? entry.title || "…" : entry.title ? `${entry.number} ${entry.title}` : entry.number;
              const active = activeId === entry.id;
              return (
                <li key={entry.id} className="group/row flex items-center">
                  <button type="button" title={title} aria-current={active ? "true" : undefined}
                    style={problem || loose ? undefined : { paddingLeft: `${0.625 + (entry.depth - 1) * 0.875}rem` }}
                    onClick={() => { setActiveId(entry.id); showPart(entry.id); }}
                    className={cx("flex min-w-0 flex-1 items-center rounded-lg py-1.5 text-left text-[13px] whitespace-nowrap transition-colors",
                      problem ? "px-2.5 font-semibold text-ink" : loose ? "px-2.5 text-body" : "pr-2.5 text-body md:group-data-[collapsed]/sidebar:!pl-2.5",
                      active ? "bg-primary-soft" : "hover:bg-sunken")}>
                    <span className={cx(WHEN_COLLAPSED, "w-full text-center font-mono text-xs tabular-nums", problem ? "font-bold" : "text-muted")}>{loose ? "…" : <SectionNumber value={entry.number} />}</span>
                    <span className={cx(WHEN_OPEN, "min-w-0 truncate")}>
                      {loose ? title : <><SectionNumber value={entry.number} />{entry.title ? ` ${entry.title}` : ""}</>}
                    </span>
                  </button>
                  {outline.editable && !loose && entry.depth < MAX_SECTION_DEPTH && <AddButton label={`Thêm ý vào ${entry.number}`} onClick={() => addPart(entry.id)} />}
                </li>
              );
            })}
          </ol>
          )}
          </div>
        )}
      </nav>
    </div>
  );
}
