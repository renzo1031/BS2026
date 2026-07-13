import { session } from './session'

export function ensureAuthenticated() {
  if (session.authenticated) return true
  wx.reLaunch({ url: '/pages/login/index' })
  return false
}

export function ensureVerified() {
  if (!ensureAuthenticated()) return false
  if (session.user?.verificationStatus === 'APPROVED') return true
  wx.reLaunch({ url: '/pages/campus-bind/index' })
  return false
}

export function routeAfterLogin() {
  if (session.user?.verificationStatus === 'APPROVED') {
    wx.switchTab({ url: '/pages/discover/index' })
  } else {
    wx.reLaunch({ url: '/pages/campus-bind/index' })
  }
}
