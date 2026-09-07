import { useInfiniteQuery, useQuery } from '@tanstack/react-query'
import { Link, useSearchParams } from 'react-router-dom'
import { api } from './api'
import type { Game, Meetup } from './types'
import './lobby.css'

const playStyles: Record<string, string> = { CASUAL: '가볍게', COMPETITIVE: '승리 지향', PRACTICE: '연습', BEGINNER: '초보 환영', SOCIAL: '친목' }
const statuses: Record<string, string> = { OPEN: '모집 중', CLOSED: '마감', CANCELED: '취소됨', DRAFT: '준비 중' }
const schedule = new Intl.DateTimeFormat('ko-KR', { month: 'numeric', day: 'numeric', weekday: 'short', hour: '2-digit', minute: '2-digit' })

function GameIcon({ game }: { game?: Game }) {
  return game?.imageUrl
    ? <img className={`lobby-game-icon lobby-game-icon--${game.slug}`} src={game.imageUrl} width="36" height="36" alt="" />
    : <span className="lobby-game-icon" aria-hidden="true">{game?.name.slice(0, 1) ?? '✳'}</span>
}

function PartyRow({ meetup, game }: { meetup: Meetup; game?: Game }) {
  return <li>
    <Link className="lobby-party" to={`/meetups/${meetup.id}`}>
      <GameIcon game={game} />
      <div className="lobby-party-copy">
        <span className="lobby-party-game">{game?.name ?? `게임 #${meetup.gameId}`}</span>
        <h3>{meetup.title}</h3>
        <p>{playStyles[meetup.playStyle] ?? meetup.playStyle}<span aria-hidden="true"> · </span>{meetup.voiceChatPolicy === 'REQUIRED' ? '음성 필수' : meetup.voiceChatPolicy === 'OPTIONAL' ? '음성 선택' : '음성 없음'}</p>
      </div>
      <time className="lobby-party-time" dateTime={meetup.startsAt}>{schedule.format(new Date(meetup.startsAt))}</time>
      <div className="lobby-party-state">
        <span className={meetup.status === 'OPEN' ? 'lobby-status is-open' : 'lobby-status'}>{statuses[meetup.status] ?? meetup.status}</span>
        <span aria-label={`참여 인원 ${meetup.reservedCount}명, 정원 ${meetup.capacity}명`}><b>{meetup.reservedCount}</b> / {meetup.capacity}명</span>
      </div>
      <span className="lobby-party-arrow" aria-hidden="true">↗</span>
    </Link>
  </li>
}

export default function Home() {
  const [params, setParams] = useSearchParams()
  const selectedId = Number(params.get('gameId'))
  const gameId = Number.isSafeInteger(selectedId) && selectedId > 0 ? selectedId : undefined
  const games = useQuery({ queryKey: ['games'], queryFn: api.games })
  const meetups = useInfiniteQuery({
    queryKey: ['meetups', 'lobby', gameId],
    initialPageParam: undefined as number | undefined,
    queryFn: ({ pageParam }) => api.meetups(gameId, pageParam),
    getNextPageParam: page => page.hasNext ? page.nextCursor ?? undefined : undefined,
  })
  const selectedGame = games.data?.find(game => game.id === gameId)
  const createPath = gameId ? `/meetups/new?gameId=${gameId}` : '/meetups/new'
  const parties = meetups.data?.pages.flatMap(page => page.items) ?? []
  const selectGame = (id?: number) => {
    setParams(current => {
      const next = new URLSearchParams(current)
      if (id) next.set('gameId', String(id))
      else next.delete('gameId')
      return next
    })
  }

  return <div className="lobby-page">
    <a className="lobby-skip" href="#party-board">모임 목록으로 건너뛰기</a>
    <div className="lobby-shell">
      <aside className="lobby-library" aria-labelledby="game-library-title">
        <div className="lobby-library-head"><span className="lobby-kicker">PICK YOUR GAME</span><h2 id="game-library-title">어떤 게임 할까요?</h2></div>
        <div className="lobby-game-list" role="group" aria-label="게임 선택">
          <button className={`lobby-game${!gameId ? ' is-selected' : ''}`} aria-pressed={!gameId} onClick={() => selectGame()}><GameIcon /><span>전체 게임</span><span className="lobby-game-arrow" aria-hidden="true">↗</span></button>
          {games.data?.map(game => <button className={`lobby-game${gameId === game.id ? ' is-selected' : ''}`} key={game.id} aria-pressed={gameId === game.id} onClick={() => selectGame(game.id)}><GameIcon game={game} /><span>{game.name}</span><span className="lobby-game-arrow" aria-hidden="true">↗</span></button>)}
        </div>
        {games.isPending && <p className="lobby-help" role="status">게임 목록을 불러오고 있어요.</p>}
        {games.isError && <div className="lobby-inline-error" role="alert"><p>게임 목록을 불러오지 못했어요.</p><button onClick={() => games.refetch()}>다시 시도</button></div>}
        <div className="lobby-library-note"><span aria-hidden="true">↳</span><p>잘하는 게임도,<br />처음 해보는 게임도.<br /><b>같이 하면 더 재밌으니까.</b></p></div>
        <span className="lobby-library-signature" aria-hidden="true">LESS SOLO.<br />MORE MOYEO.</span>
      </aside>

      <div className="lobby-main">
        <div className="lobby-intro">
          <div><p className="lobby-kicker"><span aria-hidden="true">✳</span> THE PLAYER'S LOBBY</p><h1>오늘의 한 판,<br className="lobby-mobile-break" /> 여기서 모여요<span>.</span></h1><p className="lobby-intro-description">게임을 고르고, 마음 맞는 모임에 합류하세요.</p></div>
          <Link className="lobby-create" to={createPath}><span aria-hidden="true">＋</span> 모임 만들기</Link>
        </div>

        <section className="lobby-board" id="party-board" aria-labelledby="party-board-title" tabIndex={-1}>
          <div className="lobby-board-head"><div><span className="lobby-board-index" aria-hidden="true">01 /</span><h2 id="party-board-title">{selectedGame?.name ?? (gameId ? '선택한 게임' : '전체 모임')}</h2></div><span className="lobby-board-caption">함께할 플레이어를 찾는 곳</span></div>
          {gameId && <div className="lobby-active-filter"><span>{selectedGame?.name ?? '선택한 게임'} 모임을 보고 있어요</span><button onClick={() => selectGame()}>필터 해제 <span aria-hidden="true">×</span></button></div>}
          <div aria-live="polite" aria-busy={meetups.isPending}>
            {meetups.isPending ? <div className="lobby-loading" role="status"><span className="spinner" /><p>모임을 불러오고 있어요.</p></div>
              : meetups.isError && !meetups.data ? <div className="lobby-empty" role="alert"><h3>모임을 불러오지 못했어요.</h3><p>잠시 후 다시 시도해 주세요.</p><button className="lobby-create" onClick={() => meetups.refetch()}>다시 불러오기 ↻</button></div>
              : parties.length ? <ul className="lobby-parties">{parties.map(meetup => <PartyRow key={meetup.id} meetup={meetup} game={games.data?.find(game => game.id === meetup.gameId)} />)}</ul>
              : <div className="lobby-empty"><div className="lobby-seats" aria-hidden="true"><span /><span /><span>＋</span><span /></div><p className="lobby-kicker">YOUR PARTY STARTS HERE</p><h3>첫 모임의 주인공이 되어볼까요?</h3><p>{selectedGame ? `${selectedGame.name} 모임이 아직 없어요.` : '아직 등록된 모임이 없어요.'}<br />하고 싶은 게임, 편한 시간을 정해 시작해 보세요.</p><Link className="lobby-empty-link" to={createPath}>{selectedGame ? '이 게임으로 모임 만들기' : '첫 모임 만들기'} <span aria-hidden="true">↗</span></Link></div>}
          </div>
          {meetups.hasNextPage && <div className="lobby-pagination">{meetups.isFetchNextPageError && <p role="alert">다음 모임을 불러오지 못했어요. 다시 시도해 주세요.</p>}<button onClick={() => meetups.fetchNextPage()} disabled={meetups.isFetchingNextPage}>{meetups.isFetchingNextPage ? '불러오는 중…' : '모임 더 보기 ↓'}</button></div>}
          <div className="lobby-board-foot"><span>나에게 맞는 분위기, 함께할 시간.</span><span>모임에서 확인해 보세요 <span aria-hidden="true">↗</span></span></div>
        </section>
        <div className="lobby-note"><span className="lobby-kicker">GOOD GAME, GOOD COMPANY</span><p>실력은 달라도, 즐거운 한 판은 함께.</p><span aria-hidden="true">gg.</span></div>
      </div>
    </div>
  </div>
}
