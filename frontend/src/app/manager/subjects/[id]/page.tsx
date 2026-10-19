import {ManagerRoute} from "@/features/academics/routes";
export default async function Page({params}:{params:Promise<{id:string}>}){const {id}=await params;return <ManagerRoute view="subjects" id={id}/>;}
