const assert = require('node:assert/strict')
const test = require('node:test')

const { mergeMessagesById } = require('../.test-dist/utils/messages.js')

test('merges reconnect history without duplicates and keeps snowflake id order', () => {
  const current = [
    { id: '2076296436532670463', content: 'old' },
    { id: '2076296436532670465', content: 'live' },
  ]
  const missed = [
    { id: '2076296436532670464', content: 'missed' },
    { id: '2076296436532670465', content: 'duplicate' },
  ]

  assert.deepEqual(
    mergeMessagesById(current, missed).map((item) => item.id),
    ['2076296436532670463', '2076296436532670464', '2076296436532670465'],
  )
})
