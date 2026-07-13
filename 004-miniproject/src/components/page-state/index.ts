Component({
  properties: {
    state: { type: String, value: 'ready' },
    title: { type: String, value: '' },
    message: { type: String, value: '' },
    actionText: { type: String, value: '重试' },
  },
  methods: {
    onAction() {
      this.triggerEvent('action')
    },
  },
})
