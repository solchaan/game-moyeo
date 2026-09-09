import type { CreateGamePayload, CursorPage, Game, GameDetail, GameOption, GameOptionPayload, GamePayload, Meetup, MeetupPayload, Participation, Problem, TokenPair } from './types'
import { problemMessage } from './feedback'
import { getAccessToken, setAccessToken } from './auth'

const base = import.meta.env.VITE_API_BASE_URL ?? ''
export class ApiError extends Error { constructor(public status:number, public problem:Problem, public retryAt?:number){super(problemMessage(status,problem))} }
async function request<T>(path:string, init:RequestInit = {}):Promise<T> {
  const token = getAccessToken()
  let response:Response
  try { response = await fetch(`${base}${path}`, { ...init, signal:init.signal??AbortSignal.timeout(10_000), headers:{'Content-Type':'application/json', ...(token?{Authorization:`Bearer ${token}`} : {}), ...init.headers} }) } catch(error) {
    if(init.signal?.aborted)throw error
    throw new ApiError(0,{detail:error instanceof DOMException&&error.name==='TimeoutError'?'서버 응답이 늦어지고 있습니다. 잠시 후 다시 시도해 주세요.':undefined})
  }
  if (!response.ok) { let problem:Problem={}; try{const body:unknown=await response.json();if(body&&typeof body==='object'&&!Array.isArray(body)){const value=body as Record<string,unknown>;problem={title:typeof value.title==='string'?value.title:undefined,detail:typeof value.detail==='string'?value.detail:undefined,code:typeof value.code==='string'?value.code:undefined,traceId:typeof value.traceId==='string'?value.traceId:undefined,fieldErrors:Array.isArray(value.fieldErrors)?value.fieldErrors.filter((item):item is {field:string;reason:string}=>!!item&&typeof item.field==='string'&&typeof item.reason==='string'):undefined}}}catch{problem={detail:`HTTP ${response.status}`}}; if(response.status===401&&token)setAccessToken(null,'expired'); const retryAfter=response.headers.get('Retry-After');const seconds=retryAfter===null?NaN:Number(retryAfter);const retryAt=retryAfter===null?undefined:Number.isFinite(seconds)?Date.now()+Math.max(0,seconds)*1000:Date.parse(retryAfter);throw new ApiError(response.status,problem,retryAt) }
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
  participation:(id:number)=>request<Participation>(`/api/v1/meetups/${id}/participation`),
  participate:(id:number)=>request<Participation>(`/api/v1/meetups/${id}/reservations`,{method:'POST',headers:{'Idempotency-Key':crypto.randomUUID()}}),
  cancelParticipation:(id:number)=>request<Participation>(`/api/v1/meetups/${id}/reservations/me`,{method:'DELETE',headers:{'Idempotency-Key':crypto.randomUUID()}}),
  closeParticipation:(id:number)=>request<Participation>(`/api/v1/meetups/${id}/close`,{method:'POST',headers:{'Idempotency-Key':crypto.randomUUID()}}),
  exchange:(code:string)=>request<TokenPair>('/api/v1/auth/token',{method:'POST',body:JSON.stringify({code})}),
  memberLogin:(username:string,password:string)=>request<TokenPair>('/api/v1/auth/login',{method:'POST',body:JSON.stringify({username,password})}),
  memberRegister:(body:{username:string;password:string;nickname:string;email:string|null})=>request<TokenPair>('/api/v1/auth/register',{method:'POST',body:JSON.stringify(body)}),
}
