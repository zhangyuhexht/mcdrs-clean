package top.mcdrsclean.clean



import org.bukkit.scheduler.BukkitTask

import top.mcdrsclean.PL

import top.mcdrsclean.config.Config

import top.mcdrsclean.config.Lang

import top.mcdrsclean.util.broadcastPlayersOnly

import top.mcdrsclean.util.noOnline

import top.mcdrsclean.util.noOnlineClean

import top.mcdrsclean.util.noOnlineMessage

import top.e404.eplugin.EPlugin.Companion.color



object Clean {

    private var task: BukkitTask? = null

    private val duration get() = Config.config.duration



    /**

     * start of the current cycle (wall-clock milliseconds).

     *

     * uses wall-clock time instead of counting ticks, because below 20 TPS twenty ticks take more than a second and the countdown drifts.

     */

    private var startAt = 0L



    /**

     * elapsed seconds, refreshed from the wall clock every 20 ticks

     */

    var count = 0L

        private set



    /**

     * whether a batched cleanup is still running, so a new cycle cannot start before the last one finishes

     */

    private var cleaning = false



    fun schedule() {

        count = 0

        cleaning = false

        task?.cancel()



        val total = duration

        if (total <= 0) {

            PL.warn("cleanup interval (duration) must be greater than 0, currently $total, cleanup task skipped")

            return

        }

        // cleanup task

        PL.info("&fCleanup task scheduled, interval ${total}s")

        Config.config.message.forEach { (delay, message) ->

            if (delay > total) PL.warn(Lang["warn.out_of_range", "message" to message, "duration" to total])

            else PL.info("&fcountdown reminder at ${delay}s before cleanup: ${message.color}")

        }



        startAt = System.currentTimeMillis()

        task = PL.runTaskTimer(20, 20) {

            val elapsed = (System.currentTimeMillis() - startAt) / 1000

            count = elapsed

            val remaining = total - elapsed

            if (remaining >= 0 && (!noOnline || noOnlineMessage)) {

                Config.config.message[remaining]?.let { broadcastPlayersOnly(it) }

            }

            if (remaining <= 0) {

                startAt = System.currentTimeMillis()

                count = 0

                if (!cleaning && (!noOnline || noOnlineClean)) {

                    cleaning = true

                    cleanChunked { cleaning = false }

                }

            }

        }

    }



    /**

     * batched cleanup of the whole server: dropped items -> living entities -> dense entities, in order.

     *

     * the per-tick batch size comes from batch_size in config.yml, so large removals do not block the main thread.

     */

    fun cleanChunked(onDone: () -> Unit = {}) {

        cleanDropChunked {

            cleanLivingChunked {

                cleanDenseEntitiesChunked {

                    onDone()

                }

            }

        }

    }



    /**

     * synchronous full-server cleanup (used by commands, blocks the main thread on large datasets).

     */

    fun clean() {

        cleanDrop()

        cleanLiving()

        cleanDenseEntities()

    }

}

