import {act,renderHook} from "@testing-library/react";
import {afterEach,it,expect,vi} from "vitest";
import {useCandidates} from "@/features/academics/hooks/useCandidates";
afterEach(()=>vi.useRealTimers());
it("debounces 300ms, caps candidates, and aborts stale searches",async()=>{
 vi.useFakeTimers();const load=vi.fn().mockResolvedValue(Array.from({length:30},(_,id)=>({id:String(id),name:"Student",email:"s@test.test"})));
 const {result,rerender,unmount}=renderHook(({query})=>useCandidates([],query,load),{initialProps:{query:"an"}});
 await act(async()=>{await vi.advanceTimersByTimeAsync(299);});expect(load).not.toHaveBeenCalled();
 await act(async()=>{await vi.advanceTimersByTimeAsync(1);});expect(result.current.rows).toHaveLength(20);
 const oldSignal=load.mock.calls[0]![1] as AbortSignal;rerender({query:"binh"});expect(oldSignal.aborted).toBe(true);expect(result.current.rows).toEqual([]);
 await act(async()=>{await vi.advanceTimersByTimeAsync(300);});expect(load).toHaveBeenCalledTimes(2);unmount();
});
it("loads the first candidates with an empty query so the picker is filled on open",async()=>{
 vi.useFakeTimers();const load=vi.fn().mockResolvedValue([{id:"t1",name:"Teacher",email:"t@test.test"}]);
 const {result,unmount}=renderHook(()=>useCandidates([],"",load));
 expect(result.current.loading).toBe(true);
 await act(async()=>{await vi.advanceTimersByTimeAsync(300);});
 expect(load).toHaveBeenCalledWith("",expect.any(AbortSignal));expect(result.current.rows).toHaveLength(1);unmount();
});
