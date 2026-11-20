package top.mcdrsclean.command



import org.bukkit.Bukkit

import org.bukkit.command.CommandSender

import org.bukkit.entity.EntityType

import top.mcdrsclean.PL

import top.mcdrsclean.clean.info

import top.mcdrsclean.config.Lang

import top.e404.eplugin.EPlugin.Companion.formatAsConst



fun CommandSender.sendWorldStats(worldName: String) {

    val world = Bukkit.getWorld(worldName)

    if (world == null) {

        PL.sendMsgWithPrefix(this, "&cworld &e$worldName&c does not exist")

        return

    }

    val list = world

        .entities

        .groupBy { it.type }

        .map { (k, v) -> k to v.size }

        .sortedByDescending { it.second }

    if (list.isEmpty()) {

        PL.sendMsgWithPrefix(this, Lang["command.stats.empty"])

        return

    }

    val entity = list.joinToString(Lang["command.stats.spacing"]) { (k, v) ->

        Lang[

            "command.stats.content",

            "type" to k,

            "count" to v.withColor()

        ]

    }

    // 26.x versions are no longer "1.xx"; upstream eplugin parses mcVer as "1.21.1-R...",

    // which may return null and make `mcVer!!` throw; Chunk#isForceLoaded has existed since 1.13,

    // so call it directly without a version check.

    PL.sendMsgWithPrefix(

        this,

        Lang[

            "command.stats.world",

            "world" to worldName,

            "count" to world.loadedChunks.size,

            "force" to world.loadedChunks.count { it.isForceLoaded },

            "entity" to entity

        ]

    )

}



fun CommandSender.sendEntityStats(worldName: String, typeName: String, min: Int = 0) {

    val world = Bukkit.getWorld(worldName)

    if (world == null) {

        PL.sendMsgWithPrefix(this, "&cworld &e${worldName}&c does not exist")

        return

    }

    val type = try {

        EntityType.valueOf(typeName.formatAsConst())

    } catch (t: Throwable) {

        PL.sendMsgWithPrefix(this, Lang["message.invalid_entity_type"])

        return

    }

    val list = world

        .loadedChunks

        .map { it.info() to it.entities.count { e -> e.type == type } }

        .filter { it.second > min }

        .sortedByDescending { e -> e.second }

    if (list.isEmpty()) {

        PL.sendMsgWithPrefix(this, Lang["command.stats.empty"])

        return

    }

    val entity = list.joinToString(Lang["command.stats.spacing"]) { (k, v) ->

        Lang[

            "command.stats.content",

            "type" to k,

            "count" to v.withColor()

        ]

    }

    PL.sendMsgWithPrefix(

        this,

        Lang[

            "command.stats.entity",

            "type" to typeName,

            "entity" to entity

        ]

    )

}



private fun Int.withColor() = when {

    this > 60 -> "&c$this"

    this > 30 -> "&e$this"

    else -> "&a$this"

}

