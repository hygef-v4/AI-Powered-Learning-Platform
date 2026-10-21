"use client";

import { useCallback, useEffect, useState } from "react";
import { Alert, Button, Pill, Section } from "@/shared/ui";
import { getGroups } from "./api";
import { GroupEditor, personLabel } from "./GroupEditor";
import { GroupProgressView } from "./GroupProgressView";
import { LeaderRequestList } from "./LeaderRequestList";
import { RandomSplitDialog } from "./RandomSplitDialog";
import type { Draft, GroupSetView } from "./types";

type Mode = { kind: "view" } | { kind: "edit"; initial?: Draft[] } | { kind: "progress" };

/**
 * Phần Nhóm trên tab Sinh viên của Teacher Class Detail (#32, UC 57): lớp chưa có nhóm thì có Chia ngẫu nhiên và Tạo
 * nhóm; lớp có nhóm thì thẻ từng nhóm, Sửa nhóm, Tiến độ bài nhóm, yêu cầu đổi trưởng nhóm đang chờ.
 */
export function ClassGroupsPanel({ classId }: { classId: string }) {
  const [set, setSet] = useState<GroupSetView | null>(null);
  const [mode, setMode] = useState<Mode>({ kind: "view" });
  const [splitting, setSplitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(
    () =>
      getGroups(classId).then(
        (loaded) => {
          setSet(loaded);
          setError(null);
        },
        () => setError("Không tải được nhóm của lớp, thử lại sau."),
      ),
    [classId],
  );

  useEffect(() => {
    let active = true;
    getGroups(classId).then(
      (loaded) => {
        if (active) setSet(loaded);
      },
      () => {
        if (active) setError("Không tải được nhóm của lớp, thử lại sau.");
      },
    );
    return () => {
      active = false;
    };
  }, [classId]);

  if (error && !set) {
    return (
      <Alert tone="danger" role="alert" action={<Button size="sm" onClick={() => void load()}>Tải lại</Button>}>
        {error}
      </Alert>
    );
  }
  if (!set) return <p className="text-[13px] text-muted">Đang tải nhóm...</p>;

  const archived = set.classStatus === "ARCHIVED";
  const students = set.groups.reduce((n, g) => n + g.members.filter((m) => m.active).length, 0) + set.ungrouped.length;

  if (mode.kind === "edit") {
    return (
      <GroupEditor
        classId={classId}
        set={set}
        initial={mode.initial}
        onCancel={() => setMode({ kind: "view" })}
        onSaved={(next) => {
          setSet(next);
          setMode({ kind: "view" });
        }}
      />
    );
  }
  if (mode.kind === "progress") {
    return <GroupProgressView classId={classId} set={set} onBack={() => setMode({ kind: "view" })} />;
  }

  return (
    <div className="flex flex-col gap-4">
      <LeaderRequestList set={set} onChanged={() => void load()} />
      <Section
        title="Nhóm"
        icon="groups"
        description={
          set.groups.length === 0
            ? `${students} sinh viên · lớp chưa có nhóm`
            : `${set.groups.length} nhóm · ${students} sinh viên · ${set.ungrouped.length} chưa có nhóm`
        }
        actions={
          <>
            {set.groups.length > 0 && (
              <Button size="sm" icon="monitoring" onClick={() => setMode({ kind: "progress" })}>
                Tiến độ bài nhóm
              </Button>
            )}
            {!archived && set.ungrouped.length > 0 && (
              <Button size="sm" icon="groups" onClick={() => setSplitting(true)}>
                Chia ngẫu nhiên
              </Button>
            )}
            {!archived && (
              <Button size="sm" variant="primary" icon={set.groups.length ? "tune" : "add"} onClick={() => setMode({ kind: "edit" })}>
                {set.groups.length ? "Sửa nhóm" : "Tạo nhóm"}
              </Button>
            )}
          </>
        }
      >
        {archived && <Alert tone="info">Lớp đã lưu trữ, chỉ xem được nhóm.</Alert>}
        {set.groups.length === 0 ? (
          <p className="text-[13px] text-muted">
            Mọi bài nhóm của lớp dùng chung các nhóm này. Tạo tay hoặc chia ngẫu nhiên rồi chọn trưởng nhóm cho từng nhóm.
          </p>
        ) : (
          <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-3">
            {set.groups.map((g) => (
              <article key={g.id} className="flex flex-col gap-2 rounded-2xl border border-line p-3.5">
                <div className="flex items-center gap-2">
                  <h3 className="text-[14px] font-bold text-ink">{g.name}</h3>
                  <span className="text-xs text-muted">{g.members.filter((m) => m.active).length} thành viên</span>
                  {!g.leaderId && <Pill tone="danger">Thiếu trưởng nhóm</Pill>}
                </div>
                <ul className="flex flex-col gap-1 text-[13px]">
                  {g.members.map((m) => (
                    <li key={m.accountId} className="flex items-center gap-2">
                      <span className={m.active ? "text-ink" : "text-muted line-through"}>{personLabel(m)}</span>
                      {m.email && <span className="truncate text-[11px] text-muted">{m.email}</span>}
                      {m.leader && m.active && <Pill tone="warning">Trưởng nhóm</Pill>}
                      {!m.active && <Pill tone="neutral">Đã rời lớp</Pill>}
                    </li>
                  ))}
                </ul>
              </article>
            ))}
          </div>
        )}
        {set.groups.length > 0 && set.ungrouped.length > 0 && (
          <p className="text-xs text-muted">Chưa có nhóm: {set.ungrouped.map(personLabel).join(", ")}</p>
        )}
      </Section>
      {splitting && (
        <RandomSplitDialog
          classId={classId}
          set={set}
          onClose={() => setSplitting(false)}
          onPreview={(drafts) => {
            setSplitting(false);
            setMode({ kind: "edit", initial: drafts });
          }}
        />
      )}
    </div>
  );
}
