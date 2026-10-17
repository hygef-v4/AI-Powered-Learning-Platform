// Kiểu dữ liệu của API GET /api/v1/admin/audit-logs (U02, UC 13).

export type AuditResult = "SUCCESS" | "DENIED" | "FAILURE";

export type AuditLogItem = {
  id: string;
  occurredAt: string;
  /** Rỗng là việc do hệ thống hoặc worker làm. */
  actorId: string | null;
  actorEmail: string | null;
  action: string;
  objectType: string | null;
  objectId: string | null;
  result: AuditResult;
  reason: string | null;
  details: Record<string, unknown>;
};

export type AuditLogPage = {
  items: AuditLogItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type AuditQuery = {
  actorId?: string;
  action?: string;
  objectType?: string;
  objectId?: string;
  result?: AuditResult;
  /** ISO-8601. */
  from?: string;
  to?: string;
  page: number;
  size: number;
};

/** Giá trị trong form bộ lọc; ngày dạng yyyy-mm-dd theo giờ Việt Nam. */
export type AuditFilterForm = {
  actorEmail: string;
  action: string;
  objectType: string;
  objectId: string;
  result: AuditResult | "";
  fromDate: string;
  toDate: string;
};
