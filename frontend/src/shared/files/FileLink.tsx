"use client";

import { useState } from "react";
import { ApiError } from "@/lib/api/client";

type FileLinkProps = {
  fileName: string;
  /** Gọi API của unit sở hữu để lấy URL tải (token 5 phút) ngay khi bấm; không lưu URL lâu. */
  getDownloadUrl: () => Promise<{ url: string }>;
  testId?: string;
};

export function FileLink({ fileName, getDownloadUrl, testId = "file-link" }: FileLinkProps) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | undefined>();

  const open = async () => {
    setLoading(true);
    setError(undefined);
    try {
      const { url } = await getDownloadUrl();
      window.open(url, "_blank", "noopener");
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Không mở được tệp, vui lòng thử lại.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <span className="inline-flex flex-col">
      <button
        type="button"
        onClick={open}
        disabled={loading}
        data-testid={testId}
        className="text-left text-brand underline disabled:opacity-50"
      >
        {fileName}
      </button>
      {error && (
        <span role="alert" className="text-xs text-red-600">
          {error}
        </span>
      )}
    </span>
  );
}
