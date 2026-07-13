const assert = require('node:assert/strict')
const test = require('node:test')

let page
global.Page = (definition) => { page = definition }

const calls = {
  login: 0,
  code: '',
  route: '',
}

function mockWx(envVersion) {
  Object.assign(calls, { login: 0, code: '', route: '' })
  global.wx = {
    getAccountInfoSync: () => ({ miniProgram: { envVersion } }),
    login: ({ success }) => {
      calls.login += 1
      success({ code: 'ephemeral-wx-code' })
    },
    request: ({ data, success }) => {
      calls.code = data.code
      success({
        statusCode: 200,
        data: {
          code: 'OK',
          data: {
            accessToken: 'access-token',
            refreshToken: 'refresh-token',
            expiresAt: '0',
            user: {
              id: '100',
              campusId: '1',
              nickname: '同学甲',
              role: 'STUDENT',
              status: 'ACTIVE',
              verificationStatus: 'APPROVED',
            },
          },
        },
      })
    },
    setStorageSync: () => {},
    switchTab: ({ url }) => { calls.route = url },
    reLaunch: ({ url }) => { calls.route = url },
    showToast: () => {},
  }
}

mockWx('develop')
require('../.test-dist/pages/login/index.js')

test('development login uses the stable approved test identity', async () => {
  mockWx('develop')
  const context = {
    data: { ...page.data, agreed: true },
    setData(values) { Object.assign(this.data, values) },
  }

  await page.login.call(context)

  assert.equal(calls.code, 'student-a')
  assert.equal(calls.login, 0)
  assert.equal(calls.route, '/pages/discover/index')
})

test('release login exchanges a fresh WeChat code', async () => {
  mockWx('release')
  const context = {
    data: { ...page.data, agreed: true },
    setData(values) { Object.assign(this.data, values) },
  }

  await page.login.call(context)

  assert.equal(calls.code, 'ephemeral-wx-code')
  assert.equal(calls.login, 1)
  assert.equal(calls.route, '/pages/discover/index')
})
