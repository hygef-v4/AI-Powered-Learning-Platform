import { act, fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { GroupWorkspacePage } from "@/features/groupdocs/pages/GroupWorkspacePage";
import { Toaster } from "@/shared/ui";
import type { SectionView, WorkspaceView } from "@/features/groupdocs/model/types";

const router = vi.hoisted(() => ({ push: vi.fn() }));
vi.mock("next/navigation", () => ({ useRouter: () => router }));

vi.mock("next/link", () => ({
  default: ({ href, children, ...rest }: { href: string; children: React.ReactNode }) => (
    <a href={href} {...rest}>
      {children}
    </a>
  ),
}));

const LEADER = "leader";
const MEMBER = "member";

function section(id: string, status: SectionView["status"], holderId: string | null, extra: Partial<SectionView> = {}): SectionView {
  return {
    id,
    partId: id,
    title: `Mục ${id}`,
    orderNo: 1,
    status,
    holderId,
    assignedBy: null,
    publishedBlocks: null,
    draftBlocks: null,
    draftSavedAt: null,
    lastAuthorId: null,
    version: 2,
    ...extra,
  };
}

function workspace(viewerId: string, overrides: Partial<WorkspaceView> = {}): WorkspaceView {
  return {
    documentId: "doc1",
    assignmentId: "as1",
    assignmentTitle: "Sơ đồ lớp",
    closesAt: "2026-10-30T10:00:00Z",
    lateUntil: null,
    classId: "c1",
    groupId: "g1",
    groupName: "Nhóm 1",
    leaderId: LEADER,
    members: [
      { accountId: LEADER, name: "An", leader: true },
      { accountId: MEMBER, name: "Bình", leader: false },
    ],
    status: "IN_PROGRESS",
    window: "ON_TIME",
    sharedBlocks: { blocks: [{ id: "h1", origin: "TEACHER", type: "HEADING", content: { style: "H1", text: "1. Sơ đồ" } }] },
    sections: [
      section("s1", "DONE", null, {
        lastAuthorId: LEADER,
        publishedBlocks: [{ id: "b1", origin: "STUDENT", type: "PARAGRAPH", content: { runs: [{ text: "Nội dung đã xong" }] } }],
      }),
      section("s2", "CLAIMED", MEMBER),
      section("s3", "OPEN", null),
    ],
    role: { member: true, leader: viewerId === LEADER, teacher: false },
    lastSubmission: null,
    activityMonitoring: false,
    diagramEnabled: false,
    version: 5,
    viewerId,
    ...overrides,
  };
}

function json(body: unknown, status = 200) {
  return Promise.resolve(new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } }));
}

function backend(view: WorkspaceView) {
  const calls: { url: string; method: string; body?: unknown }[] = [];
  vi.stubGlobal(
    "fetch",
    vi.fn((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      const method = init?.method ?? "GET";
      calls.push({ url, method, body: typeof init?.body === "string" ? JSON.parse(init.body) : init?.body });
      if (url === "/api/v1/group-docs/doc1" && method === "GET") return json(view);
      if (url.endsWith(":claim")) {
        const id = url.match(/sections\/([^/:]+):claim/)![1]!;
        // Như server: nhận mục tăng version.
        const version = (view.sections.find((s) => s.id === id)?.version ?? 1) + 1;
        return json(section(id, "CLAIMED", view.viewerId, { version }));
      }
      if (url.endsWith(":release")) return json(section("s2", "OPEN", null));
      if (url.endsWith(":assign")) return json(section("s3", "CLAIMED", MEMBER));
      if (url.endsWith("/revisions"))
        return json([
          { id: "r2", authorId: MEMBER, kind: "DONE", createdAt: "2026-10-22T04:00:00Z", blocks: [{ id: "x2", origin: "STUDENT", type: "PARAGRAPH", content: { runs: [{ text: "Bản nháp cũ" }] } }] },
          { id: "r1", authorId: LEADER, kind: "DONE", createdAt: "2026-10-22T03:00:00Z", blocks: [] },
        ]);
      if (url.endsWith("/submit"))
        return json({ documentId: "doc1", mode: "MANUAL", submittedAt: "2026-10-22T03:00:00Z", late: false, receiptHash: "sha256:abc", submittedBy: LEADER, warnings: [] });
      return json({ status: 404 }, 404);
    }),
  );
  return calls;
}

describe("GroupWorkspacePage", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    vi.useRealTimers();
    router.push.mockReset();
  });

  it("thành viên thấy nội dung đã Xong, nhận mục chưa làm là soạn ngay, không có nút nộp", async () => {
    const calls = backend(workspace(MEMBER));
    render(<GroupWorkspacePage documentId="doc1" />);

    expect(await screen.findByText("Nội dung đã xong")).toBeInTheDocument();
    expect(screen.queryByText("Trưởng nhóm nộp bài")).not.toBeInTheDocument();
    expect(screen.getByTestId("assignment-kind")).toHaveTextContent(/^Nhóm$/);
    expect(screen.queryByRole("button", { name: /Nộp tài liệu/ })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /Chốt phân công|Nhập từ file/ })).not.toBeInTheDocument();
    expect(screen.queryByRole("tab", { name: "Đánh giá chéo" })).not.toBeInTheDocument();
    const item = (title: string) => screen.getByRole("region", { name: title });
    const node = (title: string) => screen.getByRole("group", { name: title });
    expect(within(item("Mục s1")).getByText("An")).toBeInTheDocument();
    expect(within(item("Mục s2")).getByText("Bạn")).toBeInTheDocument();
    expect(within(item("Mục s3")).getByText("Chưa ai nhận")).toBeInTheDocument();
    // Thẻ ghi số câu như thẻ rubric.
    expect(within(item("Mục s1")).getByText("I.1. Mục s1")).toBeInTheDocument();
    expect(within(node("Mục s1")).getByText("Xong")).toBeInTheDocument();
    expect(within(node("Mục s3")).getByText("Chưa làm")).toBeInTheDocument();
    expect(within(node("Mục s3")).getByRole("button", { name: "Lịch sử" })).toBeInTheDocument();
    expect(within(item("Mục s2")).getByRole("button", { name: "Soạn tiếp" })).toBeInTheDocument();

    fireEvent.click(within(item("Mục s3")).getByRole("button", { name: "Nhận mục" }));
    await waitFor(() => expect(calls.some((c) => c.url.endsWith("/sections/s3:claim"))).toBe(true));
    // Soạn ở trang riêng, mở đúng mục vừa nhận.
    await waitFor(() => expect(router.push).toHaveBeenCalledWith("/group-docs/doc1/edit?section=s3"));
    fireEvent.click(within(item("Mục s2")).getByRole("button", { name: "Soạn tiếp" }));
    expect(router.push).toHaveBeenLastCalledWith("/group-docs/doc1/edit?section=s2");
  });

  it("mục đã Xong vẫn nhận lại để sửa (BR-U12-33); mục người khác viết thì hỏi lại trước", async () => {
    const calls = backend(workspace(MEMBER));
    render(<GroupWorkspacePage documentId="doc1" />);
    const item = await screen.findByRole("region", { name: "Mục s1" });

    fireEvent.click(within(item).getByRole("button", { name: "Sửa mục" }));
    const confirm = await screen.findByRole("dialog", { name: "Sửa mục" });
    expect(within(confirm).getByText("Mục hiện tại đã có người làm.")).toBeInTheDocument();
    expect(calls.some((c) => c.url.endsWith(":claim"))).toBe(false);

    // Quay lại thì không nhận mục.
    fireEvent.click(within(confirm).getByRole("button", { name: "Quay lại" }));
    await waitFor(() => expect(screen.queryByRole("dialog", { name: "Sửa mục" })).not.toBeInTheDocument());
    expect(calls.some((c) => c.url.endsWith(":claim"))).toBe(false);

    // Xác nhận thì nhận và sang trang soạn.
    fireEvent.click(within(item).getByRole("button", { name: "Sửa mục" }));
    fireEvent.click(within(await screen.findByRole("dialog", { name: "Sửa mục" })).getByRole("button", { name: "Sửa mục" }));
    await waitFor(() => expect(calls.some((c) => c.url.endsWith("/sections/s1:claim"))).toBe(true));
    await waitFor(() => expect(router.push).toHaveBeenCalledWith("/group-docs/doc1/edit?section=s1"));
  });

  it("mục người khác đang soạn: thành viên chỉ xem; trưởng nhóm nhả mục sau khi xác nhận", async () => {
    const view = workspace(LEADER, { sections: [section("s2", "CLAIMED", MEMBER)] });
    const calls = backend(view);
    render(<GroupWorkspacePage documentId="doc1" />);

    const item = await screen.findByRole("region", { name: "Mục s2" });
    expect(within(item).queryByRole("button", { name: /Nhận mục|Sửa mục|Soạn tiếp/ })).not.toBeInTheDocument();
    expect(calls.some((c) => c.url.endsWith(":claim"))).toBe(false);
    fireEvent.click(within(item).getByRole("button", { name: "Nhả mục" }));
    const dialog = await screen.findByRole("dialog", { name: "Nhả mục" });
    expect(within(dialog).queryByText(/Bản nháp/)).not.toBeInTheDocument();
    fireEvent.click(within(dialog).getByRole("button", { name: "Nhả mục" }));
    await waitFor(() => expect(calls.some((c) => c.url.endsWith("/sections/s2:release"))).toBe(true));
  });

  it("trưởng nhóm giao mục: người được giao soạn ngay; mục đang có người giữ thì cảnh báo nhả trước", async () => {
    const calls = backend(workspace(LEADER));
    render(<GroupWorkspacePage documentId="doc1" />);
    const item = (title: string) => screen.getByRole("region", { name: title });
    await screen.findByText("Nội dung đã xong");

    fireEvent.click(within(item("Mục s2")).getByRole("button", { name: "Giao mục" }));
    const held = await screen.findByRole("dialog", { name: "Giao mục" });
    // Ô chọn ghi sẵn người đang giữ; chưa chọn người khác thì chưa giao được.
    expect(within(held).getByLabelText("Thành viên nhận mục")).toHaveAttribute("placeholder", "Bình");
    expect(within(held).getByText("Giao cho người khác sẽ nhả mục trước đó.")).toBeInTheDocument();
    expect(within(held).getByRole("button", { name: "Giao mục" })).toBeDisabled();
    fireEvent.click(within(held).getByRole("button", { name: "Đóng" }));

    fireEvent.click(within(item("Mục s3")).getByRole("button", { name: "Giao mục" }));
    const dialog = await screen.findByRole("dialog", { name: "Giao mục" });
    expect(within(dialog).queryByRole("alert")).not.toBeInTheDocument();
    fireEvent.change(within(dialog).getByLabelText("Thành viên nhận mục"), { target: { value: "Bì" } });
    fireEvent.mouseDown(await screen.findByRole("option", { name: "Bình" }));
    fireEvent.click(within(dialog).getByRole("button", { name: "Giao mục" }));
    await waitFor(() => expect(calls.find((c) => c.url.endsWith("/sections/s3:assign"))?.body).toEqual({ assigneeId: MEMBER, replaceHolder: false }));
  });

  it("lịch sử mục chỉ xem: hiện từng bản với tác giả, không có khôi phục", async () => {
    backend(workspace(LEADER));
    render(<GroupWorkspacePage documentId="doc1" />);
    const item = await screen.findByRole("group", { name: "Mục s2" });

    fireEvent.click(within(item).getByRole("button", { name: "Lịch sử" }));

    const dialog = await screen.findByRole("dialog", { name: "Lịch sử" });
    // Bên trái chỉ ghi lúc lưu và người viết, không ghi Xong hay chưa.
    expect(await within(dialog).findByText("Bình")).toBeInTheDocument();
    expect(within(dialog).queryByText(/Xong/)).not.toBeInTheDocument();
    expect(within(dialog).getByText("Bản nháp cũ")).toBeInTheDocument();
    expect(within(dialog).queryByRole("button", { name: /Dùng bản này/ })).not.toBeInTheDocument();
  });

  it("giảng viên bấm mục không nhận được", async () => {
    const calls = backend(workspace("teacher", { role: { member: false, leader: false, teacher: true } }));
    render(<GroupWorkspacePage documentId="doc1" />);

    const item = await screen.findByRole("group", { name: "Mục s3" });
    expect(within(item).getByRole("button", { name: "Lịch sử" })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /Nhận mục|Sửa mục|Giao mục/ })).not.toBeInTheDocument();
    expect(calls.some((c) => c.url.endsWith(":claim"))).toBe(false);
  });

  it("tự nộp khi hết hạn khóa trang và ghi giờ hết hạn", async () => {
    let source: { onmessage: ((m: { data: string }) => void) | null } | null = null;
    vi.stubGlobal(
      "EventSource",
      class {
        onmessage: ((m: { data: string }) => void) | null = null;
        onerror = null;
        onopen = null;
        constructor() {
          // eslint-disable-next-line @typescript-eslint/no-this-alias
          source = this;
        }
        close() {}
      },
    );
    let resolveReload: (() => void) | null = null;
    let first = true;
    const view = workspace(MEMBER);
    vi.stubGlobal(
      "fetch",
      vi.fn(() => {
        if (first) {
          first = false;
          return json(view);
        }
        return new Promise<Response>((resolve) => {
          resolveReload = () => resolve(new Response(JSON.stringify(view), { status: 200, headers: { "Content-Type": "application/json" } }));
        });
      }),
    );
    render(<GroupWorkspacePage documentId="doc1" />);
    await screen.findByText("Nội dung đã xong");
    // Kênh sự kiện mở trong effect riêng, có thể sau khi nội dung đã hiện: chờ kênh sẵn sàng rồi mới bắn sự kiện.
    await waitFor(() => expect(source?.onmessage).toBeTypeOf("function"));

    act(() => source!.onmessage!({ data: JSON.stringify({ type: "document.auto-submitted" }) }));
    const lock = await screen.findByRole("alertdialog", { name: "Đang nộp" });
    expect(lock).toHaveTextContent(/Đã hết hạn nộp lúc 30\/10\/2026 17:00/);
    expect(lock.parentElement).toBe(document.body);
    await act(async () => resolveReload!());
    await waitFor(() => expect(screen.queryByRole("alertdialog")).not.toBeInTheDocument());
  });

  it("trưởng nhóm nộp được, popup liệt kê mục chưa Xong và gửi version", async () => {
    const calls = backend(workspace(LEADER));
    render(<GroupWorkspacePage documentId="doc1" />);
    fireEvent.click(await screen.findByRole("button", { name: /Nộp tài liệu/ }));

    const dialog = await screen.findByRole("dialog");
    expect(within(dialog).getByText("Mục s3")).toBeInTheDocument();
    expect(within(dialog).queryByText(/Chỉ nội dung của mục Xong|nộp lại trước hạn/)).not.toBeInTheDocument();
    fireEvent.click(within(dialog).getByRole("button", { name: "Vẫn nộp" }));

    await waitFor(() => expect(calls.some((c) => c.url.endsWith("/submit"))).toBe(true));
    expect(calls.find((c) => c.url.endsWith("/submit"))!.body).toEqual({ version: 5 });
    // Nộp xong về trang chi tiết bài.
    await waitFor(() => expect(router.push).toHaveBeenCalledWith("/my-assignments/as1?classId=c1"));
  });

  it("nộp lỗi thì đóng popup, báo một câu và tô đỏ các mục chưa xong", async () => {
    const view = workspace(LEADER);
    backend(view);
    const fetchMock = globalThis.fetch as ReturnType<typeof vi.fn>;
    const base = fetchMock.getMockImplementation() as (input: RequestInfo | URL, init?: RequestInit) => Promise<Response>;
    fetchMock.mockImplementation((input: RequestInfo | URL, init?: RequestInit) => String(input).endsWith("/submit")
      ? json({ status: 422, code: "SUBMIT_INVALID", detail: "Bài chưa hợp lệ", issues: [{ code: "EMPTY", message: "Phần trống", blockId: "s3" }] }, 422)
      : base(input, init));
    render(<><Toaster /><GroupWorkspacePage documentId="doc1" /></>);
    fireEvent.click(await screen.findByRole("button", { name: /Nộp tài liệu/ }));
    fireEvent.click(within(await screen.findByRole("dialog")).getByRole("button", { name: "Vẫn nộp" }));

    expect(await screen.findByText("Chưa nộp được vì còn mục chưa xong.")).toBeInTheDocument();
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    expect(screen.getByRole("region", { name: "Mục s3" })).toHaveAttribute("data-invalid", "true");
    expect(screen.getByRole("region", { name: "Mục s2" })).toHaveAttribute("data-invalid", "true");
    expect(screen.getByRole("region", { name: "Mục s1" })).not.toHaveAttribute("data-invalid");
    expect(within(screen.getByRole("region", { name: "Mục s3" })).getByText("Mục này chưa xong.")).toBeInTheDocument();

    // Nhả mục s2 (trưởng nhóm) thì mục đó hết đỏ, mục khác vẫn đỏ.
    fireEvent.click(within(screen.getByRole("region", { name: "Mục s2" })).getByRole("button", { name: "Nhả mục" }));
    fireEvent.click(within(await screen.findByRole("dialog", { name: /Nhả mục/ })).getByRole("button", { name: "Nhả mục" }));
    await waitFor(() => expect(screen.getByRole("region", { name: "Mục s2" })).not.toHaveAttribute("data-invalid"));
    expect(screen.getByRole("region", { name: "Mục s3" })).toHaveAttribute("data-invalid", "true");
  });

  it("đã nộp rồi vẫn nộp lại được ngay, không có dòng báo giờ nộp", async () => {
    const lastSubmission = { submittedBy: LEADER, mode: "MANUAL", submittedAt: "2026-10-22T03:00:00Z", late: false, receiptHash: "h" };
    backend(workspace(LEADER, { lastSubmission }));
    render(<GroupWorkspacePage documentId="doc1" />);
    expect(await screen.findByRole("button", { name: /Nộp tài liệu/ })).toBeEnabled();
    expect(screen.queryByText(/Đã nộp lúc/)).not.toBeInTheDocument();
  });

  it("nhóm đã nộp: chỉ tải DOCX hiện tại, hết hạn thì không còn nút nhận mục", async () => {
    backend(workspace(MEMBER, {
      lastSubmission: { submittedBy: null, mode: "AUTO_DEADLINE", submittedAt: "2026-10-22T03:00:00Z", late: true, receiptHash: "sha256:xyz" },
      status: "CLOSED",
      window: "CLOSED",
    }));
    render(<GroupWorkspacePage documentId="doc1" />);

    await screen.findByText("Nội dung đã xong");
    expect(screen.queryByText(/Đã nộp lúc/)).not.toBeInTheDocument();
    expect(screen.queryByRole("tab", { name: "Bản nộp" })).not.toBeInTheDocument();
    fireEvent.click(screen.getByLabelText("Thao tác khác"));
    expect(screen.getByRole("menuitem", { name: "Tải DOCX" })).toBeInTheDocument();
    expect(screen.queryByRole("menuitem", { name: /bản nộp/ })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Nhận mục" })).not.toBeInTheDocument();
  });

  it("nhả mục là nút chữ đỏ", async () => {
    backend(workspace(LEADER));
    render(<GroupWorkspacePage documentId="doc1" />);
    const item = await screen.findByRole("region", { name: "Mục s2" });
    expect(within(item).getByRole("button", { name: "Nhả mục" }).className).toContain("text-danger");
  });

  it("nhận mục thì sang trang soạn", async () => {
    const calls = backend(workspace(MEMBER));
    render(<GroupWorkspacePage documentId="doc1" />);
    const item = await screen.findByRole("region", { name: "Mục s3" });

    fireEvent.click(within(item).getByRole("button", { name: "Nhận mục" }));

    await waitFor(() => expect(calls.some((c) => c.url.endsWith("/sections/s3:claim"))).toBe(true));
    await waitFor(() => expect(router.push).toHaveBeenCalledWith("/group-docs/doc1/edit?section=s3"));
  });

  it("mục Xong do chính mình viết thì nhận lại ngay, không hỏi", async () => {
    const calls = backend(workspace(LEADER));
    render(<GroupWorkspacePage documentId="doc1" />);
    const item = await screen.findByRole("region", { name: "Mục s1" });

    fireEvent.click(within(item).getByRole("button", { name: "Sửa mục" }));

    await waitFor(() => expect(calls.some((c) => c.url.endsWith("/sections/s1:claim"))).toBe(true));
    expect(screen.queryByRole("dialog", { name: /Sửa mục Mục s1/ })).not.toBeInTheDocument();
  });

  it("người làm chỉ ghi tên, không ghi lúc nhận hay lần lưu cuối, không lộ bản nháp", async () => {
    backend(
      workspace(LEADER, {
        sections: [
          section("s2", "CLAIMED", MEMBER, { claimedAt: "2026-10-30T07:30:00Z", holderSavedAt: "2026-10-30T07:57:00Z" }),
          section("s3", "CLAIMED", MEMBER, { claimedAt: "2026-10-30T06:00:00Z", holderSavedAt: null, draftBlocks: [
            { id: "d1", origin: "STUDENT", type: "PARAGRAPH", content: { runs: [{ text: "Bản nháp riêng" }] } },
          ] }),
        ],
      }),
    );
    render(<GroupWorkspacePage documentId="doc1" />);

    const card = await screen.findByRole("region", { name: "Mục s2" });
    expect(card).toHaveTextContent("Người làmBình");
    expect(screen.queryByText(/lưu lần cuối|chưa lưu/)).not.toBeInTheDocument();
    expect(screen.queryByText("Bản nháp riêng")).not.toBeInTheDocument();
  });

  it("còn ≤ 30 phút thì nhắc hạn kèm mục chưa Xong; xa hạn thì không nhắc", async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true, now: new Date("2026-10-30T09:42:00Z") });
    backend(workspace(MEMBER, { closesAt: "2026-10-30T10:00:00Z" }));
    const { unmount } = render(<GroupWorkspacePage documentId="doc1" />);

    expect(await screen.findByText(/Còn 18 phút tới hạn nộp/)).toBeInTheDocument();
    expect(screen.getByText("1 mục chưa ai làm, sẽ không vào bản nộp.")).toBeInTheDocument();
    expect(screen.getByText(/1 mục đang soạn: hãy bấm Xong/)).toBeInTheDocument();
    expect(screen.getByText(/Nhóm chưa nộp bản nào/)).toBeInTheDocument();
    unmount();

    backend(workspace(MEMBER, { closesAt: "2026-10-30T12:00:00Z" }));
    render(<GroupWorkspacePage documentId="doc1" />);
    await screen.findByText("Nội dung đã xong");
    expect(screen.queryByText(/tới hạn nộp/)).not.toBeInTheDocument();
  });

  it("trong khoảng nộp trễ thì báo bị tính trễ và còn bao lâu tới khi đóng nhận bài", async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true, now: new Date("2026-10-30T10:20:00Z") });
    backend(
      workspace(MEMBER, {
        window: "LATE",
        lateUntil: "2026-10-30T11:00:00Z",
        sections: [section("s1", "DONE", null, { lastAuthorId: LEADER })],
      }),
    );
    render(<GroupWorkspacePage documentId="doc1" />);

    expect(await screen.findByText(/nộp bây giờ bị tính trễ\. Còn 40 phút tới khi đóng nhận bài/)).toBeInTheDocument();
    expect(screen.getByText("Mọi mục đã Xong.")).toBeInTheDocument();
  });
});
