import type { Directive } from 'vue'
import { useAuthStore } from '@/stores/auth'
import type { UserRole } from '@/types/api'

export const permissionDirective: Directive<HTMLElement, UserRole[]> = {
  mounted(element, binding) {
    if (!useAuthStore().can(binding.value)) element.hidden = true
  },
  updated(element, binding) {
    element.hidden = !useAuthStore().can(binding.value)
  },
}
