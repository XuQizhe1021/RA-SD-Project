# HQ技术培训管理系统

## 1. 项目简介

HQ技术培训管理系统用于支撑培训业务的完整处理流程，覆盖培训申请、课程管理、讲师管理、学员管理、通知发布、在线报名、签到收费、培训评价与统计报表等核心场景。

当前工程已经完成以下交付能力：

- 前端静态化并入后端 `Spring Boot`
- 生产环境单端口访问
- 统一启动器
- 私有 MySQL 运行时初始化与启停脚本
- `Inno Setup` 安装包构建脚本
- 可直接分发的 Windows 安装包

## 2. 目录说明

```text
project/
├─ backend/                       后端 Spring Boot 工程
├─ frontend/                      前端 Vue 工程
├─ installer/                     安装包目录
│  ├─ app.iss                     Inno Setup 安装脚本
│  ├─ build_installer.ps1         安装包构建脚本
│  ├─ build_installer.bat         安装包构建双击入口
│  ├─ payload/                    安装后脚本模板
│  ├─ staging/                    安装包暂存目录
│  └─ output/                     安装包输出目录
├─ build_delivery.ps1             交付态构建脚本
├─ build_delivery.bat             交付态构建双击入口
├─ launch_app.ps1                 源码交付态统一启动器
├─ launch_app.bat                 源码交付态统一启动双击入口
├─ start_project.ps1              开发态联调脚本
├─ start_project.bat              开发态联调双击入口
└─ README.md                      使用说明
```

## 3. 推荐使用方式

### 3.1 直接使用安装包

当前已经生成好的安装包位于：

- `installer/output/HQTrainingSetup.exe`

这是最接近最终交付给甲方的使用方式。安装完成后：

- 安装目录下会包含内置 Java 运行时与私有 MySQL 运行时
- 首次安装会自动初始化私有数据库
- 桌面快捷方式可直接启动系统
- 启动后统一访问地址为 `http://127.0.0.1:18080`

### 3.2 从源码重新打安装包

如果需要重新产出新的安装包，可在项目根目录执行：

```powershell
powershell.exe -ExecutionPolicy Bypass -File ".\installer\build_installer.ps1" -SkipTests
```

或者双击：

- `installer/build_installer.bat`

脚本会自动完成：

1. 构建前端静态资源并并入后端
2. 打包后端可执行 `jar`
3. 使用 `jlink` 生成内置 Java 运行时
4. 自动采集本机 MySQL 运行时文件
5. 组装安装包 `staging` 目录
6. 调用 `Inno Setup` 生成最终安装包

默认输出目录：

- `installer/output/`

### 3.3 直接运行源码交付态

如果你只想在本机快速验证交付态效果，而不经过安装包，可以继续使用：

- `build_delivery.bat`
- `launch_app.bat`

这种方式依然是“前端并入后端”的单体交付形态，但数据库仍默认连接你本机现成的 MySQL。

补充说明：

- 双击 `launch_app.bat` 后，启动器会先询问是否重建数据库
- 选择 `N` 时会保留当前数据库中的已有业务数据，仅执行应用启动
- 选择 `Y` 时会重新导入 `backend/src/main/resources/db/mysql/001_init.sql`，原有数据库数据会被初始化脚本覆盖
- 日常使用建议选择 `N`，只有在确实需要恢复初始演示数据时再选择 `Y`

### 3.4 开发态联调

如果你还需要前端热更新开发体验，可以继续使用：

- `start_project.bat`
- `start_project.ps1`

该方式会分别启动后端和 `Vite` 开发服务器，只适合开发联调，不适合作为最终交付方式。

补充说明：

- 双击 `start_project.bat` 后，启动器同样会先询问是否重建数据库
- 选择 `N` 时会保留当前数据库数据，只启动前后端开发环境
- 选择 `Y` 时会重新导入 `backend/src/main/resources/db/mysql/001_init.sql`，用于恢复初始演示数据

## 4. 安装包说明

当前安装包采用以下结构：

- 应用程序：后端可执行 `jar` + 已并入的前端静态资源
- Java 运行时：安装包内置运行时，不依赖用户本机预装 Java
- 数据库：安装包内置私有 MySQL 运行时，不依赖用户本机预装 MySQL 服务
- 启动方式：安装后通过统一启动器完成“检查数据库 -> 启动数据库 -> 启动后端 -> 打开浏览器”

安装包默认使用的内部端口：

- 应用端口：`18080`
- 私有数据库端口：`23306`

安装包默认数据库账号：

- 管理账号：`hq_app`
- 数据库密码：由安装包内部脚本自动写入外部配置文件，仅供应用连接使用

卸载时：

- 会先执行清理脚本，尝试停止后端与私有数据库
- 会询问是否同时删除本地数据库数据与日志

## 5. 重新打包前提

只有在“重新构建安装包”时，当前机器才需要这些环境：

- Java（当前脚本会自动从本机 Java 环境生成内置运行时）
- Maven 3.9 及以上
- Node.js 18 及以上
- 一份可读取的 MySQL 运行时目录
- Inno Setup 6

说明：

- 当前脚本会自动探测本机 `java`、`jlink`、`mysqld.exe` 和 `ISCC.exe`
- 若 MySQL 不是默认安装路径，可在构建安装包时通过 `-MySqlRuntimeDir` 显式指定

示例：

```powershell
powershell.exe -ExecutionPolicy Bypass -File ".\installer\build_installer.ps1" `
  -SkipTests `
  -MySqlRuntimeDir "C:\mysql-8.0.33-winx64"
```

## 6. 交付态配置说明

源码交付态配置文件位于：

- `backend/src/main/resources/application-prod.yml`

安装包运行时使用的外部配置文件位于安装目录：

- `app/application-prod.yml`

安装包中的这份配置默认连接私有数据库：

- `127.0.0.1:23306`

因此最终用户安装后：

- 不需要自己装 Java
- 不需要自己装 Node.js
- 不需要自己配 MySQL 服务

## 7. 初始账号

系统已内置基础账号，可直接登录：

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
- `executor01` 可处理学员注册审核，但不能创建内部岗位账号
- 数据库中预置了一个待审核学员账号 `student03`，可用于验证注册审核流程

## 8. 已完成的验证

本次已完成以下验证：

- `build_delivery.ps1 -SkipTests` 可成功构建前后端交付产物
- `installer/build_installer.ps1 -PrepareOnly` 可成功组装安装包 `staging` 目录
- 私有数据库初始化脚本 `init_db.ps1` 已在纯 ASCII 路径下验证通过
- 私有数据库启动脚本 `start_db.ps1` 已验证可拉起 `23306` 端口
- `Inno Setup` 安装包已成功生成

## 9. 常见问题

### 9.1 安装包构建脚本提示找不到 `ISCC.exe`

原因：

- 本机没有安装 Inno Setup 6
- 或安装路径不在脚本默认探测范围内

处理办法：

1. 安装 Inno Setup 6
2. 重新执行 `installer/build_installer.ps1`
3. 如仍未识别，可通过 `-IsccPath` 显式指定 `ISCC.exe`

### 9.2 安装包构建脚本提示找不到 MySQL 运行时目录

原因：

- 本机没有安装 MySQL
- 或 `mysqld.exe` 不在环境变量里

处理办法：

1. 确认本机存在可用的 MySQL 运行时目录
2. 通过 `-MySqlRuntimeDir` 显式指定该目录

### 9.3 在中文工作目录里直接运行 `staging` 中的私有 MySQL 脚本失败

这是 MySQL Windows 运行时对路径兼容性的老问题。最终安装包默认安装到 `Program Files` 这类 ASCII 路径，实际安装使用不受影响。

### 9.4 仍想保留前端热更新开发方式

可以继续使用：

- `start_project.bat`

但这属于开发态模式，不属于最终交付方式。

## 10. 当前结果

当前项目已经不只是“安装包方案文档”，而是已经具备完整的安装包产线与产物：

- 安装包脚本：`installer/app.iss`
- 安装包构建脚本：`installer/build_installer.ps1`
- 安装后初始化与启停脚本：`installer/payload/scripts/`
- 最终安装包：`installer/output/HQTrainingSetup.exe`
