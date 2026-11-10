import type { Metadata } from "next";
import { ManagerRoute } from "@/features/academics/pages/ClassRoutes";

export const metadata: Metadata = { title: "Thông tin môn" };
export default function Page() {
  return <ManagerRoute view="subjects" />;
}
