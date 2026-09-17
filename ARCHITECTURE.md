# Skysens Architecture

Skysens is a Fabric mod for Minecraft 26.1.2 (Java 25) targeting Hypixel SkyBlock,
plus an optional backend that holds data shared between users of the mod.

This document records the module split, the dependency rules, the shared domains,
the identity decision, the endpoint contract, the legal position, and every
architectural decision made along the way.

> **Status.** This pass produced **structure only**: module layout, build files,
> package trees, DTO/interface signatures and an endpoint contract. No business
> logic was written, no feature was migrated, and nothing in `client/net/` is wired
> into `SkysensClient.onInitializeClient()`. Every place real logic belongs carries
> a `TODO(deferred): <what>` marker.

---

## 1. Modules

```
skysens/                        root — conventions only, no code, NO Loom
├── skysens-common/             plain Java library: DTOs, domain enums, API contract
├── skysens-mod/                the Fabric mod (all of the old src/ lives here)
└── skysens-server/             Spring Boot HTTP service (NOT a Minecraft server)
```

```
          ┌─────────────────┐
          │  skysens-mod    │   fabric-loom, shadow, MoulConfig, Minecraft
          │  (Fabric mod)   │
          └────────┬────────┘
                   │  shadowImpl(project(":skysens-common"))
                   ▼
          ┌─────────────────┐
          │ skysens-common  │   pure Java. gson + annotations are compileOnly.
          │  DTO + domain   │   No Minecraft. No Fabric. No Spring.
          └────────▲────────┘
                   │  implementation(project(":skysens-common"))
                   │
          ┌────────┴────────┐
          │ skysens-server  │   Spring Boot 4.1.1, JPA, Flyway
          │  (HTTP service) │
          └─────────────────┘
```

**Dependency direction is one-way and enforced by the build:** `mod → common` and
`server → common`. Never `common → mod`, never `mod ↔ server`. `skysens-common`
declares no dependency on anything either side cannot supply.

### Why Loom is mod-only

`fabric-loom`, `splitEnvironmentSourceSets()`, the `loom { mods { … } }` block and
`shadowJar` appear **only** in `skysens-mod/build.gradle.kts`. Applying Loom at the
root would make Gradle try to provide and remap Minecraft for `skysens-common` and
`skysens-server` as well, which breaks both. The root script keeps only shared
conventions: the Java 25 toolchain, `io.freefair.lombok`, `group`/`version`, and the
shared `repositories` block.

### How `skysens-common` gets into the mod jar

`tasks.shadowJar` sets `configurations = listOf(shadowModImpl, shadowImpl)`, so it
never bundles plain `implementation` dependencies. A plain
`implementation(project(":skysens-common"))` would compile fine and then
`NoClassDefFoundError` at runtime. The mod therefore declares:

```kotlin
shadowImpl(project(":skysens-common"))
```

This is §4.2 **option (a)**. Option (b) — `from(project(":skysens-common").sourceSets…)`
inside `tasks.shadowJar` — was rejected because it reaches across project boundaries
at configuration time, which is hostile to Gradle 9 and the configuration cache, and
because it establishes no task dependency, so the jar can be assembled from stale or
absent output. Option (a) is an ordinary dependency and Gradle wires the task graph
itself.

`skysens-common` is the project's own code, not a third-party library, so it is not
relocated. Verified: `de/vantrex/skysens/common/version/ApiVersion.class` is present
in the shadow jar, MoulConfig is still relocated under
`de/vantrex/skysens/dependencies/moulconfig/` (299 entries), and **no** `com/google/gson/`
entry exists in the jar.

### Why gson is `compileOnly` in `skysens-common`

Anything on `skysens-common`'s runtime classpath rides into the mod jar through
`shadowImpl`. Shipping a second copy of Gson would violate constraint 5 — the mod
uses the Gson that Minecraft already bundles. `@Expose` is `RUNTIME`-retention, but
the annotation type is supplied by Minecraft on the mod side and simply ignored by
Jackson server-side, so `compileOnly` is correct on both. The same reasoning applies
to `org.jetbrains:annotations`.

---

## 2. Versions

`gradle.properties` remains the single source of version truth. Plugin versions are
resolved once in `settings.gradle.kts` (`pluginManagement { plugins { … } }`) via
`providers.gradleProperty(...)`, because a `plugins {}` block cannot read
`project.findProperty(...)`. Module scripts then declare plugin ids **without**
versions. As a side effect, `loom_version` in `gradle.properties` is now actually
used — the old root script hardcoded `1.17.12` next to it.

**Version catalog: recommended, not performed.** §4.1 asked for an evaluation rather
than a silent migration. A `libs.versions.toml` catalog would be a genuine
improvement — typed accessors, one file, IDE completion — but it is a cross-cutting
rewrite of every dependency declaration in the same pass that already moves 99 source
files, and it would obscure the diff that matters. **The existing
`project.findProperty(...)` pattern is kept**, unchanged in style, and the catalog is
recommended as a separate follow-up.

---

## 3. Source layout

`src/**` moved wholesale to `skysens-mod/src/**` via `git mv`, paths otherwise
unchanged, so the split environment source set layout (`src/main/**` and
`src/client/**`) stays intact and history follows. `fabric.mod.json`,
`skysens.client.mixins.json`, `skysens.accesswidener` and `splits.json` moved with
their source sets.

`tasks.processResources` moved to `skysens-mod/build.gradle.kts` along with
everything else. It expands `version`, `minecraft_version` and `loader_version` into
`fabric.mod.json`; dropping it would ship a jar whose metadata contains a literal
`${version}`.

### `all_locations.json`

Tracked at the repo root, and read by no code — it is the hand-maintained source
from which the zone enums were written. (The `.gitignore` entry is
`all_location.json`, singular, so it never matched and the file was tracked. That
typo is left alone deliberately: fixing it would make the file ignored.)

Chosen split, per §4.3 and §5.4:

| Copy | Path | Role |
|---|---|---|
| Authoritative | `skysens-server/src/main/resources/assets/all_locations.json` | Served as a versioned asset with an ETag |
| Offline snapshot | `skysens-mod/src/client/resources/assets/all_locations.json` | Bundled fallback; the floor below which the cache never drops |

---

## 4. `skysens-common` audit

Each §4.4 candidate was checked for Minecraft/Fabric imports individually. **Nothing
was bulk-moved.**

| Candidate | Imports found | Verdict |
|---|---|---|
| `dungeon/model/DungeonSplit` | `lombok.Data` only | **Moved** → `common.domain.dungeon` |
| `dungeon/enums/DungeonFloorEnum` | lombok only | **Moved** → `common.domain.dungeon` |
| `dungeon/enums/DungeonModeEnum` | lombok only | **Moved** → `common.domain.dungeon` |
| `dungeon/enums/DungeonStageEnum` | lombok only | **Moved** → `common.domain.dungeon` |
| `dungeon/enums/F7BossTypeEnum` | lombok only | **Moved** → `common.domain.dungeon` |
| `dungeon/enums/F7PhaseEnum` | lombok only | **Moved** → `common.domain.dungeon` |
| `enums/location/SkyblockLocationEnum` | lombok, `org.jetbrains.annotations`, own zone package | **Moved** → `common.domain.location`. Large but entirely MC-free; annotations are `compileOnly` |
| `enums/location/zone/ZoneEnum` | none | **Moved** → `common.domain.location.zone` |
| `enums/location/zone/*ZoneEnum` (17 classes) | lombok only | **Moved** → `common.domain.location.zone`. All 17 are plain `(displayName, scoreboardName)` enums |
| `model/Zone` | lombok, own enums | **Moved** → `common.domain.location` |
| `enums/NotificationDisplayTypeEnum` | none | **Moved** → `common.domain.notification`. Needed by `NotificationPresetDto` (§5.6) |
| `config/SensitivityConfiguration` | gson `@Expose`, lombok, `SkyblockLocationEnum` | **Moved** → `common.domain.sensitivity`. See note below |
| `util/NumberUtil` | none | **Moved** → `common.util` |
| `model/Notification` | **`net.minecraft.network.chat.Component`** | **Stays in mod.** DTO + mapper instead |
| `util/DungeonStatParser` | **`net.minecraft.network.chat.Component`, `net.minecraft.ChatFormatting`** | **Stays in mod.** Parses Minecraft chat components; meaningless server-side |
| `util/Lazy` | `com.google.common.base.Suppliers` (Guava) | **Stays in mod.** Guava arrives transitively from Minecraft. Adding a Guava dependency to `skysens-common` to relocate a 20-line helper is a bad trade, and it has no wire role |

**On `SensitivityConfiguration`.** It is the mod's live config object *and* the
payload of the most naturally shareable domain (§5.2). It is entirely
Minecraft-free. Moving it means `SensitivityProfileDto` embeds the exact type the
mod persists locally, so publish and import are lossless by construction with no
mapping layer that could drift — which is the stated reason for the monorepo in the
first place. The server's *persistence entity* remains separate
(`server/entity/SensitivityProfileEntity`); only the wire shape is shared.

**Rule for MC-coupled types** (applied above, and binding going forward): they stay
in `skysens-mod`. A plain DTO goes in `skysens-common` and an explicit mapping layer
in `client/net/mapper/`. A Minecraft type is never dragged into common to make it
fit.

### Resulting layout

```
de/vantrex/skysens/common/
├── api/            contract interfaces mirroring §7 (plain Java, no Spring)
├── domain/
│   ├── dungeon/    DungeonSplit + 5 dungeon enums
│   ├── location/   SkyblockLocationEnum, Zone, zone/ (18 types)
│   ├── notification/  NotificationDisplayTypeEnum
│   └── sensitivity/   SensitivityConfiguration
├── dto/            wire DTOs (records), one package per domain
├── util/           NumberUtil
└── version/        ApiVersion — constant + compatibility check
```

---

## 5. Shared domains

| # | Domain | DTO(s) | Direction | Notes |
|---|---|---|---|---|
| 1 | **Dungeon splits & PBs** | `SplitDefinitionSetDto`, `RunSubmissionDto`, `PersonalBestDto` | definitions: server→client (public); runs: client→server; PBs: server→client (auth) | Highest-value domain. Replaces the static bundled `splits.json` with a versioned, updatable set, plus per-player run history |
| 2 | **Sensitivity profiles** | `SensitivityProfileDto`, `PublishProfileRequest` | publish: client→server; browse/import: server→client | The most naturally shareable thing in the mod. Payload is the mod's own `SensitivityConfiguration` |
| 3 | **Boss waypoint sets** | `WaypointSetDto`, `WaypointDto` | both | Community-editable; replaces the local data in `BossWaypointsFeature`. Uses plain ints, never a Minecraft position type |
| 4 | **Location / zone data** | `LocationAssetDto`, `VersionedAssetDto` | server→client (public) | NEU-repo style: ETag/hash, client caches, refetches only on change. Ships zone data without a mod release. Deliberately *not* `SkyblockLocationEnum` — an enum cannot grow constants at runtime, which is the whole point |
| 5 | **Bazaar aggregates** | `BazaarItemSummaryDto` | server→client | **Scope warning.** Hypixel already publishes a public, unauthenticated `/skyblock/bazaar` endpoint with live order books. Skysens must not re-serve it: that would be a staler copy and pointless load on both services. The only thing worth holding here is what the public API does not provide — derived history and volatility over a window. Until a feature needs that, this DTO stays unused |
| 6 | **Notification presets** | `NotificationPresetDto` | both | Shareable presets for the `mining/notification/` features |
| 7 | **Mod manifest & feature flags** | `ModManifestDto` | server→client (public, unauthenticated) | Hit once on startup: latest version, minimum supported API version, remote kill-switches. Also where API versioning is enforced. A feature absent from the flag map is **not** disabled — absence means "no remote opinion" and the local default wins |

---

## 6. Identity & authentication — the decision

**Decision: the Mojang session-server handshake, with the server minting its own
short-lived session token. The Mojang UUID is the canonical user key.**

### Why not the display name

The mod today knows only `PlayerService.currentPlayerName` — a display name string.
Display names are mutable and trivially spoofable: anyone can claim to be anyone.
It cannot be an identity and is never sent as one. Where a name appears in a DTO
(`SensitivityProfileDto.authorName`) it is presentation-only and no authorization
decision may read it.

### The flow

```
mod (client/net/SkysensSession)          server (server/security/)
────────────────────────────────         ─────────────────────────
1. POST /api/v1/auth/handshake  ────────▶ mint one-shot serverId, stash with TTL
                                ◀──────── { serverId, expiresInSeconds }

2. Mojang joinServer(serverId)  ────────▶ (Mojang session server)
   using the running client's own
   session from Minecraft.getInstance()

3. POST /auth/handshake/complete ───────▶ MojangSessionVerifier.verify(...)
   { serverId, username }                 GET sessionserver/../hasJoined
                                          200 ⇒ take the UUID from ITS response
                                ◀──────── { accessToken, refreshToken, playerUuid }
```

The username in step 3 is a **lookup hint only**. The identity is whatever UUID
Mojang returns; a non-200 from `hasJoined` is a failed verification and never falls
back to trusting the supplied name. The user's Mojang access token never leaves
their machine — that is precisely what this mechanism is for.

### Token storage, lifetime, refresh

| | Lifetime | Storage |
|---|---|---|
| Access token | 30 min (`skysens.auth.access-token-ttl`) | **In memory only.** Dies with the process |
| Refresh token | 30 days, single-use, rotated on every refresh | Persisted through the existing repository layer under `<configDir>/skysens/repositories/`, in a file **separate** from `SimpleKeyValueRepository.json` so it is never swept into a config export or a bug report |
| Handshake challenge | 2 min | Server-side only |

Refresh is attempted lazily on the first 401, never on a timer. A second 401
disables authenticated sync for the rest of the session; the anonymous surface keeps
working.

### Anonymous vs authenticated surface

| Anonymous | Authenticated |
|---|---|
| `GET /api/v1/manifest` | everything that reads or writes user data |
| `GET /api/v1/assets/locations{,/meta}` | run submission, personal bests |
| `GET /api/v1/splits/definitions` | publish / delete profiles, waypoints, presets |
| all of `/api/v1/auth/**` (the bootstrap path) | account deletion |

The mod must be able to complete startup with no credentials at all, so the token
filter fails **open** on the anonymous surface and closed everywhere else.

### Rate limiting & abuse (design level)

- Anonymous: 60 req/min per source IP. The manifest and assets are cacheable and
  ETag-gated, so a healthy client makes a handful of requests per launch.
- Authenticated: 300 req/min per UUID — keyed to the UUID, not the IP, because
  sharing an IP is normal and sharing a Mojang account is not.
- Publishing: 20/day per UUID. Publishing is the expensive, abusable surface
  (storage, moderation, spam).
- Run submission is idempotent on `(uuid, startedAt, floor)` so a client retrying an
  outboxed run cannot inflate its history.
- Published text (`name`, `description`) needs length caps and a moderation/report
  path before profile browsing goes live. **Open decision for the maintainer.**

---

## 7. Endpoint contract

All paths are versioned under `/api/v1/`. `ApiEndpoints` in the mod derives its
constants from `ApiVersion.PATH_SEGMENT` so a version bump cannot leave a stale path
behind.

| Method | Path | Auth | Purpose | Request DTO | Response DTO |
|---|---|---|---|---|---|
| GET | `/api/v1/manifest` | — | Startup manifest: versions, feature flags | — | `ModManifestDto` |
| POST | `/api/v1/auth/handshake` | — | Begin Mojang handshake | — | `AuthHandshakeStartResponse` |
| POST | `/api/v1/auth/handshake/complete` | — | Verify via `hasJoined`, mint token | `AuthHandshakeCompleteRequest` | `SessionTokenDto` |
| POST | `/api/v1/auth/refresh` | — | Rotate refresh token | `RefreshTokenRequest` | `SessionTokenDto` |
| DELETE | `/api/v1/auth/account` | ✅ | GDPR erasure of all data for the caller | — | — |
| GET | `/api/v1/assets/locations/meta` | — | Location asset hash + updatedAt | — | `VersionedAssetDto` |
| GET | `/api/v1/assets/locations` | — | Location/zone data (ETag, 304-able) | — | `List<LocationAssetDto>` |
| GET | `/api/v1/splits/definitions` | — | Versioned split definition set | — | `SplitDefinitionSetDto` |
| POST | `/api/v1/splits/runs` | ✅ | Submit one completed run | `RunSubmissionDto` | — |
| GET | `/api/v1/splits/personal-bests` | ✅ | Caller's PBs | — | `List<PersonalBestDto>` |
| GET | `/api/v1/profiles/sensitivity` | — | Browse published profiles | `?query,page,size` | `PageDto<SensitivityProfileDto>` |
| GET | `/api/v1/profiles/sensitivity/{id}` | — | Import one profile | — | `SensitivityProfileDto` |
| POST | `/api/v1/profiles/sensitivity` | ✅ | Publish a profile | `PublishProfileRequest` | `SensitivityProfileDto` |
| DELETE | `/api/v1/profiles/sensitivity/{id}` | ✅ | Unpublish own profile | — | — |
| GET | `/api/v1/waypoints` | — | Browse waypoint sets | `?page,size` | `PageDto<WaypointSetDto>` |
| GET | `/api/v1/waypoints/{id}` | — | Fetch one set | — | `WaypointSetDto` |
| POST | `/api/v1/waypoints` | ✅ | Publish a waypoint set | `WaypointSetDto` | `WaypointSetDto` |
| GET | `/api/v1/presets/notifications` | — | Browse presets | `?featureId,page,size` | `PageDto<NotificationPresetDto>` |
| POST | `/api/v1/presets/notifications` | ✅ | Publish a preset | `NotificationPresetDto` | `NotificationPresetDto` |
| DELETE | `/api/v1/presets/notifications/{id}` | ✅ | Unpublish own preset | — | — |

Authors and owners are always taken from the bearer token, never from a request
body.

### Error envelope

Every non-2xx response carries the same shape, so the mod needs exactly one parse
path:

```
ApiError {
  code      : String        // stable machine code, e.g. VERSION_UNSUPPORTED
  message   : String        // human-readable, safe to log, never user data
  path      : String
  timestamp : Instant
  details   : Map<String,String>   // optional field-level validation detail
}
```

| Status | `code` | Meaning |
|---|---|---|
| 400 | `BAD_REQUEST` | Validation failure; `details` populated |
| 401 | `UNAUTHENTICATED` | Missing/expired token — client re-handshakes once |
| 403 | `FORBIDDEN` | Authenticated but not the owner |
| 404 | `NOT_FOUND` | — |
| 426 | `VERSION_UNSUPPORTED` | API version outside the supported window. **Client stops retrying for the session** |
| 429 | `RATE_LIMITED` | — |
| 5xx | `INTERNAL` | Client treats this exactly like being offline |

---

## 8. Client-side degradation contract

**Offline-first is a hard requirement.** Every existing feature works completely
with the server unreachable, returning 500s, or not configured. The server is an
enhancement layer, never a dependency of correctness. The full table lives in
`client/net/package-info.java`; the essentials:

- `SkysensApiClient` returns `CompletableFuture<Optional<T>>` and **never** completes
  exceptionally. Empty is the only failure signal callers get, which is what makes
  the contract enforceable rather than aspirational.
- **Disabled in config (the default): no request is constructed at all.** A fresh
  install makes zero network requests.
- Timeout / 5xx / other 4xx → empty result, cache untouched, exponential backoff up
  to 30 min. An outage is indistinguishable from "offline" to a feature.
- 401 → drop the token, one re-handshake; a second 401 disables authenticated sync
  for the session.
- Version mismatch (426, or a manifest outside the supported window) → all sync
  permanently disabled for the session, one debug line. Never retried: the answer
  cannot change until the user updates.
- **Stale is not absent.** An expired cache entry is still served; expiry only means
  "worth refreshing in the background". Bundled resources (`splits.json`,
  `assets/all_locations.json`) are the floor, so there is no state in which a
  feature has no data.
- **Threading:** all network work runs on the existing 2-thread
  `SkysensClient.SCHEDULER`. No per-feature executors. Results are applied on the
  client thread via `Minecraft.getInstance().execute(...)`. Because the pool is
  small and shared, every request carries a hard timeout (5s connect, 10s total).

### Config toggles

Declared in `client/config/categories/server/ServerCategory.java` and registered in
`SkysensConfig` as a "Server" category. **Declarations only — nothing reads them
yet.** All default to off:

`serverEnabled`, `serverBaseUrl`, `shareDungeonRuns`, `shareSensitivityProfiles`,
`downloadRemoteAssets`, `honourRemoteFeatureFlags`.

---

## 9. Legal & privacy

Flagged as decisions for the maintainer; not a blocker for the structure.

### Hypixel's rules

**This section records the assumption the design was built on; it is not a legal
confirmation.** Hypixel's Terms of Service and API terms were *not* read during this
pass, so nothing below is verified.

The working assumption is that client-side mods which do not automate gameplay are
permitted, and that the public SkyBlock API is available to third-party tools.
Against that assumption the design is clean: nothing in §5 automates play — splits,
sensitivity profiles, waypoints and zone data are all records of, or settings for,
actions the user performs themselves.

**Someone must read the current Hypixel ToS and API terms against this document
before the server is exposed publicly.** Three things to confirm, as maintainer
decisions:

1. **The base assumption above** — that non-automating client mods and third-party
   use of the public API are permitted, and specifically that nothing restricts
   *aggregating and redistributing* per-player data between users. This is the
   question §8 actually asks, and it is genuinely open.
2. **Hypixel API key terms.** If the server ever calls the Hypixel API on users'
   behalf (it does not today), key-sharing and rate-limit terms apply.
3. **Bazaar data (§5.5).** Re-serving Hypixel's public bazaar endpoint is both
   redundant and the most likely thing to run into API terms. The recommendation is
   simply not to — and that recommendation stands on engineering grounds alone,
   independent of how the legal question resolves.

No domain here falls into the category that would clearly be disallowed — automating
gameplay, or transmitting other players' data without their involvement. That is a
statement about what the design does, not a finding that the terms permit it.

### What the server stores

| Data | Source | Retention |
|---|---|---|
| Mojang UUID | Verified via `hasJoined` | Until the user deletes their account |
| Last known display name | Client-supplied, presentation only | Overwritten on each login |
| Dungeon run history | Opt-in upload (`shareDungeonRuns`) | Rolling 24 months, then aggregated into PBs and the raw runs dropped |
| Published profiles / waypoints / presets | Explicit publish action | Until unpublished or account deleted |
| Handshake challenges | Server-generated | 2 minutes |
| Refresh tokens | Server-generated | 30 days, revoked on account deletion |

No chat logs, no positions, no inventory, no other players' data.

### Opt-in and deletion

- **Everything that leaves the user's machine is opt-in by default**, surfaced as a
  visible `SkysensConfig` toggle. `serverEnabled` defaults to off; each sharing
  toggle defaults to off independently, so enabling the server does not enable
  uploads.
- `DELETE /api/v1/auth/account` erases everything keyed to the caller's UUID —
  runs, PBs, published content, tokens — and is reachable from the mod. This is the
  GDPR Art. 17 path.
- **Open decisions:** whether a published profile survives its author's account
  deletion as an anonymized artefact (others may depend on it) or is deleted with
  everything else; and whether a privacy notice must be shown in-mod at first
  enable. The conservative default — delete everything, show the notice — is
  recommended.

---

## 10. Architecture decisions

**ADR-001 — Monorepo, three Gradle modules.** One repository, one Gradle build,
`skysens-common` consumed as a project dependency by both sides. This makes the DTO
contract impossible to drift: a breaking change to a DTO fails the build of both
consumers in the same commit. *Alternative not chosen:* separate repositories with
`skysens-common` published to a Maven repository. That buys independent release
cadence at the cost of version skew between a published DTO jar and the mod that
consumes it — exactly the failure this split is meant to prevent. Revisit only if
the server acquires a release cadence genuinely independent of the mod.

**ADR-002 — Loom is mod-only; root is conventions-only.** See §1. Applying Loom at
the root would attempt to provide and remap Minecraft for the Spring module.

**ADR-003 — `skysens-common` bundled via `shadowImpl(project(...))`, not
`from(sourceSets…)`.** See §1. Option (a) over option (b) for Gradle 9 /
configuration-cache compatibility and correct task dependencies.

**ADR-004 — Persistence: PostgreSQL + Flyway, with H2 only in the `dev` profile.**
Recommended and adopted rather than left open. The data is relational and will be
queried relationally — runs belong to players, splits belong to runs, personal bests
are a projection over run history, and profile browsing needs paging and sorting
over a shared table. Postgres also gives proper `uuid` and `jsonb` types, which
matter because `SensitivityConfiguration` is a nested document that is stored whole
and never queried into. Flyway rather than `ddl-auto`: the schema is a versioned
artefact that must migrate forward on a live database, and `ddl-auto` is
`validate`/`none` in every profile so Hibernate can never mutate it. H2 is confined
to the `dev` profile so the skeleton boots with nothing installed; it runs in
PostgreSQL compatibility mode and with Flyway disabled, and it is never the target
of a migration.

**ADR-005 — Identity via the Mojang session-server handshake; UUID is the canonical
key.** See §6. *Alternatives rejected:* (a) trusting `PlayerService.currentPlayerName`
— trivially spoofable, not an identity; (b) an out-of-band account system with
email/password — a second credential for users to manage, and it would not prove the
account link that every domain here is about; (c) users pasting a Hypixel API key —
that key is a Hypixel credential, not an identity proof, and asking users to hand it
to a third party is bad practice. The session-server handshake is the conventional
route for exactly this problem, proves account ownership, and never exposes the
user's Mojang access token to Skysens.

**ADR-006 — Spring Boot 4.1.1.** The toolchain is JDK 25, so the version was
verified rather than guessed: Spring Boot 4.1.1 requires at least Java 17 and is
compatible with versions up to and including Java 26, and requires Gradle 8.14+ or
9.x (this repo is on the Gradle 9.5.0 wrapper).
**Source:** <https://docs.spring.io/spring-boot/system-requirements.html>.
Spring Boot 3.5 would also run on JDK 25, but 4.x is where JDK 25 is first-class and
it is the current line. `io.spring.dependency-management` is applied for now, though
under Boot 4 the Gradle plugin imports the BOM itself and the separate plugin is
arguably redundant — kept for the explicit `mavenBom` control it gives, and flagged
as removable in a follow-up.

**ADR-007 — Version catalog recommended, not performed.** See §2. The existing
`project.findProperty(...)` pattern is kept.

**ADR-008 — `SensitivityConfiguration` moved to `skysens-common`.** See §4. Its
being simultaneously the live config object and the shareable payload is the reason,
not an accident; the server's persistence entity stays separate.

**ADR-009 — No new third-party shaded dependency in the mod.** The API layer uses
`java.net.http.HttpClient` (JDK built-in) and the Gson Minecraft already bundles. No
OkHttp, Jackson, Retrofit or Spring client. If something genuinely must be added
later it must be shaded *and relocated* under `de.vantrex.skysens.dependencies.*`,
exactly like MoulConfig.

**ADR-010 — `client/net/` sits beside `client/repository/`, not in front of it.**
The repository layer stays the source of truth features read from; `client/net/` is
only ever an asynchronous refresher of that cache. This is what makes the
offline-first requirement structural rather than a matter of discipline in each
feature.
