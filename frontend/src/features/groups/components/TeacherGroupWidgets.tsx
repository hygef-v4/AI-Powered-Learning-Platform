"use client";

import { useEffect, useState } from "react";
import { Alert, Button, Card, Pill } from "@/shared/ui";
import { getGroups, getStudentGroup } from "../api";
import type { GroupSetView, StudentGroup } from "../model/types";
import { msg } from "@/i18n/msg";

/** Bộ nhóm của lớp; tải lại khi quay về cửa sổ (vd. sau khi sửa nhóm ở tab khác). */
function useGroupSetState(classId: string) {
  const [state, setState] = useState<{
    classId: string;
    set: GroupSetView | null;
    error: boolean;
  }>({ classId, set: null, error: false });

  useEffect(() => {
    let active = true;
    const load = () =>
      getGroups(classId).then(
        (loaded) => {
          if (active) setState({ classId, set: loaded, error: false });
        },
        () => {
          if (active) {
            setState((prev) =>
              prev.classId === classId && prev.set
                ? prev
                : { classId, set: null, error: true },
            );
          }
        },
      );
    void load();
    window.addEventListener("focus", load);
    return () => {
      active = false;
      window.removeEventListener("focus", load);
    };
  }, [classId]);

  if (state.classId !== classId) {
    return { set: null, error: false, loading: true };
  }
  return { set: state.set, error: state.error, loading: !state.set && !state.error };
}

function useGroupSet(classId: string) {
  return useGroupSetState(classId).set;
}

/** Nhóm của sinh viên trong popup Student Detail (t19). */
export function StudentGroupInfo({ classId, studentId }: { classId: string; studentId: string }) {
  const [group, setGroup] = useState<StudentGroup | null>(null);
  useEffect(() => {
    let active = true;
    getStudentGroup(classId, studentId).then(
      (loaded) => {
        if (active) setGroup(loaded);
      },
      () => undefined,
    );
    return () => {
      active = false;
    };
  }, [classId, studentId]);
  if (!group) return null;
  return (
    <div className="flex items-center gap-2 text-[13px]">
      <span className="text-muted">{msg("groups.components.TeacherGroupWidgets.nhom")}</span>
      {group.groupId ? (
        <>
          <span className="font-semibold text-ink">{group.name}</span>
          <span className="text-xs text-muted">{group.memberCount} {msg("groups.components.TeacherGroupWidgets.thanhVien")}</span>
          {group.leader && <Pill tone="warning">{msg("groups.components.TeacherGroupWidgets.truongNhom")}</Pill>}
        </>
      ) : (
        <span className="text-muted">{msg("groups.components.TeacherGroupWidgets.chuaCoNhom")}</span>
      )}
    </div>
  );
}

/** Kiểm tra sẵn sàng của lớp cho bài nhóm trên Assignment Form (t15, BR-U12-22). */
export function GroupAssignmentPanel({ classId }: { classId: string }) {
  const set = useGroupSet(classId);
  if (!set) return null;
  const errors: string[] = [];
  if (set.groups.length === 0) errors.push(msg("groups.components.TeacherGroupWidgets.lopChuaCoNhom"));
  if (set.groups.some((g) => !g.leaderId)) errors.push(msg("groups.components.TeacherGroupWidgets.coNhomChuaCoTruongNhom"));
  if (set.groups.some((g) => !g.members.some((m) => m.active))) errors.push(msg("groups.components.TeacherGroupWidgets.coNhomKhongCoThanhVien"));
  return (
    <Card className="flex flex-col gap-2">
      <p className="text-[13px] font-bold text-ink">{msg("groups.components.TeacherGroupWidgets.kiemTraSanSang")}{set.groups.length} {msg("groups.components.TeacherGroupWidgets.nhom2")}</p>
      {errors.length > 0 && (
        <Alert tone="danger" title={msg("groups.components.TeacherGroupWidgets.chuaPhatHanhDuoc")}>
          <ul className="list-disc pl-4">
            {errors.map((e) => (
              <li key={e}>{e}</li>
            ))}
          </ul>
        </Alert>
      )}
      {set.ungrouped.length > 0 && (
        <Alert tone="warning">{set.ungrouped.length} {msg("groups.components.TeacherGroupWidgets.sinhVienChuaCoNhomXac")}</Alert>
      )}
      {errors.length === 0 && set.ungrouped.length === 0 && <Alert tone="success">{msg("groups.components.TeacherGroupWidgets.lopDaSanSangChoBai")}</Alert>}
      <div className="flex flex-wrap items-center gap-2">
        <Button size="sm" icon="groups" onClick={() => window.open(`/teaching/classes/${classId}?tab=students`, "_blank", "noopener")}>
          {msg("groups.components.TeacherGroupWidgets.moTabSinhVien")}</Button>
        <span className="text-xs text-muted">{msg("groups.components.TeacherGroupWidgets.suaNhomOTabMoiQuay")}</span>
      </div>
    </Card>
  );
}
