import {render,screen,fireEvent,waitFor} from "@testing-library/react";
import {afterEach,it,expect,vi} from "vitest";
import {ClassesRoute,ClassDetailRoute} from "./routes";
vi.mock("next/navigation",()=>({useRouter:()=>({push:vi.fn()}),useSearchParams:()=>new URLSearchParams(window.location.search)}));
vi.mock("@/shared/auth/useCurrentUser",()=>({useCurrentUser:()=>({displayName:"User"})}));
afterEach(()=>vi.unstubAllGlobals());
it("routes an empty authorized teacher list through server capability",async()=>{
 const fn=vi.fn().mockImplementation((url:string)=>Promise.resolve(url.includes("/me/")?Response.json({}, {status:403}):Response.json([])));vi.stubGlobal("fetch",fn);
 render(<ClassesRoute/>);expect(await screen.findByRole("heading",{name:"Lớp được phân công"})).toBeInTheDocument();expect(fn).toHaveBeenCalledTimes(2);
});
it("reloads a conflicting manager mutation and retains its actionable error",async()=>{
 const course={id:"c",subject:{id:"s",code:"S",name:"Subject"},code:"C",name:"Class",description:"",term:"1",status:"DRAFT",teacher:{id:"t",name:"Teacher",email:"t@test.test"},version:1,activeStudentCount:0,removedStudentCount:0,showGradeDistribution:false,createdAt:"2026-01-01"};
 const fn=vi.fn().mockImplementation((url:string,init:RequestInit)=>Promise.resolve(init.method==="PUT"?Response.json({detail:"Version changed"},{status:409}):url.includes("/students?")?Response.json([]):Response.json(course)));vi.stubGlobal("fetch",fn);
 render(<ClassDetailRoute scope="manager" id="c"/>);fireEvent.click(await screen.findByTestId("class-state-open"));fireEvent.click(screen.getByTestId("class-state-confirm"));
 expect((await screen.findAllByRole("alert")).length).toBeGreaterThan(0);await waitFor(()=>expect(fn.mock.calls.filter(call=>call[0]==="/api/v1/manager/classes/c").length).toBe(2));
});
it("keeps version conflict visible after reloading a newer info editor",async()=>{
 const course={id:"c",subject:{id:"s",code:"S",name:"Subject"},code:"C",name:"Original",description:"",term:"1",status:"DRAFT",teacher:null,version:1,activeStudentCount:0,removedStudentCount:0,showGradeDistribution:false,createdAt:"2026-01-01"};
 let version=1;
 vi.stubGlobal("fetch",vi.fn().mockImplementation((url:string,init:RequestInit)=>{
  if(init.method==="PATCH"){version=2;return Promise.resolve(Response.json({detail:"Changed"},{status:409}));}
  return Promise.resolve(Response.json(url.includes("/students?")?[]:{...course,version,name:version===1?"Original":"Updated on server"}));
 }));
 render(<ClassDetailRoute scope="manager" id="c"/>);
 fireEvent.change(await screen.findByLabelText(/Tên lớp/),{target:{value:"Local edit"}});
 fireEvent.click(screen.getByTestId("class-info-save"));
 await waitFor(()=>expect(screen.getByLabelText(/Tên lớp/)).toHaveValue("Updated on server"));
 expect(screen.getByTestId("class-mutation-conflict")).toHaveTextContent("Dữ liệu đã thay đổi");
 fireEvent.change(screen.getByLabelText(/Tên lớp/),{target:{value:"Second edit"}});
 fireEvent.click(screen.getByTestId("class-info-save"));
 await waitFor(()=>expect(fetch).toHaveBeenCalledWith("/api/v1/manager/classes/c",expect.objectContaining({method:"PATCH",body:JSON.stringify({name:"Second edit",description:"",term:"1",version:2})})));
});
