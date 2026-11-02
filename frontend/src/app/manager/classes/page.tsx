import {ManagerRoute} from "@/features/academics/pages/routes";
export default async function Page({searchParams}:{searchParams:Promise<{subjectId?:string}>}){const {subjectId}=await searchParams;return <ManagerRoute view="classes" initialSubjectId={subjectId}/>;}
