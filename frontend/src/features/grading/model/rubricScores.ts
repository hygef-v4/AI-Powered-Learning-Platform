import type { Checks, ItemScores, Rubric } from "./types";

export function resolveItemPoints(rubricId: string, itemId: string, max: number, checks: Checks, scores: ItemScores = {}) {
  return scores[rubricId]?.[itemId] ?? ((checks[rubricId] ?? []).includes(itemId) ? max : 0);
}
export function isValidItemScore(value: string | number, maximum: number) {
  return /^\d+(?:\.\d{1,2})?$/.test(String(value)) && Number(value) >= 0 && Number(value) <= maximum;
}
export function hasInvalidItemScores(rubrics: Rubric[], scores: ItemScores = {}) {
  return rubrics.some((rubric) => rubric.criteria.some((criterion) => criterion.items.some((item) => {
    const value = scores[rubric.rubricId]?.[item.itemId];
    return value !== undefined && !isValidItemScore(value, Number(item.points));
  })));
}
export function hasPartialPoints(rubrics: Rubric[], checks: Checks, scores: ItemScores = {}) {
  return rubrics.some((rubric) => rubric.criteria.some((criterion) => criterion.items.some((item) => {
    const value = Number(resolveItemPoints(rubric.rubricId, item.itemId, Number(item.points), checks, scores));
    return value > 0 && value < Number(item.points);
  })));
}
/**
 * Explicit scores take precedence over legacy checkboxes, including partial points.
 */
export function buildRubricValues(rubrics: Rubric[], checks: Checks, scores: ItemScores = {}): { checks: Checks; itemScores: ItemScores } {
  const nextChecks: Checks = {};
  const nextScores: ItemScores = {};
  for (const rubric of rubrics) {
    const ticked = new Set(checks[rubric.rubricId] ?? []);
    const achieved: string[] = [];
    const perItem: ItemScores[string] = {};
    for (const item of rubric.criteria.flatMap((criterion) => criterion.items)) {
      const max = Number(item.points);
      const saved = scores[rubric.rubricId]?.[item.itemId];
      const ok = saved !== undefined ? Number(saved) >= max && max > 0 : ticked.has(item.itemId);
      if (ok) achieved.push(item.itemId);
      perItem[item.itemId] = saved ?? (ok ? max : 0);
    }
    nextChecks[rubric.rubricId] = achieved;
    nextScores[rubric.rubricId] = perItem;
  }
  return { checks: nextChecks, itemScores: nextScores };
}

/** Same decimal-cent sum as the backend; item overrides take precedence over legacy checks. */
export function calculateChecklistPoints(rubrics: Rubric[], checks: Checks, scores: ItemScores = {}): number {
  return rubrics.reduce((total, rubric) => total + rubric.criteria.reduce((sum, criterion) =>
    sum + criterion.items.reduce((points, item) => {
      const value = Number(resolveItemPoints(rubric.rubricId, item.itemId, Number(item.points), checks, scores));
      return points + (Number.isFinite(value) ? Math.round(value * 100) : 0);
    }, 0), 0), 0) / 100;
}
