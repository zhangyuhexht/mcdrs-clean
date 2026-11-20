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

 * number of dropped items removed in the last cleanup

 */

var lastDrop = 0

    private set



/**

 * scan a world and work out which dropped items to remove, without modifying the world.

 *

 * splitting planning from removal lets synchronous and batched cleanup share one matching path.

 *

 * @return Pair(items to remove, total dropped items in the world)

 */

fun World.planDropClean(): Pair<List<Item>, Int> {

    val all = entities.filterIsInstance<Item>()

    val waitingForClean = all.toMutableList()



    PL.debug { "" }

    PL.debug { "Starting dropped item cleanup in world $name" }

    PL.buildDebug {

        append("All dropped items in ").append(name).append(": [")

        all.groupingBy { it.itemStack.type.name }.eachCount()

            .entries.joinTo(this, ", ") { (k, v) -> "$k: $v" }

        append("]")

    }



    // short-circuit on hasItemMeta so plain items skip the deep copy from getItemMeta

    if (dropCfg.enchant) waitingForClean.removeIf {

        it.itemStack.hasItemMeta() && it.itemStack.itemMeta?.hasEnchants() == true

    }

    // dropCfg.writtenBook == true keeps written books (removed from the candidate list)

    if (dropCfg.writtenBook) waitingForClean.removeIf {

        it.itemStack.type == Material.WRITABLE_BOOK

                && it.itemStack.hasItemMeta()

                && (it.itemStack.itemMeta as? BookMeta)?.hasPages() == true

    }

    // dropCfg.lore == true keeps items with lore (removed from the candidate list)

    if (dropCfg.lore) waitingForClean.removeIf {

        it.itemStack.hasItemMeta() && it.itemStack.itemMeta?.hasLore() == true

    }



    // group by type before matching, so each rule compares types instead of running a regex per item

    val groups = waitingForClean.groupBy { it.itemStack.type.name }

    val black = dropCfg.black

    val toRemove = groups.entries.flatMap { (type, list) ->

        val matched = type.isMatch(dropCfg.match)

        // blacklist removes matching entries; whitelist removes non-matching ones

        val clean = if (black) matched != null else matched == null

        PL.buildDebug {

            if (!clean) append("not ")

            append("removing ").append(type).append("x").append(list.size)

            if (matched != null) append(", rule: ").append(matched.pattern)

        }

        if (clean) list else emptyList()

    }



    PL.debug { "Dropped item cleanup plan for $name complete (${toRemove.size}/${all.size})" }

    return toRemove to all.size

}



/**

 * immediately clean dropped items in a world (used by commands).

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

 * batched dropped item cleanup for the whole server (used by scheduled tasks), avoiding a main-thread stall.

 */

fun cleanDropChunked(onDone: () -> Unit = {}) {

    if (!dropCfg.enable) {

        PL.debug { "dropped item cleanup is disabled" }

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

    PL.debug { "dropped item cleanup plan complete, to remove ${toRemove.size} entities in  in ${System.currentTimeMillis() - start}ms" }



    removeEntitiesChunked(toRemove) {

        lastDrop = toRemove.size

        PL.debug { "dropped item cleanup removed ${lastDrop} entities" }

        notifyFinish(dropCfg.finish, "clean" to lastDrop, "all" to all)

        onDone()

    }

}



/**

 * synchronous full-server dropped item cleanup (used by commands).

 */

fun cleanDrop() {

    if (!dropCfg.enable) {

        PL.debug { "dropped item cleanup is disabled" }

        return

    }

    val worlds = dropWorlds()

    logDropStart(worlds)



    val start = System.currentTimeMillis()

    val result = worlds.map { it.cleanDrop() }

    lastDrop = result.sumOf { it.first }

    PL.debug { "dropped item cleanup removed ${lastDrop} entities in  in ${System.currentTimeMillis() - start}ms" }



    notifyFinish(dropCfg.finish, "clean" to lastDrop, "all" to result.sumOf { it.second })

}



private fun dropWorlds() = Bukkit.getWorlds()

    .filterNot { dropCfg.disableWorld.any { regex -> it.name matches regex } }



private fun logDropStart(worlds: List<World>) {

    PL.buildDebug {

        append("starting dropped item cleanup, worlds enabled: [")

        worlds.joinTo(this, ", ", transform = World::getName)

        append("]")

    }

    PL.debug { if (dropCfg.enchant) "keeping enchanted items" else "removing enchanted items" }

    PL.debug { if (dropCfg.writtenBook) "keeping written books" else "removing written books" }

}



/**

 * collect planned dropped items into the trash can (per config), in memory only.

 */

private fun collectToTrashcan(items: List<Item>) {

    if (items.isEmpty()) return

    if (!trashcanCfg.enable || !trashcanCfg.collect) return

    Trashcan.addItems(items.map(Item::getItemStack))

    Trashcan.update()

}

