package top.mcdrsclean.util

import org.bukkit.Bukkit
import top.mcdrsclean.config.Config

val noOnline get() = Bukkit.getOnlinePlayers().isEmpty()
val noOnlineClean get() = Config.config.noOnline.clean
val noOnlineMessage get() = Config.config.noOnline.message