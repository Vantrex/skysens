# Platform layer: mixins and Fabric hooks

`platform/` is the only place that hooks into Minecraft. Keep logic out of it: a mixin finds the features for its
injection point and delegates to them.

- `platform/mixin/`: all mixins, package `de.vantrex.skysens.client.platform.mixin`.
- `platform/hook/`: Fabric API callbacks, registered from `SkysensClient.onInitializeClient()`:
  `UseHandlers` (use item), `TickHandlers` (end of client tick), `ChatHandler` (incoming game messages, and
  intercepting a bare `!warpto`), `KeyHandlers` (key mappings, category `key.category.skysens.main`),
  `HudHandlers` (HUD elements: the secondary action bar, see `ClientUtil.sendSecondaryActionBar`; wraps the vanilla
  action bar and the secondary one so `ActionBarLayout` can lift them above Inventory HUD+; and one element that
  draws every `HudRenderFeature`).

## Current mixins

| Mixin | Target | Purpose |
|---|---|---|
| `HandledScreenMixin` | `AbstractContainerScreen` | `HandledScreenFeature`: slot drawing, clicks |
| `LivingEntityMixin` | `LivingEntity` | `LivingEntityFeature`: swing duration, attack animation |
| `EntityRenderDispatcherMixin` | `EntityRenderDispatcher` | `EntityRenderFeature`: hiding entities, scale context |
| `EntityRendererMixin` | `EntityRenderer` | Writes scale values into the render state, scales name tags |
| `EntityRenderStateMixin` | `EntityRenderState` | Duck interface `SkysensRenderStateExt` (extra fields on the render state) |
| `ItemEntityRendererMixin` | `ItemEntityRenderer` | Dropped item scale |
| `ItemInHandRendererMixin` | `ItemInHandRenderer` | `HeldItemRenderFeature`: held item transform |
| `MinecraftMixin` | `Minecraft` | `CameraFeature`: perspective cycling |
| `HudMixin` | `Hud` | `CameraFeature`: forced crosshair |
| `MouseMixin` | `MouseHandler` | `MouseFeature`: sensitivity (`turnPlayer` redirect); `RawInputFeature`: `onButton` HEAD inject, see [Raw input hooks](#raw-input-hooks) |
| `KeyboardHandlerMixin` | `KeyboardHandler` | `RawInputFeature`: `keyPress` HEAD inject, see [Raw input hooks](#raw-input-hooks) |
| `ClientPlayNetworkMixin` | `ClientPacketListener` | Strips "Don't <action>!" from titles |
| `ServerStatsMixin` | `ClientPacketListener` | Feeds time and pong packets to `InternalStatsProvider` |
| `SystemChatPacketMixin` | `ClientPacketListener` | `RawGameMessageListeningFeature`: system chat before Fabric's message events and other mods |
| `ChatComponentMixin` | `ChatComponent` | Messages other mods add straight to chat (no game message event) → `DungeonRunDetector` |
| `PlayerTabOverlayAccessor` | `PlayerTabOverlay` | `@Invoker` for the tab list entries |
| `InventoryHudKeyActionMixin` | Inventory HUD+ `KeyAction` (`@Pseudo`, `require = 0`) | Its key bindings ran → `ActionBarLayout.markDirty()` |

## Raw input hooks

`KeyboardHandlerMixin` (`KeyboardHandler.keyPress`) and the `onButton` inject in `MouseMixin` (`MouseHandler.onButton`) are
`@At("HEAD")` and `cancellable`. They run before screens, `EditBox` focus handling and `KeyMapping`s see the event, and
dispatch to every active `RawInputFeature` (`FeatureRegistry.getRawInputFeature()`, sorted by priority) until one cancels
`ci`. Events from other GLFW windows are ignored (`window != minecraft.getWindow().handle()`).

- Why not `KeyMapping.consumeClick()` from a tick hook (like `KeyHandlers`)? By then vanilla has already processed the
  press, so a key could not be *passed through when nothing ran*. Raw HEAD injects can.
- **Contract:** cancel only when the feature really handled the event (the macros: only when an action ran). Releases
  are never cancelled; a cancelled press must have its repeats cancelled too, otherwise vanilla sees a repeat without a press
  (`InputTracker.consume/isConsumed` does this for macros).
- The hook sits on the hottest path of the client. Look up cheaply first and do real work only for matching keys.
- The signatures were checked against 26.2 (`keyPress(long, int, KeyEvent)`, `onButton(long, MouseButtonInfo, int)`).
  After a Minecraft update `defaultRequire = 1` fails startup if they changed.

### Nothing may call GLFW during mod init

`onInitializeClient` runs before the window and GLFW are initialised. Anything that ends in GLFW at that point
(`InputConstants.Key.getDisplayName()`, `GLFW.glfwGetKeyName`, building key names for messages) crashes the game on
startup. `MacroService.init()` parses the saved macros on that path, so the macro validation (which builds messages with
key names) is **lazy** (`MacroService.validation()`), and the keybind conflict chat lines are skipped while there is no
player. Keep new init-time code free of key names, screens and window calls.

## Adding a mixin

1. Create the class in `platform/mixin/` and add its simple name to the `client` array of
   `src/main/resources/skysens.client.mixins.json`. A mixin missing from that list is silently never applied.
2. `injectors.defaultRequire` is 1: an injection that matches nothing fails at startup. That is wanted, because it
   shows a broken target right after a Minecraft update instead of letting the feature quietly stop working.
3. Prefix injected handler names with `skysens$` (e.g. `skysens$drawSlot$Head`) so they can't clash with other
   mods' mixins. Not all existing handlers follow this yet.
4. To change behaviour, add a method to an existing hook interface in `core/feature/`, or create a new interface
   extending `Feature`. Give it a list in `FeatureRegistry` (field, `instanceof` branch in `register`, entry in
   `sort`) and loop over it in the mixin, checking `isActive()`. Name hook methods `<target>$Head` / `$Tail` after
   the injection point and pass the `CallbackInfo` so features can cancel.
5. To carry data between two injection points (e.g. render state extraction → submit), add a duck interface and
   implement it in a mixin on the target class, like `SkysensRenderStateExt`.

## Access widener

`src/main/resources/skysens.accesswidener` (named `official`) opens up members a mixin `@Shadow`/`@Accessor` can't
reach. All current entries serve the custom config screen's rendering (`GuiGraphicsExtractor` scissor stack, fill
and blit internals, `RenderPipelines.GUI_SNIPPET`).

## Verifying

After touching mixins, build, start `runClient` and check `run/logs/latest.log` for `Mixin apply failed`,
`InvalidMixinException` or `ClassNotFoundException`. Then trigger the hooked behaviour in game; see
[../testing.md](../testing.md).
