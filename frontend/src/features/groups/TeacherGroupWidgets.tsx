"use client";

import { useEffect, useState } from "react";
import { Alert, Button, IconTile, Pill } from "@/shared/ui";
import { getGroups, getStudentGroup } from "./api";
import type { GroupSetView, StudentGroup } from "./types";

/** Bộ nhóm của lớp; tải lại khi quay về cửa sổ (vd. sau khi sửa nhóm ở tab khác). */
function useGroupSet(classId: string) {
  const [set, setSet] = useState<GroupSetView | null>(null);
  useEffect(() => {
    let active = true;
    const load = () =>
      getGroups(classId).then(
        (loaded) => {
          if (active) setSet(loaded);
        },
        () => undefined,
      );
    void load();
    window.addEventListener("focus", load);
    return () => {
      active = false;
      window.removeEventListener("focus", load);
    };
  }, [classId]);
  return set;
}

/** Số nhóm và số sinh viên chưa có nhóm trên tab Thông tin lớp (t18). */
export function GroupCountCard({ classId }: { classId: string }) {
  const set = useGroupSet(classId);
  if (!set) return null;
  return (
    <div className="flex items-start gap-3 rounded-2xl border border-line bg-white p-4">
      <IconTile icon="groups" tone="accent" />
      <div>
        <p className="text-xs font-semibold text-muted">Nhóm</p>
        <p className="text-xl font-bold text-ink">{set.groups.length}</p>
        <p className="text-xs text-muted">{set.ungrouped.length} sinh viên chưa có nhóm</p>
      </div>
    </div>
  );
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
      <span className="text-muted">Nhóm:</span>
      {group.groupId ? (
        <>
          <span className="font-semibold text-ink">{group.name}</span>
          <span className="text-xs text-muted">{group.memberCount} thành viên</span>
          {group.leader && <Pill tone="warning">Trưởng nhóm</Pill>}
        </>
      ) : (
        <span className="text-muted">Chưa có nhóm</span>
      )}
    </div>
  );
}

/** Kiểm tra sẵn sàng của lớp cho bài nhóm trên Assignment Form (t15, BR-U12-22). */
export function GroupAssignmentPanel({ classId }: { classId: string }) {
  const set = useGroupSet(classId);
  if (!set) return null;
  const errors: string[] = [];
  if (set.groups.length === 0) errors.push("Lớp chưa có nhóm.");
  if (set.groups.some((g) => !g.leaderId)) errors.push("Có nhóm chưa có trưởng nhóm.");
  if (set.groups.some((g) => !g.members.some((m) => m.active))) errors.push("Có nhóm không có thành viên đang học.");
  return (
    <div className="flex flex-col gap-2 rounded-2xl border border-line bg-white p-4">
      <p className="text-[13px] font-bold text-ink">Kiểm tra sẵn sàng · {set.groups.length} nhóm</p>
      {errors.length > 0 && (
        <Alert tone="danger" title="Chưa phát hành được">
          <ul className="list-disc pl-4">
            {errors.map((e) => (
              <li key={e}>{e}</li>
            ))}
          </ul>
        </Alert>
      )}
      {set.ungrouped.length > 0 && (
        <Alert tone="warning">{set.ungrouped.length} sinh viên chưa có nhóm; xác nhận khi phát hành nếu vẫn muốn giao bài.</Alert>
      )}
      {errors.length === 0 && set.ungrouped.length === 0 && <Alert tone="success">Lớp đã sẵn sàng cho bài nhóm.</Alert>}
      <div className="flex flex-wrap items-center gap-2">
        <Button size="sm" icon="groups" onClick={() => window.open(`/teaching/classes/${classId}?tab=students`, "_blank", "noopener")}>
          Mở tab Sinh viên
        </Button>
        <span className="text-xs text-muted">Sửa nhóm ở tab mới; quay lại đây hộp này tự cập nhật.</span>
      </div>
    </div>
  );
}
