import { expect, test } from '@playwright/test'

const token = `e30.${Buffer.from(JSON.stringify({sub:'7',exp:Math.floor(Date.now()/1000)+3600,roles:['MEMBER']})).toString('base64url')}.test`
test.beforeEach(async ({ page }) => {
  await page.route('**/api/v1/games', route => route.fulfill({json:[]}))
  await page.route('**/api/v1/meetups?*', route => route.fulfill({json:{items:[],nextCursor:null,hasNext:false}}))
  await page.route('**/api/v1/auth/login', route => route.fulfill({json:{accessToken:token}}))
})
async function login(page: import('@playwright/test').Page) {
  await page.goto('/account')
  await page.getByLabel('아이디', {exact:true}).fill('testuser')
  await page.getByLabel('비밀번호', {exact:true}).fill('password123')
  await page.getByRole('button', {name:'로그인 →',exact:true}).click()
  await expect(page.getByRole('heading',{name:'게임 계정 연결'})).toBeVisible()
}
test('approval-pending state disables linking and fits every viewport', async ({page}) => {
  await page.route('**/api/v1/members/me/riot', route => route.fulfill({json:{enabled:false,linked:false,riotId:null}}))
  await login(page)
  await expect(page.getByRole('button',{name:'Riot 계정 연결',exact:true})).toBeDisabled()
  await expect(page.getByText(/Riot 계정 연동을 준비 중/)).toBeVisible()
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
})
test('shows verified Riot ID and unlinks only after confirmation', async ({page}) => {
  let linked=true
  await page.route('**/api/v1/members/me/riot', route => {
    if(route.request().method()==='DELETE'){ linked=false; return route.fulfill({status:204}) }
    return route.fulfill({json:{enabled:true,linked,riotId:linked?'Player#KR1':null}})
  })
  await login(page)
  await expect(page.getByText('Player#KR1')).toBeVisible()
  await page.getByRole('button',{name:'연결 해제',exact:true}).click()
  await expect(page.getByRole('button',{name:'해제하기',exact:true})).toBeVisible()
  await page.getByRole('button',{name:'취소',exact:true}).click()
  expect(linked).toBe(true)
  await page.getByRole('button',{name:'연결 해제',exact:true}).click()
  await page.getByRole('button',{name:'해제하기',exact:true}).click()
  await expect(page.getByText('아직 연결된 계정이 없습니다.')).toBeVisible()
})
test('callback without opener clears code and explains recovery', async ({page}) => {
  await page.goto('/riot/callback?code=temporary-secret')
  await expect(page).toHaveURL(/\/riot\/callback$/)
  await expect(page.getByText(/인증을 시작한 창을 찾지 못했습니다/)).toBeVisible()
})

test('completes popup authentication with existing member token', async ({page,context}) => {
  let linked=false
  let completions=0
  const code='a'.repeat(43)
  await page.route('**/api/v1/members/me/riot', route => {
    if(route.request().method()==='POST') {
      expect(route.request().headers().authorization).toBe(`Bearer ${token}`)
      expect(route.request().postDataJSON()).toEqual({code})
      linked=true; completions++
      return route.fulfill({status:204})
    }
    return route.fulfill({json:{enabled:true,linked,riotId:linked?'Verified#KR1':null}})
  })
  await page.route('**/api/v1/members/me/riot/authorization', route => route.fulfill({json:{authorizationUrl:'https://auth.riotgames.com/authorize?state=test'}}))
  await login(page)
  const origin=new URL(page.url()).origin
  await context.route('https://auth.riotgames.com/authorize?*', route => route.fulfill({contentType:'text/html',body:`<script>location.replace(${JSON.stringify(`${origin}/riot/callback?code=${code}`)})</script>`}))
  await page.getByRole('button',{name:'Riot 계정 연결',exact:true}).click()
  await expect(page.getByText('Verified#KR1')).toBeVisible()
  expect(completions).toBe(1)
})
