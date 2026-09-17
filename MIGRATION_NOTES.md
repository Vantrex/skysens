# Migration note — what moved, what stayed, and why

Companion to `ARCHITECTURE.md`. This pass split a single-module Gradle project into
three modules. **No behaviour changed and zero features were migrated to the
server.**

---

## What moved

| From | To | How |
|---|---|---|
| `src/main/**`, `src/client/**` | `skysens-mod/src/main/**`, `skysens-mod/src/client/**` | `git mv src skysens-mod/src` — one move, rename detection intact, paths otherwise unchanged |
| Everything in the old root `build.gradle.kts` | `skysens-mod/build.gradle.kts` | Including `loom {}`, the shadow configurations, `tasks.shadowJar`, `tasks.jar` and `tasks.processResources` |
| 29 audited classes (see below) | `skysens-common` | `git mv` per file, then a package/import rewrite |
| `all_locations.json` (repo root) | `skysens-server/src/main/resources/assets/` (authoritative) + `skysens-mod/src/client/resources/assets/` (offline snapshot) | Read by no code; it is the hand-maintained source the zone enums were written from |

The split environment source set layout is preserved exactly. `fabric.mod.json`,
`skysens.client.mixins.json`, `skysens.accesswidener` and `splits.json` moved with
their source sets.

### Classes that moved to `skysens-common` (29)

All were audited individually for Minecraft/Fabric imports; none had any.

- `common.domain.dungeon` — `DungeonSplit`, `DungeonFloorEnum`, `DungeonModeEnum`,
  `DungeonStageEnum`, `F7BossTypeEnum`, `F7PhaseEnum`
- `common.domain.location` — `SkyblockLocationEnum`, `Zone`
- `common.domain.location.zone` — `ZoneEnum` + 17 `*ZoneEnum`
- `common.domain.notification` — `NotificationDisplayTypeEnum`
- `common.domain.sensitivity` — `SensitivityConfiguration`
- `common.util` — `NumberUtil`

Their package declarations and every import of them across the mod were rewritten.
No class body was edited.

---

## What stayed in `skysens-mod`, and why

### Minecraft-coupled, by audit

| Class | Coupling |
|---|---|
| `client/model/Notification` | `net.minecraft.network.chat.Component` |
| `client/util/DungeonStatParser` | `net.minecraft.network.chat.Component`, `net.minecraft.ChatFormatting` — parses Minecraft chat components; meaningless server-side |
| `client/util/Lazy` | Guava `com.google.common.base.Suppliers`, which arrives transitively from Minecraft. Adding a Guava dependency to `skysens-common` to relocate a 20-line helper is a bad trade, and it has no wire role |

Where the server eventually needs one of these, a plain DTO goes in
`skysens-common` and an explicit mapping layer in `client/net/mapper/`. A Minecraft
type is never dragged into common to make it fit.

### Everything else

All 70 remaining source files stayed put, unchanged except for rewritten imports:
features, mixins, handlers, services, the repository layer, MoulConfig config
categories and GUI editors, and the event system. None of them has a server-side
counterpart in this pass, and moving code with no consumer on the other side would
be churn.

In particular **`client/repository/` was not replaced.** `client/net/` sits beside
it: the repository layer remains the source of truth features read from, and the net
layer is only ever an asynchronous refresher of that cache (ADR-010).

---

## What is new

| Path | Contents |
|---|---|
| `skysens-common/…/dto/**` | Wire DTOs as records, one package per domain |
| `skysens-common/…/api/**` | Plain-Java contract interfaces mirroring the §7 endpoint table |
| `skysens-common/…/version/ApiVersion` | API version constant + compatibility check |
| `skysens-server/**` | Spring Boot skeleton: controllers, services + stub impls, repositories, entities, mappers, security, exception handling, `application.yml` with `dev`/`prod` profiles |
| `skysens-mod/…/client/net/**` | `SkysensApiClient`, `ApiEndpoints`, `SkysensSession`, `cache/`, `mapper/`, `sync/` — skeleton only |
| `skysens-mod/…/config/categories/server/ServerCategory` | Six opt-in toggles, all defaulting to off |
| `ARCHITECTURE.md`, `MIGRATION_NOTES.md` | This design record |

**Nothing new is wired in.** `client/net/` is not referenced from
`SkysensClient.onInitializeClient()`, no feature calls it, and every stub body
carries a `TODO(deferred): <what>` marker.

The one change to an existing file's behaviour surface is the new `ServerCategory`
field on `SkysensConfig`. It adds a "Server" tab to the config GUI with six toggles
that nothing reads. MoulConfig persists by field name, so existing `config.json`
files deserialize unchanged and simply gain the new defaults.

---

## Why the moves are behaviour-preserving

1. **Only package declarations and imports changed.** No class body, no field, no
   method signature was edited in any moved or referencing file.
2. **Gson persistence is unaffected.** `RepositoryRegistry` derives its on-disk
   filename from `clazz.getSimpleName()`, not from a fully-qualified name, so moving
   a class across packages cannot orphan a user's stored data. This was checked
   before the move, not assumed. `SimpleKeyValueRepository` — the only persisted
   repository — did not move at all.
3. **Config JSON is field-name-keyed.** MoulConfig and Gson serialize by field name,
   and `Map<SkyblockLocationEnum, …>` serializes enum *constant names*. Moving
   `SkyblockLocationEnum` and `SensitivityConfiguration` therefore does not change a
   single byte of an existing `config.json`.
4. **The jar is intact.** Verified after the move: MoulConfig still relocated to
   `de/vantrex/skysens/dependencies/moulconfig/` (299 entries), zero unrelocated
   MoulConfig entries, zero `com/google/gson/` entries, 58 `skysens-common` classes
   bundled, and `fabric.mod.json` expanded with real values rather than a literal
   `${version}`.

---

## Verification status

| Criterion | Status |
|---|---|
| `./gradlew build` succeeds at the root, all three modules | ✅ verified |
| `./gradlew :skysens-mod:shadowJar` produces a jar, MoulConfig relocation intact | ✅ verified (see point 4 above) |
| `./gradlew :skysens-server:bootRun` starts with no Minecraft or Fabric on its classpath | ✅ verified — context loads and the server runtime classpath contains zero `minecraft`/`fabric` entries |
| `skysens-common` compiles with neither Minecraft, Fabric nor Spring | ✅ verified — its runtime classpath is literally empty; gson and annotations are `compileOnly` |
| Every stub carries a `TODO(deferred)` marker | ✅ verified |
| No behavioural change; zero features migrated | ✅ by construction, argued above |
| **The mod launches and every feature behaves identically** | ⚠️ **not verified.** This needs a real Minecraft client on Hypixel SkyBlock and cannot be checked from a build environment. The static argument is points 1–4 above; it is not a substitute for launching the game |

---

## Follow-ups

1. **Launch the mod and exercise the features.** The one acceptance criterion that
   could not be checked here.
2. Migrate to a Gradle version catalog (ADR-007).
3. Drop `io.spring.dependency-management` if Boot 4's own BOM handling proves
   sufficient (ADR-006).
4. Resolve the open legal decisions in `ARCHITECTURE.md` §9 — published-content
   survival after account deletion, and the in-mod privacy notice.
5. Decide whether `client/net/` ever needs `Lazy`; if so, move it with an explicit
   Guava dependency rather than duplicating it.
