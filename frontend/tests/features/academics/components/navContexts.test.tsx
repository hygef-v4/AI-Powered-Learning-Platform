import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import "@/features/register";
import { AppShell } from "@/shared/layout/AppShell";
import type { NavItem } from "@/shared/layout/navigation";
import { resolveClassContext, resolveStudentClassContext, resolveSubjectContext } from "@/features/academics/components/ClassNavContexts";

let location = { pathname: "/manager", search: "" };
vi.mock("next/navigation", () => ({
  usePathname: () => location.pathname,
  useSearchParams: () => new URLSearchParams(location.search),
  useRouter: () => ({ push: vi.fn(), replace: vi.fn() }),
}));

const at = (pathname: string, search = "") => ({ pathname, search: new URLSearchParams(search) });
const labels = (items: NavItem[]) => items.map((item) => item.label);
const activeLabel = (items: NavItem[]) => items.find((item) => item.active)?.label;

describe("sidebar của môn", () => {
  it("mở nhánh môn ở các màn của một môn, mọi link mang môn đang mở", () => {
    expect(resolveSubjectContext(at("/manager"))!.items).toEqual([]);
    expect(resolveSubjectContext(at("/teaching/classes"))).toBeNull();

    const subject = resolveSubjectContext(at("/manager/subjects/s-1"))!;
    expect(labels(subject.items)).toEqual(["Thông tin môn", "Lớp học", "Học liệu", "Bài tập", "Ngân hàng câu hỏi"]);
    expect(subject.items.map((item) => item.href)).toEqual([
      "/manager/subjects/s-1",
      "/manager/classes?subjectId=s-1",
      "/manager/materials?subjectId=s-1",
      "/manager/assignments?subjectId=s-1",
      "/manager/questions?subjectId=s-1",
    ]);
    expect(activeLabel(subject.items)).toBe("Thông tin môn");
    expect(activeLabel(resolveSubjectContext(at("/manager/questions", "subjectId=s-2"))!.items)).toBe("Ngân hàng câu hỏi");
  });

  it("trang không mang ?subjectId= vẫn giữ môn mở gần nhất", () => {
    resolveSubjectContext(at("/manager/assignments", "subjectId=s-9"));
    const detail = resolveSubjectContext(at("/manager/questions/q-1"))!;
    expect(detail.items[0]!.href).toBe("/manager/subjects/s-9");
    expect(activeLabel(detail.items)).toBe("Ngân hàng câu hỏi");
  });
});

describe("sidebar của lớp", () => {
  it("lớp được phân công có các tab của trang lớp, mở đúng tab theo ?tab=", () => {
    const teaching = resolveClassContext(at("/teaching/classes/c-1", "tab=materials"))!;
    expect(labels(teaching.items)).toEqual(["Thông tin lớp", "Sinh viên", "Học liệu", "Bài tập", "Chấm điểm", "Thông báo lớp"]);
    expect(teaching.items[5]!.href).toBe("/teaching/classes/c-1/announcements");
    expect(teaching.items[0]!.href).toBe("/teaching/classes/c-1");
    expect(teaching.items[2]!.href).toBe("/teaching/classes/c-1?tab=materials");
    expect(activeLabel(teaching.items)).toBe("Học liệu");
    expect(activeLabel(resolveClassContext(at("/teaching/classes/c-1", "tab=nope"))!.items)).toBe("Thông tin lớp");
    expect(activeLabel(resolveClassContext(at("/teaching/classes/c-1/assignments/a-1/submissions"))!.items)).toBe("Bài tập");
    expect(resolveClassContext(at("/teaching/classes"))!.items).toEqual([]);
  });

  it("Thông báo lớp của một lớp vẫn ở sidebar của lớp đó", () => {
    expect(activeLabel(resolveClassContext(at("/teaching/classes/c-3/announcements"))!.items)).toBe("Thông báo lớp");
    // Thông báo lớp ở menu chính (mọi lớp) giữ menu chung, kể cả link cũ `?classId=`.
    expect(resolveClassContext(at("/announcements"))).toBeNull();
    expect(resolveClassContext(at("/announcements", "classId=c-3"))).toBeNull();
  });

  it("lớp của tôi (Sinh viên) có các trang sinh viên vào được của lớp", () => {
    const own = resolveStudentClassContext(at("/classes/c-4"))!;
    expect(labels(own.items)).toEqual(["Thông tin lớp", "Nhóm của tôi", "Học liệu", "Bài tập", "Thông báo lớp"]);
    expect(own.items.map((item) => item.href)).toEqual(["/classes/c-4", "/classes/c-4/group", "/classes/c-4?tab=materials", "/my-assignments?classId=c-4", "/classes/c-4/announcements"]);
    expect(activeLabel(own.items)).toBe("Thông tin lớp");
    expect(activeLabel(resolveStudentClassContext(at("/classes/c-4/group"))!.items)).toBe("Nhóm của tôi");
    expect(activeLabel(resolveStudentClassContext(at("/classes/c-4/announcements"))!.items)).toBe("Thông báo lớp");
    expect(activeLabel(resolveStudentClassContext(at("/classes/c-4", "tab=materials"))!.items)).toBe("Học liệu");
    expect(activeLabel(resolveStudentClassContext(at("/materials/m-1", "classId=c-4"))!.items)).toBe("Học liệu");
    expect(activeLabel(resolveStudentClassContext(at("/my-assignments/a-1", "classId=c-4"))!.items)).toBe("Bài tập");
    expect(resolveStudentClassContext(at("/classes"))!.items).toEqual([]);
    expect(resolveStudentClassContext(at("/my-assignments"))).toBeNull();
  });

  it("lớp của môn (Chủ nhiệm môn) không có sidebar lớp, vẫn là sidebar của môn ở mục Lớp học", () => {
    expect(resolveClassContext(at("/manager/classes/c-2", "tab=students"))).toBeNull();
    expect(activeLabel(resolveSubjectContext(at("/manager/classes/c-2"))!.items)).toBe("Lớp học");
  });
});

describe("AppShell với cây môn, lớp trên sidebar", () => {
  afterEach(() => vi.unstubAllGlobals());

  const nav: NavItem[] = [
    { href: "/manager", label: "Môn được phân công", icon: "menu_book" },
    { href: "/teaching/classes", label: "Lớp được phân công", icon: "class" },
  ];
  const resolve = (where: { pathname: string; search: URLSearchParams }) => resolveClassContext(where) ?? resolveSubjectContext(where);

  it("ở trang của lớp thì giữ menu role; dưới Lớp được phân công là ô chọn lớp rồi các mục của lớp đang mở", async () => {
    const value = { id: "c-1", code: "SE1801", name: "Lớp chiều thứ Hai", subject: { id: "s-1", code: "SWR302", name: "Software Requirement" } };
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json([value, { ...value, id: "c-2", code: "SE1802" }])));
    location = { pathname: "/teaching/classes/c-1", search: "tab=gradebook" };
    render(<AppShell role="TEACHER" nav={nav} user={null} context={resolve}><p>Nội dung</p></AppShell>);

    expect(screen.getByRole("navigation", { name: "Menu Giảng viên" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /Lớp được phân công/ })).toHaveAttribute("href", "/teaching/classes");
    const branch = screen.getByRole("group", { name: "Các lớp được phân công" });
    // Ô thả xuống hiện lớp đang mở, nhánh là các mục của lớp đó.
    await waitFor(() => expect(branch).toHaveTextContent("SE1801"));
    expect(screen.getByRole("menuitemradio", { name: /SE1801/ })).toHaveAttribute("aria-checked", "true");
    expect(screen.getByRole("link", { name: "Chấm điểm" })).toHaveAttribute("aria-current", "page");
    expect(screen.getByRole("link", { name: "Chấm điểm" })).toHaveAttribute("href", "/teaching/classes/c-1?tab=gradebook");
    // Đang ở Chấm điểm của lớp này: chọn lớp khác trong ô thì sang luôn Chấm điểm của lớp đó.
    expect(screen.getByRole("menuitemradio", { name: /SE1802/ })).toHaveAttribute("href", "/teaching/classes/c-2?tab=gradebook");
  });

  it("ở trang của môn thì giữ menu role, nhánh môn mở dưới Môn được phân công", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json([{ id: "s-1", code: "SWR302", name: "Software Requirement" }])));
    location = { pathname: "/manager/subjects/s-1", search: "" };
    render(<AppShell role="SUBJECT_MANAGER" nav={nav} user={null} context={resolve}><p>Nội dung</p></AppShell>);

    expect(screen.getByRole("navigation", { name: "Menu Chủ nhiệm môn" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /Môn được phân công/ })).toHaveAttribute("href", "/manager");
    expect(screen.getByRole("link", { name: /Môn được phân công/ })).not.toHaveAttribute("aria-current");
    expect(screen.getByRole("link", { name: "Thông tin môn" })).toHaveAttribute("aria-current", "page");
    // Ô chọn môn hiện môn đang mở.
    await waitFor(() => expect(screen.getByRole("group", { name: "Các môn được phân công" })).toHaveTextContent("Software Requirement"));
    expect(screen.getByRole("link", { name: /Lớp được phân công/ })).toBeInTheDocument();
  });

  it("mũi tên của mục cha chỉ mở, đóng nhánh, không chuyển trang: đang ở lớp vẫn mở được nhánh môn để vào Bài tập của môn", async () => {
    const subject = { id: "s-1", code: "SWR302", name: "Software Requirement" };
    const value = { id: "c-1", code: "SE1801", name: "Lớp", subject };
    vi.stubGlobal("fetch", vi.fn().mockImplementation((url: string) =>
      Promise.resolve(Response.json(String(url).includes("subjects") ? [subject] : [value]))));
    location = { pathname: "/teaching/classes/c-1", search: "" };
    render(<AppShell role="SUBJECT_MANAGER" nav={nav} user={null} context={resolve}><p>Nội dung</p></AppShell>);
    // Tên mục cha vẫn là link sang trang danh sách; mũi tên ở cuối hàng mới là chỗ mở nhánh.
    expect(screen.getByRole("link", { name: /Môn được phân công/ })).toHaveAttribute("href", "/manager");
    const toggle = screen.getByRole("button", { name: "Mở Môn được phân công" });
    expect(toggle).toHaveAttribute("aria-expanded", "false");
    fireEvent.click(toggle);
    expect(screen.getByRole("button", { name: "Thu gọn Môn được phân công" })).toHaveAttribute("aria-expanded", "true");
    fireEvent.click(await within(screen.getByRole("group", { name: "Các môn được phân công" })).findByRole("menuitemradio", { name: /Software Requirement/ }));
    expect(within(screen.getByRole("group", { name: "Các môn được phân công" })).getByRole("link", { name: "Bài tập" })).toHaveAttribute("href", expect.stringContaining("s-1"));
  });

  it("trang Môn được phân công hiện ô Chọn môn, chưa môn nào mở", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json([{ id: "s-1", code: "SWR302", name: "Software Requirement" }])));
    location = { pathname: "/manager", search: "" };
    render(<AppShell role="SUBJECT_MANAGER" nav={nav} user={null} context={resolve}><p>Nội dung</p></AppShell>);
    expect(screen.getByRole("link", { name: /Môn được phân công/ })).toHaveAttribute("aria-current", "page");
    expect(await screen.findByRole("menuitemradio", { name: /SWR302/ })).toHaveAttribute("aria-checked", "false");
    expect(screen.getByRole("group", { name: "Các môn được phân công" })).toHaveTextContent("Chọn môn");
    expect(screen.queryByRole("link", { name: "Thông tin môn" })).not.toBeInTheDocument();
  });
});
