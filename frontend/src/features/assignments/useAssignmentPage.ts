"use client";

import { useCallback, useEffect, useState } from "react";
import type { ListFilter } from "./api";
import type { AssignmentPage } from "./types";

type Loader = (filter: ListFilter, signal: AbortSignal) => Promise<AssignmentPage>;
type Loaded = { load: Loader | null; key: string; data: AssignmentPage | null; error: unknown };

/** Tải danh sách bài theo bộ lọc; tải lại khi bộ lọc hoặc nguồn đổi, hoặc khi gọi {@code reload}. */
export function useAssignmentPage(load: Loader, filter: ListFilter) {
  const [loaded, setLoaded] = useState<Loaded>({ load: null, key: "", data: null, error: null });
  const [tick, setTick] = useState(0);
  const key = `${JSON.stringify(filter)}#${tick}`;
  useEffect(() => {
    const controller = new AbortController();
    const filterValue = JSON.parse(key.slice(0, key.lastIndexOf("#"))) as ListFilter;
    load(filterValue, controller.signal).then(
      (data) => { if (!controller.signal.aborted) setLoaded({ load, key, data, error: null }); },
      (error) => { if (!controller.signal.aborted) setLoaded({ load, key, data: null, error }); },
    );
    return () => controller.abort();
  }, [load, key]);
  const reload = useCallback(() => setTick((value) => value + 1), []);
  const current = loaded.load === load && loaded.key === key;
  return {
    data: loaded.load === load ? loaded.data : null,
    error: current ? loaded.error : null,
    loading: !current,
    reload,
  };
}
