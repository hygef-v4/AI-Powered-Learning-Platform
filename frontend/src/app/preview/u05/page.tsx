import { notFound } from "next/navigation";
import { U05Preview } from "@/features/academics/preview/U05Preview";

export const dynamic = "force-dynamic";

export default function U05PreviewPage() {
  if (process.env.NODE_ENV !== "development") notFound();
  return <U05Preview/>;
}
