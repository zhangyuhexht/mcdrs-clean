package top.mcdrsclean.command

import top.mcdrsclean.PL
import top.e404.eplugin.command.ECommandManager

object Commands : ECommandManager(
    PL,
    "mcdrsclean",
    Debug,
    Reload,
    Clean,
    Stats,
    EntityStats,
    Trash,
    Players,
    Show
)
