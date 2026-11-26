"use client";

import { useCallback } from "react";
import { gradingApi } from "../api";
import type { SubmissionPage, SubmissionRow } from "../model/types";
import { useResource } from "./useResource";

// Cung cấp toàn bộ hàng bài nộp cùng quyền chỉnh sửa cho bộ chọn và bảng theo lô.
export function useSubmissionDirectory(classId: string, assignmentId: string) {
  // Duyệt các trang bài nộp đến đủ số lượng, giữ metadata quyền từ phản hồi server.
  const load = useCallback(async () => {
    const rows: SubmissionRow[] = [];
    let editable: SubmissionPage["editable"];
    let readOnlyReason: SubmissionPage["readOnlyReason"];
    for (let page = 1; ; page += 1) {
      const result = await gradingApi.listSubmissions(classId, assignmentId, "ALL", page);
      if (editable !== false) {
        editable = result.editable;
        readOnlyReason = result.readOnlyReason;
      }
      rows.push(...result.rows);
      if (!result.rows.length || rows.length >= result.total) return { rows, editable, readOnlyReason };
    }
  }, [classId, assignmentId]);
  const resource = useResource(load);
  const { refresh: refreshDirectory } = resource;
  // Làm mới danh sách và trả riêng các hàng cho caller cần cập nhật nội dung.
  const refresh = useCallback(async () => (await refreshDirectory()).rows, [refreshDirectory]);
  return { ...resource, value: resource.value?.rows ?? null, refresh,
    editable: resource.value?.editable, readOnlyReason: resource.value?.readOnlyReason };
}
