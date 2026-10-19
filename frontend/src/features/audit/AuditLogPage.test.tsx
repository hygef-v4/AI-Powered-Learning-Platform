import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { AuditLogPage } from "./AuditLogPage";
import { dayEnd, dayStart, defaultFilter } from "./labels";
import type { AuditLogItem, AuditLogPage as Page } from "./types";

let search = new URLSearchParams();
vi.mock("next/navigation", () => ({ useSearchParams: () => search }));

const ADMIN_ROW: AuditLogItem = {
  id: "7f21c9e4-0b8d-4a51-a2c3-118e5f0d9b77",
  occurredAt: "2026-10-14T10:55:12Z",
  actorId: "11111111-2222-3333-4444-555555555555",
  actorEmail: "phongdt@fpt.edu.vn",
  action: "ROLE_CHANGED",
  objectType: "ACCOUNT",
  objectId: "3f9c2a7d-11e0-4b9b-8f3a-6c2d0a5e1e81",
  result: "SUCCESS",
  reason: null,
  details: { before: { role: "STUDENT" }, after: { role: "TEACHER" }, actorType: "ADMIN" },
};

const SYSTEM_ROW: AuditLogItem = {
  ...ADMIN_ROW,
  id: "8f21c9e4-0b8d-4a51-a2c3-118e5f0d9b78",
  actorId: null,
  actorEmail: null,
  action: "BACKUP_FAILED",
  objectType: "BACKUP",
  objectId: "bk-20261011",
  result: "FAILURE",
  reason: "Hết thời gian kết nối Google Drive",
  details: {},
};

function page(items: AuditLogItem[], totalElements = items.length): Page {
  return { items, page: 0, size: 50, totalElements, totalPages: Math.ceil(totalElements / 50) };
}

function json(body: unknown, status = 200) {
  return Promise.resolve(new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } }));
}

const auditCalls = (fetchMock: ReturnType<typeof vi.fn>) =>
  fetchMock.mock.calls.map(([url]) => String(url)).filter((url) => url.startsWith("/api/v1/admin/audit-logs"));

describe("AuditLogPage", () => {
  beforeEach(() => {
    search = new URLSearchParams();
  });
  afterEach(() => vi.unstubAllGlobals());

  it("mở màn tra cứu 7 ngày gần nhất, 50 dòng mỗi trang", async () => {
    const fetchMock = vi.fn(() => json(page([ADMIN_ROW])));
    vi.stubGlobal("fetch", fetchMock);
    render(<AuditLogPage />);

    expect(await screen.findByText("phongdt@fpt.edu.vn")).toBeInTheDocument();
    const url = new URL(auditCalls(fetchMock)[0]!, "http://x");
    const filter = defaultFilter();
    expect(url.searchParams.get("from")).toBe(dayStart(filter.fromDate));
    expect(url.searchParams.get("to")).toBe(dayEnd(filter.toDate));
    expect(url.searchParams.get("size")).toBe("50");
    expect(url.searchParams.get("page")).toBe("0");
    expect(url.searchParams.has("actorId")).toBe(false);
  });

  it("việc do hệ thống làm hiện Hệ thống, kết quả hiện nhãn tiếng Việt", async () => {
    vi.stubGlobal("fetch", vi.fn(() => json(page([SYSTEM_ROW]))));
    render(<AuditLogPage />);

    const row = await screen.findByTestId(`audit-table-row-${SYSTEM_ROW.id}`);
    expect(within(row).getByText("Hệ thống")).toBeInTheDocument();
    expect(within(row).getByText("Thất bại")).toBeInTheDocument();
    expect(within(row).getByText("Hết thời gian kết nối Google Drive")).toBeInTheDocument();
  });

  it("lọc sẵn đối tượng khi mở từ Account Detail", async () => {
    search = new URLSearchParams({ objectType: "ACCOUNT", objectId: "abc" });
    const fetchMock = vi.fn(() => json(page([])));
    vi.stubGlobal("fetch", fetchMock);
    render(<AuditLogPage />);

    expect(await screen.findByTestId("audit-table-empty")).toHaveTextContent("Không có bản ghi nào khớp bộ lọc.");
    const url = new URL(auditCalls(fetchMock)[0]!, "http://x");
    expect(url.searchParams.get("objectType")).toBe("ACCOUNT");
    expect(url.searchParams.get("objectId")).toBe("abc");
    expect(screen.getByTestId("audit-filters-object-id-input")).toHaveValue("abc");
  });

  it("email không khớp tài khoản nào thì báo lỗi dưới ô và không tra cứu", async () => {
    const fetchMock = vi.fn((url: string) =>
      url.startsWith("/api/v1/admin/accounts") ? json({ items: [{ id: "x", email: "khac@fpt.edu.vn" }] }) : json(page([])),
    );
    vi.stubGlobal("fetch", fetchMock);
    render(<AuditLogPage />);
    await screen.findByTestId("audit-table-empty");

    fireEvent.change(screen.getByTestId("audit-filters-actor-input"), { target: { value: "nguyenx@fpt.edu.vn" } });
    fireEvent.click(screen.getByTestId("audit-filters-submit-button"));

    expect(await screen.findByText("Không tìm thấy người thực hiện với email này.")).toBeInTheDocument();
    expect(auditCalls(fetchMock)).toHaveLength(1);
  });

  it("email khớp thì gửi actorId của tài khoản đó", async () => {
    const fetchMock = vi.fn((url: string) =>
      url.startsWith("/api/v1/admin/accounts")
        ? json({ items: [{ id: ADMIN_ROW.actorId, email: "PhongDT@fpt.edu.vn" }] })
        : json(page([ADMIN_ROW])),
    );
    vi.stubGlobal("fetch", fetchMock);
    render(<AuditLogPage />);
    await screen.findByText("phongdt@fpt.edu.vn");

    fireEvent.change(screen.getByTestId("audit-filters-actor-input"), { target: { value: " phongdt@fpt.edu.vn " } });
    fireEvent.click(screen.getByTestId("audit-filters-submit-button"));

    await waitFor(() => expect(auditCalls(fetchMock)).toHaveLength(2));
    expect(new URL(auditCalls(fetchMock)[1]!, "http://x").searchParams.get("actorId")).toBe(ADMIN_ROW.actorId);
  });

  it("bấm dòng mở chi tiết chỉ đọc kèm JSON, nút Đóng đóng popup", async () => {
    vi.stubGlobal("fetch", vi.fn(() => json(page([ADMIN_ROW]))));
    render(<AuditLogPage />);

    fireEvent.click(await screen.findByTestId(`audit-table-row-${ADMIN_ROW.id}`));
    const dialog = screen.getByRole("dialog", { name: "Chi tiết bản ghi" });
    expect(within(dialog).getByText(ADMIN_ROW.id)).toBeInTheDocument();
    expect(within(dialog).getByText("Bản ghi không sửa hoặc xóa được")).toBeInTheDocument();
    expect(within(dialog).getByTestId("audit-detail-json")).toHaveTextContent('"role": "TEACHER"');
    expect(within(dialog).queryByRole("button", { name: /Sửa|Xóa/ })).not.toBeInTheDocument();

    fireEvent.click(within(dialog).getByTestId("audit-detail-close-button"));
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  });

  it("không có quyền thì báo 403, lỗi khác báo tải lại", async () => {
    vi.stubGlobal("fetch", vi.fn(() => json({ status: 403, code: "AUDIT_FORBIDDEN" }, 403)));
    const { unmount } = render(<AuditLogPage />);
    expect(await screen.findByRole("alert")).toHaveTextContent("Bạn không có quyền xem Nhật ký audit.");
    unmount();

    vi.stubGlobal("fetch", vi.fn(() => json({ status: 500 }, 500)));
    render(<AuditLogPage />);
    expect(await screen.findByRole("alert")).toHaveTextContent("Không tải được nhật ký audit, thử lại sau.");
  });

  it("chuyển trang giữ bộ lọc đã áp dụng", async () => {
    const fetchMock = vi.fn(() => json(page([ADMIN_ROW], 120)));
    vi.stubGlobal("fetch", fetchMock);
    render(<AuditLogPage />);
    await screen.findByText("phongdt@fpt.edu.vn");

    fireEvent.click(screen.getByRole("button", { name: "Trang sau" }));

    await waitFor(() => expect(auditCalls(fetchMock)).toHaveLength(2));
    const first = new URL(auditCalls(fetchMock)[0]!, "http://x").searchParams;
    const second = new URL(auditCalls(fetchMock)[1]!, "http://x").searchParams;
    expect(second.get("page")).toBe("1");
    expect(second.get("from")).toBe(first.get("from"));
  });
});
