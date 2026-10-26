"use client";

import {useCallback, useEffect, useRef, useState, type ReactNode} from "react";
import {useRouter} from "next/navigation";
import {ApiError} from "@/shared/api/client";
import {academicsApi, classCapability, type Scope} from "./api";
import {ClassDashboardPage} from "./ClassDashboardPage";
import {ClassListPage} from "./ClassListPage";
import {ReadOnlyClassPage} from "./ReadOnlyClassPage";
import {ManagedClassPage, type ManagedClassActions} from "./ManagedClassPage";
import {ManagerDashboardPage, MySubjectPage} from "./ManagerDashboardPage";
import {ClassFormDialog} from "./ClassFormDialog";
import {ReopenConflictError} from "./ClassDialogs";
import {EmptyState, operationError} from "./ui";
import type {SubjectView, CourseClassView, EnrollmentView} from "./types";
import styles from "./academics.module.css";

type ResourceLoader<T> = (signal: AbortSignal) => Promise<T>;
type ResourceState<T> = {
  data?: T;
  error?: unknown;
  loading: boolean;
  key?: ResourceLoader<T>;
};

function useResource<T>(load: ResourceLoader<T>) {
  const [state, setState] = useState<ResourceState<T>>({loading: true});
  const controller = useRef<AbortController | null>(null);

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

async function allClasses(scope: Scope, signal: AbortSignal) {
  const rows: CourseClassView[] = [];
  for (let page = 1; ; page++) {
    const next = await academicsApi.classes(scope, {page, size: 100}, signal);
    rows.push(...next);
    if (next.length < 100) return rows;
  }
}

async function allStudents(scope: "manager" | "teaching", id: string, signal: AbortSignal) {
  const rows: EnrollmentView[] = [];
  for (let page = 1; ; page++) {
    const next = await academicsApi.students(scope, id, {page, size: 100}, signal);
    rows.push(...next);
    if (next.length < 100) return rows;
  }
}

function Pending({error, retry}: {error?: unknown; retry: () => void}) {
  if (!error) return <p role="status" className={styles.loading}>Đang tải…</p>;
  return <section role="alert" className={styles.panel}>
    <p>{operationError(error)}</p>
    <button onClick={() => void Promise.resolve(retry()).catch(() => {})}>Thử lại</button>
  </section>;
}

// Khung trang (thanh trên, menu bên theo role của phiên, hồ sơ, đăng xuất) do AuthenticatedLayout của U01 vẽ ở layout route.
export function ClassesRoute({teaching = false}: {teaching?: boolean}) {
  const router = useRouter();
  const load = useCallback(async (signal: AbortSignal) => {
    const mode = teaching
      ? {audience: "teacher" as const, classes: await academicsApi.classes("teaching", {}, signal)}
      : await classCapability(signal);
    return {
      ...mode,
      classes: mode.classes.length === 100
        ? await allClasses(mode.audience === "student" ? "me" : "teaching", signal)
        : mode.classes,
    };
  }, [teaching]);
  const state = useResource(load);
  if (!state.data) return <Pending error={state.error} retry={state.refresh}/>;

  const {audience, classes} = state.data;
  return <ClassDashboardPage audience={audience} classes={classes}
    onOpen={id => router.push(`${audience === "student" ? "/classes" : "/teaching/classes"}/${encodeURIComponent(id)}`)}/>;
}

export function ClassDetailRoute({id, scope}: {id: string; scope: Scope}) {
  const router = useRouter();
  const load = useCallback(async (signal: AbortSignal) => ({
    value: await academicsApi.detail(scope, id, signal),
    students: scope === "me" ? [] : await allStudents(scope, id, signal),
  }), [id, scope]);
  const state = useResource(load);
  const loadTeachers = useCallback((q: string, signal: AbortSignal) => academicsApi.candidates("teacher", q, signal), []);
  const loadCandidates = useCallback((q: string, signal: AbortSignal) => academicsApi.studentCandidates(id, q, signal), [id]);
  const [mutationConflict, setMutationConflict] = useState<{classId: string; message: string} | null>(null);
  if (!state.data) return <Pending error={state.error} retry={state.refresh}/>;

  async function mutate<T>(action: () => Promise<T>): Promise<T> {
    setMutationConflict(null);
    try {
      const result = await action();
      await state.refresh();
      return result;
    } catch (error) {
      if (error instanceof ApiError && error.problem.status === 409) {
        setMutationConflict({classId: id, message: "Dữ liệu đã thay đổi. Đã tải lại thông tin mới nhất; vui lòng kiểm tra trước khi tiếp tục."});
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
    onSaveInfo: async input => { await mutate(() => academicsApi.info(id, input)); },
    onAssign: async (teacherId, version) => { await mutate(() => academicsApi.teacher(id, teacherId, version)); },
    onChangeStatus: async (status, version) => { await mutate(() => academicsApi.status(id, status, version)); },
    onDistribution: async (enabled, version) => { await mutate(() => academicsApi.distribution(id, enabled, version)); },
    onEnrollmentStatus: async (accountId, status) => { await mutate(() => academicsApi.enrollmentStatus(id, accountId, status)); },
    onEnroll: async accountId => { await mutate(() => academicsApi.enroll(id, accountId)); },
    onBatch: async text => await mutate(() => academicsApi.batch(id, text)),
  };

  let content: ReactNode;
  if (state.error) {
    content = <Pending error={state.error} retry={state.refresh}/>;
  } else if (scope === "manager") {
    content = <ManagedClassPage value={value} students={students} teachers={[]} candidates={[]}
      loadTeachers={loadTeachers} loadCandidates={loadCandidates}
      onBack={() => router.push("/manager/classes")} actions={actions}/>;
  } else {
    content = <ReadOnlyClassPage audience={scope === "me" ? "student" : "teacher"} value={value} students={students}
      loadStudent={scope === "teaching" ? academicsApi.student : undefined}
      onBack={() => router.push(scope === "me" ? "/classes" : "/teaching/classes")}/>;
  }
  return <>
    {mutationConflict?.classId === id && <p role="alert" className={styles.error} data-testid="class-mutation-conflict">{mutationConflict.message}</p>}
    {content}
  </>;
}

export function ManagerRoute({view, id, initialSubjectId}: {view: "overview" | "subjects" | "classes"; id?: string; initialSubjectId?: string}) {
  const router = useRouter();
  const [creating, setCreating] = useState<SubjectView | null>(null);
  const load = useCallback(async (signal: AbortSignal) => {
    const subjects = await academicsApi.subjects(signal);
    const classes = await allClasses("manager", signal);
    let teachingCount = 0;
    if (view === "overview") {
      try {
        teachingCount = (await allClasses("teaching", signal)).length;
      } catch (error) {
        if (!(error instanceof ApiError) || ![403, 404].includes(error.problem.status)) throw error;
      }
    }
    return {subjects, classes, teachingCount, subject: id ? await academicsApi.subject(id, signal) : subjects[0]};
  }, [id, view]);
  const state = useResource(load);
  const loadTeachers = useCallback((q: string, signal: AbortSignal) => academicsApi.candidates("teacher", q, signal), []);
  if (!state.data) return <Pending error={state.error} retry={state.refresh}/>;

  const {subjects, classes, subject, teachingCount} = state.data;
  let content: ReactNode;
  if (view === "overview") {
    content = <ManagerDashboardPage teachingCount={teachingCount} subjects={subjects} classes={classes}
      onSubject={id => router.push(`/manager/subjects/${encodeURIComponent(id)}`)}
      onTeaching={() => router.push("/teaching/classes")}/>;
  } else if (view === "subjects") {
    content = subject ? <MySubjectPage subject={subject} subjects={subjects} classes={classes}
      onSwitch={id => router.push(`/manager/subjects/${encodeURIComponent(id)}`)}
      onClasses={() => router.push(`/manager/classes?subjectId=${encodeURIComponent(subject.id)}`)}/>
      : <EmptyState title="Bạn chưa được giao quản lý môn"/>;
  } else {
    content = <ClassListPage initialSubjectId={initialSubjectId} subjects={subjects} classes={classes}
      onOpen={id => router.push(`/manager/classes/${encodeURIComponent(id)}`)} onCreate={setCreating}/>;
  }
  return <>
    {content}
    {creating && <ClassFormDialog loadTeachers={loadTeachers} subject={creating} teachers={[]}
      onClose={() => setCreating(null)}
      onSubmit={async input => {
        await academicsApi.create(input);
        await state.refresh();
      }}/>}
  </>;
}
