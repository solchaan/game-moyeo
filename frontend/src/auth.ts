let accessToken: string | null = null

export function getAccessToken() {
  return accessToken
}

export function setAccessToken(token: string | null) {
  accessToken = token
  window.dispatchEvent(new Event('gamemoyeo:auth'))
}

export function isAuthenticated() {
  return accessToken !== null
}
