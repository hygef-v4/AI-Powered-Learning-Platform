import type { Metadata } from "next";
import {ClassesRoute} from "@/features/academics/pages/ClassRoutes";

export const metadata: Metadata = { title: "Lớp học" };
export default function Page(){return <ClassesRoute/>;}
