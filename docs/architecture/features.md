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

The dispatchers check `isActive()` first, with one exception: `GameMessageListeningFeature` is called
unconditionally, so it must check `isActive()` and `SkysensClient.getInstance().isOnSkyBlock()` itself
(see `ChatCommandFeature`). Messages SkySens sends itself are skipped (`ClientUtil.isSendingOwnMessage()`). Messages
hidden by the chat filter are still delivered to the other features.

## Adding a feature

1. Create `features/<name>/<Name>Feature.java`, annotate it with `@SkySensFeature` (a marker only) and implement the
   hook interfaces it needs. `isActive()` usually just returns the feature's config toggle.
2. Add its options in `features/<name>/config/` and hang them into a category, either a new `@Category` field in
   `SkysensConfig` or an `@Accordion` in an existing category. See [config-and-persistence.md](config-and-persistence.md).
3. Register an instance in `FeatureService.register()`. Without this the feature never runs.
4. If no hook interface fits, add one. See [platform.md](platform.md).

A feature that other code needs to reach is kept as a field on `FeatureService` (e.g. `getRewardChestFeature()`) or
is a singleton (`DungeonRunDetector.getInstance()`, `CombatTracker`, `AfkTracker`, `SlayerTracker`).

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

Domain events go in `skyblock/event/`. An event that only matters inside one feature stays in that feature's
package (like `DungeonRunCompleteEvent`).

## Notifications

To tell the player something, use `NotificationService.getInstance().sendNotification(...)` (title, chat
or action bar, depending on the user's General settings; held back while AFK if configured), or `ClientUtil.sendPrefixed(...)` for
plain chat output. `ClientUtil.sendDebug(...)` only prints when Dev → Debug Mode is on.
