"use client";

import { useEffect, useRef, useState } from "react";
import { getPolicies, uploadFile } from "../api";
import { checkFile, type ArtifactPurpose, type PurposePolicy, type UploadResult } from "../model/purposes";

export type UploadState =
  | { status: "idle" }
  | { status: "uploading"; percent: number }
  | { status: "done"; result: UploadResult }
  | { status: "error"; message: string };

export function useFileUpload(purpose: ArtifactPurpose) {
  const [policy, setPolicy] = useState<PurposePolicy | null>(null);
  const [state, setState] = useState<UploadState>({ status: "idle" });
  const abortRef = useRef<(() => void) | null>(null);

  useEffect(() => {
    let active = true;
    getPolicies()
      .then((policies) => {
        if (active) setPolicy(policies.find((p) => p.purpose === purpose) ?? null);
      })
      .catch(() => {
        if (active) setState({ status: "error", message: "Không tải được giới hạn tệp" });
      });
    return () => {
      active = false;
      abortRef.current?.();
    };
  }, [purpose]);

  async function start(file: File): Promise<UploadResult | null> {
    if (!policy) return null;
    const problem = checkFile(file, policy);
    if (problem) {
      setState({ status: "error", message: problem });
      return null;
    }
    setState({ status: "uploading", percent: 0 });
    const handle = uploadFile(file, purpose, (percent) => setState({ status: "uploading", percent }));
    abortRef.current = handle.abort;
    try {
      const result = await handle.promise;
      setState({ status: "done", result });
      return result;
    } catch (e) {
      setState({ status: "error", message: e instanceof Error ? e.message : "Tải tệp lên không thành công" });
      return null;
    } finally {
      abortRef.current = null;
    }
  }

  return { policy, state, start, cancel: () => abortRef.current?.() };
}
