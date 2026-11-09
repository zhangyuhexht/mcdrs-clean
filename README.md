# MCDRS Clean

> 面向 **Paper 26.3** 的轻量实体清理插件：定时清理掉落物、生物与密集实体，倒计时提醒默认不进后台，清理过程分片执行。

MCDRS Clean 是一个基于 Bukkit/Paper API 的清理插件，由 GPL-3.0 开源清理插件二次开发而来，已完成完整改名、包命名空间迁移与 Paper 26.3 适配，并重构了清理调度、日志与权限逻辑。

## 环境要求

| 项目 | 要求 |
| --- | --- |
| 服务端 | Paper 26.3（`api-version: '26.3'`） |
| Java | 25（Paper 26.1 起的最低要求；编译同样需要 JDK 25） |
| 可选依赖 | PlaceholderAPI（提供占位符） |

> 插件产物为 Java 21 字节码，运行在 Java 25 的服务端上。`api-version: '26.3'` 是单向声明，旧版核心会拒绝加载；Mod 混合核心（Mohist / Arclight）不在支持范围内。

## 特性

- **定时清理**：间隔可配置；倒计时按墙钟计算，服务器掉 TPS 也不会漂移
- **后台不刷屏**：倒计时和清理结果默认只发给在线玩家，需要后台同步打印时把 `console_broadcast` 设为 `true`
- **分片执行**：每 tick 只处理 `batch_size` 个实体 / 区块，避免一次性移除大量实体顶高 MSPT
- **规则可配**：按世界、实体类型（正则）、物品名与标签白名单过滤；可保留拴绳、乘骑、已命名实体
- **密集实体检测**：按区块统计并限制数量，超限可通知管理员
- **公共垃圾桶**：GUI 存取，带过期倒计时
- **安全**：全服清理指令需要 10 秒内二次确认，并使用独立权限节点
- **PlaceholderAPI**：7 个占位符，可用于计分板、聊天前缀等

## 安装

1. 从 [Releases](https://github.com/zhangyuhexht/mcdrs-clean/releases) 下载最新版 `MCDRSClean-1.21.0.jar`
2. 放入服务端 `plugins/` 目录并启动一次，生成 `plugins/MCDRSClean/`
3. 按需编辑 `config.yml`，然后执行 `/mcdrsclean reload`

## 指令

主命令 `/mcdrsclean`，别名 `/mcdrs`、`/mc`（若别名与其他插件冲突，请使用主命令）。

| 指令 | 说明 |
| --- | --- |
| `/mcdrsclean reload` | 重载配置与语言文件，清理任务重新计时 |
| `/mcdrsclean clean` | 执行一次全服清理（**需在 10 秒内再次执行确认**） |
| `/mcdrsclean clean entity [世界名]` | 只清理生物 |
| `/mcdrsclean clean drop [世界名]` | 只清理掉落物 |
| `/mcdrsclean clean chunk [世界名]` | 只执行密集实体清理 |
| `/mcdrsclean stats [世界名]` | 统计实体与区块 |
| `/mcdrsclean entity <实体名> [世界名] [数量阈值]` | 按区块统计指定实体 |
| `/mcdrsclean trash` | 打开公共垃圾桶 |
| `/mcdrsclean show` | 打开密集实体统计菜单 |
| `/mcdrsclean players` | 列出在线玩家及所在位置 |
| `/mcdrsclean debug` | 切换 debug 消息的接收 |

## 权限

| 节点 | 默认 | 说明 |
| --- | --- | --- |
| `mcdrs.admin` | OP | 查询、统计、重载等管理指令 |
| `mcdrs.clean` | OP | 执行清理指令（危险操作，可单独授权） |
| `mcdrs.trash` | OP | 打开垃圾桶 |

## 配置

完整默认配置见 [`src/main/resources/config.yml`](src/main/resources/config.yml)，常用项：

| 键 | 默认值 | 说明 |
| --- | --- | --- |
| `duration` | `600` | 清理间隔（秒） |
| `message` | `60 / 30 / 10 / 0` | 清理前倒计时文案，键为剩余秒数 |
| `console_broadcast` | `false` | 广播是否同时输出到后台 / 日志文件 |
| `batch_size` | `200` | 每 tick 处理的实体 / 区块数量 |
| `debug` | `false` | 输出详细调试信息 |
| `bstats` | `false` | 是否上报统计（需先填入自有 bStats ID） |
| `no_online` | — | 无在线玩家时的清理策略 |
| `living` / `drop` / `chunk` / `trashcan` | — | 各类清理的规则与开关 |

消息文案在 `lang.yml`，支持 `&` 颜色码。

## PlaceholderAPI

安装 PlaceholderAPI 后自动注册，占位符前缀为 `%mcdrs_...%`：

| 占位符 | 含义 |
| --- | --- |
| `%mcdrs_before_next%` | 距离下一次清理的秒数 |
| `%mcdrs_before_next_formatted%` | 同上，格式化时间 |
| `%mcdrs_last_drop%` | 上次清理的掉落物数量 |
| `%mcdrs_last_living%` | 上次清理的生物数量 |
| `%mcdrs_last_chunk%` | 上次清理的密集实体数量 |
| `%mcdrs_trashcan_countdown%` | 垃圾桶清理倒计时（秒） |
| `%mcdrs_trashcan_countdown_formatted%` | 同上，格式化时间 |

## 构建

```bash
git clone https://github.com/zhangyuhexht/mcdrs-clean.git
cd mcdrs-clean
./gradlew shadowJar        # 需要 JDK 25
```

产物位于 `build/libs/MCDRSClean-1.21.0.jar`。

构建依赖 `top.e404:eplugin-*:1.4.0`（第三方 Kotlin 工具库，发布在上游作者私有仓库，Maven Central 没有）：

1. 克隆 [EPlugin](https://github.com/4o4E/EPlugin)，把 `buildSrc/src/main/kotlin/Versions.kt` 的 `group` / `version` 改为 `top.e404` / `1.4.0`，`settings.gradle.kts` 只保留 `core`、`menu`、`serialization`、`hook-placeholderapi` 四个模块；
2. 用 JDK 21 + Gradle 9.x 执行这四个模块的 `publishToMavenLocal`（Gradle 9 下需删掉 publication 中重复的 `sourcesJar` artifact）；
3. 回到本项目，用 JDK 25 执行 `./gradlew shadowJar`。

> 请用 `shadowJar` 而不是 `build`：仓库内的测试套件基于 MockBukkit 1.20，在 JDK 25 下无法运行。

## 验证

在 **Paper 26.3-245 / Java 25** 测试服做过实测：

- 插件正常启用，`plugins` 列表可见，指令、别名、二次确认均正常，无 `NoSuchMethodError` / `InvalidPluginException`
- `console_broadcast: false` 时后台周期广播 **0 条**；改为 `true` 后恢复 **6 条**
- 有实体场景下 29 个掉落物清理 28 个（1 个白名单物品正确保留）
- `batch_size=1` 时清理确实跨 tick 分片执行

## 开源许可

本项目采用 [GNU General Public License v3.0](LICENSE)。

本仓库是一个 GPL-3.0 开源清理插件的**修改 / 派生版本**：已完整改名、迁移包命名空间、适配 Paper 26.3，并重构了清理调度、日志与权限逻辑。依 GPL-3.0，派生版本须继续以 GPL-3.0 开源且不得附加额外限制；上游代码的版权仍归其原作者所有。
