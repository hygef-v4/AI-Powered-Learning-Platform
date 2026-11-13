import { render, screen, within } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { GroupProgressCard } from "@/features/groups/components/GroupProgressCard";
import type { GroupSetView } from "@/features/groups/model/types";

vi.mock("next/link", () => ({
  default: ({ href, children, ...rest }: { href: string; children: React.ReactNode }) => (
    <a href={href} {...rest}>
      {children}
    </a>
  ),
}));

const set: GroupSetView = {
  classId: "c1",
  classStatus: "OPEN",
  version: 1,
  groups: [
    {
      id: "g1",
      name: "Nhóm 1",
      leaderId: "u1",
      used: true,
      members: [
        { accountId: "u1", name: "Trần Đức Huy", email: "huy@fpt.edu.vn", active: true, leader: true },
        { accountId: "u2", name: "Võ Minh Tài", email: "tai@fpt.edu.vn", active: true, leader: false },
      ],
    },
    { id: "g2", name: "Nhóm 2", leaderId: null, used: false, members: [] },
  ],
  ungrouped: [],
  pendingRequests: [],
};

function json(body: unknown) {
  return Promise.resolve(new Response(JSON.stringify(body), { status: 200, headers: { "Content-Type": "application/json" } }));
}

function backend(assignments: unknown[]) {
  vi.stubGlobal(
    "fetch",
    vi.fn((input: RequestInfo | URL) => {
      const url = String(input);
      if (url.endsWith("/group-assignments")) return json(assignments);
      if (url.endsWith("/progress"))
        return json([
          {
            groupId: "g1", groupName: "Nhóm 1", leaderId: "u1", documentId: "doc1", documentStatus: "IN_PROGRESS",
            done: 2, claimed: 1, open: 1, submitted: false, submittedAt: null, late: false, submitMode: null,
            sections: [{ id: "s2", title: "2. Sơ đồ lớp", status: "CLAIMED", holderId: "u2" }],
          },
          {
            groupId: "g2", groupName: "Nhóm 2", leaderId: null, documentId: "doc2", documentStatus: "IN_PROGRESS",
            done: 4, claimed: 0, open: 0, submitted: true, submittedAt: "2026-11-07T12:00:00Z", late: true, submitMode: "MANUAL",
            sections: [],
          },
        ]);
      return Promise.resolve(new Response(null, { status: 404 }));
    }),
  );
}

describe("GroupProgressCard", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("hiện tiến độ từng nhóm, mục đang soạn và lối vào tài liệu nhóm", async () => {
    backend([{ assignmentId: "a1", title: "Tài liệu thiết kế nhóm", status: "OPEN", opensAt: null, closesAt: "2026-11-17T11:51:00Z" }]);
    render(<GroupProgressCard classId="c1" set={set} />);

    const first = (await screen.findByText("Nhóm 1")).closest("tr")!;
    expect(within(first).getByText("2 / 4")).toBeInTheDocument();
    expect(within(first).getByText("Đang làm")).toBeInTheDocument();
    expect(within(first).getByText("2. Sơ đồ lớp · Võ Minh Tài")).toBeInTheDocument();
    expect(within(first).getByRole("link", { name: "Xem tài liệu" })).toHaveAttribute("href", "/group-docs/doc1");
    const second = screen.getByText("Nhóm 2").closest("tr")!;
    expect(within(second).getByText("Đã nộp · trễ")).toBeInTheDocument();
  });

  it("lớp chưa có bài nhóm thì không hiện thẻ", async () => {
    backend([]);
    const { container } = render(<GroupProgressCard classId="c1" set={set} />);
    await vi.waitFor(() => expect(vi.mocked(fetch)).toHaveBeenCalled());
    await new Promise((r) => setTimeout(r, 0));
    expect(container).toBeEmptyDOMElement();
  });
});
