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
    // schedule cleanup tasks on load
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
    // update check off by default so offline servers do not make requests on startup
    // kept only for old config compatibility; MCDRS Clean no longer checks for updates
    var update: Boolean = false,
    @SerialName("console_broadcast")
    var consoleBroadcast: Boolean = false,
    // entities handled per tick for batched cleanup
    @SerialName("batch_size")
    var batchSize: Int = 200,
    var bstats: Boolean = true,
    // all fields have defaults, so a missing key in a hand-edited config cannot stop the plugin from loading
    var duration: Long = 600,
    var message: MutableMap<Long, String> = mutableMapOf(
        60L to "&fentities will be cleaned in 1 minute",
        30L to "&fentities will be cleaned in 30 seconds",
        10L to "&fentities will be cleaned in 10 seconds",
        0L to "&fcleaning now",
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
