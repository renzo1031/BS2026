import { getCustomNavLayout } from '../../utils/custom-nav'

Component({
  properties: {
    title: { type: String, value: '' },
  },
  data: {
    statusBarHeight: 0,
    navBarHeight: 44,
    totalHeight: 44,
    rightInset: 16,
  },
  lifetimes: {
    attached() {
      this.setData(getCustomNavLayout())
    },
  },
})
