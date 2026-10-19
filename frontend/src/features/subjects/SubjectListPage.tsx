"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { Button, PageHeader, Select } from "@/shared/ui";
import { getSubjects } from "./api";
import { SubjectFilters } from "./SubjectFilters";
import { SubjectReadError } from "./SubjectReadError";
import { SubjectTable } from "./SubjectTable";
import { SubjectFormDialog } from "./SubjectFormDialog";
import type { SubjectStatus } from "./types";
import { useSubjectRead } from "./useSubjectRead";

export function SubjectListPage() {
  const [creating, setCreating] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);
  const focusAfterCreate = useRef(false);
  const [search, setSearch] = useState("");
  const [query, setQuery] = useState({ q: "", status: "ACTIVE" as SubjectStatus, page: 0, size: 20 });
  useEffect(() => {
    const timer = setTimeout(() => setQuery((current) => current.q === search.trim() ? current : { ...current, q: search.trim(), page: 0 }), 300);
    return () => clearTimeout(timer);
  }, [search]);
  const load = useCallback(() => getSubjects(query), [query]);
  const resource = useSubjectRead(JSON.stringify(query), load);
  useEffect(() => {
    if (!resource.loading && focusAfterCreate.current) {
      focusAfterCreate.current = false;
      document.getElementById(resource.error === null ? "subjects-create-open" : "subjects-create-notice")?.focus();
    }
  }, [resource.loading, resource.error]);
  const searching = search.trim() !== query.q;
  const data = searching ? null : resource.data;
  const counts = data?.statusCounts ?? null;
  const total = counts ? counts.ACTIVE + counts.ARCHIVED : null;

  return <>
    <PageHeader title="Môn học" subtitle={total === null ? undefined : `${total} môn học`}
      actions={<Button id="subjects-create-open" variant="primary" onClick={() => { setNotice(null); setCreating(true); }} disabled={resource.loading || resource.error !== null || searching} data-testid="subjects-create-open">Thêm môn học</Button>} />
    {notice && <p id="subjects-create-notice" tabIndex={-1} role="status" className="text-sm text-body">{notice}</p>}
    {creating && <SubjectFormDialog onClose={() => setCreating(false)} onCreated={(subject) => {
      setCreating(false);
      focusAfterCreate.current = true;
      setNotice(`Đã tạo môn ${subject.code}. Danh sách đang được tải lại theo bộ lọc hiện tại.`);
      resource.reload();
    }} />}
    <SubjectFilters search={search} status={query.status} counts={counts} onSearch={setSearch}
      onStatus={(status) => setQuery((current) => ({ ...current, status, q: search.trim(), page: 0 }))} />
    {(resource.loading || searching) && <p role="status" className="py-8 text-center text-sm text-muted">Đang tải môn học…</p>}
    {!searching && resource.error !== null && <SubjectReadError error={resource.error} onRetry={resource.reload} />}
    {data && <div className="flex flex-col gap-3">
      <SubjectTable items={data.items} />
      <div className="flex flex-wrap items-center justify-between gap-3 text-xs text-muted" aria-label="Phân trang môn học">
        <p>{data.items.length > 0 ? `Hiển thị ${data.page * data.size + 1} – ${data.page * data.size + data.items.length} trong ${data.totalElements} môn` : `${data.totalElements} môn`}</p>
        <div className="flex flex-wrap items-center gap-2">
          <Select aria-label="Số môn mỗi trang" value={query.size} data-testid="subjects-page-size"
            onChange={(event) => setQuery((current) => ({ ...current, size: Number(event.target.value), page: 0 }))}>
            {[20, 50, 100].map((size) => <option key={size} value={size}>{size} / trang</option>)}
          </Select>
          <Button size="sm" disabled={query.page === 0} onClick={() => setQuery((current) => ({ ...current, page: current.page - 1 }))} data-testid="subjects-page-prev">Trang trước</Button>
          <span>Trang {data.page + 1} / {Math.max(1, Math.ceil(data.totalElements / data.size))}</span>
          <Button size="sm" disabled={(query.page + 1) * query.size >= data.totalElements} onClick={() => setQuery((current) => ({ ...current, page: current.page + 1 }))} data-testid="subjects-page-next">Trang sau</Button>
          {data.items.length === 0 && query.page > 0 && <Button size="sm" onClick={() => setQuery((current) => ({ ...current, page: 0 }))}>Về trang đầu</Button>}
        </div>
      </div>
    </div>}
  </>;
}
