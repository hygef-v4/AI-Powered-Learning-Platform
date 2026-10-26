import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { ClassGroupsPanel } from "./ClassGroupsPanel";
import type { GroupSetView } from "./types";

const CLASS = "c1";
const A = { accountId: "a", name: "An", email: "an@fpt.edu.vn" };
const B = { accountId: "b", name: "Bình", email: "binh@fpt.edu.vn" };
const C = { accountId: "c", name: "Chi", email: "chi@fpt.edu.vn" };
const D = { accountId: "d", name: "Dũng", email: "dung@fpt.edu.vn" };

const WITH_GROUPS: GroupSetView = {
  classId: CLASS,
  classStatus: "OPEN",
  version: 3,
  groups: [
    {
      id: "g1",
      name: "Nhóm 1",
      leaderId: "a",
      used: false,
      members: [
        { ...A, active: true, leader: true },
        { ...B, active: true, leader: false },
        { accountId: "x", name: "Xuân", email: null, active: false, leader: false },
      ],
    },
  ],
  ungrouped: [C, D],
  pendingRequests: [
    {
      id: "r1",
      groupId: "g1",
      groupName: "Nhóm 1",
      requestedBy: B,
      proposedLeader: B,
      reason: "An bận thực tập, không theo được bài",
      createdAt: "2026-10-21T03:00:00Z",
    },
  ],
};

function json(body: unknown, status = 200) {
  return Promise.resolve(new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } }));
}

function backend(set: GroupSetView, onPut?: (body: unknown) => Response | Promise<Response>) {
  const calls: { url: string; method: string; body?: unknown }[] = [];
  vi.stubGlobal(
    "fetch",
    vi.fn((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      const method = init?.method ?? "GET";
      const body = init?.body ? JSON.parse(String(init.body)) : undefined;
      calls.push({ url, method, body });
      if (url.endsWith("/groups") && method === "GET") return json(set);
      if (url.endsWith("/groups") && method === "PUT") return onPut ? Promise.resolve(onPut(body)) : json({ ...set, version: 4 });
      if (url.endsWith("groups:random-preview"))
        return json([
          { id: "g1", name: "Nhóm 1", leaderId: "a", memberIds: ["a", "b"], generated: false },
          { id: null, name: "Nhóm 2", leaderId: "c", memberIds: ["c", "d"], generated: true },
        ]);
      if (url.includes(":approve") || url.includes(":reject")) return json({});
      return json({ status: 404 }, 404);
    }),
  );
  return calls;
}

describe("ClassGroupsPanel", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("lớp chưa có nhóm hiện Tạo nhóm và Chia ngẫu nhiên", async () => {
    backend({ ...WITH_GROUPS, groups: [], ungrouped: [A, B, C, D], pendingRequests: [] });
    render(<ClassGroupsPanel classId={CLASS} />);

    expect(await screen.findByText("4 sinh viên · lớp chưa có nhóm")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Tạo nhóm/ })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Chia ngẫu nhiên/ })).toBeInTheDocument();
  });

  it("thẻ nhóm đánh dấu trưởng nhóm và người đã rời lớp", async () => {
    backend(WITH_GROUPS);
    render(<ClassGroupsPanel classId={CLASS} />);

    expect(await screen.findByText("1 nhóm · 4 sinh viên · 2 chưa có nhóm")).toBeInTheDocument();
    expect(screen.getByText("Xuân").closest("li")).toHaveTextContent("Đã rời lớp");
    expect(screen.getByText("Chưa có nhóm: Chi, Dũng")).toBeInTheDocument();
  });

  it("sửa nhóm gửi cả bộ kèm version, bỏ người đã rời lớp", async () => {
    const calls = backend(WITH_GROUPS);
    render(<ClassGroupsPanel classId={CLASS} />);
    fireEvent.click(await screen.findByRole("button", { name: /Sửa nhóm/ }));

    fireEvent.change(screen.getByLabelText("Chuyển Chi"), { target: { value: "g1" } });
    fireEvent.click(screen.getByRole("button", { name: /Lưu cho cả lớp/ }));

    await waitFor(() => expect(calls.some((c) => c.method === "PUT")).toBe(true));
    expect(calls.find((c) => c.method === "PUT")!.body).toEqual({
      version: 3,
      groups: [{ key: "g1", id: "g1", name: "Nhóm 1", leaderId: "a", memberIds: ["a", "b", "c"] }],
    });
  });

  it("kéo thả chuyển thành viên giữa nhóm và khu Chưa có nhóm; nhóm mới ghi (mới)", async () => {
    const calls = backend(WITH_GROUPS);
    render(<ClassGroupsPanel classId={CLASS} />);
    fireEvent.click(await screen.findByRole("button", { name: /Sửa nhóm/ }));
    fireEvent.click(screen.getByRole("button", { name: /Thêm nhóm/ }));

    const fresh = screen.getByRole("region", { name: "Nhóm Nhóm 2" });
    expect(within(fresh).getByText("(mới)")).toBeInTheDocument();
    expect(within(fresh).getByText(/Chưa có trưởng nhóm/)).toBeInTheDocument();

    fireEvent.dragStart(screen.getByText("Chi").closest("li")!);
    fireEvent.dragOver(fresh);
    fireEvent.drop(fresh);
    fireEvent.dragStart(screen.getByText("Bình").closest("li")!);
    fireEvent.drop(screen.getByRole("region", { name: "Chưa có nhóm" }));
    fireEvent.click(screen.getByRole("button", { name: /Lưu cho cả lớp/ }));

    await waitFor(() => expect(calls.some((c) => c.method === "PUT")).toBe(true));
    expect(calls.find((c) => c.method === "PUT")!.body).toEqual({
      version: 3,
      groups: [
        { key: "g1", id: "g1", name: "Nhóm 1", leaderId: "a", memberIds: ["a"] },
        { key: "new-1", id: null, name: "Nhóm 2", leaderId: null, memberIds: ["c"] },
      ],
    });
  });

  it("xóa nhóm đã dùng: lý do của nhóm đã bị xóa khỏi màn vẫn hiện trong thông báo lỗi", async () => {
    backend(WITH_GROUPS, () =>
      new Response(
        JSON.stringify({
          status: 400,
          code: "GROUP_SET_INVALID",
          issues: [{ groupKey: "g1", code: "GROUP_IN_USE", message: "Nhóm Nhóm 1 đã có tài liệu hoặc bản nộp, chỉ chuyển thành viên được" }],
        }),
        { status: 400, headers: { "Content-Type": "application/json" } },
      ),
    );
    render(<ClassGroupsPanel classId={CLASS} />);
    fireEvent.click(await screen.findByRole("button", { name: /Sửa nhóm/ }));
    fireEvent.click(screen.getByRole("button", { name: "Xóa Nhóm 1" }));
    fireEvent.click(screen.getByRole("button", { name: /Lưu cho cả lớp/ }));

    const alert = await screen.findByText(/Bộ nhóm có lỗi/);
    expect(alert.closest('[role="alert"]')).toHaveTextContent("Nhóm Nhóm 1 đã có tài liệu hoặc bản nộp, chỉ chuyển thành viên được");
  });

  it("lỗi bộ nhóm hiện theo từng nhóm, không rời trình sửa", async () => {
    backend(WITH_GROUPS, () =>
      new Response(
        JSON.stringify({ status: 400, code: "GROUP_SET_INVALID", issues: [{ groupKey: "g1", code: "GROUP_NAME_DUPLICATE", message: "Tên nhóm bị trùng" }] }),
        { status: 400, headers: { "Content-Type": "application/json" } },
      ),
    );
    render(<ClassGroupsPanel classId={CLASS} />);
    fireEvent.click(await screen.findByRole("button", { name: /Sửa nhóm/ }));
    fireEvent.click(screen.getByRole("button", { name: /Lưu cho cả lớp/ }));

    const group = await screen.findByRole("region", { name: "Nhóm Nhóm 1" });
    expect(await within(group).findByText("Tên nhóm bị trùng")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Lưu cho cả lớp/ })).toBeInTheDocument();
  });

  it("chia ngẫu nhiên đưa bản xem trước vào trình sửa trước khi lưu", async () => {
    const calls = backend(WITH_GROUPS);
    render(<ClassGroupsPanel classId={CLASS} />);
    fireEvent.click(await screen.findByRole("button", { name: /Chia ngẫu nhiên/ }));
    const dialog = await screen.findByRole("dialog");
    fireEvent.click(within(dialog).getByRole("button", { name: "Xem trước" }));
    fireEvent.click(await within(dialog).findByRole("button", { name: /Sửa rồi lưu 1 nhóm mới/ }));

    expect(await screen.findByRole("region", { name: "Nhóm Nhóm 2" })).toBeInTheDocument();
    expect(calls.find((c) => c.url.endsWith("groups:random-preview"))!.body).toEqual({ maxSize: 4 });
    expect(calls.some((c) => c.method === "PUT")).toBe(false);
  });

  it("duyệt yêu cầu dùng người được đề xuất, từ chối cần lý do", async () => {
    const calls = backend(WITH_GROUPS);
    render(<ClassGroupsPanel classId={CLASS} />);

    expect(await screen.findByText("An bận thực tập, không theo được bài")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Từ chối" })).toBeDisabled();
    fireEvent.click(screen.getByRole("button", { name: "Duyệt" }));

    await waitFor(() => expect(calls.some((c) => c.url.includes(":approve"))).toBe(true));
    expect(calls.find((c) => c.url.includes(":approve"))!.body).toEqual({ newLeaderId: "b" });
  });
});
