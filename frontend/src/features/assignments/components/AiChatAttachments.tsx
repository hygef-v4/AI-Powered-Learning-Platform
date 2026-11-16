"use client";

import { IconButton } from "@/shared/ui";
import type { Material } from "../model/types";
import type { useAiChatAttachments } from "../hooks/useAiChatAttachments";
import { SourceImagePreview } from "./SourceImagePreview";
import { msg } from "@/i18n/msg";

export function AiChatAttachments({ attachments, templateName, materials, disabled, onUnchoose }: {
  attachments: ReturnType<typeof useAiChatAttachments>;
  templateName?: string;
  materials: Material[];
  disabled: boolean;
  onUnchoose: (id: string) => void;
}) {
  const { sources, template, processing } = attachments;
  const locked = disabled || processing;
  if (!templateName && !materials.length && !sources.uploads.length) return null;
  const card = "flex min-w-0 items-start gap-2 rounded-lg border border-line bg-shell px-2 py-1.5";
  const action = "text-[11px] font-semibold text-primary-ink hover:underline disabled:text-muted disabled:no-underline";
  return <div className="space-y-1.5">
    <ul aria-label={msg("assignments.components.AiChatAttachments.tepGuiKemYeuCau")} data-testid="ai-revise-attachments" className="flex max-h-40 flex-col gap-1.5 overflow-y-auto">
      {templateName && <li className={card}>
        <div className="min-w-0 flex-1">
          <p className="text-[10px] font-semibold text-primary-ink">{msg("assignments.components.AiChatAttachments.mauDe")}</p>
          <p className="truncate text-xs text-ink" title={templateName}>{templateName}</p>
          {template && <button type="button" disabled={locked || !sources.canUpload} onClick={() => void attachments.demote()} className={action}>{msg("assignments.components.AiChatAttachments.dungLamThamKhao")}</button>}
        </div>
        <IconButton icon="close" label={msg("assignments.components.AiChatAttachments.go", { templateName: String(templateName) })} size={24} disabled={locked} onClick={attachments.removeTemplate} />
      </li>}
      {materials.map(item => <li key={item.materialId} className={card}>
        <div className="min-w-0 flex-1"><p className="text-[10px] text-muted">{msg("assignments.components.AiChatAttachments.thamKhaoHocLieu")}</p><p className="truncate text-xs" title={item.title}>{item.title}</p></div>
        <IconButton icon="close" label={msg("assignments.components.AiChatAttachments.go2", { title: String(item.title) })} size={24} disabled={locked} onClick={() => onUnchoose(item.materialId)} />
      </li>)}
      {sources.uploads.map(item => <li key={item.id} className={`${card} ${item.status === "ERROR" ? "border-danger" : ""}`}>
        {item.file.type.startsWith("image/") && <SourceImagePreview file={item.file} className="h-12 w-12 shrink-0" />}
        <div className="min-w-0 flex-1">
          <p className="text-[10px] text-muted">{msg("assignments.components.AiChatAttachments.thamKhaoTep")}</p>
          <p className="truncate text-xs" title={item.file.name}>{item.file.name}</p>
          <p role={item.status === "ERROR" ? "alert" : "status"} className={`break-words text-[10px] ${item.status === "ERROR" ? "text-danger" : "text-muted"}`}>
            {item.status === "ERROR" ? item.error : item.status === "UPLOADING" ? msg("assignments.components.AiChatAttachments.dangTai", { percent: String(item.percent) }) : item.status === "READING" ? msg("assignments.components.AiChatAttachments.dangDoc") : msg("assignments.components.AiChatAttachments.sanSang")}
          </p>
          <div className="flex flex-wrap gap-x-3">
            {item.status === "ERROR" && <button type="button" disabled={locked} className={action} onClick={() => sources.retry(item)}>{msg("assignments.components.AiChatAttachments.thuLai")}<span className="sr-only"> {item.file.name}</span></button>}
            {item.file.name.toLowerCase().endsWith(".docx") && <button type="button" className={action}
              aria-label={msg("assignments.components.AiChatAttachments.dungLamMauDe", { name: String(item.file.name) })} disabled={locked || item.status === "UPLOADING" || item.status === "READING"}
              onClick={() => void attachments.promote(item)}>{msg("assignments.components.AiChatAttachments.dungLamMauDe2")}</button>}
          </div>
        </div>
        <IconButton icon="close" label={msg("assignments.components.AiChatAttachments.go3", { name: String(item.file.name) })} size={24} disabled={locked} onClick={() => sources.remove(item)} />
      </li>)}
    </ul>
    {processing && <p role="status" className="text-[11px] text-muted">{msg("assignments.components.AiChatAttachments.dangChuyenVaiTroTep")}</p>}
    <p className="text-[10px] text-muted">{msg("assignments.components.AiChatAttachments.thamKhao3Tep2Mb")}</p>
  </div>;
}
