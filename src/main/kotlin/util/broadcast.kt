package top.mcdrsclean.util

import org.bukkit.Bukkit
import top.mcdrsclean.PL
import top.mcdrsclean.config.Config
import top.e404.eplugin.EPlugin.Companion.placeholder

/**
 * 向所有在线玩家广播消息, 默认不写入后台(控制台/日志).
 *
 * 上游 EPlugin#broadcastMsg 的第一行是 info(message), 会把每一条广播都打到后台.
 * 清理倒计时、清理结果、垃圾桶清空这类周期性消息因此不断刷屏, 也白白占用日志空间.
 * 这里改为只发给在线玩家(消息内容、颜色、前缀与原来完全一致);
 * 若确实需要在后台保留广播, 把 config.yml 的 console_broadcast 设为 true 即可.
 */
fun broadcastPlayersOnly(message: String) {
    if (Config.config.consoleBroadcast) PL.info(message)
    Bukkit.getOnlinePlayers().forEach { PL.sendMsgWithPrefix(it, message) }
}

/**
 * 广播清理结果.
 *
 * 统一处理两件容易写重复的事:
 * 1. 模板为空则不广播;
 * 2. 无在线玩家且 no_online.message 为 false 时不广播.
 */
fun notifyFinish(template: String, vararg placeholders: Pair<String, Any?>) {
    if (template.isBlank()) return
    if (noOnline && !noOnlineMessage) return
    broadcastPlayersOnly(template.placeholder(*placeholders))
}
