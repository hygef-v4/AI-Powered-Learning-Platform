import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { GroupProposalSection } from "@/features/groups/components/GroupProposalSection";
import type { MyProposalView, ProposalView } from "@/features/groups/model/types";

const FORMATION = { enabled: true, minSize: 2, maxSize: 3, deadline: null, open: true, closedReason: null } as const;

const PROPOSAL: ProposalView = {
  id: "p1",
  name: "Nhóm Mây",
  status: "DRAFT",
  leaderId: "a",
  leader: true,
  version: 3,
  members: [{ accountId: "a", name: "An", email: "an@fpt.edu.vn", leader: true, expired: false }],
  invitations: [{ accountId: "b", name: "Bình", email: "binh@fpt.edu.vn", leader: false, expired: false }],
  freeSeats: 1,
  submittedAt: null,
};

const base = (overrides: Partial<MyProposalView> = {}): MyProposalView => ({
  classId: "c1",
  classStatus: "OPEN",
  grouped: false,
  formation: { ...FORMATION },
  proposal: null,
  invitations: [],
  lastRejected: null,
  candidates: [],
  ...overrides,
});

function json(body: unknown, status = 200) {
  return Promise.resolve(new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } }));
}

type Call = { url: string; method: string; body?: unknown };

function stub(first: MyProposalView, respond: (call: Call) => Promise<Response> = () => json(first)) {
  const calls: Call[] = [];
  vi.stubGlobal("fetch", vi.fn((input: RequestInfo | URL, init?: RequestInit) => {
    const call = { url: String(input), method: init?.method ?? "GET", body: init?.body ? JSON.parse(String(init.body)) : undefined };
    calls.push(call);
    return call.method === "GET" ? json(first) : respond(call);
  }));
  return calls;
}

describe("GroupProposalSection", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("đợt mở và chưa có đề xuất: tạo đề xuất với tên đã cắt khoảng trắng", async () => {
    const calls = stub(base(), () => json(base({ proposal: PROPOSAL })));
    render(<GroupProposalSection classId="c1" onGrouped={vi.fn()} />);

    fireEvent.click(await screen.findByTestId("proposal-create"));
    fireEvent.change(screen.getByTestId("proposal-name-input"), { target: { value: "  Nhóm Mây  " } });
    fireEvent.click(screen.getByTestId("proposal-name-save"));

    expect(await screen.findByTestId("proposal-card")).toHaveTextContent("Nhóm Mây");
    expect(calls.at(-1)).toMatchObject({ url: "/api/v1/classes/c1/group-proposals", method: "POST", body: { name: "Nhóm Mây" } });
  });

  it("trưởng nhóm thiếu người thì nút Gửi duyệt khóa, thu hồi lời mời gửi kèm version", async () => {
    const calls = stub(base({ proposal: PROPOSAL }), () => json(base({ proposal: { ...PROPOSAL, invitations: [], freeSeats: 2 } })));
    render(<GroupProposalSection classId="c1" onGrouped={vi.fn()} />);

    expect(await screen.findByTestId("proposal-submit")).toBeDisabled();
    expect(screen.getByText(/cần thêm 1 người/)).toBeInTheDocument();
    fireEvent.click(screen.getByTestId("proposal-revoke-b"));

    await waitFor(() => expect(calls.at(-1)).toMatchObject({
      url: "/api/v1/group-proposals/p1/invitations/b:revoke", method: "POST", body: { version: 3 },
    }));
  });

  it("mời bạn: chặn chọn quá số chỗ còn và gửi danh sách đã chọn", async () => {
    const candidates = [
      { accountId: "c", name: "Chi", email: "chi@fpt.edu.vn", inOtherProposal: false },
      { accountId: "d", name: "Dũng", email: "dung@fpt.edu.vn", inOtherProposal: true },
    ];
    const calls = stub(base({ proposal: PROPOSAL, candidates }));
    render(<GroupProposalSection classId="c1" onGrouped={vi.fn()} />);

    fireEvent.click(await screen.findByTestId("proposal-invite"));
    expect(screen.getByText("Đang ở đề xuất khác")).toBeInTheDocument();
    fireEvent.click(screen.getByTestId("invite-candidate-c"));
    expect(screen.getByTestId("invite-candidate-d")).toBeDisabled();
    fireEvent.click(screen.getByTestId("invite-members-submit"));

    await waitFor(() => expect(calls.at(-1)).toMatchObject({
      url: "/api/v1/group-proposals/p1/invitations", body: { accountIds: ["c"], version: 3 },
    }));
  });

  it("lời mời: chấp nhận gọi đúng API; đang ở đề xuất khác thì không có nút chấp nhận", async () => {
    const invitation = { proposalId: "p9", proposalName: "Nhóm Sao", status: "DRAFT" as const, leaderName: "Giang", memberCount: 1, expired: false, canAccept: true };
    const calls = stub(base({ invitations: [invitation] }), () => json(base()));
    const { unmount } = render(<GroupProposalSection classId="c1" onGrouped={vi.fn()} />);

    fireEvent.click(await screen.findByTestId("invitation-accept-p9"));
    await waitFor(() => expect(calls.at(-1)).toMatchObject({ url: "/api/v1/group-proposals/p9:accept", method: "POST" }));
    unmount();

    stub(base({ proposal: PROPOSAL, invitations: [{ ...invitation, canAccept: false }] }));
    render(<GroupProposalSection classId="c1" onGrouped={vi.fn()} />);
    expect(await screen.findByText(/Rời đề xuất hiện tại để chấp nhận/)).toBeInTheDocument();
    expect(screen.queryByTestId("invitation-accept-p9")).not.toBeInTheDocument();
  });

  it("đề xuất vừa đổi ở nơi khác (409) thì báo và tải lại", async () => {
    const ready = { ...PROPOSAL, members: [...PROPOSAL.members, { accountId: "b", name: "Bình", email: null, leader: false, expired: false }], invitations: [] };
    const calls = stub(base({ proposal: ready }), () => json({ code: "PROPOSAL_VERSION_CONFLICT", detail: "x" }, 409));
    render(<GroupProposalSection classId="c1" onGrouped={vi.fn()} />);

    fireEvent.click(await screen.findByTestId("proposal-submit"));

    expect(await screen.findByText("Đề xuất vừa thay đổi. Đã tải lại, hãy thử lại.")).toBeInTheDocument();
    await waitFor(() => expect(calls.filter((c) => c.method === "GET")).toHaveLength(2));
  });

  it("đã bị từ chối thì hiện lý do; đợt đóng thì báo giảng viên sẽ xếp nhóm", async () => {
    stub(base({
      formation: { ...FORMATION, open: false, closedReason: "DEADLINE_PASSED", deadline: "2026-11-10T03:00:00Z" },
      lastRejected: { proposalId: "p0", name: "Nhóm Cũ", reason: "Trùng đề tài", decidedAt: "2026-11-09T03:00:00Z" },
    }));
    render(<GroupProposalSection classId="c1" onGrouped={vi.fn()} />);

    expect(await screen.findByText(/Lý do: Trùng đề tài/)).toBeInTheDocument();
    expect(screen.getByText(/Đã hết hạn/)).toBeInTheDocument();
    expect(screen.getByText(/Giảng viên sẽ xếp nhóm/)).toBeInTheDocument();
    expect(screen.queryByTestId("proposal-create")).not.toBeInTheDocument();
  });

  it("đã có nhóm chính thức thì báo trang tải lại nhóm", async () => {
    stub(base({ grouped: true }));
    const onGrouped = vi.fn();
    render(<GroupProposalSection classId="c1" onGrouped={onGrouped} />);

    await waitFor(() => expect(onGrouped).toHaveBeenCalled());
    expect(screen.queryByTestId("group-proposal-section")).not.toBeInTheDocument();
  });
});
