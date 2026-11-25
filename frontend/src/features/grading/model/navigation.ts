export type NavigationFilters = { status?: string; late?: string };

// Dựng link bản chấm và giữ các bộ lọc nguồn được caller truyền vào.
export function buildEvaluationHref(id: string, filters: NavigationFilters = {}) {
  const query = new URLSearchParams();
  if (filters.status) query.set("status", filters.status);
  if (filters.late) query.set("late", filters.late);
  return `/teaching/evaluations/${id}${query.size ? `?${query}` : ""}`;
}
