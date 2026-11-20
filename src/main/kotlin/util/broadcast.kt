package top.mcdrsclean.util



import org.bukkit.Bukkit

import top.mcdrsclean.PL

import top.mcdrsclean.config.Config

import top.e404.eplugin.EPlugin.Companion.placeholder



/**

 * broadcast a message to online players without writing it to the console or log.

 *

 * upstream EPlugin#broadcastMsg calls info(message) first, printing every broadcast to the console.

 * periodic countdown, result and trash-clear messages therefore spam the console and waste log space.

 * this version sends them to online players only, with identical text, colours and prefix;

 * set console_broadcast to true in config.yml to keep console output.

 */

fun broadcastPlayersOnly(message: String) {

    if (Config.config.consoleBroadcast) PL.info(message)

    Bukkit.getOnlinePlayers().forEach { PL.sendMsgWithPrefix(it, message) }

}



/**

 * broadcast cleanup results.

 *

 * handles two things that are otherwise easy to duplicate:

 * 1. no broadcast when the template is empty;

 * 2. no broadcast with no online players when no_online.message is false.

 */

fun notifyFinish(template: String, vararg placeholders: Pair<String, Any?>) {

    if (template.isBlank()) return

    if (noOnline && !noOnlineMessage) return

    broadcastPlayersOnly(template.placeholder(*placeholders))

}

