import { fireEvent, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { ProposalReviewList } from "@/features/groups/components/ProposalReviewList";
import type { ClassProposalsView, FormationView, ReviewProposalView } from "@/features/groups/model/types";

const FORMATION: FormationView = { classId: "c1", enabled: true, minSize: 2, maxSize: 4, deadline: null, version: 2, open: true, closedReason: null };

const proposal = (id: string, name: string, status: ReviewProposalView["status"] = "SUBMITTED"): ReviewProposalView => ({
  id,
  name,
  status,
  leader: { accountId: `${id}-l`, name: `Trưởng ${name}`, email: null },
  members: [{ accountId: `${id}-l`, name: `Trưởng ${name}`, email: null }, { accountId: `${id}-m`, name: `Bạn ${name}`, email: null }],
  pendingInvitations: 0,
  submittedAt: "2026-11-11T03:00:00Z",
  warnings: [],
});

const VIEW: ClassProposalsView = {
  classId: "c1",
  formation: FORMATION,
  proposals: [proposal("p1", "Nhóm Mây"), proposal("p2", "Nhóm Sao"), proposal("p3", "Nhóm Gió", "DRAFT")],
  ungrouped: [
    { person: { accountId: "p1-l", name: "Trưởng Nhóm Mây", email: "a@fpt.edu.vn" }, proposalId: "p1", proposalName: "Nhóm Mây", proposalStatus: "SUBMITTED" },
    { person: { accountId: "x", name: "Xuân", email: "x@fpt.edu.vn" }, proposalId: null, proposalName: null, proposalStatus: null },
  ],
};

function json(body: unknown, status = 200) {
  return Promise.resolve(new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } }));
}

type Call = { url: string; method: string; body?: unknown };

function stub(respond: (call: Call) => Promise<Response>) {
  const calls: Call[] = [];
  vi.stubGlobal("fetch", vi.fn((input: RequestInfo | URL, init?: RequestInit) => {
    const call = { url: String(input), method: init?.method ?? "GET", body: init?.body ? JSON.parse(String(init.body)) : undefined };
    calls.push(call);
    return respond(call);
  }));
  return calls;
}

describe("ProposalReviewList", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("duyệt nhiều: báo lỗi tại thẻ đề xuất không duyệt được và tải lại bộ nhóm", async () => {
    const calls = stub((call) => (call.url.endsWith(":approve")
      ? json({ approved: [{ proposalId: "p1", groupId: "g1" }], failed: [{ proposalId: "p2", code: "GROUP_NAME_DUPLICATE", message: "Tên nhóm bị trùng: Nhóm Sao" }], groupSetVersion: 5 })
      : json(VIEW)));
    const onGroupsChanged = vi.fn();
    render(<ProposalReviewList classId="c1" setVersion={4} readOnly={false} onGroupsChanged={onGroupsChanged} />);

    fireEvent.click(await screen.findByLabelText("Chọn Nhóm Mây"));
    fireEvent.click(screen.getByLabelText("Chọn Nhóm Sao"));
    fireEvent.click(screen.getByTestId("proposals-approve-selected"));

    expect(await screen.findByText("Tên nhóm bị trùng: Nhóm Sao")).toBeInTheDocument();
    expect(calls.find((c) => c.url.endsWith(":approve"))).toMatchObject({
      url: "/api/v1/classes/c1/group-proposals:approve", body: { proposalIds: ["p1", "p2"] },
    });
    expect(onGroupsChanged).toHaveBeenCalledTimes(1);
  });

  it("lớp chưa bật và chưa có đề xuất: chỉ một dòng mời bật", async () => {
    stub(() => json({ ...VIEW, formation: { ...FORMATION, enabled: false, open: false, closedReason: "DISABLED" }, proposals: [] }));
    render(<ProposalReviewList classId="c1" setVersion={1} readOnly={false} onGroupsChanged={vi.fn()} />);

    expect(await screen.findByText(/Bật để sinh viên tự lập nhóm/)).toBeInTheDocument();
    expect(screen.queryByTestId("ungrouped-students")).not.toBeInTheDocument();
  });
});
