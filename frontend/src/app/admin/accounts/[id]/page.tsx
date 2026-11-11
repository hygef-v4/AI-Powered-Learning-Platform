import type { Metadata } from "next";
import { AccountDetailPage } from "@/features/identity/admin/pages/AccountDetailPage";

export const metadata: Metadata = { title: "Tài khoản" };

export default function Page() {
  return <AccountDetailPage />;
}
