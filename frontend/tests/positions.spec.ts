import { expect, test } from '@playwright/test'
const token=`e30.${Buffer.from(JSON.stringify({sub:'7',exp:Math.floor(Date.now()/1000)+3600,roles:['MEMBER']})).toString('base64url')}.test`
const game={id:1,slug:'league-of-legends',name:'리그 오브 레전드',active:true}
const party={id:41,gameId:1,ownerId:1,title:'포지션 테스트 모임',startsAt:'2099-10-01T12:00:00Z',capacity:5,reservedCount:1,status:'OPEN',roleRequirements:{},mapOptionId:2}
const state={meetupId:41,participantCount:1,capacity:5,remainingCapacity:4,recruitmentStatus:'OPEN',participationStatus:'NOT_JOINED',canParticipate:true,canCancel:false,canClose:false,availableActions:['PARTICIPATE']}
async function login(page:import('@playwright/test').Page,path:string) {
  await page.goto('/account')
  await page.getByLabel('아이디',{exact:true}).fill('testuser')
  await page.getByLabel('비밀번호',{exact:true}).fill('password123')
  await page.getByRole('button',{name:'로그인 →',exact:true}).click()
  await expect(page.getByRole('heading',{name:'게임 계정 연결'})).toBeVisible()
  if(path!='/account') {
    await page.getByRole('link',{name:'← 모임 찾기',exact:true}).first().click()
    await page.getByRole('link',{name:/포지션 테스트 모임/}).click()
  }
}
test.beforeEach(async({page})=>{
  await page.route('**/api/v1/auth/login',r=>r.fulfill({json:{accessToken:token}}))
  await page.route('**/api/v1/games',r=>r.fulfill({json:[game]}))
  await page.route('**/api/v1/games/1',r=>r.fulfill({json:{game,options:[{id:3,type:'REGION',code:'KR',displayName:'대한민국',active:true},{id:2,type:'MAP',code:'SUMMONERS_RIFT',displayName:'소환사의 협곡',active:true,roleSelectionAllowed:true}]}}))
  await page.route('**/api/v1/members/me/riot',r=>r.fulfill({json:{enabled:false,linked:false,riotId:null}}))
  await page.route('**/api/v1/members/me/games/1/profile',r=>r.fulfill({json:{primaryPosition:'MID',secondaryPosition:'SUPPORT'}}))
  await page.route('**/api/v1/meetups?*',r=>r.fulfill({json:{items:[party],nextCursor:null,hasNext:false}}))
  await page.route('**/api/v1/meetups/41',r=>r.fulfill({json:party}))
  await page.route('**/api/v1/meetups/41/participation',r=>r.fulfill({json:state}))
  await page.route('**/api/v1/meetups/41/participation/preferences',r=>r.fulfill({json:{enabled:true,positions:{primaryPosition:'MID',secondaryPosition:'SUPPORT'}}}))
})
test('saves profile and prevents duplicate positions',async({page})=>{
  let saved:unknown
  await page.route('**/api/v1/members/me/games/1/profile',r=>{
    if(r.request().method()==='PUT')saved=r.request().postDataJSON()
    return r.fulfill({json:saved??{primaryPosition:'MID',secondaryPosition:'SUPPORT'}})
  })
  await login(page,'/account')
  await expect(page.getByLabel('주 포지션',{exact:true})).toHaveValue('MID')
  await page.getByLabel('주 포지션',{exact:true}).selectOption('SUPPORT')
  await expect(page.getByLabel('부 포지션',{exact:true})).toHaveValue('')
  await page.getByLabel('부 포지션',{exact:true}).selectOption('JUNGLE')
  await page.getByRole('button',{name:'포지션 저장'}).click()
  await expect(page.getByText('기본 포지션을 저장했습니다.')).toBeVisible()
  expect(saved).toEqual({primaryPosition:'SUPPORT',secondaryPosition:'JUNGLE'})
  expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true)
})
test('prefills join from profile and submits editable positions only to reservation',async({page})=>{
  let submitted:unknown
  let profileWrites=0
  await page.route('**/api/v1/members/me/games/1/profile',r=>{
    if(r.request().method()==='PUT')profileWrites++
    return r.fulfill({json:{primaryPosition:'MID',secondaryPosition:'SUPPORT'}})
  })
  await page.route('**/api/v1/meetups/41/reservations',r=>{
    submitted=r.request().postDataJSON()
    return r.fulfill({json:{...state,participantCount:2,participationStatus:'JOINED',canParticipate:false,canCancel:true}})
  })
  await login(page,'/meetups/41')
  await expect(page.getByLabel('주 포지션',{exact:true})).toHaveValue('MID')
  await expect(page.getByLabel('부 포지션',{exact:true})).toHaveValue('SUPPORT')
  await page.getByLabel('주 포지션',{exact:true}).selectOption('TOP')
  await page.getByRole('button',{name:'참여 신청하기',exact:true}).click()
  await expect(page.getByText('✓ 이 모임에 참여 중입니다.')).toBeVisible()
  expect(submitted).toEqual({primaryPosition:'TOP',secondaryPosition:'SUPPORT'})
  expect(profileWrites).toBe(0)
})
test('does not allow joining before defaults load and offers retry after failure',async({page})=>{
  await page.route('**/api/v1/meetups/41/participation/preferences',r=>r.fulfill({status:503,json:{detail:'프로필 조회 실패'}}))
  await login(page,'/meetups/41')
  await expect(page.getByRole('button',{name:'다시 시도',exact:true})).toBeVisible({timeout:15000})
  await expect(page.getByRole('button',{name:'참여 신청하기',exact:true})).toHaveCount(0)
  await page.route('**/api/v1/meetups/41/participation/preferences',r=>r.fulfill({json:{enabled:true,positions:{primaryPosition:null,secondaryPosition:null}}}))
  await page.getByRole('button',{name:'다시 시도',exact:true}).click()
  await expect(page.getByLabel('주 포지션',{exact:true})).toHaveValue('')
  await expect(page.getByLabel('부 포지션',{exact:true})).toBeDisabled()
  await expect(page.getByRole('button',{name:'참여 신청하기',exact:true})).toBeEnabled()
})
