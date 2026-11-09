import { act, fireEvent, render, screen, within } from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { groupDocContext } from "@/features/groupdocs/components/groupDocNav";
import { findSectionForHeading } from "@/features/groupdocs/model/outline";
import { GroupWorkspacePage } from "@/features/groupdocs/pages/GroupWorkspacePage";
import type { SectionView, WorkspaceView } from "@/features/groupdocs/model/types";
import { publishOutline } from "@/shared/document/outline";
import type { DocumentBlock } from "@/shared/document";

vi.mock("next/link", () => ({
  default: ({ href, children, ...rest }: { href: string; children: React.ReactNode }) => (
    <a href={href} {...rest}>
      {children}
    </a>
  ),
}));

const heading = (id: string, style: "H1" | "H2" | "H3", text: string): DocumentBlock => ({ id, origin: "TEACHER", type: "HEADING", content: { style, text } });
// "1. Hệ thống" là heading cha (không phải mục); 1.1, 1.2 là mục; 1.2.1 là heading con trong mục 1.2.
const blocks: DocumentBlock[] = [heading("h0", "H1", "Hệ thống"), heading("p1", "H2", "1.1. Đặc tả"), heading("p2", "H2", "Thiết kế"), heading("p2a", "H3", "Sơ đồ lớp")];
const section = (id: string, partId: string, title: string): SectionView => ({
  id, partId, title, orderNo: 1, status: "OPEN", holderId: null, assignedBy: null, publishedBlocks: null, draftBlocks: null,
  draftSavedAt: null, lastAuthorId: null, version: 1,
});
const sections = [section("s1", "p1", "Đặc tả"), section("s2", "p2", "Thiết kế")];

afterEach(() => {
  act(() => publishOutline(null));
  vi.unstubAllGlobals();
});

it("dòng trên sidebar trỏ tới mục chứa nó: chính mục, mục cha, hoặc mục đầu tiên sau heading cha", () => {
  expect(findSectionForHeading(blocks, sections, "p2")?.id).toBe("s2");
  expect(findSectionForHeading(blocks, sections, "p2a")?.id).toBe("s2");
  expect(findSectionForHeading(blocks, sections, "h0")?.id).toBe("s1");
  expect(findSectionForHeading(blocks, sections, "khác")).toBeNull();
});

it("sidebar chỉ thay menu ở trang tài liệu nhóm", () => {
  expect(groupDocContext({ pathname: "/group-docs/d1", search: new URLSearchParams() })?.label).toBe("Các phần của bài");
  expect(groupDocContext({ pathname: "/group-docs/d1/x", search: new URLSearchParams() })).toBeNull();
  expect(groupDocContext({ pathname: "/attempts/a1", search: new URLSearchParams() })).toBeNull();
});

it("trang tài liệu nhóm đăng mục lục: lối về Bài tập, không có nút thêm mục; bấm dòng thì cuộn tới mục", async () => {
  const view: WorkspaceView = {
    documentId: "doc1", assignmentId: "as1", assignmentTitle: "Lab", closesAt: null, lateUntil: null, classId: "c1", groupId: "g1",
    groupName: "Nhóm 1", leaderId: "m1", members: [{ accountId: "m1", name: "An", leader: true }], status: "IN_PROGRESS", window: "ON_TIME",
    sharedBlocks: { blocks }, sections, role: { member: true, leader: true, teacher: false }, lastSubmission: null, activityMonitoring: false,
    diagramEnabled: false, version: 1, viewerId: "m1",
  };
  vi.stubGlobal("fetch", vi.fn(() => Promise.resolve(new Response(JSON.stringify(view), { status: 200, headers: { "Content-Type": "application/json" } }))));
  const scrolled: string[] = [];
  Element.prototype.scrollIntoView = function (this: Element) { scrolled.push(this.id); };
  const nav = groupDocContext({ pathname: "/group-docs/doc1", search: new URLSearchParams() })!;
  render(<>{nav.header}<GroupWorkspacePage documentId="doc1" /></>);

  const outline = await screen.findByTestId("assignment-outline");
  expect(screen.getByRole("link", { name: /Bài tập/ })).toHaveAttribute("href", "/my-assignments/as1?classId=c1");
  expect(screen.queryByRole("button", { name: "Thêm mục" })).not.toBeInTheDocument();
  expect(screen.queryByRole("link", { name: "Thoát" })).not.toBeInTheDocument();
  // Heading tự gõ số trùng số sidebar thì không lặp lại.
  // Tên nút gồm số khi thu gọn sidebar rồi "số tiêu đề".
  expect(within(outline).getByRole("button", { name: /^1\.1\.\s*1\.1\. Đặc tả$/ })).toBeInTheDocument();
  fireEvent.click(within(outline).getByRole("button", { name: /1\.2\.1\. Sơ đồ lớp/ }));
  await vi.waitFor(() => expect(scrolled).toContain("section-s2"));
});
