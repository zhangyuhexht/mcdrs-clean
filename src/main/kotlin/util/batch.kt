package top.mcdrsclean.util

import org.bukkit.entity.Entity
import top.mcdrsclean.PL
import top.mcdrsclean.config.Config

/**
 * 分 tick 移除实体, 把工作量摊到多个 tick, 避免一次性移除大量实体长时间阻塞主线程.
 *
 * 注意: 实体操作仍然在主线程执行, 这里只是"分批"而不是"异步".
 * 每 tick 的批量由 config.yml 的 batch_size 控制.
 *
 * @param entities 待移除的实体(调用方需保证是当前已加载的实体)
 * @param onDone 全部移除后的回调(主线程)
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
