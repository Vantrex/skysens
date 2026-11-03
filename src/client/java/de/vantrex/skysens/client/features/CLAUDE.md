# features/

One package per feature; its config classes go in `<feature>/config/`. Full guide:
[docs/architecture/features.md](../../../../../../../../docs/architecture/features.md).

Checklist for a new or changed feature:

- Implement the hook interfaces from `core/feature/` and annotate the class with `@SkySensFeature`.
- Register it in `core/feature/FeatureService.register()`. Nothing is auto-discovered.
- `GameMessageListeningFeature`s are called even when inactive: check `isActive()` and `isOnSkyBlock()` yourself.
- Don't reach into another feature's package. Shared SkyBlock logic belongs in `skyblock/`.
- Don't rename `@Expose` config fields or repository classes, because that resets users' saved data
  ([config-and-persistence.md](../../../../../../../../docs/architecture/config-and-persistence.md)).
