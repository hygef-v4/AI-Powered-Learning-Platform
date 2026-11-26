import {render,screen,fireEvent} from "@testing-library/react";
import {it,expect,afterEach} from "vitest";
import {ReadOnlyClassPage} from "@/features/academics/pages/ReadOnlyClassPage";
import {loadClassCapability} from "@/features/academics/api";
import {vi} from "vitest";
import {teacherClassTabs,classCardSections} from "@/features/academics/slots";
import {ClassDashboardPage} from "@/features/academics/pages/ClassDashboardPage";
import {ManagerDashboardPage,MySubjectPage} from "@/features/academics/pages/ManagerDashboardPage";
import type {CourseClassView} from "@/features/academics/model/types";
vi.mock("next/navigation", () => ({ useSearchParams: () => new URLSearchParams(window.location.search) }));
afterEach(() => window.history.replaceState(null, "", "/"));
const value:CourseClassView={id:"c",subject:{id:"s",code:"S",name:"Subject"},code:"C",name:"Class",description:"",startsOn:"2027-01-10",term:"SP27",status:"OPEN",teacher:null,version:1,activeStudentCount:0,removedStudentCount:0,showGradeDistribution:false,createdAt:"2026-01-01"};
it("hosts student card extension and lists managed subjects without stat extensions",()=>{
 classCardSections.register({key:"test-card",order:1,component:()=> <p>Card extension</p>});
 render(<><ClassDashboardPage audience="student" classes={[value]} onOpen={()=>{}}/><ManagerDashboardPage subjects={[value.subject]} onSubject={()=>{}}/></>);
 expect(screen.getByText("Card extension")).toBeInTheDocument();
 expect(screen.getByTestId("subject-row-s")).toHaveTextContent("Subject");
});
it("shows subject info like the teacher class info and an overview of the subject's open classes",async()=>{
 const fn=vi.fn().mockImplementation(()=>Promise.resolve(Response.json([value,{...value,id:"c2",code:"C2",activeStudentCount:4,teacher:{id:"t",name:"Teacher One",email:"t@example.test"}}])));
 vi.stubGlobal("fetch",fn);
 render(<MySubjectPage subject={{...value.subject,description:"Mô tả môn",status:"ACTIVE",classCounts:{OPEN:2,ARCHIVED:1}}}/>);
 // Thẻ chỉ xem như Thông tin lớp: số lớp lấy từ classCounts, có trạng thái và mô tả.
 expect(screen.getByRole("heading",{name:"Thông tin môn",level:2})).toBeInTheDocument();
 expect(screen.getByText("Số lớp").nextSibling).toHaveTextContent("2 đang mở · 1 lưu trữ");
 expect(screen.getByText("Trạng thái").nextSibling).toHaveTextContent("Hoạt động");
 expect(screen.getByText("Mô tả môn")).toBeInTheDocument();
 expect(screen.queryByRole("textbox")).not.toBeInTheDocument();
 // Tổng quan lớp chỉ đọc lớp đang mở của môn này.
 expect(await screen.findAllByTestId("subject-open-class")).toHaveLength(2);
 const url=String(fn.mock.calls[0]?.[0]);
 expect(url).toContain("manager/classes?");
 expect(url).toContain("subjectId=s");
 expect(url).toContain("status=OPEN");
 expect(screen.getByLabelText("4 Sinh viên")).toBeInTheDocument();
 expect(screen.getByLabelText("1 Lớp chưa có giảng viên")).toBeInTheDocument();
 expect(screen.getByText("Teacher One")).toBeInTheDocument();
 vi.unstubAllGlobals();
});
it("hosts every teacher tab including info and gradebook",()=>{
 teacherClassTabs.register({key:"test-teacher",order:1,component:({tab})=><p>Extension {tab}</p>});
 render(<ReadOnlyClassPage audience="teacher" value={value}/>);
 expect(screen.getByText("Extension info")).toBeInTheDocument(); fireEvent.click(screen.getByRole("tab",{name:"Chấm điểm"})); expect(screen.getByText("Extension gradebook")).toBeInTheDocument();
});
it("picks the class list by role and never falls back to another audience",async()=>{
 const fn=vi.fn().mockImplementation(()=>Promise.resolve(Response.json([])));vi.stubGlobal("fetch",fn);
 await expect(loadClassCapability("STUDENT")).resolves.toEqual({audience:"student",classes:[]});expect(String(fn.mock.calls[0]?.[0])).toContain("/me/classes");
 fn.mockClear();
 await expect(loadClassCapability("TEACHER")).resolves.toEqual({audience:"teacher",classes:[]});expect(fn).toHaveBeenCalledTimes(1);expect(String(fn.mock.calls[0]?.[0])).toContain("/teaching/classes");
 fn.mockReset().mockImplementation(()=>Promise.resolve(Response.json({}, {status:503})));
 await expect(loadClassCapability("STUDENT")).rejects.toMatchObject({problem:{status:503}});expect(fn).toHaveBeenCalledTimes(1);vi.unstubAllGlobals();
});

it("restores dashboard filters without sharing their query parameter", () => {
  window.history.replaceState(null, "", "/?classStatus=ARCHIVED&subjectStatus=ARCHIVED");
  render(<><ClassDashboardPage audience="teacher" classes={[{...value,status:"ARCHIVED"}]} onOpen={()=>{}}/><ManagerDashboardPage subjects={[{...value.subject,status:"ARCHIVED"}]} onSubject={()=>{}}/></>);
  expect(screen.getByRole("tab", {name:/Kết thúc/})).toHaveAttribute("aria-selected", "true");
  expect(screen.getByRole("tab", {name:/Lưu trữ/})).toHaveAttribute("aria-selected", "true");
  fireEvent.click(screen.getByRole("tab", {name:/Đang dạy/}));
  const params = new URLSearchParams(window.location.search);
  expect(params.get("classStatus")).toBe("OPEN");
  expect(params.get("subjectStatus")).toBe("ARCHIVED");
});
