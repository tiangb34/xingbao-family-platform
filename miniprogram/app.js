App({
  globalData: { user: null },
  onLaunch() { const user = wx.getStorageSync('xingbao-user'); if (user) this.globalData.user = user; },
  setUser(user) { this.globalData.user = user; wx.setStorageSync('xingbao-user', user); },
  clearUser() { this.globalData.user = null; wx.removeStorageSync('xingbao-user'); },
  requireUser() { if (this.globalData.user) return this.globalData.user; wx.navigateTo({url:'/pages/auth/index'}); return null; }
});
