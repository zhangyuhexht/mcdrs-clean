# MCDRS Clean

A cleanup plugin for Paper 26.3. It clears dropped items, mobs and overcrowded chunks on a timer, warns players before it runs, and keeps those warnings out of the console.

This started as a fork of a GPL-3.0 cleanup plugin. The code now lives under `top.mcdrsclean`, the commands and permissions were renamed, and the whole thing was rebuilt against the 26.3 API. Cleanup runs in small per-tick batches now instead of removing everything in one main-thread pass.

## Requirements

Paper 26.3, Java 25. That Java version is not optional: Paper 26.1 and up require it, and the paper-api classes are compiled for it. The plugin jar itself targets Java 21 bytecode. PlaceholderAPI is optional; without it the placeholders are simply not registered.

`api-version` is declared as `'26.3'`, so older cores will refuse to load the plugin. That was a deliberate choice. Mohist, Arclight and other modded hybrids are out of scope.

## Install

Put the jar in `plugins/`, boot the server once to generate the config folder, then edit `plugins/MCDRSClean/config.yml`. `/mcdrsclean reload` applies changes and restarts the countdown from zero.

## Commands

Three names work: `/mcdrsclean`, `/mcdrs` and `/mc`. Use the long form if a short alias is taken by another plugin.

- `/mcdrsclean clean` - cleans everything. You have to run it a second time within 10 seconds to confirm.
- `/mcdrsclean clean drop [world]`, `clean entity [world]`, `clean chunk [world]` - one category at a time. Passing a world name skips the countdown notice.
- `/mcdrsclean stats [world]` - entity and chunk counts for a world.
- `/mcdrsclean entity <type> [world] [min-count]` - per-chunk counts for one entity type. `min-count` hides chunks below that number.
- `/mcdrsclean trash` - opens the shared trash can.
- `/mcdrsclean show` - opens the crowded-chunk overview.
- `/mcdrsclean players` - lists online players and where they are.
- `/mcdrsclean debug` - toggles whether you receive debug messages.
- `/mcdrsclean reload` - reloads config and language files.

## Permissions

`mcdrs.admin` covers the query and reload commands. `mcdrs.clean` is for the destructive ones, so it can be handed out separately. `mcdrs.trash` opens the trash can. All three default to OP.

## Config

`src/main/resources/config.yml` has the full file with comments. The settings people usually touch:

- `duration` (600) - seconds between runs.
- `message` - keys are seconds remaining, values are the warning texts sent to players.
- `console_broadcast` (false) - leave it false and warnings only go to players. Set it to true if you want them in the log as well.
- `batch_size` (200) - entities or chunks handled per tick. Lower it if a cleanup causes MSPT spikes.
- `no_online` - what to do when nobody is online.
- `living`, `drop`, `chunk`, `trashcan` - rules for each category, mostly regex based.

Messages live in `lang.yml` and still use `&` colour codes. Moving them to Adventure components would be cleaner, but the EPlugin layer this depends on is string-based throughout.

## PlaceholderAPI

Install PlaceholderAPI and these show up under the `mcdrs` identifier:

- `%mcdrs_before_next%` / `%mcdrs_before_next_formatted%` - seconds until the next cleanup, raw or formatted.
- `%mcdrs_last_drop%`, `%mcdrs_last_living%`, `%mcdrs_last_chunk%` - counts from the last run.
- `%mcdrs_trashcan_countdown%` / `%mcdrs_trashcan_countdown_formatted%` - time until the trash can is emptied.

## Building

```
git clone https://github.com/zhangyuhexht/mcdrs-clean.git
cd mcdrs-clean
./gradlew shadowJar
```

You need JDK 25. Output ends up at `build/libs/MCDRSClean-1.21.0.jar`.

One thing to know before you build: `top.e404:eplugin-*:1.4.0` is not on Maven Central. Grab the EPlugin source, set `group` and `version` in `buildSrc/src/main/kotlin/Versions.kt` to `top.e404` / `1.4.0`, trim `settings.gradle.kts` down to `core`, `menu`, `serialization` and `hook-placeholderapi`, then publish those four modules with JDK 21 + Gradle 9 (`publishToMavenLocal`). Under Gradle 9 you will need to remove the duplicate `sourcesJar` artifact in the publication or it errors out. Then come back here and run `shadowJar`.

Use `shadowJar`, not `build`. There is a leftover test suite from the original project that targets MockBukkit 1.20 and does not run on JDK 25.

## Tested against

Paper 26.3-245 on Java 25. The plugin enables cleanly, commands and aliases respond, the confirm step works, and there were no `NoSuchMethodError` or `InvalidPluginException` errors in the log. With `console_broadcast: false` there were zero periodic broadcast lines in the console; flipping it to true brought back six. In one run 29 dropped items were found and 28 removed, with the whitelisted item left alone. Setting `batch_size` to 1 spread a cleanup across two ticks, so the batching does what it says.

## License

GPL-3.0, see `LICENSE`. This is a modified version of another GPL-3.0 plugin, and the original upstream code remains with its author.
