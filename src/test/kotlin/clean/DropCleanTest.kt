package top.mcdrsclean.test.clean



import org.bukkit.Location

import org.bukkit.Material

import org.bukkit.enchantments.Enchantment

import org.bukkit.entity.Entity

import org.bukkit.inventory.ItemStack

import org.bukkit.inventory.meta.BookMeta

import org.junit.jupiter.api.BeforeEach

import org.junit.jupiter.api.DisplayName

import org.junit.jupiter.api.Nested

import org.junit.jupiter.api.Test

import top.mcdrsclean.clean.cleanDrop

import top.mcdrsclean.clean.lastDrop

import top.mcdrsclean.config.Config

import top.mcdrsclean.test.consoleOut

import top.mcdrsclean.test.dropItems

import top.mcdrsclean.test.resetConfig

import top.mcdrsclean.test.world

import top.e404.eplugin.util.editItemMeta



abstract class DropCleanTest {



    @BeforeEach

    fun enable() {

        resetConfig()

        Config.config.drop.enable = true

    }



    @Nested

    @DisplayName("enchanted item cleanup")

    inner class TestCleanEntityWithEnchant {

        @Test

        @DisplayName("enabled")

        fun onEnable() {

            // true keeps enchanted items

            Config.config.drop.enchant = true

            Config.config.drop.match = mutableListOf(Regex("DIAMOND.*"))



            val location = Location(world, 8.0, 8.0, 8.0)

            val entities = world.dropItems(location, 2) { index ->

                ItemStack(Material.DIAMOND_CHESTPLATE).apply {

                    if (index == 0) addUnsafeEnchantment(Enchantment.DURABILITY, 1)

                }

            }



            cleanDrop()



            assert(entities.count(Entity::isValid) == 1) { "With enchanted item removal disabled, enchanted items should be kept\n$consoleOut" }

        }



        @Test

        @DisplayName("disabled")

        fun onDisable() {

            // true keeps enchanted items

            Config.config.drop.enchant = false

            Config.config.drop.match = mutableListOf(Regex("DIAMOND.*"))



            val location = Location(world, 8.0, 8.0, 8.0)

            val entities = world.dropItems(location, 2) { index ->

                ItemStack(Material.DIAMOND_CHESTPLATE).apply {

                    if (index == 0) addUnsafeEnchantment(Enchantment.DURABILITY, 1)

                }

            }



            cleanDrop()



            assert(entities.none(Entity::isValid)) { "With enchanted item removal disabled, matching items should all be removed\n$consoleOut" }

        }

    }



    @Nested

    @DisplayName("written book cleanup")

    inner class TestCleanWrittenBook {

        @Test

        @DisplayName("enabled")

        fun onEnable() {

            // true keeps written books

            Config.config.drop.writtenBook = true

            Config.config.drop.match = mutableListOf(Regex("WRITABLE_BOOK"))



            val location = Location(world, 8.0, 8.0, 8.0)

            val entities = world.dropItems(location, 2) { index ->

                ItemStack(Material.WRITABLE_BOOK).apply {

                    if (index == 0) editItemMeta {

                        this as BookMeta

                        @Suppress("DEPRECATION")

                        this.pages = mutableListOf("a", "b")

                    }

                }

            }



            cleanDrop()



            assert(entities.count(Entity::isValid) == 1) { "With written book removal disabled, written books should be kept\n$consoleOut" }

        }



        @Test

        @DisplayName("disabled")

        fun onDisable() {

            // true keeps written books

            Config.config.drop.writtenBook = false

            Config.config.drop.match = mutableListOf(Regex("WRITABLE_BOOK"))



            val location = Location(world, 8.0, 8.0, 8.0)

            val entities = world.dropItems(location, 2) { index ->

                ItemStack(Material.WRITABLE_BOOK).apply {

                    if (index == 0) editItemMeta {

                        this as BookMeta

                        @Suppress("DEPRECATION")

                        this.pages = mutableListOf("a", "b")

                    }

                }

            }



            cleanDrop()



            assert(entities.none(Entity::isValid)) { "With written book removal disabled, matching items should all be removed\n$consoleOut" }

        }

    }



    @Nested

    @DisplayName("blacklist / whitelist")

    inner class TestBlackWhiteList {

        @Test

        @DisplayName("blacklist")

        fun blackList() {

            // true uses blacklist mode (only matching items are removed)

            Config.config.drop.black = true

            Config.config.drop.match = mutableListOf(Regex("DIAMOND.*"))



            val location = Location(world, 8.0, 8.0, 8.0)

            val shouldClean = world.dropItems(location, 2) { index ->

                when (index) {

                    0 -> ItemStack(Material.DIAMOND_CHESTPLATE)

                    1 -> ItemStack(Material.DIAMOND_SWORD)

                    else -> throw Exception()

                }

            }

            val shouldNotClean = world.dropItems(location, 2) { index ->

                when (index) {

                    0 -> ItemStack(Material.STONE)

                    1 -> ItemStack(Material.BOW)

                    else -> throw Exception()

                }

            }



            cleanDrop()



            assert(shouldClean.none(Entity::isValid)) { "blacklist mode should remove all matching entities\n$consoleOut" }

            assert(shouldNotClean.all(Entity::isValid)) { "blacklist mode should keep all non-matching entities\n$consoleOut" }

        }



        @Test

        @DisplayName("whitelist")

        fun whiteList() {

            // false uses whitelist mode (matching items are kept)

            Config.config.drop.black = false

            Config.config.drop.match = mutableListOf(Regex("DIAMOND.*"))



            val location = Location(world, 8.0, 8.0, 8.0)

            val shouldNotClean = world.dropItems(location, 2) { index ->

                when (index) {

                    0 -> ItemStack(Material.DIAMOND_CHESTPLATE)

                    1 -> ItemStack(Material.DIAMOND_SWORD)

                    else -> throw Exception()

                }

            }

            val shouldClean = world.dropItems(location, 2) { index ->

                when (index) {

                    0 -> ItemStack(Material.STONE)

                    1 -> ItemStack(Material.BOW)

                    else -> throw Exception()

                }

            }



            cleanDrop()



            assert(shouldClean.none(Entity::isValid)) { "whitelist mode should remove all non-matching entities\n$consoleOut" }

            assert(shouldNotClean.all(Entity::isValid)) { "whitelist mode should keep all matching entities\n$consoleOut" }

        }

    }



    @Test

    @DisplayName("papi")

    fun testPapi() {

        val count = 2

        Config.config.drop.match = mutableListOf(Regex("DIAMOND"))



        val location = Location(world, 8.0, 8.0, 8.0)

        world.dropItems(location, count) { _ -> ItemStack(Material.DIAMOND) }



        cleanDrop()



        assert(lastDrop == count) { "PAPI returned the wrong count for the last dropped item cleanup\n$consoleOut" }

    }

}