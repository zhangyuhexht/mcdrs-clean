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

    // do not reuse the upstream bStats ID; apply for your own and fill it in to report stats,
    // and enable bstats in config.yml. ID 0 sends nothing.
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
        // bstats is toggled from config, so initialise it after Config loads
        if (!unit && Config.config.bstats && bstatsId > 0) bstats()
        Commands.register()
        Clean.schedule()
        HookManager.register()
        MenuManager.register()
        DespawnListener.register()
        Trashcan.register()
        if (PapiHook.enable) Papi.register()
        for (line in logo) info(line)
        info("&aMCDRS Clean loaded".color)
    }

    override fun onDisable() {
        MenuManager.shutdown()
        if (PapiHook.enable) Papi.unregister()
        Bukkit.getScheduler().cancelTasks(this)
        info("&aMCDRS Clean disabled".color)
    }
}

lateinit var PL: EPlugin
    private set

internal var unit = false
