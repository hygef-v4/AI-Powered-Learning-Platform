"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { Button, PageHeader, PageNav, cx, showToast } from "@/shared/ui";
import { getSubjects } from "./api";
import { SubjectFilters } from "./SubjectFilters";
import { SubjectReadError } from "./SubjectReadError";
import { SubjectTable } from "./SubjectTable";
import { SubjectFormDialog } from "./SubjectFormDialog";
import type { SubjectPage, SubjectStatus } from "./types";
import { useSubjectRead } from "./useSubjectRead";

const PAGE_SIZE = 20;

/**
 * Danh sách môn học (UC 16), cùng bố cục trang Tài khoản: một thẻ gồm tab trạng thái (mặc định Tất cả) và ô tìm cùng
 * một hàng, bảng có STT, thanh chuyển trang ở giữa. Đổi tab hay gõ tìm thì bảng cũ mờ đi tại chỗ tới khi có kết quả
 * mới, không nhảy sang dòng "Đang tải". Thêm môn xong báo nổi ở góc phải.
 */
export function SubjectListPage() {
  const [creating, setCreating] = useState(false);
  const focusAfterCreate = useRef(false);
  const [search, setSearch] = useState("");
  const [query, setQuery] = useState<{ q: string; status: SubjectStatus | undefined; page: number; size: number }>({ q: "", status: undefined, page: 0, size: PAGE_SIZE });
  useEffect(() => {
    const timer = setTimeout(() => setQuery((current) => current.q === search.trim() ? current : { ...current, q: search.trim(), page: 0 }), 300);
    return () => clearTimeout(timer);
  }, [search]);
  const load = useCallback(() => getSubjects(query), [query]);
  const resource = useSubjectRead(JSON.stringify(query), load);
  useEffect(() => {
    if (!resource.loading && focusAfterCreate.current) {
      focusAfterCreate.current = false;
      document.getElementById("subjects-create-open")?.focus();
    }
  }, [resource.loading]);
  // Giữ trang kết quả gần nhất để hiện mờ trong lúc tải trang mới (đổi tab, tìm, chuyển trang).
  const [last, setLast] = useState<SubjectPage | null>(null);
  if (resource.data && resource.data !== last) setLast(resource.data);
  const searching = search.trim() !== query.q;
  const stale = resource.loading || searching;
  const data = resource.error !== null ? null : (resource.data ?? last);

  return <>
    <PageHeader title="Môn học"
      actions={<Button id="subjects-create-open" variant="primary" icon="add" onClick={() => setCreating(true)} disabled={resource.error !== null} data-testid="subjects-create-open">Thêm môn học</Button>} />
    {creating && <SubjectFormDialog onClose={() => setCreating(false)} onCreated={() => {
      setCreating(false);
      focusAfterCreate.current = true;
      showToast("Đã thêm môn học");
      resource.reload();
    }} />}
    <section className="flex flex-col gap-3.5 rounded-xl border border-line bg-surface p-5">
      <SubjectFilters search={search} status={query.status} onSearch={setSearch}
        onStatus={(status) => setQuery((current) => ({ ...current, status, q: search.trim(), page: 0 }))} />
      {!data && resource.loading && <p role="status" className="py-8 text-center text-sm text-muted">Đang tải môn học…</p>}
      {resource.error !== null && <SubjectReadError error={resource.error} onRetry={resource.reload} />}
      {data && <div aria-busy={stale} className={cx("flex flex-col gap-3.5 transition-opacity duration-200", stale && "opacity-50")}>
        <SubjectTable items={data.items} offset={data.page * data.size} />
        <PageNav page={data.page} size={data.size} total={data.totalElements} onPage={(page) => setQuery((current) => ({ ...current, page }))} />
      </div>}
    </section>
  </>;
}
