# Architecture overview

## Source sets

Loom splits the code into two source sets (`splitEnvironmentSourceSets()` in `build.gradle.kts`):

- `src/main/` holds only resources and an empty `Skysens` main initializer: `fabric.mod.json`,
  `skysens.client.mixins.json` and `skysens.accesswidener`.
- `src/client/` holds the actual mod: `de.vantrex.skysens.client.*`, plus `assets/skysens/` (lang file and the
  config screen fonts).

## Startup

`SkysensClient.onInitializeClient()` runs once, in this order:

1. Creates `config/skysens/`.
2. `RepositoryRegistry.loadRepositories()` reads the JSON repositories (see
   [config-and-persistence.md](config-and-persistence.md)).
3. `SkysensConfig.getConfig()` loads `config/skysens/config.json` through MoulConfig.
4. Subscribes to the Hypixel Mod API location packet. That packet is the only source of `isOnSkyBlock()` and
   fires `SkyblockJoinEvent` / `SkyblockQuitEvent`. It also feeds `LocationService` (location, then zones).
5. Adds shutdown hooks that save the config and the repositories.
6. Registers the `/skysens` client command tree (config screen, `scale export|import|reset`,
   `dungeon profit [reset [alltime]]`) and the dev-only `/mocklocation`.
7. Registers the Fabric hooks in `platform/hook/` (use, tick, chat, keys). Their static init touches
   `FeatureService.getInstance()`, which creates and registers every feature.
8. Starts `ProfitService`.

`SkysensClient.SCHEDULER` is a small daemon-thread pool for delayed work. Anything that touches Minecraft state
must hop back with `Minecraft.getInstance().execute(...)`.

## Layers and dependency direction

```
platform (mixin, hook) ──► core (Feature interfaces, FeatureRegistry, events)
                                   ▲
features/<name> ───────────────────┘
      │
      ├──► skyblock (location, player, price, profit, domain events)
      ├──► integration (SkyHanni, Odin, Hypixel API; reflection only)
      ├──► chatcommand, notification, stats, persistence
      └──► config (SkysensConfig root)
ui ──► config (reads MoulConfig's processed option tree)
```

- `platform` should talk to features through the hook interfaces in `core/feature/`. There are a few direct calls
  where a hook interface would only have one implementation: the visuals render helpers (`EntityScaleHelper`,
  `SkysensRenderStateExt`), `DungeonRunDetector` (from `ChatComponentMixin`), `InternalStatsProvider` (from
  `ServerStatsMixin`) and `WarpToMenuScreen` (from `ChatHandler`/`KeyHandlers`). Don't add more without a reason.
- A feature may use any shared package, but not another feature's internals. If two features need the same
  thing, move it to `skyblock/` (domain logic) or `util/` (generic helpers).
- `SkysensConfig` is the root that references every category, so `config/` imports each feature's `config/`
  subpackage. That cycle is intended: the config tree has to know all categories.

## Where new code goes

| You are adding… | Put it in… |
|---|---|
| A new user-facing feature | `features/<name>/`, its options in `features/<name>/config/` |
| A behaviour change that needs a new injection point | A mixin in `platform/mixin/` plus a hook interface in `core/feature/` (see [platform.md](platform.md)) |
| Reusable SkyBlock knowledge (zones, item ids, prices, player state) | `skyblock/<area>/` |
| A `!command` | Generic: `chatcommand/impl/`. Owned by one feature: that feature's package |
| Data that must survive restarts | A repository registered in `RepositoryRegistry` |
| Reading another mod's data | `integration/<mod>/`, behind a provider or bridge interface with a fallback |

## Singletons

Services are eager singletons (`@Getter private static final X instance = new X()`), reached through
`X.getInstance()`. Config is read live through `SkysensConfig.getInstance().<category>.<field>`. Features read it
on every call (usually in `isActive()`), so toggling an option takes effect immediately.
