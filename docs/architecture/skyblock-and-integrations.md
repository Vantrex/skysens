# SkyBlock domain and other-mod integrations

## `skyblock/`: shared domain logic

Code that several features need and that is about SkyBlock itself, not about one feature.

| Package | Contents |
|---|---|
| `location/` | `LocationService`: current island (`SkyblockLocationEnum`, from the Mod API `map`) and zone (`Zone`, from the sidebar's `⏣`/`ф` line). `zone/` holds one `ZoneEnum` per island with the sidebar names |
| `event/` | `SkyblockJoinEvent`, `SkyblockQuitEvent`, `LocationChangeEvent`, `ZoneChangeEvent`, and from `SlayerTracker`: `SlayerQuestStartEvent`, `SlayerBossSpawnEvent`, `SlayerBossCocoonEvent`, `SlayerQuestEndEvent` |
| `player/` | `PlayerService` (own name), `AfkTracker`, `CombatTracker` (both registered as features) |
| `slayer/` | `SlayerTracker` (registered as a feature): whether a slayer quest is running, its boss and whether the boss spawned or was cocooned, from chat and the sidebar. Always on; the slayer stats in `features/slayer/` build on its events |
| `entity/` | `EntityClassifier`: player / NPC / other, with the NPC heuristic (v2 UUID or no tab list entry) |
| `item/` | `HypixelExtraAttributes` (item NBT), `ItemIdUtil` (display name → Hypixel id) |
| `price/` | `ItemPriceService` + `ItemPriceProvider` chain |
| `profit/` | `ProfitService`, `ProfitRepository`, `!profit`; combines SkyHanni's trackers with SkySens' own |

### Locations and zones

- `isOnSkyBlock()` and the island come from the Hypixel Mod API location packet (`SkysensClient`). Use
  `/mocklocation <map>` (or `null`) in the dev client to fake an island.
- The zone is polled from the scoreboard every few client ticks (`TICKS_UNTIL_ZONE_CHECK`) and fires
  `ZoneChangeEvent` when it changes. An island change resets it to null first.
  Unknown zones are reported once through `ClientUtil.sendDebug` ("Unknown zone on …"). Add them to the island's
  `*ZoneEnum` with the exact sidebar text.
- A new island needs a `SkyblockLocationEnum` constant (Mod API map name) and, if it has zones, a `ZoneEnum`.

## Provider chains

Data that SkyHanni or Odin may already have is read through a list of providers, tried in order. An empty result
falls through to the next provider, and the last one is always SkySens' own implementation:

| Service | Providers in order |
|---|---|
| `ItemPriceService` | `SkyHanniPriceProvider` → `HypixelPriceProvider` (Hypixel item list + bazaar API, EliteSkyblock lowest BIN) |
| `ServerStatsService` (TPS, ping) | `SkyHanniStatsProvider` → `OdinStatsProvider` → `InternalStatsProvider` (time/pong packets) |

Chat command bridges (`OdinChatCommandsBridge`, `SkyHanniPartyCommandsBridge`) work the same way, see
[chat-commands.md](chat-commands.md). `SkyHanniTrackerReader` feeds `ProfitService` with SkyHanni's profit trackers.

## `integration/`: rules for touching other mods

- **No compile-time dependency** on SkyHanni or Odin; they may be absent or a different version. Check
  `FabricLoader.getInstance().isModLoaded(...)` in `isAvailable()`.
- Reach their Kotlin `object`s with `KotlinObjectAccessor` (reflection on `INSTANCE`). Any reflective failure logs
  one warning and switches the accessor off for the session, so SkySens falls back instead of crashing.
- Adapters are `public` classes in `integration/<mod>/` that implement an interface owned by the consumer
  (`ItemPriceProvider` in `skyblock/price`, `ServerStatsProvider` in `stats`, `ChatCommandBridge` in `chatcommand`).
  To support a new mod, add an adapter and put it in the right place in the consumer's provider list.
- Network calls (`HypixelPriceProvider`) must not block the client thread: prefetch and refresh asynchronously.
