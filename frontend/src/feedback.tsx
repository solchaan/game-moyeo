import { t, useLocale, getLocale } from './locale'
import { Children, cloneElement, createContext, isValidElement, useContext, useEffect, useId, useRef, useState, useSyncExternalStore } from 'react'
import type { FormHTMLAttributes, LabelHTMLAttributes, ReactElement, HTMLAttributes } from 'react'
import type { Problem } from './types'

const fieldNames: Record<string, string> = {gameId:'게임',title:'모임 제목',description:'설명',startsAt:'시작 시간',endsAt:'종료 시간',recruitmentDeadline:'모집 마감 시간',capacity:'정원',modeOptionId:'게임 모드',platformOptionId:'플랫폼',regionOptionId:'지역',minimumTierOptionId:'최소 티어',maximumTierOptionId:'최대 티어',playStyle:'플레이 스타일',voiceChatPolicy:'음성 채팅',approvalType:'참여 방식',roleRequirements:'역할별 인원',username:'아이디',password:'비밀번호',nickname:'닉네임',email:'이메일',name:'게임 이름',slug:'URL 식별자',imageUrl:'대표 이미지',displayName:'표시 이름',code:'관리 코드',sortOrder:'순서',type:'조건 종류'}
export function fieldLabel(field: string) {
  const option = /^options\[(\d+)\]\.(.+)$/.exec(field)
  return option ? `${Number(option[1])+1}번째 조건 · ${fieldNames[option[2]] ?? t("입력값")}` : t(fieldNames[field] ?? "입력 항목")
}
export function fieldReason(reason: string) {
  if (/[가-힣]/.test(reason)) return t(reason)
  if (/must (not be (blank|null|empty))/.test(reason)) return t("필수 항목을 입력해 주세요.")
  if (/at least one letter and one number/.test(reason)) return t("영문과 숫자를 모두 포함해 주세요.")
  if (/only letters, numbers, or underscores/.test(reason)) return t("영문, 숫자, 밑줄만 입력해 주세요.")
  if (/future/.test(reason)) return t("현재보다 뒤의 시간을 선택해 주세요.")
  if (/well-formed email/.test(reason)) return t("올바른 이메일 주소를 입력해 주세요.")
  if (/greater than 0/.test(reason)) return t("0보다 큰 값을 입력해 주세요.")
  const size = /size must be between (\d+) and (\d+)/.exec(reason)
  if (size) return getLocale() === 'en' ? `Use between ${size[1]} and ${size[2]} characters.` : `${size[1]}~${size[2]} 범위의 길이로 입력해 주세요.`
  const max = /less than or equal to (\d+)/.exec(reason)
  if (max) return getLocale() === 'en' ? `Enter ${max[1]} or less.` : `${max[1]} 이하로 입력해 주세요.`
  const min = /greater than or equal to (\d+)/.exec(reason)
  if (min) return getLocale() === 'en' ? `Enter ${min[1]} or more.` : `${min[1]} 이상으로 입력해 주세요.`
  return t("입력 형식과 허용 범위를 확인해 주세요.")
}
const statusMessages: Record<number,string> = {0:'서버에 연결하지 못했습니다. 인터넷 연결을 확인하고 다시 시도해 주세요.',400:'입력한 내용을 확인해 주세요.',401:'로그인이 필요하거나 만료되었습니다. 다시 로그인해 주세요.',403:'이 작업을 수행할 권한이 없습니다.',404:'요청한 정보를 찾을 수 없습니다.',409:'현재 상태에서는 처리할 수 없습니다. 최신 내용을 확인해 주세요.',412:'다른 곳에서 정보가 변경됐습니다. 새로고침 후 다시 시도해 주세요.',423:'로그인 시도가 여러 번 실패했습니다. 잠시 후 다시 시도해 주세요.',429:'요청이 많습니다. 잠시 후 다시 시도해 주세요.',500:'서버 오류로 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.',503:'서버가 일시적으로 응답하지 않습니다. 잠시 후 다시 시도해 주세요.'}
const codeMessages: Record<string,string> = {INVALID_MEMBER_CREDENTIALS:'아이디 또는 비밀번호가 올바르지 않습니다.',USERNAME_ALREADY_EXISTS:'이미 사용 중인 아이디입니다.',NICKNAME_ALREADY_EXISTS:'이미 사용 중인 닉네임입니다.',MEMBER_ACCOUNT_LOCKED:'로그인 시도가 여러 번 실패해 계정이 잠겼습니다. 15분 후 다시 시도해 주세요.',MEMBER_ACCOUNT_DISABLED:'이용이 제한된 계정입니다.',INVALID_CREDENTIALS:'아이디 또는 비밀번호가 올바르지 않습니다.',RESERVATION_CAPACITY_EXCEEDED:'방금 마지막 자리가 신청됐어요. 모집 상태를 다시 확인해 주세요.',MEETUP_PARTICIPATION_CLOSED:'모집이 마감되어 더 이상 참여할 수 없어요.',INVALID_REQUEST_BODY:'입력 형식이 올바르지 않습니다. 날짜와 숫자 항목을 확인해 주세요.'}
export function problemMessage(status: number, problem: Problem) {
  if (status >= 500) return t(statusMessages[status] ?? statusMessages[500])
  if (problem.fieldErrors?.length) return problem.fieldErrors.map(item=>`${fieldLabel(item.field)}: ${fieldReason(item.reason)}`).join('\n')
  return t(codeMessages[problem.code ?? ''] ?? (problem.detail && /[가-힣]/.test(problem.detail) ? problem.detail : statusMessages[status] ?? '요청을 처리하지 못했습니다. 다시 시도해 주세요.'))
}
export function errorFields(error: unknown): Record<string,string> {
  const problem = (error as {problem?:Problem} | null)?.problem
  const fields = Object.fromEntries((problem?.fieldErrors ?? []).map(item=>[item.field,fieldReason(item.reason)]))
  if (problem?.code==='USERNAME_ALREADY_EXISTS') fields.username=t(codeMessages.USERNAME_ALREADY_EXISTS)
  if (problem?.code==='NICKNAME_ALREADY_EXISTS') fields.nickname=t(codeMessages.NICKNAME_ALREADY_EXISTS)
  return fields
}

type Toast = {id:number;message:string;traceId?:string}
let toasts: Toast[] = []
let sequence = 0
const listeners = new Set<()=>void>()
const recent = new Map<string,number>()
const emit = () => listeners.forEach(listener=>listener())
export function notifyError(error: unknown) {
  const message = error instanceof Error ? error.message : typeof error === 'string' ? error : t("요청을 처리하지 못했습니다. 다시 시도해 주세요.")
  const now = Date.now()
  for (const [key,time] of recent) if (now-time > 8_000) recent.delete(key)
  if (recent.has(message)) return
  recent.set(message,now)
  toasts = [...toasts.slice(-2),{id:++sequence,message,traceId:(error as {problem?:Problem}|null)?.problem?.traceId}]
  emit()
}
function dismiss(id:number) {toasts=toasts.filter(toast=>toast.id!==id);emit()}
function ToastItem({toast}:{toast:Toast}) {
  const [paused,setPaused]=useState(false)
  useEffect(()=>{if(paused)return;const timer=setTimeout(()=>dismiss(toast.id),10_000);return()=>clearTimeout(timer)},[toast.id,paused])
  return <div className="error-toast" onMouseEnter={()=>setPaused(true)} onMouseLeave={()=>setPaused(false)} onFocus={()=>setPaused(true)} onBlur={event=>{if(!event.currentTarget.contains(event.relatedTarget))setPaused(false)}}><span className="error-toast-icon" aria-hidden="true">!</span><div><b>{t("요청을 완료하지 못했어요")}</b><p>{t(toast.message)}</p>{toast.traceId&&<small>{t("문의 번호:")}{toast.traceId}</small>}</div><button type="button" aria-label={t("오류 알림 닫기")} onClick={()=>dismiss(toast.id)}>×</button></div>
}
const subscribe=(listener:()=>void)=>{listeners.add(listener);return()=>{listeners.delete(listener)}}
export function ErrorToasts() {
  useLocale()
  const items=useSyncExternalStore(subscribe,()=>toasts)
  return <aside className="error-toasts" aria-label={t("오류 알림")} aria-live="polite" aria-relevant="additions" aria-atomic="false">{items.map(toast=><ToastItem key={toast.id} toast={toast}/>)}</aside>
}

const FieldsContext = createContext<Record<string,string>>({})
export function FeedbackForm({error,localErrors={},children,onSubmit,...props}:FormHTMLAttributes<HTMLFormElement>&{error?:unknown;localErrors?:Record<string,string>}) {
  useLocale()
  const ref=useRef<HTMLFormElement>(null)
  const retryAt=(error as {retryAt?:number}|null)?.retryAt
  const [now,setNow]=useState(Date.now)
  const cooldown=retryAt?Math.max(0,Math.ceil((retryAt-now)/1000)):0
  useEffect(()=>{if(!retryAt)return;setNow(Date.now());const timer=setInterval(()=>setNow(Date.now()),1000);return()=>clearInterval(timer)},[retryAt])
  const [cleared,setCleared]=useState<Set<string>>(new Set())
  const [nativeErrors,setNativeErrors]=useState<Record<string,string>>({})
  const fields=Object.fromEntries(Object.entries({...errorFields(error),...localErrors,...nativeErrors}).filter(([,reason])=>!!reason))
  const previousLocalErrors=useRef<Record<string,string>>({})
  const localSignature=JSON.stringify(localErrors)
  useEffect(()=>{
    const hasNewError=Object.entries(localErrors).some(([field,reason])=>reason&&!previousLocalErrors.current[field])
    previousLocalErrors.current=localErrors
    if(hasNewError)requestAnimationFrame(()=>ref.current?.querySelector<HTMLElement>('[aria-invalid="true"]')?.focus())
  },[localSignature])
  for(const key of cleared)delete fields[key]
  useEffect(()=>{setCleared(new Set());if(error)requestAnimationFrame(()=>ref.current?.querySelector<HTMLElement>('[aria-invalid="true"]')?.focus())},[error])
  return <FieldsContext.Provider value={fields}><form {...props} ref={ref} noValidate onChange={event=>{const target=event.target;if(target instanceof HTMLInputElement||target instanceof HTMLSelectElement||target instanceof HTMLTextAreaElement)setCleared(previous=>new Set(previous).add(target.name))}} onSubmit={event=>{
    if(retryAt&&retryAt>Date.now()){event.preventDefault();return}
    setCleared(new Set())
    const invalid:Record<string,string>={}
    for(const control of Array.from(event.currentTarget.elements)) {
      if((control instanceof HTMLInputElement||control instanceof HTMLSelectElement||control instanceof HTMLTextAreaElement)&&control.name&&!control.validity.valid)invalid[control.name]=control.validity.valueMissing?t("필수 항목을 입력해 주세요."):control.validity.typeMismatch?t("올바른 형식으로 입력해 주세요."):control.validity.rangeUnderflow?(getLocale() === 'en' ? `Enter ${control.getAttribute('min')} or more.` : `${control.getAttribute('min')} 이상으로 입력해 주세요.`):control.validity.rangeOverflow?(getLocale() === 'en' ? `Enter ${control.getAttribute('max')} or less.` : `${control.getAttribute('max')} 이하로 입력해 주세요.`):control.validity.tooShort?(getLocale() === 'en' ? `Enter ${control.getAttribute('minlength')} characters or more.` : `${control.getAttribute('minlength')}자 이상 입력해 주세요.`):t("입력 형식과 허용 범위를 확인해 주세요.")
    }
    setNativeErrors(invalid)
    if(Object.keys(invalid).length){event.preventDefault();notifyError(t("입력한 항목을 확인해 주세요."));requestAnimationFrame(()=>ref.current?.querySelector<HTMLElement>('[aria-invalid="true"]')?.focus());return}
    onSubmit?.(event)
  }}><fieldset className="feedback-controls" disabled={cooldown>0}>{children}</fieldset>{cooldown>0&&<p role="status" className="field-help">{t("요청이 많습니다.")}{cooldown}{t("초 후 다시 시도할 수 있어요.")}</p>}{Object.keys(fields).length>0&&<div className="field-error-summary" role="alert"><b>{t("다음 항목을 확인해 주세요.")}</b><ul>{Object.entries(fields).map(([field,reason])=><li key={field}>{fieldLabel(field)}: {reason}</li>)}</ul></div>}</form></FieldsContext.Provider>
}
export function Field({name,children,...props}:LabelHTMLAttributes<HTMLLabelElement>&{name:string}) {
  const errors=useContext(FieldsContext)
  const id=useId()
  const error=errors[name]
  return <label {...props}>{Children.map(children,child=>{
    if(!isValidElement(child)||!['input','select','textarea'].includes(String(child.type)))return child
    const control=child as ReactElement<HTMLAttributes<HTMLElement>&{name?:string}>
    return cloneElement(control,{name,'aria-invalid':!!error,'aria-describedby':[control.props['aria-describedby'],error?id:undefined].filter(Boolean).join(' ')||undefined})
  })}{error&&<small id={id} className="field-error">{error}</small>}</label>
}
