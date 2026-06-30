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

登录为 Mock 登录，无密码。

| 用户名 | 角色 | 说明 |
| --- | --- | --- |
| `mentor` | 导师 | 查看和管理全部任务 |
| `intern` | 实习生 | 仅查看自己的任务 |
| `intern2` | 实习生 | 种子数据中的第二位实习生 |

## 主要功能

- 任务 CRUD
- 任务状态流转：待办、进行中、已完成
- 按状态、负责人、截止日期、关键词筛选
- 卡片/表格视图切换
- 优先级颜色区分
- 截止日期提醒
- 导师/实习生角色可见范围
- ECharts 仪表盘
- RSS 实时资讯刷新、搜索、任务详情关联资讯
- 任务列表导出 CSV

## 主要 API

| Method | Path | Description |
| --- | --- | --- |
| POST | `/api/auth/mock-login` | Mock 登录 |
| GET | `/api/users` | 用户列表 |
| GET | `/api/tasks` | 任务列表与筛选 |
| POST | `/api/tasks` | 新建任务 |
| PUT | `/api/tasks/{id}` | 更新任务 |
| PATCH | `/api/tasks/{id}/status` | 更新任务状态 |
| DELETE | `/api/tasks/{id}` | 删除任务 |
| GET | `/api/dashboard/summary` | 仪表盘统计 |
| GET | `/api/news` | 资讯列表/搜索 |
| GET | `/api/news/related` | 按任务关键词获取相关资讯 |
| POST | `/api/news/refresh` | 刷新 RSS 资讯 |

前端会在请求头中携带 `X-User-Id`，后端据此做轻量角色可见性控制。
