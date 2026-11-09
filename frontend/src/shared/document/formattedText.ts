import type { Align, Run } from "./types";

export type FormattedBlock = {
  type: "PARAGRAPH" | "UL" | "OL";
  align: Align;
  indent: number;
  runs: Pick<Run, "text" | "bold" | "italic" | "underline">[];
};
export type FormattedText = { blocks: FormattedBlock[] };

export function formattedTextOf(text: string): FormattedText {
  return { blocks: [{ type: "PARAGRAPH", align: "LEFT", indent: 0, runs: [{ text }] }] };
}

export function formattedTextPlain(document: FormattedText): string {
  return document.blocks.map((block) => block.runs.map((run) => run.text).join("")).join("\n");
}

export const FORMATTED_ALIGN = {
  LEFT: "left", CENTER: "center", RIGHT: "right", JUSTIFY: "justify",
} as const;
