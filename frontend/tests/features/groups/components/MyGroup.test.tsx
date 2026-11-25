import { fireEvent, render, screen, within } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { MyGroupPage } from "@/features/groups/components/MyGroup";
import type { MyGroupView } from "@/features/groups/model/types";

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
  amLeader: false,
};

function json(body: unknown, status = 200) {
  return Promise.resolve(new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } }));
}

describe("MyGroupPage", () => {
  it("keeps student codes in current and proposed leader controls without changing the selected id", async () => {
    vi.stubGlobal("fetch", vi.fn(() => json({ ...VIEW, members: VIEW.members.map((m, i) => ({
      ...m, studentCode: `SE00000${i + 1}`,
    })) })));
    render(<MyGroupPage classId="c1" />);
    fireEvent.click(await screen.findByRole("button", { name: /Yêu cầu đổi trưởng nhóm/ }));
    const dialog = await screen.findByRole("dialog");
    expect(within(dialog).getByLabelText("Trưởng nhóm hiện tại")).toHaveValue("An · SE000001");
    expect(within(dialog).getByRole("option", { name: "Bình · SE000002" })).toHaveValue("b");
  });
  afterEach(() => vi.unstubAllGlobals());

  it("mỗi thành viên một thẻ, nhãn Trưởng nhóm trên thẻ trưởng nhóm; bài nhóm chỉ là link sang trang Bài tập", async () => {
    vi.stubGlobal("fetch", vi.fn(() => json(VIEW)));
    render(<MyGroupPage classId="c1" />);

    const cards = await screen.findAllByTestId("group-member");
    expect(cards).toHaveLength(2);
    expect(cards[0]).toHaveTextContent("Trưởng nhóm");
    expect(cards[0]!.querySelector("svg")).not.toBeNull();
    expect(cards[1]).not.toHaveTextContent("Trưởng nhóm");
    expect(screen.queryByText("Bạn")).not.toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Nhóm 1" })).toBeInTheDocument();
    expect(screen.getByText("2 thành viên")).toBeInTheDocument();
    expect(screen.getByTestId("group-works-link")).toHaveAttribute("href", "/my-assignments?classId=c1&mode=GROUP");
    expect(screen.queryByText("Sơ đồ lớp")).not.toBeInTheDocument();
    expect(screen.queryByText(/Lý do từ chối/)).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Sửa tên nhóm" })).not.toBeInTheDocument();
  });

  it("trưởng nhóm sửa tên ngay tại chỗ, không cần duyệt", async () => {
    const calls: { url: string; method?: string; body?: unknown }[] = [];
    let name = "Nhóm 1";
    vi.stubGlobal(
      "fetch",
      vi.fn((input: RequestInit | URL | string, init?: RequestInit) => {
        calls.push({ url: String(input), method: init?.method, body: init?.body ? JSON.parse(String(init.body)) : undefined });
        if (String(input).endsWith("/name")) { name = "Sao Băng"; return json({ groupId: "g1", name }); }
        return json({ ...VIEW, name, amLeader: true });
      }),
    );
    render(<MyGroupPage classId="c1" />);
    fireEvent.click(await screen.findByRole("button", { name: "Sửa tên nhóm" }));
    const input = screen.getByRole("textbox", { name: "Tên nhóm" });
    fireEvent.change(input, { target: { value: "  " } });
    fireEvent.click(screen.getByRole("button", { name: "Lưu" }));
    expect(await screen.findByRole("alert")).toHaveTextContent("Tên nhóm cần 1–100 ký tự.");
    fireEvent.change(input, { target: { value: " Sao Băng " } });
    fireEvent.click(screen.getByRole("button", { name: "Lưu" }));

    expect(await screen.findByRole("heading", { name: "Sao Băng" })).toBeInTheDocument();
    expect(calls.find((c) => c.url === "/api/v1/groups/g1/name")).toMatchObject({ method: "PUT", body: { name: "Sao Băng" } });
  });
});
