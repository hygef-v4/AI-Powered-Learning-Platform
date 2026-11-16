import type { Metadata } from "next";
import { AccountDetailPage } from "@/features/identity/admin/pages/AccountDetailPage";
import { translateOnServer } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  return { title: await translateOnServer("app.admin.accounts.id.page.taiKhoan") };
}

export default function Page() {
  return <AccountDetailPage />;
}
