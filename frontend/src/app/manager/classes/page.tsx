import type { Metadata } from "next";
import {ManagerRoute} from "@/features/academics/pages/ClassRoutes";
import { serverMsg } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  return { title: await serverMsg("app.manager.classes.page.lopCuaMon") };
}
export default async function Page({searchParams}:{searchParams:Promise<{subjectId?:string}>}){const {subjectId}=await searchParams;return <ManagerRoute view="classes" initialSubjectId={subjectId}/>;}
