"use client";

import { useState, type ReactNode } from "react";
import { Icon } from "@/shared/ui/Icon";

type Props = {
  /** Gọi API của unit sở hữu (đã kiểm quyền) để lấy link tải 5 phút. */
  fetchUrl: () => Promise<{ url: string }>;
  children: ReactNode;
};

/** Chỉ lấy URL tải khi bấm rồi mở; không lưu URL. */
export function FileLink({ fetchUrl, children }: Props) {
  const [error, setError] = useState<string | null>(null);
  return (
    <span className="inline-flex items-center gap-2">
      <button
        type="button"
        className="inline-flex items-center gap-1 text-[13px] font-semibold text-primary hover:underline"
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
        <Icon name="description" size={16} />
        {children}
      </button>
      {error && <span className="text-xs text-danger">{error}</span>}
    </span>
  );
}
