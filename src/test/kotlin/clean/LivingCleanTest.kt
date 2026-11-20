package top.mcdrsclean.test.clean



import be.seeseemelk.mockbukkit.entity.LivingEntityMock

import org.bukkit.Location

import org.bukkit.entity.Entity

import org.bukkit.entity.EntityType

import org.junit.jupiter.api.BeforeEach

import org.junit.jupiter.api.DisplayName

import org.junit.jupiter.api.Nested

import org.junit.jupiter.api.Test

import top.mcdrsclean.clean.cleanLiving

import top.mcdrsclean.clean.lastLiving

import top.mcdrsclean.config.Config

import top.mcdrsclean.test.*



abstract class LivingCleanTest {



    @BeforeEach

    fun enable() {

        resetConfig()

        Config.config.living.enable = true

    }



    @Nested

    @DisplayName("named entity cleanup")

    inner class TestCleanEntityWithCustomName {

        @Test

        @DisplayName("enabled")

        fun onEnable() {

            // true removes named entities

            Config.config.living.settings.name = true

            Config.config.living.match = mutableListOf(Regex("ZOMBIE"))



            val chunk = world.getChunkAt(0, 0)

            chunk.load()

            val location = Location(world, 8.0, 8.0, 8.0)

            val entities = world.spawnEntities(location, EntityType.ZOMBIE, 16) { _, zombie ->

                @Suppress("DEPRECATION")

                zombie.customName = "custom name"

            }



            cleanLiving()



            assert(entities.none(Entity::isValid)) { "With named entity removal enabled, named entities should all be removed\n$consoleOut" }

        }



        @Test

        @DisplayName("disabled")

        fun onDisable() {

            // true removes named entities

            Config.config.living.settings.name = false

            Config.config.living.match = mutableListOf(Regex("ZOMBIE"))



            val chunk = world.getChunkAt(0, 0)

            chunk.load()

            val location = Location(world, 8.0, 8.0, 8.0)

            val entities = world.spawnEntities(location, EntityType.ZOMBIE, 16) { _, zombie ->

                @Suppress("DEPRECATION")

                zombie.customName = "custom name"

            }



            cleanLiving()

            assert(entities.all(Entity::isValid)) { "With named entity removal disabled, named entities should not be removed\n$consoleOut" }

        }

    }



    @Nested

    @DisplayName("leashed entity cleanup")

    inner class TestCleanEntityWithLead {

        @Test

        @DisplayName("enabled")

        fun onEnable() {

            // true removes leashed entities

            Config.config.living.settings.lead = true

            Config.config.living.match = mutableListOf(Regex("SHEEP"))



            val chunk = world.getChunkAt(0, 0)

            chunk.load()

            val location = Location(world, 8.0, 8.0, 8.0)

            val entities = world.spawnEntities(location, EntityType.SHEEP, 16) { _, sheep ->

                sheep as LivingEntityMock

                sheep.setLeashHolder(sheep)

            }



            cleanLiving()

            assert(entities.none(Entity::isValid)) { "With leashed entity removal enabled, leashed entities should be removed\n$consoleOut" }

        }



        @Test

        @DisplayName("disabled")

        fun onDisable() {

            // true removes leashed entities

            Config.config.living.settings.lead = false

            Config.config.living.match = mutableListOf(Regex("SHEEP"))



            val chunk = world.getChunkAt(0, 0)

            chunk.load()

            val location = Location(world, 8.0, 8.0, 8.0)

            val entities = world.spawnEntities(location, EntityType.SHEEP, 16) { _, sheep ->

                sheep as LivingEntityMock

                sheep.setLeashHolder(sheep)

            }



            cleanLiving()

            assert(entities.all(Entity::isValid)) { "With leashed entity removal disabled, leashed entities should not be removed\n$consoleOut" }

        }

    }



    @Nested

    @DisplayName("riding entity cleanup")

    inner class TestCleanEntityWithMount {

        @Test

        @DisplayName("enabled")

        fun onEnable() {

            // true removes riding entities

            Config.config.living.settings.mount = true

            Config.config.living.match = mutableListOf(Regex("HORSE"))



            val chunk = world.getChunkAt(0, 0)

            chunk.load()

            val location = Location(world, 8.0, 8.0, 8.0)

            val entities = world.spawnEntities(location, EntityType.HORSE, 16) { index, horse ->

                horse as LivingEntityMock

                if (index < 8) horse.addPassenger(player)

                else player.addPassenger(horse)

            }



            cleanLiving()

            assert(entities.none(Entity::isValid)) { "With riding entity removal enabled, riding entities should be removed\n$consoleOut" }

        }



        @Test

        @DisplayName("disabled")

        fun onDisable() {

            // true removes riding entities

            Config.config.living.settings.mount = false

            Config.config.living.match = mutableListOf(Regex("HORSE"))



            val chunk = world.getChunkAt(0, 0)

            chunk.load()

            val location = Location(world, 8.0, 8.0, 8.0)

            val entities = world.spawnEntities(location, EntityType.HORSE, 16) { index, horse ->

                horse as LivingEntityMock

                if (index < 8) horse.addPassenger(player)

                else player.addPassenger(horse)

            }



            cleanLiving()

            assert(entities.all(Entity::isValid)) { "With riding entity removal disabled, riding entities should not be removed\n$consoleOut" }

        }

    }



    @Nested

    @DisplayName("blacklist / whitelist")

    inner class TestBlackWhiteList {

        @Test

        @DisplayName("blacklist")

        fun blackList() {

            // true uses blacklist mode (only matching items are removed)

            Config.config.living.black = true

            Config.config.living.match = mutableListOf(Regex("ZOMBIE.*"))



            val chunk = world.getChunkAt(0, 0)

            chunk.load()

            val location = Location(world, 8.0, 8.0, 8.0)

            val shouldClean = listOf(

                world.spawnEntity(location, EntityType.ZOMBIE),

                world.spawnEntity(location, EntityType.ZOMBIE_HORSE),

            )

            val shouldNotClean = listOf(

                world.spawnEntity(location, EntityType.SHEEP),

                world.spawnEntity(location, EntityType.COW),

            )



            cleanLiving()



            assert(shouldClean.none(Entity::isValid)) { "blacklist mode should remove all matching entities\n$consoleOut" }

            assert(shouldNotClean.all(Entity::isValid)) { "blacklist mode should keep all non-matching entities\n$consoleOut" }

        }



        @Test

        @DisplayName("whitelist")

        fun whiteList() {

            // false uses whitelist mode (matching items are kept)

            Config.config.living.black = false

            Config.config.living.match = mutableListOf(Regex("ZOMBIE.*"))



            val chunk = world.getChunkAt(0, 0)

            chunk.load()

            val location = Location(world, 8.0, 8.0, 8.0)

            val shouldClean = listOf(

                world.spawnEntity(location, EntityType.SHEEP),

                world.spawnEntity(location, EntityType.COW),

            )

            val shouldNotClean = listOf(

                world.spawnEntity(location, EntityType.ZOMBIE),

                world.spawnEntity(location, EntityType.ZOMBIE_HORSE),

            )



            cleanLiving()



            assert(shouldClean.none(Entity::isValid)) { "whitelist mode should remove all non-matching entities\n$consoleOut" }

            assert(shouldNotClean.all(Entity::isValid)) { "whitelist mode should keep all matching entities\n$consoleOut" }

        }

    }



    @Test

    @DisplayName("papi")

    fun testPapi() {

        val count = 2

        Config.config.living.match = mutableListOf(Regex("ZOMBIE"))



        val chunk = world.getChunkAt(0, 0)

        chunk.load()

        val location = Location(world, 8.0, 8.0, 8.0)

        world.spawnEntities(location, EntityType.ZOMBIE, count)



        cleanLiving()



        assert(lastLiving == count) { "PAPI returned the wrong count for the last living entity cleanup\n$consoleOut" }

    }

}