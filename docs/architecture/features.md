# Features

A feature is a class in `features/<name>/` that implements one or more **hook interfaces** from `core/feature/`.
Each hook interface stands for one injection point in Minecraft. A mixin or Fabric hook in `platform/` loops over
the registered features of that type and calls them.

## Hook interfaces

All extend `Feature` (`isActive()`, `priority()`; lower priority runs first).

| Interface | Called from | When |
|---|---|---|
| `HandledScreenFeature` | `HandledScreenMixin` | Drawing a slot / clicking in a container whose title matches `inventoryName()` |
| `LivingEntityFeature` | `LivingEntityMixin` | Swing duration and attack animation; the first feature to cancel `cir` wins |
| `EntityRenderFeature` | `EntityRenderDispatcherMixin`, `EntityScaleHelper` | Model scale, name tag scale, hiding entities |
| `HeldItemRenderFeature` | `ItemInHandRendererMixin` | Transforming the first-person held item |
| `CameraFeature` | `MinecraftMixin`, `HudMixin` | Camera perspective cycling, forcing the crosshair |
| `MouseFeature` | `MouseMixin` | Mouse sensitivity multiplier |
| `ItemRightClickFeature` | `platform/hook/UseHandlers` | Right-clicking with an item |
| `AfterTickFeature` | `platform/hook/TickHandlers` | End of every client tick, **only while on SkyBlock** |
| `GameMessageListeningFeature` | `platform/hook/ChatHandler` | Every incoming game (chat) message |
| `RawGameMessageListeningFeature` | `SystemChatPacketMixin` → `ChatHandler.onRawGameMessage` | Every game message as the server sent it, before Fabric's events and other mods cancel or rewrite it |
| `RawInputFeature` | `KeyboardHandlerMixin` (`keyPress`), `MouseMixin` (`onButton`) | Raw key and mouse button events (press, release, repeat) at the head of the handler, before screens and key mappings. Cancelling `ci` swallows the event. Used by `MacroFeature` |
| `HudRenderFeature` | `platform/hook/HudHandlers` | Every frame on top of the HUD, **only while on SkyBlock** and not hidden with F1 |

The dispatchers check `isActive()` first, with one exception: `GameMessageListeningFeature` is called
unconditionally, so it must check `isActive()` and `SkysensClient.getInstance().isOnSkyBlock()` itself
(see `ChatCommandFeature`). Messages SkySens sends itself are skipped (`ClientUtil.isSendingOwnMessage()`). Messages
hidden by the chat filter are still delivered to the other features.

`RawGameMessageListeningFeature` is also called unconditionally. Use it for parsers that need Hypixel's original
message: other mods (Skyblocker, SkyHanni) cancel some messages ("Profile ID: ...") and rewrite others (Skyblocker
adds prices to the `[Sacks]` hover) before `GameMessageListeningFeature` sees them. `SackTracker` and `ProfileService`
use it. Features that react to what is actually shown in chat keep using `GameMessageListeningFeature`.

`RawInputFeature` sees **every** key/button event of the game window, also off SkyBlock, in any screen and while typing
into text fields, so the dispatcher only checks `isActive()` and the feature has to gate itself (SkyBlock, open screen,
focused text field; see `MacroGate`). Cancel only when you really handled the event: the contract is "consume the key
only when an action ran", otherwise other bindings and mods must still receive it. `action` is the GLFW action
(1 press, 0 release, 2 repeat). See [platform.md](platform.md#raw-input-hooks).

## Movable HUDs

Every HUD element should be movable in the HUD editor (`/skysens hud`, or "Edit HUD Positions" in General). Implement
`core/hud/MovableHud` for it: size, position and scale getters/setters, a preview that also works without data, and a
reset. A `HudRenderFeature` returns its elements from `movableHuds()`; HUDs that don't belong to a feature register in
`HudRegistry` (see `SecondaryActionBarHud`, registered in `HudHandlers`). While the editor is open
`HudRegistry.isEditing()` is true and the live HUD isn't drawn, the editor draws the previews. Store positions in
percent of the screen; for an element with a fixed default place use `HudPosition`, which keeps the default until the
element is moved.

## Adding a feature

1. Create `features/<name>/<Name>Feature.java`, annotate it with `@SkySensFeature` (a marker only) and implement the
   hook interfaces it needs. `isActive()` usually just returns the feature's config toggle.
2. Add its options in `features/<name>/config/` and hang them into a category, either a new `@Category` field in
   `SkysensConfig` or an `@Accordion` in an existing category. See [config-and-persistence.md](config-and-persistence.md).
3. Register an instance in `FeatureService.register()`. Without this the feature never runs.
4. If no hook interface fits, add one. See [platform.md](platform.md).

A feature that other code needs to reach is kept as a field on `FeatureService` (e.g. `getRewardChestFeature()`) or
is a singleton (`DungeonRunDetector.getInstance()`, `CombatTracker`, `AfkTracker`, `SlayerTracker`).

### Larger features: macros

Most features are one class plus config. The conditional macros (`features/macro/`) are the biggest one and show how
a feature can have its own sub-architecture: `MacroFeature` is a thin `RawInputFeature` that hands every key event to
`MacroEngine`; the model, node/condition registries, engine, actions, analysis and editor screen are plain packages
below it, with their own explicit registries (`NodeTypes`, `ConditionTypes`, `PlaceholderRegistry`). The macro engine
reads game state only through the shared trackers in `skyblock/` (party, slayer, dungeon class, pet, sidebar events, mayor,
calendar, transfer cooldown). Developer guide: [macros.md](macros.md); user guide: [../features/macros.md](../features/macros.md).

## Events

For things that happen outside Minecraft's call graph there's a tiny synchronous bus in `core/event/`:

```java
EventListenerRegistry.REGISTRY.registerListener(this::onZoneChange, ZoneChangeEvent.class);
EventListenerRegistry.REGISTRY.fireEvent(new ZoneChangeEvent(...));
```

Listeners are matched by exact event class (no inheritance) and run on the firing thread. Existing events:

| Event | Fired by |
|---|---|
| `SkyblockJoinEvent`, `SkyblockQuitEvent` | `SkysensClient` (Hypixel Mod API location packet, disconnect) |
| `LocationChangeEvent`, `ZoneChangeEvent` | `LocationService` |
| `DungeonRunCompleteEvent` | `DungeonRunDetector` |
| `ProfileChangeEvent` | `ProfileService` (profile name from chat, or once from the tab list after joining) |
| `PartyJoinEvent`, `PartyLeaveEvent`, `PartyKickEvent`, `PartyMemberJoinEvent`, `PartyMemberLeaveEvent`, `PartyRoleChangeEvent`, `PartyMembersUpdateEvent` | `PartyTracker` (party chat messages, `/party list`, Mod API party info) |
| `CoopMemberJoinEvent`, `CoopMemberQuitEvent` | `CoopMemberTracker` ("<name> joined/left SkyBlock." in chat) |
| `CoopMembersUpdateEvent` | `CoopMemberTracker` (co-op widget in the tab list on the private island changed) |
| `SackItemsAddedEvent`, `SackItemsRemovedEvent` | `SackTracker` in `skyblock/sack/` (the "[Sacks] +N items, -N items." chat message, one event per direction) |
| `PetChangeEvent`, `PetLevelUpEvent`, `PetsMenuUpdateEvent` | `PetTracker` (chat, autopet, /pets menu, tab list; real changes only) |
| `DungeonClassChangeEvent`, `DungeonClassLevelUpEvent`, `DungeonPartyClassesUpdateEvent` | `DungeonClassTracker` (own class and the run party's classes; real changes only) |
| `DianaRitualChangeEvent`, `DianaDoingChangeEvent`, `GriffinBurrowDugEvent`, `MythologicalMobSpawnEvent`, `DianaTreasureEvent` | `DianaTracker` |
| `SidebarEventChangeEvent`, `MiningEventChangeEvent`, `JacobsContestChangeEvent`, `KuudraTierChangeEvent`, `GardenVisitorsChangeEvent` | `SidebarEventTracker` (never on the first read after joining) |
| `SkyblockCalendarEventChangeEvent` | `SkyblockEventCalendar` (event window starts/ends, checked once a second on SkyBlock) |
| `SlayerQuestStartEvent`, `SlayerBossSpawnEvent`, `SlayerBossCocoonEvent`, `SlayerQuestEndEvent` | `SlayerTracker` |
| `TransferCooldownStartEvent`, `TransferCooldownEndEvent`, `TransferCooldownDeniedEvent` | `TransferCooldownTracker` (server switch, cooldown over, "You may only use this command after Ns" message) |

Domain events go in `skyblock/event/`. An event that only matters inside one feature stays in that feature's
package (like `DungeonRunCompleteEvent`).

## Notifications

To tell the player something, use `NotificationService.getInstance().sendNotification(...)` (title, chat
or action bar, depending on the user's General settings; held back while AFK if configured), or `ClientUtil.sendPrefixed(...)` for
plain chat output.

Every notification has a `NotificationLevelEnum` (`.level(...)` on the builder, default `NORMAL`). `NORMAL` ones wait
while notifications are held (AFK, combat, dungeons/Kuudra, slayer quests); `IMPORTANT` ones ignore those holds and are
shown before queued normal ones. The user picks the level of each notification next to the feature's own option
(e.g. `OrderAlerts.outbidLevel`, `SlayerRngMeterAccordion.notifyWhenFullLevel`), so a new notification gets a
`NotificationLevelEnum` field in its feature's config (description suffixed with `NotificationLevelEnum.LEVELS_DESC`)
and passes it to `.level(...)`. `ClientUtil.sendDebug(...)` only prints when Dev → Debug Mode is on.
