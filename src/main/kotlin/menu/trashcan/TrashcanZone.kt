package top.mcdrsclean.menu.trashcan



import org.bukkit.Material

import org.bukkit.Sound

import org.bukkit.entity.Player

import org.bukkit.event.inventory.ClickType

import org.bukkit.event.inventory.InventoryClickEvent

import org.bukkit.inventory.ItemStack

import top.mcdrsclean.PL

import top.mcdrsclean.clean.Trashcan

import top.mcdrsclean.clean.Trashcan.sign

import top.e404.eplugin.menu.zone.MenuButtonZone

import top.e404.eplugin.util.emptyItem

import top.e404.eplugin.util.splitByPage

import kotlin.math.max

import kotlin.math.min



class TrashcanZone(

    override val menu: TrashcanMenu,

    override val data: MutableList<TrashInfo>

) : MenuButtonZone<TrashInfo>(menu, 0, 0, 9, 5, data) {

    override val inv = menu.inv



    override fun update() {

        if (page != 0 && page * pageSize >= data.size) page--

        val byPage = data.splitByPage(pageSize, page)

        for (i in 0 until pageSize) {

            val displayable = byPage.getOrNull(i)

            // clear slots that are not in the list

            if (displayable == null) {

                menu.inv.setItem(zone2menu(i)!!, emptyItem)

                continue

            }

            // update the icon

            displayable.update()

            // update the menu item

            menu.inv.setItem(zone2menu(i)!!, displayable.item)

        }

    }



    override fun onClick(menuIndex: Int, zoneIndex: Int, itemIndex: Int, event: InventoryClickEvent): Boolean {

        val player = event.whoClicked as Player

        val info = data.getOrNull(itemIndex) ?: return true

        // number of items planned for pickup

        val planTake = when (event.click) {

            // left-click: take one

            ClickType.LEFT, ClickType.DOUBLE_CLICK -> 1

            // shift + left-click: take a stack

            ClickType.SHIFT_LEFT -> info.item.maxStackSize

            // right-click: take half

            ClickType.RIGHT -> max(min(info.item.maxStackSize / 2, info.amount / 2), 1)

            // other clicks: take nothing

            else -> {

                player.playSound(player.location, Sound.ENTITY_BLAZE_DEATH, 1F, 1F)

                return true

            }

        }.let { min(it, info.amount) }



        // number of items to take

        var waitForTake = planTake

        PL.debug { "player ${player.name}plans to take ${info.origin.type}x${planTake}, , expected remaining ${info.amount - planTake}" }

        val maxStackSize = info.origin.type.maxStackSize

        // walk through the inventory

        for (i in (0 until 36)) {

            if (waitForTake == 0) break

            require(waitForTake > 0)



            val item = player.inventory.getItem(i)

            // empty slot

            if (item == null || item.type == Material.AIR) {

                val count = min(waitForTake, maxStackSize)

                waitForTake -= count

                player.inventory.setItem(i, info.origin.clone().apply { amount = count })

                continue

            }

            // different type

            if (!item.isSimilar(info.origin)) continue

            // full stack

            if (item.amount >= maxStackSize) continue

            // merge with the same type

            val count = min(waitForTake, maxStackSize - item.amount)

            waitForTake -= count

            player.inventory.setItem(i, item.clone().apply { amount += count })

        }



        // at this point total is the amount remaining in info



        // total amount taken

        val totalTake = planTake - waitForTake

        PL.debug { "player ${player.name}actually took ${info.origin.type}x${totalTake}, , actually remaining ${info.amount - totalTake}" }



        // remove the taken amount from the trash can

        info.amount -= totalTake

        require(info.amount >= 0)



        // all items were taken

        if (info.amount == 0) {

            Trashcan.trashData.remove(info.origin.sign())

            Trashcan.trashValues.removeAt(itemIndex)

        }



        // refresh the trash can

        Trashcan.update()

        return true

    }



    fun onClickSelfInv(event: InventoryClickEvent) {

        val player = event.whoClicked as Player

        event.isCancelled = true

        val clicked = event.currentItem

        if (clicked == null || clicked.type == Material.AIR) return

        // number of items to insert

        val count = when (event.click) {

            // left-click: insert one

            ClickType.LEFT, ClickType.DOUBLE_CLICK -> 1

            // shift + left-click: insert all

            ClickType.SHIFT_LEFT -> clicked.amount

            // right-click: insert half

            ClickType.RIGHT -> max(clicked.amount / 2, 1)



            // other clicks: insert nothing

            else -> {

                player.playSound(player.location, Sound.ENTITY_BLAZE_DEATH, 1F, 1F)

                return

            }

        }

        PL.debug { "player ${player.name}inserted ${clicked.type}x${count}, , remaining ${clicked.amount - count}" }

        // insert everything

        if (count == clicked.amount) {

            event.currentItem = emptyItem

            Trashcan.addItem(clicked)

            Trashcan.update()

            return

        }

        // insert a specific amount

        clicked.amount -= count

        event.currentItem = clicked

        Trashcan.addItem(clicked.clone().apply { amount = count })

        Trashcan.update()

    }



    /**

     * shift inserts the whole selected ItemStack into the trash can

     *

     * @param clicked clicked item

     * @return

     */

    fun onShiftPutin(clicked: ItemStack, event: InventoryClickEvent) {

        if (clicked.type == Material.AIR) return

        Trashcan.addItem(clicked)

        Trashcan.update()

        event.whoClicked.inventory.setItem(event.slot, emptyItem)

    }

}

