# SkySens

Fabric client mod for Hypixel SkyBlock (Minecraft 26.2, Java 25, Gradle Kotlin DSL).

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
- Modrinth dependencies are declared in the `modrinth { dependencies { } }` block in `build.gradle.kts`.