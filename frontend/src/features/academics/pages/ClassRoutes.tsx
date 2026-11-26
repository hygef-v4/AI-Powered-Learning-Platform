"use client";

import {useCallback, useEffect, useRef, useState, type ReactNode} from "react";
import {useRouter} from "next/navigation";
import {ApiError} from "@/shared/api/client";
import {academicsApi, loadClassCapability, type Scope} from "../api";
import {ClassDashboardPage} from "./ClassDashboardPage";
import {ClassListPage} from "./ClassListPage";
import {ReadOnlyClassPage} from "./ReadOnlyClassPage";
import {ManagedClassPage, type ManagedClassActions} from "./ManagedClassPage";
import {ManagerDashboardPage, MySubjectPage} from "./ManagerDashboardPage";
import {ClassFormDialog} from "../components/ClassFormDialog";
import {ReopenConflictError} from "../components/ClassDialogs";
import { EmptyState } from "@/shared/ui";
import { formatOperationError } from "../model/format";
import type {SubjectView, CourseClassView, EnrollmentView} from "../model/types";
import styles from "../styles/academics.module.css";
import { PageLoadError, PageLoading } from "@/shared/ui/PageStatus";
import { msg } from "@/i18n/msg";

type ResourceLoader<T> = (signal: AbortSignal) => Promise<T>;
type ResourceState<T> = {
  data?: T;
  error?: unknown;
  loading: boolean;
  key?: ResourceLoader<T>;
};

// Cung cấp cùng cách biểu diễn dữ liệu, trạng thái tải và lỗi cho các route lớp.
function useResource<T>(load: ResourceLoader<T>) {
  const [state, setState] = useState<ResourceState<T>>({loading: true});
  const controller = useRef<AbortController | null>(null);

  // Hủy lần tải đang chạy, cập nhật dữ liệu hoặc lỗi nếu lượt mới còn hiệu lực.
  const refresh = useCallback(async () => {
    controller.current?.abort();
    const current = new AbortController();
    controller.current = current;
    try {
      const data = await load(current.signal);
      if (!current.signal.aborted) setState({data, loading: false, key: load});
    } catch (error) {
      if (!current.signal.aborted) setState({error, loading: false, key: load});
      throw error;
    }
  }, [load]);

  useEffect(() => {
    const current = new AbortController();
    controller.current = current;
    Promise.resolve()
      .then(() => load(current.signal))
      .then(data => {
        if (!current.signal.aborted) setState({data, loading: false, key: load});
      }, error => {
        if (!current.signal.aborted) setState({error, loading: false, key: load});
      });
    return () => controller.current?.abort();
  }, [load]);

  return {...(state.key === load ? state : {loading: true}), refresh};
}

// Ghép các trang lớp đến khi API trả về ít hơn giới hạn một trang.
async function listAllClasses(scope: Scope, signal: AbortSignal) {
  const rows: CourseClassView[] = [];
  for (let page = 1; ; page++) {
    const next = await academicsApi.listClasses(scope, {page, size: 100}, signal);
    rows.push(...next);
    if (next.length < 100) return rows;
  }
}

// Ghép mọi trang ghi danh của lớp đang xem, dùng cùng tín hiệu hủy.
async function listAllStudents(scope: "manager" | "teaching", id: string, signal: AbortSignal) {
  const rows: EnrollmentView[] = [];
  for (let page = 1; ; page++) {
    const next = await academicsApi.listStudents(scope, id, {page, size: 100}, signal);
    rows.push(...next);
    if (next.length < 100) return rows;
  }
}

// Hiện trạng thái tải hoặc lỗi có nút thử lại trước khi dữ liệu sẵn sàng.
function Pending({error, retry}: {error?: unknown; retry: () => void}) {
  if (!error) return <PageLoading/>;
  return <PageLoadError onRetry={() => void Promise.resolve(retry()).catch(() => {})}>{formatOperationError(error)}</PageLoadError>;
}

// Khung trang (thanh trên, menu bên theo role của phiên, hồ sơ, đăng xuất) do AuthenticatedLayout của U01 vẽ ở layout route.
// Dựng danh sách lớp và dẫn đến trang chi tiết tương ứng với vai trò đang xem.
export function ClassesRoute({teaching = false}: {teaching?: boolean}) {
  const router = useRouter();
  // Chọn nguồn theo phạm vi giảng dạy hoặc khả năng của tài khoản rồi lấy đủ trang nếu cần.
  const load = useCallback(async (signal: AbortSignal) => {
    const mode = teaching
      ? {audience: "teacher" as const, classes: await academicsApi.listClasses("teaching", {}, signal)}
      : await loadClassCapability(signal);
    return {
      ...mode,
      classes: mode.classes.length === 100
        ? await listAllClasses(mode.audience === "student" ? "me" : "teaching", signal)
        : mode.classes,
    };
  }, [teaching]);
  const state = useResource(load);
  if (!state.data) return <Pending error={state.error} retry={state.refresh}/>;

  const {audience, classes} = state.data;
  return <ClassDashboardPage audience={audience} classes={classes}
    onOpen={id => router.push(`${audience === "student" ? "/classes" : "/teaching/classes"}/${encodeURIComponent(id)}`)}/>;
}

// Chọn giao diện quản lý hoặc chỉ xem theo phạm vi và gắn các thao tác tương ứng.
export function ClassDetailRoute({id, scope}: {id: string; scope: Scope}) {
  const router = useRouter();
  // Tải chi tiết lớp và chỉ yêu cầu danh sách ghi danh ở phạm vi quản lý hoặc giảng dạy.
  const load = useCallback(async (signal: AbortSignal) => ({
    value: await academicsApi.loadClass(scope, id, signal),
    students: scope === "me" ? [] : await listAllStudents(scope, id, signal),
  }), [id, scope]);
  const state = useResource(load);
  // Tra tài khoản người dạy theo từ khóa để đưa vào lựa chọn phân công.
  const loadTeachers = useCallback((q: string, signal: AbortSignal) => academicsApi.listCandidates("teacher", q, signal), []);
  // Giới hạn tìm tài khoản cho ghi danh vào lớp đang mở.
  const loadCandidates = useCallback((q: string, signal: AbortSignal) => academicsApi.listStudentCandidates(id, q, signal), [id]);
  const [mutationConflict, setMutationConflict] = useState<{classId: string; message: string} | null>(null);
  if (!state.data) return <Pending error={state.error} retry={state.refresh}/>;

  // Tải lại dữ liệu sau khi ghi và làm mới bản đang xem trước khi báo xung đột version.
  async function mutate<T>(action: () => Promise<T>): Promise<T> {
    setMutationConflict(null);
    try {
      const result = await action();
      await state.refresh();
      return result;
    } catch (error) {
      if (error instanceof ApiError && error.problem.status === 409) {
        setMutationConflict({classId: id, message: msg("academics.pages.ClassRoutes.theDataHasChangedThe")});
        await state.refresh().catch(() => {});
        const details = error.problem as typeof error.problem & {conflictingStudents?: unknown[]};
        if (details.conflictingStudents?.length) {
          throw new ReopenConflictError(details.conflictingStudents.map(student => typeof student === "string" ? student : JSON.stringify(student)));
        }
      }
      throw error;
    }
  }

  const {value, students} = state.data;
  const actions: ManagedClassActions = {
    // Lưu thông tin lớp rồi tải lại bản hiện hành.
    onSaveInfo: async input => { await mutate(() => academicsApi.updateClassInfo(id, input)); },
    // Đổi phân công giảng viên với version của lớp đang mở.
    onAssign: async (teacherId, version) => { await mutate(() => academicsApi.assignTeacher(id, teacherId, version)); },
    // Đổi trạng thái lớp và đi qua cùng luồng xử lý xung đột.
    onChangeStatus: async (status, version) => { await mutate(() => academicsApi.changeClassStatus(id, status, version)); },
    // Đổi trạng thái ghi danh của một tài khoản và tải lại danh sách.
    onEnrollmentStatus: async (accountId, status) => { await mutate(() => academicsApi.changeEnrollmentStatus(id, accountId, status)); },
    // Ghi danh một tài khoản đã chọn rồi làm mới trang lớp.
    onEnroll: async accountId => { await mutate(() => academicsApi.enrollStudent(id, accountId)); },
    // Ghi danh theo danh sách và chuyển kết quả từng dòng cho hộp thoại.
    onBatch: async text => await mutate(() => academicsApi.enrollStudents(id, text)),
  };

  let content: ReactNode;
  if (state.error) {
    content = <Pending error={state.error} retry={state.refresh}/>;
  } else if (scope === "manager") {
    content = <ManagedClassPage value={value} students={students} teachers={[]} candidates={[]}
      loadTeachers={loadTeachers} loadCandidates={loadCandidates}
      onBack={() => router.push("/manager/classes")} actions={actions}/>;
  } else {
    content = <ReadOnlyClassPage audience={scope === "me" ? "student" : "teacher"} value={value} students={students}/>;
  }
  return <>
    {mutationConflict?.classId === id && <p role="alert" className={styles.error} data-testid="class-mutation-conflict">{mutationConflict.message}</p>}
    {content}
  </>;
}

// Dùng chung dữ liệu môn cho tổng quan, thông tin môn và danh sách lớp của quản lý.
export function ManagerRoute({view, id, initialSubjectId}: {view: "overview" | "subjects" | "classes"; id?: string; initialSubjectId?: string}) {
  const router = useRouter();
  const [creating, setCreating] = useState<SubjectView | null>(null);
  // Luôn tải môn quản lý và chỉ lấy danh sách lớp khi màn hình lớp cần dùng.
  const load = useCallback(async (signal: AbortSignal) => {
    const subjects = await academicsApi.listSubjects(signal);
    // Chỉ mục Lớp học cần danh sách lớp; Thông tin môn tự tải lớp đang mở của môn.
    const classes = view === "classes" ? await listAllClasses("manager", signal) : [];
    return {subjects, classes, subject: id ? await academicsApi.loadSubject(id, signal) : subjects[0]};
  }, [id, view]);
  const state = useResource(load);
  // Tra giảng viên cho form quản lý và chuyển tín hiệu hủy của lần tìm hiện tại.
  const loadTeachers = useCallback((q: string, signal: AbortSignal) => academicsApi.listCandidates("teacher", q, signal), []);
  if (!state.data) return <Pending error={state.error} retry={state.refresh}/>;

  const {subjects, classes, subject} = state.data;
  let content: ReactNode;
  if (view === "overview") {
    content = <ManagerDashboardPage subjects={subjects}
      onSubject={id => router.push(`/manager/subjects/${encodeURIComponent(id)}`)}/>;
  } else if (view === "subjects") {
    content = subject ? <MySubjectPage subject={subject}/>
      : <EmptyState title={msg("academics.pages.ClassRoutes.youHaveNotBeenAssigned")}/>;
  } else {
    content = <ClassListPage key={initialSubjectId ?? ""} initialSubjectId={initialSubjectId} subjects={subjects} classes={classes}
      onOpen={id => router.push(`/manager/classes/${encodeURIComponent(id)}`)} onCreate={setCreating}/>;
  }
  return <>
    {content}
    {creating && <ClassFormDialog loadTeachers={loadTeachers} subject={creating} teachers={[]}
      onClose={() => setCreating(null)}
      onSubmit={async input => {
        // Không còn lớp nháp: tạo xong thì mở lớp luôn.
        const created = await academicsApi.createClass(input);
        await academicsApi.changeClassStatus(created.id, "OPEN", created.version);
        await state.refresh();
      }}/>}
  </>;
}
