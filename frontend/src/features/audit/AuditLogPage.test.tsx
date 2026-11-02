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
    expect(url.searchParams.get("recordView")).toBe("true");
  });

  it("bảng có cột STT, thời gian ghi giờ phút ngày tháng năm, không còn dòng Hiển thị", async () => {
    vi.stubGlobal("fetch", vi.fn(() => json(page([ADMIN_ROW, SYSTEM_ROW]))));
    render(<AuditLogPage />);

    const row = await screen.findByTestId(`audit-table-row-${ADMIN_ROW.id}`);
    expect(screen.getByRole("columnheader", { name: "STT" })).toBeInTheDocument();
    expect(within(row).getByText("1")).toBeInTheDocument();
    expect(within(screen.getByTestId(`audit-table-row-${SYSTEM_ROW.id}`)).getByText("2")).toBeInTheDocument();
    expect(within(row).getByText("17:55 14/10/2026")).toBeInTheDocument();
    expect(within(row).queryByText(/3f9c2a/)).not.toBeInTheDocument();
    expect(screen.queryByText(/Hiển thị|dòng mỗi trang/)).not.toBeInTheDocument();
    expect(screen.queryByText("Người thực hiện (email)")).not.toBeInTheDocument();
    expect(screen.queryByTestId("audit-filters-object-id-input")).not.toBeInTheDocument();
    expect(screen.queryByTestId("audit-filters-submit-button")).not.toBeInTheDocument();
  });

  it("đổi bộ lọc là tra cứu luôn và không ghi thêm lần xem; xóa lọc cũng vậy", async () => {
    const fetchMock = vi.fn(() => json(page([ADMIN_ROW])));
    vi.stubGlobal("fetch", fetchMock);
    render(<AuditLogPage />);
    await screen.findByText("phongdt@fpt.edu.vn");

    fireEvent.change(screen.getByTestId("audit-filters-result-select"), { target: { value: "DENIED" } });
    await waitFor(() => expect(auditCalls(fetchMock)).toHaveLength(2));
    const filtered = new URL(auditCalls(fetchMock)[1]!, "http://x").searchParams;
    expect(filtered.get("result")).toBe("DENIED");
    expect(filtered.get("recordView")).toBe("false");

    fireEvent.click(screen.getByTestId("audit-filters-clear-button"));
    await waitFor(() => expect(auditCalls(fetchMock)).toHaveLength(3));
    const cleared = new URL(auditCalls(fetchMock)[2]!, "http://x").searchParams;
    expect(cleared.has("result")).toBe(false);
    expect(cleared.get("recordView")).toBe("false");
  });

  it("cột đối tượng hiện email hoặc tên thay cho loại đối tượng; không có tên thì hiện loại", async () => {
    const onAccount = { ...ADMIN_ROW, objectLabel: "sv01@fpt.edu.vn" };
    const onSubject = { ...ADMIN_ROW, id: "9f21c9e4-0b8d-4a51-a2c3-118e5f0d9b79", objectType: "SUBJECT", objectLabel: "SWD392 · Kiến trúc phần mềm" };
    vi.stubGlobal("fetch", vi.fn(() => json(page([onAccount, onSubject, SYSTEM_ROW]))));
    render(<AuditLogPage />);

    expect(within(await screen.findByTestId(`audit-table-row-${onAccount.id}`)).getByText("sv01@fpt.edu.vn")).toBeInTheDocument();
    expect(within(screen.getByTestId(`audit-table-row-${onSubject.id}`)).getByText("SWD392 · Kiến trúc phần mềm")).toBeInTheDocument();
    expect(within(screen.getByTestId(`audit-table-row-${SYSTEM_ROW.id}`)).getByText("Bản sao lưu")).toBeInTheDocument();
    expect(within(screen.getByTestId(`audit-table-row-${onAccount.id}`)).queryByText("Tài khoản")).not.toBeInTheDocument();
  });

  it("việc do hệ thống làm hiện Hệ thống, kết quả hiện nhãn tiếng Việt", async () => {
    vi.stubGlobal("fetch", vi.fn(() => json(page([SYSTEM_ROW]))));
    render(<AuditLogPage />);

    const row = await screen.findByTestId(`audit-table-row-${SYSTEM_ROW.id}`);
    expect(within(row).getByText("Hệ thống")).toBeInTheDocument();
    expect(within(row).getByText("Thất bại")).toBeInTheDocument();
    // Lý do ghi gọn 2–3 chữ, đủ câu khi rê chuột.
    expect(within(row).getByText("Quá giờ")).toHaveAttribute("title", "Hết thời gian kết nối Google Drive");
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
    expect(screen.getByTestId("audit-filters-object-type-select")).toHaveValue("ACCOUNT");
  });

  it("gõ dở email là lọc ngay theo phần đã gõ, xóa trống thì hiện lại mọi người", async () => {
    const fetchMock = vi.fn(() => json(page([ADMIN_ROW])));
    vi.stubGlobal("fetch", fetchMock);
    render(<AuditLogPage />);
    await screen.findByText("phongdt@fpt.edu.vn");

    fireEvent.change(screen.getByTestId("audit-filters-actor-input"), { target: { value: " phong" } });
    await waitFor(() => expect(auditCalls(fetchMock)).toHaveLength(2));
    const typed = new URL(auditCalls(fetchMock)[1]!, "http://x").searchParams;
    expect(typed.get("actor")).toBe("phong");
    expect(typed.has("actorId")).toBe(false);
    expect((fetchMock.mock.calls as unknown[][]).some(([url]) => String(url).startsWith("/api/v1/admin/accounts"))).toBe(false);

    fireEvent.change(screen.getByTestId("audit-filters-actor-input"), { target: { value: "" } });
    await waitFor(() => expect(auditCalls(fetchMock)).toHaveLength(3));
    expect(new URL(auditCalls(fetchMock)[2]!, "http://x").searchParams.has("actor")).toBe(false);
  });

  it("xóa trống cả ngày thì tra cứu mọi thời điểm thay vì không hiện gì", async () => {
    const fetchMock = vi.fn(() => json(page([ADMIN_ROW])));
    vi.stubGlobal("fetch", fetchMock);
    render(<AuditLogPage />);
    await screen.findByText("phongdt@fpt.edu.vn");

    fireEvent.change(screen.getByTestId("audit-filters-from-input"), { target: { value: "" } });
    fireEvent.change(screen.getByTestId("audit-filters-to-input"), { target: { value: "" } });
    await waitFor(() => expect(auditCalls(fetchMock)).toHaveLength(2));
    const all = new URL(auditCalls(fetchMock)[1]!, "http://x").searchParams;
    expect(all.has("from")).toBe(false);
    expect(all.has("to")).toBe(false);
  });

  it("bảng chỉ xem: bấm dòng không mở popup chi tiết", async () => {
    vi.stubGlobal("fetch", vi.fn(() => json(page([ADMIN_ROW]))));
    render(<AuditLogPage />);

    fireEvent.click(await screen.findByTestId(`audit-table-row-${ADMIN_ROW.id}`));
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  });

  it("không có quyền thì báo 403, lỗi khác báo tải lại", async () => {
    vi.stubGlobal("fetch", vi.fn(() => json({ status: 403, code: "AUDIT_FORBIDDEN" }, 403)));
    const { unmount } = render(<AuditLogPage />);
    expect(await screen.findByRole("alert")).toHaveTextContent("Bạn không có quyền xem Nhật ký.");
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
    expect(second.get("recordView")).toBe("false");
  });
});
