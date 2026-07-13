export type MenuButtonRect = {
  top: number
  bottom: number
  left: number
  width: number
  height: number
}

export type CustomNavLayout = {
  statusBarHeight: number
  navBarHeight: number
  totalHeight: number
  rightInset: number
}

export function calculateCustomNavLayout(windowWidth: number, statusBarHeight: number, menu: MenuButtonRect): CustomNavLayout {
  const safeWidth = Number.isFinite(windowWidth) && windowWidth > 0 ? windowWidth : 375
  const safeStatus = Number.isFinite(statusBarHeight) && statusBarHeight >= 0 ? statusBarHeight : 0
  const hasMenu = menu.width > 0 && menu.height > 0 && menu.left > 0
  const gap = hasMenu ? Math.max(4, menu.top - safeStatus) : 4
  const navBarHeight = hasMenu ? menu.height + gap * 2 : 44
  const rightInset = hasMenu ? Math.max(16, safeWidth - menu.left + 8) : 16
  return {
    statusBarHeight: safeStatus,
    navBarHeight,
    totalHeight: Math.ceil(safeStatus + navBarHeight),
    rightInset: Math.ceil(rightInset),
  }
}

export function getCustomNavLayout(): CustomNavLayout {
  const windowInfo = wx.getWindowInfo()
  const menu = wx.getMenuButtonBoundingClientRect()
  return calculateCustomNavLayout(windowInfo.windowWidth, windowInfo.statusBarHeight, menu)
}
