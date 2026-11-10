"use client";

// U05: cây môn (Chủ nhiệm môn) và cây lớp (Giảng viên, Chủ nhiệm môn, Sinh viên) trên sidebar, kiểu cây thư mục: menu của
// role giữ nguyên, các môn, lớp mở ra ngay dưới mục cha (Môn được phân công, Lớp được phân công, Lớp của tôi); môn, lớp
// của trang đang mở mở sẵn các mục của nó.

import { useEffect, useState } from "react";
import { subjectNavEntries } from "@/features/identity/slots";
import type { NavContext, NavItem, NavLocation } from "@/shared/layout/navigation";
import { NavTree, type NavTreeNode } from "@/shared/layout/NavTree";
import type { IconName } from "@/shared/ui/Icon";
import { academicsApi, type Scope } from "../api";

const encodeId = encodeURIComponent;

const SUBJECTS_HREF = "/manager";

/** Các màn quản lý một môn, gồm cả chi tiết một lớp của môn (`/manager/classes/{id}`, mục Lớp học). */
const SUBJECT_ROOTS = ["/manager/subjects", "/manager/questions", "/manager/quizzes", "/manager/assignments", "/manager/materials", "/manager/classes"];

function isUnderPath(pathname: string, root: string) {
  return pathname === root || pathname.startsWith(`${root}/`);
}

// Môn mở gần nhất: trang không mang `?subjectId=` (ví dụ chi tiết câu hỏi) vẫn mở nhánh của môn đó.
let lastSubjectId: string | null = null;

/** Trang Môn được phân công: cây các môn, chưa môn nào mở; trang của một môn: nhánh môn đó mở sẵn. */
export function resolveSubjectContext({ pathname, search }: NavLocation): NavContext | null {
  const onList = pathname === SUBJECTS_HREF;
  if (!onList && !SUBJECT_ROOTS.some((root) => isUnderPath(pathname, root))) return null;
  const fromPath = /^\/manager\/subjects\/([^/]+)/.exec(pathname)?.[1];
  const subjectId = onList ? null : (fromPath ? decodeURIComponent(fromPath) : search.get("subjectId")) || lastSubjectId;
  if (!onList) lastSubjectId = subjectId;
  const entries = subjectNavEntries.entries();
  const current = onList
    ? null
    : entries
        .filter((entry) => isUnderPath(pathname, entry.path))
        .reduce<string | null>((best, entry) => (best === null || entry.path.length > best.length ? entry.path : best), null);
  const build = (id: string): NavItem[] =>
    entries.map((entry) => ({
      href: entry.href?.(id) ?? `${entry.path}?subjectId=${encodeId(id)}`,
      label: entry.title,
      icon: entry.icon,
      active: id === subjectId && entry.path === current,
    }));
  return {
    key: "subject",
    label: "Môn được phân công",
    parentHref: SUBJECTS_HREF,
    header: <SubjectNavTree subjectId={subjectId} itemsFor={build} />,
    items: subjectId ? build(subjectId) : [],
  };
}

type ClassItem = { key: string; label: string; icon: IconName; href: (id: string) => string };
type ClassScope = Extract<Scope, "teaching" | "me">;

const teachingPage = (tab?: string) => (id: string) => `/teaching/classes/${encodeId(id)}${tab ? `?tab=${tab}` : ""}`;
const teachingAnnouncements = (id: string) => `/teaching/classes/${encodeId(id)}/announcements`;
const studentAnnouncements = (id: string) => `/classes/${encodeId(id)}/announcements`;

/** Các tab của trang lớp (`?tab=`) đưa ra sidebar, thêm Thông báo lớp của lớp đó. */
const TEACHING_ITEMS: ClassItem[] = [
  { key: "info", label: "Thông tin lớp", icon: "info", href: teachingPage() },
  { key: "students", label: "Sinh viên", icon: "groups", href: teachingPage("students") },
  { key: "materials", label: "Học liệu", icon: "folder_open", href: teachingPage("materials") },
  { key: "evals", label: "Bài tập", icon: "assignment", href: teachingPage("evals") },
  { key: "gradebook", label: "Chấm điểm", icon: "receipt_long", href: teachingPage("gradebook") },
  { key: "announcements", label: "Thông báo lớp", icon: "campaign", href: teachingAnnouncements },
];
/** Các trang sinh viên vào được của một lớp. */
const STUDENT_ITEMS: ClassItem[] = [
  { key: "info", label: "Thông tin lớp", icon: "info", href: (id) => `/classes/${encodeId(id)}` },
  { key: "group", label: "Nhóm của tôi", icon: "group", href: (id) => `/classes/${encodeId(id)}/group` },
  { key: "materials", label: "Học liệu", icon: "folder_open", href: (id) => `/classes/${encodeId(id)}?tab=materials` },
  { key: "assignments", label: "Bài tập", icon: "assignment", href: (id) => `/my-assignments?classId=${encodeId(id)}` },
  { key: "announcements", label: "Thông báo lớp", icon: "campaign", href: studentAnnouncements },
];

/** Mục cha của cây lớp trên menu role. */
const CLASS_ROOTS: Record<ClassScope, { href: string; label: string }> = {
  teaching: { href: "/teaching/classes", label: "Lớp được phân công" },
  me: { href: "/classes", label: "Lớp của tôi" },
};

/** `id` rỗng: trang danh sách lớp, cây chưa mở lớp nào. */
function buildClass(scope: ClassScope, id: string | null, open: string | null): NavContext {
  const list = scope === "teaching" ? TEACHING_ITEMS : STUDENT_ITEMS;
  const build = (classId: string): NavItem[] =>
    list.map((item) => ({ href: item.href(classId), label: item.label, icon: item.icon, active: classId === id && item.key === open }));
  return {
    key: `class:${scope}`,
    label: CLASS_ROOTS[scope].label,
    parentHref: CLASS_ROOTS[scope].href,
    header: <ClassNavTree scope={scope} id={id} itemsFor={build} />,
    items: id ? build(id) : [],
  };
}

/**
 * Lớp được phân công (`/teaching/classes`, `/teaching/classes/{id}`, kể cả Thông báo lớp `/teaching/classes/{id}/announcements`).
 * Lớp học của môn (Chủ nhiệm môn) nằm trong cây môn.
 */
export function resolveClassContext({ pathname, search }: NavLocation): NavContext | null {
  if (pathname === CLASS_ROOTS.teaching.href) return buildClass("teaching", null, null);
  const match = /^\/teaching\/classes\/([^/]+)(\/.*)?$/.exec(pathname);
  if (!match) return null;
  const tabs = TEACHING_ITEMS.filter((item) => item.key !== "announcements");
  // Trang con của lớp: Thông báo lớp; còn lại (bài nộp của một bài tập) thuộc tab Bài tập.
  const open = match[2] === "/announcements" ? "announcements" : match[2] ? "evals" : tabs.find((tab) => tab.key === search.get("tab"))?.key ?? "info";
  return buildClass("teaching", decodeURIComponent(match[1]!), open);
}

/** Lớp của tôi (`/classes`, `/classes/{id}`, Học liệu, Nhóm của tôi, Thông báo lớp) và Bài tập, một học liệu của một lớp (`?classId=`). */
export function resolveStudentClassContext({ pathname, search }: NavLocation): NavContext | null {
  if (pathname === CLASS_ROOTS.me.href) return buildClass("me", null, null);
  const own = /^\/classes\/([^/]+)(\/group|\/announcements)?$/.exec(pathname);
  if (own) return buildClass("me", decodeURIComponent(own[1]!), own[2] ? own[2].slice(1) : search.get("tab") === "materials" ? "materials" : "info");
  const classId = search.get("classId");
  // Bài mở từ trang Bài tập chung (`from=all`) giữ menu chung, không mở cây lớp.
  if (!classId || search.get("from") === "all") return null;
  if (isUnderPath(pathname, "/my-assignments")) return buildClass("me", classId, "assignments");
  if (isUnderPath(pathname, "/materials")) return buildClass("me", classId, "materials");
  return null;
}

/** Thêm nút của trang đang mở khi danh sách chưa tải xong hay không có nó, để nhánh đang mở luôn hiện. */
function withCurrent(nodes: NavTreeNode[], currentId: string | null): NavTreeNode[] {
  if (!currentId || nodes.some((node) => node.id === currentId)) return nodes;
  return [{ id: currentId }, ...nodes];
}

function SubjectNavTree({ subjectId, itemsFor }: { subjectId: string | null; itemsFor: (id: string) => NavItem[] }) {
  const [subjects, setSubjects] = useState<NavTreeNode[] | null>(null);
  useEffect(() => {
    const request = new AbortController();
    academicsApi.listSubjects(request.signal).then(
      (list) => setSubjects(list.map((item) => ({ id: item.id, code: item.code, name: item.name }))),
      () => setSubjects((previous) => previous ?? []),
    );
    return () => request.abort();
  }, []);
  return (
    <NavTree
      label="Các môn được phân công"
      nodes={withCurrent(subjects ?? [], subjectId)}
      currentId={subjectId}
      itemsFor={itemsFor}
      loading={subjects === null}
    />
  );
}

function ClassNavTree({ scope, id, itemsFor }: { scope: ClassScope; id: string | null; itemsFor: (id: string) => NavItem[] }) {
  const [classes, setClasses] = useState<NavTreeNode[] | null>(null);
  useEffect(() => {
    const request = new AbortController();
    // Lớp không còn tên (yêu cầu 2026-11-02): mã môn nhỏ ở trên, mã lớp ở dưới.
    academicsApi.listClasses(scope, { size: 100 }, request.signal).then(
      (list) => setClasses(list.map((item) => ({ id: item.id, code: item.subject.code, name: item.code }))),
      () => setClasses((previous) => previous ?? []),
    );
    return () => request.abort();
  }, [scope]);
  return (
    <NavTree
      label={`Các ${CLASS_ROOTS[scope].label.toLowerCase()}`}
      nodes={withCurrent(classes ?? [], id)}
      currentId={id}
      itemsFor={itemsFor}
      loading={classes === null}
    />
  );
}
