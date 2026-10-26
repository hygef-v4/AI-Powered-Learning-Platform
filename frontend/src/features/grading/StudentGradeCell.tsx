"use client";
import { useCallback, useState } from "react";
import { gradingApi } from "./api";
import { message, useResource } from "./useResource";
import { points } from "./types";
export function StudentGradeCell({
  classId,
  assignmentId,
}: {
  classId: string;
  assignmentId: string;
}) {
  const load = useCallback(() => gradingApi.myGrades(classId), [classId]);
  const { value, error } = useResource(load);
  const [bins, setBins] = useState<{ label: string; count: number }[] | null>(null);
  const [distributionError, setDistributionError] = useState("");
  const grade = value?.find((g) => g.assignmentId === assignmentId);
  async function distribution() {
    try {
      setBins(await gradingApi.distribution(classId, assignmentId));
      setDistributionError("");
    } catch (e) {
      setDistributionError(message(e));
    }
  }
  return (
    <div>
      {error ? (
        <p role="alert">{error}</p>
      ) : (
        <span>
          {grade ? points(grade.score, grade.maxScore) : "Chưa có điểm công bố"}
          {grade?.updated && " · Đã cập nhật"}
        </span>
      )}
      {grade && (
        <button className="ml-2 text-primary-ink" onClick={() => void distribution()}>
          Phân bố điểm
        </button>
      )}
      {bins && (
        <p>
          {bins.length
            ? bins.map((b) => `${b.label}: ${b.count}`).join(" · ")
            : "Phân bố chưa được cho phép hoặc chưa đủ dữ liệu đảm bảo riêng tư."}
        </p>
      )}
      {distributionError && <p role="alert">{distributionError}</p>}
    </div>
  );
}
