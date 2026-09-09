import { expect, test } from '@playwright/test'
import type { Page } from '@playwright/test'

const game={id:1,slug:'test-game',name:'테스트 게임',description:null,imageUrl:null,active:true}
const token=(roles=['MEMBER'])=>`e30.${Buffer.from(JSON.stringify({sub:'7',exp:Math.floor(Date.now()/1000)+3600,roles})).toString('base64url')}.test`
const startsAt='2099-10-01T18:00'
const endsAt='2099-10-01T20:00'
const meetup={id:9,gameId:1,ownerId:7,title:'함께 게임해요',description:'입력 유지 확인',playStyle:'CASUAL',voiceChatPolicy:'OPTIONAL',approvalType:'FIRST_COME',startsAt:'2099-10-01T09:00:00Z',endsAt:'2099-10-01T11:00:00Z',capacity:5,reservedCount:1,status:'OPEN',roleRequirements:{}}

test.beforeEach(async({page})=>{
  await page.route('**/api/v1/games',route=>route.fulfill({json:[game]}))
  await page.route('**/api/v1/games/1',route=>route.fulfill({json:{game,options:[]}}))
  await page.route('**/api/v1/meetups?*',route=>route.fulfill({json:{items:[],hasNext:false,nextCursor:null}}))
})
async function login(page:Page,roles?:string[]) {
  await page.route('**/api/v1/auth/login',route=>route.fulfill({json:{accessToken:token(roles)}}))
  await page.goto('/meetups/new?gameId=1')
  await page.getByLabel('아이디',{exact:true}).fill('testuser')
  await page.getByLabel('비밀번호',{exact:true}).fill('password123')
  await page.getByRole('button',{name:'로그인 →',exact:true}).click()
  await expect(page).toHaveURL(roles?.includes('ADMIN')?/\/admin\/games$/:/\/meetups\/new\?gameId=1$/)
}
async function fillMeetup(page:Page) {
  await page.getByLabel('모임 제목').fill(meetup.title)
  await page.getByLabel('상세 설명').fill(meetup.description)
  await page.getByLabel('시작 시간').fill(startsAt)
  await page.getByLabel('종료 시간').fill(endsAt)
}

test('shows server field errors, focuses input, preserves values and succeeds on retry',async({page})=>{
  let attempts=0
  await page.route('**/api/v1/meetups',async route=>{
    attempts++
    expect(route.request().headers()['idempotency-key']).toBeTruthy()
    expect(route.request().postDataJSON().startsAt).toMatch(/Z$/)
    if(attempts===1)return route.fulfill({status:400,json:{code:'INVALID_MEETUP_OPTION',detail:'Invalid request content.',traceId:'trace-test',fieldErrors:[{field:'startsAt',reason:'시작 시간은 현재보다 10분 넘게 뒤로 설정해 주세요.'}]}})
    return route.fulfill({status:201,json:meetup})
  })
  await page.route('**/api/v1/meetups/9',route=>route.fulfill({json:meetup}))
  await page.route('**/api/v1/meetups/9/participation',route=>route.fulfill({json:{participantCount:1,capacity:5,recruitmentStatus:'OPEN',participationStatus:'OWNER'}}))
  await login(page)
  await fillMeetup(page)
  await page.getByRole('button',{name:'모임 만들기 →'}).click()
  const start=page.locator('[name="startsAt"]')
  await expect(start).toBeFocused()
  await expect(start).toHaveAttribute('aria-invalid','true')
  await expect(start).toHaveAccessibleDescription('시작 시간은 현재보다 10분 넘게 뒤로 설정해 주세요.')
  await expect(page.locator('.error-toasts')).toContainText('시작 시간')
  await expect(page.locator('.error-toasts')).toContainText('trace-test')
  await expect(page.getByLabel('모임 제목')).toHaveValue(meetup.title)
  await page.getByRole('button',{name:'오류 알림 닫기'}).click()
  await expect(start).toHaveAttribute('aria-invalid','true')
  await start.fill('2099-10-01T19:00')
  await expect(start).toHaveAttribute('aria-invalid','false')
  await page.getByRole('button',{name:'모임 만들기 →'}).click()
  await expect(page).toHaveURL(/\/meetups\/9$/)
  expect(attempts).toBe(2)
})

test('empty form identifies required fields without sending request',async({page})=>{
  let attempts=0
  await page.route('**/api/v1/meetups',route=>{attempts++;return route.abort()})
  await login(page)
  await page.getByRole('button',{name:'모임 만들기 →'}).click()
  await expect(page.locator('[name="title"]')).toBeFocused()
  await expect(page.locator('[name="startsAt"]')).toHaveAttribute('aria-invalid','true')
  await expect(page.locator('.error-toasts')).toContainText('입력한 항목')
  expect(attempts).toBe(0)
})

for(const [status,message] of [[403,'권한'],[409,'현재 상태'],[429,'요청이 많습니다'],[500,'서버 오류']] as const) {
  test(`handles ${status} with a Korean toast and preserved input`,async({page})=>{
    await page.route('**/api/v1/meetups',route=>route.fulfill({status,json:{detail:'Internal English error / SQL should not be shown'}}))
    await login(page)
    await fillMeetup(page)
    await page.getByRole('button',{name:'모임 만들기 →'}).click()
    await expect(page.locator('.error-toasts')).toContainText(message)
    await expect(page.locator('.error-toasts')).not.toContainText('SQL')
    await expect(page.getByLabel('모임 제목')).toHaveValue(meetup.title)
    expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true)
  })
}

test('network failures show one dismissible toast',async({page})=>{
  await page.route('**/api/v1/meetups',route=>route.abort('failed'))
  await login(page)
  await fillMeetup(page)
  await page.getByRole('button',{name:'모임 만들기 →'}).click()
  await expect(page.locator('.error-toasts')).toContainText('인터넷 연결')
  await page.getByRole('button',{name:'모임 만들기 →'}).click()
  await expect(page.locator('.error-toast')).toHaveCount(1)
  await page.getByRole('button',{name:'오류 알림 닫기'}).focus()
  await page.keyboard.press('Enter')
  await expect(page.locator('.error-toast')).toHaveCount(0)
})

test('registration field errors attach to nickname',async({page})=>{
  await page.route('**/api/v1/auth/register',route=>route.fulfill({status:400,json:{fieldErrors:[{field:'nickname',reason:'must not be blank'}]}}))
  await page.goto('/login')
  await page.getByRole('button',{name:'아직 계정이 없나요? 회원가입'}).click()
  await page.getByLabel('아이디',{exact:true}).fill('testuser')
  await page.getByLabel('닉네임',{exact:true}).fill('테스트')
  await page.getByLabel('비밀번호',{exact:true}).fill('password123')
  await page.getByRole('button',{name:'가입하고 시작하기 →'}).click()
  await expect(page.locator('[name="nickname"]')).toHaveAttribute('aria-invalid','true')
  await expect(page.locator('[name="nickname"]')).toHaveAccessibleDescription('필수 항목을 입력해 주세요.')
})

test('nested admin option validation points to the correct row',async({page})=>{
  await page.route('**/api/v1/admin/games',route=>route.fulfill({status:400,json:{fieldErrors:[{field:'options[0].code',reason:'must match pattern'}]}}))
  await login(page,['ADMIN'])
  await page.getByRole('link',{name:'새 게임 등록 →'}).click()
  await page.getByLabel('게임 이름').fill('새 게임')
  await page.getByLabel('URL 식별자').fill('new-game')
  await page.getByRole('button',{name:'+ 게임 모드 추가',exact:true}).click()
  await page.getByLabel('표시 이름').fill('일반전')
  await page.getByLabel('관리 코드').fill('NORMAL')
  await page.getByRole('button',{name:'게임 등록하기 →'}).click()
  await expect(page.locator('[name="options[0].code"]')).toHaveAttribute('aria-invalid','true')
  await expect(page.locator('.error-toasts')).toContainText('1번째 조건 · 관리 코드')
})

test('Retry-After pauses form submission and allows retry afterward',async({page})=>{
  await page.route('**/api/v1/meetups',route=>route.fulfill({status:429,headers:{'Retry-After':'2'},json:{}}))
  await login(page)
  await fillMeetup(page)
  await page.getByRole('button',{name:'모임 만들기 →'}).click()
  await expect(page.getByRole('button',{name:'모임 만들기 →'})).toBeDisabled()
  await expect(page.getByRole('status')).toContainText('초 후 다시 시도')
  await expect(page.getByRole('button',{name:'모임 만들기 →'})).toBeEnabled({timeout:5000})
})

test('admin local validation focuses the missing option name',async({page})=>{
  await login(page,['ADMIN'])
  await page.getByRole('link',{name:'새 게임 등록 →'}).click()
  await page.getByLabel('게임 이름').fill('새 게임')
  await page.getByLabel('URL 식별자').fill('new-game')
  await page.getByRole('button',{name:'+ 게임 모드 추가',exact:true}).click()
  await page.getByRole('button',{name:'게임 등록하기 →'}).click()
  await expect(page.locator('[name="options[0].displayName"]')).toBeFocused()
  await expect(page.locator('[name="options[0].displayName"]')).toHaveAccessibleDescription('표시 이름을 입력해 주세요.')
  await page.locator('[name="options[0].displayName"]').fill('일반전')
  await expect(page.locator('[name="options[0].displayName"]')).toBeFocused()
})
