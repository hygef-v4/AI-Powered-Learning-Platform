import type { Metadata } from "next";
import { ErrorPage } from "@/shared/ui/ErrorPage";

export const metadata: Metadata = { title: "Không tìm thấy trang" };

export default function NotFound() {
  return <ErrorPage kind="not-found" />;
}
