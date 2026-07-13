const assert = require('node:assert/strict')
const test = require('node:test')

test('notifies listeners when the socket disconnects', async () => {
  const callbacks = {}
  global.wx = {
    request: ({ success }) => success({
      statusCode: 200,
      data: { code: 'OK', data: { ticket: 'ticket-1' } },
    }),
    connectSocket: () => {
      const task = {
        onOpen: (callback) => { callbacks.open = callback },
        onMessage: (callback) => { callbacks.message = callback },
        onError: (callback) => { callbacks.error = callback },
        onClose: (callback) => { callbacks.close = callback },
        send: () => {},
        close: () => {},
      }
      setImmediate(() => callbacks.open())
      return task
    },
  }

  const { session } = require('../.test-dist/utils/session.js')
  const { realtimeSocket } = require('../.test-dist/utils/socket.js')
  realtimeSocket.close()
  realtimeSocket.connecting = null
  session.accessToken = 'access-token'
  session.user = { id: '100' }
  const events = []
  const unsubscribe = realtimeSocket.subscribe((event) => events.push(event.type))

  await realtimeSocket.connect()
  callbacks.close()
  callbacks.message({ data: '{"type":"MESSAGE"}' })

  unsubscribe()
  realtimeSocket.close()
  session.accessToken = ''
  session.user = null
  assert.deepEqual(events, ['DISCONNECTED'])
})

test('clears a failed socket so connect can try again', async () => {
  const callbacks = []
  let connections = 0
  global.wx = {
    request: ({ success }) => success({
      statusCode: 200,
      data: { code: 'OK', data: { ticket: `ticket-${connections + 1}` } },
    }),
    connectSocket: () => {
      const current = {}
      callbacks.push(current)
      connections += 1
      const task = {
        onOpen: (callback) => { current.open = callback },
        onMessage: (callback) => { current.message = callback },
        onError: (callback) => { current.error = callback },
        onClose: (callback) => { current.close = callback },
        send: () => {},
        close: () => {},
      }
      setImmediate(() => connections === 1
        ? current.error({ errMsg: 'network failed' })
        : current.open())
      return task
    },
  }

  const { session } = require('../.test-dist/utils/session.js')
  const { realtimeSocket } = require('../.test-dist/utils/socket.js')
  realtimeSocket.close()
  realtimeSocket.connecting = null
  session.accessToken = 'access-token'
  session.user = { id: '100' }

  await assert.rejects(realtimeSocket.connect(), /network failed/)
  await realtimeSocket.connect()
  const attempts = connections

  realtimeSocket.close()
  session.accessToken = ''
  session.user = null
  assert.equal(attempts, 2)
})

test('manual close clears the reconnect timer for the next connection', async () => {
  const callbacks = []
  const scheduled = []
  const originalSetTimeout = global.setTimeout
  const originalClearTimeout = global.clearTimeout
  global.setTimeout = (callback) => {
    scheduled.push(callback)
    return scheduled.length
  }
  global.clearTimeout = () => {}
  global.wx = {
    request: ({ success }) => success({
      statusCode: 200,
      data: { code: 'OK', data: { ticket: `ticket-${callbacks.length + 1}` } },
    }),
    connectSocket: () => {
      const current = {}
      callbacks.push(current)
      const task = {
        onOpen: (callback) => { current.open = callback },
        onMessage: (callback) => { current.message = callback },
        onError: (callback) => { current.error = callback },
        onClose: (callback) => { current.close = callback },
        send: () => {},
        close: () => {},
      }
      setImmediate(() => current.open())
      return task
    },
  }

  const { session } = require('../.test-dist/utils/session.js')
  const { realtimeSocket } = require('../.test-dist/utils/socket.js')
  session.accessToken = 'access-token'
  session.user = { id: '100' }

  await realtimeSocket.connect()
  callbacks[0].close()
  realtimeSocket.close()
  await realtimeSocket.connect()
  callbacks[1].close()
  const scheduledCount = scheduled.length

  realtimeSocket.close()
  session.accessToken = ''
  session.user = null
  global.setTimeout = originalSetTimeout
  global.clearTimeout = originalClearTimeout
  assert.equal(scheduledCount, 2)
})

test('replaces a connecting task after close without stale callbacks clearing the new promise', async () => {
  const callbacks = []
  let connections = 0
  global.wx = {
    request: ({ success }) => success({
      statusCode: 200,
      data: { code: 'OK', data: { ticket: `ticket-${connections + 1}` } },
    }),
    connectSocket: () => {
      const current = {}
      callbacks.push(current)
      connections += 1
      const task = {
        onOpen: (callback) => { current.open = callback },
        onMessage: (callback) => { current.message = callback },
        onError: (callback) => { current.error = callback },
        onClose: (callback) => { current.close = callback },
        send: () => {},
        close: () => {},
      }
      return task
    },
  }

  const { session } = require('../.test-dist/utils/session.js')
  const { realtimeSocket } = require('../.test-dist/utils/socket.js')
  realtimeSocket.close()
  realtimeSocket.connecting = null
  session.accessToken = 'access-token'
  session.user = { id: '100' }

  const first = realtimeSocket.connect().catch(() => undefined)
  await new Promise((resolve) => setImmediate(resolve))
  realtimeSocket.close()
  const second = realtimeSocket.connect()
  await new Promise((resolve) => setImmediate(resolve))
  assert.equal(connections, 2)

  let thirdResolved = false
  const third = realtimeSocket.connect().then(() => { thirdResolved = true })
  callbacks[0].open()
  await first
  await Promise.resolve()
  assert.equal(thirdResolved, false)
  callbacks[1].open()
  await Promise.all([second, third])

  realtimeSocket.close()
  session.accessToken = ''
  session.user = null
})

test('ignores a ticket response from a connection invalidated by close', async () => {
  const ticketResolvers = []
  const callbacks = []
  let connections = 0
  global.wx = {
    request: ({ success }) => { ticketResolvers.push(success) },
    connectSocket: () => {
      const current = {}
      callbacks.push(current)
      connections += 1
      return {
        onOpen: (callback) => { current.open = callback },
        onMessage: (callback) => { current.message = callback },
        onError: (callback) => { current.error = callback },
        onClose: (callback) => { current.close = callback },
        send: () => {},
        close: () => {},
      }
    },
  }

  const { session } = require('../.test-dist/utils/session.js')
  const { realtimeSocket } = require('../.test-dist/utils/socket.js')
  realtimeSocket.close()
  realtimeSocket.connecting = null
  session.accessToken = 'access-token'
  session.user = { id: '100' }

  const first = realtimeSocket.connect()
  await new Promise((resolve) => setImmediate(resolve))
  realtimeSocket.close()
  const second = realtimeSocket.connect()
  await new Promise((resolve) => setImmediate(resolve))
  assert.equal(ticketResolvers.length, 2)
  ticketResolvers[1]({ statusCode: 200, data: { code: 'OK', data: { ticket: 'ticket-new' } } })
  await new Promise((resolve) => setImmediate(resolve))
  assert.equal(connections, 1)
  callbacks[0].open()
  await second

  ticketResolvers[0]({ statusCode: 200, data: { code: 'OK', data: { ticket: 'ticket-old' } } })
  await first
  await new Promise((resolve) => setImmediate(resolve))
  assert.equal(connections, 1)

  realtimeSocket.close()
  session.accessToken = ''
  session.user = null
})

test('schedules reconnect when the websocket ticket request fails', async () => {
  const scheduled = []
  const originalSetTimeout = global.setTimeout
  const originalClearTimeout = global.clearTimeout
  global.setTimeout = (callback) => {
    scheduled.push(callback)
    return scheduled.length
  }
  global.clearTimeout = () => {}
  global.wx = {
    request: ({ fail }) => fail({ errMsg: 'ticket network failed' }),
  }

  const { session } = require('../.test-dist/utils/session.js')
  const { realtimeSocket } = require('../.test-dist/utils/socket.js')
  realtimeSocket.close()
  realtimeSocket.connecting = null
  session.accessToken = 'access-token'
  session.refreshToken = ''
  session.user = { id: '100' }

  await assert.rejects(realtimeSocket.connect(), /ticket network failed/)
  const scheduledCount = scheduled.length

  realtimeSocket.close()
  session.accessToken = ''
  session.user = null
  global.setTimeout = originalSetTimeout
  global.clearTimeout = originalClearTimeout
  assert.equal(scheduledCount, 1)
})
