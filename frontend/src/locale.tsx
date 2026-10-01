import { useEffect, useSyncExternalStore } from 'react'
import type { Game, GameOption } from './types'
import { english } from './translations'

export type Locale = 'ko' | 'en'
const key = 'gamemoyeo:language'
const listeners = new Set<() => void>()
function storedLocale(): Locale {
  try { return localStorage.getItem(key) === 'en' ? 'en' : 'ko' } catch { return 'ko' }
}
let locale = storedLocale()
const subscribe = (listener: () => void) => { listeners.add(listener); return () => { listeners.delete(listener) } }
export function setLocale(next: Locale) {
  locale = next
  try { localStorage.setItem(key, next) } catch { /* A session-only preference still works. */ }
  listeners.forEach(listener => listener())
}
window.addEventListener('storage', event => {
  if (event.key === key || event.key === null) { locale = storedLocale(); listeners.forEach(listener => listener()) }
})
export const getLocale = () => locale
export function useLocale() { return useSyncExternalStore(subscribe, () => locale) }
export function t(korean: string) { return locale === 'en' ? english[korean] ?? korean : korean }
export function useDocumentLocale() {
  const current = useLocale()
  useEffect(() => {
    document.documentElement.lang = current
    document.title = current === 'en' ? 'Game Moyeo | League party finder' : '게임모여 | 롤 파티 모집'
    document.querySelector('meta[name="description"]')?.setAttribute('content', current === 'en'
      ? 'Find League of Legends duos and teams on KR and NA servers.'
      : 'KR·북미 서버에서 함께할 롤 듀오와 파티원을 찾아보세요.')
  }, [current])
}
export function LanguagePicker() {
  const current = useLocale()
  return <select className="language-picker" aria-label={t('언어')} value={current} onChange={event => setLocale(event.target.value as Locale)}>
    <option value="ko">KOR</option><option value="en">EN</option>
  </select>
}

const gameNames: Record<string, string> = { 'league-of-legends': 'League of Legends', 'fc-online': 'FC Online', 'pubg-battlegrounds': 'PUBG: BATTLEGROUNDS', valorant: 'VALORANT', 'lost-ark': 'Lost Ark', 'sudden-attack': 'Sudden Attack', overwatch: 'Overwatch' }
export const gameName = (game: Game) => locale === 'en' ? gameNames[game.slug] ?? game.name : game.name
const optionNames: Record<string, string> = {
  NORMAL_DRAFT: 'Normal Draft', RANKED_SOLO: 'Ranked Solo/Duo', RANKED_FLEX: 'Ranked Flex', ARAM: 'ARAM',
  TOP: 'Top', JUNGLE: 'Jungle', MID: 'Mid', BOTTOM: 'Bottom', SUPPORT: 'Support',
  IRON: 'Iron', BRONZE: 'Bronze', SILVER: 'Silver', GOLD: 'Gold', PLATINUM: 'Platinum', EMERALD: 'Emerald',
  DIAMOND: 'Diamond', MASTER: 'Master', GRANDMASTER: 'Grandmaster', CHALLENGER: 'Challenger',
  KR: 'Korea (KR)', NA: 'North America (NA)', PC: 'PC', SUMMONERS_RIFT: "Summoner's Rift", HOWLING_ABYSS: 'Howling Abyss',
}
export function optionName(option: GameOption) {
  if (option.type === 'REGION' && option.code === 'KR') return t('한국 (KR)')
  if (option.type === 'REGION' && option.code === 'NA') return t('북미 (NA)')
  return locale === 'en' ? optionNames[option.code] ?? option.displayName : option.displayName
}
export function formatDate(value: string) {
  return new Intl.DateTimeFormat(locale === 'en' ? 'en-US' : 'ko-KR', {month:'short',day:'numeric',weekday:'short',hour:'2-digit',minute:'2-digit'}).format(new Date(value))
}
