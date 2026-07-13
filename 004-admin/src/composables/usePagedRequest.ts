import { computed, shallowReadonly, shallowRef } from 'vue'
import { errorMessage } from '@/api/http'
import type { PageResult } from '@/types/api'

export function usePagedRequest<T>(loader: (page: number, size: number) => Promise<PageResult<T>>, initialSize = 10) {
  const records = shallowRef<T[]>([])
  const page = shallowRef(1)
  const size = shallowRef(initialSize)
  const total = shallowRef(0)
  const loading = shallowRef(false)
  const error = shallowRef('')
  let requestSequence = 0

  const empty = computed(() => !loading.value && !error.value && records.value.length === 0)

  async function load() {
    const sequence = ++requestSequence
    loading.value = true
    error.value = ''
    try {
      const result = await loader(page.value, size.value)
      if (sequence !== requestSequence) return
      records.value = result.records
      total.value = Number(result.total)
    } catch (cause) {
      if (sequence === requestSequence) error.value = errorMessage(cause)
    } finally {
      if (sequence === requestSequence) loading.value = false
    }
  }

  function changePage(value: number) {
    page.value = value
    void load()
  }

  function changeSize(value: number) {
    size.value = value
    page.value = 1
    void load()
  }

  return {
    records: shallowReadonly(records),
    page: shallowReadonly(page),
    size: shallowReadonly(size),
    total: shallowReadonly(total),
    loading: shallowReadonly(loading),
    error: shallowReadonly(error),
    empty,
    load,
    changePage,
    changeSize,
  }
}
