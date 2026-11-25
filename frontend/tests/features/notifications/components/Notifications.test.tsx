import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { EmailPreferencePanel } from "@/features/notifications/components/EmailPreferencePanel";
import { NotificationBell } from "@/features/notifications/components/NotificationBell";
import { notificationsApi } from "@/features/notifications/api";
vi.mock("@/features/notifications/api", async (original) => ({
  ...(await original<typeof import("@/features/notifications/api")>()),
  notificationsApi: {
    countUnread: vi.fn(),
    listNotifications: vi.fn(),
    markRead: vi.fn(),
    markAllRead: vi.fn(),
    loadPreferences: vi.fn(),
    savePreferences: vi.fn(),
  },
}));
const push = vi.fn();
vi.mock("next/navigation", () => ({ useRouter: () => ({ push }) }));
const sessionRole = vi.hoisted(() => ({ value: "STUDENT" }));
vi.mock("@/features/identity/hooks/SessionProvider", () => ({
  useSession: () => ({
    status: "authenticated",
    profile: { accountId: "a", role: sessionRole.value },
  }),
}));
const n = {
  id: "n",
  type: "GRADE_PUBLISHED",
  title: "Bạn có điểm mới",
  summary: "Đăng nhập để xem",
  link: "/classes/1",
  readAt: null,
  createdAt: "2026-10-21T08:00:00Z",
};
beforeEach(() => {
  vi.clearAllMocks();
  sessionRole.value = "STUDENT";
  vi.mocked(notificationsApi.countUnread).mockResolvedValue({ count: 1 });
  vi.mocked(notificationsApi.listNotifications).mockResolvedValue({
    items: [n],
    page: 1,
    size: 20,
    hasMore: false,
  });
  vi.mocked(notificationsApi.loadPreferences).mockResolvedValue({
    ENROLLED: true,
    ASSIGNMENT_OPENED: true,
    DEADLINE_REMINDER: true,
    GRADE_PUBLISHED: true,
    ANNOUNCEMENT_IMPORTANT: true,
  });
});
afterEach(() => {
  vi.unstubAllGlobals();
  vi.useRealTimers();
});
it.each(["STUDENT", "TEACHER", "SUBJECT_MANAGER", "ADMIN"])(
  "%s can open settings from its notification bell",
  async (role) => {
    sessionRole.value = role;
    const values: Record<string, boolean> =
      role === "ADMIN"
        ? { BACKUP_FAILED: true }
        : role === "STUDENT"
          ? { ENROLLED: true }
          : { QUESTION_REVIEWED: false };
    vi.mocked(notificationsApi.loadPreferences).mockResolvedValue(values);
    render(<NotificationBell />);
    fireEvent.click(await screen.findByTestId("notification-bell"));
    fireEvent.click(screen.getByRole("button", { name: "Cài đặt thông báo" }));
    const label =
      role === "ADMIN"
        ? "Sao lưu thất bại"
        : role === "STUDENT"
          ? "Ghi danh vào lớp"
          : "Kết quả duyệt câu hỏi";
    expect(await screen.findByRole("switch", { name: label })).toBeInTheDocument();
    expect(screen.getAllByRole("switch")).toHaveLength(1);
  },
);
it("staff switches save only the applicable type and show the persisted response", async () => {
  vi.mocked(notificationsApi.loadPreferences).mockResolvedValue({
    QUESTION_PROPOSED: false,
    QUESTION_REVIEWED: false,
    LEADER_CHANGE_REQUESTED: false,
  });
  vi.mocked(notificationsApi.savePreferences).mockResolvedValue({
    QUESTION_PROPOSED: true,
    QUESTION_REVIEWED: false,
    LEADER_CHANGE_REQUESTED: false,
  });
  render(<EmailPreferencePanel />);
  const toggle = await screen.findByRole("switch", { name: "Đề xuất câu hỏi chờ duyệt" });
  expect(screen.queryByRole("switch", { name: "Ghi danh vào lớp" })).not.toBeInTheDocument();
  fireEvent.click(toggle);
  await waitFor(() => expect(toggle).toHaveAttribute("aria-checked", "true"));
  expect(notificationsApi.savePreferences).toHaveBeenCalledWith({ QUESTION_PROPOSED: true });
});
