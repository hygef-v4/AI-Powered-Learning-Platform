import type { Metadata } from "next";
import {ManagerRoute} from "@/features/academics/pages/ClassRoutes";
import { translateOnServer } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  return { title: await translateOnServer("app.manager.classes.page.lopCuaMon") };
}
export default async function Page({searchParams}:{searchParams:Promise<{subjectId?:string}>}){const {subjectId}=await searchParams;return <ManagerRoute view="classes" initialSubjectId={subjectId}/>;}
