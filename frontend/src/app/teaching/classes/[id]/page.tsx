import type { Metadata } from "next";
import {ClassDetailRoute} from "@/features/academics/pages/ClassRoutes";
import { serverMsg } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  return { title: await serverMsg("app.teaching.classes.id.page.lopDuocPhanCong") };
}
export default async function Page({params}:{params:Promise<{id:string}>}){const {id}=await params;return <ClassDetailRoute id={id} scope="teaching"/>;}
