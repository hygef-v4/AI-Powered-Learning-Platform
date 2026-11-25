import { afterEach, expect, it, vi } from "vitest";
import { academicsApi } from "@/features/academics/api";
afterEach(() => vi.unstubAllGlobals());
it("preserves raw batch lines and cancellation",async()=>{
 const fn=vi.fn().mockImplementation(()=>Promise.resolve(Response.json([]))); vi.stubGlobal("fetch",fn);
 await academicsApi.enrollStudents("c","a@test.test\n\nb@test.test");
 expect(fn.mock.calls[0]![1].body).toBe(JSON.stringify({text:"a@test.test\n\nb@test.test"}));
 const signal=new AbortController().signal; await academicsApi.listCandidates("teacher","an",signal);
 expect(fn.mock.calls[1]![0]).toBe("/api/v1/manager/teacher-candidates?q=an&limit=20");
 expect(fn.mock.calls[1]![1].signal).toBe(signal);
});
it("forwards the class term filter only when it is set", async () => {
 const fn = vi.fn().mockImplementation(()=>Promise.resolve(Response.json([]))); vi.stubGlobal("fetch",fn);
 await academicsApi.listClasses("teaching",{term:"SP27-BL5"});
 await academicsApi.listClasses("teaching",{term:""});
 expect(fn.mock.calls[0]![0]).toBe("/api/v1/teaching/classes?page=1&size=100&term=SP27-BL5");
 expect(fn.mock.calls[1]![0]).toBe("/api/v1/teaching/classes?page=1&size=100");
});
