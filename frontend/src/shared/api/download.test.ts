import { describe,it,expect,vi,afterEach } from "vitest";
import { apiDownload } from "./client";

afterEach(()=>vi.unstubAllGlobals());
describe("apiDownload",()=>{
 it("reads binary and retains cookies and MIME accept",async()=>{
   const fetcher=vi.fn().mockResolvedValue(new Response("csv-content",{status:200}));vi.stubGlobal("fetch",fetcher);
   const result=await apiDownload("/api/v1/classes/c/gradebook/export",{headers:{Accept:"text/csv"}});
   expect(await result.text()).toBe("csv-content");expect(fetcher.mock.calls[0]![1].credentials).toBe("include");expect(fetcher.mock.calls[0]![1].headers.get("Accept")).toBe("text/csv");
 });
 it("rejects forbidden downloads instead of saving an error page",async()=>{
   vi.stubGlobal("fetch",vi.fn().mockResolvedValue(new Response(JSON.stringify({code:"GRADE_NOT_FOUND"}),{status:404})));await expect(apiDownload("/api/v1/file")).rejects.toMatchObject({problem:{status:404}});
 });
 it("refreshes an expired session before retrying the binary response",async()=>{
   const fetcher=vi.fn().mockResolvedValueOnce(new Response(null,{status:401})).mockResolvedValueOnce(new Response("{}",{status:200})).mockResolvedValueOnce(new Response("binary",{status:200}));
   vi.stubGlobal("fetch",fetcher);expect(await (await apiDownload("/api/v1/file",{headers:{Accept:"text/csv"}})).text()).toBe("binary");
   expect(fetcher.mock.calls[1]![0]).toBe("/api/v1/auth/refresh");expect(fetcher.mock.calls[2]![1].headers.get("Accept")).toBe("text/csv");
 });
});
