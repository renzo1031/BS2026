const assert = require('node:assert/strict')
const test = require('node:test')

test('only an approved avatar can be bound', () => {
  let avatar
  assert.doesNotThrow(() => {
    avatar = require('../.test-dist/utils/avatar.js')
  })

  assert.equal(avatar.avatarReviewState('PENDING_SCAN'), 'pending')
  assert.equal(avatar.avatarReviewState('APPROVED'), 'approved')
  assert.equal(avatar.avatarReviewState('REJECTED'), 'rejected')
  assert.equal(avatar.avatarReviewState('DELETED'), 'rejected')
})
