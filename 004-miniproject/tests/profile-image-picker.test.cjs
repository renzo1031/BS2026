const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const test = require('node:test')

let page
const calls = { chooseImage: 0, pendingFileId: '' }

global.Page = (definition) => { page = definition }
global.wx = {
  chooseImage: async () => {
    calls.chooseImage += 1
    return { tempFiles: [{ path: 'wxfile://avatar.png', size: 1024 }] }
  },
  uploadFile: ({ success }) => success({
    statusCode: 200,
    data: JSON.stringify({ code: 'OK', data: { id: 'avatar-file-1', status: 'PENDING_SCAN' } }),
  }),
  getStorageSync: () => '',
  setStorageSync: (_, value) => { calls.pendingFileId = value },
  removeStorageSync: () => {},
  showToast: () => {},
}

require('../.test-dist/packageAccount/profile-edit/index.js')

test('avatar picker uses the image-only native selector', async () => {
  calls.chooseImage = 0
  calls.pendingFileId = ''
  const context = {
    data: {
      ...page.data,
      canUploadAvatar: true,
      userId: '100',
    },
    setData(values) { Object.assign(this.data, values) },
  }

  await page.chooseAvatar.call(context)

  assert.equal(calls.chooseImage, 1)
  assert.equal(calls.pendingFileId, 'avatar-file-1')
  assert.equal(context.data.pendingAvatarFileId, 'avatar-file-1')
})

test('every image picker avoids the media privacy API', () => {
  const files = [
    'src/packageAccount/profile-edit/index.ts',
    'src/packageBusiness/activity-editor/index.ts',
    'src/pages/campus-bind/index.ts',
  ]

  for (const file of files) {
    const source = fs.readFileSync(path.join(__dirname, '..', file), 'utf8')
    assert.match(source, /wx\.chooseImage\(/)
    assert.doesNotMatch(source, /wx\.chooseMedia\(/)
  }
})
