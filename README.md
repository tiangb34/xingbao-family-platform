# 星宝家庭互助平台 V1.0

依据《星宝家庭互助平台 V1.0 轻量化概要设计文档（修订版）》实现的可运行后端 MVP。项目采用 Spring Boot 3 + H2（本地文件库）以便直接启动验证；生产环境可替换为 MySQL、Redis、对象存储和真实鉴权服务。

## 启动

```bash
mvn spring-boot:run
```

服务默认监听 `http://localhost:8088`，H2 控制台为 `http://localhost:8088/h2-console`。

## 前端入口

- `admin-web/`：管理端静态 Web 应用，支持运营概览、待审核内容处理和账号状态调整。
- `mobile-web/`：手机优先的 H5 APP 原型，支持档案、打卡、知识投稿、社区、二手互助和工具浏览。

启动后端后可直接访问（推荐）：

```text
管理端：http://localhost:8088/admin/
APP 端：http://localhost:8088/app/
```

也可在前端目录中单独启动静态预览：

```bash
cd admin-web && python3 -m http.server 5174
# 或
cd mobile-web && python3 -m http.server 5175
```

分别打开 `http://localhost:5174` 和 `http://localhost:5175`。两端默认连接 `http://localhost:8088/api/v1`。

## 已实现 API

| 模块 | 路径 | 功能 |
|---|---|---|
| 认证 | `/api/v1/auth` | 手机号注册、登录、隐私与敏感信息同意记录 |
| 星宝档案 | `/api/v1/children` | 多子女档案、敏感授权校验、逻辑删除 |
| 成长记录 | `/api/v1/children/{id}/records` | 三类打卡记录、趋势汇总 |
| 知识内容 | `/api/v1/contents` | 来源/许可必填、待审流转、公开列表 |
| 社区 | `/api/v1/community/posts` | 发帖、匿名标记、风控分级、删除 |
| 二手互助 | `/api/v1/second-hand` | 信息发布、风控、线下自主置换声明 |
| 工具资源 | `/api/v1/tools` | 量表说明、政策、提示卡、情绪手册资源 |
| 后台 | `/api/v1/admin` | 审核队列、审核动作、账号状态、基础统计 |

## 重要边界

- AI 风控为规则化占位实现，生产须接入受评估模型及人工审核 SOP。
- H2、开发令牌、明文 `diagnosisCiphertext` 字段仅用于本地 MVP；生产必须使用真实身份认证、KMS/字段加密、私有对象存储和审计日志。
- 平台不提供线上交易、支付、订单、担保、物流或售后。

详见 [功能进度](docs/FUNCTION_PROGRESS.md)、[架构说明](docs/PROJECT_CONTEXT.md)。
