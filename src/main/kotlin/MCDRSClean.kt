package top.mcdrsclean

import org.bukkit.Bukkit
import org.bukkit.plugin.PluginDescriptionFile
import org.bukkit.plugin.java.JavaPluginLoader
import top.mcdrsclean.clean.Clean
import top.mcdrsclean.clean.Trashcan
import top.mcdrsclean.command.Commands
import top.mcdrsclean.config.Config
import top.mcdrsclean.config.Lang
import top.mcdrsclean.hook.HookManager
import top.mcdrsclean.hook.PapiHook
import top.mcdrsclean.listener.DespawnListener
import top.mcdrsclean.menu.MenuManager
import top.mcdrsclean.papi.Papi
import top.e404.eplugin.EPlugin
import java.io.File

open class MCDRSClean : EPlugin {
    companion object {
        val logo = listOf(
            """&6 __  __   ____  ____   ____   ____  """.color,
            """&6|  \/  | / ___||  _ \ |  _ \ / ___| """.color,
            """&6| |\/| || |    | | | || |_) |\___ \ """.color,
            """&6| |  | || |___ | |_| ||  _ <  ___) |""".color,
            """&6|_|  |_| \____||____/ |_| \_\|____/ """.color
        )
    }

    @Suppress("UNUSED")
    constructor() : super()

    @Suppress("UNUSED")
    constructor(
        loader: JavaPluginLoader,
        description: PluginDescriptionFile,
        dataFolder: File,
        file: File
    ) : super(loader, description, dataFolder, file)

    override val debugPrefix get() = langManager["debug_prefix"]
    override val prefix get() = langManager["prefix"]

    // 不再复用上游插件的 bStats ID; 如需统计请申请自有 ID 后填入,
    // 并在 config.yml 中打开 bstats 开关. ID 为 0 时不会发起任何上报.
    override val bstatsId = 0
    override var debug: Boolean
        get() = Config.config.debug
        set(value) {
            Config.config.debug = value
        }
    override val langManager by lazy { Lang }

    init {
        PL = this
    }

    override fun onEnable() {
        Lang.load(null)
        Config.load(null)
        // bstats 的开关在配置里, 需要在 Config 加载之后再初始化
        if (!unit && Config.config.bstats && bstatsId > 0) bstats()
        Commands.register()
        Clean.schedule()
        HookManager.register()
        MenuManager.register()
        DespawnListener.register()
        Trashcan.register()
        if (PapiHook.enable) Papi.register()
        for (line in logo) info(line)
        info("&aMCDRS Clean 加载完成".color)
    }

    override fun onDisable() {
        MenuManager.shutdown()
        if (PapiHook.enable) Papi.unregister()
        Bukkit.getScheduler().cancelTasks(this)
        info("&aMCDRS Clean 已卸载".color)
    }
}

lateinit var PL: EPlugin
    private set

internal var unit = false
