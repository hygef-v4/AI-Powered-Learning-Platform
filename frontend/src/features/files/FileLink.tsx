"use client";

import { useState, type ReactNode } from "react";

type Props = {
  /** Gọi API của unit sở hữu (đã kiểm quyền) để lấy link tải 5 phút. */
  fetchUrl: () => Promise<{ url: string }>;
  children: ReactNode;
};

/** Chỉ lấy URL tải khi bấm rồi mở; không lưu URL. */
export function FileLink({ fetchUrl, children }: Props) {
  const [error, setError] = useState<string | null>(null);
  return (
    <span>
      <button
        type="button"
        className="text-blue-700 hover:underline"
        onClick={async () => {
          setError(null);
          try {
            const { url } = await fetchUrl();
            window.open(url, "_blank", "noopener");
          } catch {
            setError("Không mở được tệp");
          }
        }}
      >
        {children}
      </button>
      {error && <span className="ml-2 text-sm text-red-700">{error}</span>}
    </span>
  );
}
