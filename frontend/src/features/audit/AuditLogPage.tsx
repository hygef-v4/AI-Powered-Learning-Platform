"use client";

import { useSearchParams } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import { cx } from "@/shared/ui/cx";
import { ApiError } from "@/shared/api/client";
import { Alert, PageHeader } from "@/shared/ui";
import { AuditFilters } from "./AuditFilters";
import { AuditTable } from "./AuditTable";
import { getAuditLogs } from "./api";
import { defaultFilter, toQuery } from "./labels";
import type { AuditFilterForm, AuditLogPage as Page, AuditQuery } from "./types";

export const PAGE_SIZE = 50;
/** Gõ hay chọn liền nhiều ô thì gom lại một lần tra cứu. */
export const FILTER_DELAY_MS = 300;

function initialFilter(params: URLSearchParams | null): AuditFilterForm {
  return {
    ...defaultFilter(),
    objectType: params?.get("objectType") ?? "",
    objectId: params?.get("objectId") ?? "",
    actorEmail: params?.get("actorEmail") ?? "",
  };
}

/**
 * Màn #14 Audit Log (UC 13): chỉ đọc, mặc định 7 ngày gần nhất; mở từ Account Detail thì điền sẵn Người thực hiện là
 * email của tài khoản đó.
 * Đổi bộ lọc là tra cứu luôn (không có nút Lọc). Chỉ lần mở màn ghi một lần xem audit; lọc, xóa lọc, chuyển
 * trang gửi `recordView: false`.
 */
export function AuditLogPage() {
  const params = useSearchParams();
  const [form, setForm] = useState<AuditFilterForm>(() => initialFilter(params));
  const [data, setData] = useState<Page | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const applied = useRef<AuditQuery | null>(null);
  const request = useRef(0);
  const opened = useRef(false);
  const sentinel = useRef<HTMLDivElement>(null);
  const [stuck, setStuck] = useState(false);

  /** Card lọc đã dính dưới thanh trên cùng chưa (mốc nằm ngay trên card trượt khỏi màn). */
  useEffect(() => {
    const node = sentinel.current;
    if (!node || typeof IntersectionObserver === "undefined") return;
    const observer = new IntersectionObserver(([entry]) => setStuck(entry ? !entry.isIntersecting : false), { rootMargin: "-57px 0px 0px 0px" });
    observer.observe(node);
    return () => observer.disconnect();
  }, []);

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
    // Mở từ Account Detail: ô Người thực hiện điền sẵn email, kèm actorId nên không phải tra lại.
    const query = { ...toQuery(initialFilter(params), params?.get("actorId") ?? undefined, 0, PAGE_SIZE), recordView: true };
    const id = ++request.current;
    applied.current = query;
    getAuditLogs(query).then(
      (page) => show(id, page),
      (e: unknown) => fail(id, e),
    );
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  /** Tra cứu theo bộ lọc đang có; người thực hiện gửi đúng phần đã gõ (email hay tên), để trống là mọi người. */
  const search = (filter: AuditFilterForm) => {
    if (filter.fromDate && filter.toDate && filter.fromDate > filter.toDate) return;
    setLoading(true);
    void load({ ...toQuery(filter, undefined, 0, PAGE_SIZE), recordView: false });
  };

  /** Mỗi lần đổi bộ lọc (sau lần mở màn) thì tra cứu lại sau một nhịp ngắn. */
  useEffect(() => {
    if (!opened.current) {
      opened.current = true;
      return;
    }
    const timer = setTimeout(() => search(form), FILTER_DELAY_MS);
    return () => clearTimeout(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [form]);

  const clear = () => setForm(defaultFilter());

  const changePage = (page: number) => {
    if (!applied.current) return;
    setLoading(true);
    void load({ ...applied.current, page, recordView: false });
  };

  return (
    <>
      <PageHeader title="Nhật ký audit" />
      {/* Bộ lọc dính dưới thanh trên cùng khi cuộn; khi dính thì phủ nền cả khoảng trên card để dòng bảng không lộ ra. */}
      <div ref={sentinel} aria-hidden="true" className="-mb-7 h-px" />
      <div
        className={cx(
          "sticky top-14 z-10 bg-shell pt-2",
          stuck && "before:absolute before:inset-x-0 before:bottom-full before:h-14 before:bg-shell",
        )}
        data-stuck={stuck || undefined}
      >
        <AuditFilters value={form} onChange={setForm} onClear={clear} />
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
          <AuditTable data={data} onPageChange={changePage} />
        </div>
      )}
    </>
  );
}
