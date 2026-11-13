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
    if (!file.name.toLowerCase().endsWith(".docx")) throw new Error("Mẫu đề phải là DOCX.");
    if (file.size < 1 || file.size > 20 * 1024 * 1024) throw new Error("Tệp DOCX phải có nội dung và tối đa 20 MB.");
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
  const current = useRef<SourceUpload[]>([]);
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

  function commit(files: SourceUpload[]) {
    current.current = files;
    setUploads(files);
  }

  function update(id: string, change: (item: SourceUpload) => SourceUpload) {
    commit(current.current.map(item => item.id === id ? change(item) : item));
  }

  async function upload(file: File, id: string = crypto.randomUUID()): Promise<SourceUpload | undefined> {
    if (!policy) { report.current("Chưa tải được chính sách tệp nguồn."); return; }
    if (!current.current.some(item => item.id === id) && current.current.length >= MAX_SOURCE_FILES) {
      report.current("Chỉ chọn tối đa 3 tệp nguồn."); return;
    }
    aborts.current.get(id)?.();
    aborts.current.delete(id);
    const issue = file.size < 1 ? "Tệp không có nội dung." : checkFile(file, policy);
    const start: SourceUpload = issue ? { id, file, status: "ERROR", percent: 0, error: issue }
      : { id, file, status: "UPLOADING", percent: 0 };
    commit(current.current.some(item => item.id === id)
      ? current.current.map(item => item.id === id ? start : item) : [...current.current, start]);
    if (issue) return start;
    const controller = new AbortController();
    let stopUpload: (() => void) | undefined;
    const abort = () => { controller.abort(); stopUpload?.(); };
    aborts.current.set(id, abort);
    const active = () => aborts.current.get(id) === abort && !controller.signal.aborted;
    try {
      const handle = uploadFile(file, "ASSIGNMENT_SOURCE", percent => {
        if (active()) update(id, item => ({ ...item, percent }));
      });
      stopUpload = handle.abort;
      const result = await handle.promise;
      if (!active()) return;
      update(id, item => ({ ...item, status: "READING", percent: 100, fileRef: result.fileRef.value }));
      const previews = await assignmentsApi.previewAiSources(assignmentId, [result.fileRef.value], controller.signal);
      if (!active()) return;
      if (!previews[0]) throw new Error("Không đọc được nội dung tệp.");
      const ready: SourceUpload = { id, file, status: "READY", percent: 100, fileRef: result.fileRef.value, preview: previews[0] };
      update(id, () => ready);
      return ready;
    } catch (cause) {
      if (!active()) return;
      const failed: SourceUpload = { id, file, status: "ERROR", percent: 0,
        error: cause instanceof Error ? cause.message : "Tải tệp lên không thành công" };
      update(id, () => failed);
      return failed;
    } finally {
      if (aborts.current.get(id) === abort) aborts.current.delete(id);
    }
  }

  function add(files: FileList | File[] | null) {
    if (!files) return;
    const unique = [...new Set(Array.from(files))].filter(file => !current.current.some(item => item.file === file));
    const remaining = MAX_SOURCE_FILES - current.current.length;
    if (unique.length > remaining) report.current(`Chỉ chọn tối đa 3 tệp nguồn; ${unique.length - Math.max(remaining, 0)} tệp chưa được thêm.`);
    unique.slice(0, Math.max(0, remaining)).forEach(file => void upload(file));
  }

  function remove(item: SourceUpload) {
    aborts.current.get(item.id)?.();
    aborts.current.delete(item.id);
    commit(current.current.filter(file => file.id !== item.id));
  }

  function clear() {
    aborts.current.forEach(abort => abort());
    aborts.current.clear();
    commit([]);
  }

  function restore(files: SourceUpload[]) {
    clear();
    commit(files.filter(item => item.status === "READY").slice(0, MAX_SOURCE_FILES));
  }

  return {
    uploads,
    canUpload: !!policy && uploads.length < MAX_SOURCE_FILES,
    accept: policy ? acceptAttribute(policy) : undefined,
    pending: uploads.some(item => item.status !== "READY"),
    fileRefs: uploads.flatMap(item => item.status === "READY" && item.fileRef ? [item.fileRef] : []),
    add,
    addOne: (file: File) => upload(file),
    remove,
    retry: (item: SourceUpload) => void upload(item.file, item.id),
    restore,
    clear,
  };
}
