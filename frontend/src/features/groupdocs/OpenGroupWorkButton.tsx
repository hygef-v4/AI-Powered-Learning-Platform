"use client";

import { useEffect, useState } from "react";
import { ApiError } from "@/shared/api/client";
import { ButtonLink, Section } from "@/shared/ui";
import { getMyGroupDoc } from "./api";
import type { WorkspaceView } from "./types";

/** Trên Assignment Detail bài nhóm (s12d): nhóm, mục của bạn, tiến độ, nút Mở tài liệu nhóm. */
export function OpenGroupWorkButton({ assignmentId }: { assignmentId: string; classId: string }) {
  const [view, setView] = useState<WorkspaceView | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    getMyGroupDoc(assignmentId).then(
      (loaded) => {
        if (active) setView(loaded);
      },
      (e: unknown) => {
        if (!active) return;
        setMessage(e instanceof ApiError && e.problem.code === "GROUP_NONE" ? "Bạn chưa có nhóm cho bài này." : "Chưa mở được tài liệu nhóm.");
      },
    );
    return () => {
      active = false;
    };
  }, [assignmentId]);

  if (message) return <p className="text-[13px] text-muted">{message}</p>;
  if (!view) return null;
  const leader = view.members.find((m) => m.leader)?.name ?? "chưa có";
  const mine = view.sections.filter((s) => s.holderId === view.viewerId).map((s) => s.title);
  const done = view.sections.filter((s) => s.status === "DONE").length;
  return (
    <Section title="Bài nhóm" icon="groups" tone="accent">
      <dl className="grid grid-cols-[110px_1fr] gap-y-1 text-[13px]">
        <dt className="text-muted">Nhóm</dt>
        <dd>
          {view.groupName} · trưởng nhóm {leader}
        </dd>
        <dt className="text-muted">Mục của bạn</dt>
        <dd>{mine.length ? mine.join(", ") : "Chưa giữ mục nào"}</dd>
        <dt className="text-muted">Tiến độ</dt>
        <dd>
          {done} / {view.sections.length} mục Xong
        </dd>
      </dl>
      <div>
        <ButtonLink href={`/group-docs/${view.documentId}`} variant="primary">
          Mở tài liệu nhóm
        </ButtonLink>
      </div>
    </Section>
  );
}
