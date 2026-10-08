const app = getApp();
// 开发者工具可临时改为局域网地址；正式版必须替换为已备案 HTTPS 域名。
const BASE_URL = 'http://192.168.10.74:8088/api/v1';
function request(path, options = {}) {
  return new Promise((resolve, reject) => wx.request({url: BASE_URL + path, method: options.method || 'GET', data: options.data, header: {'content-type':'application/json'}, success: result => { const body = result.data || {}; if (result.statusCode >= 200 && result.statusCode < 300 && body.code === 200) resolve(body.data); else reject(new Error(body.message || '请求失败')); }, fail: () => reject(new Error('网络连接失败，请稍后重试'))}));
}
function upload(filePath) {
  return new Promise((resolve, reject) => wx.uploadFile({url: BASE_URL + '/files', filePath, name:'file', success: response => { try { const body = JSON.parse(response.data); if (body.code === 200) resolve(absoluteUrl(body.data.url)); else reject(new Error(body.message || '上传失败')); } catch (e) { reject(new Error('上传响应异常')); } }, fail: () => reject(new Error('图片上传失败'))}));
}
function absoluteUrl(url) { return /^https?:/.test(url) ? url : BASE_URL.replace('/api/v1','') + url; }
function user() { return app.globalData.user; }
function ensureUser() { return app.requireUser(); }
function notify(error) { wx.showToast({title:error.message || '操作失败', icon:'none'}); }
module.exports = {request, upload, absoluteUrl, user, ensureUser, notify};
