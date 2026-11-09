package top.mcdrsclean.clean

import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.World
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import top.mcdrsclean.PL
import top.mcdrsclean.config.Config
import top.mcdrsclean.util.info
import top.mcdrsclean.util.notifyFinish
import top.e404.eplugin.EPlugin.Companion.placeholder

private inline val chunkCfg get() = Config.config.chunk

/**
 * 最近一次清理区块密集实体的数量
 */
var lastChunk = 0
    private set

/**
 * 同步清理全服区块中的密集实体(供命令使用).
 */
fun cleanDenseEntities() {
    if (!chunkCfg.enable) {
        PL.debug { "密集实体清理已禁用" }
        return
    }
    val worlds = chunkWorlds()
    logChunkStart(worlds)

    val start = System.currentTimeMillis()
    lastChunk = worlds.sumOf { it.cleanChunkDenseEntities() }
    PL.debug { "密集实体清理共${lastChunk}个, 耗时${System.currentTimeMillis() - start}ms" }

    notifyFinish(chunkCfg.finish, "clean" to lastChunk)
}

/**
 * 分片清理全服区块中的密集实体(供定时任务使用).
 *
 * 密集实体检查天然以区块为单位, 因此按"每 tick 处理 N 个区块"推进,
 * 连扫描开销也一起摊开, 而不是先扫全图再分片删除.
 */
fun cleanDenseEntitiesChunked(onDone: () -> Unit = {}) {
    if (!chunkCfg.enable) {
        PL.debug { "密集实体清理已禁用" }
        onDone()
        return
    }
    val worlds = chunkWorlds()
    logChunkStart(worlds)

    val start = System.currentTimeMillis()
    // 快照当前加载的区块; 处理时再检查 isLoaded, 因为清理过程中区块可能被卸载
    val chunks = worlds.flatMap { it.loadedChunks.toList() }
    val batchSize = Config.config.batchSize.coerceAtLeast(1)
    var index = 0
    var cleaned = 0

    PL.runTaskTimer(1, 1) { runnable ->
        val end = (index + batchSize).coerceAtMost(chunks.size)
        for (i in index until end) {
            val chunk = chunks[i]
            if (chunk.isLoaded) cleaned += chunk.cleanDenseEntities()
        }
        index = end
        if (index >= chunks.size) {
            runnable.cancel()
            lastChunk = cleaned
            PL.debug { "密集实体清理共${lastChunk}个, 耗时${System.currentTimeMillis() - start}ms" }
            notifyFinish(chunkCfg.finish, "clean" to lastChunk)
            onDone()
        }
    }
}

/**
 * 清理指定世界加载区块中的密集实体(同步, 供命令使用).
 *
 * @return 清理的实体数量
 */
fun World.cleanChunkDenseEntities() = loadedChunks.sumOf { it.cleanDenseEntities() }

private fun Chunk.cleanDenseEntities(): Int {
    // 最终要移除的实体
    val willBeRemoved = entities.toMutableList()
    if (willBeRemoved.isEmpty()) return 0
    PL.debug { "" }
    val chunkInfo = info()
    PL.debug { "开始检测区块${chunkInfo}中的密集实体" }
    PL.buildDebug {
        append("所有实体共").append(willBeRemoved.size).append("个: [")
        willBeRemoved.info().entries.joinTo(this, ", ") { (k, v) -> "$k: $v" }
        append("]")
    }
    val settings = chunkCfg.settings
    // chunkCfg.settings.name == true 时清理被命名的生物, false -> 从列表中移除(不清理)
    if (!settings.name) willBeRemoved.removeIf { it.customName != null }
    // chunkCfg.settings.lead == true 时清理拴绳拴住的生物, false -> 从列表中移除(不清理)
    if (!settings.lead) willBeRemoved.removeIf { it is LivingEntity && it.isLeashed }
    // chunkCfg.settings.mount == true 时清理乘骑中的生物, false 从列表中移除(不清理)
    if (!settings.mount) willBeRemoved.removeIf { it.isInsideVehicle || it.passengers.isNotEmpty() }

    // 先按类型分组, 再让每条 limit 规则在"类型集合"上匹配,
    // 避免 规则数 × 实体数 的重复全量扫描
    val byType = willBeRemoved.groupBy { it.type.name }
        .mapValues { it.value.toMutableList() }
        .toMutableMap()
    val planned = mutableListOf<Entity>()

    chunkCfg.limit.forEach { (regex, limit) ->
        val matchedTypes = byType.keys.filter { it.matches(regex) }
        if (matchedTypes.isEmpty()) return@forEach
        val matches = matchedTypes.flatMap { byType[it] ?: emptyList() }
        val execute = matches.size > limit
        PL.debug { "检查区块($chunkInfo)的密集实体, 规则${regex}" }
        PL.buildDebug {
            append("匹配实体").append(matches.size).append("个(")
            if (execute) append("&c清理其中&a").append(matches.size - limit).append("个&b")
            else append("不清理")
            append("): ")
            matches.info().entries.joinTo(this, ", ", "[", "]") { (k, v) -> "$k: $v" }
        }

        // 不清理匹配数量未达阈值的
        if (!execute) return@forEach

        // 截取超出阈值的实体
        val toRemove = matches.subList(limit, matches.size).toList()
        planned.addAll(toRemove)
        // 已计划的实体不能再次被后续规则匹配
        toRemove.forEach { byType[it.type.name]?.remove(it) }
    }

    if (!chunkCfg.format.isNullOrBlank()) {
        val recv = Bukkit.getOnlinePlayers().filter { it.hasPermission("mcdrs.admin") }
        entities.asList().info().filter { it.value > chunkCfg.count }.forEach { (entity, count) ->
            val message = chunkCfg.format!!.placeholder(
                "chunk" to chunkInfo,
                "entity" to entity,
                "count" to count,
            )
            recv.forEach { PL.sendMsgWithPrefix(it, message) }
        }
    }

    planned.forEach(Entity::remove)
    return planned.size
}

fun Chunk.info() = "x: ${x * 16}..${x * 16 + 15}, z: ${z * 16}..${z * 16 + 15}"

private fun chunkWorlds() = Bukkit.getWorlds()
    .filterNot { chunkCfg.disableWorld.any { regex -> it.name matches regex } }

private fun logChunkStart(worlds: List<World>) {
    PL.debug { "开始进行密集实体检查" }
    PL.debug {
        buildString {
            append("启用密集实体检查的世界: [")
            worlds.joinTo(this, ", ", transform = World::getName)
            append("]")
        }
    }
    PL.debug { if (chunkCfg.settings.name) "清理被命名的生物" else "不清理被命名的生物" }
    PL.debug { if (chunkCfg.settings.lead) "清理拴绳拴住的生物" else "不清理拴绳拴住的生物" }
    PL.debug { if (chunkCfg.settings.mount) "清理乘骑中的生物" else "不清理乘骑中的生物" }
}
