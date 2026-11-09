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
     * 本轮计时起点(墙钟毫秒).
     *
     * 用墙钟而不是累加 tick, 避免 TPS 低于 20 时 20 tick 实际超过 1 秒, 导致倒计时越走越慢.
     */
    private var startAt = 0L

    /**
     * 已过的秒数, 每 20 tick 按墙钟刷新一次
     */
    var count = 0L
        private set

    /**
     * 分片清理是否还在进行, 避免上一轮没清完又开下一轮
     */
    private var cleaning = false

    fun schedule() {
        count = 0
        cleaning = false
        task?.cancel()

        val total = duration
        if (total <= 0) {
            PL.warn("清理间隔(duration)必须大于 0, 当前为 $total, 已跳过清理任务")
            return
        }
        // 清理任务
        PL.info("&f设置清理任务, 间隔${total}秒")
        Config.config.message.forEach { (delay, message) ->
            if (delay > total) PL.warn(Lang["warn.out_of_range", "message" to message, "duration" to total])
            else PL.info("&f设置清理前${delay}秒提醒: ${message.color}")
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
     * 分片清理全服: 掉落物 -> 生物 -> 密集实体, 依次执行.
     *
     * 每 tick 的批量由 config.yml 的 batch_size 控制, 避免一次性移除大量实体阻塞主线程.
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
     * 同步清理全服(供命令使用, 数据量大时会占用主线程).
     */
    fun clean() {
        cleanDrop()
        cleanLiving()
        cleanDenseEntities()
    }
}
