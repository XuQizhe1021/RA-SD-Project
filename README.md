# HQ技术培训管理系统

## 1. 项目说明

本项目当前保留的是源码开发态运行方式，不再维护安装包方案。

开发态启动目标是：

- 检测本机 MySQL 服务是否存在
- 启动 MySQL 服务并确认数据库连通
- 按需导入 `001_init.sql` 创建或重建业务数据库
- 分别启动后端 Spring Boot 和前端 Vite 开发服务器

## 2. 目录说明

```text
project/
├─ backend/                       后端 Spring Boot 工程
│  └─ src/main/resources/db/mysql/001_init.sql
├─ frontend/                      前端 Vue 3 + Vite 工程
├─ build_delivery.ps1             交付态构建脚本
├─ build_delivery.bat             交付态构建双击入口
├─ launch_app.ps1                 源码交付态统一启动器
├─ launch_app.bat                 源码交付态统一启动双击入口
├─ start_project.ps1              开发态联调脚本
├─ start_project.bat              开发态联调双击入口
└─ README.md                      使用说明
```

## 3. 开发环境要求

在 Windows 环境下运行时，建议提前准备：

- Java 17 及以上
- Maven 3.9 及以上
- Node.js 18 及以上
- MySQL 8.0.x

默认数据库配置如下：

- 主机：`127.0.0.1`
- 端口：`3306`
- 数据库名：`hq_training`
- 用户名：`root`
- 密码：`123456`

后端默认启动端口：

- `18080`

前端默认启动端口：

- `5173`

## 4. 推荐启动方式

最推荐直接使用：

- `start_project.bat`

它会自动完成下面这些事情：

1. 检测本机 MySQL 服务
2. 尝试启动 MySQL 服务
3. 询问你是否要重建数据库
4. 打开一个后端终端执行 `mvn spring-boot:run`
5. 打开一个前端终端执行 `npm install`（首次）和 `npm run dev`

补充说明：

- 启动时如果选择 `N`，会保留当前数据库数据
- 启动时如果选择 `Y`，会重新导入 `backend/src/main/resources/db/mysql/001_init.sql`
- 日常联调建议选择 `N`
- 只有在需要恢复初始演示数据时才选择 `Y`

## 5. 详细开发态指令启动教程

下面这套是完整的手工命令启动流程，适合你在实验报告、答辩演示或排查问题时直接照着操作。

### 5.1 打开项目根目录

先进入项目根目录：

```powershell
Set-Location "E:\学习\实验报告\软件过程与项目管理\实验3-敏捷项目开发综合实践-要求+报告模板for2023未来技术学生\project"
```

### 5.2 检测本机 MySQL 服务

先查看系统里是否存在 MySQL 服务：

```powershell
Get-Service | Where-Object { $_.Name -match "mysql" -or $_.DisplayName -match "mysql" }
```

如果能看到类似 `MySQL80`、`MySQL` 这样的服务名，说明本机已经安装了 MySQL 服务。

再检查 `mysql.exe` 客户端是否可用：

```powershell
Get-Command mysql.exe
```

如果这里报找不到命令，说明你需要：

- 安装 MySQL Client
- 或把 MySQL 的 `bin` 目录加入系统 `PATH`

常见路径类似：

- `C:\Program Files\MySQL\MySQL Server 8.0\bin`
- `C:\mysql-8.0.33-winx64\bin`

### 5.3 启动 MySQL 服务

如果服务存在但没有启动，可以执行：

```powershell
Start-Service MySQL80
```

如果你的服务名不是 `MySQL80`，请把它替换成你机器上的实际服务名，例如：

```powershell
Start-Service MySQL
```

启动后，建议再确认一下端口是否可用：

```powershell
Test-NetConnection 127.0.0.1 -Port 3306
```

当 `TcpTestSucceeded` 为 `True` 时，说明数据库端口已经可连接。

### 5.4 创建数据库

先进入 MySQL：

```powershell
mysql -h127.0.0.1 -P3306 -uroot -p123456
```

在 MySQL 命令行里执行：

```sql
CREATE DATABASE IF NOT EXISTS hq_training
DEFAULT CHARACTER SET utf8mb4
DEFAULT COLLATE utf8mb4_unicode_ci;
```

执行完成后退出：

```sql
exit
```

### 5.5 导入初始化脚本

项目初始化脚本路径是：

- `backend/src/main/resources/db/mysql/001_init.sql`

在项目根目录执行下面这条命令导入：

```powershell
cmd /c "mysql --default-character-set=utf8mb4 -h127.0.0.1 -P3306 -uroot -p123456 hq_training < backend\src\main\resources\db\mysql\001_init.sql"
```

说明：

- 这条命令会创建表结构并导入初始化数据
- 如果你想恢复初始演示数据，可以再次执行这条命令

### 5.6 启动后端

新开一个 PowerShell 窗口，执行：

```powershell
Set-Location "E:\学习\实验报告\软件过程与项目管理\实验3-敏捷项目开发综合实践-要求+报告模板for2023未来技术学生\project\backend"
$env:DB_HOST = "127.0.0.1"
$env:DB_PORT = "3306"
$env:DB_NAME = "hq_training"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "123456"
mvn spring-boot:run
```

后端启动成功后，默认监听：

- `http://localhost:18080`

### 5.7 启动前端

再新开一个 PowerShell 窗口，执行：

```powershell
Set-Location "E:\学习\实验报告\软件过程与项目管理\实验3-敏捷项目开发综合实践-要求+报告模板for2023未来技术学生\project\frontend"
npm install
npm run dev
```

说明：

- 第一次启动必须先执行 `npm install`
- 如果以后依赖没有变化，可以直接执行 `npm run dev`

前端启动成功后，默认访问地址：

- `http://localhost:5173`

### 5.8 访问系统

浏览器打开：

```text
http://localhost:5173
```

如果前后端都启动正常，页面即可正常登录和使用。

## 6. 一键脚本与手工命令的对应关系

如果你使用 `start_project.bat`，它本质上对应的是下面这套流程：

1. 检测 MySQL 服务是否存在
2. 自动启动 MySQL 服务
3. 按你的选择决定是否导入 `001_init.sql`
4. 启动后端
5. 启动前端

所以：

- 想省事时，用 `start_project.bat`
- 想写实验报告或排查问题时，用第 5 节的手工命令

## 7. 初始账号

系统初始化后可直接使用以下账号登录：

| 角色 | 用户名 | 密码 |
| --- | --- | --- |
| 系统管理员 | `admin01` | `123456` |
| 经理 | `manager01` | `123456` |
| 执行人 | `executor01` | `123456` |
| 现场工作人员 | `staff01` | `123456` |
| 学员 | `student01` | `123456` |
| 学员 | `student02` | `123456` |

补充说明：

- `admin01` 可创建内部岗位账号，并审核学员注册申请
- 数据库中预置了一个待审核学员账号 `student03`

## 8. 常见问题

### 8.1 `Get-Service` 找不到 MySQL 服务

原因通常是：

- 本机没有安装 MySQL Server
- MySQL 没有注册成 Windows 服务

处理建议：

1. 安装 MySQL Server 8.0
2. 确保服务名能在 `Get-Service` 中查到

### 8.2 `Get-Command mysql.exe` 找不到客户端

原因通常是：

- 没装 MySQL Client
- `PATH` 没配置

处理建议：

1. 找到 MySQL `bin` 目录
2. 加到系统环境变量 `PATH`
3. 重新打开 PowerShell 再试

### 8.3 `mvn spring-boot:run` 启动失败

优先检查：

- Java 版本是否满足 17+
- Maven 是否安装成功
- 数据库是否已启动
- `hq_training` 数据库是否已创建并导入脚本

### 8.4 前端启动失败

优先检查：

- Node.js 版本是否满足 18+
- 是否已经执行过 `npm install`
- `frontend/node_modules` 是否完整

### 8.5 想重置数据库数据

可以重新执行：

```powershell
cmd /c "mysql --default-character-set=utf8mb4 -h127.0.0.1 -P3306 -uroot -p123456 hq_training < backend\src\main\resources\db\mysql\001_init.sql"
```

或者直接运行：

- `start_project.bat`

然后在提示时选择 `Y`。
