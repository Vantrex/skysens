# SkySens: smoke test of the package restructure

> Paste everything below into Claude Desktop with computer use enabled. Fill in the two `<...>` placeholders first.

---

You are testing a Minecraft Fabric mod called **SkySens** (a client mod for Hypixel SkyBlock, Minecraft 26.2, Java 25) on my Windows PC. I restructured its packages on branch `refactor/package-by-feature` (commit `33ab53b`). Only packages and imports changed; behaviour should be identical to `development`. A static bytecode comparison already showed the code is equivalent. Your job is to confirm in the running game that nothing broke, especially the **mixins** (their package changed to `de.vantrex.skysens.client.platform.mixin`), the **config**, and **saved data**.

- Repo: `D:\work\coding\skysens`
- Dev run folder (used by `gradlew runClient`): `D:\work\coding\skysens\run`
- My real launcher instance's mods folder: `<PATH TO MODS FOLDER OF MY LAUNCHER INSTANCE>`
- My real instance's config folder: `<PATH TO .minecraft/config OF THAT INSTANCE>`

## Ground rules

- **Never change source code or git state.** No commits, checkouts, pushes, or edits under `src/`. If something fails, report it; don't fix it.
- **Back up before touching data.** Before copying jars or launching, copy `config/skysens/` (including `repositories/`) of whichever instance you use to a timestamped backup folder next to it.
- **Hypixel safety: you do not play the game on Hypixel.** Automated input on Hypixel can get my account banned. On Hypixel you may only read the screen, take screenshots, read log files, and open/close menus or type the commands I list below *after asking me*. Ask me to do any movement, combat, or clicking in game menus.
- **Don't write in public chat, and don't trade.** Never send messages in all-chat, and never buy, sell, or cancel anything on the bazaar or auction house. Party chat commands only when I'm in a party alone (I'll set that up).
- **Ask when in doubt.** If a step needs something you can't see or do, ask me instead of guessing.

## Phase 1: build and offline checks

1. In `D:\work\coding\skysens`, run `git log --oneline -1` and confirm the branch is `refactor/package-by-feature` at `33ab53b` (or a later commit on that branch; tell me which).
2. Run `gradlew.bat build`. It must end with `BUILD SUCCESSFUL`.
3. In `build\libs`, identify the jar just built (newest timestamp, *not* ending in `-dev`/`-sources`). Ignore older jars such as `skysens-1.0.7.jar`; they contain the old package layout.
4. Open the jar as a zip and confirm:
   - `skysens.client.mixins.json` has `"package": "de.vantrex.skysens.client.platform.mixin"`.
   - Every class listed in its `client` array exists under `de/vantrex/skysens/client/platform/mixin/`, and that folder contains nothing else.

## Phase 2: dev client (no Hypixel needed)

1. Back up `run\config\skysens\`.
2. Start the dev client with `gradlew.bat runClient`.
3. Watch `run\logs\latest.log` while it starts. **Fail** if you see `Mixin apply failed`, `InvalidMixinException`, `ClassNotFoundException`/`NoClassDefFoundError` mentioning `de.vantrex`, or a crash report appearing in `run\crash-reports`.
4. Confirm the title screen appears.
5. Open a singleplayer world (create a new creative world if none exists). In chat run `/skysens`. The SkySens config screen must open.
6. In the config screen, click through every category in the sidebar: General, Chat/Chat Commands, Chat Filter, Bazaar, Dungeon, Mining, QoL, Slayer, Visuals, Dev. Each must render without errors in the log. Use the search field once.
7. **Persistence test.**
   - Change three harmless settings in different categories (for example, a Visuals scale slider, a QoL toggle, and the theme) and write down their new values.
   - Close the screen, quit the game normally, then start it again.
   - Confirm all three values persisted, and that no `config.json.broken-*` file appeared in `run\config\skysens\`.
   - Restore the backup afterwards.
8. **Visuals and mixin checks in singleplayer.** Each of these goes through a mixin:
   - **Visuals:** enable/adjust held-item transform, swing animation, and dropped-item scale. Drop an item, look at your hand, and swing. The changes must be visible.
   - **Commands:** run `/skysens scale export`. It should put a string in chat/clipboard without errors. Don't run `reset`.
   - **Keybind:** open Controls → Key Binds and confirm the SkySens category with the "warp to menu" binding exists.
9. Quit the game. Confirm no crash report was created and the log has no `ERROR` lines mentioning `skysens`, apart from errors you confirm also exist on `development` (only check that if something looks suspicious).

## Phase 3: real launcher on Hypixel (with my help)

1. Back up `<config folder>\skysens\`. Remove any older `skysens*.jar` from the mods folder, keeping a copy aside, and copy in the new jar from Phase 1.
2. **Run A: SkySens without SkyHanni/Odin.**
   - Move SkyHanni and Odin jars, if present, temporarily out of the mods folder.
   - Ask me to launch the game and join Hypixel SkyBlock.
3. **Run B: with SkyHanni and Odin.** Put SkyHanni and Odin back, and repeat the same checks as Run A. This covers the integration adapters, whose visibility changed.
4. For each run, check the following, asking me to perform any in-game actions:
   - **Startup:** the game reaches the title screen; `latest.log` shows no mixin or `de.vantrex` class errors.
   - **Saved data:**
     - My existing settings are unchanged; open `/skysens` and spot-check a few against the backup's `config.json`.
     - The files in `skysens\repositories\` (e.g. `ProfitRepository.json`, `DungeonRunRepository.json`, `BazaarOrderRepository.json`, `CoopRepository.json`) still have the same names.
     - After quitting, those files have been rewritten with the same data, not reset to empty.
   - **Location tracking:** after joining SkyBlock, ask me to walk between two zones of the Hub. Check that the location/zone shown by the party command `!zone` (ask me to send it in a solo party) matches where I am.
   - **Tab list / server stats:** ask me to send `!tps` and `!ping` in my solo party. Both should answer, and in Run B they should use the SkyHanni/Odin values where available.
   - **Other chat commands:** in the solo party, ask me to send `!help`, `!coords`, `!fps`, `!uptime`, `!profit`, `!runs`, `!slayer`, `!activity`, `!buffs`. Each must reply without a stack trace in the log.
   - **Bazaar:** ask me to open the bazaar's "Manage Orders" screen if I have orders. My own buy/sell orders should be highlighted per the Bazaar config, and clicks on blocked items should be disabled as configured. Look only; don't click.
   - **Prices and profit:** ask me to open `/skysens`, find the profit tracker section, and confirm values are shown. In Run B, confirm SkyHanni-based prices/tracker import works (no "SkyHanni unavailable" style message while SkyHanni is installed).
   - **Entity rendering:** with player/NPC scale and "hide other players" set in Visuals, ask me to stand in the Hub. Confirm the scaling and hiding take effect.
   - **Chat filter:** with Chat Filter enabled, confirm filtered spam lines don't appear (ask me to stand in the Hub for a minute).
   - **Warp menu:** ask me to press the "warp to menu" key. The warp menu screen must open and list my co-op members, if any.
   - **Dungeons/slayer/mining:** only if I say I'm doing them right now, ask me to do one run or boss and check the stats, profit, and notification features. Otherwise mark them "not tested".
5. Afterwards, restore my original jar setup if I ask you to. Keep the new jar in place by default.

## Report

When done, give me:

1. A table with one row per check: **check | result (pass / fail / not tested) | evidence** (log line, screenshot, file comparison).
2. For every failure: the exact log excerpt or crash report path, what I was doing, and whether it also happens on `development` if you were able to compare.
3. Where the backups are, and what state the mods/config folders are left in.
