# 更新记录

## 1.21.0 (2026-11-10)

MCDRS Clean 的首个版本，基于 GPL-3.0 开源清理插件二次开发。

### 平台适配

- 目标核心改为 **Paper 26.3**（`api-version: '26.3'`），需要 **Java 25**
- 编译依赖由 `org.spigotmc:spigot-api` 换成 `io.papermc.paper:paper-api:26.3.build.244-stable`
- 工具链升级到 Gradle 9.8.2 / Kotlin 2.4.21 / Shadow 9.6.1，产物保持 Java 21 字节码
- `Enchantment.DURABILITY` → `Enchantment.UNBREAKING`（26.x 移除了旧别名，实机编译发现）
- 移除对上游 `mcVer` 的依赖（26.x 的版本号无法被旧版正则解析）
- 修正 `jar` 与 `shadowJar` 产物同名互相覆盖的问题

### 完整改名

- 插件名、包名 `top.mcdrsclean`、主类、指令、权限、占位符、消息前缀、数据目录、构建产物统一为 MCDRS Clean
- 移除指向上游仓库的自动更新检查器与上游 bStats ID，不再向原项目上报统计
- 项目目录、Gradle 工程名与 group 同步更名

### 功能改进

- 清理倒计时与清理结果默认**只发给在线玩家**，新增 `console_broadcast` 开关控制后台日志
- 清理改为**分片执行**（先算计划、再逐 tick 移除），新增 `batch_size`
- 倒计时改按墙钟计算，避免低 TPS 时实际间隔被拉长
- 密集实体规则匹配先按类型分组，复杂度从 O(规则 × 实体) 降到 O(规则 × 类型)
- 掉落物判断先用 `hasItemMeta()` 短路，避免逐个深拷贝 ItemMeta
- 垃圾桶去重不再依赖已弃用的 `durability`
- `/mcdrsclean clean` 增加 10 秒二次确认，新增独立的 `mcdrs.clean` 权限
- 配置字段全部补默认值，手改配置漏键不再导致插件加载失败
- `update` / `bstats` 默认关闭，离线环境启动不再尝试联网

### 验证

- Paper 26.3-245 测试服两轮对照：后台广播静默 / 恢复均通过
- 二次确认、分片跨 tick、有实体清理、白名单保留均实测通过
