import { t, useLocale, gameName, optionName as localizedOptionName, formatDate } from './locale'
import { useEffect } from 'react'
import { useInfiniteQuery, useQuery } from '@tanstack/react-query'
import { Link, useSearchParams } from 'react-router-dom'
import { rememberServer, selectedServer, type LeagueServer } from './server'
import { api } from './api'
import type { Game, GameDetail, Meetup } from './types'
import './lobby.css'

const playStyles: Record<string, string> = { CASUAL: '가볍게', COMPETITIVE: '승리 지향', PRACTICE: '연습', BEGINNER: '초보 환영', SOCIAL: '친목' }
const statuses: Record<string, string> = { OPEN: '모집 중', CLOSED: '마감', CANCELED: '취소됨', DRAFT: '준비 중' }

function GameIcon({ game }: { game?: Game }) {
  return game?.imageUrl
    ? <img className={`lobby-game-icon lobby-game-icon--${game.slug}`} src={game.imageUrl} width="36" height="36" alt="" />
    : <span className="lobby-game-icon" aria-hidden="true">{game?.name.slice(0, 1) ?? '✳'}</span>
}

function PartyRow({ meetup, game, detail }: { meetup: Meetup; game?: Game; detail?: GameDetail }) {
  const locale = useLocale()
  const optionName = (id: number | null) => { const option = detail?.options.find(option => option.id === id); return option ? localizedOptionName(option) : undefined }
  const mode = optionName(meetup.modeOptionId)
  const minimum = optionName(meetup.minimumTierOptionId)
  const maximum = optionName(meetup.maximumTierOptionId)
  const roles = Object.entries(meetup.roleRequirements).map(([id, capacity]) => {
    const name = optionName(Number(id))
    return name ? `${name} ${capacity}${t('명')}` : null
  }).filter(Boolean)

  return <li>
    <Link className="lobby-party" to={`/meetups/${meetup.id}`}>
      <GameIcon game={game} />
      <div className="lobby-party-copy">
        <span className="lobby-party-game">{mode ?? (game ? gameName(game) : `${t('게임')} #${meetup.gameId}`)}</span>
        <h3>{meetup.title}</h3>
        {game?.slug === 'league-of-legends' && <p className="lobby-party-server">{optionName(meetup.regionOptionId) ?? t('서버 미지정')}</p>}
        {(minimum || maximum || roles.length > 0) && <p className="lobby-party-conditions">{[minimum || maximum ? `${minimum ?? t("제한 없음")} ~ ${maximum ?? t("제한 없음")}` : null, ...roles].filter(Boolean).join(' · ')}</p>}
        <p>{t(playStyles[meetup.playStyle] ?? meetup.playStyle)}<span aria-hidden="true"> · </span>{meetup.voiceChatPolicy === 'REQUIRED' ? t("음성 필수") : meetup.voiceChatPolicy === 'OPTIONAL' ? t("음성 선택") : t("음성 없음")}</p>
      </div>
      <time className="lobby-party-time" dateTime={meetup.startsAt}>{formatDate(meetup.startsAt)}</time>
      <div className="lobby-party-state">
        <span className={meetup.status === 'OPEN' ? 'lobby-status is-open' : 'lobby-status'}>{t(statuses[meetup.status] ?? meetup.status)}</span>
        <span aria-label={locale === 'en' ? `${meetup.reservedCount} of ${meetup.capacity} players` : `참여 인원 ${meetup.reservedCount}명, 정원 ${meetup.capacity}명`}><b>{meetup.reservedCount}</b> / {meetup.capacity}{t("명")}</span>
      </div>
      <span className="lobby-party-arrow" aria-hidden="true">↗</span>
    </Link>
  </li>
}

export default function Home() {
  const locale = useLocale()
  const [params, setParams] = useSearchParams()
  const selectedId = Number(params.get('gameId'))
  const games = useQuery({ queryKey: ['games'], queryFn: api.games })
  const league = games.data?.find(game => game.slug === 'league-of-legends' && game.active)
  const allGames = params.get('view') === 'all'
  const gameId = Number.isSafeInteger(selectedId) && selectedId > 0 ? selectedId : allGames ? undefined : league?.id
  const isLeague = !allGames && (!gameId || gameId === league?.id)
  const detail = useQuery({ queryKey: ['game', gameId], queryFn: () => { if (!gameId) throw new Error(t("게임을 선택해 주세요.")); return api.game(gameId) }, enabled: !!gameId })
  const server = selectedServer(params.get('server'))
  useEffect(() => { if (isLeague) rememberServer(server) }, [isLeague, server])
  const region = isLeague && server !== 'ALL' ? detail.data?.options.find(option => option.type === 'REGION' && option.active && option.code === server) : undefined
  const serverReady = !isLeague || server === 'ALL' || !!region
  const canLoad = games.isSuccess && (allGames || !!gameId) && serverReady
  const changeServer = (value: LeagueServer) => {
    rememberServer(value)
    setParams(current => { const next = new URLSearchParams(current); next.set('server', value); return next })
  }
  const meetups = useInfiniteQuery({
    queryKey: ['meetups', 'lobby', gameId, isLeague ? server : 'ALL', region?.id],
    enabled: canLoad,
    initialPageParam: undefined as number | undefined,
    queryFn: ({ pageParam }) => api.meetups(gameId, pageParam, region?.id),
    getNextPageParam: page => page.hasNext ? page.nextCursor ?? undefined : undefined,
  })
  const selectedGame = games.data?.find(game => game.id === gameId)
  const createPath = gameId ? `/meetups/new?gameId=${gameId}${isLeague && server !== 'ALL' ? `&server=${server}` : ''}` : '/meetups/new'
  const parties = meetups.data?.pages.flatMap(page => page.items) ?? []
  const selectGame = (id?: number) => {
    setParams(current => {
      const next = new URLSearchParams(current)
      if (id) { next.set('gameId', String(id)); next.delete('view') }
      else { next.delete('gameId'); next.set('view', 'all') }
      return next
    })
  }

  return <div className="lobby-page">
    <a className="lobby-skip" href="#party-board">{t("모임 목록으로 건너뛰기")}</a>
    <div className="lobby-shell">
      <aside className="lobby-library" aria-labelledby="game-library-title">
        <div className="lobby-library-head"><span className="lobby-kicker" id="game-library-title">LEAGUE OF LEGENDS</span></div>
        <div className="lobby-game-list" role="group" aria-label={t("롤 파티 선택")}>
          {league && <button className={`lobby-game${isLeague ? ' is-selected' : ''}`} aria-pressed={isLeague} onClick={() => selectGame(league.id)}><GameIcon game={league} /><span>{t("롤 파티 모집")}</span><span className="lobby-game-arrow" aria-hidden="true">↗</span></button>}
        </div>
        <p className="lobby-help">{t("듀오부터 다인 파티까지,")}<br />{t("나와 맞는 플레이어를 만나요.")}</p>
        <details className="lobby-other-games" open={!isLeague || undefined}>
          <summary>{t("다른 게임")}</summary>
          <div className="lobby-game-list" role="group" aria-label={t("다른 게임 선택")}>
            <button className={`lobby-game${allGames && !gameId ? ' is-selected' : ''}`} aria-pressed={allGames && !gameId} onClick={() => selectGame()}><span>{t("전체 게임")}</span></button>
            {games.data?.filter(game => game.id !== league?.id).map(game => <button className={`lobby-game${gameId === game.id ? ' is-selected' : ''}`} aria-pressed={gameId === game.id} key={game.id} onClick={() => selectGame(game.id)}><GameIcon game={game} /><span>{gameName(game)}</span></button>)}
          </div>
        </details>
        {games.isPending && <p className="lobby-help" role="status">{t("게임 목록을 불러오고 있어요.")}</p>}
        {games.isError && <div className="lobby-inline-error" role="alert"><p>{t("게임 목록을 불러오지 못했어요.")}</p><button onClick={() => games.refetch()}>{t("다시 시도")}</button></div>}
        <span className="lobby-library-signature" aria-hidden="true">FIND YOUR DUO.<br />BUILD YOUR TEAM.</span>
      </aside>

      <div className="lobby-main">
        <div className="lobby-intro">
          <div><p className="lobby-kicker">{isLeague ? 'LEAGUE OF LEGENDS · PARTY FINDER' : "THE PLAYER'S LOBBY"}</p><h2 className="lobby-headline">{isLeague ? t("같이할 소환사를 찾고 있나요?") : t("함께할 파티를 찾아보세요")}</h2><p className="lobby-intro-description">{isLeague ? t("랭크도, 일반도, 칼바람도. 나와 맞는 롤 파티에서 함께해요.") : t("마음 맞는 모임에 합류하세요.")}</p></div>
          <Link className="lobby-create" to={createPath}><span aria-hidden="true">＋</span> {isLeague ? t("롤 파티 만들기") : t("모임 만들기")}</Link>
        </div>

        {isLeague && <div className="lobby-server-filter">
          <label>{t('서버')}<select aria-label={t('서버')} value={server} onChange={event => changeServer(event.target.value as LeagueServer)}>
            <option value="KR">{t('한국 (KR)')}</option><option value="NA">{t('북미 (NA)')}</option><option value="ALL">{t('전체 서버')}</option>
          </select></label>
        </div>}
        <section className="lobby-board" id="party-board" aria-labelledby="party-board-title" tabIndex={-1}>
          <div className="lobby-board-head"><div><span className="lobby-board-index" aria-hidden="true">01 /</span><h1 id="party-board-title">{isLeague ? t("롤 파티 모집") : (selectedGame ? gameName(selectedGame) : undefined) ?? (gameId ? t("선택한 게임") : t("전체 모임"))}</h1></div><span className="lobby-board-caption">{t("함께할 플레이어를 찾는 곳")}</span></div>
          {gameId && !isLeague && <div className="lobby-active-filter"><span>{(selectedGame ? gameName(selectedGame) : undefined) ?? t("선택한 게임")} {t("모임을 보고 있어요")}</span><button onClick={() => selectGame()}>{t("필터 해제")}<span aria-hidden="true">×</span></button></div>}
          <div aria-live="polite" aria-busy={games.isPending || (isLeague && !!gameId && detail.isPending) || (canLoad && meetups.isPending)}>
            {games.isError ? <div className="lobby-empty" role="alert"><h3>{t("게임 정보를 불러오지 못했어요.")}</h3><button className="lobby-create" onClick={() => games.refetch()}>{t("다시 불러오기")}</button></div> : games.isSuccess && !gameId && !allGames ? <div className="lobby-empty"><h3>{t("롤 파티 모집을 준비하고 있어요.")}</h3><p>{t("게임 등록 후 롤 파티를 이용할 수 있어요.")}</p><button className="lobby-create" onClick={() => selectGame()}>{t("다른 게임 둘러보기")}</button></div> : isLeague && server !== 'ALL' && detail.isError ? <div className="lobby-empty" role="alert"><h3>{t('서버 정보를 불러오지 못했어요.')}</h3><button className="lobby-create" onClick={() => detail.refetch()}>{t('다시 시도')}</button></div>
              : isLeague && server !== 'ALL' && detail.isSuccess && !region ? <div className="lobby-empty"><h3>{t('선택한 서버를 준비 중입니다.')}</h3><button className="lobby-create" onClick={() => changeServer('ALL')}>{t('전체 서버')}</button></div>
              : meetups.isPending ? <div className="lobby-loading" role="status"><span className="spinner" /><p>{t("모임을 불러오고 있어요.")}</p></div>
              : meetups.isError && !meetups.data ? <div className="lobby-empty" role="alert"><h3>{t("모임을 불러오지 못했어요.")}</h3><p>{t("잠시 후 다시 시도해 주세요.")}</p><button className="lobby-create" onClick={() => meetups.refetch()}>{t("다시 불러오기 ↻")}</button></div>
              : parties.length ? <ul className="lobby-parties">{parties.map(meetup => <PartyRow key={meetup.id} meetup={meetup} game={games.data?.find(game => game.id === meetup.gameId)} detail={detail.data} />)}</ul>
              : <div className="lobby-empty"><div className="lobby-seats" aria-hidden="true"><span /><span /><span>＋</span><span /></div><p className="lobby-kicker">YOUR PARTY STARTS HERE</p><h3>{isLeague ? t("첫 롤 파티를 모집해 보세요") : t("첫 모임의 주인공이 되어볼까요?")}</h3><p>{isLeague && server !== 'ALL' ? t('이 서버에 등록된 모임이 없어요.') : selectedGame ? (locale === 'en' ? `No ${gameName(selectedGame)} parties yet.` : `${gameName(selectedGame)} 모임이 아직 없어요.`) : t('아직 등록된 모임이 없어요.')}<br />{isLeague ? t("모드와 티어, 플레이 시간을 정하고 함께할 소환사를 초대해요.") : t("하고 싶은 게임, 편한 시간을 정해 시작해 보세요.")}</p><Link className="lobby-empty-link" to={createPath}>{isLeague ? t("롤 파티 모집하기") : selectedGame ? t("이 게임으로 모임 만들기") : t("첫 모임 만들기")} <span aria-hidden="true">↗</span></Link></div>}
          </div>
          {meetups.hasNextPage && <div className="lobby-pagination">{meetups.isFetchNextPageError && <p role="alert">{t("다음 모임을 불러오지 못했어요. 다시 시도해 주세요.")}</p>}<button onClick={() => meetups.fetchNextPage()} disabled={meetups.isFetchingNextPage}>{meetups.isFetchingNextPage ? t("불러오는 중…") : t("모임 더 보기 ↓")}</button></div>}
        </section>
      </div>
    </div>
  </div>
}
