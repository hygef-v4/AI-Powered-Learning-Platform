import type { Metadata } from "next";
import {ClassesRoute} from "@/features/academics/pages/ClassRoutes";
import { translateOnServer } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  return { title: await translateOnServer("app.teaching.classes.page.assignedClasses") };
}
export default function Page(){return <ClassesRoute teaching/>;}
