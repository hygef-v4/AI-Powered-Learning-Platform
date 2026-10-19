"use client";
import {useEffect,useState} from "react";
import type {PersonView} from "./types";
export type CandidateLoader=(query:string,signal:AbortSignal)=>Promise<PersonView[]>;
export function useCandidates(initial:PersonView[],query:string,load?:CandidateLoader) {
 const [state,setState]=useState<{rows:PersonView[];error:string;loading:boolean;query?:string}>({rows:[],error:"",loading:false});
 useEffect(()=>{if(!load)return; const abort=new AbortController(); const timer=setTimeout(()=>{
  // Ô tìm trống vẫn tải 20 người đầu để mở popup là có danh sách chọn ngay.
  setState({rows:[],error:"",loading:true,query});
  load(query.trim(),abort.signal).then(rows=>{if(!abort.signal.aborted)setState({rows:rows.slice(0,20),error:"",loading:false,query});},()=>{if(!abort.signal.aborted)setState({rows:[],error:"Không tải được danh sách. Vui lòng thử lại.",loading:false,query});});
 },300);return()=>{clearTimeout(timer);abort.abort();};},[query,load]);
 return load ? state.query===query?state:{rows:[],error:"",loading:true} : {rows:initial,error:"",loading:false};
}
