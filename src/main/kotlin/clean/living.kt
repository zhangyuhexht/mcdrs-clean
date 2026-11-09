package top.mcdrsclean.clean

import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import top.mcdrsclean.PL
import top.mcdrsclean.config.Config
import top.mcdrsclean.util.info
import top.mcdrsclean.util.isMatch
import top.mcdrsclean.util.notifyFinish
import top.mcdrsclean.util.removeEntitiesChunked

private inline val livingCfg get() = Config.config.living

/**
 * 最近一次清理生物实体的数量
 */
var lastLiving = 0
    private set

/**
 * 扫描一个世界并算出需要清理的生物, 不修改世界.
 *
 * @return Pair(待清理的生物, 该世界生物总数)
 */
fun World.planLivingClean(): Pair<List<LivingEntity>, Int> {
    val all = livingEntities.filterNot { it is Player }
    val total = all.size
    val waitingForClean = all.toMutableList()

    PL.debug { "" }
    PL.debug { "开始清理世界${name}的生物" }
    PL.buildDebug {
        append("所有实体共").append(total).append("个: [")
        all.info().entries.joinTo(this, ", ") { (k, v) -> "$k: $v" }
        append("]")
    }

    // livingCfg.settings.name == false 时不清理命名的生物(从列表中移除)
    if (!livingCfg.settings.name) waitingForClean.removeIf { it.customName != null }
    // livingCfg.settings.lead == false 时不清理拴绳拴住的生物(从列表中移除)
    if (!livingCfg.settings.lead) waitingForClean.removeIf { it.isLeashed }
    // livingCfg.settings.mount == false 时不清理乘骑中的生物(从列表中移除)
    if (!livingCfg.settings.mount) waitingForClean.removeIf { it.isInsideVehicle || it.passengers.isNotEmpty() }

    // 按类型分组后匹配: 每条规则只需要比对类型, 而不是对每个实体做一次正则匹配
    val groups = waitingForClean.groupBy { it.type.name }
    val black = livingCfg.black
    val toRemove = groups.entries.flatMap { (type, list) ->
        val matched = type.isMatch(livingCfg.match)
        // 黑名单: 命中规则的清理; 白名单: 未命中规则的清理
        val clean = if (black) matched != null else matched == null
        PL.buildDebug {
            if (!clean) append("不")
            append("清理").append(type).append("x").append(list.size)
            if (matched != null) append(", 命中规则: ").append(matched.pattern)
        }
        if (clean) list else emptyList()
    }

    PL.debug { "世界${name}生物清理计划完成(${toRemove.size}/$total)" }
    return toRemove to total
}

/**
 * 立即清理指定世界的生物(供命令使用).
 *
 * @return Pair(clean, all)
 */
fun World.cleanLiving(): Pair<Int, Int> {
    val (toRemove, total) = planLivingClean()
    toRemove.forEach(LivingEntity::remove)
    return toRemove.size to total
}

/**
 * 分片清理全服生物(供定时任务使用), 避免一次性移除大量实体阻塞主线程.
 */
fun cleanLivingChunked(onDone: () -> Unit = {}) {
    if (!livingCfg.enable) {
        PL.debug { "生物清理已禁用" }
        onDone()
        return
    }
    val worlds = livingWorlds()
    logLivingStart(worlds)

    val start = System.currentTimeMillis()
    val plans = worlds.map { it.planLivingClean() }
    val toRemove = plans.flatMap { it.first }
    val all = plans.sumOf { it.second }
    PL.debug { "生物清理计划完成, 待清理${toRemove.size}个, 耗时${System.currentTimeMillis() - start}ms" }

    removeEntitiesChunked(toRemove) {
        lastLiving = toRemove.size
        PL.debug { "生物清理共${lastLiving}个" }
        notifyFinish(livingCfg.finish, "clean" to lastLiving, "all" to all)
        onDone()
    }
}

/**
 * 同步清理全服生物(供命令使用).
 */
fun cleanLiving() {
    if (!livingCfg.enable) {
        PL.debug { "生物清理已禁用" }
        return
    }
    val worlds = livingWorlds()
    logLivingStart(worlds)

    val start = System.currentTimeMillis()
    val result = worlds.map { it.cleanLiving() }
    lastLiving = result.sumOf { it.first }
    PL.debug { "生物清理共${lastLiving}个, 耗时${System.currentTimeMillis() - start}ms" }

    notifyFinish(livingCfg.finish, "clean" to lastLiving, "all" to result.sumOf { it.second })
}

private fun livingWorlds() = Bukkit.getWorlds()
    .filterNot { livingCfg.disableWorld.any { regex -> it.name matches regex } }

private fun logLivingStart(worlds: List<World>) {
    PL.buildDebug {
        append("开始清理生物, 启用生物清理的世界: [")
        worlds.joinTo(this, ", ", transform = World::getName)
        append("]")
    }
    PL.debug { if (livingCfg.settings.name) "清理被命名的生物" else "不清理被命名的生物" }
    PL.debug { if (livingCfg.settings.lead) "清理拴绳拴住的生物" else "不清理拴绳拴住的生物" }
    PL.debug { if (livingCfg.settings.mount) "清理乘骑中的生物" else "不清理乘骑中的生物" }
}
