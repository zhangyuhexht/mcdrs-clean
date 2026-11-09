package top.mcdrsclean.command

import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import top.mcdrsclean.PL
import top.mcdrsclean.config.Config
import top.mcdrsclean.config.Lang
import top.mcdrsclean.menu.MenuManager
import top.mcdrsclean.menu.dense.DenseMenu
import top.mcdrsclean.menu.dense.EntityInfo
import top.e404.eplugin.command.ECommand

object Show : ECommand(
    PL,
    "show",
    "(?i)s|show",
    true,
    "mcdrs.admin"
) {
    override val usage get() = Lang["command.usage.show"]

    override fun onCommand(sender: CommandSender, args: Array<out String>) {
        sender as Player
        plugin.runTaskAsync {
            val data = Bukkit.getServer().worlds.flatMap { world ->
                world.loadedChunks.toList()
            }.flatMap { chunk ->
                chunk.entities.groupBy(Entity::getType).filter { (_, list) ->
                    list.size > Config.config.chunk.count
                }.map { (type, list) ->
                    EntityInfo(type, list.size, chunk)
                }
            }.sortedByDescending { it.amount }.toMutableList()
            plugin.runTask {
                MenuManager.openMenu(DenseMenu(data), sender)
            }
        }
    }
}
