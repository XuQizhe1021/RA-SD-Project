# HQ技术培训管理系统

## 项目说明

本仓库用于承接《软件过程与项目管理》实验3中 `7.7-7.13` 冲刺期的开发工作，当前已完成以下交付：

- `frontend`：`Vue 3 + Vite + Element Plus + Pinia + Vue Router` 管理端骨架
- `backend`：`Spring Boot 3` 认证、课程管理、讲师管理基础服务
- `backend/src/main/resources/db/mysql/001_init.sql`：增量1核心表结构与初始化数据脚本
- `frontend/src/views/CoursesView.vue`：课程管理页面
- `frontend/src/views/LecturersView.vue`：讲师管理页面

## 目录结构

```text
project/
├─ frontend/   # 管理端前端工程
├─ backend/    # 后端服务工程
└─ README.md   # 仓库说明
```

## 已完成的 7.7-7.8 工作

- 项目工程初始化
- 登录接口与 Token 鉴权基础能力
- 按角色返回菜单
- 基础首页与后台布局
- 增量1核心数据库建表脚本
- 课程管理 CRUD 页面与接口
- 讲师管理 CRUD 页面与接口
- 课程管理与讲师管理已切换为真实 `MySQL` 持久化实现

## 7.8 当日可演示内容

- 执行人登录系统后进入 `课程管理`
- 查看课程列表，按课程状态和讲师筛选
- 新增课程、编辑课程、发布课程
- 进入 `讲师管理`
- 查看讲师列表，新增讲师、编辑讲师、停用讲师
- 将课程管理与讲师管理页面截图提供给 E 归档

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

同时包含 `课程管理` 与 `讲师管理` 的演示种子数据，导入后可直接登录前端查看真实库记录。

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
4. 确认课程列表有 `2` 条数据
5. 确认讲师列表有 `2` 条数据
