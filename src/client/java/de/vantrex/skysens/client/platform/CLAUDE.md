# platform/

Minecraft-facing glue only: `mixin/` (all mixins) and `hook/` (Fabric API callbacks). Delegate to features through
the hook interfaces in `core/feature/`; keep logic out of here. Full guide:
[docs/architecture/platform.md](../../../../../../../../docs/architecture/platform.md).

- Every new mixin must be listed in `src/main/resources/skysens.client.mixins.json` (`client` array), or it is
  never applied.
- `defaultRequire` is 1: an injection that doesn't match fails at startup.
- Prefix handler methods with `skysens$`. Loop over the feature list from `FeatureRegistry` and check `isActive()`.
- After changes, start `runClient` and check `run/logs/latest.log` for mixin errors.
