# 星宝家庭互助微信小程序（用户端）

本目录仅包含家长使用的小程序；`admin-web` 管理台继续保持网页端。

## 本地调试

1. 先启动后端：`mvn spring-boot:run`。
2. 打开微信开发者工具，导入本目录 `miniprogram`。
3. 在 `utils/api.js` 设置开发环境 API 地址，例如本机局域网地址：
   `http://192.168.10.74:8088/api/v1`。
4. 仅本地调试时，可在开发者工具中暂时关闭“校验合法域名、Web-view（业务域名）、TLS 版本以及 HTTPS 证书”。

## 正式发布前必须替换

- `project.config.json` 中的 `appid`。
- `utils/api.js` 中的地址：替换为已备案的 HTTPS API 域名，例如 `https://api.example.com/api/v1`。
- 微信公众平台「开发管理 → 开发设置」中的 request、uploadFile、downloadFile 合法域名。
- 当前手机号开发登录：改为微信 `wx.login` + 后端 code 换取用户身份；AppSecret 只能保存在服务端。
- H2 与本地 `uploads`：替换为 MySQL 和对象存储。

## 已迁移页面

- 首页、档案创建/编辑、社区帖子/图片上传/点赞/评论、居家工具、成长学习、每日训练与视觉提示卡、我的/消息。

小程序端与现有 Spring Boot API 复用。管理端不需要启动或迁移为小程序。
