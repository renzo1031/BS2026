import { config, shallowMount } from '@vue/test-utils'
import ActivityReviewDrawer from './ActivityReviewDrawer.vue'
import type { ActivityView } from '@/types/api'

config.global.renderStubDefaultSlot = true

const activity: ActivityView = {
  id: '1',
  campusId: '1',
  creatorId: '2',
  sceneName: '自习',
  title: '周末图书馆自习',
  description: '一起学习',
  meetingMode: 'OFFLINE',
  joinQuestions: [],
  startAt: '2026-07-13T08:00:00Z',
  endAt: '2026-07-13T10:00:00Z',
  applyDeadline: '2026-07-12T10:00:00Z',
  capacity: 4,
  acceptedCount: 1,
  reviewStatus: 'APPROVED',
  lifecycleStatus: 'RECRUITING',
  moderationStatus: 'NORMAL',
  version: 1,
  tags: [],
  mediaIds: [],
  createdAt: '2026-07-11T08:00:00Z',
}

function render(readonly: boolean) {
  return shallowMount(ActivityReviewDrawer, {
    props: { modelValue: true, activity, queueRows: [activity], busy: false, readonly },
  })
}

test('只读模式不渲染任何审核操作', () => {
  expect(render(true).text()).not.toMatch(/认领任务|处理决定|提交决策/)
  expect(render(false).text()).toContain('认领任务')
})
