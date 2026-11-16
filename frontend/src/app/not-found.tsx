import type { Metadata } from "next";
import { ErrorPage } from "@/shared/ui/ErrorPage";
import { translateOnServer } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  return { title: await translateOnServer("app.notfound.khongTimThayTrang") };
}

export default function NotFound() {
  return <ErrorPage kind="not-found" />;
}
