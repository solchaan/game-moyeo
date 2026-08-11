export type OptionType = 'MODE'|'TIER'|'ROLE'|'PLATFORM'|'REGION'|'MAP'
export interface Game { id:number; slug:string; name:string; description:string|null; imageUrl:string|null; active:boolean }
export interface GamePayload { slug:string; name:string; description:string|null; imageUrl:string|null }
export interface GameOptionPayload { type:OptionType; code:string; displayName:string; sortOrder:number; metadata:string|null }
export interface CreateGamePayload extends GamePayload { options:GameOptionPayload[] }
export interface TokenPair { accessToken:string; refreshToken:string; expiresIn:number; tokenType:string }
export interface GameOption { id:number; gameId:number; type:OptionType; code:string; displayName:string; sortOrder:number; active:boolean; metadata:string|null }
export interface GameDetail { game:Game; options:GameOption[] }
export interface Meetup { id:number; gameId:number; ownerId:number; title:string; description:string|null; modeOptionId:number|null; platformOptionId:number|null; regionOptionId:number|null; minimumTierOptionId:number|null; maximumTierOptionId:number|null; playStyle:string; voiceChatPolicy:string; approvalType:string; recruitmentDeadline:string|null; startsAt:string; endsAt:string; capacity:number; reservedCount:number; status:string; roleRequirements:Record<string,number> }
export interface CursorPage<T> { items:T[]; nextCursor:number|null; hasNext:boolean }
export interface Problem { title?:string; detail?:string; code?:string; fieldErrors?:{field:string;reason:string}[] }
export interface MeetupPayload { gameId:number; modeOptionId:number|null; platformOptionId:number|null; regionOptionId:number|null; minimumTierOptionId:number|null; maximumTierOptionId:number|null; title:string; description:string; playStyle:string; voiceChatPolicy:string; approvalType:string; recruitmentDeadline:string|null; startsAt:string; endsAt:string; capacity:number; roleRequirements:{roleOptionId:number;capacity:number}[] }
