# SkyBlock domain and other-mod integrations

## `skyblock/`: shared domain logic

Code that several features need and that is about SkyBlock itself, not about one feature.

| Package | Contents |
|---|---|
| `location/` | `LocationService`: current island (`SkyblockLocationEnum`, from the Mod API `map`) and zone (`Zone`, from the sidebar's `⏣`/`ф` line). `zone/` holds one `ZoneEnum` per island with the sidebar names |
| `event/` | `SkyblockJoinEvent`, `SkyblockQuitEvent`, `LocationChangeEvent`, `ZoneChangeEvent`, and from `SlayerTracker`: `SlayerQuestStartEvent`, `SlayerBossSpawnEvent`, `SlayerBossCocoonEvent`, `SlayerQuestEndEvent` |
| `player/` | `PlayerService` (own name), `ProfileService` (current SkyBlock profile, fires `ProfileChangeEvent`), `AfkTracker`, `CombatTracker` (the last three registered as features) |
| `party/` | `PartyTracker` (registered as a feature): whether the player is in a Hypixel party, its members, everyone's `PartyRole` (leader, moderator, member; `getRole()`, `hasRole(...)`) and the size. Also picks up a party the player was already in when joining Hypixel. Read once per connection from the Mod API (`PartyService`), then kept up to date from the party chat messages and `/party list`. Not saved. Fires `PartyJoinEvent`, `PartyLeaveEvent` (with a `PartyLeaveReason`), `PartyKickEvent` (followed by a leave with `KICKED`), `PartyMemberJoinEvent`, `PartyMemberLeaveEvent`, `PartyRoleChangeEvent` (promote, demote, transfer) and `PartyMembersUpdateEvent`. Use `checkInParty()` instead of the cached state where a wrong answer would do harm, like `!warpto` does before inviting |
| `coop/` | `CoopMemberTracker` (co-op members and online status per profile from the tab list and chat, always-on feature, fires the co-op events), `CoopRepository` |
| `slayer/` | `SlayerTracker` (registered as a feature): whether a slayer quest is running, its boss and whether the boss spawned or was cocooned, from chat and the sidebar. Always on; the slayer stats in `features/slayer/` build on its events, as does the RNG meter tracking in `features/slayer/rng/` (drop data from the community wiki, exact XP read from the /rng menu) and the kills-left line above the action bar (XP per kill learned from sidebar progress) |
| `server/` | `TransferCooldownTracker` (registered as a feature, always on): Hypixel blocks transfer commands (/warp, /hub, ...) for a few seconds after a server switch. A cooldown (4 s, `DEFAULT_COOLDOWN_MS`) starts on every server switch seen in the Mod API location packet (`onServerSwitch`), is corrected by the chat message "You may only use this command after Ns on the server!" and then also learns the command sent just before as transfer-limited (saved in `TransferCooldownRepository`; do not rename). `isActive()`, `remainingMs()`, `isTransferLimited(command)` (defaults in `DEFAULT_TRANSFER_LIMITED`, add new roots there), `learnedCommands()`. Outgoing commands are seen through Fabric's `ClientSendMessageEvents.COMMAND`. Fires `TransferCooldownStartEvent`, `TransferCooldownEndEvent` and `TransferCooldownDeniedEvent`. The macro send path (`TransferWaitQueue`) uses it when "Wait for Transfer Cooldown" in the General category is on |
| `dungeon/` | `DungeonClassTracker` (registered as a feature): the player's selected `DungeonClass` and level (saved per profile in `DungeonClassRepository`), whether a run is going on, its floor and, during a run, every party member's class, level and alive state (`DungeonPartyMember`). Sources: "You selected the X Dungeon Class!", the party finder join line, the dungeon tab list ("Name (Mage XXV)") and the class menu. Fires `DungeonClassChangeEvent`, `DungeonClassLevelUpEvent` and `DungeonPartyClassesUpdateEvent`, only on real changes |
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
- A new island needs a `SkyblockLocationEnum` constant and, if it has zones, a `ZoneEnum`. Islands are matched by the
  Mod API `mode` (e.g. `foraging_2`), which survives island renames; the `map` name and the constant name are fallbacks.
  Never rename or remove a constant, the sensitivity config is keyed by it.

### Pets (`skyblock/pet/`)

`PetTracker` (singleton, always active) knows the active pet; any feature can read it:
`PetTracker.getInstance().getActivePet()` (`Optional<Pet>`), `isActive("GOLDEN_DRAGON")`, `getKnownPets()` (every pet
seen in the /pets menu on this profile). `Pet` is a record (type id, name, `PetRarity`, level, held item id, skin,
uuid, candy used). `PetInfoParser` builds a `Pet` from an item's `petInfo` ExtraAttributes (`fromStack`, reusable for
held-item/inventory checks) or from "[Lvl 100] Golden Dragon" (`fromDisplay`, rarity from the color codes).

Change sources, each tagged with a `PetChangeSource`: `CHAT` ("You summoned/despawned your X!"), `AUTOPET`
("Autopet equipped your [Lvl N] X! VIEW RULE"), `PETS_MENU` (click on a pet in /pets, or the menu showing an active
pet: `petInfo.active` / "Click to despawn!"), `LOADOUT` and `TAB_LIST` (the "Pet:" widget, read once a second). The
tab list is the fallback and source of truth, but is ignored for 6 s after another source changed the pet and must read
the same twice, so one change fires one event. Hypixel has no known loadout chat/GUI format: a change seen by the tab
list within 10 s after a chat line mentioning a "loadout" is tagged `LOADOUT`, otherwise it is `TAB_LIST`. "Your X
leveled up to level N!" updates the level. The active pet is saved per profile name in `ActivePetRepository` and
restored silently after joining.

Events (`skyblock/event/`): `PetChangeEvent(oldPet, newPet, source)` (`newPet` null = despawned),
`PetLevelUpEvent(pet, oldLevel, newLevel)`, `PetsMenuUpdateEvent(knownPets)`. They only fire on real changes.

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
- `InventoryHudBridge` reads Inventory HUD+'s HUD position from its public static fields and mirrors its layout
  math. `ActionBarLayout` (`features/actionbar/`) uses it to move the action bars above the HUD, recomputing only
  when marked dirty (its key bindings via `InventoryHudKeyActionMixin`, closing its config screens) or on a resize.
- Network calls (`HypixelPriceProvider`) must not block the client thread: prefetch and refresh asynchronously.

## Calendar and sidebar events

**Calendar (`skyblock/time/`).** `SkyblockCalendar` is pure math on the real time: year 1 began 2019-06-11 17:55 UTC,
a day is 20 real minutes, a month 31 days, a year 12 months (124 real hours). `date(ms)` returns a `SkyblockDate`
(year, `SkyblockMonth` with its season, day 1-31, hour, minute; the day is taken to start at 0:00, the offset to the
in-game clock is unverified). `SkyblockEventCalendar` holds fixed windows (`SkyblockCalendarEvent`: Spooky Festival
Late Autumn 29-31, Season of Jerry Late Winter 24-26, New Year Late Winter 29-31, Traveling Zoo Early Summer and
Early Winter 1-3) with `isEventActive(event)` and `nextStart(event)`, and fires `SkyblockCalendarEventChangeEvent`
once a second on transitions while on SkyBlock. Dark Auction, Jacob's contest, Hoppity's Hunt and Carnival are not
modelled (timing unconfirmed); use the sidebar tracker.

**Sidebar events (`skyblock/sidebar/`).** `SidebarEventTracker` reads the sidebar once a second and exposes the timed
events (`SidebarEventType`), the mining event and zone, Jacob's contest and crop, Kuudra tier and tokens, Garden
visitors, Crystal Hollows heat and the Bingo profile. It is **tri-state**: `isKnown()` is false (and `Optional` getters
are empty) until the first sidebar read since joining or an island change, so callers can tell unknown from false.
State is rebuilt from the sidebar after a rejoin. The first read fires no events; later real changes fire
`SidebarEventChangeEvent`, `MiningEventChangeEvent`, `JacobsContestChangeEvent`, `KuudraTierChangeEvent` and
`GardenVisitorsChangeEvent`. The patterns are in `docs/specs/skyblock-events-research.md`.

## Diana tracker (`skyblock/diana/`)

`DianaTracker` (always active, registered in `FeatureService`) tracks the Mythological Ritual for any feature.

- **Ritual:** `MayorService.isMythologicalRitualActive()` (perk "Mythological Ritual"; Jerry's "Perkpocalypse" is not
  counted because the API doesn't say whose perks it borrows). The chat line "The mythological ritual isn't active!"
  forces it to false until the next successful mayor refresh. `MayorService.hasPerk(name)` and `isDataKnown()` are generic.
- **Doing Diana:** hub + ritual + a spade (`ANCESTRAL_SPADE`, `ARCHAIC_SPADE`, `DEIFIC_SPADE`) in the hotbar. The Griffin pet is read from `PetTracker`.
- **Chat only (unknown until seen):** burrow chain progress `(x/y)`, mob spawns (`MythologicalMob`, with `isRare()`), treasure drops.
- **Tri-state:** `getRitualActive()`, `getSpadeInHotbar()` and `getDoingDiana()` return `Optional<Boolean>`, empty meaning unknown
  (e.g. before the first mayor fetch); `getChainProgress()` is null until the first burrow line. The plain `isX()` getters treat unknown as false.
- **Events:** `DianaRitualChangeEvent`, `DianaDoingChangeEvent` (not fired when a value first becomes known),
  `GriffinBurrowDugEvent`, `MythologicalMobSpawnEvent`, `DianaTreasureEvent`.
- Chain, last mob and rare-mob state reset on quit and on island change.
