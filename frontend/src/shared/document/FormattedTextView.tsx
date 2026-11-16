import { FORMATTED_ALIGN, type FormattedBlock, type FormattedText } from "./formattedText";
import { cx } from "@/shared/ui";
import { RunsView } from "./DocumentViewer";

export function FormattedTextView({ document, text, className }: {
  document?: FormattedText | null;
  text: string;
  className?: string;
}) {
  if (!document?.blocks.length) {
    return <p className={cx("whitespace-pre-wrap break-words text-body", className)}>{text}</p>;
  }
  const groups: { blocks: FormattedBlock[]; start: number }[] = [];
  document.blocks.forEach((block, index) => {
    const previous = groups.at(-1);
    const last = previous?.blocks[0];
    if (last && block.type !== "PARAGRAPH" && last.type === block.type
      && last.indent === block.indent && last.align === block.align) previous.blocks.push(block);
    else groups.push({ blocks: [block], start: index });
  });
  return <div className={cx("space-y-1 whitespace-pre-wrap break-words text-body", className)}>
    {groups.map(({ blocks, start }) => {
      const first = blocks[0]!;
      const style = { textAlign: FORMATTED_ALIGN[first.align], marginInlineStart: `${first.indent * 1.25}rem` };
      if (first.type === "PARAGRAPH") return <p key={start} style={style} className="min-h-[1.5em]"><RunsView runs={first.runs} /></p>;
      const Tag = first.type === "OL" ? "ol" : "ul";
      return <Tag key={start} style={style} className={cx("ps-8", first.type === "OL" ? "list-decimal" : "list-disc")}>
        {blocks.map((block, index) => <li key={start + index} className="min-h-[1.5em]"><RunsView runs={block.runs} /></li>)}
      </Tag>;
    })}
  </div>;
}
