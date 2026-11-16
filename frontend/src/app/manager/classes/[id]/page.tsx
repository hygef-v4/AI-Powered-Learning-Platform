import type { Metadata } from "next";
import {ClassDetailRoute} from "@/features/academics/pages/ClassRoutes";
import { translateOnServer } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  return { title: await translateOnServer("app.manager.classes.id.page.subjectClasses") };
}
export default async function Page({params}:{params:Promise<{id:string}>}){const {id}=await params;return <ClassDetailRoute id={id} scope="manager"/>;}
