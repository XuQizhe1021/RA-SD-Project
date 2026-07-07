# HQ技术培训管理系统

## 项目说明

本仓库用于承接《软件过程与项目管理》实验3中 `7.7 Sprint启动日` 的开发工作，当前已完成以下基础交付：

- `frontend`：`Vue 3 + Vite + Element Plus + Pinia + Vue Router` 管理端骨架
- `backend`：`Spring Boot 3` 认证与菜单基础服务
- `backend/src/main/resources/db/mysql/001_init.sql`：增量1核心表结构与初始化数据脚本

## 目录结构

```text
project/
├─ frontend/   # 管理端前端工程
├─ backend/    # 后端服务工程
└─ README.md   # 仓库说明
```

## 已完成的 7.7 工作

- 项目工程初始化
- 登录接口与 Token 鉴权基础能力
- 按角色返回菜单
- 基础首页与后台布局
- 增量1核心数据库建表脚本

## 演示账号

当前后端内置了 4 个演示账号，便于 7.7 当天直接登录验证：

| 角色 | 用户名 | 密码 |
| --- | --- | --- |
| 经理 | `manager01` | `123456` |
| 执行人 | `executor01` | `123456` |
| 现场工作人员 | `staff01` | `123456` |
| 学员 | `student01` | `123456` |

## 前端启动

```bash
cd frontend
npm install
npm run dev
```

默认访问地址：`http://localhost:5173`

## 后端启动

```bash
cd backend
mvn spring-boot:run
```

默认访问地址：`http://localhost:18080`

## 数据库脚本

数据库初始化脚本位于：

- `backend/src/main/resources/db/mysql/001_init.sql`

脚本覆盖增量1主闭环所需核心表：

- 用户与角色
- 培训申请
- 课程
- 讲师
- 学员
- 通知
- 报名
- 签到
- 收费
- 评价
- 操作日志
