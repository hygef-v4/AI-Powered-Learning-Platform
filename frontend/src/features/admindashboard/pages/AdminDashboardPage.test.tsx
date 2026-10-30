import {act, fireEvent, render, screen, waitFor, within} from "@testing-library/react";
import {afterEach, expect, it, vi} from "vitest";
import {AdminDashboardPage} from "./AdminDashboardPage";
import {activityFixture, available, dashboardFixture, routes, unavailable} from "../model/testFixtures";
import {REFRESH_MS} from "../hooks/useLiveOverview";

afterEach(() => { vi.unstubAllGlobals(); vi.useRealTimers(); });

it("bốn ô số: tài khoản hoạt động kèm tăng giảm hôm nay, môn, lớp có nháp và lưu trữ, token AI trên hạn mức", async () => {
  vi.stubGlobal("fetch", routes());
  render(<AdminDashboardPage/>);
  expect(screen.getByRole("status")).toHaveTextContent("Đang tải");
  const accounts = await screen.findByTestId("dashboard-accounts");
  expect(accounts).toHaveTextContent("3");
  expect(within(accounts).getByText("−2 hôm nay")).toHaveClass("text-danger");
  expect(screen.getByTestId("dashboard-subjects")).toHaveTextContent("4");
  expect(within(screen.getByTestId("dashboard-classes")).getByText("1 nháp")).toHaveClass("text-warning");
  expect(within(screen.getByTestId("dashboard-classes")).getByText("1 lưu trữ")).toHaveClass("text-danger");
  expect(screen.getByTestId("dashboard-tokens")).toHaveTextContent("182.000");
  expect(screen.getByTestId("dashboard-tokens")).toHaveTextContent("/ 500.000");
  expect(screen.getByTestId("dashboard-live")).toHaveTextContent("Trực tiếp");
  expect(screen.queryByRole("button", {name: /Tải lại|7 ngày|30 ngày/})).not.toBeInTheDocument();
});

it("không đổi trong hôm nay thì chữ màu thường; nguồn lỗi chỉ làm thẻ của nó không khả dụng", async () => {
  const activity = {...activityFixture(), activeAccountChangeToday: available(0), assignmentsByDay: unavailable()};
  const data = dashboardFixture();
  data.subjects = unavailable();
  vi.stubGlobal("fetch", routes({dashboard: data, activity, usage: Response.json({}, {status: 503})}));
  render(<AdminDashboardPage/>);
  const change = await screen.findByText("Không đổi hôm nay");
  expect(change).not.toHaveClass("text-danger");
  expect(change).not.toHaveClass("text-success");
  expect(screen.getByTestId("dashboard-subjects")).toHaveTextContent("Không khả dụng");
  expect(screen.getByTestId("dashboard-tokens")).toHaveTextContent("Không khả dụng");
  expect(screen.getByTestId("dashboard-assignments")).toHaveTextContent("Không khả dụng");
  expect(screen.getByTestId("dashboard-security")).toHaveTextContent("gv03@fpt.edu.vn");
});

it("bấm vào biểu đồ thì hiện số, bấm ra ngoài thì ẩn", async () => {
  vi.stubGlobal("fetch", routes());
  render(<AdminDashboardPage/>);
  const card = await screen.findByTestId("dashboard-assignments");
  const chart = within(card).getByRole("button", {name: /Số bài tập được giao/});
  // Ngày cuối có 3 cá nhân + 2 nhóm: tổng 5 chỉ hiện sau khi bấm.
  expect(within(chart).queryByText("5")).not.toBeInTheDocument();
  fireEvent.click(chart);
  expect(within(chart).getByText("5")).toBeInTheDocument();
  expect(chart).toHaveAttribute("aria-pressed", "true");
  fireEvent.pointerDown(document.body);
  expect(within(chart).queryByText("5")).not.toBeInTheDocument();

  const roles = within(screen.getByTestId("dashboard-roles")).getByRole("button");
  fireEvent.click(roles);
  expect(within(roles).getByText("3")).toBeInTheDocument();
  // Chú thích không ghi số.
  expect(within(screen.getByTestId("dashboard-roles")).getByText("Sinh viên").textContent).toBe("Sinh viên");
});

it("lịch sao lưu 30 ngày, mỗi ô một ngày theo kết quả", async () => {
  vi.useFakeTimers({toFake: ["Date"]});
  vi.setSystemTime(new Date("2026-10-20T03:00:00Z"));
  vi.stubGlobal("fetch", routes({backups: {items: [
    {scheduledFor: "2026-10-20", state: "SUCCESS", snapshotAt: null, startedAt: null, completedAt: null, byteSize: 1, safeError: null, purgedAt: null},
    {scheduledFor: "2026-10-19", state: "FAILED", snapshotAt: null, startedAt: null, completedAt: null, byteSize: null, safeError: "x", purgedAt: null},
  ], page: 0, size: 40, totalElements: 2, totalPages: 1}}));
  render(<AdminDashboardPage/>);
  const calendar = await screen.findByRole("list", {name: "Kết quả sao lưu theo ngày"});
  expect(within(calendar).getAllByRole("listitem")).toHaveLength(30);
  expect(within(calendar).getByLabelText("20/10: Thành công")).toBeInTheDocument();
  expect(within(calendar).getByLabelText("19/10: Thất bại")).toBeInTheDocument();
  expect(within(calendar).getByLabelText("18/10: Không chạy")).toBeInTheDocument();
});

it("hoạt động đáng ngờ: sự việc dễ đọc, tài khoản không rõ, lý do gọn", async () => {
  vi.stubGlobal("fetch", routes());
  render(<AdminDashboardPage/>);
  const card = await screen.findByTestId("dashboard-security");
  expect(within(card).getByText("Tài khoản bị khóa tạm")).toBeInTheDocument();
  expect(within(card).getByText("Thử quá nhiều")).toHaveAttribute("title", "TOO_MANY_FAILURES");
  expect(within(card).getByText("Không rõ")).toBeInTheDocument();
  expect(within(card).getByText("Sai email")).toBeInTheDocument();
  expect(within(card).getByText(/^Bị từ chối:/)).toBeInTheDocument();
  expect(within(card).getByRole("link", {name: "Mở Nhật ký"})).toHaveAttribute("href", "/admin/audit");
});

it("tự cập nhật định kỳ; lần cập nhật lỗi thì giữ số cũ và báo mất kết nối", async () => {
  vi.useFakeTimers({shouldAdvanceTime: true});
  let failing = false;
  const ok = routes();
  const request = vi.fn((url: string) => failing ? Promise.resolve(Response.json({}, {status: 503})) : ok(url));
  vi.stubGlobal("fetch", request);
  render(<AdminDashboardPage/>);
  await screen.findByTestId("dashboard-accounts");
  const first = request.mock.calls.length;
  failing = true;
  await act(async () => { await vi.advanceTimersByTimeAsync(REFRESH_MS); });
  await waitFor(() => expect(screen.getByTestId("dashboard-live")).toHaveTextContent("Mất kết nối"));
  expect(request.mock.calls.length).toBeGreaterThan(first);
  expect(screen.getByTestId("dashboard-accounts")).toHaveTextContent("3");
  failing = false;
  await act(async () => { await vi.advanceTimersByTimeAsync(REFRESH_MS); });
  await waitFor(() => expect(screen.getByTestId("dashboard-live")).toHaveTextContent("Trực tiếp"));
});

it("bị từ chối quyền thì xóa số và không lộ chi tiết lỗi", async () => {
  vi.stubGlobal("fetch", routes({dashboard: Response.json({detail: "secret SQL"}, {status: 403})}));
  render(<AdminDashboardPage/>);
  expect(await screen.findByRole("alert")).toHaveTextContent("Không có quyền");
  expect(screen.queryByTestId("dashboard-accounts")).not.toBeInTheDocument();
  expect(screen.queryByText("secret SQL")).not.toBeInTheDocument();
});

it("lần đầu lỗi thì có Thử lại; tải lại sau khi gắn lại thay vì dùng số cũ", async () => {
  let failing = true;
  const ok = routes();
  vi.stubGlobal("fetch", vi.fn((url: string) => failing ? Promise.resolve(Response.json({}, {status: 503})) : ok(url)));
  const view = render(<AdminDashboardPage/>);
  expect(await screen.findByRole("alert")).toHaveTextContent("Không tải được");
  failing = false;
  fireEvent.click(screen.getByRole("button", {name: "Tải lại trang"}));
  await screen.findByTestId("dashboard-accounts");
  view.unmount();
  const before = ok.mock.calls.length;
  render(<AdminDashboardPage/>);
  await screen.findByTestId("dashboard-accounts");
  expect(ok.mock.calls.length).toBeGreaterThan(before);
});

it("hủy yêu cầu đang chờ khi rời trang", async () => {
  const signals: AbortSignal[] = [];
  vi.stubGlobal("fetch", vi.fn((_url: string, init: RequestInit) => {
    if (init?.signal) signals.push(init.signal as AbortSignal);
    return new Promise<Response>(() => {});
  }));
  const view = render(<AdminDashboardPage/>);
  await waitFor(() => expect(signals.length).toBeGreaterThan(0));
  view.unmount();
  expect(signals.every((signal) => signal.aborted)).toBe(true);
});
