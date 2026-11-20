package top.mcdrsclean.clean



import org.bukkit.entity.Player

import org.bukkit.event.EventHandler

import org.bukkit.event.inventory.InventoryCloseEvent

import org.bukkit.inventory.ItemStack

import org.bukkit.scheduler.BukkitTask

import top.mcdrsclean.PL

import top.mcdrsclean.config.Config

import top.mcdrsclean.config.Lang

import top.mcdrsclean.menu.MenuManager

import top.mcdrsclean.util.broadcastPlayersOnly

import top.mcdrsclean.menu.trashcan.TrashInfo

import top.mcdrsclean.menu.trashcan.TrashcanMenu

import top.e404.eplugin.listener.EListener



object Trashcan : EListener(PL) {

    /**

     * trash can items and counts; ItemStack amount is unused, the value is authoritative

     */

    val trashData = mutableMapOf<ItemSign, TrashInfo>()



    val trashValues = mutableListOf<TrashInfo>()



    private val openTrash = mutableMapOf<Player, TrashcanMenu>()



    var task: BukkitTask? = null



    fun cleanTrash(){

        Config.plugin.debug { "clearing trash can" }

        trashData.clear()

        trashValues.clear()

        broadcastPlayersOnly(Lang["command.trash_clean_done"])

    }



    /**

     * seconds until the trash can is cleared

     */

    var countdown = 0L



    fun schedule() {

        task?.cancel()

        val duration = Config.config.trashcan.duration

        if (duration == null) {

            task = null

            return

        }

        if (!Config.config.trashcan.enable) return

        countdown = duration

        task = Config.plugin.runTaskTimer(20, 20) {

            countdown--

            if (countdown <= 0L) {

                countdown = duration

                cleanTrash()

                update()

            }

        }

    }



    fun ItemStack.sign() = ItemSign(this)

    class ItemSign(val item: ItemStack) {

        override fun equals(other: Any?): Boolean {

            if (other == null) return false

            if (other !is ItemSign) return false

            return item.isSimilar(other.item)

        }



        // equals uses isSimilar (ignores amount), so hashCode must not include amount either.

        // ItemMeta#hashCode already covers durability, enchantments, names and all other metadata; the old code also mixed in

        // the deprecated ItemStack#getDurability, which is unreliable under the 1.20.5+ data component model,

        // which could treat items with different durability as the same item.

        override fun hashCode(): Int {

            var hash = 1

            hash = hash * 31 + item.type.hashCode()

            if (item.hasItemMeta()) hash = hash * 31 + item.itemMeta.hashCode()

            return hash

        }

    }



    @EventHandler

    fun InventoryCloseEvent.onEvent() {

        openTrash.remove(player)

    }



    fun open(player: Player) {

        val menu = TrashcanMenu()

        MenuManager.openMenu(menu, player)

        openTrash[player] = menu

    }



    fun addItems(items: Collection<ItemStack>) {

        for (item in items) {

            val sign = item.sign()

            val exists = trashData[sign]

            if (exists != null) {

                exists.amount += item.amount

                continue

            }

            val info = TrashInfo(item, item.amount)

            trashData[sign] = info

        }

        trashValues.clear()

        trashValues.addAll(trashData.values)

        openTrash.values.forEach { it.zone.update() }

    }



    fun addItem(item: ItemStack) {

        val sign = item.sign()

        val exists = trashData[sign]

        if (exists != null) {

            exists.amount += item.amount

            openTrash.values.forEach { it.zone.update() }

            return

        }

        val info = TrashInfo(item, item.amount)

        trashData[sign] = info

        trashValues.clear()

        trashValues.addAll(trashData.values)

        openTrash.values.forEach { it.zone.update() }

    }



    fun update() {

        plugin.debug { "updating the trash can menu for all players" }

        openTrash.entries.forEach { (player, menu) ->

            plugin.debug { "updating the trash can menu for player ${player.name}" }

            menu.updateIcon()

        }

    }

}