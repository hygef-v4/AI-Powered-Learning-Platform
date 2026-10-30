import { afterEach, expect, it, vi } from "vitest";
import { academicsApi } from "./index";
afterEach(() => vi.unstubAllGlobals());
it("bounds authenticated list requests", async () => {
 const fn = vi.fn().mockImplementation(()=>Promise.resolve(Response.json([]))); vi.stubGlobal("fetch",fn);
 await academicsApi.classes("manager",{page:0,size:500,q:"a & b"});
 expect(fn.mock.calls[0]![0]).toBe("/api/v1/manager/classes?page=1&size=100&q=a+%26+b");
 expect(fn.mock.calls[0]![1].credentials).toBe("include");
});
it("preserves mutation conflict payload",async()=>{
 vi.stubGlobal("fetch",vi.fn().mockResolvedValue(Response.json({detail:"Changed",conflictingStudents:["a"]},{status:409})));
 await expect(academicsApi.status("a/b","OPEN",4)).rejects.toMatchObject({problem:{status:409,conflictingStudents:["a"]}});
 expect(fetch).toHaveBeenCalledWith("/api/v1/manager/classes/a%2Fb/status",expect.objectContaining({method:"PUT",body:JSON.stringify({status:"OPEN",version:4})}));
});
it("preserves raw batch lines and cancellation",async()=>{
 const fn=vi.fn().mockImplementation(()=>Promise.resolve(Response.json([]))); vi.stubGlobal("fetch",fn);
 await academicsApi.batch("c","a@test.test\n\nb@test.test");
 expect(fn.mock.calls[0]![1].body).toBe(JSON.stringify({text:"a@test.test\n\nb@test.test"}));
 const signal=new AbortController().signal; await academicsApi.candidates("teacher","an",signal);
 expect(fn.mock.calls[1]![0]).toBe("/api/v1/manager/teacher-candidates?q=an&limit=20");
 expect(fn.mock.calls[1]![1].signal).toBe(signal);
});
it("rejects unsuccessful enrollment outcomes returned with HTTP 200",async()=>{
 vi.stubGlobal("fetch",vi.fn().mockImplementation(()=>Promise.resolve(Response.json({result:"IN_OTHER_CLASS"}))));
 await expect(academicsApi.enroll("c","s")).rejects.toThrow("Đang ở lớp khác cùng môn");
});
