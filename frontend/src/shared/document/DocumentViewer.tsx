"use client";

import { Fragment, type CSSProperties, type ReactNode } from "react";
import { Icon, cx } from "@/shared/ui";
import {
  diagramTypeLabels,
  svgDataUri,
  type Align,
  type DocumentBlock,
  type DocumentModel,
  type ListItem,
  type Run,
} from "./types";

export type ImageUrl = (blockId: string) => string | null;

/** Cỡ chữ tiêu đề dùng chung cho trang xem và trang soạn để hai bên trông như nhau. */
export const HEADING_CLASS: Record<string, string> = {
  TITLE: "text-[26px] leading-tight font-bold text-ink",
  SUBTITLE: "text-base text-muted",
  H1: "text-[21px] leading-snug font-bold text-ink",
  H2: "text-lg leading-snug font-bold text-ink",
  H3: "text-base font-bold text-ink",
  H4: "text-[15px] font-semibold italic text-ink",
  H5: "text-sm font-semibold text-ink",
  H6: "text-sm italic text-body",
};

export const PARAGRAPH_CLASS = "text-sm leading-[1.65] text-ink";

export const ALIGN_CLASS: Record<Align, string> = {
  LEFT: "text-left", CENTER: "text-center", RIGHT: "text-right", JUSTIFY: "text-justify",
};

export function alignClass(align: Align | undefined): string {
  return align ? ALIGN_CLASS[align] : "";
}

/** Kiểu chữ của một đoạn: đậm, nghiêng, mã theo lớp; gạch chân, gạch ngang, màu chữ, màu tô nền theo style. */
export function runClass(run: Run | undefined): string {
  return cx(run?.bold && "font-bold", run?.italic && "italic", run?.code && "font-mono text-[13px]");
}

export function runStyle(run: Run | undefined): CSSProperties {
  const lines = [run?.underline && "underline", run?.strike && "line-through"].filter(Boolean).join(" ");
  return {
    ...(lines ? { textDecorationLine: lines } : {}),
    ...(run?.color ? { color: run.color } : {}),
    ...(run?.highlight ? { backgroundColor: run.highlight } : {}),
  };
}

export function RunsView({ runs }: { runs: Run[] | undefined }) {
  return (
    <>
      {(runs ?? []).map((run, index) => {
        const style = runStyle(run);
        const plain = !run.bold && !run.italic && !run.code && Object.keys(style).length === 0;
        return plain ? <Fragment key={index}>{run.text}</Fragment>
          : <span key={index} className={cx(runClass(run), run.code && "rounded bg-shell px-1")} style={style}>{run.text}</span>;
      })}
    </>
  );
}

function ListView({ ordered, items, align }: { ordered?: boolean; items: ListItem[]; align?: Align }) {
  const Tag = ordered ? "ol" : "ul";
  return (
    <Tag className={cx(ordered ? "list-decimal" : "list-disc", "pl-6", PARAGRAPH_CLASS, alignClass(align))}>
      {items.map((item, index) => (
        <li key={index} style={{ marginLeft: `${(item.level ?? 0) * 24}px` }} className="whitespace-pre-wrap">
          <RunsView runs={item.runs} />
        </li>
      ))}
    </Tag>
  );
}

/** Chú thích dưới ảnh, sơ đồ (lưu ở {@code alt}). */
function Caption({ children }: { children: ReactNode }) {
  return <figcaption className="text-center text-xs text-muted italic">{children}</figcaption>;
}

/** Một block chỉ đọc; ảnh mở qua {@code imageUrl}, sơ đồ hiện ảnh SVG xem trước, bấm vào thì vẽ (nếu được). */
export function BlockView({ block, imageUrl, previewUrl, onOpenDiagram }: {
  block: DocumentBlock;
  imageUrl?: ImageUrl;
  previewUrl?: string | null;
  onOpenDiagram?: (blockId: string) => void;
}) {
  switch (block.type) {
    case "HEADING":
      return <p className={cx(HEADING_CLASS[block.content.style] ?? HEADING_CLASS.H1, alignClass(block.content.align))}>{block.content.text || "(Chưa đặt tên)"}</p>;
    case "PARAGRAPH":
      return <p className={cx("whitespace-pre-wrap", PARAGRAPH_CLASS, alignClass(block.content.align))}><RunsView runs={block.content.runs} /></p>;
    case "LIST":
      return <ListView ordered={block.content.ordered} items={block.content.items} align={block.content.align} />;
    case "TABLE":
      return (
        <div className="overflow-x-auto">
          <table className="w-full border-collapse text-[13px]">
            <tbody>
              {block.content.rows.map((row, r) => (
                <tr key={r}>
                  {row.map((cell, c) => {
                    const Cell = block.content.headerRow && r === 0 ? "th" : "td";
                    return (
                      <Cell key={c} className={cx("border border-line-strong px-2.5 py-1.5 text-left align-top whitespace-pre-wrap", Cell === "th" && "bg-canvas font-semibold")}>
                        <RunsView runs={cell.runs} />
                      </Cell>
                    );
                  })}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      );
    case "IMAGE": {
      const src = previewUrl ?? (block.content.hasFile ? imageUrl?.(block.id) : null);
      return (
        <figure className="flex flex-col items-center gap-1">
          {src ? (
            // Ảnh riêng tư qua link tải có hạn của U03, không qua tối ưu ảnh của Next.
            // eslint-disable-next-line @next/next/no-img-element
            <img src={src} alt={block.content.alt ?? ""} style={{ width: block.content.width ? `${block.content.width}px` : undefined }} className="max-w-full" />
          ) : (
            <span className="flex w-full items-center justify-center rounded-lg border border-dashed border-line-strong p-6 text-muted"><Icon name="upload_file" size={20} /></span>
          )}
          {block.content.alt && <Caption>{block.content.alt}</Caption>}
        </figure>
      );
    }
    case "DIAGRAM": {
      // Khung vẽ như bản demo (yêu cầu 2026-10-22): rộng hết trang, viền đứt, bấm vào là mở Draw.io; đã vẽ thì hiện hình.
      const svg = block.content.svgPreview;
      const drawn = !!svg;
      return (
        <figure className="flex flex-col gap-1">
          <button type="button" onClick={() => onOpenDiagram?.(block.id)} disabled={!onOpenDiagram}
            aria-label={onOpenDiagram ? (drawn ? "Sửa sơ đồ" : "Vẽ sơ đồ") : undefined}
            className={cx("flex min-h-60 w-full items-center justify-center overflow-hidden rounded-lg p-0 transition-colors",
              drawn ? "bg-white" : "bg-canvas",
              onOpenDiagram ? "cursor-pointer border-2 border-dashed border-line-strong hover:border-primary"
                : drawn ? "cursor-default border border-line" : "cursor-default border-2 border-dashed border-line-strong")}>
            {svg ? (
              // eslint-disable-next-line @next/next/no-img-element
              <img src={svgDataUri(svg)} alt={block.content.alt || diagramTypeLabels[block.content.diagramType]}
                className="mx-auto block h-auto max-h-[560px] max-w-full p-3" />
            ) : (
              <span className="flex flex-col items-center gap-2 text-[13px] text-muted">
                <svg aria-hidden width={40} height={40} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={1.5} strokeLinecap="round" strokeLinejoin="round">
                  <rect x="3" y="3" width="7" height="7" rx="1" /><rect x="14" y="14" width="7" height="7" rx="1" /><path d="M10 6.5h4a2 2 0 0 1 2 2V14" />
                </svg>
                {onOpenDiagram ? "Vẽ sơ đồ" : "Chưa có sơ đồ"}
              </span>
            )}
          </button>
          {block.content.alt && <Caption>{block.content.alt}</Caption>}
        </figure>
      );
    }
  }
}

/** Tài liệu chỉ đọc; block của người soạn có thể làm nhạt khi xem bài làm. */
export function DocumentViewer({ document, imageUrl, markTeacherBlocks = false, empty = "Tài liệu trống." }: {
  document: DocumentModel;
  imageUrl?: ImageUrl;
  markTeacherBlocks?: boolean;
  empty?: string;
}) {
  if (document.blocks.length === 0) return <p className="text-sm text-muted">{empty}</p>;
  return (
    <div className="flex flex-col gap-3">
      {document.blocks.map((block) => (
        <div key={block.id} className={markTeacherBlocks && block.origin === "TEACHER" && block.type !== "HEADING" ? "opacity-70" : undefined}>
          <BlockView block={block} imageUrl={imageUrl} />
        </div>
      ))}
    </div>
  );
}
