import { fireEvent, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { GroupAssignmentPanel } from "@/features/groups/components/TeacherGroupWidgets";
import type { GroupSetView } from "@/features/groups/model/types";

const A = { accountId: "a", name: "An", email: null };
const B = { accountId: "b", name: "Bình", email: null };

const SET: GroupSetView = {
  classId: "c1",
  classStatus: "OPEN",
  version: 1,
  groups: [{ id: "g1", name: "Nhóm 1", leaderId: null, used: false, members: [{ ...A, active: true, leader: false }] }],
  ungrouped: [B],
  pendingRequests: [],
};

function serve(...sets: GroupSetView[]) {
  let i = 0;
  const fetch = vi.fn(() => {
    const body = sets[Math.min(i++, sets.length - 1)];
    return Promise.resolve(new Response(JSON.stringify(body), { status: 200, headers: { "Content-Type": "application/json" } }));
  });
  vi.stubGlobal("fetch", fetch);
  return fetch;
}

describe("GroupAssignmentPanel", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("thiếu trưởng nhóm chặn, chưa có nhóm cảnh báo, nút mở tab Sinh viên ở tab mới", async () => {
    serve(SET);
    const open = vi.fn();
    vi.stubGlobal("open", open);
    render(<GroupAssignmentPanel classId="c1" />);

    expect(await screen.findByText("Có nhóm chưa có trưởng nhóm.")).toBeInTheDocument();
    expect(screen.getByText(/1 sinh viên chưa có nhóm/)).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: /Mở tab Sinh viên/ }));
    expect(open).toHaveBeenCalledWith("/teaching/classes/c1?tab=students", "_blank", "noopener");
  });

  it("quay lại cửa sổ thì kiểm tra lại", async () => {
    const fetch = serve(SET, { ...SET, groups: [{ id: "g1", name: "Nhóm 1", used: false, leaderId: "a", members: [{ ...A, active: true, leader: true }, { ...B, active: true, leader: false }] }], ungrouped: [] });
    render(<GroupAssignmentPanel classId="c1" />);
    expect(await screen.findByText("Có nhóm chưa có trưởng nhóm.")).toBeInTheDocument();

    fireEvent.focus(window);
    expect(await screen.findByText("Lớp đã sẵn sàng cho bài nhóm.")).toBeInTheDocument();
    expect(fetch).toHaveBeenCalledTimes(2);
  });
});
