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

 * number of living entities removed in the last cleanup

 */

var lastLiving = 0

    private set



/**

 * scan a world and work out which living entities to remove, without modifying the world.

 *

 * @return Pair(entities to remove, total living entities in the world)

 */

fun World.planLivingClean(): Pair<List<LivingEntity>, Int> {

    val all = livingEntities.filterNot { it is Player }

    val total = all.size

    val waitingForClean = all.toMutableList()



    PL.debug { "" }

    PL.debug { "Starting living entity cleanup in world $name" }

    PL.buildDebug {

        append("all entities (").append(total).append("): [")

        all.info().entries.joinTo(this, ", ") { (k, v) -> "$k: $v" }

        append("]")

    }



    // livingCfg.settings.name == false keeps named entities (removed from the list)

    if (!livingCfg.settings.name) waitingForClean.removeIf { it.customName != null }

    // livingCfg.settings.lead == false keeps leashed entities (removed from the list)

    if (!livingCfg.settings.lead) waitingForClean.removeIf { it.isLeashed }

    // livingCfg.settings.mount == false keeps riding entities (removed from the list)

    if (!livingCfg.settings.mount) waitingForClean.removeIf { it.isInsideVehicle || it.passengers.isNotEmpty() }



    // group by type before matching, so each rule compares types instead of running a regex per entity

    val groups = waitingForClean.groupBy { it.type.name }

    val black = livingCfg.black

    val toRemove = groups.entries.flatMap { (type, list) ->

        val matched = type.isMatch(livingCfg.match)

        // blacklist removes matching entries; whitelist removes non-matching ones

        val clean = if (black) matched != null else matched == null

        PL.buildDebug {

            if (!clean) append("not ")

            append("removing ").append(type).append("x").append(list.size)

            if (matched != null) append(", rule: ").append(matched.pattern)

        }

        if (clean) list else emptyList()

    }



    PL.debug { "Living entity cleanup plan for $name complete (${toRemove.size}/$total)" }

    return toRemove to total

}



/**

 * immediately clean living entities in a world (used by commands).

 *

 * @return Pair(clean, all)

 */

fun World.cleanLiving(): Pair<Int, Int> {

    val (toRemove, total) = planLivingClean()

    toRemove.forEach(LivingEntity::remove)

    return toRemove.size to total

}



/**

 * batched living entity cleanup for the whole server (used by scheduled tasks), avoiding a main-thread stall.

 */

fun cleanLivingChunked(onDone: () -> Unit = {}) {

    if (!livingCfg.enable) {

        PL.debug { "living entity cleanup is disabled" }

        onDone()

        return

    }

    val worlds = livingWorlds()

    logLivingStart(worlds)



    val start = System.currentTimeMillis()

    val plans = worlds.map { it.planLivingClean() }

    val toRemove = plans.flatMap { it.first }

    val all = plans.sumOf { it.second }

    PL.debug { "living entity cleanup plan complete, to remove ${toRemove.size} entities in  in ${System.currentTimeMillis() - start}ms" }



    removeEntitiesChunked(toRemove) {

        lastLiving = toRemove.size

        PL.debug { "living entity cleanup removed ${lastLiving} entities" }

        notifyFinish(livingCfg.finish, "clean" to lastLiving, "all" to all)

        onDone()

    }

}



/**

 * synchronous full-server living entity cleanup (used by commands).

 */

fun cleanLiving() {

    if (!livingCfg.enable) {

        PL.debug { "living entity cleanup is disabled" }

        return

    }

    val worlds = livingWorlds()

    logLivingStart(worlds)



    val start = System.currentTimeMillis()

    val result = worlds.map { it.cleanLiving() }

    lastLiving = result.sumOf { it.first }

    PL.debug { "living entity cleanup removed ${lastLiving} entities in  in ${System.currentTimeMillis() - start}ms" }



    notifyFinish(livingCfg.finish, "clean" to lastLiving, "all" to result.sumOf { it.second })

}



private fun livingWorlds() = Bukkit.getWorlds()

    .filterNot { livingCfg.disableWorld.any { regex -> it.name matches regex } }



private fun logLivingStart(worlds: List<World>) {

    PL.buildDebug {

        append("starting living entity cleanup, worlds enabled: [")

        worlds.joinTo(this, ", ", transform = World::getName)

        append("]")

    }

    PL.debug { if (livingCfg.settings.name) "removing named entities" else "keeping named entities" }

    PL.debug { if (livingCfg.settings.lead) "removing leashed entities" else "keeping leashed entities" }

    PL.debug { if (livingCfg.settings.mount) "removing riding entities" else "keeping riding entities" }

}

