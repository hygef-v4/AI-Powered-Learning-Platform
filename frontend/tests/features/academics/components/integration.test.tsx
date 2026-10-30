import {render,screen,fireEvent} from "@testing-library/react";
import {it,expect} from "vitest";
import {ReadOnlyClassPage} from "@/features/academics/pages/ReadOnlyClassPage";
import {classCapability} from "@/features/academics/api";
import {vi} from "vitest";
import {teacherClassTabs,classCardSections} from "@/features/academics/slots";
import {ClassDashboardPage} from "@/features/academics/pages/ClassDashboardPage";
import {ManagerDashboardPage,MySubjectPage} from "@/features/academics/pages/ManagerDashboardPage";
import type {CourseClassView} from "@/features/academics/model/types";
const value:CourseClassView={id:"c",subject:{id:"s",code:"S",name:"Subject"},code:"C",name:"Class",description:"",term:"1",status:"OPEN",teacher:null,version:1,activeStudentCount:0,removedStudentCount:0,showGradeDistribution:false,createdAt:"2026-01-01"};
it("hosts student card extension and lists managed subjects without stat extensions",()=>{
 classCardSections.register({key:"test-card",order:1,component:()=> <p>Card extension</p>});
 render(<><ClassDashboardPage audience="student" classes={[value]} onOpen={()=>{}}/><ManagerDashboardPage subjects={[value.subject]} onSubject={()=>{}}/><MySubjectPage subject={value.subject} classes={[value]}/></>);
 expect(screen.getByText("Card extension")).toBeInTheDocument();
 expect(screen.getByTestId("subject-row-s")).toHaveTextContent("Subject");
 // Thông tin môn chỉ còn thẻ thông tin, ô Số lớp là số lớp đang mở.
 expect(screen.getByLabelText("Số lớp")).toHaveValue("1");
});
it("hosts every teacher tab including info and gradebook",()=>{
 teacherClassTabs.register({key:"test-teacher",order:1,component:({tab})=><p>Extension {tab}</p>});
 render(<ReadOnlyClassPage audience="teacher" value={value} onBack={()=>{}}/>);
 expect(screen.getByText("Extension info")).toBeInTheDocument(); fireEvent.click(screen.getByRole("tab",{name:"Chấm điểm"})); expect(screen.getByText("Extension gradebook")).toBeInTheDocument();
});
it("accepts empty authorized classes and never falls back on server failure",async()=>{
 const fn=vi.fn().mockImplementation(()=>Promise.resolve(Response.json([])));vi.stubGlobal("fetch",fn);
 await expect(classCapability()).resolves.toEqual({audience:"student",classes:[]});expect(fn).toHaveBeenCalledTimes(1);
 fn.mockReset().mockImplementation(()=>Promise.resolve(Response.json({}, {status:503})));
 await expect(classCapability()).rejects.toMatchObject({problem:{status:503}});expect(fn).toHaveBeenCalledTimes(1);vi.unstubAllGlobals();
});
