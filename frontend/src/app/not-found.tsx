import type { Metadata } from "next";
import { ErrorPage } from "@/shared/ui/ErrorPage";
import { serverMsg } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  return { title: await serverMsg("app.notfound.khongTimThayTrang") };
}

export default function NotFound() {
  return <ErrorPage kind="not-found" />;
}
