export type NavigationFilters = { status?: string; late?: string };

export function evaluationHref(id: string, filters: NavigationFilters = {}) {
  const query = new URLSearchParams();
  if (filters.status) query.set("status", filters.status);
  if (filters.late) query.set("late", filters.late);
  return `/teaching/evaluations/${id}${query.size ? `?${query}` : ""}`;
}
