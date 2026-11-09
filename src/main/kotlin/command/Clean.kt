package top.mcdrsclean.command

import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import top.mcdrsclean.PL
import top.mcdrsclean.clean.*
import top.mcdrsclean.clean.Clean
import top.mcdrsclean.clean.Trashcan.cleanTrash
import top.mcdrsclean.config.Lang
import top.e404.eplugin.command.ECommand

object Clean : ECommand(
    PL,
    "clean",
    "(?i)clean",
    false,
    "mcdrs.clean"
) {
    override val usage get() = Lang["command.usage.clean"]

    private val arg = listOf("entity", "drop", "chunk", "trash")

    /** 二次确认窗口(毫秒) */
    private const val CONFIRM_WINDOW = 10_000L

    /** 待确认的清理请求: 发送者名 -> 上次请求时间 */
    private val pending = mutableMapOf<String, Long>()

    /**
     * 全服范围的清理需要在窗口期内再次执行以确认, 避免误触清空全服.
     *
     * 第一次调用只记录时间并返回 false; 第二次在窗口内调用才返回 true.
     */
    private fun confirm(sender: CommandSender): Boolean {
        val now = System.currentTimeMillis()
        pending.entries.removeIf { now - it.value > CONFIRM_WINDOW }
        val last = pending[sender.name]
        pending[sender.name] = now
        return last != null && now - last <= CONFIRM_WINDOW
    }

    override fun onTabComplete(
        sender: CommandSender,
        args: Array<out String>,
        complete: MutableList<String>,
    ) {
        when (args.size) {
            2 -> complete.addAll(arg)
            3 -> Bukkit.getWorlds().forEach { complete.add(it.name) }
        }
    }

    override fun onCommand(
        sender: CommandSender,
        args: Array<out String>,
    ) {
        when (args.size) {
            1 -> {
                if (!confirm(sender)) {
                    PL.sendMsgWithPrefix(sender, Lang["command.clean_confirm"])
                    return
                }
                PL.sendMsgWithPrefix(sender, Lang["command.clean_started"])
                Clean.cleanChunked {
                    PL.sendMsgWithPrefix(sender, Lang["command.clean_done_all"])
                }
            }

            2 -> {
                if (!confirm(sender)) {
                    PL.sendMsgWithPrefix(sender, Lang["command.clean_confirm"])
                    return
                }
                when (args[1].lowercase()) {
                    "e", "entity" -> cleanLivingChunked()
                    "d", "drop" -> cleanDropChunked()
                    "c", "chunk" -> cleanDenseEntitiesChunked()
                    "t", "trash" -> cleanTrash()
                    else -> sender.sendMessage(usage)
                }
            }

            3 -> {
                val world = Bukkit.getWorld(args[2])
                if (world == null) {
                    PL.sendMsgWithPrefix(sender, Lang["message.invalid_world", "world" to args[2]])
                    return
                }
                val count = when (args[1].lowercase()) {
                    "e", "entity" -> world.cleanLiving().run { "($first/$second)" }
                    "d", "drop" -> world.cleanDrop().run { "($first/$second)" }
                    "c", "chunk" -> world.cleanChunkDenseEntities()
                    else -> {
                        sender.sendMessage(usage)
                        return
                    }
                }
                PL.sendMsgWithPrefix(sender, Lang["command.clean_done", "count" to count])
            }

            else -> sender.sendMessage(usage)
        }
    }
}
