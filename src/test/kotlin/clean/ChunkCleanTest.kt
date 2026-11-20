package top.mcdrsclean.test.clean



import be.seeseemelk.mockbukkit.entity.LivingEntityMock

import org.bukkit.Location

import org.bukkit.entity.Entity

import org.bukkit.entity.EntityType

import org.junit.jupiter.api.BeforeEach

import org.junit.jupiter.api.DisplayName

import org.junit.jupiter.api.Nested

import org.junit.jupiter.api.Test

import top.mcdrsclean.clean.cleanDenseEntities

import top.mcdrsclean.clean.lastChunk

import top.mcdrsclean.config.Config

import top.mcdrsclean.test.*



abstract class ChunkCleanTest {



    @BeforeEach

    fun enable() {

        resetConfig()

        Config.config.chunk.enable = true

    }



    @Nested

    @DisplayName("named entity cleanup")

    inner class TestCleanEntityWithCustomName {

        @Test

        @DisplayName("enabled")

        fun onEnable() {

            // true removes named entities

            Config.config.chunk.settings.name = true

            val limit = 5

            Config.config.chunk.limit[Regex("ZOMBIE")] = limit



            val chunk = world.getChunkAt(0, 0)

            chunk.load()

            val location = Location(world, 8.0, 8.0, 8.0)

            val entities = world.spawnEntities(location, EntityType.ZOMBIE, 16) { _, zombie ->

                @Suppress("DEPRECATION")

                zombie.customName = "custom name"

            }



            cleanDenseEntities()



            val valid = entities.count(Entity::isValid)

            assert(valid == limit) { "With named entity removal enabled, named entities should be removed($valid != $limit)\n$consoleOut" }

        }



        @Test

        @DisplayName("disabled")

        fun onDisable() {

            // true removes named entities

            Config.config.chunk.settings.name = false

            Config.config.chunk.limit[Regex("ZOMBIE")] = 5



            val chunk = world.getChunkAt(0, 0)

            chunk.load()

            val location = Location(world, 8.0, 8.0, 8.0)

            val entities = world.spawnEntities(location, EntityType.ZOMBIE, 16) { _, zombie ->

                @Suppress("DEPRECATION")

                zombie.customName = "custom name"

            }



            cleanDenseEntities()

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

            Config.config.chunk.settings.lead = true

            val limit = 5

            Config.config.chunk.limit = mutableMapOf(Regex("SHEEP") to limit)



            val chunk = world.getChunkAt(0, 0)

            chunk.load()

            val location = Location(world, 8.0, 8.0, 8.0)

            val entities = world.spawnEntities(location, EntityType.SHEEP, 16) { _, sheep ->

                sheep as LivingEntityMock

                sheep.setLeashHolder(sheep)

            }



            cleanDenseEntities()

            val valid = entities.count(Entity::isValid)

            assert(valid == limit) { "With leashed entity removal enabled, leashed entities should be removed($valid != $limit)\n$consoleOut" }

        }



        @Test

        @DisplayName("disabled")

        fun onDisable() {

            // true removes leashed entities

            Config.config.chunk.settings.lead = false

            Config.config.chunk.limit = mutableMapOf(Regex("SHEEP") to 5)



            val chunk = world.getChunkAt(0, 0)

            chunk.load()

            val location = Location(world, 8.0, 8.0, 8.0)

            val entities = world.spawnEntities(location, EntityType.SHEEP, 16) { _, sheep ->

                sheep as LivingEntityMock

                sheep.setLeashHolder(sheep)

            }



            cleanDenseEntities()

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

            Config.config.chunk.settings.mount = true

            val limit = 5

            Config.config.chunk.limit = mutableMapOf(Regex("HORSE") to limit)



            val chunk = world.getChunkAt(0, 0)

            chunk.load()

            val location = Location(world, 8.0, 8.0, 8.0)

            val entities = world.spawnEntities(location, EntityType.HORSE, 16) { index, horse ->

                horse as LivingEntityMock

                if (index < 8) horse.addPassenger(player)

                else player.addPassenger(horse)

            }



            cleanDenseEntities()

            val valid = entities.count(Entity::isValid)

            assert(valid == limit) { "With riding entity removal enabled, riding entities should be removed($valid != $limit)\n$consoleOut" }

        }



        @Test

        @DisplayName("disabled")

        fun onDisable() {

            // true removes riding entities

            Config.config.chunk.settings.mount = false

            Config.config.chunk.limit = mutableMapOf(Regex("HORSE") to 5)



            val chunk = world.getChunkAt(0, 0)

            chunk.load()

            val location = Location(world, 8.0, 8.0, 8.0)

            val entities = world.spawnEntities(location, EntityType.HORSE, 16) { index, horse ->

                horse as LivingEntityMock

                if (index < 8) horse.addPassenger(player)

                else player.addPassenger(horse)

            }



            cleanDenseEntities()

            assert(entities.all(Entity::isValid)) { "With riding entity removal disabled, riding entities should not be removed\n$consoleOut" }

        }

    }



    @Test

    @DisplayName("papi")

    fun testPapi() {

        val limit = 5

        val count = 16

        Config.config.chunk.limit = mutableMapOf(Regex("ZOMBIE") to limit)



        val chunk = world.getChunkAt(0, 0)

        chunk.load()

        val location = Location(world, 8.0, 8.0, 8.0)

        val entities = world.spawnEntities(location, EntityType.ZOMBIE, count)



        cleanDenseEntities()

        val valid = entities.count(Entity::isValid)

        assert(valid == limit)

        assert(lastChunk == count - limit) { "PAPI returned the wrong count for the last chunk cleanup\n$consoleOut" }

    }

}