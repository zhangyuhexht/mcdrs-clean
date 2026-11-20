package top.mcdrsclean.listener



import org.bukkit.event.EventHandler

import org.bukkit.event.EventPriority

import org.bukkit.event.entity.ItemDespawnEvent

import top.mcdrsclean.PL

import top.mcdrsclean.clean.Trashcan

import top.mcdrsclean.config.Config

import top.e404.eplugin.config.matches

import top.e404.eplugin.listener.EListener



object DespawnListener : EListener(PL) {

    @EventHandler(priority = EventPriority.HIGHEST)

    fun ItemDespawnEvent.onEvent() {

        val item = entity.itemStack

        PL.debug { "item expired and removed: ${item.type.name}, world: ${entity.world.name}" }

        Config.config.trashcan.run {

            if (!enable

                || !despawn.enable

                || despawn.disableWorlds.matches(entity.world.name)

                || !despawn.match.matches(item.type.name)

            ) return

        }

        PL.debug { "moving matching item to trash can: ${item.type.name}, world: ${entity.world.name}" }

        Trashcan.addItem(item.clone())

        // ensure the item is removed

        entity.remove()

    }

}