"use client";

import { useEffect, useRef } from "react";
import { msg } from "@/i18n/msg";

export function SourceImagePreview({ file, className = "h-20 max-w-40" }: { file: File; className?: string }) {
  const image = useRef<HTMLImageElement>(null);
  useEffect(() => {
    if (!image.current || !file.type.startsWith("image/") || !URL.createObjectURL) return;
    const url = URL.createObjectURL(file);
    image.current.src = url;
    return () => URL.revokeObjectURL(url);
  }, [file]);
  if (!file.type.startsWith("image/")) return null;
  // Local blobs have no public URL for the Next image optimizer.
  // eslint-disable-next-line @next/next/no-img-element
  return <img ref={image} alt={msg("assignments.components.SourceImagePreview.sourceImage", { name: String(file.name) })} className={`rounded-lg border border-line object-contain ${className}`} />;
}
