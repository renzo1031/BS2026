import type { CampusView, IdentityView, Tokens } from '../../types/api'
import { ensureAuthenticated, routeAfterLogin } from '../../utils/navigation'
import { request, showError, uploadImage } from '../../utils/request'
import { session } from '../../utils/session'

Page({
  data: {
    state: 'loading',
    campuses: [] as CampusView[],
    campusNames: [] as string[],
    campusIndex: 0,
    identifier: '',
    proofPath: '',
    binding: null as IdentityView | null,
    submitting: false,
  },

  onLoad() {
    if (ensureAuthenticated()) void this.load()
  },

  async load() {
    this.setData({ state: 'loading' })
    try {
      const [campuses, binding] = await Promise.all([
        request<CampusView[]>({ path: '/campuses' }),
        request<IdentityView | null>({ path: '/me/identity-bindings/current' }),
      ])
      if (binding?.status === 'APPROVED') {
        session.updateUser({ ...session.user!, campusId: binding.campusId, verificationStatus: 'APPROVED' })
        routeAfterLogin()
        return
      }
      this.setData({ campuses, campusNames: campuses.map((item) => item.name), binding, state: 'ready' })
    } catch (error) {
      this.setData({ state: 'error' })
      showError(error)
    }
  },

  onCampusChange(event: WechatMiniprogram.PickerChange) {
    this.setData({ campusIndex: Number(event.detail.value) })
  },

  onIdentifierInput(event: WechatMiniprogram.Input) {
    this.setData({ identifier: event.detail.value.trim() })
  },

  async chooseProof() {
    try {
      const result = await wx.chooseImage({ count: 1, sourceType: ['album', 'camera'] })
      const file = result.tempFiles[0]
      if (file) this.setData({ proofPath: file.path })
    } catch (error) {
      if ((error as { errMsg?: string }).errMsg?.includes('cancel')) return
      showError(error)
    }
  },

  async submit() {
    const campus = this.data.campuses[this.data.campusIndex]
    if (!campus || !this.data.identifier || !this.data.proofPath) {
      wx.showToast({ title: '请完整填写并上传证明', icon: 'none' })
      return
    }
    this.setData({ submitting: true })
    try {
      const file = await uploadImage(this.data.proofPath, 'IDENTITY_PROOF', campus.id)
      const binding = await request<IdentityView>({
        path: '/me/identity-bindings',
        method: 'POST',
        data: {
          campusId: campus.id,
          // identityLabel 是学校配置的展示文案；接口字段使用稳定机器值。
          identifierType: 'STUDENT_ID',
          identifier: this.data.identifier,
          proofFileId: file.id,
        },
      })
      session.updateUser({ ...session.user!, campusId: campus.id, verificationStatus: 'PENDING' })
      this.setData({ binding, proofPath: '' })
      wx.showToast({ title: '已提交审核', icon: 'success' })
    } catch (error) {
      showError(error)
    } finally {
      this.setData({ submitting: false })
    }
  },

  relogin() {
    session.clear()
    wx.reLaunch({ url: '/pages/login/index' })
  },
})
