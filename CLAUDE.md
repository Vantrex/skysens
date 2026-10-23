# SkySens

Fabric client mod for Hypixel SkyBlock (Minecraft 26.2, Java 25, Gradle Kotlin DSL).

## Build and run

- There is no `gradlew.bat`; run `sh ./gradlew build` (Git Bash). `JAVA_HOME` must point to a JDK 25
  (e.g. a Corretto 25 under `~/.jdks/`); it isn't set globally.
- `sh ./gradlew runClient` starts a dev client using `run/` (config in `run/config/skysens/`, log in `run/logs/latest.log`).
- The release jar is `build/libs/skysens-<version>.jar` (not `-dev`). MoulConfig is shaded and relocated into it.
- There are no unit tests. Verify changes in the dev client, see [docs/testing.md](docs/testing.md).

## Architecture in one screen

Almost all code lives in `src/client/java/de/vantrex/skysens/client/`, organised **by feature**:

| Package | What lives there |
|---|---|
| `SkysensClient` | Entry point: loads repositories and config, wires Hypixel Mod API, hooks, `/skysens` commands |
| `core/` | Framework: `Feature` + hook interfaces, `FeatureRegistry`/`FeatureService`, the `SkysensEvent` bus |
| `platform/` | Minecraft-facing glue only: `mixin/` (all mixins) and `hook/` (Fabric API callbacks). Dispatches into features |
| `features/<name>/` | One package per feature, with its own `config/` subpackage for its config classes |
| `chatcommand/` | `!command` framework for party/co-op chat, commands in `impl/` (feature-owned commands live in their feature) |
| `skyblock/` | Shared SkyBlock domain: location/zones, player state, item prices, profit, domain events |
| `integration/` | Optional adapters to SkyHanni, Odin and the Hypixel price API, via reflection |
| `config/` | `SkysensConfig` root (MoulConfig) plus the categories that don't belong to one feature |
| `ui/` | The custom config screen built on top of MoulConfig's option tree |
| `persistence/` | `RepositoryRegistry`: JSON repositories in `config/skysens/repositories/` |
| `notification/`, `stats/`, `util/` | Notification display, server TPS/ping, small helpers |

Dependencies point inwards: `platform` → `core` ← `features` → `skyblock`/`integration`/`config`. Features never
reference mixins; mixins mostly loop over the feature lists in `FeatureRegistry` (a few call one feature directly).

## Read before working on…

| Topic | Doc |
|---|---|
| Startup order, layers, where new code goes | [docs/architecture/overview.md](docs/architecture/overview.md) |
| Adding or changing a feature, the hook interfaces, events | [docs/architecture/features.md](docs/architecture/features.md) |
| Mixins, Fabric hooks, access widener | [docs/architecture/platform.md](docs/architecture/platform.md) |
| Config options, the config screen, saved data | [docs/architecture/config-and-persistence.md](docs/architecture/config-and-persistence.md) |
| `!commands` in party/co-op chat | [docs/architecture/chat-commands.md](docs/architecture/chat-commands.md) |
| Location/zones, prices, profit, SkyHanni/Odin integration | [docs/architecture/skyblock-and-integrations.md](docs/architecture/skyblock-and-integrations.md) |

## Rules that are easy to break

- **Don't rename persisted names.** Repository class names are their file names (`ProfitRepository.json`), and
  config field names are JSON keys. Renaming either silently resets users' data. Moving a class to another package is fine.
- **Register new things explicitly.** Features in `FeatureService.register()`, chat commands in `ChatCommandService`,
  repositories in `RepositoryRegistry`, mixins in `src/main/resources/skysens.client.mixins.json`. Nothing is
  discovered by classpath scanning; `@SkySensFeature` is only a marker.
- **No compile-time dependency on SkyHanni/Odin.** Go through `integration/` with reflection and a fallback.
- `Optional` only as a return type, never as a method parameter.

## Branching workflow

- `development` is the integration branch for all feature work. `master` only receives releases.
- Start every feature or fix on its own branch created from the latest `origin/development`
  (e.g. `feature/<name>`, `fix/<name>`), never from `master`.
- Open pull requests for feature branches against `development`, not `master`.
- Only merge `development` into `master` once multiple features are done on it, and only when the user
  asks for a release. Don't open or merge a `development` → `master` PR on your own initiative.

## Releases

Every push to `master` runs `.github/workflows/release.yml`, which publishes a new version to Modrinth
and creates a tagged GitHub release. That's why `master` only gets merges from `development`.

- The version comes from the git tags: the latest `v*` tag's patch number is bumped (`v1.0.3` → `1.0.4`).
- For a minor/major release, set `mod_version` in `gradle.properties` (e.g. `1.1.0`) in the change
  that goes to `master`; a version higher than the latest tag is released as-is.
- Every push to `development` runs `.github/workflows/beta.yml`, which publishes a beta to Modrinth
  (version type `beta`) as `<next release version>-beta.<run number>`, e.g. `1.0.4-beta.12`. Betas create
  no git tag or GitHub release, so they don't affect release numbering.
- Modrinth dependencies are declared in the `modrinth { dependencies { } }` block in `build.gradle.kts`.
