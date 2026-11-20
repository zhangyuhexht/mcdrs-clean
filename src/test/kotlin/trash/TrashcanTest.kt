package trash



import be.seeseemelk.mockbukkit.inventory.SimpleInventoryViewMock

import org.bukkit.Bukkit

import org.bukkit.Location

import org.bukkit.Material

import org.bukkit.event.inventory.ClickType

import org.bukkit.event.inventory.InventoryAction

import org.bukkit.event.inventory.InventoryClickEvent

import org.bukkit.event.inventory.InventoryType

import org.bukkit.inventory.ItemStack

import org.junit.jupiter.api.*

import top.mcdrsclean.clean.Trashcan

import top.mcdrsclean.clean.cleanDrop

import top.mcdrsclean.config.Config

import top.mcdrsclean.menu.MenuManager

import top.mcdrsclean.test.*

import top.e404.eplugin.menu.menu.InventoryMenu

import kotlin.test.assertNotNull



abstract class TrashcanTest {



    @BeforeEach

    fun setup() {

        resetConfig()

        Config.config.trashcan.enable = true

        Config.config.trashcan.duration = null



        Trashcan.trashData.clear()

        Trashcan.trashValues.clear()

    }



    @AfterEach

    fun cleanUp() {

        player.inventory.clear()

        MenuManager.closeMenu(player)

        Trashcan.trashData.clear()

        Trashcan.trashValues.clear()

    }



    @Nested

    @DisplayName("collect cleaned drops into the trash can")

    inner class TestCollectItemWhenClean {

        @Test

        @DisplayName("enabled")

        fun enable() {

            Config.config.drop.enable = true

            Config.config.drop.match = mutableListOf(Regex(".*"))

            Config.config.trashcan.collect = true



            val chunk = world.getChunkAt(0, 0)

            chunk.load()

            val location = Location(world, 8.0, 8.0, 8.0)

            val amount = 64

            world.dropItem(location, ItemStack(Material.STONE, amount))



            cleanDrop()



            assert(Trashcan.trashValues.size == 1) { "cleaned drops were not added to the trash can\n$consoleOut" }

            assert(Trashcan.trashValues[0].amount == amount) { "wrong item count in the trash can\n$consoleOut" }

        }



        @Test

        @DisplayName("disabled")

        fun disable() {

            Config.config.drop.enable = true

            Config.config.drop.match = mutableListOf(Regex(".*"))

            Config.config.trashcan.collect = false



            val chunk = world.getChunkAt(0, 0)

            chunk.load()

            val location = Location(world, 8.0, 8.0, 8.0)

            val amount = 64

            world.dropItem(location, ItemStack(Material.STONE, amount))



            cleanDrop()



            assert(Trashcan.trashValues.isEmpty()) {

                "cleaned drops should not be added to the trash can\n$consoleOut"

            }

        }

    }



    private fun leftClick(slot: Int): Pair<InventoryMenu, InventoryClickEvent> {

        Trashcan.open(player)

        val menu = MenuManager.menus[player]!!

        val inventoryClickEvent = InventoryClickEvent(

            SimpleInventoryViewMock(

                player,

                menu.inv,

                player.inventory,

                InventoryType.CHEST

            ),

            InventoryType.SlotType.CONTAINER,

            slot,

            ClickType.LEFT,

            InventoryAction.COLLECT_TO_CURSOR

        )

        Bukkit.getPluginManager().callEvent(inventoryClickEvent)

        return menu to inventoryClickEvent

    }



    private fun rightClick(slot: Int): Pair<InventoryMenu, InventoryClickEvent> {

        Trashcan.open(player)

        val menu = MenuManager.menus[player]!!

        val inventoryClickEvent = InventoryClickEvent(

            SimpleInventoryViewMock(

                player,

                menu.inv,

                player.inventory,

                InventoryType.CHEST

            ),

            InventoryType.SlotType.CONTAINER,

            slot,

            ClickType.RIGHT,

            InventoryAction.PICKUP_HALF

        )

        Bukkit.getPluginManager().callEvent(inventoryClickEvent)

        return menu to inventoryClickEvent

    }



    private fun shiftLeftClick(slot: Int): Pair<InventoryMenu, InventoryClickEvent> {

        Trashcan.open(player)

        val menu = MenuManager.menus[player]!!

        val inventoryClickEvent = InventoryClickEvent(

            SimpleInventoryViewMock(

                player,

                menu.inv,

                player.inventory,

                InventoryType.CHEST

            ),

            InventoryType.SlotType.CONTAINER,

            slot,

            ClickType.SHIFT_LEFT,

            InventoryAction.MOVE_TO_OTHER_INVENTORY

        )

        Bukkit.getPluginManager().callEvent(inventoryClickEvent)

        return menu to inventoryClickEvent

    }



    @Nested

    @DisplayName("take items from the trash can")

    inner class TestTakeItemFromTrashcan {

        @Test

        @DisplayName("take one")

        fun takeOne() {

            Trashcan.addItem(ItemStack(Material.STONE, 2))



            val (menu, event) = leftClick(0)

            assert(event.isCancelled) { "the menu click should be cancelled\n$consoleOut" }

            val item = menu.inv.getItem(0)

            assertNotNull(item) { "one item should remain in the menu after pickup\n$consoleOut" }

            assert(item.type != Material.AIR) { "the item should not be empty\n$consoleOut" }

            assert(Trashcan.trashValues.size == 1) { "the trash can should contain one item type\n$consoleOut" }

            assert(Trashcan.trashValues.size == 1) { "the trash can should contain one item type\n$consoleOut" }

            assert(Trashcan.trashValues[0].amount == 1) { "one item should remain in the trash can\n$consoleOut" }



            val playerItem = player.inventory.getItem(0)

            assertNotNull(playerItem) { "the player should have the item after pickup\n$consoleOut" }

            assert(playerItem.type == Material.STONE) { "the item type should match\n$consoleOut" }

            assert(playerItem.amount == 1) { "the item amount should be 1\n$consoleOut" }

        }



        @Test

        @DisplayName("take half")

        fun takeHalf() {

            Trashcan.addItem(ItemStack(Material.STONE, 64))



            val (menu, event) = rightClick(0)

            assert(event.isCancelled) { "the menu click should be cancelled\n$consoleOut" }

            val item = menu.inv.getItem(0)

            assertNotNull(item) { "the menu item should not be empty\n$consoleOut" }

            assert(item.type == Material.STONE) { "the menu item type should be unchanged\n$consoleOut" }

            assert(Trashcan.trashValues.size == 1) { "one item type should remain in the trash can\n$consoleOut" }

            assert(Trashcan.trashValues[0].amount == 32) { "the trash can should contain 32 items\n$consoleOut" }

            val playerItem = player.inventory.getItem(0)

            assertNotNull(playerItem) { "the inventory item should not be empty\n$consoleOut" }

            assert(playerItem.type == Material.STONE) { "the inventory item type should match\n$consoleOut" }

            assert(playerItem.amount == 32) { "the inventory should contain 32 items\n$consoleOut" }

        }



        @Test

        @DisplayName("take a stack")

        fun takeFullStack() {

            Trashcan.addItems(listOf(ItemStack(Material.STONE, 64), ItemStack(Material.STONE, 64)))



            val (menu, event) = shiftLeftClick(0)

            assert(event.isCancelled) { "the menu click should be cancelled\n$consoleOut" }

            val item = menu.inv.getItem(0)

            assertNotNull(item) { "the menu item should not be empty\n$consoleOut" }

            assert(item.type == Material.STONE) { "the menu item type should be unchanged\n${Trashcan.trashValues[0]}\\n$consoleOut" }

            assert(Trashcan.trashValues.size == 1) { "one item type should remain in the trash can\n${Trashcan.trashValues[0]}\\n$consoleOut" }

            assert(Trashcan.trashValues[0].amount == 64) { "the trash can should contain 64 items\n${Trashcan.trashValues[0]}\n$consoleOut" }

            val playerItem = player.inventory.getItem(0)

            assertNotNull(playerItem) { "the inventory item should not be empty\n$consoleOut" }

            assert(playerItem.type == Material.STONE) { "the inventory item type should match\n${playerItem}\n$consoleOut" }

            assert(playerItem.amount == 64) { "the inventory should contain 64 items\n${playerItem}\n$consoleOut" }

        }



        @Test

        @DisplayName("take all")

        fun takeAll() {

            Trashcan.addItems(listOf(ItemStack(Material.STONE, 64)))



            val (menu, event) = shiftLeftClick(0)

            assert(event.isCancelled) { "the menu click should be cancelled\n$consoleOut" }

            val item = menu.inv.getItem(0)

            assert(item == null || item.type == Material.AIR) { "the trash can menu should be empty\n$consoleOut" }

            assert(Trashcan.trashValues.isEmpty()) { "the trash can should be empty\n$consoleOut" }

            val playerItem = player.inventory.getItem(0)

            assertNotNull(playerItem) { "the inventory item should not be empty\n$consoleOut" }

            assert(playerItem.type == Material.STONE) { "the inventory item type should match\n$consoleOut" }

            assert(playerItem.amount == 64) { "the inventory should contain 64 items\n$consoleOut" }

        }

    }



    @Nested

    @DisplayName("insert items into the trash can")

    inner class TestPutItemToTrashcan {

        @Test

        @DisplayName("insert one")

        fun putOne() {

            player.inventory.setItem(0, ItemStack(Material.STONE, 64))



            val (menu, event) = leftClick(81)

            assert(event.isCancelled) { "the menu click should be cancelled\n$consoleOut" }

            val item = menu.inv.getItem(0)

            assertNotNull(item) { "the menu item should not be empty\n$consoleOut" }

            assert(item.type == Material.STONE) { "the menu item type should be unchanged\n${Trashcan.trashValues[0]}\\n$consoleOut" }

            assert(Trashcan.trashValues.size == 1) { "one item type should remain in the trash can\n${Trashcan.trashValues[0]}\\n$consoleOut" }

            assert(Trashcan.trashValues[0].amount == 1) { "the trash can should contain one item\n${Trashcan.trashValues[0]}\n$consoleOut" }

            val playerItem = player.inventory.getItem(0)

            assertNotNull(playerItem) { "the inventory item should not be empty\n$consoleOut" }

            assert(playerItem.type == Material.STONE) { "the inventory item type should match\n${playerItem}\n$consoleOut" }

            assert(playerItem.amount == 63) { "the inventory should contain 63 items\n${playerItem}\n$consoleOut" }

        }



        @Test

        @DisplayName("insert half")

        fun putHalf() {

            player.inventory.setItem(0, ItemStack(Material.STONE, 64))



            val (menu, event) = rightClick(81)

            assert(event.isCancelled) { "the menu click should be cancelled\n$consoleOut" }

            val item = menu.inv.getItem(0)

            assertNotNull(item) { "the menu item should not be empty\n$consoleOut" }

            assert(item.type == Material.STONE) { "the menu item type should be unchanged\n${Trashcan.trashValues[0]}\\n$consoleOut" }

            assert(Trashcan.trashValues.size == 1) { "one item type should remain in the trash can\n${Trashcan.trashValues[0]}\\n$consoleOut" }

            assert(Trashcan.trashValues[0].amount == 32) { "the trash can should contain 32 items\n${Trashcan.trashValues[0]}\n$consoleOut" }

            val playerItem = player.inventory.getItem(0)

            assertNotNull(playerItem) { "the inventory item should not be empty\n$consoleOut" }

            assert(playerItem.type == Material.STONE) { "the inventory item type should match\n${playerItem}\n$consoleOut" }

            assert(playerItem.amount == 32) { "the inventory should contain 32 items\n${playerItem}\n$consoleOut" }

        }



        @Test

        @DisplayName("insert a stack")

        fun put() {

            player.inventory.setItem(0, ItemStack(Material.STONE, 64))

            player.inventory.setItem(1, ItemStack(Material.STONE, 64))



            val (menu, event) = shiftLeftClick(81)

            assert(event.isCancelled) { "the menu click should be cancelled\n$consoleOut" }

            val item = menu.inv.getItem(0)

            assertNotNull(item) { "the menu item should not be empty\n$consoleOut" }

            assert(item.type == Material.STONE) { "the menu item type should be unchanged\n${Trashcan.trashValues[0]}\\n$consoleOut" }

            assert(Trashcan.trashValues.size == 1) { "one item type should remain in the trash can\n${Trashcan.trashValues[0]}\\n$consoleOut" }

            assert(Trashcan.trashValues[0].amount == 64) { "the trash can should contain 64 items\n${Trashcan.trashValues[0]}\n$consoleOut" }

            val item1 = player.inventory.getItem(0)

            assert(item1 == null || item1.type == Material.AIR) {

                "slot 1 should be empty after inserting a stack\n$consoleOut"

            }

            val item2 = player.inventory.getItem(1)

            assert(item2 != null && item2.amount == 64) {

                "slot 2 should hold 64 after inserting a stack\n$consoleOut"

            }

        }

    }

}