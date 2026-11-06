# SkyBlock domain and other-mod integrations

## `skyblock/`: shared domain logic

Code that several features need and that is about SkyBlock itself, not about one feature. The trackers are what the
conditional macros read ([macros.md](macros.md)), but any feature can use them.

| Package | Contents |
|---|---|
| `location/` | `LocationService`: current island (`SkyblockLocationEnum`, from the Mod API `map`) and zone (`Zone`, from the sidebar's `⏣`/`ф` line). `zone/` holds one `ZoneEnum` per island with the sidebar names |
| `event/` | Domain events: `SkyblockJoinEvent`, `SkyblockQuitEvent`, `LocationChangeEvent`, `ZoneChangeEvent` and the tracker events listed in [features.md](features.md#events) (party, co-op, slayer, pet, dungeon class, Diana, sidebar, calendar, transfer cooldown) |
| `player/` | `PlayerService` (own name), `ProfileService` (current SkyBlock profile, fires `ProfileChangeEvent`), `AfkTracker`, `CombatTracker` (the last three registered as features) |
| `party/` | `PartyTracker` (registered as a feature): whether the player is in a Hypixel party, its members, everyone's `PartyRole` (leader, moderator, member; `getRole()`, `hasRole(...)`) and the size. Also picks up a party the player was already in when joining Hypixel. Read once per connection from the Mod API (`PartyService`), then kept up to date from the party chat messages and `/party list`. Not saved. Fires `PartyJoinEvent`, `PartyLeaveEvent` (with a `PartyLeaveReason`), `PartyKickEvent` (followed by a leave with `KICKED`), `PartyMemberJoinEvent`, `PartyMemberLeaveEvent`, `PartyRoleChangeEvent` (promote, demote, transfer) and `PartyMembersUpdateEvent`. Use `checkInParty()` instead of the cached state where a wrong answer would do harm, like `!warpto` does before inviting |
| `coop/` | `CoopMemberTracker` (co-op members and online status per profile from the tab list and chat, always-on feature, fires the co-op events), `CoopRepository` |
| `slayer/` | `SlayerTracker` (registered as a feature): whether a slayer quest is running, its boss and tier, whether the boss spawned or was cocooned, from chat and the sidebar. Always on; the slayer stats in `features/slayer/` build on its events, as does the RNG meter tracking in `features/slayer/rng/` (drop data from the community wiki, exact XP read from the /rng menu) and the kills-left line above the action bar (XP per kill learned from sidebar progress). See [Slayer](#slayer) |
| `server/` | `TransferCooldownTracker` + `TransferCooldownRepository`: Hypixel's command cooldown after a server switch. See [Transfer cooldown](#transfer-cooldown) |
| `dungeon/` | `DungeonClassTracker` + `DungeonClassRepository`, `DungeonClass`, `DungeonPartyMember`: selected class, run, floor, party classes. See [Dungeon classes](#dungeon-classes-and-runs) |
| `pet/` | `PetTracker` + `ActivePetRepository`, `Pet`, `PetRarity`, `PetInfoParser`, `PetChangeSource`: the active pet. See [Pets](#pets) |
| `diana/` | `DianaTracker`, `MythologicalMob`: the Mythological Ritual. See [Diana](#diana) |
| `sidebar/` | `SidebarEventTracker`, `SidebarEventType`: timed events, mining event, Jacob's contest, Kuudra, Garden, heat and Bingo from the sidebar. See [Sidebar events](#sidebar-events) |
| `time/` | `SkyblockCalendar`, `SkyblockDate`, `SkyblockMonth`, `SkyblockEventCalendar`, `SkyblockCalendarEvent`: the SkyBlock clock and fixed event windows. See [Calendar](#calendar) |
| `mayor/` | `MayorService`: election data from the Hypixel API (mayor, minister, perks). See [Mayor](#mayor) |
| `entity/` | `EntityClassifier`: player / NPC / other, with the NPC heuristic (v2 UUID or no tab list entry) |
| `item/` | `HypixelExtraAttributes` (item NBT), `ItemIdUtil` (display name → Hypixel id) |
| `price/` | `ItemPriceService` + `ItemPriceProvider` chain |
| `profit/` | `ProfitService`, `ProfitRepository`, `!profit`; combines SkyHanni's trackers with SkySens' own |

## Known vs unknown

Right after joining Hypixel or SkyBlock (or after switching profile or island) a tracker often has not seen the data
yet. A plain `false`, `null` or empty list would then be indistinguishable from "really not" and could make a feature
act on a guess. The convention for every tracker in `skyblock/`:

- **State must be rebuildable.** Read sources that exist again after a rejoin: the Hypixel Mod API, the sidebar, the tab
  list, item NBT, an API fetch, or saved per-profile state that is restored on `ProfileChangeEvent`. A one-time start
  message ("Slayer quest started!", "You summoned your Rabbit") may *update* state but must never be the only source of truth.
- **Expose "known" separately.** Either a tri-state getter (`Optional<Boolean>`, empty = unknown), an `isXKnown()` /
  `isKnown()` flag, or a nullable value documented as "null = unknown". A plain `isX()` convenience getter may map unknown
  to `false` and says so in its javadoc.
- **Reset the known flag** on `SkyblockJoinEvent` / `SkyblockQuitEvent` / `LocationChangeEvent` as appropriate, and set it
  again only when the source produced data.
- **Events fire on real changes only**, and not for the transition from unknown to the first known value (the restore after a
  rejoin is silent).

| Tracker | "Known" signal | Unknown until | Reset by |
|---|---|---|---|
| `LocationService` | `getCurrentLocation()` / `getCurrentZone()` not `null` | the Mod API location packet / the first sidebar zone line | an island change resets the zone |
| `PartyTracker` | `getInParty()` is not empty | the Mod API answered | quit / join |
| `SlayerTracker` | `isSidebarKnown()` | the sidebar was read once since the last join or island change | `SkyblockJoinEvent`, `SkyblockQuitEvent`, `LocationChangeEvent` |
| `PetTracker` | `isStateKnown()` | the saved pet of the current profile was restored on `ProfileChangeEvent` | `SkyblockQuitEvent` |
| `DungeonClassTracker` | `getSelectedClass() != null` (level `0` = unknown); `getFloor()` null and `getPartyMembers()` empty until read | the class was seen (chat, menu, tab list) or restored from `DungeonClassRepository` on `ProfileChangeEvent` | the run state is cleared on quit |
| `SidebarEventTracker` | `isKnown()` | the first sidebar read since join / island change | `SkyblockJoinEvent`, `SkyblockQuitEvent`, `LocationChangeEvent` |
| `DianaTracker` | `Optional` getters; `getChainProgress()` is `null` | mayor data fetched (ritual), player present (spade); the burrow chain only comes from chat | quit; an island change resets the burrow state |
| `MayorService` | `isDataKnown()` | the first successful election fetch | never within a session |
| `SkyblockCalendar`, `SkyblockEventCalendar` | always known (pure math on the real clock) | n/a | n/a |
| `TransferCooldownTracker` | `isActive()` is a plain state (cooldown running), not tri-state | n/a | n/a |

Macro conditions turn these into `Tri` values, see [macros.md](macros.md#tri-state-semantics).

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

### Slayer

`SlayerTracker.getInstance()`: `isQuestActive()`, `isBossSpawned()`, `isBossCocooned()`, `getBoss()` (as shown, "Revenant
Horror IV"), `getBossName()` (without the tier), `getBossTier()` (1-5, 0 = unknown) and **`isSidebarKnown()`**: true once the
sidebar was read since the last join or island change. Until then "no quest" may only mean "not read yet", so consumers that
must not guess check it first. Chat messages and the sidebar both feed the state; the sidebar is the source of truth after a
rejoin. Events: `SlayerQuestStartEvent`, `SlayerBossSpawnEvent`, `SlayerBossCocoonEvent`, `SlayerQuestEndEvent`.

### Pets

`PetTracker` (singleton, always active) knows the active pet; any feature can read it:
`PetTracker.getInstance().getActivePet()` (`Optional<Pet>`), `isActive("GOLDEN_DRAGON")`, `hasActivePet()`,
`getKnownPets()` (every pet seen in the /pets menu on this profile) and **`isStateKnown()`** (false until the saved pet of
the current profile was restored, and again after leaving SkyBlock; while false, an empty `getActivePet()` does not mean "no
pet"). `Pet` is a record (type id, name, `PetRarity`, level, held item id, skin, uuid, candy used). `PetInfoParser` builds a
`Pet` from an item's `petInfo` ExtraAttributes (`fromStack`, reusable for held-item/inventory checks) or from "[Lvl 100]
Golden Dragon" (`fromDisplay`, rarity from the color codes).

Change sources, each tagged with a `PetChangeSource`: `CHAT` ("You summoned/despawned your X!"), `AUTOPET`
("Autopet equipped your [Lvl N] X! VIEW RULE"), `PETS_MENU` (click on a pet in /pets, or the menu showing an active
pet: `petInfo.active` / "Click to despawn!"), `LOADOUT` and `TAB_LIST` (the "Pet:" widget, read once a second). The
tab list is the fallback and source of truth, but is ignored for 6 s after another source changed the pet and must read
the same twice, so one change fires one event. Hypixel has no known loadout chat/GUI format: a change seen by the tab
list within 10 s after a chat line mentioning a "loadout" (`You equipped Loadout N!`) is tagged `LOADOUT`, otherwise it is
`TAB_LIST`. "Your X leveled up to level N!" updates the level. The active pet is saved per profile name in
`ActivePetRepository` and restored silently after joining (`ProfileChangeEvent`).

Events (`skyblock/event/`): `PetChangeEvent(oldPet, newPet, source)` (`newPet` null = despawned),
`PetLevelUpEvent(pet, oldLevel, newLevel)`, `PetsMenuUpdateEvent(knownPets)`. They only fire on real changes.

### Dungeon classes and runs

`DungeonClassTracker.getInstance()` (registered as a feature): `getSelectedClass()` (`@Nullable DungeonClass`: `HEALER`,
`MAGE`, `BERSERK`, `ARCHER`, `TANK`; null = unknown), `getSelectedClassLevel()` (0 = unknown), `isSelectedClass(c)`,
`isInDungeon()`, `getFloor()` (`"F7"` / `"M7"`, null outside a run or while unread), `getPartyMembers()` (run only, including
the player; `DungeonPartyMember(name, dungeonClass, level, alive)`), `getPartyMember(name)` and `getClassOf(name)`. Sources:
"You selected the X Dungeon Class!", the party finder join line, the dungeon tab list ("Name (Mage XXV)") and the class menu;
the floor comes from the sidebar in the Catacombs. The own class and level are saved per profile in `DungeonClassRepository`
(`selectedClasses`, `classLevels`) and restored silently on `ProfileChangeEvent`. Events: `DungeonClassChangeEvent`,
`DungeonClassLevelUpEvent`, `DungeonPartyClassesUpdateEvent`, only on real changes.

### Diana

`DianaTracker` (always active, registered in `FeatureService`) tracks the Mythological Ritual for any feature.

- **Ritual:** `MayorService.isMythologicalRitualActive()` (perk "Mythological Ritual"; Jerry's "Perkpocalypse" is not
  counted because the API doesn't say whose perks it borrows). The chat line "The mythological ritual isn't active!"
  forces it to false until the next successful mayor refresh. `MayorService.hasPerk(name)` and `isDataKnown()` are generic.
- **Doing Diana:** hub + ritual + a spade (`ANCESTRAL_SPADE`, `ARCHAIC_SPADE`, `DEIFIC_SPADE`) in the hotbar. The Griffin pet is read from `PetTracker`.
- **Chat only (unknown until seen):** burrow chain progress `(x/y)`, mob spawns (`MythologicalMob`, with `isRare()`), treasure drops.
- **Tri-state:** `getRitualActive()`, `getSpadeInHotbar()` and `getDoingDiana()` return `Optional<Boolean>`, empty meaning unknown
  (e.g. before the first mayor fetch). `getDoingDiana()` is already false when one part is known to be false (e.g. not on the
  hub), even if another is unknown. `getChainProgress()` is null until the first burrow line. The plain `isX()` getters treat
  unknown as false.
- **Events:** `DianaRitualChangeEvent`, `DianaDoingChangeEvent` (not fired when a value first becomes known),
  `GriffinBurrowDugEvent`, `MythologicalMobSpawnEvent`, `DianaTreasureEvent`.
- Chain, last mob and rare-mob state reset on quit and on island change. Patterns and sources:
  `docs/specs/skyblock-events-research.md`.

### Sidebar events

`SidebarEventTracker` reads the sidebar once a second and exposes the timed events (`SidebarEventType`: Spooky Festival,
Traveling Zoo, New Year), the mining event and zone, Jacob's contest and crop, Kuudra tier and tokens, Garden visitors,
Crystal Hollows heat and the Bingo profile. It is **tri-state**: `isKnown()` is false (and the `Optional` / `OptionalInt`
getters are empty; the plain `isEventActive` returns false) until the first sidebar read since joining or an island change, so
callers can tell unknown from false. State is rebuilt from the sidebar after a rejoin. The first read fires no events; later
real changes fire `SidebarEventChangeEvent`, `MiningEventChangeEvent`, `JacobsContestChangeEvent`, `KuudraTierChangeEvent` and
`GardenVisitorsChangeEvent`. The patterns are in `docs/specs/skyblock-events-research.md`.

### Calendar

`SkyblockCalendar` (`skyblock/time/`) is pure math on the real time: year 1 began 2019-06-11 17:55 UTC, a day is 20 real
minutes, a month 31 days, a year 12 months (124 real hours). `date(ms)` / `now()` return a `SkyblockDate` (year,
`SkyblockMonth` with its season, day 1-31, hour, minute; the day is taken to start at 0:00, the offset to the in-game clock
is unverified). `SkyblockEventCalendar` holds fixed windows (`SkyblockCalendarEvent`: Spooky Festival Late Autumn 29-31,
Season of Jerry Late Winter 24-26, New Year Late Winter 29-31, Traveling Zoo Early Summer and Early Winter 1-3) with the
static `isEventActive(event)` and `nextStart(event)`, and fires `SkyblockCalendarEventChangeEvent` once a second on
transitions while on SkyBlock. It is never unknown. Dark Auction, Jacob's contest, Hoppity's Hunt and Carnival are not
modelled (timing unconfirmed); use the sidebar tracker.

### Mayor

`MayorService.getInstance()` fetches `https://api.hypixel.net/v2/resources/skyblock/election` (every 10 minutes, retry after
1 minute; `refreshIfNeeded()` is time-guarded and asynchronous, consumers call it). API: `isDataKnown()` (false until the
first successful fetch; before that `hasPerk` says false), `getDataVersion()` (increases with each successful refresh),
`hasPerk(name)` (mayor or minister, case-insensitive), `isSlayerXpBuffActive()`, `isMythologicalRitualActive()`,
`getMayorName()`, `getMinisterName()` and `getElection()`, an immutable snapshot `Election(mayor, minister, mayorPerks,
ministerPerks)` with the perk names in their original spelling. Consumers: `DianaTracker`, the slayer RNG meter and the macro
Mayor condition.

### Transfer cooldown

`TransferCooldownTracker` (`skyblock/server/`, registered as a feature, always on): Hypixel blocks transfer commands
(/warp, /hub, ...) for a few seconds after a server switch. A cooldown (4 s, `DEFAULT_COOLDOWN_MS`) starts on every server
switch seen in the Mod API location packet (`onServerSwitch`), is corrected by the chat message "You may only use this command
after Ns on the server!" and then also learns the command sent just before as transfer-limited (saved in
`TransferCooldownRepository.learnedCommands`; do not rename). API: `isActive()` (cooldown running), `remainingMs()`,
`isTransferLimited(command)` (defaults in `DEFAULT_TRANSFER_LIMITED`: warp, is, hub, visit, lobby, l, play, server, garden,
dungeon_hub, bz, ah, `p warp`, `party warp`; add new roots there) and `learnedCommands()`. Outgoing commands are seen through
Fabric's `ClientSendMessageEvents.COMMAND`. Fires `TransferCooldownStartEvent`, `TransferCooldownEndEvent` and
`TransferCooldownDeniedEvent` (with the refused command if it was sent within `RECENT_SEND_MS`). The macro send path
(`TransferWaitQueue`) uses it when "Wait for Transfer Cooldown" in the General category is on.

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
