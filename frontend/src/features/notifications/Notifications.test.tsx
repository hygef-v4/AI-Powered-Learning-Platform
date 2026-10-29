import { act, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { EmailPreferencePanel } from "./EmailPreferencePanel";
import { NotificationPopup } from "./NotificationPopup";
import { NotificationBell } from "./NotificationBell";
import { notificationsApi, safeNotificationLink } from "./api";
vi.mock("./api", async original => ({ ...await original<typeof import("./api")>(), notificationsApi: { count: vi.fn(), list: vi.fn(), read: vi.fn(), readAll: vi.fn(), preferences: vi.fn(), savePreferences: vi.fn() } }));
const push = vi.fn(); vi.mock("next/navigation", () => ({ useRouter: () => ({ push }) }));
vi.mock("@/features/identity/session", () => ({ useSession: () => ({ status: "authenticated", profile: { accountId: "a", role: "STUDENT" } }) }));
const n = { id: "n", type: "GRADE_PUBLISHED", title: "Bạn có điểm mới", summary: "Đăng nhập để xem", link: "/classes/1", readAt: null, createdAt: "2026-10-21T08:00:00Z" };
beforeEach(() => { vi.clearAllMocks(); vi.mocked(notificationsApi.list).mockResolvedValue({ items: [n], page: 1, size: 20, hasMore: false }); vi.mocked(notificationsApi.preferences).mockResolvedValue({ ENROLLED: true, ASSIGNMENT_OPENED: true, DEADLINE_REMINDER: true, GRADE_PUBLISHED: true, ANNOUNCEMENT_IMPORTANT: true }); });
afterEach(() => { vi.unstubAllGlobals(); vi.useRealTimers(); });
it("rejects external, protocol-relative and escaped notification links", () => { expect(safeNotificationLink("/classes/1")).toBe("/classes/1"); for (const link of ["https://evil.test", "//evil.test", "/\\evil.test", "/\n/evil.test"]) expect(safeNotificationLink(link)).toBeNull(); });
it("preference update only appears after API success", async () => { vi.mocked(notificationsApi.savePreferences).mockRejectedValue(new Error("down")); render(<EmailPreferencePanel />); const toggle = await screen.findByRole("switch", { name: "Ghi danh vào lớp" }); expect(toggle).toHaveAttribute("aria-checked", "true"); fireEvent.click(toggle); expect(await screen.findByRole("alert")).toHaveTextContent("Chưa lưu được"); expect(toggle).toHaveAttribute("aria-checked", "true"); });
it("shows all five learning preference types including important announcement", async () => { render(<EmailPreferencePanel />); expect(await screen.findByRole("switch", { name: "Thông báo quan trọng của lớp" })).toBeInTheDocument(); expect(screen.getAllByRole("switch")).toHaveLength(5); });
it("notification opens only after read succeeds", async () => { vi.mocked(notificationsApi.read).mockResolvedValue(undefined); render(<NotificationPopup count={1} student={false} revision={0} onChanged={vi.fn()} onClose={vi.fn()} />); fireEvent.click(await screen.findByTestId("notification-item")); await waitFor(() => expect(push).toHaveBeenCalledWith("/classes/1")); expect(notificationsApi.read).toHaveBeenCalledWith("n"); expect(screen.queryByRole("switch")).not.toBeInTheDocument(); });
it("failed read does not navigate", async () => { vi.mocked(notificationsApi.read).mockRejectedValue(new Error("down")); render(<NotificationPopup count={1} student={false} revision={0} onChanged={vi.fn()} onClose={vi.fn()} />); fireEvent.click(await screen.findByTestId("notification-item")); expect(await screen.findByRole("alert")).toHaveTextContent("Chưa đánh dấu"); expect(push).not.toHaveBeenCalled(); });
it("read-all is an explicit mutation and unread tab requests unread filter", async () => { vi.mocked(notificationsApi.readAll).mockResolvedValue(undefined); const changed = vi.fn(); render(<NotificationPopup count={2} student={false} revision={0} onChanged={changed} onClose={vi.fn()} />); await screen.findByTestId("notification-item"); fireEvent.click(screen.getByTestId("notifications-read-all")); await waitFor(() => expect(changed).toHaveBeenCalled()); fireEvent.click(screen.getByRole("tab", { name: "Chưa đọc (2)" })); await waitFor(() => expect(notificationsApi.list).toHaveBeenLastCalledWith(true, 1, expect.any(AbortSignal))); });
it("student opens email settings from the gear as its own view and comes back to the list", async () => { render(<NotificationPopup count={1} student revision={0} onChanged={vi.fn()} onClose={vi.fn()} />); await screen.findByTestId("notification-item"); expect(screen.queryByRole("switch")).not.toBeInTheDocument(); fireEvent.click(screen.getByRole("button", { name: "Cài đặt thông báo" })); expect(await screen.findByRole("switch", { name: "Ghi danh vào lớp" })).toBeInTheDocument(); expect(screen.getByRole("heading", { name: "Cài đặt" })).toBeInTheDocument(); expect(screen.queryByRole("tab")).not.toBeInTheDocument(); expect(screen.getByTestId("notifications-read-all")).not.toBeVisible(); fireEvent.click(screen.getByRole("button", { name: "Quay lại thông báo" })); expect(screen.getByRole("heading", { name: "Thông báo" })).toBeInTheDocument(); expect(screen.getByTestId("notification-item")).toBeInTheDocument(); });
it("non-students have no settings gear", async () => { render(<NotificationPopup count={1} student={false} revision={0} onChanged={vi.fn()} onClose={vi.fn()} />); await screen.findByTestId("notification-item"); expect(screen.queryByRole("button", { name: "Cài đặt thông báo" })).not.toBeInTheDocument(); });
it("refresh keeps current rows on screen until the new list arrives", async () => { const props = { count: 1, student: false, revision: 0, onChanged: vi.fn(), onClose: vi.fn() }; const { rerender } = render(<NotificationPopup {...props} />); await screen.findByTestId("notification-item"); let resolve!: (value: Awaited<ReturnType<typeof notificationsApi.list>>) => void; vi.mocked(notificationsApi.list).mockReturnValueOnce(new Promise(r => { resolve = r; })); rerender(<NotificationPopup {...props} revision={1} />); expect(screen.getByTestId("notification-item")).toBeInTheDocument(); expect(screen.queryByText("Đang tải…")).not.toBeInTheDocument(); await act(async () => { resolve({ items: [{ ...n, readAt: "2026-10-21T09:00:00Z" }], page: 1, size: 20, hasMore: false }); }); expect(screen.getByTestId("notification-item")).not.toHaveClass("unread"); });
it("escape closes popup", () => { const close = vi.fn(); render(<NotificationPopup count={0} student={false} revision={0} onChanged={vi.fn()} onClose={close} />); fireEvent.keyDown(screen.getByRole("dialog"), { key: "Escape" }); expect(close).toHaveBeenCalled(); });
it("refresh after loading more restarts at newest page", async () => { vi.mocked(notificationsApi.list).mockResolvedValue({ items: [n], page: 1, size: 20, hasMore: true }); const props = { count: 1, student: false, revision: 0, onChanged: vi.fn(), onClose: vi.fn() }; const { rerender } = render(<NotificationPopup {...props} />); fireEvent.click(await screen.findByText("Tải thêm")); await waitFor(() => expect(notificationsApi.list).toHaveBeenLastCalledWith(false, 2, expect.any(AbortSignal))); rerender(<NotificationPopup {...props} revision={1} />); await waitFor(() => expect(notificationsApi.list).toHaveBeenLastCalledWith(false, 1, expect.any(AbortSignal))); });
it("unread filter failure never exposes stale read-list rows", async () => { render(<NotificationPopup count={1} student={false} revision={0} onChanged={vi.fn()} onClose={vi.fn()} />); await screen.findByTestId("notification-item"); vi.mocked(notificationsApi.list).mockRejectedValueOnce(new Error("unavailable")); fireEvent.click(screen.getByRole("tab", { name: "Chưa đọc (1)" })); expect(await screen.findByRole("alert")).toHaveTextContent("Không thể tải"); expect(screen.queryByTestId("notification-item")).not.toBeInTheDocument(); });
it("bell connects one account stream and closes it on unmount", async () => { class FakeEventSource { static instances: FakeEventSource[] = []; onmessage: (() => void) | null = null; onerror: (() => void) | null = null; addEventListener = vi.fn(); close = vi.fn(); constructor() { FakeEventSource.instances.push(this); } } vi.stubGlobal("EventSource", FakeEventSource); vi.mocked(notificationsApi.count).mockResolvedValue({ count: 2 }); const { unmount } = render(<NotificationBell />); expect(await screen.findByRole("button", { name: "Thông báo, 2 chưa đọc" })).toBeInTheDocument(); expect(FakeEventSource.instances).toHaveLength(1); const stream = FakeEventSource.instances[0]!; fireEvent.click(screen.getByTestId("notification-bell")); await screen.findByRole("dialog"); unmount(); expect(stream.close).toHaveBeenCalled(); });
it("reconnects with bounded backoff and refreshes unread count after an outage", async () => {
  vi.useFakeTimers();
  class ReconnectingStream {
    static instances: ReconnectingStream[] = [];
    onmessage: (() => void) | null = null; onerror: (() => void) | null = null;
    addEventListener = vi.fn(); close = vi.fn();
    constructor() { ReconnectingStream.instances.push(this); }
  }
  vi.stubGlobal("EventSource", ReconnectingStream);
  vi.mocked(notificationsApi.count).mockResolvedValue({ count: 2 });
  const { unmount } = render(<NotificationBell />);
  await act(async () => {});
  for (const delay of [1000, 2000, 4000, 8000, 16000, 30000]) {
    const previous = ReconnectingStream.instances.at(-1)!;
    const size = ReconnectingStream.instances.length;
    await act(async () => { previous.onerror?.(); });
    expect(previous.close).toHaveBeenCalledOnce();
    await act(async () => { await vi.advanceTimersByTimeAsync(delay - 1); });
    expect(ReconnectingStream.instances).toHaveLength(size);
    await act(async () => { await vi.advanceTimersByTimeAsync(1); });
    expect(ReconnectingStream.instances).toHaveLength(size + 1);
  }
  vi.mocked(notificationsApi.count).mockResolvedValue({ count: 7 });
  await act(async () => { ReconnectingStream.instances.at(-1)!.onmessage?.(); });
  expect(screen.getByRole("button", { name: "Thông báo, 7 chưa đọc" })).toBeInTheDocument();
  await act(async () => { ReconnectingStream.instances.at(-1)!.onerror?.(); });
  const size = ReconnectingStream.instances.length;
  unmount();
  await vi.advanceTimersByTimeAsync(60000);
  expect(ReconnectingStream.instances).toHaveLength(size);
});
