"use client";

import { useSearchParams } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import { ApiError } from "@/shared/api/client";
import { Alert, PageHeader } from "@/shared/ui";
import { AuditDetailDialog } from "./AuditDetailDialog";
import { AuditFilters } from "./AuditFilters";
import { AuditTable } from "./AuditTable";
import { findActorIdByEmail, getAuditLogs } from "./api";
import { defaultFilter, toQuery } from "./labels";
import type { AuditFilterForm, AuditLogItem, AuditLogPage as Page, AuditQuery } from "./types";

export const PAGE_SIZE = 50;
/** Chờ người dùng gõ xong email (hoặc chọn liền nhiều ô) rồi mới tra cứu. */
export const FILTER_DELAY_MS = 400;
const ACTOR_NOT_FOUND = "Không tìm thấy người thực hiện với email này.";
const EMAIL_FORMAT = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

function initialFilter(params: URLSearchParams | null): AuditFilterForm {
  return {
    ...defaultFilter(),
    objectType: params?.get("objectType") ?? "",
    objectId: params?.get("objectId") ?? "",
  };
}

function emptyPage(): Page {
  return { items: [], page: 0, size: PAGE_SIZE, totalElements: 0, totalPages: 0 };
}

/**
 * Màn #14 Audit Log (UC 13): chỉ đọc, mặc định 7 ngày gần nhất; mở từ Account Detail thì lọc sẵn đối tượng.
 * Đổi bộ lọc là tra cứu luôn (không có nút Lọc). Chỉ lần mở màn ghi một lần xem audit; lọc, xóa lọc, chuyển
 * trang gửi `recordView: false`.
 */
export function AuditLogPage() {
  const params = useSearchParams();
  const [form, setForm] = useState<AuditFilterForm>(() => initialFilter(params));
  const [data, setData] = useState<Page | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actorError, setActorError] = useState<string | null>(null);
  const [selected, setSelected] = useState<AuditLogItem | null>(null);
  const applied = useRef<AuditQuery | null>(null);
  const request = useRef(0);
  const opened = useRef(false);

  /** Kết quả của lần tra cứu {@code id}; lần cũ hơn lần mới nhất thì bỏ qua. */
  const show = (id: number, page: Page) => {
    if (id !== request.current) return;
    setError(null);
    setData(page);
    setLoading(false);
  };

  const fail = (id: number, e: unknown) => {
    if (id !== request.current) return;
    setData(null);
    setError(
      e instanceof ApiError && e.problem.status === 403
        ? "Bạn không có quyền xem Nhật ký audit."
        : "Không tải được nhật ký audit, thử lại sau.",
    );
    setLoading(false);
  };

  const load = (query: AuditQuery, id: number = ++request.current) => {
    applied.current = query;
    return getAuditLogs(query).then(
      (page) => show(id, page),
      (e: unknown) => fail(id, e),
    );
  };

  /** Mở màn: tra cứu bộ lọc mặc định (và đối tượng trên URL) một lần; đây là lần duy nhất được ghi audit. */
  useEffect(() => {
    const query = { ...toQuery(initialFilter(params), undefined, 0, PAGE_SIZE), recordView: true };
    const id = ++request.current;
    applied.current = query;
    getAuditLogs(query).then(
      (page) => show(id, page),
      (e: unknown) => fail(id, e),
    );
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  /** Đổi email người thực hiện thành actorId qua U01; không khớp thì báo lỗi dưới ô và trả trang rỗng. */
  const search = async (filter: AuditFilterForm) => {
    const email = filter.actorEmail.trim();
    if (email && !EMAIL_FORMAT.test(email)) return;
    if (filter.fromDate && filter.toDate && filter.fromDate > filter.toDate) return;
    setLoading(true);
    const id = ++request.current;
    const actorId = email ? await findActorIdByEmail(email) : undefined;
    if (id !== request.current) return;
    if (email && !actorId) {
      setActorError(ACTOR_NOT_FOUND);
      show(id, emptyPage());
      return;
    }
    setActorError(null);
    await load({ ...toQuery(filter, actorId ?? undefined, 0, PAGE_SIZE), recordView: false }, id);
  };

  /** Mỗi lần đổi bộ lọc (sau lần mở màn) thì tra cứu lại sau một nhịp ngắn. */
  useEffect(() => {
    if (!opened.current) {
      opened.current = true;
      return;
    }
    const timer = setTimeout(() => void search(form), FILTER_DELAY_MS);
    return () => clearTimeout(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [form]);

  const clear = () => {
    setActorError(null);
    setForm(defaultFilter());
  };

  const changePage = (page: number) => {
    if (!applied.current) return;
    setLoading(true);
    void load({ ...applied.current, page, recordView: false });
  };

  return (
    <>
      <PageHeader title="Nhật ký audit" />
      {/* Bộ lọc dính dưới thanh trên cùng khi cuộn bảng. */}
      <div className="sticky top-16 z-10">
        <AuditFilters value={form} onChange={setForm} onClear={clear} actorError={actorError} />
      </div>
      {error && (
        <Alert tone="danger" role="alert">
          {error}
        </Alert>
      )}
      {loading && !data && (
        <p className="text-[13px] text-muted" data-testid="audit-loading">
          Đang tải…
        </p>
      )}
      {data && (
        <div className={loading ? "opacity-60" : undefined} aria-busy={loading}>
          <AuditTable data={data} onOpen={setSelected} onPageChange={changePage} />
        </div>
      )}
      <AuditDetailDialog item={selected} onClose={() => setSelected(null)} />
    </>
  );
}
