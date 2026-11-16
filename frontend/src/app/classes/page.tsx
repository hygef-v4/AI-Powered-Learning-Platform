import type { Metadata } from "next";
import {ClassesRoute} from "@/features/academics/pages/ClassRoutes";
import { serverMsg } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  return { title: await serverMsg("app.classes.page.lopHoc") };
}
export default function Page(){return <ClassesRoute/>;}
