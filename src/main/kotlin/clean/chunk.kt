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

 * number of dense entities removed in the last chunk cleanup

 */

var lastChunk = 0

    private set



/**

 * synchronous dense entity cleanup in all worlds (used by commands).

 */

fun cleanDenseEntities() {

    if (!chunkCfg.enable) {

        PL.debug { "dense entity cleanup is disabled" }

        return

    }

    val worlds = chunkWorlds()

    logChunkStart(worlds)



    val start = System.currentTimeMillis()

    lastChunk = worlds.sumOf { it.cleanChunkDenseEntities() }

    PL.debug { "dense entity cleanup removed ${lastChunk} entities in  in ${System.currentTimeMillis() - start}ms" }



    notifyFinish(chunkCfg.finish, "clean" to lastChunk)

}



/**

 * batched dense entity cleanup in all worlds (used by scheduled tasks).

 *

 * dense entity checks are chunk-based, so advance N chunks per tick,

 * spreading out the scan cost too, instead of scanning the whole map before deleting.

 */

fun cleanDenseEntitiesChunked(onDone: () -> Unit = {}) {

    if (!chunkCfg.enable) {

        PL.debug { "dense entity cleanup is disabled" }

        onDone()

        return

    }

    val worlds = chunkWorlds()

    logChunkStart(worlds)



    val start = System.currentTimeMillis()

    // snapshot the loaded chunks and recheck isLoaded while processing, since chunks may unload during cleanup

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

            PL.debug { "dense entity cleanup removed ${lastChunk} entities in  in ${System.currentTimeMillis() - start}ms" }

            notifyFinish(chunkCfg.finish, "clean" to lastChunk)

            onDone()

        }

    }

}



/**

 * clean dense entities in loaded chunks of a world (synchronous, used by commands).

 *

 * @return number of entities removed

 */

fun World.cleanChunkDenseEntities() = loadedChunks.sumOf { it.cleanDenseEntities() }



private fun Chunk.cleanDenseEntities(): Int {

    // entities that will actually be removed

    val willBeRemoved = entities.toMutableList()

    if (willBeRemoved.isEmpty()) return 0

    PL.debug { "" }

    val chunkInfo = info()

    PL.debug { "checking dense entities in chunk${chunkInfo}" }

    PL.buildDebug {

        append("all entities (").append(willBeRemoved.size).append("): [")

        willBeRemoved.info().entries.joinTo(this, ", ") { (k, v) -> "$k: $v" }

        append("]")

    }

    val settings = chunkCfg.settings

    // chunkCfg.settings.name == true removes named entities; false drops them from the list

    if (!settings.name) willBeRemoved.removeIf { it.customName != null }

    // chunkCfg.settings.lead == true removes leashed entities; false drops them from the list

    if (!settings.lead) willBeRemoved.removeIf { it is LivingEntity && it.isLeashed }

    // chunkCfg.settings.mount == true removes riding entities; false drops them from the list

    if (!settings.mount) willBeRemoved.removeIf { it.isInsideVehicle || it.passengers.isNotEmpty() }



    // group by type first, then match each limit rule against the type set,

    // avoiding a full rescan for every rule × entity

    val byType = willBeRemoved.groupBy { it.type.name }

        .mapValues { it.value.toMutableList() }

        .toMutableMap()

    val planned = mutableListOf<Entity>()



    chunkCfg.limit.forEach { (regex, limit) ->

        val matchedTypes = byType.keys.filter { it.matches(regex) }

        if (matchedTypes.isEmpty()) return@forEach

        val matches = matchedTypes.flatMap { byType[it] ?: emptyList() }

        val execute = matches.size > limit

        PL.debug { "checking chunk($chunkInfo)dense entities, rule${regex}" }

        PL.buildDebug {

            append("Matched ").append(matches.size).append(" (")

            if (execute) append("&cremoving &a").append(matches.size - limit).append("&b ")

            else append("no removal")

            append("): ")

            matches.info().entries.joinTo(this, ", ", "[", "]") { (k, v) -> "$k: $v" }

        }



        // skip rules whose match count is below the threshold

        if (!execute) return@forEach



        // take only the entities above the threshold

        val toRemove = matches.subList(limit, matches.size).toList()

        planned.addAll(toRemove)

        // already-planned entities cannot be matched again by later rules

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

    PL.debug { "starting dense entity check" }

    PL.debug {

        buildString {

            append("worlds with dense entity checks enabled: [")

            worlds.joinTo(this, ", ", transform = World::getName)

            append("]")

        }

    }

    PL.debug { if (chunkCfg.settings.name) "removing named entities" else "keeping named entities" }

    PL.debug { if (chunkCfg.settings.lead) "removing leashed entities" else "keeping leashed entities" }

    PL.debug { if (chunkCfg.settings.mount) "removing riding entities" else "keeping riding entities" }

}

