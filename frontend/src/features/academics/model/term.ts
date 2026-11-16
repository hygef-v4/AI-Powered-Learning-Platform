/**
 * Học kỳ của lớp (D-U04-05, BR-U05-61, 62, 65): cùng quy tắc với `ClassTermPolicy` ở backend.
 * Chỉ dùng để xem trước trong form và sắp/lọc danh sách đã tải; giá trị đã lưu luôn lấy `term` từ response.
 */
const TERM_PATTERN = /^(SP|SU|FA)(\d{2})(-BL5)?$/;
const SEASON_RANK: Record<string, number> = { SP: 0, SU: 1, FA: 2 };
const DATE_PATTERN = /^(\d{4})-(\d{2})-(\d{2})$/;

/** Returns the term code for an ISO `yyyy-MM-dd` start date, or null when the date is missing or outside 2000–2099. */
export function deriveTerm(startsOn: string | null | undefined): string | null {
  const match = DATE_PATTERN.exec(startsOn ?? "");
  if (!match) return null;
  const year = Number(match[1]);
  const month = Number(match[2]);
  if (year < 2000 || year > 2099 || month < 1 || month > 12) return null;
  const season = month <= 4 ? "SP" : month <= 8 ? "SU" : "FA";
  const block = month % 4 === 0 ? "-BL5" : "";
  return `${season}${String(year % 100).padStart(2, "0")}${block}`;
}

/** Checks whether a value is a well-formed term code such as `SP27` or `SP27-BL5`. */
export function isTerm(value: string | null | undefined): value is string {
  return TERM_PATTERN.test(value ?? "");
}

function rankTerm(term: string) {
  const match = TERM_PATTERN.exec(term);
  if (!match) return -1;
  return Number(match[2]) * 10 + SEASON_RANK[match[1]!]! * 2 + (match[3] ? 1 : 0);
}

/** Sorts terms newest first: year, then FA > SU > SP, then the `-BL5` block before the base term. */
export function compareTermsNewestFirst(a: string, b: string) {
  return rankTerm(b) - rankTerm(a) || a.localeCompare(b);
}

/** Returns the distinct terms of loaded classes, newest first. */
export function listTermOptions(classes: readonly { term?: string | null }[]) {
  const terms = new Set<string>();
  for (const item of classes) if (isTerm(item.term)) terms.add(item.term);
  return [...terms].sort(compareTermsNewestFirst);
}
