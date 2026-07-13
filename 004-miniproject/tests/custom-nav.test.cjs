const assert = require('node:assert/strict')
const test = require('node:test')

const { calculateCustomNavLayout } = require('../.test-dist/utils/custom-nav.js')

test('keeps custom title content below the status bar and capsule', () => {
  const layout = calculateCustomNavLayout(375, 20, {
    top: 24,
    bottom: 56,
    left: 307,
    width: 68,
    height: 32,
  })

  assert.equal(layout.navBarHeight, 40)
  assert.equal(layout.totalHeight, 60)
  assert.equal(layout.rightInset, 76)
})

test('falls back to a stable compact bar when system metrics are unavailable', () => {
  const layout = calculateCustomNavLayout(0, -1, { top: 0, bottom: 0, left: 0, width: 0, height: 0 })

  assert.equal(layout.totalHeight, 44)
  assert.equal(layout.rightInset, 16)
})
