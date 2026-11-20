package top.mcdrsclean.menu



import org.bukkit.Location

import org.bukkit.entity.Player

import org.bukkit.event.EventHandler

import org.bukkit.event.player.PlayerQuitEvent

import org.bukkit.scheduler.BukkitTask

import top.mcdrsclean.PL

import top.e404.eplugin.menu.EMenuManager



object MenuManager : EMenuManager(PL) {

    val temps = mutableMapOf<Player, Temp>()



    @EventHandler

    fun PlayerQuitEvent.onEvent() {

        // leaving within 30s teleports the player back to their previous position

        temps.remove(player)?.run {

            task.cancel()

            player.teleport(location)

        }

    }

}





data class Temp(

    val player: Player,

    val location: Location,

    val task: BukkitTask

)

