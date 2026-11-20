# Changelog

## 1.21.0 (2026-11-10)

First MCDRS Clean release. Forked from a GPL-3.0 cleanup plugin and rebuilt for Paper 26.3.

Platform work:

- Moved to Paper 26.3 with `api-version: '26.3'`, which means Java 25 is required.
- Replaced `org.spigotmc:spigot-api` with `io.papermc.paper:paper-api:26.3.build.244-stable`.
- Toolchain is now Gradle 9.8.2 / Kotlin 2.4.21 / Shadow 9.6.1. The jar still targets Java 21 bytecode.
- `Enchantment.DURABILITY` became `Enchantment.UNBREAKING`. The old alias is gone in 26.x and the first real compile caught it.
- Dropped the `mcVer` check from EPlugin, whose regex only understood `1.x` style versions.
- Gave the plain jar a classifier so it stops overwriting the shaded one.

Naming:

- Everything moved to MCDRS Clean: package `top.mcdrsclean`, main class, commands, permissions, placeholders, message prefix, data folder and jar name.
- Removed the old update checker and the upstream bStats ID. Neither points at this project, so neither belongs here.

Behaviour changes:

- Countdown and result broadcasts now go to online players only. `console_broadcast: true` brings back the old console output if you want it.
- Cleanup is planned first and then removed in per-tick batches. `batch_size` controls how much runs in one tick.
- The countdown uses wall-clock time, so a lagging server no longer stretches a 600-second interval.
- Dense-entity rules group entities by type before matching, instead of walking the full list once per regex.
- Item checks call `hasItemMeta()` first, avoiding an ItemMeta clone for every dropped item.
- Trashcan deduplication no longer relies on the deprecated `durability` field.
- `/mcdrsclean clean` asks you to run it again within 10 seconds, behind the new `mcdrs.clean` permission.
- Every config field has a default now; missing a key no longer breaks startup.
- `update` and `bstats` default to false.

Checked on Paper 26.3-245: the console-broadcast on/off comparison, the confirm step, batching across ticks, actual entity removal and whitelist retention all passed.
