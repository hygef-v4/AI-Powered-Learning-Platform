"use client";
import { useEffect, useState } from "react";
import { Button, ClassFilter, PageHeader } from "@/shared/ui";
import { PageLoadError, PageLoading } from "@/shared/ui/PageStatus";
import { announcementsApi, announcementError } from "../api";
import { AnnouncementFeed } from "../components/AnnouncementFeed";
import { CreateAnnouncementDialog } from "../components/AnnouncementDialogs";
import type { AnnouncementFeed as Feed } from "../model/types";

/**
 * Thông báo lớp (U14). Trong một lớp (`classId`, đường dẫn dưới trang lớp) chỉ có thông báo của lớp đó, không có ô chọn lớp.
 * Ở menu chính hiện mọi lớp, có ô lọc theo lớp (`initialClassId` mở sẵn một lớp, cho link cũ `?classId=`), mỗi thông báo ghi
 * mã lớp. Người dạy có nút Tạo thông báo mở popup; ở menu chính popup cho chọn các lớp để đăng.
 */
export function ClassAnnouncementsPage({ classId: fixedClassId, initialClassId = "" }: { classId?: string; initialClassId?: string }) {
  const [filter, setFilter] = useState(initialClassId);
  const classId = fixedClassId ?? filter;
  const [pagination, setPagination] = useState({ classId, page: 1 });
  const page = pagination.classId === classId ? pagination.page : 1;
  const setPage = (next: number) => setPagination({ classId, page: next });
  const [revision, setRevision] = useState(0);
  const [creating, setCreating] = useState(false);
  const key = `${classId}:${page}:${revision}`;
  const [result, setResult] = useState<{ key: string; data?: Feed; error?: string }>({ key: "" });
  useEffect(() => {
    const controller = new AbortController();
    void announcementsApi.list(classId, page, 20, controller.signal)
      .then((data) => { if (!controller.signal.aborted) setResult({ key, data }); })
      .catch((e) => { if (!controller.signal.aborted) setResult((previous) => ({ key, data: previous.data, error: announcementError(e) })); });
    return () => controller.abort();
  }, [classId, page, key]);
  // Đang tải lại thì giữ danh sách cũ để trang không nháy trống.
  const data = result.data;
  const error = result.key === key ? result.error : undefined;
  const reload = () => setRevision((value) => value + 1);

  if (!data) return error ? <PageLoadError>{error}</PageLoadError> : <PageLoading label="Đang tải thông báo…" />;
  const manageable = data.classes.filter((c) => c.canManage && (!fixedClassId || c.classId === fixedClassId));
  const codes = fixedClassId ? undefined : Object.fromEntries(data.classes.map((c) => [c.classId, c.code]));

  return (
    <div className="flex flex-col gap-4">
      <PageHeader title="Thông báo lớp" actions={<>
        {!fixedClassId && (
          <ClassFilter classes={data.classes} value={filter} onChange={setFilter} testId="announcement-class-filter" />
        )}
        {manageable.length > 0 && <Button variant="primary" icon="add" onClick={() => setCreating(true)} data-testid="announcement-create">Tạo thông báo</Button>}
      </>} />
      {error && <div role="alert" className="flex items-center gap-3 text-[13px] text-danger">{error}<Button size="sm" onClick={reload}>Thử lại</Button></div>}
      <AnnouncementFeed items={data.items} classCodes={codes} onChanged={reload} />
      {(page > 1 || data.hasMore) && (
        <nav className="flex items-center justify-end gap-2" aria-label="Phân trang thông báo">
          <Button size="sm" disabled={page === 1} onClick={() => setPage(page - 1)}>Trước</Button>
          <span className="text-[13px] text-muted tabular-nums">Trang {page}</span>
          <Button size="sm" disabled={!data.hasMore} onClick={() => setPage(page + 1)}>Sau</Button>
        </nav>
      )}
      {creating && <CreateAnnouncementDialog classes={manageable} fixedClassId={fixedClassId}
        onClose={() => setCreating(false)} onCreated={reload} />}
    </div>
  );
}
