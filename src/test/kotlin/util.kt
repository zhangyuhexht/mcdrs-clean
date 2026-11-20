package top.mcdrsclean.test



import be.seeseemelk.mockbukkit.ServerMock

import be.seeseemelk.mockbukkit.WorldMock

import be.seeseemelk.mockbukkit.entity.PlayerMock

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer

import org.bukkit.Location

import org.bukkit.entity.Entity

import org.bukkit.entity.EntityType

import org.bukkit.inventory.ItemStack

import top.mcdrsclean.MCDRSClean

import top.mcdrsclean.config.*



lateinit var server: ServerMock

lateinit var plugin: MCDRSClean

lateinit var world: WorldMock

lateinit var player: PlayerMock



fun WorldMock.spawnEntities(

    location: Location,

    type: EntityType,

    count: Int,

    edit: (index: Int, spawned: Entity) -> Unit = { _, _ -> }

) = (0 until count).map { index ->

    val spawned = spawnEntity(location, type)

    edit(index, spawned)

    spawned

}



fun WorldMock.dropItems(

    location: Location,

    count: Int,

    generator: (index: Int) -> ItemStack

) = (0 until count).map { index ->

    val spawned = dropItem(location, generator(index))

    spawned

}



private val serializer = LegacyComponentSerializer.builder().build()

private val colorRegex = Regex("§[\\da-fk-or]")

val consoleOut

    get() = buildString {

        while (true) {

            val component = server.consoleSender.nextComponentMessage() ?: break

            appendLine(

                serializer.serialize(component)

                    .replace(colorRegex, "")

                    .replace("[MCDRSDebug]", "[DEBUG]")

                    .replace("[MCDRSClean]", "[INFO ]")

            )

        }

    }



val enableDebug = System.getProperty("mcdrs.debug") != null



/**

 * upstream KtxConfig#config has an internal setter at eplugin module scope, so test code cannot assign it directly,

 * use reflection to reach the setter and inject the test config.

 */

// the setter is declared on the parent KtxConfig, so use methods (includes inherited) instead of declaredMethods;

// Kotlin internal compiles to public with a module suffix, so plain reflection works.

private val configSetter: java.lang.reflect.Method = Config::class.java.methods

    // after type erasure the setter takes Object, so do not match on ConfigData

    .firstOrNull { it.name.startsWith("setConfig") && it.parameterCount == 1 }

    ?: error("Config#setConfig not found; the eplugin dependency version may have changed")



fun resetConfig() {

    world.entities.forEach(Entity::remove)

    configSetter.invoke(

        Config,

        ConfigData(

            debug = enableDebug,

            update = false,

            duration = Long.MAX_VALUE,

            message = mutableMapOf(),

            living = LivingConfig(enable = false),

            drop = DropConfig(enable = false),

            chunk = ChunkConfig(enable = false),

            trashcan = TrashcanConfig(),

            noOnline = NoOnlineConfig()

        )

    )

    // clear console output

    consoleOut

}