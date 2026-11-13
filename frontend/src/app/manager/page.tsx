import type { Metadata } from "next";
import {ManagerRoute} from "@/features/academics/pages/ClassRoutes";

export const metadata: Metadata = { title: "Tổng quan" };
export default function Page(){return <ManagerRoute view="overview"/>;}
