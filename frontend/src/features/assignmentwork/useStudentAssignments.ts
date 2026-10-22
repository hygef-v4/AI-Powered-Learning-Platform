"use client";

import { useEffect, useState } from "react";
import { workApi } from "./api";
import type { StudentRow } from "./types";

type Loaded = { key: string; rows: StudentRow[] | null; error: unknown };

/** Danh sách bài của tôi (F1), lọc sẵn lớp nếu có. */
export function useStudentAssignments(classId?: string) {
  const key = classId ?? "";
  const [loaded, setLoaded] = useState<Loaded>({ key: "\u0000", rows: null, error: null });
  useEffect(() => {
    const controller = new AbortController();
    workApi.list(key || undefined, controller.signal).then(
      (rows) => { if (!controller.signal.aborted) setLoaded({ key, rows, error: null }); },
      (error) => { if (!controller.signal.aborted) setLoaded({ key, rows: null, error }); },
    );
    return () => controller.abort();
  }, [key]);
  const current = loaded.key === key;
  return { rows: current ? loaded.rows : null, error: current ? loaded.error : null, loading: !current };
}

/** Bài còn làm được: đang mở, chưa nộp hoặc đang làm. */
export function upcoming(row: StudentRow): boolean {
  return row.status === "OPEN" && (row.myStatus === "NOT_STARTED" || row.myStatus === "IN_PROGRESS" || row.myStatus === "GROUP");
}
