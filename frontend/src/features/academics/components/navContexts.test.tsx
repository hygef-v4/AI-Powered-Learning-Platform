import { render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import "@/features/register";
import { AppShell } from "@/shared/layout/AppShell";
import type { NavItem } from "@/shared/layout/navigation";
import { classContext, studentClassContext, subjectContext } from "./navContexts";

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
  it("chỉ hiện ở các màn của một môn, mọi link mang môn đang mở", () => {
    expect(subjectContext(at("/manager"))).toBeNull();
    expect(subjectContext(at("/teaching/classes"))).toBeNull();

    const subject = subjectContext(at("/manager/subjects/s-1"))!;
    expect(labels(subject.items)).toEqual(["Thông tin môn", "Lớp học", "Học liệu", "Bài tập", "Ngân hàng câu hỏi"]);
    expect(subject.items.map((item) => item.href)).toEqual([
      "/manager/subjects/s-1",
      "/manager/classes?subjectId=s-1",
      "/manager/materials?subjectId=s-1",
      "/manager/assignments?subjectId=s-1",
      "/manager/questions?subjectId=s-1",
    ]);
    expect(activeLabel(subject.items)).toBe("Thông tin môn");
    expect(activeLabel(subjectContext(at("/manager/questions", "subjectId=s-2"))!.items)).toBe("Ngân hàng câu hỏi");
  });

  it("trang không mang ?subjectId= vẫn giữ môn mở gần nhất", () => {
    subjectContext(at("/manager/assignments", "subjectId=s-9"));
    const detail = subjectContext(at("/manager/questions/q-1"))!;
    expect(detail.items[0]!.href).toBe("/manager/subjects/s-9");
    expect(activeLabel(detail.items)).toBe("Ngân hàng câu hỏi");
  });
});

describe("sidebar của lớp", () => {
  it("lớp được phân công có các tab của trang lớp, mở đúng tab theo ?tab=", () => {
    const teaching = classContext(at("/teaching/classes/c-1", "tab=materials"))!;
    expect(labels(teaching.items)).toEqual(["Thông tin lớp", "Sinh viên", "Học liệu", "Bài tập", "Chấm điểm", "Thông báo lớp"]);
    expect(teaching.items[5]!.href).toBe("/teaching/classes/c-1/announcements");
    expect(teaching.items[0]!.href).toBe("/teaching/classes/c-1");
    expect(teaching.items[2]!.href).toBe("/teaching/classes/c-1?tab=materials");
    expect(activeLabel(teaching.items)).toBe("Học liệu");
    expect(activeLabel(classContext(at("/teaching/classes/c-1", "tab=nope"))!.items)).toBe("Thông tin lớp");
    expect(activeLabel(classContext(at("/teaching/classes/c-1/assignments/a-1/submissions"))!.items)).toBe("Bài tập");
    expect(classContext(at("/teaching/classes"))).toBeNull();
  });

  it("Thông báo lớp của một lớp vẫn ở sidebar của lớp đó", () => {
    expect(activeLabel(classContext(at("/teaching/classes/c-3/announcements"))!.items)).toBe("Thông báo lớp");
    // Thông báo lớp ở menu chính (mọi lớp) giữ menu chung, kể cả link cũ `?classId=`.
    expect(classContext(at("/announcements"))).toBeNull();
    expect(classContext(at("/announcements", "classId=c-3"))).toBeNull();
  });

  it("lớp của tôi (Sinh viên) có các trang sinh viên vào được của lớp", () => {
    const own = studentClassContext(at("/classes/c-4"))!;
    expect(labels(own.items)).toEqual(["Thông tin lớp", "Nhóm của tôi", "Học liệu", "Bài tập", "Thông báo lớp"]);
    expect(own.items.map((item) => item.href)).toEqual(["/classes/c-4", "/classes/c-4/group", "/classes/c-4?tab=materials", "/my-assignments?classId=c-4", "/classes/c-4/announcements"]);
    expect(activeLabel(own.items)).toBe("Thông tin lớp");
    expect(activeLabel(studentClassContext(at("/classes/c-4/group"))!.items)).toBe("Nhóm của tôi");
    expect(activeLabel(studentClassContext(at("/classes/c-4/announcements"))!.items)).toBe("Thông báo lớp");
    expect(activeLabel(studentClassContext(at("/classes/c-4", "tab=materials"))!.items)).toBe("Học liệu");
    expect(activeLabel(studentClassContext(at("/materials/m-1", "classId=c-4"))!.items)).toBe("Học liệu");
    expect(activeLabel(studentClassContext(at("/my-assignments/a-1", "classId=c-4"))!.items)).toBe("Bài tập");
    expect(studentClassContext(at("/classes"))).toBeNull();
    expect(studentClassContext(at("/my-assignments"))).toBeNull();
  });

  it("lớp của môn (Chủ nhiệm môn) không có sidebar lớp, vẫn là sidebar của môn ở mục Lớp học", () => {
    expect(classContext(at("/manager/classes/c-2", "tab=students"))).toBeNull();
    expect(activeLabel(subjectContext(at("/manager/classes/c-2"))!.items)).toBe("Lớp học");
  });
});

describe("AppShell với sidebar theo ngữ cảnh", () => {
  it("ô chọn lớp hiện mã nhỏ và tên lớp to (không có tên môn), chọn lớp khác thì giữ đúng mục đang mở", async () => {
    const value = { id: "c-1", code: "SE1801", name: "Lớp chiều thứ Hai", subject: { id: "s-1", code: "SWR302", name: "Software Requirement" } };
    vi.stubGlobal("fetch", vi.fn().mockImplementation((url: string) =>
      Promise.resolve(Response.json(url.includes("/classes/c-1") ? value : [value, { ...value, id: "c-2", code: "SE1802" }]))));
    location = { pathname: "/teaching/classes/c-1", search: "tab=gradebook" };
    render(<AppShell role="TEACHER" nav={nav} user={null} context={resolve}><p>Nội dung</p></AppShell>);

    await waitFor(() => expect(screen.getByLabelText("Chọn lớp")).toHaveTextContent("SE1801"));
    expect(screen.queryByText("Software Requirement")).not.toBeInTheDocument();
    expect(screen.getByLabelText("Chọn lớp")).toHaveTextContent("Lớp chiều thứ Hai");
    expect(await screen.findByRole("menuitem", { name: /SE1802/ })).toHaveAttribute("href", "/teaching/classes/c-2?tab=gradebook");
    expect(screen.getByRole("link", { name: "Lớp được phân công" })).toHaveAttribute("href", "/teaching/classes");
  });

  afterEach(() => vi.unstubAllGlobals());

  const nav: NavItem[] = [
    { href: "/manager", label: "Môn được phân công", icon: "menu_book" },
    { href: "/teaching/classes", label: "Lớp được phân công", icon: "class" },
  ];
  const resolve = (where: { pathname: string; search: URLSearchParams }) => classContext(where) ?? subjectContext(where);

  it("ở trang của môn thì thay menu của role bằng menu môn, có lối về và tên môn", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json({ id: "s-1", code: "SWR302", name: "Software Requirement" })));
    location = { pathname: "/manager/subjects/s-1", search: "" };
    render(<AppShell role="SUBJECT_MANAGER" nav={nav} user={null} context={resolve}><p>Nội dung</p></AppShell>);

    const menu = screen.getByRole("navigation", { name: "Menu môn học" });
    expect(screen.queryByRole("navigation", { name: "Menu Chủ nhiệm môn" })).not.toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Môn được phân công" })).toHaveAttribute("href", "/manager");
    expect(screen.getByRole("link", { name: "Thông tin môn" })).toHaveAttribute("aria-current", "page");
    expect(await screen.findByText("Software Requirement")).toBeInTheDocument();
    expect(menu).toBeInTheDocument();
    expect(screen.getByLabelText("Chọn môn")).toHaveTextContent("SWR302");
  });

  it("ngoài trang môn, lớp thì vẫn là menu của role", () => {
    location = { pathname: "/manager", search: "" };
    render(<AppShell role="SUBJECT_MANAGER" nav={nav} user={null} context={resolve}><p>Nội dung</p></AppShell>);
    expect(screen.getByRole("navigation", { name: "Menu Chủ nhiệm môn" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Môn được phân công" })).toHaveAttribute("aria-current", "page");
  });
});
