"use client";

import { useEffect, useState } from "react";
import { workApi } from "./api";
import type { Tone } from "@/shared/ui/tones";
import { myStatusLabels, myStatusTones, type StudentRow } from "./types";

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

/** Đang làm: lượt làm dở của mình, hoặc bài nhóm nhóm đã bắt đầu (có tài liệu nhóm) mà chưa nộp; chỉ bài còn mở. */
export function doing(row: StudentRow): boolean {
  if (row.status !== "OPEN") return false;
  return row.myStatus === "IN_PROGRESS" || (row.myStatus === "GROUP" && !!row.group?.groupDocumentId && !row.group.submitted);
}

/**
 * Trạng thái hiện trên dòng bài: bài nhóm không có nhãn riêng mà theo tiến độ của nhóm (Chưa làm, Đang làm, Đã nộp), như
 * bài cá nhân.
 */
export function rowStatus(row: StudentRow): { label: string; tone: Tone } {
  if (doing(row)) return { label: myStatusLabels.IN_PROGRESS, tone: myStatusTones.IN_PROGRESS };
  if (row.myStatus === "GROUP") {
    const key = row.group?.submitted ? "SUBMITTED" : "NOT_STARTED";
    return { label: myStatusLabels[key], tone: myStatusTones[key] };
  }
  return { label: myStatusLabels[row.myStatus], tone: myStatusTones[row.myStatus] };
}

/** Bài còn làm được: đang mở, chưa nộp hoặc đang làm. */
export function upcoming(row: StudentRow): boolean {
  return row.status === "OPEN" && (row.myStatus === "NOT_STARTED" || row.myStatus === "IN_PROGRESS" || row.myStatus === "GROUP");
}
