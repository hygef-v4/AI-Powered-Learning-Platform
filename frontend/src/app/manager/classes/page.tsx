import type { Metadata } from "next";
import {ManagerRoute} from "@/features/academics/pages/ClassRoutes";

export const metadata: Metadata = { title: "Lớp của môn" };
export default async function Page({searchParams}:{searchParams:Promise<{subjectId?:string}>}){const {subjectId}=await searchParams;return <ManagerRoute view="classes" initialSubjectId={subjectId}/>;}
