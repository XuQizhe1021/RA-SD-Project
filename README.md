# HQ技术培训管理系统

## 项目说明

本仓库用于承接《软件过程与项目管理》实验3中 `7.7-7.13` 冲刺期的开发工作，当前已完成以下交付：

- `frontend`：`Vue 3 + Vite + Element Plus + Pinia + Vue Router` 管理端骨架
- `backend`：`Spring Boot 3` 认证、课程管理、讲师管理、报名管理、签到管理、收费管理基础服务
- `backend/src/main/resources/db/mysql/001_init.sql`：增量1核心表结构与初始化数据脚本
- `frontend/src/views/CoursesView.vue`：课程管理页面
- `frontend/src/views/LecturersView.vue`：讲师管理页面
- `frontend/src/views/EnrollmentsView.vue`：报名管理页面
- `frontend/src/views/AttendanceView.vue`：签到管理页面
- `frontend/src/views/PaymentsView.vue`：收费管理页面

## 目录结构

```text
project/
├─ frontend/   # 管理端前端工程
├─ backend/    # 后端服务工程
└─ README.md   # 仓库说明
```

## 已完成的 7.7-7.10 工作

- 项目工程初始化
- 登录接口与 Token 鉴权基础能力
- 按角色返回菜单
- 基础首页与后台布局
- 增量1核心数据库建表脚本
- 课程管理 CRUD 页面与接口
- 讲师管理 CRUD 页面与接口
- 报名管理列表、报名审核与状态流转页面及接口
- 签到管理列表与执行签到页面及接口
- 收费管理列表与执行收费页面及接口
- 课程管理与讲师管理已切换为真实 `MySQL` 持久化实现
- 报名审核通过后自动初始化签到记录与收费记录
- 签到与收费动作均已接入真实 `MySQL` 数据库

## 7.10 当日可演示内容

- 执行人登录系统后进入 `课程管理`
- 查看课程列表，按课程状态和讲师筛选
- 新增课程、编辑课程、发布课程
- 进入 `讲师管理`
- 查看讲师列表，新增讲师、编辑讲师、停用讲师
- 进入 `报名管理`
- 查看报名列表，按状态、课程、学员筛选
- 新增报名，模拟学员提交报名
- 审核通过或驳回报名，查看状态流转
- 进入 `签到管理`
- 查看签到列表并执行签到
- 进入 `收费管理`
- 查看收费列表并执行收费
- 将课程管理、讲师管理、报名管理、签到管理、收费管理页面截图提供给 E 归档

## 演示账号

当前后端内置了 5 个演示账号，便于 7.10 当天直接登录验证：

| 角色 | 用户名 | 密码 |
| --- | --- | --- |
| 经理 | `manager01` | `123456` |
| 执行人 | `executor01` | `123456` |
| 现场工作人员 | `staff01` | `123456` |
| 学员 | `student01` | `123456` |
| 学员 | `student02` | `123456` |

## 角色权限说明

本版本已按实验说明中的主业务闭环重新收敛角色边界，前后端均按角色控制模块可见性、页面操作与数据范围：

| 角色 | 可访问模块 | 主要能力 |
| --- | --- | --- |
| 经理 | 首页概览、培训申请、课程管理、讲师管理、统计报表 | 查看培训申请、掌握课程计划与讲师资源、跟踪经营统计 |
| 执行人 | 首页概览、课程管理、讲师管理、学员管理、通知发布、报名管理、统计报表 | 维护课程与讲师、发布通知、处理报名审核、支撑培训执行 |
| 现场工作人员 | 首页概览、签到管理、收费管理、评价管理 | 核验报名名单、执行签到与收费、整理培训评价 |
| 学员 | 首页概览、通知发布、报名管理、收费管理、评价管理 | 浏览通知、提交本人报名、查看并完成本人缴费、提交课程评价 |

补充说明：

- 后端登录已改为读取 `user_account`、`user_role` 表，不再使用内存账号。
- 课程、讲师、报名、签到、收费接口均会校验当前登录人角色。
- 学员仅能查看和操作自己的报名与缴费记录，无法访问审核类功能。
- 前端路由守卫会根据后端返回的菜单拦截越权 URL，避免“知道路径就能进页面”的问题。

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

## 数据库连接

后端默认连接本机 MySQL：

- 地址：`127.0.0.1:3306`
- 数据库：`hq_training`
- 用户名：`root`
- 密码：`123456`

如需修改，可通过环境变量覆盖：

- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USERNAME`
- `DB_PASSWORD`

## 数据库脚本

数据库初始化脚本位于：

- `backend/src/main/resources/db/mysql/001_init.sql`

导入方式示例：

```powershell
$tempSql = "$env:LOCALAPPDATA\Temp\hq_init.sql"
Copy-Item .\backend\src\main\resources\db\mysql\001_init.sql $tempSql -Force
mysql --default-character-set=utf8mb4 -uroot -p123456 -e "SOURCE $($tempSql -replace '\\','/')"
```

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

同时包含 `课程管理`、`讲师管理`、`报名管理`、`签到收费联调` 所需的演示种子数据，导入后可直接登录前端查看真实库记录。

说明：

- 若旧库中尚未导入 `student_profile` 演示数据，报名模块首次拉取学员选项时会自动补齐默认演示学员档案。
- 初始化脚本内置了 `2` 个学员账号，便于演示“企业付费”和“个人付费”两种收费路径。

## 整体启动流程

建议严格按以下顺序启动：

1. 启动数据库
2. 导入初始化脚本
3. 启动后端
4. 启动前端
5. 打开浏览器验证页面

### 1. 启动数据库

如果本机 `MySQL` 服务具备管理员启动权限，可直接使用：

```powershell
sc.exe start MySQL
```

如果当前环境无法启动系统服务，可使用用户态临时实例方式启动：

```powershell
$runtime = "$env:LOCALAPPDATA\Temp\hq-mysql-runtime"
New-Item -ItemType Directory -Force -Path "$runtime\data" | Out-Null

& 'C:\mysql-8.0.33-winx64\bin\mysqld.exe' `
  --initialize-insecure `
  --basedir='C:/mysql-8.0.33-winx64' `
  --datadir="$($runtime -replace '\\','/')/data"

& 'C:\mysql-8.0.33-winx64\bin\mysqld.exe' `
  --basedir='C:/mysql-8.0.33-winx64' `
  --datadir="$($runtime -replace '\\','/')/data" `
  --port=3306 `
  --bind-address=127.0.0.1 `
  --mysqlx=0 `
  --console
```

首次使用用户态实例时，需要设置 root 密码：

```powershell
mysql -uroot -e "ALTER USER 'root'@'localhost' IDENTIFIED BY '123456'; FLUSH PRIVILEGES;"
```

### 2. 导入数据库脚本

```powershell
$tempSql = "$env:LOCALAPPDATA\Temp\hq_init.sql"
Copy-Item .\backend\src\main\resources\db\mysql\001_init.sql $tempSql -Force
mysql --default-character-set=utf8mb4 -uroot -p123456 -e "SOURCE $($tempSql -replace '\\','/')"
```

### 3. 启动后端

```powershell
cd .\backend
mvn spring-boot:run
```

后端默认地址：

- `http://localhost:18080`

### 4. 启动前端

```powershell
cd .\frontend
npm run dev
```

前端默认地址：

- `http://localhost:5173`

### 5. 验证系统

浏览器打开：

- `http://localhost:5173`

推荐使用演示账号：

- 用户名：`executor01`
- 密码：`123456`

验证路径建议：

1. 登录进入首页
2. 打开 `课程管理`
3. 打开 `讲师管理`
4. 打开 `报名管理`
5. 新增一条报名记录并执行审核
6. 打开 `签到管理` 并执行签到
7. 打开 `收费管理` 并执行收费
8. 确认课程列表有 `2` 条数据
9. 确认讲师列表有 `2` 条数据
