import { ApiError } from "@/shared/api/client";
import type { SubjectManager } from "./types";

/** Boundary U04; adapter HTTP/schema do U01 bàn giao sau, không phải endpoint đã thống nhất. */
export type ManagerCandidate = SubjectManager & { role: "SUBJECT_MANAGER"; status: "ACTIVE" };
export type SearchSubjectManagers = (query: { q: string; limit: 20 }) => Promise<ManagerCandidate[]>;

export const searchSubjectManagers: SearchSubjectManagers = async () => {
  throw new ApiError({ status: 503, code: "SUBJECT_MANAGER_SEARCH_UNAVAILABLE" });
};
