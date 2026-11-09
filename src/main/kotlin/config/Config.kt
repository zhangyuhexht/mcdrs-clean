package top.mcdrsclean.config

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import top.mcdrsclean.PL
import top.mcdrsclean.clean.Trashcan
import top.e404.eplugin.config.JarConfigDefault
import top.e404.eplugin.config.KtxConfig
import top.e404.eplugin.config.serialization.RegexSerialization

object Config : KtxConfig<ConfigData>(
    plugin = PL,
    path = "config.yml",
    default = JarConfigDefault(PL, "config.yml"),
    serializer = ConfigData.serializer(),
    format = Yaml(configuration = YamlConfiguration(strictMode = false))
) {
    // 加载时处理清理task
    override fun onLoad(config: ConfigData, sender: CommandSender?) {
        if (Bukkit.isPrimaryThread()) {
            Trashcan.schedule()
            return
        }
        plugin.runTask { Trashcan.schedule() }
    }
}

@Serializable
data class ConfigData(
    var debug: Boolean = false,
    // 检查更新默认关闭: 无外网环境(尤其国内)启动时不必发请求
    // 保留仅为兼容旧配置文件; MCDRS Clean 已移除自动更新检查
    var update: Boolean = false,
    @SerialName("console_broadcast")
    var consoleBroadcast: Boolean = false,
    // 每 tick 处理/移除的实体数, 用于分片清理
    @SerialName("batch_size")
    var batchSize: Int = 200,
    var bstats: Boolean = true,
    // 以下字段全部给默认值: 用户手改配置漏了某一项时, 不会因为反序列化失败导致插件起不来
    var duration: Long = 600,
    var message: MutableMap<Long, String> = mutableMapOf(
        60L to "&f将在1分钟后进行清理",
        30L to "&f将在30秒后进行清理",
        10L to "&f将在10秒后进行清理",
        0L to "&f正在清理",
    ),
    var living: LivingConfig = LivingConfig(),
    var drop: DropConfig = DropConfig(),
    var chunk: ChunkConfig = ChunkConfig(),
    var trashcan: TrashcanConfig = TrashcanConfig(),
    @SerialName("no_online")
    var noOnline: NoOnlineConfig = NoOnlineConfig(),
)

@Serializable
data class DropConfig(
    var enable: Boolean = true,
    @SerialName("disable_world")
    var disableWorld: MutableList<@Serializable(RegexSerialization::class) Regex> = mutableListOf(),
    var finish: String = "",
    @SerialName("is_black")
    var black: Boolean = true,
    var enchant: Boolean = false,
    var lore: Boolean = false,
    @SerialName("written_book")
    var writtenBook: Boolean = false,
    var match: MutableList<@Serializable(RegexSerialization::class) Regex> = mutableListOf(),
)

@Serializable
data class LivingConfig(
    var enable: Boolean = true,
    @SerialName("disable_world")
    var disableWorld: MutableList<@Serializable(RegexSerialization::class) Regex> = mutableListOf(),
    var finish: String = "",
    var settings: Settings = Settings(),
    @SerialName("is_black")
    var black: Boolean = true,
    var match: MutableList<@Serializable(RegexSerialization::class) Regex> = mutableListOf(),
)

@Serializable
data class Settings(
    var name: Boolean = false,
    var lead: Boolean = false,
    var mount: Boolean = false,
)

@Serializable
data class ChunkConfig(
    var enable: Boolean = true,
    @SerialName("disable_world")
    var disableWorld: MutableList<@Serializable(RegexSerialization::class) Regex> = mutableListOf(),
    var finish: String = "",
    var settings: Settings = Settings(),
    var count: Int = 50,
    var format: String? = null,
    var limit: MutableMap<@Serializable(RegexSerialization::class) Regex, Int> = mutableMapOf(),
)

@Serializable
data class TrashcanConfig(
    var enable: Boolean = true,
    var collect: Boolean = true,
    var duration: Long? = 6000,
    var despawn: DespawnConfig = DespawnConfig(),
)

@Serializable
data class DespawnConfig(
    var enable: Boolean = false,
    @SerialName("disable_world")
    var disableWorlds: MutableList<@Serializable(RegexSerialization::class) Regex> = mutableListOf(),
    var match: MutableList<@Serializable(RegexSerialization::class) Regex> = mutableListOf(),
)

@Serializable
data class NoOnlineConfig(
    var clean: Boolean = true,
    var message: Boolean = true,
)
