let accessToken: string | null = null
let expirationTimer: ReturnType<typeof setTimeout> | null = null

export type AuthEndReason = 'expired' | null

function tokenExpiration(token: string) {
  try {
    const encodedPayload = token.split('.')[1]
    if (!encodedPayload) return null
    const normalized = encodedPayload.replace(/-/g, '+').replace(/_/g, '/')
    const payload = JSON.parse(atob(normalized.padEnd(Math.ceil(normalized.length / 4) * 4, '='))) as { exp?:number }
    return typeof payload.exp === 'number' ? payload.exp * 1000 : null
  } catch {
    return null
  }
}

export function getAccessToken() {
  return accessToken
}

export function setAccessToken(token: string | null, reason: AuthEndReason = null) {
  if (expirationTimer) clearTimeout(expirationTimer)
  expirationTimer = null
  accessToken = token
  if (token) {
    const expiresAt = tokenExpiration(token)
    if (expiresAt) {
      const remaining = expiresAt - Date.now()
      if (remaining <= 0) {
        accessToken = null
        reason = 'expired'
      } else {
        expirationTimer = setTimeout(() => setAccessToken(null, 'expired'), remaining)
      }
    }
  }
  window.dispatchEvent(new CustomEvent<AuthEndReason>('gamemoyeo:auth', { detail:reason }))
}

export function isAuthenticated() {
  return accessToken !== null
}
