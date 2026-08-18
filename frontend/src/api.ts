import type { CreateGamePayload, CursorPage, Game, GameDetail, GameOption, GameOptionPayload, GamePayload, Meetup, MeetupPayload, Problem, TokenPair } from './types'
import { getAccessToken, setAccessToken } from './auth'

const base = import.meta.env.VITE_API_BASE_URL ?? ''
export class ApiError extends Error { constructor(public status:number, public problem:Problem){super(problem.detail || problem.title || '요청을 처리하지 못했습니다.')} }
async function request<T>(path:string, init:RequestInit = {}):Promise<T> {
  const token = getAccessToken()
  const response = await fetch(`${base}${path}`, { ...init, signal:init.signal??AbortSignal.timeout(10_000), headers:{'Content-Type':'application/json', ...(token?{Authorization:`Bearer ${token}`} : {}), ...init.headers} })
  if (!response.ok) { let problem:Problem={}; try{problem=await response.json()}catch{problem={detail:`HTTP ${response.status}`}}; if(response.status===401&&token)setAccessToken(null,'expired'); throw new ApiError(response.status,problem) }
  if (response.status===204) return undefined as T
  return response.json()
}
export const api = {
  games:()=>request<Game[]>('/api/v1/games'),
  game:(id:number)=>request<GameDetail>(`/api/v1/games/${id}`),
  createGame:(body:CreateGamePayload)=>request<GameDetail>('/api/v1/admin/games',{method:'POST',headers:{'Idempotency-Key':crypto.randomUUID()},body:JSON.stringify(body)}),
  updateGame:(gameId:number,body:GamePayload)=>request<Game>(`/api/v1/admin/games/${gameId}`,{method:'PUT',headers:{'Idempotency-Key':crypto.randomUUID()},body:JSON.stringify(body)}),
  createGameOption:(gameId:number,body:GameOptionPayload)=>request<GameOption>(`/api/v1/admin/games/${gameId}/options`,{method:'POST',headers:{'Idempotency-Key':crypto.randomUUID()},body:JSON.stringify(body)}),
  updateGameOption:(gameId:number,optionId:number,body:GameOptionPayload)=>request<GameOption>(`/api/v1/admin/games/${gameId}/options/${optionId}`,{method:'PUT',headers:{'Idempotency-Key':crypto.randomUUID()},body:JSON.stringify(body)}),
  deleteGameOption:(gameId:number,optionId:number)=>request<void>(`/api/v1/admin/games/${gameId}/options/${optionId}`,{method:'DELETE',headers:{'Idempotency-Key':crypto.randomUUID()}}),
  meetups:(gameId?:number,cursor?:number)=>request<CursorPage<Meetup>>(`/api/v1/meetups?size=20${gameId?`&gameId=${gameId}`:''}${cursor?`&cursor=${cursor}`:''}`),
  meetup:(id:number)=>request<Meetup>(`/api/v1/meetups/${id}`),
  createMeetup:(body:MeetupPayload)=>request<Meetup>('/api/v1/meetups',{method:'POST',headers:{'Idempotency-Key':crypto.randomUUID()},body:JSON.stringify(body)}),
  updateMeetup:(id:number,body:MeetupPayload)=>request<Meetup>(`/api/v1/meetups/${id}`,{method:'PUT',headers:{'Idempotency-Key':crypto.randomUUID()},body:JSON.stringify(body)}),
  deleteMeetup:(id:number)=>request<void>(`/api/v1/meetups/${id}`,{method:'DELETE',headers:{'Idempotency-Key':crypto.randomUUID()}}),
  exchange:(code:string)=>request<TokenPair>('/api/v1/auth/token',{method:'POST',body:JSON.stringify({code})}),
  memberLogin:(username:string,password:string)=>request<TokenPair>('/api/v1/auth/login',{method:'POST',body:JSON.stringify({username,password})}),
  memberRegister:(body:{username:string;password:string;nickname:string;email:string|null})=>request<TokenPair>('/api/v1/auth/register',{method:'POST',body:JSON.stringify(body)}),
  adminLogin:(username:string,password:string)=>request<TokenPair>('/api/v1/auth/admin/login',{method:'POST',body:JSON.stringify({username,password})}),
}
