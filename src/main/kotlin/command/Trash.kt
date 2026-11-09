package top.mcdrsclean.command

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import top.mcdrsclean.PL
import top.mcdrsclean.clean.Trashcan
import top.mcdrsclean.config.Config
import top.mcdrsclean.config.Lang
import top.e404.eplugin.command.ECommand

object Trash : ECommand(
    PL,
    "trash",
    "(?i)t|trash",
    true,
    "mcdrs.trash"
) {
    override val usage get() = Lang["command.usage.trash"]

    override fun onCommand(sender: CommandSender, args: Array<out String>) {
        sender as Player
        if (!Config.config.trashcan.enable) {
            plugin.sendMsgWithPrefix(sender, Lang["command.trash_disable"])
            return
        }
        Trashcan.open(sender)
        plugin.sendMsgWithPrefix(sender, Lang["command.trash_open"])
    }
}
