import { expect, test } from '@playwright/test'

const games = [{ id: 1, slug: 'league-of-legends', name: '리그 오브 레전드', imageUrl: null, description: null, active: true }, { id: 3, slug: 'valorant', name: 'VALORANT', imageUrl: null, description: null, active: true }]
const regions = [{ id: 101, gameId: 1, type: 'REGION', code: 'KR', displayName: '대한민국', active: true }, { id: 102, gameId: 1, type: 'REGION', code: 'NA', displayName: '북미', active: true }]
const emptyPage = { items: [], nextCursor: null, hasNext: false }
const party = {
  id: 41, gameId: 1, ownerId: 1, title: '오늘 저녁 가볍게 한 판 같이 해요',
  description: '', playStyle: 'CASUAL', voiceChatPolicy: 'OPTIONAL', approvalType: 'FIRST_COME',
  startsAt: '2026-10-01T12:00:00Z', endsAt: '2026-10-01T14:00:00Z',
  reservedCount: 2, capacity: 5, status: 'OPEN', roleRequirements: {},
}

test.beforeEach(async ({ page }) => {
  await page.route('**/api/v1/games/*', route => route.fulfill({ json: { game: games[0], options: regions } }))
  await page.route('**/api/v1/games', route => route.fulfill({ json: games }))
  await page.route('**/api/v1/meetups?*', route => route.fulfill({ json: emptyPage }))
})

test('shows the lobby and its empty state without horizontal overflow', async ({ page }) => {
  await page.goto('/')
  await expect(page.getByRole('heading', { level: 1, name: '롤 파티 모집', exact: true })).toBeVisible()
  await expect(page.getByRole('heading', { name: '첫 롤 파티를 모집해 보세요' })).toBeVisible()
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
})

test('omits the removed promotional copy and keeps game navigation accessible', async ({ page }) => {
  await page.goto('/')
  await expect(page.getByRole('heading', { level: 1, name: '롤 파티 모집', exact: true })).toBeVisible()
  await expect(page.getByRole('complementary', { name: 'LEAGUE OF LEGENDS' })).toBeVisible()
  await expect(page.getByRole('button', { name: '롤 파티 모집' })).toHaveAttribute('aria-pressed', 'true')
  await expect(page.getByRole('button', { name: 'VALORANT' })).not.toBeVisible()
  await page.getByText('다른 게임', { exact: true }).click()
  await expect(page.getByRole('button', { name: 'VALORANT' })).toBeVisible()
  for (const copy of [
    /어떤 게임 할까요/, /잘하는 게임도/, /처음 해보는 게임도/, /같이 하면 더 재밌으니까/,
    /오늘의 한 판/, /여기서 모여요/, /나에게 맞는 분위기/, /함께할 시간/,
    /모임에서 확인해\s*보세요/, /gg\./i, /실력은 달라도/, /즐거운 한 판은 함께/,
    /good game, good company/i,
  ]) {
    await expect(page.getByText(copy)).toHaveCount(0)
  }
})

test('persists the selected game across reload and browser back', async ({ page }) => {
  await page.goto('/')
  await page.getByText('다른 게임', { exact: true }).click()
  await page.getByRole('button', { name: 'VALORANT' }).click()
  await expect(page).toHaveURL(/\?gameId=3$/)
  await page.reload()
  await expect(page.getByRole('button', { name: 'VALORANT' })).toHaveAttribute('aria-pressed', 'true')
  await page.getByRole('button', { name: '필터 해제' }).click()
  await expect(page.getByRole('heading', { name: '전체 모임', exact: true })).toBeVisible()
  await page.goBack()
  await expect(page.getByRole('button', { name: 'VALORANT' })).toHaveAttribute('aria-pressed', 'true')
})

test('keeps the selected game when creation requires login', async ({ page }) => {
  await page.goto('/?gameId=3')
  await page.getByRole('link', { name: '이 게임으로 모임 만들기' }).click()
  await expect(page).toHaveURL(/\/login$/)
  await expect(page.getByRole('heading', { name: '로그인', exact: true })).toBeVisible()
  expect(await page.evaluate(() => sessionStorage.getItem('gamemoyeo:login-return-to'))).toBe('/meetups/new?gameId=3')
})

test('displays server statuses and appends the next cursor page', async ({ page }) => {
  await page.route('**/api/v1/meetups?*', route => {
    const next = new URL(route.request().url()).searchParams.has('cursor')
    return route.fulfill({ json: next
      ? { items: [{ ...party, id: 42, title: '다음 페이지 모임', status: 'CLOSED' }], nextCursor: null, hasNext: false }
      : { items: [party], nextCursor: 41, hasNext: true } })
  })
  await page.goto('/')
  await expect(page.getByText('모집 중', { exact: true })).toBeVisible()
  await page.getByRole('button', { name: '모임 더 보기' }).click()
  await expect(page.getByRole('link', { name: /오늘 저녁 가볍게/ })).toBeVisible()
  await expect(page.getByRole('link', { name: /다음 페이지 모임/ })).toBeVisible()
  await expect(page.getByText('마감', { exact: true })).toBeVisible()
  await expect(page.getByRole('button', { name: '모임 더 보기' })).toHaveCount(0)
})

test('recovers from API failure with retry', async ({ page }) => {
  let fail = true
  await page.route('**/api/v1/meetups?*', route => fail
    ? route.fulfill({ status: 503, json: { detail: 'Temporarily unavailable' } })
    : route.fulfill({ json: emptyPage }))
  await page.goto('/')
  await expect(page.getByRole('alert')).toContainText('모임을 불러오지 못했어요.')
  fail = false
  await page.getByRole('button', { name: '다시 불러오기' }).click()
  await expect(page.getByRole('heading', { name: '첫 롤 파티를 모집해 보세요' })).toBeVisible()
})

test('serves direct SPA routes and keyboard game selection', async ({ page }) => {
  await page.goto('/login')
  await expect(page.getByRole('heading', { name: '로그인', exact: true })).toBeVisible()
  await page.goto('/')
  await page.getByText('다른 게임', { exact: true }).click()
  const game = page.getByRole('button', { name: 'VALORANT' })
  await game.focus()
  await page.keyboard.press('Enter')
  await expect(game).toHaveAttribute('aria-pressed', 'true')
})


test('requests only League parties by default and preserves creation target', async ({ page }) => {
  const requests: string[] = []
  page.on('request', request => { if (request.url().includes('/api/v1/meetups?')) requests.push(request.url()) })
  await page.goto('/')
  await expect(page.getByRole('heading', { name: '첫 롤 파티를 모집해 보세요' })).toBeVisible()
  expect(requests.length).toBeGreaterThan(0)
  expect(requests.every(url => new URL(url).searchParams.get('gameId') === '1')).toBe(true)
  await page.getByRole('link', { name: '롤 파티 모집하기' }).click()
  await expect(page).toHaveURL(/\/login$/)
  expect(await page.evaluate(() => sessionStorage.getItem('gamemoyeo:login-return-to'))).toBe('/meetups/new?gameId=1&server=KR')
})

test('keeps other games available when League is not configured', async ({ page }) => {
  await page.route('**/api/v1/games', route => route.fulfill({ json: [games[1]] }))
  await page.goto('/')
  await expect(page.getByRole('heading', { name: '롤 파티 모집을 준비하고 있어요.' })).toBeVisible()
  await page.getByRole('button', { name: '다른 게임 둘러보기' }).click()
  await expect(page.getByRole('heading', { name: '전체 모임', exact: true })).toBeVisible()
})

test('shows server mode, tier and position requirements', async ({ page }) => {
  await page.route('**/api/v1/games/1', route => route.fulfill({ json: { game: games[0], options: [...regions,
    { id: 10, type: 'MODE', displayName: '솔로 랭크', active: true },
    { id: 20, type: 'TIER', displayName: '골드', active: true },
    { id: 30, type: 'ROLE', displayName: '서포터', active: true },
  ] } }))
  await page.route('**/api/v1/meetups?*', route => route.fulfill({ json: { ...emptyPage, items: [{ ...party, modeOptionId: 10, minimumTierOptionId: 20, maximumTierOptionId: 20, roleRequirements: { 30: 1 } }] } }))
  await page.goto('/')
  await expect(page.getByText('솔로 랭크', { exact: true })).toBeVisible()
  await expect(page.getByText('골드 ~ 골드 · 서포터 1명', { exact: true })).toBeVisible()
})

test('defaults creation to League, submits positions, and clears them when switching games', async ({ page }) => {
  const token = `e30.${Buffer.from(JSON.stringify({ sub: '7', exp: Math.floor(Date.now() / 1000) + 3600, roles: ['MEMBER'] })).toString('base64url')}.test`
  await page.route('**/api/v1/auth/login', route => route.fulfill({ json: { accessToken: token } }))
  await page.route('**/api/v1/games/1', route => route.fulfill({ json: { game: games[0], options: [...regions,
    { id: 30, type: 'ROLE', displayName: '서포터', active: true },
  ] } }))
  await page.goto('/meetups/new')
  await page.getByLabel('아이디', { exact: true }).fill('testuser')
  await page.getByLabel('비밀번호', { exact: true }).fill('password123')
  await page.getByRole('button', { name: '로그인 →', exact: true }).click()
  await expect(page.getByRole('heading', { name: '함께할 롤 파티원을 모집해요' })).toBeVisible()
  await expect(page.getByRole('combobox', { name: '게임', exact: true })).toHaveValue('1')
  await page.getByLabel('서포터', { exact: true }).fill('1')
  await page.getByLabel('모임 제목').fill('함께할 서포터 구해요')
  await page.getByLabel('시작 시간').fill('2099-10-01T18:00')
  await page.getByLabel('종료 시간').fill('2099-10-01T20:00')
  const bodies: { gameId: number; roleRequirements: {roleOptionId: number; capacity: number}[] }[] = []
  await page.route('**/api/v1/meetups', route => {
    bodies.push(route.request().postDataJSON())
    return route.fulfill({ status: 400, json: { detail: '입력 내용을 확인해 주세요.' } })
  })
  await page.getByRole('button', { name: '모임 만들기 →' }).click()
  await expect.poll(() => bodies.length).toBe(1)
  expect(bodies[0].gameId).toBe(1)
  expect(bodies[0].roleRequirements).toEqual([{ roleOptionId: 30, capacity: 1 }])
  await page.getByRole('combobox', { name: '게임', exact: true }).selectOption('3')
  await page.getByRole('button', { name: '모임 만들기 →' }).click()
  await expect.poll(() => bodies.length).toBe(2)
  expect(bodies[1].gameId).toBe(3)
  expect(bodies[1].roleRequirements).toEqual([])
})

test('language and server choices persist independently with server pagination', async ({ page }) => {
  const regionsRequested: string[] = []
  await page.route('**/api/v1/meetups?*', route => {
    const params = new URL(route.request().url()).searchParams
    const region = params.get('regionOptionId') ?? 'ALL'
    regionsRequested.push(region)
    const next = params.has('cursor')
    return route.fulfill({ json: { items: [{ ...party, id: next ? 42 : 41, regionOptionId: Number(region), title: next ? '두 번째 북미 파티' : '직접 작성한 파티 제목' }], nextCursor: next ? null : 41, hasNext: !next } })
  })
  await page.goto('/')
  await expect(page.getByRole('link', { name: /직접 작성한 파티 제목/ })).toBeVisible()
  expect(regionsRequested).toEqual(['101'])
  await page.getByRole('combobox', { name: '서버', exact: true }).selectOption('NA')
  await expect(page).toHaveURL(/server=NA/)
  await expect.poll(() => regionsRequested.at(-1)).toBe('102')
  await page.getByRole('combobox', { name: '언어' }).selectOption('en')
  await expect(page.locator('html')).toHaveAttribute('lang', 'en')
  await expect(page.getByRole('heading', { name: 'Looking for your next teammate?' })).toBeVisible()
  await expect(page.getByRole('combobox', { name: 'Server', exact: true })).toHaveValue('NA')
  await expect(page.getByRole('link', { name: /직접 작성한 파티 제목/ })).toBeVisible()
  await page.getByRole('button', { name: 'Load more parties' }).click()
  await expect(page.getByRole('link', { name: /두 번째 북미 파티/ })).toBeVisible()
  expect(regionsRequested.slice(1).every(region => region === '102')).toBe(true)
  await page.reload()
  await expect(page.getByRole('combobox', { name: 'Language' })).toHaveValue('en')
  await expect(page.getByRole('combobox', { name: 'Server', exact: true })).toHaveValue('NA')
  await page.getByRole('combobox', { name: 'Server', exact: true }).selectOption('KR')
  await expect(page.getByRole('combobox', { name: 'Language' })).toHaveValue('en')
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
})

test('English NA creation keeps selected server through login and language changes', async ({ page }) => {
  const token = `e30.${Buffer.from(JSON.stringify({ sub: '7', exp: Math.floor(Date.now() / 1000) + 3600, roles: ['MEMBER'] })).toString('base64url')}.test`
  await page.route('**/api/v1/auth/login', route => route.fulfill({ json: { accessToken: token } }))
  await page.goto('/?server=NA')
  await page.getByRole('combobox', { name: '언어' }).selectOption('en')
  await page.getByRole('link', { name: 'Create a League party', exact: true }).first().click()
  await page.getByRole('textbox', { name: 'Username', exact: true }).fill('testuser')
  await page.getByLabel('Password', { exact: true }).fill('password123')
  await page.getByRole('button', { name: 'Log in →' }).click()
  await expect(page.getByRole('combobox', { name: 'Server', exact: true })).toHaveValue('102')
  await page.getByRole('textbox', { name: 'Party title', exact: true }).fill('NA duo tonight')
  await page.getByRole('combobox', { name: 'Language' }).selectOption('ko')
  await expect(page.getByRole('combobox', { name: '서버', exact: true })).toHaveValue('102')
  await expect(page.getByRole('textbox', { name: '모임 제목', exact: true })).toHaveValue('NA duo tonight')
  await page.getByLabel('시작 시간').fill('2099-10-01T18:00')
  await page.getByLabel('종료 시간').fill('2099-10-01T20:00')
  let submitted: Record<string, unknown> | undefined
  await page.route('**/api/v1/meetups', route => { submitted = route.request().postDataJSON(); return route.fulfill({ status: 400, json: { detail: '입력한 내용을 확인해 주세요.' } }) })
  await page.getByRole('button', { name: '모임 만들기 →' }).click()
  await expect.poll(() => submitted?.regionOptionId).toBe(102)
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
})

test('all servers preserves legacy parties and other games do not inherit League server filters', async ({ page }) => {
  const queries: URLSearchParams[] = []
  page.on('request', request => { if (request.url().includes('/api/v1/meetups?')) queries.push(new URL(request.url()).searchParams) })
  await page.goto('/?server=ALL')
  await expect(page.getByRole('heading', { name: '첫 롤 파티를 모집해 보세요' })).toBeVisible()
  expect(queries[0].has('regionOptionId')).toBe(false)
  await page.getByText('다른 게임', { exact: true }).click()
  await page.getByRole('button', { name: 'VALORANT' }).click()
  await expect(page.getByRole('heading', { name: 'VALORANT', exact: true })).toBeVisible()
  await expect(page.getByRole('combobox', { name: '서버', exact: true })).toHaveCount(0)
  await expect.poll(() => queries.at(-1)?.get('gameId')).toBe('3')
  expect(queries.at(-1)?.has('regionOptionId')).toBe(false)
})
