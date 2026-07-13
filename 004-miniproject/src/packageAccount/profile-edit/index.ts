import type { FileView, UserView } from '../../types/api'
import { ensureAuthenticated } from '../../utils/navigation'
import { ApiError, request, showError, uploadImage } from '../../utils/request'
import { session } from '../../utils/session'
import { avatarReviewState, type AvatarReviewState } from '../../utils/avatar'

const AVATAR_PENDING_KEY_PREFIX = 'campusBuddy.avatarPending.'

function avatarPendingKey(userId: string) {
  return `${AVATAR_PENDING_KEY_PREFIX}${userId}`
}

function readTags(value?: string | null) {
  if (!value) return []
  try {
    const parsed: unknown = JSON.parse(value)
    return Array.isArray(parsed) ? parsed.filter((item): item is string => typeof item === 'string') : []
  } catch {
    return []
  }
}

Page({
  data: {
    state: 'loading',
    nickname: '',
    bio: '',
    gradeName: '',
    majorName: '',
    interestTagsText: '',
    tagsPreview: [] as string[],
    tagsCount: 0,
    userId: '',
    canUploadAvatar: false,
    avatarCurrentUrl: '',
    avatarPreviewUrl: '',
    pendingAvatarFileId: '',
    avatarStatus: 'none' as AvatarReviewState,
    avatarHint: '点击头像上传，审核通过后生效',
    avatarUploading: false,
    avatarRefreshing: false,
    saving: false,
  },

  onLoad() {
    if (ensureAuthenticated()) void this.load()
  },

  onPullDownRefresh() {
    void this.load().finally(() => wx.stopPullDownRefresh())
  },

  async load() {
    if (!ensureAuthenticated()) return
    this.setData({ state: 'loading' })
    try {
      const user = await request<UserView>({ path: '/me' })
      const currentAvatarUrl = await this.avatarUrl(user.avatarFileId)
      const pendingAvatarFileId = wx.getStorageSync<string>(avatarPendingKey(user.id)) || ''
      this.setData({
        userId: user.id,
        nickname: user.nickname || '',
        bio: user.bio || '',
        gradeName: user.gradeName || '',
        majorName: user.majorName || '',
        interestTagsText: readTags(user.interestTagsJson).join('，'),
        tagsPreview: readTags(user.interestTagsJson).slice(0, 6),
        tagsCount: readTags(user.interestTagsJson).length,
        canUploadAvatar: Boolean(user.campusId && user.verificationStatus === 'APPROVED'),
        avatarCurrentUrl: currentAvatarUrl,
        avatarPreviewUrl: currentAvatarUrl,
        pendingAvatarFileId,
        avatarStatus: pendingAvatarFileId ? 'pending' : 'none',
        avatarHint: pendingAvatarFileId
          ? '新头像审核中，审核通过后回到本页即可启用'
          : user.campusId && user.verificationStatus === 'APPROVED' ? '点击头像上传，审核通过后生效' : '完成校园认证后才能设置头像',
        state: 'ready',
      })
      if (pendingAvatarFileId) await this.checkPendingAvatar(true)
    } catch (error) {
      this.setData({ state: 'error' })
      showError(error)
    }
  },

  onInput(event: WechatMiniprogram.Input) {
    const field = String(event.currentTarget.dataset.field)
    const value = event.detail.value
    if (field === 'interestTagsText') {
      const tags = [...new Set(value.split(/[，,\n]/).map((item) => item.trim()).filter(Boolean))]
      this.setData({ interestTagsText: value, tagsPreview: tags.slice(0, 6), tagsCount: tags.length })
      return
    }
    this.setData({ [field]: value })
  },

  async avatarUrl(fileId?: string | null) {
    if (!fileId) return ''
    try { return (await request<{ url: string }>({ path: `/files/${fileId}/url` })).url } catch { return '' }
  },

  async chooseAvatar() {
    if (!this.data.canUploadAvatar) {
      wx.showToast({ title: '完成校园认证后才能设置头像', icon: 'none' })
      return
    }
    if (this.data.avatarUploading || this.data.avatarRefreshing || this.data.saving) return
    if (this.data.pendingAvatarFileId) {
      wx.showToast({ title: '请先处理当前头像审核', icon: 'none' })
      return
    }
    try {
      const result = await wx.chooseImage({ count: 1, sourceType: ['album', 'camera'], sizeType: ['compressed'] })
      const file = result.tempFiles[0]
      if (!file) return
      if (file.size && file.size > 2 * 1024 * 1024) {
        wx.showToast({ title: '头像图片不能超过 2MB', icon: 'none' })
        return
      }
      this.setData({ avatarUploading: true, avatarPreviewUrl: file.path })
      const uploaded = await uploadImage(file.path, 'AVATAR')
      wx.setStorageSync(avatarPendingKey(this.data.userId), uploaded.id)
      const state = avatarReviewState(uploaded.status)
      this.setData({
        pendingAvatarFileId: uploaded.id,
        avatarStatus: state,
        avatarHint: state === 'approved' ? '审核通过，正在启用' : '新头像审核中，审核通过后回到本页即可启用',
      })
      if (state === 'approved') {
        this.setData({ avatarUploading: false })
        await this.checkPendingAvatar(true)
      } else if (state === 'rejected') {
        wx.removeStorageSync(avatarPendingKey(this.data.userId))
        this.setData({ pendingAvatarFileId: '', avatarStatus: 'rejected', avatarPreviewUrl: this.data.avatarCurrentUrl, avatarHint: '头像审核未通过，请重新选择' })
        wx.showToast({ title: '头像审核未通过', icon: 'none' })
      } else wx.showToast({ title: '头像已上传，等待审核', icon: 'none' })
    } catch (error) {
      this.setData({ avatarPreviewUrl: this.data.avatarCurrentUrl, avatarStatus: 'none' })
      showError(error)
    } finally {
      this.setData({ avatarUploading: false })
    }
  },

  refreshAvatar() { void this.checkPendingAvatar(false) },

  onAvatarImageError() {
    this.setData({ avatarPreviewUrl: this.data.avatarPreviewUrl === this.data.avatarCurrentUrl ? '' : this.data.avatarCurrentUrl })
  },

  async checkPendingAvatar(silent = false) {
    const fileId = this.data.pendingAvatarFileId
    if (!fileId || this.data.avatarRefreshing || this.data.avatarUploading || this.data.saving) return
    this.setData({ avatarRefreshing: true })
    try {
      const file = await request<FileView>({ path: `/files/${fileId}` })
      const state = avatarReviewState(file.status)
      if (state === 'approved') {
        const user = await request<UserView>({ path: '/me/profile', method: 'PUT', data: { avatarFileId: fileId } })
        session.updateUser(user)
        const url = await this.avatarUrl(user.avatarFileId)
        wx.removeStorageSync(avatarPendingKey(user.id))
        this.setData({
          pendingAvatarFileId: '', avatarCurrentUrl: url, avatarPreviewUrl: url,
          avatarStatus: 'none', avatarHint: '头像已启用，点击头像可更换',
        })
        wx.showToast({ title: '头像已启用', icon: 'success' })
        return
      }
      if (state === 'rejected') {
        const reason = file.scanResult?.startsWith('REJECTED:') ? file.scanResult.slice('REJECTED:'.length).trim() : ''
        wx.removeStorageSync(avatarPendingKey(this.data.userId))
        this.setData({
          pendingAvatarFileId: '', avatarPreviewUrl: this.data.avatarCurrentUrl,
          avatarStatus: 'rejected', avatarHint: reason ? `头像审核未通过：${reason}` : '头像审核未通过，请重新选择',
        })
        if (!silent) wx.showToast({ title: '头像审核未通过', icon: 'none' })
        return
      }
      const url = await this.avatarUrl(fileId)
      this.setData({ avatarStatus: 'pending', avatarPreviewUrl: url || this.data.avatarPreviewUrl, avatarHint: '新头像审核中，审核通过后回到本页即可启用' })
      if (!silent) wx.showToast({ title: '头像仍在审核中', icon: 'none' })
    } catch (error) {
      if (error instanceof ApiError && error.statusCode === 404) {
        wx.removeStorageSync(avatarPendingKey(this.data.userId))
        this.setData({
          pendingAvatarFileId: '', avatarPreviewUrl: this.data.avatarCurrentUrl,
          avatarStatus: 'none', avatarHint: this.data.canUploadAvatar ? '点击头像上传，审核通过后生效' : '完成校园认证后才能设置头像',
        })
        return
      }
      if (!silent) showError(error)
    } finally {
      this.setData({ avatarRefreshing: false })
    }
  },

  async save() {
    if (this.data.avatarUploading || this.data.avatarRefreshing) return
    const nickname = this.data.nickname.trim()
    const bio = this.data.bio.trim()
    const gradeName = this.data.gradeName.trim()
    const majorName = this.data.majorName.trim()
    const tags = [...new Set(this.data.interestTagsText
      .split(/[，,\n]/)
      .map((item) => item.trim())
      .filter(Boolean))]
    const interestTagsJson = JSON.stringify(tags)

    if (!nickname || Array.from(nickname).length > 40) {
      wx.showToast({ title: '昵称须为 1 到 40 字', icon: 'none' })
      return
    }
    if (Array.from(bio).length > 300 || Array.from(gradeName).length > 32 || Array.from(majorName).length > 80) {
      wx.showToast({ title: '简介、年级或专业内容过长', icon: 'none' })
      return
    }
    if (interestTagsJson.length > 500) {
      wx.showToast({ title: '兴趣标签内容过多', icon: 'none' })
      return
    }

    this.setData({ saving: true })
    try {
      const user = await request<UserView>({
        path: '/me/profile',
        method: 'PUT',
        data: { nickname, bio, gradeName, majorName, interestTagsJson },
      })
      session.updateUser(user)
      wx.showToast({ title: '资料已保存', icon: 'success' })
      setTimeout(() => wx.navigateBack(), 600)
    } catch (error) {
      showError(error)
    } finally {
      this.setData({ saving: false })
    }
  },
})
