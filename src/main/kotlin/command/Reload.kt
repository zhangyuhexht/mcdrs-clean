package top.mcdrsclean.command

import org.bukkit.command.CommandSender
import top.mcdrsclean.PL
import top.mcdrsclean.clean.Clean
import top.mcdrsclean.config.Config
import top.mcdrsclean.config.Lang
import top.e404.eplugin.command.ECommand

object Reload : ECommand(
    PL,
    "reload",
    "(?i)r|reload",
    false,
    "mcdrs.admin"
) {
    override val usage get() = Lang["command.usage.reload"]

    override fun onCommand(sender: CommandSender, args: Array<out String>) {
        plugin.runTaskAsync {
            Lang.load(sender)
            Config.load(sender)
            plugin.runTask {
                Clean.schedule()
                plugin.sendMsgWithPrefix(sender, Lang["command.reload_done"])
            }
        }
    }
}
