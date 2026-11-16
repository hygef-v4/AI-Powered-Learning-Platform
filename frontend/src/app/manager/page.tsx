import type { Metadata } from "next";
import {ManagerRoute} from "@/features/academics/pages/ClassRoutes";
import { translateOnServer } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  return { title: await translateOnServer("app.manager.page.overview") };
}
export default function Page(){return <ManagerRoute view="overview"/>;}
