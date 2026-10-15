import { useEffect } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from './api'
import { t } from './locale'
import { positions } from './Positions'
import PositionIcon from './PositionIcon'

const statuses = { OPEN:'모집 중', FILLED:'모집 인원 충족', CLOSED:'모집 마감', EMPTY:'비어 있음', OCCUPIED:'참여 중' }

export default function MeetupPositions({meetupId, revision}:{meetupId:number;revision:number}) {
  const qc=useQueryClient()
  const query=useQuery({queryKey:['position-availability',meetupId],queryFn:()=>api.positionAvailability(meetupId)})
  useEffect(()=>{void qc.invalidateQueries({queryKey:['position-availability',meetupId]})},[qc,meetupId,revision])
  if(query.isPending)return <p role="status">{t('포지션 현황을 불러오는 중...')}</p>
  if(query.error)return <div className="position-load-error" role="alert"><p>{t('포지션 현황을 불러오지 못했습니다.')}</p><button className="secondary" onClick={()=>void query.refetch()}>{t('다시 시도')}</button></div>
  const data=query.data
  if(!data.enabled)return null
  const owner=data.ownerPositions
  return <section className="meetup-positions" aria-labelledby="position-heading">
    <h2 className="meetup-section-title" id="position-heading"><span aria-hidden="true">02 /</span>{t('포지션 현황')}</h2>
    <p className="field-help">{t('주 포지션 기준이며, 부 포지션은 중복 집계하지 않습니다.')}</p>
    <p className="position-owner"><span>{t('방장')}</span> {owner.primaryPosition?t(positions[owner.primaryPosition]):t('미설정')}
      {owner.secondaryPosition&&<> · {t('부 포지션')} {t(positions[owner.secondaryPosition])}</>}
    </p>
    <ul className="position-roster">{data.positions.map(slot=><li key={slot.position} className={`position-slot is-${slot.status.toLowerCase()} ${slot.participantCount>0?'has-participants':'is-vacant'}`}>
      <div className="position-icon"><PositionIcon position={slot.position}/><span className="position-icon-mark" aria-hidden="true">{slot.participantCount>0?'✓':'−'}</span></div>
      <div className="position-slot-heading"><h3>{t(positions[slot.position])}</h3></div>
      <span className="position-occupancy">{t(slot.participantCount>0?'참여 중':'비어 있음')}</span>
      <p className="position-slot-count"><strong>{slot.participantCount}</strong>{slot.capacity!==null?` / ${slot.capacity}`:''}{t('명')}</p>
      {slot.status!=='EMPTY'&&slot.status!=='OCCUPIED'&&<span className="position-slot-status">{t(statuses[slot.status])}</span>}
      {slot.remainingCapacity!==null&&<small>{t('남은 모집 인원')} {slot.remainingCapacity}{t('명')}</small>}
    </li>)}</ul>
    <div className="position-roster-notes">
      <p>{t('전체 남은 자리')} {data.remainingCapacity}{t('명')}</p>
      {(data.unassignedCount>0||data.anyPositionCount>0)&&<p>{t('미설정')} {data.unassignedCount}{t('명')} · {t('상관없음')} {data.anyPositionCount}{t('명')}</p>}
      <p>{t('모집 인원이 없는 포지션은 현재 참여 현황만 표시합니다.')}</p>
    </div>
  </section>
}
