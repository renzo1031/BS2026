export function mergeMessagesById<T extends { id: string }>(current: T[], incoming: T[]) {
  const byId = new Map<string, T>()
  for (const message of [...current, ...incoming]) {
    if (!byId.has(message.id)) byId.set(message.id, message)
  }
  return [...byId.values()].sort((left, right) => {
    if (left.id.length !== right.id.length) return left.id.length - right.id.length
    return left.id < right.id ? -1 : left.id > right.id ? 1 : 0
  })
}
