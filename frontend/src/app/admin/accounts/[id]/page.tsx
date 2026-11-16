import type { Metadata } from "next";
import { AccountDetailPage } from "@/features/identity/admin/pages/AccountDetailPage";
import { serverMsg } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  return { title: await serverMsg("app.admin.accounts.id.page.taiKhoan") };
}

export default function Page() {
  return <AccountDetailPage />;
}
