"use client";

import { useId, useState } from "react";
import { Button, Icon, TextInput } from "@/shared/ui";
import type { SelectableMaterial } from "../model/types";

const MAX_MATERIALS = 10;

export function MaterialSourceSelect({ materials, value, onChange, disabled = false }: {
  materials: SelectableMaterial[] | null;
  value: string[];
  onChange: (ids: string[]) => void;
  disabled?: boolean;
}) {
  const id = useId();
  const [search, setSearch] = useState("");
  const matches = materials?.filter((material) => material.title.toLocaleLowerCase("vi").includes(search.trim().toLocaleLowerCase("vi"))) ?? [];
  const toggle = (materialId: string) => onChange(value.includes(materialId)
    ? value.filter((selected) => selected !== materialId)
    : value.length < MAX_MATERIALS ? [...value, materialId] : value);
  const selectMatches = () => onChange([...new Set([
    ...value, ...matches.filter((material) => material.hasText).map((material) => material.materialId),
  ])].slice(0, MAX_MATERIALS));

  return <div className="flex min-w-0 flex-col gap-2">
    <details className="relative" onKeyDown={(event) => {
      if (event.key === "Escape") {
        event.currentTarget.open = false;
        event.currentTarget.querySelector("summary")?.focus();
      }
    }}>
      <summary aria-disabled={disabled} onClick={(event) => disabled && event.preventDefault()}
        className="flex cursor-pointer list-none items-center justify-between gap-2 rounded-lg border border-field bg-surface px-3 py-2 text-[13px] text-ink focus-visible:outline-focus">
        <span>{value.length ? `${value.length} tài liệu đã chọn` : "Chọn tài liệu nguồn"}</span>
        <Icon name="expand_more" size={18} />
      </summary>
      <fieldset disabled={disabled} aria-label="Tài liệu nguồn cho AI"
        className="absolute inset-x-0 top-full z-30 mt-1 flex min-w-0 flex-col gap-2 rounded-xl border border-line bg-surface p-3 shadow-lg">
        <TextInput type="search" aria-label="Tìm tài liệu nguồn" icon="search" value={search}
          placeholder="Tìm tài liệu…" onChange={(event) => setSearch(event.target.value)} />
        <div className="flex flex-wrap gap-2">
          <Button size="sm" variant="ghost" disabled={value.length >= MAX_MATERIALS || !matches.some((material) => material.hasText && !value.includes(material.materialId))}
            onClick={selectMatches}>Chọn kết quả tìm kiếm</Button>
          <Button size="sm" variant="ghost" disabled={!value.length} onClick={() => onChange([])}>Bỏ chọn tất cả</Button>
        </div>
        <ul id={id} className="flex max-h-64 flex-col gap-1 overflow-y-auto" aria-label="Danh sách tài liệu nguồn">
          {matches.map((material) => {
            const checked = value.includes(material.materialId);
            return <li key={material.materialId}>
              <label className="flex items-start gap-2 rounded-lg px-2 py-1.5 text-[13px] text-body hover:bg-shell">
                <input type="checkbox" checked={checked}
                  disabled={!checked && (!material.hasText || value.length >= MAX_MATERIALS)}
                  onChange={() => toggle(material.materialId)} data-testid="ai-draft-material" />
                <span className="min-w-0 break-words">{material.title}
                  {!material.hasText && <span className="block text-xs text-muted">Chưa có nội dung văn bản</span>}
                </span>
              </label>
            </li>;
          })}
        </ul>
        {!materials && <p role="status" className="text-xs text-muted">Đang tải tài liệu…</p>}
        {materials && !matches.length && <p className="text-xs text-muted">Không có tài liệu phù hợp.</p>}
        {materials?.length !== 0 && materials?.every((material) => !material.hasText) &&
          <p className="text-xs text-muted">Chưa có nguồn văn bản dùng được. Hãy tải tài liệu có chữ hoặc soạn không kèm tài liệu.</p>}
        <p className="border-t border-line pt-2 text-xs text-muted" aria-live="polite">Đã chọn {value.length}/{MAX_MATERIALS} tài liệu</p>
      </fieldset>
    </details>
    {value.length > 0 && <ul className="flex flex-wrap gap-1.5" aria-label="Tài liệu đã chọn">
      {value.map((materialId) => {
        const title = materials?.find((material) => material.materialId === materialId)?.title ?? "Tài liệu không còn khả dụng";
        return <li key={materialId} className="inline-flex max-w-full items-center gap-1 rounded-lg bg-primary-soft px-2 py-1 text-xs text-primary-ink">
          <span className="min-w-0 truncate">{title}</span>
          <button type="button" disabled={disabled} onClick={() => toggle(materialId)} aria-label={`Bỏ ${title}`}
            className="rounded focus-visible:outline-focus"><Icon name="close" size={14} /></button>
        </li>;
      })}
    </ul>}
  </div>;
}
