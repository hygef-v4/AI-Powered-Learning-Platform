"use client";

// U05: sidebar của một môn (Chủ nhiệm môn) và của một lớp (Giảng viên, Chủ nhiệm môn, Sinh viên), thay menu của role khi
// đang ở trang của môn hay lớp đó. Đầu sidebar là ô chọn để chuyển sang môn, lớp khác mà vẫn ở đúng mục đang mở.

import Link from "next/link";
import { useEffect, useState, type ReactNode } from "react";
import { subjectNavEntries } from "@/features/identity/slots";
import { FADE_LABEL, type NavContext, type NavItem, type NavLocation } from "@/shared/layout/navigation";
import { cx } from "@/shared/ui/cx";
import { Icon, type IconName } from "@/shared/ui/Icon";
import { Menu } from "@/shared/ui/Menu";
import { academicsApi, type Scope } from "./api";
import type { CourseClassView, SubjectView } from "./types";

const enc = encodeURIComponent;

/** Phát sau khi sửa lớp để mã lớp trên sidebar cập nhật theo; `detail` là id lớp. */
export const CLASS_CHANGED_EVENT = "academics:class-changed";

/** Các màn quản lý một môn. Chi tiết lớp (`/manager/classes/{id}`) thuộc sidebar của lớp. */
const SUBJECT_ROOTS = ["/manager/subjects", "/manager/questions", "/manager/quizzes", "/manager/assignments", "/manager/materials", "/manager/classes"];

function under(pathname: string, root: string) {
  return pathname === root || pathname.startsWith(`${root}/`);
}

/** Đổi môn, lớp mà giữ mục đang mở: dựng lại các mục cho id mới rồi lấy link của mục đang mở. */
function switcher(build: (id: string) => NavItem[], items: NavItem[]) {
  const index = Math.max(0, items.findIndex((item) => item.active));
  return (id: string) => build(id)[index]!.href;
}

// Môn mở gần nhất: trang không mang `?subjectId=` (ví dụ chi tiết câu hỏi) vẫn giữ sidebar của môn đó.
let lastSubjectId: string | null = null;

export function subjectContext({ pathname, search }: NavLocation): NavContext | null {
  if (!SUBJECT_ROOTS.some((root) => under(pathname, root)) || /^\/manager\/classes\/[^/]+/.test(pathname)) return null;
  const fromPath = /^\/manager\/subjects\/([^/]+)/.exec(pathname)?.[1];
  const subjectId = (fromPath ? decodeURIComponent(fromPath) : search.get("subjectId")) || lastSubjectId;
  lastSubjectId = subjectId;
  const entries = subjectNavEntries.entries();
  const current = entries
    .filter((entry) => under(pathname, entry.path))
    .reduce<string | null>((best, entry) => (best === null || entry.path.length > best.length ? entry.path : best), null);
  const build = (id: string | null): NavItem[] =>
    entries.map((entry) => ({
      href: id ? entry.href?.(id) ?? `${entry.path}?subjectId=${enc(id)}` : entry.path,
      label: entry.title,
      icon: entry.icon,
      active: entry.path === current,
    }));
  const items = build(subjectId);
  return {
    key: `subject:${subjectId ?? ""}`,
    label: "Menu môn học",
    header: <SubjectNavHeader subjectId={subjectId} hrefFor={switcher(build, items)} />,
    items,
  };
}

type ClassItem = { key: string; label: string; icon: IconName; href: (id: string) => string };
type ClassScope = Extract<Scope, "teaching" | "manager" | "me">;

const classPage = (scope: string) => (tab?: string) => (id: string) => `/${scope}/classes/${enc(id)}${tab ? `?tab=${tab}` : ""}`;
const teachingPage = classPage("teaching");
const managerPage = classPage("manager");
const announcements = (id: string) => `/announcements?classId=${enc(id)}`;

/** Các tab của trang lớp (`?tab=`) đưa ra sidebar, thêm Thông báo lớp của lớp đó. */
const TEACHING_ITEMS: ClassItem[] = [
  { key: "info", label: "Thông tin lớp", icon: "info", href: teachingPage() },
  { key: "students", label: "Sinh viên", icon: "groups", href: teachingPage("students") },
  { key: "materials", label: "Học liệu", icon: "folder_open", href: teachingPage("materials") },
  { key: "evals", label: "Đánh giá", icon: "assignment", href: teachingPage("evals") },
  { key: "gradebook", label: "Sổ điểm", icon: "receipt_long", href: teachingPage("gradebook") },
  { key: "announcements", label: "Thông báo lớp", icon: "campaign", href: announcements },
];
const MANAGER_ITEMS: ClassItem[] = [
  { key: "info", label: "Thông tin lớp", icon: "info", href: managerPage() },
  { key: "students", label: "Sinh viên", icon: "groups", href: managerPage("students") },
];
/** Các trang sinh viên vào được của một lớp. */
const STUDENT_ITEMS: ClassItem[] = [
  { key: "info", label: "Thông tin lớp", icon: "info", href: (id) => `/classes/${enc(id)}` },
  { key: "assignments", label: "Bài tập", icon: "assignment", href: (id) => `/my-assignments?classId=${enc(id)}` },
  { key: "announcements", label: "Thông báo lớp", icon: "campaign", href: announcements },
  { key: "group", label: "Nhóm của tôi", icon: "group", href: (id) => `/classes/${enc(id)}/group` },
];

const BACK: Record<ClassScope, { href: string; label: string }> = {
  teaching: { href: "/teaching/classes", label: "Lớp được phân công" },
  manager: { href: "/manager/classes", label: "Lớp của môn" },
  me: { href: "/classes", label: "Lớp của tôi" },
};

function buildClass(scope: ClassScope, id: string, open: string): NavContext {
  const list = scope === "teaching" ? TEACHING_ITEMS : scope === "manager" ? MANAGER_ITEMS : STUDENT_ITEMS;
  const build = (classId: string): NavItem[] =>
    list.map((item) => ({ href: item.href(classId), label: item.label, icon: item.icon, active: item.key === open }));
  const items = build(id);
  return {
    key: `class:${scope}:${id}`,
    label: "Menu lớp học",
    header: <ClassNavHeader scope={scope} id={id} hrefFor={switcher(build, items)} />,
    items,
  };
}

/** Lớp được phân công (`/teaching/classes/{id}`), lớp của môn (`/manager/classes/{id}`) và Thông báo lớp của một lớp. */
export function classContext({ pathname, search }: NavLocation): NavContext | null {
  if (pathname === "/announcements" && search.get("classId")) return buildClass("teaching", search.get("classId")!, "announcements");
  const match = /^\/(teaching|manager)\/classes\/([^/]+)(\/.*)?$/.exec(pathname);
  if (!match) return null;
  const scope = match[1] as "teaching" | "manager";
  const tabs = (scope === "teaching" ? TEACHING_ITEMS : MANAGER_ITEMS).filter((item) => item.key !== "announcements");
  // Trang con của lớp (bài nộp của một bài tập) thuộc tab Đánh giá.
  const open = match[3] ? "evals" : tabs.find((tab) => tab.key === search.get("tab"))?.key ?? "info";
  return buildClass(scope, decodeURIComponent(match[2]!), open);
}

/** Lớp của tôi (`/classes/{id}`, Nhóm của tôi) và Bài tập, Thông báo lọc theo một lớp (`?classId=`). */
export function studentClassContext({ pathname, search }: NavLocation): NavContext | null {
  const own = /^\/classes\/([^/]+)(\/group)?$/.exec(pathname);
  if (own) return buildClass("me", decodeURIComponent(own[1]!), own[2] ? "group" : "info");
  const classId = search.get("classId");
  if (!classId) return null;
  if (under(pathname, "/my-assignments")) return buildClass("me", classId, "assignments");
  if (pathname === "/announcements") return buildClass("me", classId, "announcements");
  return null;
}

function SubjectNavHeader({ subjectId, hrefFor }: { subjectId: string | null; hrefFor: (id: string) => string }) {
  const [subject, setSubject] = useState<SubjectView | null>(null);
  const [subjects, setSubjects] = useState<SubjectView[]>([]);
  useEffect(() => {
    if (!subjectId) return;
    const request = new AbortController();
    academicsApi.subject(subjectId, request.signal).then(setSubject, () => {});
    return () => request.abort();
  }, [subjectId]);
  useEffect(() => {
    const request = new AbortController();
    academicsApi.subjects(request.signal).then(setSubjects, () => {});
    return () => request.abort();
  }, []);
  const shown = subject?.id === subjectId ? subject : null;
  return (
    <NavHeader back={{ href: "/manager", label: "Môn được phân công" }}>
      {subjectId && (
        <Switcher
          label="Chọn môn"
          loading={!shown}
          code={shown?.code}
          name={shown?.name}
          options={subjects.map((item) => ({ id: item.id, href: hrefFor(item.id), code: item.code, name: item.name, selected: item.id === subjectId }))}
        />
      )}
    </NavHeader>
  );
}

function ClassNavHeader({ scope, id, hrefFor }: { scope: ClassScope; id: string; hrefFor: (id: string) => string }) {
  const [value, setValue] = useState<CourseClassView | null>(null);
  const [classes, setClasses] = useState<CourseClassView[]>([]);
  const [version, setVersion] = useState(0);
  useEffect(() => {
    const onChange = (event: Event) => {
      if ((event as CustomEvent<string>).detail === id) setVersion((current) => current + 1);
    };
    window.addEventListener(CLASS_CHANGED_EVENT, onChange);
    return () => window.removeEventListener(CLASS_CHANGED_EVENT, onChange);
  }, [id]);
  useEffect(() => {
    const request = new AbortController();
    academicsApi.detail(scope, id, request.signal).then(setValue, () => {});
    return () => request.abort();
  }, [scope, id, version]);
  const shown = value?.id === id ? value : null;
  // Lớp của môn chỉ chuyển giữa các lớp cùng môn.
  const subjectId = scope === "manager" ? shown?.subject.id : undefined;
  useEffect(() => {
    if (scope === "manager" && !subjectId) return;
    const request = new AbortController();
    academicsApi.classes(scope, { size: 100, subjectId }, request.signal).then(setClasses, () => {});
    return () => request.abort();
  }, [scope, subjectId]);
  const back = scope === "manager" && shown ? { href: `/manager/classes?subjectId=${enc(shown.subject.id)}`, label: BACK.manager.label } : BACK[scope];
  return (
    <NavHeader back={back}>
      <Switcher
        label="Chọn lớp"
        loading={!shown}
        code={shown?.code}
        name={shown?.name}
        options={classes.map((item) => ({ id: item.id, href: hrefFor(item.id), code: item.code, name: item.name, selected: item.id === id }))}
      />
    </NavHeader>
  );
}

/**
 * Lối về trang danh sách, rồi ô chọn môn hay lớp đang mở. Hai phần giữ chiều cao cố định cả khi sidebar thu gọn nên các mục
 * bên dưới đứng yên chỗ cũ.
 */
function NavHeader({ back, children }: { back: { href: string; label: string }; children?: ReactNode }) {
  return (
    <div className="mb-3 flex shrink-0 flex-col gap-2.5 border-b border-line pb-3.5">
      <div className="overflow-hidden">
        <Link
          href={back.href}
          title={back.label}
          className="inline-flex max-w-full items-center gap-2.5 rounded-md px-2.5 py-1 text-xs font-semibold whitespace-nowrap text-muted transition-colors hover:text-ink"
        >
          <Icon name="arrow_back" size={17} className="shrink-0" />
          <span className={FADE_LABEL}>{back.label}</span>
        </Link>
      </div>
      {children}
    </div>
  );
}

type SwitchOption = { id: string; href: string; code: string; name: string; selected: boolean };

/** Sidebar thu gọn (chỉ màn rộng): ô chọn chỉ còn mã; mở đủ: mã nhỏ sát trên tên, hai dòng đọc như một nhãn. */
const WHEN_COLLAPSED = "hidden md:group-data-[collapsed]/sidebar:block";
const WHEN_OPEN = "md:group-data-[collapsed]/sidebar:hidden";

/**
 * Ô thả xuống: hiện môn, lớp đang mở; mở ra là danh sách để chuyển sang môn, lớp khác. Cao cố định ở cả hai trạng thái của
 * sidebar; thu gọn thì danh sách tràn ra bên phải sidebar.
 */
function Switcher({ label, code, name, loading, options }: { label: string; code?: string; name?: string; loading: boolean; options: SwitchOption[] }) {
  return (
    <Menu
      ariaLabel={label}
      variant="tool"
      className="w-full"
      summaryClassName={cx(
        "!h-11 w-full !whitespace-normal rounded-lg border border-line bg-surface !px-2.5 !py-1 text-left hover:!bg-surface hover:border-line-strong",
        "[&>svg:last-child]:ml-auto [&>svg:last-child]:shrink-0",
        "md:group-data-[collapsed]/sidebar:!px-0 md:group-data-[collapsed]/sidebar:[&>svg:last-child]:hidden",
      )}
      label={
        loading ? (
          <span aria-hidden className="flex flex-1 flex-col gap-1">
            <span className="h-2 w-10 rounded bg-sunken" />
            <span className="h-3 w-28 max-w-full rounded bg-sunken" />
          </span>
        ) : (
          <span className="min-w-0 flex-1">
            <span className={cx(WHEN_OPEN, "block leading-none")} title={name}>
              <span className="block font-mono text-[9.5px] leading-none tracking-wide text-muted">{code}</span>
              <span className="mt-[3px] block truncate text-[13px] leading-[1.15] font-semibold text-ink">{name}</span>
            </span>
            <span className={cx(WHEN_COLLAPSED, "text-center font-mono text-[10px] leading-tight font-bold tracking-[-0.04em] text-ink [overflow-wrap:anywhere]")}>{code}</span>
          </span>
        )
      }
    >
      <div className="flex max-h-72 w-[208px] flex-col gap-0.5 overflow-y-auto">
        {options.length === 0 && <span className="px-2.5 py-2 text-[13px] text-muted">Đang tải…</span>}
        {options.map((option) => (
          <Link
            key={option.id}
            href={option.href}
            role="menuitem"
            data-menu-item
            aria-current={option.selected ? "true" : undefined}
            className={cx(
              "flex items-center gap-2 rounded-lg px-2.5 py-1.5 text-left text-[13px] transition-colors hover:bg-sunken",
              option.selected ? "bg-primary-soft text-ink" : "text-ink",
            )}
          >
            {/* Cùng kiểu ô chọn: mã nhỏ sát trên tên một dòng, tên dài thì cắt và hiện đủ khi rê chuột. */}
            <span className="min-w-0 flex-1 leading-none" title={option.name}>
              <span className="block font-mono text-[9.5px] leading-none tracking-wide text-muted">{option.code}</span>
              <span className="mt-[3px] block truncate text-[13px] leading-[1.15] font-semibold">{option.name}</span>
            </span>
            {option.selected && <Icon name="check" size={15} className="shrink-0 text-primary-ink" />}
          </Link>
        ))}
      </div>
    </Menu>
  );
}
