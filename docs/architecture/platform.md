# Platform layer: mixins and Fabric hooks

`platform/` is the only place that hooks into Minecraft. Keep logic out of it: a mixin finds the features for its
injection point and delegates to them.

- `platform/mixin/`: all mixins, package `de.vantrex.skysens.client.platform.mixin`.
- `platform/hook/`: Fabric API callbacks, registered from `SkysensClient.onInitializeClient()`:
  `UseHandlers` (use item), `TickHandlers` (end of client tick), `ChatHandler` (incoming game messages, and
  intercepting a bare `!warpto`), `KeyHandlers` (key mappings, category `key.category.skysens.main`),
  `HudHandlers` (HUD elements: the secondary action bar, see `ClientUtil.sendSecondaryActionBar`).

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
| `MouseMixin` | `MouseHandler` | `MouseFeature`: sensitivity |
| `ClientPlayNetworkMixin` | `ClientPacketListener` | Strips "Don't <action>!" from titles |
| `ServerStatsMixin` | `ClientPacketListener` | Feeds time and pong packets to `InternalStatsProvider` |
| `ChatComponentMixin` | `ChatComponent` | Messages other mods add straight to chat (no game message event) → `DungeonRunDetector` |
| `PlayerTabOverlayAccessor` | `PlayerTabOverlay` | `@Invoker` for the tab list entries |

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
