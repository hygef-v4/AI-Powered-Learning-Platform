import { apiFetch, ApiError } from "@/shared/api/client";
import {resultLabels} from "../model/types";
import type { EnrollmentResult, ClassInfoInput, ClassStatus, CourseClassView, CreateClassInput, EnrollmentRowResult, EnrollmentView, PersonView, SubjectView } from "../model/types";
export type Scope = "me" | "teaching" | "manager";
export type ListQuery = {page?:number;size?:number;q?:string;status?:string;subjectId?:string};
export type ManagedSubject = SubjectView & {status?:string;classCounts?:Partial<Record<ClassStatus,number>>};
const base="/api/v1/";
const enc=encodeURIComponent;
function query(input:ListQuery={}) { const p=new URLSearchParams({page:String(Math.max(1,Math.floor(input.page||1))),size:String(Math.max(1,Math.min(100,Math.floor(input.size||100))))}); for(const key of ["q","status","subjectId"] as const) if(input[key]) p.set(key,input[key]!); return p.toString(); }
function write<T>(path:string,method:string,body:unknown) { return apiFetch<T>(base+path,{method,body:JSON.stringify(body)}); }
export const academicsApi={
 classes:(scope:Scope,input:ListQuery={},signal?:AbortSignal)=>apiFetch<CourseClassView[]>(`${base}${scope}/classes?${query(input)}`,{signal}),
 detail:(scope:Scope,id:string,signal?:AbortSignal)=>apiFetch<CourseClassView>(`${base}${scope}/classes/${enc(id)}`,{signal}),
 students:(scope:"teaching"|"manager",id:string,input:ListQuery={},signal?:AbortSignal)=>apiFetch<EnrollmentView[]>(`${base}${scope}/classes/${enc(id)}/students?${query(input)}`,{signal}),
 student:(id:string,accountId:string,signal?:AbortSignal)=>apiFetch<EnrollmentView>(`${base}teaching/classes/${enc(id)}/students/${enc(accountId)}`,{signal}),
 subjects:(signal?:AbortSignal)=>apiFetch<ManagedSubject[]>(base+"manager/subjects",{signal}),
 subject:(id:string,signal?:AbortSignal)=>apiFetch<ManagedSubject>(base+"manager/subjects/"+enc(id),{signal}),
 studentCandidates:(id:string,q:string,signal?:AbortSignal)=>apiFetch<PersonView[]>(`${base}manager/classes/${enc(id)}/student-candidates?${new URLSearchParams({q,limit:"20"})}`,{signal}),
 candidates:(kind:"teacher"|"student",q:string,signal?:AbortSignal)=>apiFetch<PersonView[]>(`${base}manager/${kind}-candidates?${new URLSearchParams({q,limit:"20"})}`,{signal}),
 create:(input:CreateClassInput)=>write<CourseClassView>("manager/classes","POST",input),
 info:(id:string,input:ClassInfoInput)=>write<CourseClassView>(`manager/classes/${enc(id)}`,"PATCH",input),
 teacher:(id:string,teacherId:string|null,version:number)=>write<CourseClassView>(`manager/classes/${enc(id)}/teacher`,"PUT",{teacherId,version}),
 status:(id:string,status:ClassStatus,version:number)=>write<CourseClassView>(`manager/classes/${enc(id)}/status`,"PUT",{status,version}),
 distribution:(id:string,enabled:boolean,version:number)=>write<CourseClassView>(`manager/classes/${enc(id)}/grade-distribution`,"PUT",{enabled,version}),
 enroll:async(id:string,accountId:string)=>{const response=await write<{result:EnrollmentResult}>(`manager/classes/${enc(id)}/enrollments`,"POST",{accountId});if(!["ENROLLED","RESTORED","ALREADY_ENROLLED"].includes(response.result))throw new Error(resultLabels[response.result]??"Không thể ghi danh sinh viên.");return response;},
 enrollmentStatus:(id:string,accountId:string,status:"ACTIVE"|"REMOVED")=>write<unknown>(`manager/classes/${enc(id)}/enrollments/${enc(accountId)}/status`,"PUT",{status}),
 batch:(id:string,text:string)=>write<EnrollmentRowResult[]>(`manager/classes/${enc(id)}/enrollments:batch`,"POST",{text}),
};
/** Resolve display mode only through successful, session-authorized endpoints. */
export async function classCapability(signal?:AbortSignal) {
 try { return {audience:"student" as const,classes:await academicsApi.classes("me",{},signal)}; }
 catch(error) { if(!(error instanceof ApiError)||![403,404].includes(error.problem.status)) throw error; }
 return {audience:"teacher" as const,classes:await academicsApi.classes("teaching",{},signal)};
}
