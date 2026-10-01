export type LeagueServer = 'KR' | 'NA' | 'ALL'
const key = 'gamemoyeo:league-server'
export function selectedServer(value: string | null): LeagueServer {
  if (value === 'KR' || value === 'NA' || value === 'ALL') return value
  try { const stored = localStorage.getItem(key); if (stored === 'NA' || stored === 'ALL') return stored } catch { /* Use KR by default. */ }
  return 'KR'
}
export function rememberServer(value: LeagueServer) {
  try { localStorage.setItem(key, value) } catch { /* The URL still preserves the selection. */ }
}
