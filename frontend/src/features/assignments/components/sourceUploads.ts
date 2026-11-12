"use client";

import { useEffect, useRef, useState } from "react";
import { getPolicies, uploadFile } from "@/features/files/api";
import { acceptAttribute, checkFile, type PurposePolicy } from "@/features/files/model/purposes";
import { assignmentsApi } from "../api";
import { errorMessage } from "../model/format";
import { templateText } from "../model/templateText";
import type { AiSourcePreview } from "../model/types";

export const MAX_SOURCE_FILES = 3;

export type SourceUpload = {
  id: string;
  file: File;
  status: "UPLOADING" | "READING" | "READY" | "ERROR";
  percent: number;
  fileRef?: string;
  error?: string;
  preview?: AiSourcePreview;
};

/** Mẫu đề: DOCX nhập qua bộ nhập khung đề (tối đa 20 MB, không giới hạn 2 MB của tệp nguồn), gửi AI dạng chữ. */
export type TemplateFile = { file: File; status: "READING" | "READY" | "ERROR"; text?: string; questionCount?: number; error?: string };

/** Đọc mẫu đề bằng bộ nhập DOCX; số câu hỏi là số mục ngoài cùng của mẫu. Không đọc được thì trả trạng thái lỗi. */
export async function readTemplateFile(assignmentId: string, file: File): Promise<TemplateFile> {
  try {
    const preview = await assignmentsApi.importDocx(assignmentId, file);
    const { text, questionCount } = templateText(preview.document);
    if (!text.trim()) throw new Error("Mẫu đề không có nội dung đọc được.");
    return { file, status: "READY", text, questionCount };
  } catch (cause) {
    return { file, status: "ERROR", error: errorMessage(cause) };
  }
}

/**
 * Tệp tài liệu tham khảo tải lên cho AI (tối đa 3 tệp): tải lên, đọc thử nội dung, gỡ, thử lại. Dùng chung cho popup Tạo
 * nháp bằng AI và khung chat sửa đề. {@code enabled}: chỉ tải giới hạn tệp khi cần (khung chat đang mở).
 */
export function useSourceUploads(assignmentId: string, onError: (message: string) => void, enabled = true) {
  const [policy, setPolicy] = useState<PurposePolicy | null>(null);
  const [uploads, setUploads] = useState<SourceUpload[]>([]);
  const aborts = useRef(new Map<string, () => void>());
  const report = useRef(onError);
  useEffect(() => {
    report.current = onError;
  }, [onError]);

  useEffect(() => {
    if (!enabled) return;
    let active = true;
    getPolicies()
      .then((policies) => {
        if (active) setPolicy(policies.find((item) => item.purpose === "ASSIGNMENT_SOURCE") ?? null);
      })
      .catch(() => {
        if (active) report.current("Không tải được giới hạn tệp nguồn.");
      });
    return () => {
      active = false;
    };
  }, [enabled]);

  // Rời trang: dừng các lượt tải đang chạy.
  useEffect(() => {
    const pending = aborts.current;
    return () => {
      pending.forEach((abort) => abort());
      pending.clear();
    };
  }, []);

  function update(id: string, change: (item: SourceUpload) => SourceUpload) {
    setUploads((files) => files.map((item) => (item.id === id ? change(item) : item)));
  }

  async function upload(file: File, id: string = crypto.randomUUID()) {
    if (!policy) {
      report.current("Chưa tải được chính sách tệp nguồn.");
      return;
    }
    const issue = checkFile(file, policy);
    const start: SourceUpload = issue
      ? { id, file, status: "ERROR", percent: 0, error: issue }
      : { id, file, status: "UPLOADING", percent: 0 };
    setUploads((files) => (files.some((item) => item.id === id)
      ? files.map((item) => (item.id === id ? start : item)) : [...files, start]));
    if (issue) return;
    const handle = uploadFile(file, "ASSIGNMENT_SOURCE", (percent) => update(id, (item) => ({ ...item, percent })));
    const controller = new AbortController();
    aborts.current.set(id, () => {
      handle.abort();
      controller.abort();
    });
    try {
      const result = await handle.promise;
      update(id, (item) => ({ ...item, status: "READING", percent: 100, fileRef: result.fileRef.value }));
      const previews = await assignmentsApi.previewAiSources(assignmentId, [result.fileRef.value], controller.signal);
      if (!previews[0]) throw new Error("Không đọc được nội dung tệp.");
      update(id, (item) => ({ ...item, status: "READY", preview: previews[0] }));
    } catch (cause) {
      update(id, (item) => ({
        ...item,
        status: "ERROR",
        error: cause instanceof Error ? cause.message : "Tải tệp lên không thành công",
      }));
    } finally {
      aborts.current.delete(id);
    }
  }

  function add(files: FileList | File[] | null) {
    if (!files) return;
    const remaining = MAX_SOURCE_FILES - uploads.length;
    if (files.length > remaining) report.current("Chỉ chọn tối đa 3 tệp nguồn.");
    Array.from(files).slice(0, Math.max(0, remaining)).forEach((file) => void upload(file));
  }

  function remove(item: SourceUpload) {
    aborts.current.get(item.id)?.();
    aborts.current.delete(item.id);
    setUploads((files) => files.filter((file) => file.id !== item.id));
  }

  /** Dừng mọi lượt tải và bỏ hết tệp (đóng popup, hoặc đã gửi kèm yêu cầu). */
  function clear() {
    aborts.current.forEach((abort) => abort());
    aborts.current.clear();
    setUploads([]);
  }

  return {
    uploads,
    canUpload: !!policy && uploads.length < MAX_SOURCE_FILES,
    accept: policy ? acceptAttribute(policy) : undefined,
    /** Còn tệp chưa sẵn sàng (đang tải, đang đọc hoặc lỗi). */
    pending: uploads.some((item) => item.status !== "READY"),
    fileRefs: uploads.flatMap((item) => (item.fileRef ? [item.fileRef] : [])),
    add,
    remove,
    retry: (item: SourceUpload) => void upload(item.file, item.id),
    clear,
  };
}
