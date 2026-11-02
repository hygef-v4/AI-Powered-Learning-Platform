import {ClassDetailRoute} from "@/features/academics/routes";
export default async function Page({params}:{params:Promise<{id:string}>}){const {id}=await params;return <ClassDetailRoute id={id} scope="teaching"/>;}
