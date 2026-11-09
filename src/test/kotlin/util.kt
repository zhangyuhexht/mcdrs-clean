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
 * 上游 KtxConfig#config 的 setter 是 internal(eplugin 模块级), 测试代码无法直接赋值,
 * 这里用反射拿到 setter 注入测试配置.
 */
// setter 定义在父类 KtxConfig 上, 必须用 methods(含继承) 而不是 declaredMethods;
// Kotlin 的 internal 在字节码里是 public(方法名带模块后缀), 直接反射调用即可.
private val configSetter: java.lang.reflect.Method = Config::class.java.methods
    // 泛型 T 擦除后 setter 的参数是 Object, 不能按 ConfigData 比对类型
    .firstOrNull { it.name.startsWith("setConfig") && it.parameterCount == 1 }
    ?: error("找不到 Config#setConfig, eplugin 依赖版本可能已变更")

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
    // 清空控制台输出
    consoleOut
}