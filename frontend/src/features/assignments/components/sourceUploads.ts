"use client";

import { useEffect, useRef, useState } from "react";
import { getPolicies, uploadFile } from "@/features/files/api";
import { acceptAttribute, checkFile, type PurposePolicy } from "@/features/files/model/purposes";
import { assignmentsApi } from "../api";
import { describeError } from "../model/format";
import { extractTemplateText } from "../model/templateText";
import type { AiSourcePreview } from "../model/types";
import { msg } from "@/i18n/msg";

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
    if (!file.name.toLowerCase().endsWith(".docx")) throw new Error(msg("assignments.components.sourceUploads.mauDePhaiLaDocx"));
    if (file.size < 1 || file.size > 20 * 1024 * 1024) throw new Error(msg("assignments.components.sourceUploads.tepDocxPhaiCoNoiDung"));
    const preview = await assignmentsApi.importDocx(assignmentId, file);
    const { text, questionCount } = extractTemplateText(preview.document);
    if (!text.trim()) throw new Error(msg("assignments.components.sourceUploads.mauDeKhongCoNoiDung"));
    return { file, status: "READY", text, questionCount };
  } catch (cause) {
    return { file, status: "ERROR", error: describeError(cause) };
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
        if (active) report.current(msg("assignments.components.sourceUploads.khongTaiDuocGioiHanTep"));
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

  function updateUpload(id: string, change: (item: SourceUpload) => SourceUpload) {
    commit(current.current.map(item => item.id === id ? change(item) : item));
  }

  async function upload(file: File, id: string = crypto.randomUUID()): Promise<SourceUpload | undefined> {
    if (!policy) { report.current(msg("assignments.components.sourceUploads.chuaTaiDuocChinhSachTep")); return; }
    if (!current.current.some(item => item.id === id) && current.current.length >= MAX_SOURCE_FILES) {
      report.current(msg("assignments.components.sourceUploads.chiChonToiDa3Tep")); return;
    }
    aborts.current.get(id)?.();
    aborts.current.delete(id);
    const issue = file.size < 1 ? msg("assignments.components.sourceUploads.tepKhongCoNoiDung") : checkFile(file, policy);
    const start: SourceUpload = issue ? { id, file, status: "ERROR", percent: 0, error: issue }
      : { id, file, status: "UPLOADING", percent: 0 };
    commit(current.current.some(item => item.id === id)
      ? current.current.map(item => item.id === id ? start : item) : [...current.current, start]);
    if (issue) return start;
    const controller = new AbortController();
    let stopUpload: (() => void) | undefined;
    const abortUpload = () => { controller.abort(); stopUpload?.(); };
    aborts.current.set(id, abortUpload);
    const isUploadActive = () => aborts.current.get(id) === abortUpload && !controller.signal.aborted;
    try {
      const handle = uploadFile(file, "ASSIGNMENT_SOURCE", percent => {
        if (isUploadActive()) updateUpload(id, item => ({ ...item, percent }));
      });
      stopUpload = handle.abort;
      const result = await handle.promise;
      if (!isUploadActive()) return;
      updateUpload(id, item => ({ ...item, status: "READING", percent: 100, fileRef: result.fileRef.value }));
      const previews = await assignmentsApi.previewAiSources(assignmentId, [result.fileRef.value], controller.signal);
      if (!isUploadActive()) return;
      if (!previews[0]) throw new Error(msg("assignments.components.sourceUploads.khongDocDuocNoiDungTep"));
      const ready: SourceUpload = { id, file, status: "READY", percent: 100, fileRef: result.fileRef.value, preview: previews[0] };
      updateUpload(id, () => ready);
      return ready;
    } catch (cause) {
      if (!isUploadActive()) return;
      const failed: SourceUpload = { id, file, status: "ERROR", percent: 0,
        error: cause instanceof Error ? cause.message : msg("assignments.components.sourceUploads.taiTepLenKhongThanhCong") };
      updateUpload(id, () => failed);
      return failed;
    } finally {
      if (aborts.current.get(id) === abortUpload) aborts.current.delete(id);
    }
  }

  function addFiles(files: FileList | File[] | null) {
    if (!files) return;
    const unique = [...new Set(Array.from(files))].filter(file => !current.current.some(item => item.file === file));
    const remaining = MAX_SOURCE_FILES - current.current.length;
    if (unique.length > remaining) report.current(msg("assignments.components.sourceUploads.chiChonToiDa3Tep2", { value: String(unique.length - Math.max(remaining, 0)) }));
    unique.slice(0, Math.max(0, remaining)).forEach(file => void upload(file));
  }

  function removeUpload(item: SourceUpload) {
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
    add: addFiles,
    addOne: (file: File) => upload(file),
    remove: removeUpload,
    retry: (item: SourceUpload) => void upload(item.file, item.id),
    restore,
    clear,
  };
}
