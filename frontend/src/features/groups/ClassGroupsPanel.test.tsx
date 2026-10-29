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

const putBody = (calls: { method: string; body?: unknown }[]) => calls.find((c) => c.method === "PUT")!.body as {
  version: number;
  groups: { key: string; id: string | null; name: string; leaderId: string | null; memberIds: string[] }[];
};

describe("ClassGroupsPanel", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("thẻ Nhóm (Tất cả, từng nhóm, Chưa có nhóm) lọc bảng sinh viên chỉ để xem; báo trang lớp ẩn bảng của mình và báo tên nhóm, trưởng nhóm", async () => {
    backend(WITH_GROUPS);
    const loaded = vi.fn();
    const table = vi.fn();
    render(<ClassGroupsPanel classId={CLASS} onGroupNamesLoaded={loaded} onStudentTable={table} />);

    const list = within(await screen.findByRole("list", { name: "Danh sách nhóm" }));
    expect(list.getAllByRole("button", { pressed: false }).concat(list.getAllByRole("button", { pressed: true }))
      .filter((b) => b.hasAttribute("aria-pressed")).length).toBe(3);
    expect(screen.queryByRole("combobox", { name: "Lọc theo nhóm" })).not.toBeInTheDocument();
    // Không sửa nhóm ngay trên dòng.
    expect(screen.queryByRole("combobox", { name: /Nhóm của/ })).not.toBeInTheDocument();
    expect(screen.queryByRole("checkbox")).not.toBeInTheDocument();
    expect(screen.queryByText("Xuân")).not.toBeInTheDocument();
    expect(loaded).toHaveBeenCalledWith({ a: "Nhóm 1", b: "Nhóm 1" }, ["a"]);
    expect(table).toHaveBeenLastCalledWith(true);

    // Thẻ Nhóm: Tất cả đầu tiên, Chưa có nhóm cuối cùng; mỗi mục chỉ ghi số sinh viên; nhóm có sẵn nút sửa và xóa.
    const items = list.getAllByRole("button").filter((b) => b.hasAttribute("aria-pressed"));
    expect(items.map((b) => b.textContent)).toEqual(["Tất cả4 sinh viên", "Nhóm 12 sinh viên", "Chưa có nhóm2 sinh viên"]);
    expect(list.getByRole("button", { name: "Sửa Nhóm 1" })).toBeInTheDocument();
    expect(list.getByRole("button", { name: "Xóa Nhóm 1" })).toBeInTheDocument();
    fireEvent.click(items[1]!);
    expect(screen.getByRole("heading", { name: "Nhóm 1", level: 2 })).toBeInTheDocument();
    expect(screen.queryByText("Chi")).not.toBeInTheDocument();
    fireEvent.click(items[0]!);
    expect(screen.getByText("Chi")).toBeInTheDocument();
  });

  it("nút Tạo nhóm và Chia ngẫu nhiên nằm trong thẻ Nhóm; bấm dòng để xem sinh viên, có ngày vào lớp", async () => {
    backend(WITH_GROUPS);
    const view = vi.fn();
    render(<ClassGroupsPanel classId={CLASS} onView={view}
      students={[{ ...A, enrolledAt: "2026-09-01T03:00:00Z" }, { ...B, enrolledAt: "2026-09-02T03:00:00Z" }]} />);

    fireEvent.click(await screen.findByText("Bình"));
    expect(view).toHaveBeenCalledWith("b");
    expect(screen.getByText("1/9/2026")).toBeInTheDocument();
    // Cột cuối: nút Bảng điểm sang trang Chấm điểm của người đó, không mở Student Detail.
    const grades = screen.getAllByRole("link", { name: "Bảng điểm" })[0]!;
    expect(grades).toHaveAttribute("href", "/teaching/classes/c1?tab=gradebook&student=a");
    view.mockClear();
    fireEvent.click(grades);
    expect(view).not.toHaveBeenCalled();
    const card = within(screen.getByRole("region", { name: "Nhóm" }));
    expect(card.getByRole("button", { name: /Tạo nhóm/ })).toBeInTheDocument();
    expect(card.getByRole("button", { name: /Chia ngẫu nhiên/ })).toBeInTheDocument();
  });

  it("Tạo nhóm: chọn người chưa có nhóm, chọn trưởng nhóm, lưu ngay cả bộ kèm version rồi lọc sang nhóm mới", async () => {
    const calls = backend(WITH_GROUPS, (body) => json({
      ...WITH_GROUPS, version: 4, ungrouped: [],
      groups: [...WITH_GROUPS.groups, { id: "g2", name: "Nhóm B", leaderId: "d", used: false, members: [{ ...C, active: true, leader: false }, { ...D, active: true, leader: true }] }],
      _sent: body,
    }));
    render(<ClassGroupsPanel classId={CLASS} />);
    fireEvent.click(await screen.findByRole("button", { name: /Tạo nhóm/ }));
    const dialog = await screen.findByRole("dialog", { name: "Tạo nhóm" });

    // Chỉ liệt kê người chưa có nhóm.
    expect(within(dialog).getAllByRole("checkbox").map((c) => c.getAttribute("aria-label"))).toEqual(["Chọn Chi", "Chọn Dũng"]);
    fireEvent.change(within(dialog).getByLabelText(/Tên nhóm/), { target: { value: "Nhóm B" } });
    fireEvent.click(within(dialog).getByRole("checkbox", { name: "Chọn Chi" }));
    fireEvent.click(within(dialog).getByRole("checkbox", { name: "Chọn Dũng" }));
    fireEvent.change(within(dialog).getByRole("combobox", { name: /Trưởng nhóm/ }), { target: { value: "d" } });
    fireEvent.click(within(dialog).getByRole("button", { name: "Tạo nhóm" }));

    await waitFor(() => expect(calls.some((c) => c.method === "PUT")).toBe(true));
    expect(putBody(calls)).toEqual({
      version: 3,
      groups: [
        { key: "g1", id: "g1", name: "Nhóm 1", leaderId: "a", memberIds: ["a", "b"] },
        { key: "new-1", id: null, name: "Nhóm B", leaderId: "d", memberIds: ["c", "d"] },
      ],
    });
    await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
    expect(screen.getByRole("heading", { name: "Nhóm B", level: 2 })).toBeInTheDocument();
  });

  it("Sửa nhóm: thành viên hiện tại đứng đầu, thêm người chưa có nhóm; người ở nhóm khác không có trong danh sách", async () => {
    const two = { ...WITH_GROUPS, pendingRequests: [], ungrouped: [D], groups: [
      WITH_GROUPS.groups[0]!,
      { id: "g2", name: "Nhóm 2", leaderId: "c", used: false, members: [{ ...C, active: true, leader: true }] },
    ] };
    const calls = backend(two);
    render(<ClassGroupsPanel classId={CLASS} />);
    fireEvent.click(await screen.findByRole("button", { name: "Sửa Nhóm 1" }));
    const dialog = await screen.findByRole("dialog", { name: "Sửa Nhóm 1" });

    expect(within(dialog).getAllByRole("checkbox").map((c) => c.getAttribute("aria-label"))).toEqual(["Chọn An", "Chọn Bình", "Chọn Dũng"]);
    fireEvent.click(within(dialog).getByRole("checkbox", { name: "Chọn Dũng" }));
    fireEvent.click(within(dialog).getByRole("checkbox", { name: "Chọn An" }));
    fireEvent.click(within(dialog).getByRole("button", { name: "Lưu nhóm" }));

    await waitFor(() => expect(calls.some((c) => c.method === "PUT")).toBe(true));
    expect(putBody(calls).groups).toEqual([
      { key: "g1", id: "g1", name: "Nhóm 1", leaderId: "b", memberIds: ["b", "d"] },
      { key: "g2", id: "g2", name: "Nhóm 2", leaderId: "c", memberIds: ["c"] },
    ]);
  });

  it("tên nhóm trùng hoặc chưa chọn ai thì không lưu được", async () => {
    backend(WITH_GROUPS);
    render(<ClassGroupsPanel classId={CLASS} />);
    fireEvent.click(await screen.findByRole("button", { name: /Tạo nhóm/ }));
    const dialog = await screen.findByRole("dialog", { name: "Tạo nhóm" });
    const submit = within(dialog).getByRole("button", { name: "Tạo nhóm" });
    expect(submit).toBeDisabled();
    fireEvent.click(within(dialog).getByRole("checkbox", { name: "Chọn Chi" }));
    expect(submit).toBeEnabled();
    fireEvent.change(within(dialog).getByLabelText(/Tên nhóm/), { target: { value: "nhóm 1" } });
    expect(dialog).toHaveTextContent("Tên nhóm bị trùng");
    expect(submit).toBeDisabled();
  });

  it("Xóa nhóm hỏi lại rồi lưu; nhóm đã có bài thì không xóa được; lỗi máy chủ hiện theo nhóm", async () => {
    const calls = backend(WITH_GROUPS, () =>
      new Response(
        JSON.stringify({ status: 400, code: "GROUP_SET_INVALID", issues: [{ groupKey: "g1", code: "GROUP_IN_USE", message: "Nhóm Nhóm 1 đã có tài liệu hoặc bản nộp" }] }),
        { status: 400, headers: { "Content-Type": "application/json" } },
      ),
    );
    render(<ClassGroupsPanel classId={CLASS} />);
    fireEvent.click(await screen.findByRole("button", { name: "Xóa Nhóm 1" }));
    const dialog = await screen.findByRole("dialog", { name: "Xóa Nhóm 1?" });
    expect(dialog).not.toHaveTextContent("sinh viên sẽ về");
    fireEvent.click(within(dialog).getByRole("button", { name: "Xóa nhóm" }));

    await waitFor(() => expect(calls.some((c) => c.method === "PUT")).toBe(true));
    expect(putBody(calls).groups).toEqual([]);
    expect(await within(dialog).findByRole("alert")).toHaveTextContent("Nhóm 1: Nhóm Nhóm 1 đã có tài liệu hoặc bản nộp");
  });

  it("nhóm đã có bài: nút Xóa nhóm bị khóa", async () => {
    backend({ ...WITH_GROUPS, groups: WITH_GROUPS.groups.map((g) => ({ ...g, used: true })) });
    render(<ClassGroupsPanel classId={CLASS} />);
    expect(await screen.findByRole("button", { name: /Không xóa được Nhóm 1/ })).toBeDisabled();
  });

  it("Chia ngẫu nhiên chỉ có ô sĩ số tối đa: lấp nhóm đang có còn thiếu trước, chưa lưu cho tới khi bấm Lưu", async () => {
    const calls = backend(WITH_GROUPS);
    render(<ClassGroupsPanel classId={CLASS} />);
    fireEvent.click(await screen.findByRole("button", { name: /Chia ngẫu nhiên/ }));
    const dialog = await screen.findByRole("dialog");
    expect(within(dialog).queryByRole("radio")).not.toBeInTheDocument();
    expect(within(dialog).getAllByRole("textbox")).toHaveLength(1);
    expect(within(dialog).getByRole("textbox")).toHaveValue("");
    fireEvent.change(within(dialog).getByRole("textbox"), { target: { value: "5" } });
    fireEvent.click(within(dialog).getByRole("button", { name: "Xem trước" }));

    expect(within(dialog).getByRole("cell", { name: "Nhóm 1" })).toBeInTheDocument();
    expect(calls.some((c) => c.method === "PUT")).toBe(false);
    fireEvent.click(within(dialog).getByRole("button", { name: "Lưu 1 nhóm" }));
    await waitFor(() => expect(calls.some((c) => c.method === "PUT")).toBe(true));
    expect(putBody(calls).groups).toHaveLength(1);
    expect([...putBody(calls).groups[0]!.memberIds].sort()).toEqual(["a", "b", "c", "d"]);
  });

  it("Chia ngẫu nhiên: nhóm đang có đã đủ sĩ số thì tạo nhóm mới cho người còn lại", async () => {
    const calls = backend(WITH_GROUPS);
    render(<ClassGroupsPanel classId={CLASS} />);
    fireEvent.click(await screen.findByRole("button", { name: /Chia ngẫu nhiên/ }));
    const dialog = await screen.findByRole("dialog");
    fireEvent.change(within(dialog).getByRole("textbox"), { target: { value: "2" } });
    fireEvent.click(within(dialog).getByRole("button", { name: "Xem trước" }));
    fireEvent.click(within(dialog).getByRole("button", { name: "Lưu 1 nhóm" }));

    await waitFor(() => expect(calls.some((c) => c.method === "PUT")).toBe(true));
    const groups = putBody(calls).groups;
    expect(groups[0]!.memberIds).toEqual(["a", "b"]);
    expect(groups[1]!.name).toBe("Nhóm 2");
    expect([...groups[1]!.memberIds].sort()).toEqual(["c", "d"]);
    expect(groups[1]!.memberIds).toContain(groups[1]!.leaderId);
  });

  it("Chia ngẫu nhiên khi ai cũng đã có nhóm thì không chia được", async () => {
    backend({ ...WITH_GROUPS, ungrouped: [], pendingRequests: [] });
    render(<ClassGroupsPanel classId={CLASS} students={[A, B]} />);
    fireEvent.click(await screen.findByRole("button", { name: /Chia ngẫu nhiên/ }));
    const dialog = await screen.findByRole("dialog");
    expect(within(dialog).getByRole("button", { name: "Xem trước" })).toBeDisabled();
  });

  it("lớp lưu trữ chỉ xem: không có nút tạo, chia, sửa, xóa nhóm", async () => {
    backend({ ...WITH_GROUPS, classStatus: "ARCHIVED", pendingRequests: [] });
    render(<ClassGroupsPanel classId={CLASS} />);
    await screen.findByRole("list", { name: "Danh sách nhóm" });
    expect(screen.queryByRole("button", { name: /Tạo nhóm/ })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /Chia ngẫu nhiên/ })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Sửa Nhóm 1" })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Xóa Nhóm 1" })).not.toBeInTheDocument();
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
