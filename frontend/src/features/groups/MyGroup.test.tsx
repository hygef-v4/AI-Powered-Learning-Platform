import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { MyGroupPage } from "./MyGroup";
import type { MyGroupView } from "./types";

vi.mock("next/link", () => ({
  default: ({ href, children, ...rest }: { href: string; children: React.ReactNode }) => (
    <a href={href} {...rest}>
      {children}
    </a>
  ),
}));

const VIEW: MyGroupView = {
  classId: "c1",
  classStatus: "OPEN",
  groupId: "g1",
  name: "Nhóm 1",
  leaderId: "a",
  members: [
    { accountId: "a", name: "An", email: "an@fpt.edu.vn", leader: true },
    { accountId: "b", name: "Bình", email: "binh@fpt.edu.vn", leader: false },
  ],
  works: [
    { assignmentId: "as1", title: "Sơ đồ lớp", closesAt: "2026-10-30T10:00:00Z", documentId: "doc1", doneSections: 1, totalSections: 3, submitted: false, submittedAt: null, late: false },
  ],
  pendingRequest: false,
  myRequests: [
    { id: "r0", status: "REJECTED", proposedLeaderId: null, reason: "Lý do cũ đủ mười ký tự", decisionReason: "Nhóm đang ổn", createdAt: "2026-10-10T03:00:00Z", decidedAt: "2026-10-11T03:00:00Z" },
  ],
};

function json(body: unknown, status = 200) {
  return Promise.resolve(new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } }));
}

describe("MyGroupPage", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("hiện thành viên, bài nhóm và lý do từ chối của yêu cầu gần nhất", async () => {
    vi.stubGlobal("fetch", vi.fn(() => json(VIEW)));
    render(<MyGroupPage classId="c1" />);

    expect(await screen.findByText("Thành viên (2)")).toBeInTheDocument();
    expect(screen.getByText("Lý do từ chối: Nhóm đang ổn")).toBeInTheDocument();
    expect(screen.getByText("Đang làm · 1 / 3 mục xong")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Mở tài liệu" })).toHaveAttribute("href", "/group-docs/doc1");
  });

  it("gửi yêu cầu cần lý do ít nhất 10 ký tự và gửi người được đề xuất", async () => {
    const calls: { url: string; body?: unknown }[] = [];
    vi.stubGlobal(
      "fetch",
      vi.fn((input: RequestInfo | URL, init?: RequestInit) => {
        calls.push({ url: String(input), body: init?.body ? JSON.parse(String(init.body)) : undefined });
        return String(input).includes("leader-requests") ? json({}, 201) : json(VIEW);
      }),
    );
    render(<MyGroupPage classId="c1" />);
    fireEvent.click(await screen.findByRole("button", { name: /Yêu cầu đổi trưởng nhóm/ }));

    const dialog = await screen.findByRole("dialog");
    const send = within(dialog).getByRole("button", { name: "Gửi yêu cầu" });
    fireEvent.change(within(dialog).getByLabelText(/Lý do/), { target: { value: "ngắn" } });
    expect(send).toBeDisabled();
    fireEvent.change(within(dialog).getByLabelText(/Lý do/), { target: { value: "An bận thực tập cả tháng" } });
    fireEvent.change(within(dialog).getByLabelText(/Đề xuất/), { target: { value: "b" } });
    fireEvent.click(send);

    await waitFor(() => expect(calls.some((c) => c.url === "/api/v1/groups/g1/leader-requests")).toBe(true));
    expect(calls.find((c) => c.url === "/api/v1/groups/g1/leader-requests")!.body).toEqual({
      reason: "An bận thực tập cả tháng",
      proposedLeaderId: "b",
    });
  });

  it("nhóm đang có yêu cầu chờ thì khóa nút gửi", async () => {
    vi.stubGlobal("fetch", vi.fn(() => json({ ...VIEW, pendingRequest: true })));
    render(<MyGroupPage classId="c1" />);
    expect(await screen.findByRole("button", { name: /Yêu cầu đổi trưởng nhóm/ })).toBeDisabled();
  });
});
