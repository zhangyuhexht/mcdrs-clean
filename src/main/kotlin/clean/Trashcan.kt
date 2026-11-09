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
     * 垃圾桶中的物品及其数量, ItemStack的数量没有作用, 以value的数量为准
     */
    val trashData = mutableMapOf<ItemSign, TrashInfo>()

    val trashValues = mutableListOf<TrashInfo>()

    private val openTrash = mutableMapOf<Player, TrashcanMenu>()

    var task: BukkitTask? = null

    fun cleanTrash(){
        Config.plugin.debug { "清空垃圾桶" }
        trashData.clear()
        trashValues.clear()
        broadcastPlayersOnly(Lang["command.trash_clean_done"])
    }

    /**
     * 垃圾桶清空倒计时, 单位秒
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

        // equals 用 isSimilar(忽略数量), hashCode 也不能包含数量.
        // ItemMeta#hashCode 已经覆盖耐久/附魔/名字等全部元数据; 旧实现额外混入
        // 已弃用的 ItemStack#getDurability, 在 1.20.5+ 的 data component 模型下不可靠,
        // 会让不同耐久的同类物品被误判为同一物品.
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
        plugin.debug { "更新全部玩家的公共垃圾桶菜单" }
        openTrash.entries.forEach { (player, menu) ->
            plugin.debug { "更新玩家${player.name}的公共垃圾桶菜单" }
            menu.updateIcon()
        }
    }
}