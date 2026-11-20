package top.mcdrsclean.papi



import org.bukkit.OfflinePlayer

import org.bukkit.entity.Player

import top.mcdrsclean.PL

import top.mcdrsclean.clean.*

import top.mcdrsclean.config.Config

import top.e404.eplugin.hook.placeholderapi.PapiExpansion

import top.e404.eplugin.util.parseSecondAsDuration



/**

 * PAPI expansion

 *

 * - `%mcdrs_before_next%` - seconds until the next cleanup

 * - `%mcdrs_before_next_formatted%` - time until the next cleanup, formatted

 * - `%mcdrs_last_drop%` - dropped items removed in the last cleanup

 * - `%mcdrs_last_living%` - living entities removed in the last cleanup

 * - `%mcdrs_last_chunk%` - dense entities removed in the last cleanup

 * - `%mcdrs_trashcan_countdown%` - seconds until the trash can is cleared

 * - `%mcdrs_trashcan_countdown_formatted%` - time until the trash can is cleared, formatted

 */

object Papi : PapiExpansion(PL, "mcdrs") {

    override fun onPlaceholderRequest(player: Player?, params: String) = onRequest(player, params)



    override fun onRequest(player: OfflinePlayer?, params: String): String? {

        return when (params.lowercase()) {

            "before_next" -> (Config.config.duration - Clean.count).toString()

            "before_next_formatted" -> (Config.config.duration - Clean.count).parseSecondAsDuration()

            "last_drop" -> lastDrop.toString()

            "last_living" -> lastLiving.toString()

            "last_chunk" -> lastChunk.toString()

            "trashcan_countdown" -> Trashcan.countdown.toString()

            "trashcan_countdown_formatted" -> Trashcan.countdown.parseSecondAsDuration()

            else -> null

        }

    }



    private val placeholders = mutableListOf(

        "%mcdrs_before_next%",

        "%mcdrs_before_next_formatted%",

        "%mcdrs_last_drop%",

        "%mcdrs_last_living%",

        "%mcdrs_last_chunk%",

        "%mcdrs_trashcan_countdown%",

        "%mcdrs_trashcan_countdown_formatted%",

    )



    override fun getPlaceholders() = placeholders

}

