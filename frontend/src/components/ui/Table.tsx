import type { ReactNode } from "react";

export type Column<T> = {
  key: string;
  header: string;
  render: (row: T) => ReactNode;
  className?: string;
};

type TableProps<T> = {
  columns: Column<T>[];
  rows: T[];
  rowKey: (row: T) => string;
  loading?: boolean;
  emptyText?: string;
};

export function Table<T>({ columns, rows, rowKey, loading = false, emptyText = "Không có dữ liệu." }: TableProps<T>) {
  return (
    <div className="overflow-x-auto rounded-md border border-slate-200 bg-white">
      <table className="min-w-full text-sm" aria-busy={loading || undefined}>
        <thead className="bg-slate-100 text-left text-slate-600">
          <tr>
            {columns.map((col) => (
              <th key={col.key} scope="col" className={`px-3 py-2 font-medium ${col.className ?? ""}`}>
                {col.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.length === 0 ? (
            <tr>
              <td colSpan={columns.length} className="px-3 py-6 text-center text-slate-500">
                {loading ? "Đang tải…" : emptyText}
              </td>
            </tr>
          ) : (
            rows.map((row) => (
              <tr key={rowKey(row)} className="border-t border-slate-100 hover:bg-slate-50">
                {columns.map((col) => (
                  <td key={col.key} className={`px-3 py-2 ${col.className ?? ""}`}>
                    {col.render(row)}
                  </td>
                ))}
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );
}
