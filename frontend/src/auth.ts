let accessToken: string | null = null
let expirationTimer: ReturnType<typeof setTimeout> | null = null

export type AuthEndReason = 'expired' | null

type TokenPayload = { exp?:number; roles?:unknown }

function tokenPayload(token: string):TokenPayload|null {
  try {
    const encodedPayload = token.split('.')[1]
    if (!encodedPayload) return null
    const normalized = encodedPayload.replace(/-/g, '+').replace(/_/g, '/')
    return JSON.parse(atob(normalized.padEnd(Math.ceil(normalized.length / 4) * 4, '='))) as TokenPayload
  } catch {
    return null
  }
}

function tokenExpiration(token: string) {
  const expiration = tokenPayload(token)?.exp
  return typeof expiration === 'number' ? expiration * 1000 : null
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

export function hasRole(role:string) {
  if (!accessToken) return false
  const roles = tokenPayload(accessToken)?.roles
  return Array.isArray(roles) && roles.includes(role)
}

export function isAdmin() {
  return hasRole('ADMIN')
}
