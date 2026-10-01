import { t, useLocale } from './locale'
import { useEffect, useRef, useState } from 'react'
import { useMutation, useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { api } from './api'

export default function RiotAccountPage() {
  useLocale()
  const status = useQuery({ queryKey: ['riot-account'], queryFn: api.riotStatus, staleTime: 0, gcTime: 0 })
  const popup = useRef<Window | null>(null)
  const [waiting, setWaiting] = useState(false)
  const [message, setMessage] = useState('')
  const [confirmUnlink, setConfirmUnlink] = useState(false)
  const complete = useMutation({
    mutationFn: api.riotComplete,
    onSuccess: () => { setMessage(t("Riot 계정이 연결되었습니다.")); void status.refetch() },
    onError: (error: Error) => setMessage(error.message),
  })
  const unlink = useMutation({
    mutationFn: api.riotUnlink,
    onSuccess: () => { setConfirmUnlink(false); setMessage(t("Riot 계정 연결을 해제했습니다.")); void status.refetch() },
    onError: (error: Error) => setMessage(error.message),
  })
  useEffect(() => {
    function receive(event: MessageEvent) {
      if (event.origin !== window.location.origin || !popup.current || event.source !== popup.current
        || event.data?.type !== 'gamemoyeo:riot-result') return
      popup.current.close()
      popup.current = null
      setWaiting(false)
      if (typeof event.data.code === 'string' && /^[A-Za-z0-9_-]{43}$/.test(event.data.code)) {
        complete.mutate(event.data.code)
      } else {
        setMessage(t("Riot 인증이 취소되었거나 만료되었습니다. 다시 연결해 주세요."))
      }
    }
    window.addEventListener('message', receive)
    const timer = window.setInterval(() => {
      if (popup.current?.closed) {
        popup.current = null
        setWaiting(false)
        setMessage(t("인증 창이 닫혔습니다. 연결 상태를 확인하거나 다시 시도해 주세요."))
      }
    }, 500)
    return () => { window.removeEventListener('message', receive); clearInterval(timer); popup.current?.close() }
  }, [])
  async function start() {
    setMessage('')
    const child = window.open('about:blank', '_blank', 'popup,width=520,height=740')
    if (!child) { setMessage(t("팝업이 차단되었습니다. 이 사이트의 팝업을 허용한 후 다시 시도해 주세요.")); return }
    popup.current = child
    setWaiting(true)
    try {
      const result = await api.riotStart()
      const url = new URL(result.authorizationUrl)
      if (url.origin !== 'https://auth.riotgames.com' || url.pathname !== '/authorize') throw new Error(t("인증 주소가 올바르지 않습니다."))
      if (!child.closed) child.location.replace(url.href)
    } catch (error) {
      child.close(); popup.current = null; setWaiting(false)
      setMessage(error instanceof Error ? error.message : t("연결을 시작하지 못했습니다."))
    }
  }
  const busy = waiting || complete.isPending || unlink.isPending
  return <section className="riot-account-page">
    <Link to="/">{t("← 모임 찾기")}</Link>
    <h1>{t("게임 계정 연결")}</h1>
    <p>{t("게임모여 계정에 본인의 Riot 계정을 연결합니다. 리그 오브 레전드와 VALORANT에서 사용하는 계정입니다.")}</p>
    <article className="riot-account-card">
      <h2>Riot Games</h2>
      {status.isPending ? <p role="status">{t("연결 상태를 확인하고 있어요.")}</p> : status.error ?
        <div role="alert"><p>{status.error.message}</p><button onClick={() => void status.refetch()}>{t("다시 시도")}</button></div> : <>
          <p>{status.data?.linked ? <>{t("연결된 Riot ID:")}<strong>{status.data.riotId}</strong></> : t("아직 연결된 계정이 없습니다.")}</p>
          {!status.data?.enabled && <p>{t("Riot 계정 연동을 준비 중입니다. 준비가 완료되면 연결할 수 있어요.")}</p>}
          {!status.data?.linked && <button className="primary" disabled={!status.data?.enabled || busy} onClick={() => void start()}>
            {busy ? t("인증을 진행하고 있어요…") : t("Riot 계정 연결")}
          </button>}
          {status.data?.linked && !confirmUnlink && <button disabled={busy} onClick={() => setConfirmUnlink(true)}>{t("연결 해제")}</button>}
          {confirmUnlink && <div><p>{t("Riot 계정 연결을 해제할까요? 게임모여 계정은 유지됩니다.")}</p>
            <button disabled={busy} onClick={() => unlink.mutate()}>{t("해제하기")}</button>
            <button disabled={busy} onClick={() => setConfirmUnlink(false)}>{t("취소")}</button></div>}
        </>}
      {message && <p role="status" aria-live="polite">{t(message)}</p>}
    </article>
    <p>{t("인증은 Riot 공식 창에서 진행됩니다. 연결을 완료하면 계정 식별자와 Riot ID를 저장하며, 계정 비밀번호는 받지 않습니다.")}</p>
  </section>
}

export function RiotCallbackPage() {
  const [orphan, setOrphan] = useState(false)
  useEffect(() => {
    const params = new URLSearchParams(window.location.search)
    const code = params.get('code')
    window.history.replaceState(null, '', '/riot/callback')
    if (!window.opener) { setOrphan(true); return }
    window.opener.postMessage({ type: 'gamemoyeo:riot-result', code }, window.location.origin)
    window.close()
  }, [])
  return <section className="riot-account-page"><h1>{t("Riot 계정 인증")}</h1><p role="status">
    {orphan ? t("인증을 시작한 창을 찾지 못했습니다. 게임모여에서 다시 로그인하고 계정 연결을 시작해 주세요.") : t("인증 결과를 전달했습니다. 이 창을 닫고 게임모여로 돌아가 주세요.")}
  </p><Link to="/account">{t("게임모여로 돌아가기")}</Link></section>
}
