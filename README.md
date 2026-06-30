# InternTaskHub

实习任务协作台 MVP，用于导师和实习生之间分配、跟踪日常任务，并把技术资讯与任务详情关联。

## 技术栈

- Backend: Java 17, Spring Boot 3.x, Spring Data JPA, H2
- Frontend: Vue 3, Vite, Element Plus, Axios, ECharts
- API: RESTful API
- Database script: MySQL-compatible `schema.sql`
- AI assistance: Codex + ChatGPT, usage notes in `docs/DESIGN.md`

## 项目结构

```text
InternTaskHub/
  backend/        Spring Boot REST API
  frontend/       Vue 3 task management UI
  docs/           Design document and delivery notes
  schema.sql      MySQL-compatible schema
```

## 启动后端

需要 JDK 17。

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

如果 Maven Wrapper 提示 `JAVA_HOME environment variable is not defined correctly`，但 `java -version` 正常，可以在当前 PowerShell 临时设置：

```powershell
$javaExe = (Get-Command java).Source
$env:JAVA_HOME = Split-Path (Split-Path $javaExe)
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd spring-boot:run
```

后端默认地址：`http://localhost:8080`

H2 控制台：`http://localhost:8080/h2-console`

- JDBC URL: `jdbc:h2:mem:interntaskhub`
- User Name: `sa`
- Password: 留空

## 启动前端

```powershell
cd frontend
npm install
npm run dev
```

前端默认地址：`http://localhost:5173`

如需修改 API 地址，可在前端目录创建 `.env.local`：

```text
VITE_API_BASE_URL=http://localhost:8080/api
```

## 演示账号

登录走后端 `/api/auth/mock-login` 接口，是用于 MVP 演示的 Mock 登录，但必须提交账号和密码并由后端校验。所有演示账号密码均为 `123456`。登录名使用拼音/英文，页面展示名使用中文。

| 登录名 | 密码 | 页面展示名 | 角色 | 说明 |
| --- | --- | --- | --- | --- |
| `daoshiA` | `123456` | 导师A | 导师 | 查看和管理全部任务 |
| `daoshiB` | `123456` | 导师B | 导师 | 查看和管理全部任务 |
| `xiaozhao` | `123456` | 实习生小赵 | 实习生 | 仅查看自己的任务 |
| `xiaoli` | `123456` | 实习生小李 | 实习生 | 仅查看自己的任务 |
| `xiaowang` | `123456` | 实习生小王 | 实习生 | 仅查看自己的任务 |
| `xiaoliu` | `123456` | 实习生小刘 | 实习生 | 仅查看自己的任务 |

后端启动时会通过 `DataInitializer` 自动创建以上 6 个账号和 12 条演示任务。
登录页中的演示账号卡片只会填入账号，不会自动填入密码，也不会绕过登录校验。

## 主要功能

- 任务 CRUD
- 任务状态流转：待办、进行中、已完成
- 按状态、负责人、截止日期、关键词筛选
- 卡片/表格视图切换
- 优先级颜色区分
- 截止日期提醒
- 导师/实习生角色可见范围
- ECharts 仪表盘
- 技术资讯刷新、搜索、任务详情关联资讯
- 个人设置：修改显示名、查看登录账号和角色、可选修改密码
- 任务列表导出 CSV

## 技术资讯来源

技术资讯模块会优先抓取中文技术 RSS，并保留英文技术 RSS 作为兜底：

- InfoQ 中文：`https://www.infoq.cn/feed/`
- OSChina 开源社区：`https://www.oschina.net/news/rss`
- Hacker News 技术快讯：`https://hnrss.org/newest?q=关键词`

后端会过滤技术关键词，并按发布时间倒序展示。英文源内容会生成中文化标题或中文摘要，避免页面呈现为纯英文资料列表。

## 主要 API

| Method | Path | Description |
| --- | --- | --- |
| POST | `/api/auth/mock-login` | Mock 登录，提交 `username` 和 `password` |
| GET | `/api/users` | 用户列表 |
| GET | `/api/tasks` | 任务列表与筛选 |
| POST | `/api/tasks` | 新建任务 |
| PUT | `/api/tasks/{id}` | 更新任务 |
| PATCH | `/api/tasks/{id}/status` | 更新任务状态 |
| DELETE | `/api/tasks/{id}` | 删除任务 |
| GET | `/api/dashboard/summary` | 仪表盘统计 |
| GET | `/api/news` | 资讯列表/搜索 |
| GET | `/api/news/related` | 按任务关键词获取相关资讯 |
| POST | `/api/news/refresh` | 刷新技术资讯 |
| PUT | `/api/users/me` | 更新显示名和可选新密码 |

前端会在请求头中携带 `X-User-Id`，后端据此做轻量角色可见性控制。
