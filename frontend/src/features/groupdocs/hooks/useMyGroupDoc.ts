"use client";

import { useEffect, useState } from "react";
import { ApiError } from "@/shared/api/client";
import { getMyGroupDoc } from "../api";
import type { WorkspaceView } from "../model/types";

/** Tài liệu nhóm của mình cho một bài; chưa có nhóm hoặc lỗi thì trả lời báo thay cho tài liệu. */
export function useMyGroupDoc(assignmentId: string) {
  const [state, setState] = useState<{ assignmentId: string; view: WorkspaceView | null; message: string | null } | null>(null);

  useEffect(() => {
    let active = true;
    getMyGroupDoc(assignmentId).then(
      (view) => {
        if (active) setState({ assignmentId, view, message: null });
      },
      (e: unknown) => {
        if (!active) return;
        const message = e instanceof ApiError && e.problem.code === "GROUP_NONE" ? "Bạn chưa có nhóm cho bài này." : "Chưa mở được tài liệu nhóm.";
        setState({ assignmentId, view: null, message });
      },
    );
    return () => {
      active = false;
    };
  }, [assignmentId]);

  const current = state?.assignmentId === assignmentId ? state : null;
  return { view: current?.view ?? null, message: current?.message ?? null };
}
