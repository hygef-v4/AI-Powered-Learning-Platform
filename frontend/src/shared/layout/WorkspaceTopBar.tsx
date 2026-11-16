import type { ReactNode, Ref } from "react";

/**
 * Thanh trên cùng của các trang soạn / làm bài dạng tài liệu (soạn đề, làm bài, lịch sử nộp, tài liệu nhóm): tên bài và
 * trạng thái bên trái, chỗ đặt nút Cả trang / Từng phần ở giữa, thao tác bên phải. Dính dưới thanh trên của khung trang.
 */
export function WorkspaceTopBar({ barRef, title, status, setViewSlot, actions }: {
  barRef: Ref<HTMLElement>;
  title: string;
  /** Đặt ngay sau tên bài (nhãn trạng thái, thời gian còn lại…). */
  status?: ReactNode;
  setViewSlot: (element: HTMLElement | null) => void;
  actions?: ReactNode;
}) {
  return (
    <header ref={barRef} className="sticky top-14 z-30 main-bleed -mt-8 grid grid-cols-[minmax(0,1fr)_auto_minmax(0,1fr)] items-center gap-x-4 gap-y-2 border-b border-line bg-[var(--c-topbar)] py-3 backdrop-blur-md md:-mt-10">
      <div className="flex min-w-0 flex-1 items-center gap-x-3">
        <h1 className="min-w-0 truncate text-[15px] font-semibold text-ink" title={title}>{title}</h1>
        {status}
      </div>
      <div ref={setViewSlot} data-testid="view-slot" className="flex justify-center" />
      <div className="flex flex-wrap items-center justify-end gap-2">{actions}</div>
    </header>
  );
}
