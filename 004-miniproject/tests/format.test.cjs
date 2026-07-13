const assert = require('node:assert/strict')
const test = require('node:test')

const { formatDateTime, queryString, statusLabel } = require('../.test-dist/utils/format.js')

test('formats valid date time and preserves invalid value', () => {
  assert.match(formatDateTime('2026-07-12T09:24:00Z'), /^2026-07-12 \d{2}:24$/)
  assert.equal(formatDateTime('not-a-date'), 'not-a-date')
  assert.equal(formatDateTime(), '未提供')
})

test('builds query string without empty values', () => {
  assert.equal(queryString({ page: 1, keyword: '', mode: 'OFFLINE', active: true }), '?page=1&mode=OFFLINE&active=true')
})

test('maps known status and falls back to raw value', () => {
  assert.equal(statusLabel('APPROVED'), '已通过')
  assert.equal(statusLabel('EXPIRED'), '已过期')
  assert.equal(statusLabel('CUSTOM'), 'CUSTOM')
  assert.equal(statusLabel(), '未知状态')
})
