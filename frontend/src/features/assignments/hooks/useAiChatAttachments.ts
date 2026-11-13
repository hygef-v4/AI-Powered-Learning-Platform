"use client";

import { useEffect, useRef, useState } from "react";
import { readTemplateFile, useSourceUploads, type SourceUpload, type TemplateFile } from "../components/sourceUploads";

type Replacement = { item: SourceUpload } | { templateId: string };
export type AttachmentSnapshot = { template: TemplateFile | null; templateId: string; uploads: SourceUpload[] };

export function useAiChatAttachments(assignmentId: string, onError: (message: string) => void, enabled: boolean) {
  const sources = useSourceUploads(assignmentId, onError, enabled);
  const [template, setTemplate] = useState<TemplateFile | null>(null);
  const [templateId, setTemplateId] = useState("");
  const [replacement, setReplacement] = useState<Replacement | null>(null);
  const [processing, setProcessing] = useState(false);
  const operation = useRef(0);
  const changing = useRef(false);
  useEffect(() => () => { operation.current++; }, []);

  async function promote(item: SourceUpload, confirmed = false) {
    if (changing.current) return;
    if ((template || templateId) && !confirmed) { setReplacement({ item }); return; }
    const version = ++operation.current;
    changing.current = true;
    setProcessing(true);
    const result = await readTemplateFile(assignmentId, item.file);
    if (version !== operation.current) return;
    changing.current = false;
    setProcessing(false);
    if (result.status !== "READY") { onError(result.error ?? "Không đọc được mẫu đề."); return; }
    setTemplate(result);
    setTemplateId("");
    sources.remove(item);
  }

  async function demote() {
    if (!template || changing.current) return;
    const version = ++operation.current;
    changing.current = true;
    setProcessing(true);
    const result = await sources.addOne(template.file);
    if (version !== operation.current) return;
    changing.current = false;
    setProcessing(false);
    if (result?.status === "READY") setTemplate(null);
    else if (result) { sources.remove(result); onError(result.error ?? "Không thể dùng tệp làm tham khảo."); }
  }

  function chooseExisting(id: string, confirmed = false) {
    if (changing.current || id === templateId) return;
    if ((template || templateId) && !confirmed) { setReplacement({ templateId: id }); return; }
    setTemplate(null);
    setTemplateId(id);
  }

  function removeTemplate() {
    operation.current++;
    changing.current = false;
    setProcessing(false);
    setTemplate(null);
    setTemplateId("");
  }

  function clear() {
    removeTemplate();
    setReplacement(null);
    sources.clear();
  }

  function restore(snapshot: AttachmentSnapshot) {
    removeTemplate();
    sources.restore(snapshot.uploads);
    setTemplate(snapshot.template);
    setTemplateId(snapshot.templateId);
  }

  return {
    sources, template, templateId, replacement, processing,
    pending: sources.pending || processing,
    addFiles: sources.add,
    promote, demote, chooseExisting, removeTemplate, clear, restore,
    snapshot: (): AttachmentSnapshot => ({ template, templateId, uploads: [...sources.uploads] }),
    cancelReplacement: () => setReplacement(null),
    confirmReplacement: async () => {
      const next = replacement;
      setReplacement(null);
      if (next && "item" in next) await promote(next.item, true);
      else if (next) chooseExisting(next.templateId, true);
    },
  };
}
