package top.mcdrsclean.util



import org.bukkit.entity.Entity

import top.mcdrsclean.PL

import top.mcdrsclean.config.Config



/**

 * remove entities across ticks to spread the work and avoid a long main-thread stall.

 *

 * note: entity operations still run on the main thread; this batches the work, it is not async.

 * the per-tick batch size comes from batch_size in config.yml.

 *

 * @param entities entities to remove (caller must ensure they are currently loaded)

 * @param onDone callback after all entities are removed (main thread)

 */

fun removeEntitiesChunked(entities: List<Entity>, onDone: () -> Unit = {}) {

    if (entities.isEmpty()) {

        onDone()

        return

    }

    val batchSize = Config.config.batchSize.coerceAtLeast(1)

    var index = 0

    PL.runTaskTimer(1, 1) { runnable ->

        val end = (index + batchSize).coerceAtMost(entities.size)

        for (i in index until end) entities[i].remove()

        index = end

        if (index >= entities.size) {

            runnable.cancel()

            onDone()

        }

    }

}

