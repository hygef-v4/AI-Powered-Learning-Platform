import type { Run } from "./types";

/** Định dạng của một đoạn chữ (run), không gồm chữ. */
export type RunFormat = Omit<Run, "text">;
export type RunFlag = "bold" | "italic" | "underline" | "strike" | "code";

const FLAGS: RunFlag[] = ["bold", "italic", "underline", "strike", "code"];

/** Định dạng của run, chỉ giữ cờ đang bật và màu có giá trị. */
export function formatOf(run: Run | undefined): RunFormat {
  const format: RunFormat = {};
  if (!run) return format;
  for (const flag of FLAGS) if (run[flag]) format[flag] = true;
  if (run.color) format.color = run.color;
  if (run.highlight) format.highlight = run.highlight;
  return format;
}

export function hasFormat(run: Run | undefined): boolean {
  return Object.keys(formatOf(run)).length > 0;
}

export function sameFormat(a: RunFormat, b: RunFormat): boolean {
  return FLAGS.every((flag) => !!a[flag] === !!b[flag]) && (a.color ?? "") === (b.color ?? "") && (a.highlight ?? "") === (b.highlight ?? "");
}

export function sameRuns(a: Run[], b: Run[]): boolean {
  return a.length === b.length && a.every((run, i) => run.text === b[i]!.text && sameFormat(run, b[i]!));
}

/** Gộp các run liền nhau cùng định dạng, bỏ run rỗng; đoạn trống còn một run rỗng giữ định dạng của run đầu. */
export function normalizeRuns(runs: Run[]): Run[] {
  const out: Run[] = [];
  for (const run of runs) {
    if (!run.text) continue;
    const last = out.at(-1);
    if (last && sameFormat(last, run)) out[out.length - 1] = { ...last, text: last.text + run.text };
    else out.push({ ...formatOf(run), text: run.text });
  }
  return out.length > 0 ? out : [{ ...formatOf(runs[0]), text: "" }];
}

export function runsLength(runs: Run[]): number {
  return runs.reduce((sum, run) => sum + run.text.length, 0);
}

/** Cắt run tại vị trí {@code at} (tính theo ký tự của cả đoạn). */
function cut(runs: Run[], at: number): Run[] {
  const out: Run[] = [];
  let pos = 0;
  for (const run of runs) {
    const end = pos + run.text.length;
    if (at > pos && at < end) out.push({ ...run, text: run.text.slice(0, at - pos) }, { ...run, text: run.text.slice(at - pos) });
    else out.push(run);
    pos = end;
  }
  return out;
}

/** Các run nằm trong [start, end), giữ định dạng. */
export function sliceRuns(runs: Run[], start: number, end: number): Run[] {
  const out: Run[] = [];
  let pos = 0;
  for (const run of cut(cut(runs, start), end)) {
    const from = pos;
    pos += run.text.length;
    if (run.text && from >= start && pos <= end) out.push(run);
  }
  return out;
}

/** Áp {@code change} cho chữ trong [start, end) (vùng bôi đen), phần còn lại giữ nguyên định dạng. */
export function mapRange(runs: Run[], start: number, end: number, change: (run: Run) => Run): Run[] {
  let pos = 0;
  return normalizeRuns(cut(cut(runs, start), end).map((run) => {
    const from = pos;
    pos += run.text.length;
    return run.text && from >= start && pos <= end ? change(run) : run;
  }));
}

/** Bật hoặc tắt một định dạng cho vùng bôi đen: cả vùng đang có thì tắt, không thì bật. */
export function toggleFlagRange(runs: Run[], start: number, end: number, flag: RunFlag): Run[] {
  const selected = sliceRuns(runs, start, end);
  const on = !(selected.length > 0 && selected.every((run) => run[flag]));
  return mapRange(runs, start, end, (run) => ({ ...run, [flag]: on || undefined }));
}

/** Màu chữ hoặc màu tô nền cho vùng bôi đen; {@code undefined} là bỏ màu. */
export function colorRange(runs: Run[], start: number, end: number, key: "color" | "highlight", value: string | undefined): Run[] {
  return mapRange(runs, start, end, (run) => ({ ...run, [key]: value }));
}

/** Định dạng tại con trỏ: của chữ ngay trước con trỏ; đầu đoạn thì của chữ đầu tiên. */
export function formatAt(runs: Run[], at: number): RunFormat {
  let pos = 0;
  for (const run of runs) {
    const end = pos + run.text.length;
    if (run.text && (at === 0 ? pos === 0 : at > pos && at <= end)) return formatOf(run);
    pos = end;
  }
  return formatOf(runs.at(-1));
}

/** Tách đoạn tại con trỏ (Enter; bỏ vùng bôi đen [start, end)): hai nửa giữ định dạng, nửa trống theo định dạng chỗ tách. */
export function splitRuns(runs: Run[], start: number, end = start): [Run[], Run[]] {
  const format = formatAt(runs, start);
  const before = sliceRuns(runs, 0, start);
  const after = sliceRuns(runs, end, runsLength(runs));
  return [
    before.length > 0 ? normalizeRuns(before) : [{ ...format, text: "" }],
    after.length > 0 ? normalizeRuns(after) : [{ ...format, text: "" }],
  ];
}

/** Thay [start, end) bằng {@code text} (dán), chữ mới theo định dạng tại con trỏ. */
export function insertText(runs: Run[], start: number, end: number, text: string): Run[] {
  const format = formatAt(runs, start);
  return normalizeRuns([...sliceRuns(runs, 0, start), { ...format, text }, ...sliceRuns(runs, end, runsLength(runs))]);
}

/** Tách đoạn theo dòng (đoạn thành danh sách), mỗi dòng giữ định dạng. */
export function runLines(runs: Run[]): Run[][] {
  const lines: Run[][] = [[]];
  for (const run of runs) {
    run.text.split("\n").forEach((part, i) => {
      if (i > 0) lines.push([]);
      if (part) lines.at(-1)!.push({ ...run, text: part });
    });
  }
  return lines.map((line) => (line.length > 0 ? normalizeRuns(line) : [{ ...formatOf(runs[0]), text: "" }]));
}

/** Ghép các dòng thành một đoạn (danh sách thành đoạn); dấu xuống dòng theo định dạng cuối dòng trước. */
export function joinLines(lines: Run[][]): Run[] {
  const out: Run[] = [];
  lines.forEach((line, i) => {
    if (i > 0) out.push({ ...formatOf(lines[i - 1]!.at(-1)), text: "\n" });
    out.push(...line);
  });
  return normalizeRuns(out);
}
