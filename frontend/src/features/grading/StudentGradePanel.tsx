"use client";
import { useCallback } from "react";
import { gradingApi } from "./api";
import { useResource } from "./useResource";
import { points } from "./types";
export function StudentGradePanel({ attemptId }: { attemptId: string }) {
  const load = useCallback(() => gradingApi.attempt(attemptId), [attemptId]);
  const { value, error } = useResource(load);
  return (
    <section className="rounded-xl border p-4">
      <h3 className="font-bold">Kết quả chính thức</h3>
      {error && <p role="alert">{error}</p>}
      {value?.score != null ? (
        <>
          <p>
            {points(value.score, value.maxScore)} {value.updated && "· Đã cập nhật"}
          </p>
          <p className="whitespace-pre-wrap">{value.feedback}</p>
          <p>
            Công bố: {value.publishedAt ? new Date(value.publishedAt).toLocaleString("vi-VN") : "—"}
          </p>
        </>
      ) : (
        !error && <p>Chưa có điểm được công bố.</p>
      )}
    </section>
  );
}
