package top.mcdrsclean.clean

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.entity.Item
import org.bukkit.inventory.meta.BookMeta
import top.mcdrsclean.PL
import top.mcdrsclean.config.Config
import top.mcdrsclean.util.isMatch
import top.mcdrsclean.util.notifyFinish
import top.mcdrsclean.util.removeEntitiesChunked

private inline val dropCfg get() = Config.config.drop
private inline val trashcanCfg get() = Config.config.trashcan


/**
 * 最近一次清理掉落物的数量
 */
var lastDrop = 0
    private set

/**
 * 扫描一个世界并算出需要清理的掉落物, 不修改世界.
 *
 * 把"算"和"删"拆开之后, 同步清理和分片清理可以复用同一套匹配逻辑.
 *
 * @return Pair(待清理的掉落物, 该世界掉落物总数)
 */
fun World.planDropClean(): Pair<List<Item>, Int> {
    val all = entities.filterIsInstance<Item>()
    val waitingForClean = all.toMutableList()

    PL.debug { "" }
    PL.debug { "开始清理世界${name}中的掉落物" }
    PL.buildDebug {
        append("世界").append(name).append("中的所有掉落物: [")
        all.groupingBy { it.itemStack.type.name }.eachCount()
            .entries.joinTo(this, ", ") { (k, v) -> "$k: $v" }
        append("]")
    }

    // 先用 hasItemMeta 短路: 普通物品直接跳过, 避免 getItemMeta 的深拷贝
    if (dropCfg.enchant) waitingForClean.removeIf {
        it.itemStack.hasItemMeta() && it.itemStack.itemMeta?.hasEnchants() == true
    }
    // dropCfg.writtenBook == true 时不清理写过的书(从列表中移除)
    if (dropCfg.writtenBook) waitingForClean.removeIf {
        it.itemStack.type == Material.WRITABLE_BOOK
                && it.itemStack.hasItemMeta()
                && (it.itemStack.itemMeta as? BookMeta)?.hasPages() == true
    }
    // dropCfg.lore == true 时不清理带描述的物品(从列表中移除)
    if (dropCfg.lore) waitingForClean.removeIf {
        it.itemStack.hasItemMeta() && it.itemStack.itemMeta?.hasLore() == true
    }

    // 按类型分组后匹配: 每条规则只需要比对类型, 而不是对每个物品做一次正则匹配
    val groups = waitingForClean.groupBy { it.itemStack.type.name }
    val black = dropCfg.black
    val toRemove = groups.entries.flatMap { (type, list) ->
        val matched = type.isMatch(dropCfg.match)
        // 黑名单: 命中规则的清理; 白名单: 未命中规则的清理
        val clean = if (black) matched != null else matched == null
        PL.buildDebug {
            if (!clean) append("不")
            append("清理").append(type).append("x").append(list.size)
            if (matched != null) append(", 命中规则: ").append(matched.pattern)
        }
        if (clean) list else emptyList()
    }

    PL.debug { "世界${name}掉落物清理计划完成(${toRemove.size}/${all.size})" }
    return toRemove to all.size
}

/**
 * 立即清理指定世界的掉落物(供命令使用).
 *
 * @return Pair(clean, all)
 */
fun World.cleanDrop(): Pair<Int, Int> {
    val (toRemove, total) = planDropClean()
    collectToTrashcan(toRemove)
    toRemove.forEach(Item::remove)
    return toRemove.size to total
}

/**
 * 分片清理全服掉落物(供定时任务使用), 避免一次性移除大量实体阻塞主线程.
 */
fun cleanDropChunked(onDone: () -> Unit = {}) {
    if (!dropCfg.enable) {
        PL.debug { "掉落物清理已禁用" }
        onDone()
        return
    }
    val worlds = dropWorlds()
    logDropStart(worlds)

    val start = System.currentTimeMillis()
    val plans = worlds.map { it.planDropClean() }
    val toRemove = plans.flatMap { it.first }
    val all = plans.sumOf { it.second }
    collectToTrashcan(toRemove)
    PL.debug { "掉落物清理计划完成, 待清理${toRemove.size}个, 耗时${System.currentTimeMillis() - start}ms" }

    removeEntitiesChunked(toRemove) {
        lastDrop = toRemove.size
        PL.debug { "掉落物清理共${lastDrop}个" }
        notifyFinish(dropCfg.finish, "clean" to lastDrop, "all" to all)
        onDone()
    }
}

/**
 * 同步清理全服掉落物(供命令使用).
 */
fun cleanDrop() {
    if (!dropCfg.enable) {
        PL.debug { "掉落物清理已禁用" }
        return
    }
    val worlds = dropWorlds()
    logDropStart(worlds)

    val start = System.currentTimeMillis()
    val result = worlds.map { it.cleanDrop() }
    lastDrop = result.sumOf { it.first }
    PL.debug { "掉落物清理共${lastDrop}个, 耗时${System.currentTimeMillis() - start}ms" }

    notifyFinish(dropCfg.finish, "clean" to lastDrop, "all" to result.sumOf { it.second })
}

private fun dropWorlds() = Bukkit.getWorlds()
    .filterNot { dropCfg.disableWorld.any { regex -> it.name matches regex } }

private fun logDropStart(worlds: List<World>) {
    PL.buildDebug {
        append("开始清理掉落物, 启用掉落物清理的世界: [")
        worlds.joinTo(this, ", ", transform = World::getName)
        append("]")
    }
    PL.debug { if (dropCfg.enchant) "不清理附魔的物品" else "清理附魔的物品" }
    PL.debug { if (dropCfg.writtenBook) "不清理成书" else "清理成书" }
}

/**
 * 把待清理的掉落物收进垃圾桶(按配置), 只改内存, 不碰世界.
 */
private fun collectToTrashcan(items: List<Item>) {
    if (items.isEmpty()) return
    if (!trashcanCfg.enable || !trashcanCfg.collect) return
    Trashcan.addItems(items.map(Item::getItemStack))
    Trashcan.update()
}
