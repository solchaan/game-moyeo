import { expect, test } from '@playwright/test'

const games = [{ id: 3, slug: 'valorant', name: 'VALORANT', imageUrl: null, description: null, active: true }]
const emptyPage = { items: [], nextCursor: null, hasNext: false }
const party = {
  id: 41, gameId: 3, ownerId: 1, title: '오늘 저녁 가볍게 한 판 같이 해요',
  description: '', playStyle: 'CASUAL', voiceChatPolicy: 'OPTIONAL', approvalType: 'FIRST_COME',
  startsAt: '2026-10-01T12:00:00Z', endsAt: '2026-10-01T14:00:00Z',
  reservedCount: 2, capacity: 5, status: 'OPEN', roleRequirements: {},
}

test.beforeEach(async ({ page }) => {
  await page.route('**/api/v1/games', route => route.fulfill({ json: games }))
  await page.route('**/api/v1/meetups?*', route => route.fulfill({ json: emptyPage }))
})

test('shows the lobby and its empty state without horizontal overflow', async ({ page }) => {
  await page.goto('/')
  await expect(page.getByRole('heading', { level: 1, name: '전체 모임', exact: true })).toBeVisible()
  await expect(page.getByRole('heading', { name: '첫 모임의 주인공이 되어볼까요?' })).toBeVisible()
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
})

test('omits the removed promotional copy and keeps game navigation accessible', async ({ page }) => {
  await page.goto('/')
  await expect(page.getByRole('heading', { level: 1, name: '전체 모임', exact: true })).toBeVisible()
  await expect(page.getByRole('complementary', { name: 'PICK YOUR GAME' })).toBeVisible()
  await expect(page.getByRole('group', { name: '게임 선택' }).getByRole('button', { name: '전체 게임' })).toBeVisible()
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
  await expect(page.getByRole('heading', { name: '첫 모임의 주인공이 되어볼까요?' })).toBeVisible()
})

test('serves direct SPA routes and keyboard game selection', async ({ page }) => {
  await page.goto('/login')
  await expect(page.getByRole('heading', { name: '로그인', exact: true })).toBeVisible()
  await page.goto('/')
  const game = page.getByRole('button', { name: 'VALORANT' })
  await game.focus()
  await page.keyboard.press('Enter')
  await expect(game).toHaveAttribute('aria-pressed', 'true')
})
