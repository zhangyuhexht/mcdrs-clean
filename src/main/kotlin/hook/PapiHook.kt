package top.mcdrsclean.hook

import top.mcdrsclean.PL
import top.e404.eplugin.hook.EHookManager
import top.e404.eplugin.hook.placeholderapi.PlaceholderAPIHook

object HookManager : EHookManager(PL, PapiHook)

object PapiHook : PlaceholderAPIHook(PL)