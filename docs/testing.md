# Testing changes

There are no automated tests. A change is verified by building it and checking it in the game.

## Build

```bash
sh ./gradlew build
```

It must end with `BUILD SUCCESSFUL`. Gradle needs a JDK 25 in `JAVA_HOME`.

## Dev client (offline, singleplayer)

```bash
sh ./gradlew runClient
```

- Before testing settings or saved data, back up `run/config/skysens/` and restore it afterwards.
- Watch `run/logs/latest.log`. A broken change usually shows up as `Mixin apply failed`, `InvalidMixinException`,
  `ClassNotFoundException`/`NoClassDefFoundError` mentioning `de.vantrex`, or a new file in `run/crash-reports/`.
  The offline dev account always logs four harmless errors (user properties, Realms ×2, profile key pair).
- `/skysens` opens the config screen; Dev → Debug Mode turns on `ClientUtil.sendDebug` output in chat.
- `/mocklocation <map>` fakes a SkyBlock island, so location features can be checked offline. Most SkyBlock features
  still need Hypixel (`isOnSkyBlock()` comes from the Hypixel Mod API).
- After changing options, check that `run/config/skysens/config.json` holds them, that they survive a restart, and
  that no `config.json.broken-*` file appears.

## On Hypixel

Hypixel-only behaviour (chat commands, bazaar, dungeons, prices) is tested by the user in their own launcher instance.
Don't send automated input on Hypixel; ask the user to perform in-game actions, and only read the screen and logs.
Party chat commands are tested in a party with only the user in it. Test once without and once with SkyHanni and
Odin installed, because the integrations change which code path answers.

A full checklist for a large refactor is in [refactor-smoke-test-prompt.md](refactor-smoke-test-prompt.md).
