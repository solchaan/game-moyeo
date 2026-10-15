import { useId, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from './api'
import { getMemberId } from './auth'
import { t } from './locale'
import type { PositionPreferences } from './types'

export const positions = { TOP: '탑', JUNGLE: '정글', MID: '미드', ADC: '원딜', SUPPORT: '서포터', ANY: '상관없음' }
export function PositionFields({value, onChange, disabled = false}:{value:PositionPreferences;onChange:(value:PositionPreferences)=>void;disabled?:boolean}) {
  const id=useId()
  return <fieldset disabled={disabled} className="position-fields"><legend>{t('선호 포지션')}</legend>
    <label htmlFor={`${id}-primary`}>{t('주 포지션')}</label><select id={`${id}-primary`} value={value.primaryPosition ?? ''} onChange={e=>{
      const primaryPosition = (e.target.value || null) as PositionPreferences['primaryPosition']
      onChange({primaryPosition, secondaryPosition:!primaryPosition || primaryPosition===value.secondaryPosition ? null : value.secondaryPosition})
    }}><option value="">{t('미설정')}</option>{Object.entries(positions).map(([code,label])=><option key={code} value={code}>{t(label)}</option>)}</select>
    <label htmlFor={`${id}-secondary`}>{t('부 포지션')}</label><select id={`${id}-secondary`} disabled={!value.primaryPosition} value={value.secondaryPosition ?? ''} onChange={e=>onChange({...value,secondaryPosition:(e.target.value || null) as PositionPreferences['secondaryPosition']})}>
      <option value="">{t('미설정')}</option>{Object.entries(positions).filter(([code])=>code!==value.primaryPosition).map(([code,label])=><option key={code} value={code}>{t(label)}</option>)}
    </select>
  </fieldset>
}
function ProfileEditor({gameId, initial}:{gameId:number;initial:PositionPreferences}) {
  const [value,setValue]=useState(initial)
  const qc=useQueryClient()
  const save=useMutation({mutationFn:()=>api.saveGameProfile(gameId,value),onSuccess:()=>{void qc.invalidateQueries({queryKey:['game-profile']});void qc.invalidateQueries({queryKey:['participation-preferences']})}})
  return <form onSubmit={e=>{e.preventDefault();save.mutate()}}><PositionFields value={value} onChange={next=>{setValue(next);save.reset()}} disabled={save.isPending}/>
    <button className="primary" disabled={save.isPending}>{t(save.isPending?'저장 중...':'포지션 저장')}</button>
    {save.error&&<p role="alert">{save.error.message}</p>}{save.isSuccess&&<p role="status">{t('기본 포지션을 저장했습니다.')}</p>}
  </form>
}
function GameProfile({gameId}:{gameId:number}) {
  const profile=useQuery({queryKey:['game-profile',gameId,getMemberId()],queryFn:()=>api.gameProfile(gameId),gcTime:0})
  if(profile.isPending)return <p role="status">{t('프로필을 불러오는 중...')}</p>
  if(profile.error)return <div role="alert">{profile.error.message}<button onClick={()=>void profile.refetch()}>{t('다시 시도')}</button></div>
  return <ProfileEditor gameId={gameId} initial={profile.data}/>
}
export function LeagueProfile() {
  const games=useQuery({queryKey:['games'],queryFn:api.games})
  const league=games.data?.find(game=>game.slug==='league-of-legends')
  return <article className="riot-account-card" id="league-profile"><p className="lobby-kicker">01 / LEAGUE OF LEGENDS</p><h2>{t('롤 프로필')}</h2><p>{t('소환사의 협곡 참여 신청에 기본값으로 사용됩니다.')}</p>
    {games.isPending?<p role="status">{t('프로필을 불러오는 중...')}</p>:games.error?<div role="alert">{games.error.message}<button onClick={()=>void games.refetch()}>{t('다시 시도')}</button></div>:league?<GameProfile gameId={league.id}/>:<p>{t('롤 프로필을 아직 설정할 수 없습니다.')}</p>}
  </article>
}
export function ParticipationForm({meetupId,pending,onSubmit}:{meetupId:number;pending:boolean;onSubmit:(value:PositionPreferences)=>void}) {
  const defaults=useQuery({queryKey:['participation-preferences',meetupId,getMemberId()],queryFn:()=>api.participationPreferences(meetupId),gcTime:0})
  const [draft,setDraft]=useState<PositionPreferences|null>(null)
  if(defaults.isPending)return <p role="status">{t('프로필을 불러오는 중...')}</p>
  if(defaults.error)return <div role="alert">{defaults.error.message}<button onClick={()=>void defaults.refetch()}>{t('다시 시도')}</button></div>
  const value=draft??defaults.data.positions
  return <form onSubmit={e=>{e.preventDefault();onSubmit(defaults.data.enabled?value:{primaryPosition:null,secondaryPosition:null})}}>
    {defaults.data.enabled&&<><PositionFields value={value} onChange={setDraft} disabled={pending}/><p className="field-help">{t('이번 신청의 포지션을 바꿔도 프로필은 유지됩니다.')}</p></>}
    <button className="primary wide" disabled={pending}>{t(pending?'참여 신청 중...':'참여 신청하기')}</button>
  </form>
}

export function OwnerPositionPreview({gameId}:{gameId:number}) {
  const profile=useQuery({queryKey:['game-profile',gameId,getMemberId()],queryFn:()=>api.gameProfile(gameId),gcTime:0})
  return <div className="owner-position-preview">
    <p>{t('방장도 정원과 포지션별 인원에 포함됩니다.')}</p>
    {profile.isPending?<p role="status">{t('프로필을 불러오는 중...')}</p>:profile.error?<div role="alert"><p>{profile.error.message}</p><button type="button" className="secondary" onClick={()=>void profile.refetch()}>{t('다시 시도')}</button></div>:<p>
      {t('방장 주 포지션')}: <strong>{profile.data.primaryPosition?t(positions[profile.data.primaryPosition]):t('미설정')}</strong>
      {profile.data.secondaryPosition&&<> · {t('부 포지션')}: {t(positions[profile.data.secondaryPosition])}</>}
    </p>}
    <small>{t('모임을 만들 때 프로필의 포지션이 자동으로 저장됩니다.')}</small>
  </div>
}
