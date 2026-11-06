# Config, config screen and saved data

Everything is stored under `<minecraft>/config/skysens/`:

| File | Owner | Content |
|---|---|---|
| `config.json` | `SkysensConfig` (MoulConfig `ManagedConfig`) | All options |
| `sensitivity.json` | `SensitivityService` | Per-location/zone mouse sensitivity |
| `repositories/<ClassName>.json` | `RepositoryRegistry` | `SimpleKeyValueRepository`, `ProfitRepository`, `CoopRepository`, `BazaarOrderRepository`, `DungeonRunRepository`, `SlayerRngRepository`, `TrophyFrogRepository`, `WitchIngredientRepository`, `ActivePetRepository`, `DungeonClassRepository`, `TransferCooldownRepository`, `MacroRepository` |
| `repositories/ActivityTracker.json` | `ActivityTracker` | Activity timeline (saved by the tracker itself) |

## Options (MoulConfig)

MoulConfig is the **data layer only**: annotations describe the option tree, and Gson persists every `@Expose`
field. The tree is:

```
SkysensConfig                       @Category fields, one per sidebar entry, in declaration order
└── <X>Category                     plain class; fields are options or @Accordion groups
    └── <Y>Accordion                plain class; nested accordions are allowed
        └── option fields           @Expose + @ConfigOption(name, desc) + one @ConfigEditor* annotation
```

Conventions:

- A feature's config classes live in `features/<name>/config/`. Categories that span several features stay in
  `config/categories/` (General, QOL, Misc, Dev); chat commands keep theirs in `chatcommand/config/`.
- Every persisted field needs `@Expose`. Initialise it with its default (`public boolean foo = true;`).
- Supported editors (what the custom screen can draw, see `ui/model/ConfigTree.kindOf`): `@ConfigEditorBoolean`,
  `@ConfigEditorSlider`, `@ConfigEditorDropdown`, `@ConfigEditorColour`, `@ConfigEditorText`, `@Accordion`, and the
  SkySens-specific `@ConfigEditorDynamicZoneDropdown` and `@ConfigEditorMultiSelect` (a `List<SomeEnum>` of checked
  constants, drawn by `MultiSelectWidget`, kept in the tree by `MultiSelectEditorStub`). Any other editor shows as "unsupported" in the screen.
- `@ConfigEditorDynamicZoneDropdown` is registered as a custom processor in `SkysensConfig.getConfig()` and backed by
  `ZoneDropdownEditorStub`, which only exists so MoulConfig keeps the option in the tree.
- `guiTheme` and `guiFont` on `SkysensConfig` are `@Expose` without `@Category`, so they're saved but not shown as
  options. The theme picker in the screen header edits them.
- Read options live: `SkysensConfig.getInstance().<category>.<accordion>.<field>`.

### Loading safety and migrations

- If `config.json` can't be parsed, MoulConfig would fall back to defaults and overwrite the file on the next save.
  `SkysensConfig` first copies the unreadable file to `config.json.broken-<timestamp>`.
- Moving an option to another category changes its JSON path, so existing values would be lost. Add a migration:
  one class per migration in `config/migration/` implementing `ConfigMigration`, e.g. `LegacySwingConfigMigration`
  (swing settings moved from QOL to Visuals) or `LegacyNotificationLevelsConfigMigration`.
  - `shouldMigrate(root)` checks the raw `config.json` (read before MoulConfig loads it) for the old path. It must
    turn false once the config has been saved, which happens naturally because the save drops keys that no longer
    exist on the config classes.
  - `migrate(root, config)` copies the old values into the loaded `SkysensConfig`. Don't save there.
  - Register it in the `MIGRATIONS` list in `ConfigMigrations`. `SkysensConfig.getConfig()` runs them in order and
    saves once if any applied; a failing migration is logged and skipped.
  - Never put migration logic into `SkysensConfig` itself.

## The config screen (`ui/`)

`/skysens` opens `SkysensConfigScreen`, a fully custom screen. MoulConfig's own GUI is not used.

- `ui/model/ConfigTree` builds `CategoryNode`/`OptionNode`s from MoulConfig's processor every time the screen
  opens, and assigns each option an `OptionKind`.
- `ui/WidgetFactory` maps `OptionKind` → widget in `ui/widget/`. A new editor type needs an `OptionKind`, a
  `kindOf` branch and a widget.
- `ui/render/`: themes (`ThemePreset`), fonts (`FontFamily`, TTFs in `assets/skysens/font/`), animation and drawing
  helpers. The search field filters all leaf options (`ConfigTree.allLeaves` + `OptionNode.matches`) and shows
  them with a breadcrumb.
- Widgets write straight into the config object, so features see changes at once. The file is written when the
  screen closes (`SkysensConfigScreen.removed()` → `saveToFile()`) and in the shutdown hook.

## Repositories (`persistence/`)

`RepositoryRegistry` keeps one static instance per repository class. It loads them at startup, saves them in a
shutdown hook, and maps each class to `repositories/<SimpleClassName>.json` with Gson.

To add one:

1. Create a plain class with a no-arg constructor (the class lives with its feature, e.g.
   `features/bazaar/BazaarOrderRepository`).
2. Add a static field plus a line in both `loadRepositories()` and `saveRepositories()`.

For small values that don't deserve their own class (buff expiry times, …), use
`RepositoryRegistry.KEY_VALUE_REPOSITORY.put/get`.

**Saving:** repositories are written in the shutdown hook, or earlier with `RepositoryRegistry.saveRepository(obj)` where
losing a crash's worth of changes would hurt (the macro editor does this on every commit).

### Tolerant loading

`loadRepository` must not take the client down for a hand-edited or damaged file. If the JSON can't be parsed
(`JsonParseException`/`IllegalStateException`), the file is copied to `repositories/<ClassName>.json.broken-<epoch millis>`,
a warning is logged and the repository starts empty (a missing file also gives an empty instance). A file that parses but has
the wrong shape for a nested value is the repository's own business: `MacroRepository` keeps raw `JsonObject`s and parses them
defensively. A real `IOException` (unreadable file) still fails loudly. Tell users to send the `.broken-*` file when a bug
report says "my data is gone".

### Repositories added for trackers and macros

| Repository | Content | Written by |
|---|---|---|
| `ActivePetRepository` | `activePets`: profile name -> last seen active pet (`type`, `name`, `rarity`, `level`, `heldItem`, `skin`, `uuid`, `candyUsed`) | `PetTracker`, restored silently on `ProfileChangeEvent` |
| `DungeonClassRepository` | `selectedClasses`: profile -> `DungeonClass` name; `classLevels`: `"<profile>:<CLASS>"` -> last seen level | `DungeonClassTracker` |
| `TransferCooldownRepository` | `learnedCommands`: command roots learned from the "You may only use this command after Ns on the server!" message | `TransferCooldownTracker` |
| `MacroRepository` | `version` (int) and `workflows` (list of raw workflow JSON objects) | `MacroService.commit` (macro editor) |

`MacroRepository` is versioned JSON: nodes, ports and params are persisted *by id/name* (`trigger.hotkey`, `logic.switch`,
condition ids like `dungeon.floor`, enum constants such as `SINGLE`/`DOUBLE_TAP`), and keys or whole nodes that this version
does not know are kept (`extra`, `UnknownNode`) and written back, so a downgrade or a newer shared macro loses nothing. The
class name, `version`, `workflows` and every id/enum name inside the workflows are part of the persisted format: **never
rename**. Format details, the clipboard envelope and how to evolve the version: [macros.md](macros.md#data-model-and-json).
Pet, class and learned-command repositories are best-effort caches: tracker state is rebuilt from the game after a rejoin
(see [skyblock-and-integrations.md](skyblock-and-integrations.md#known-vs-unknown)), the saved value is only what was
seen last.

## Macro, transfer cooldown and dev options

All persisted names below are JSON keys in `config.json`; don't rename them.

| Option | Path | Default | Notes |
|---|---|---|---|
| Enable Macros | `macroCategory.enabled` | on | Master toggle. Off: `MacroFeature.isActive()` is false, no key is touched |
| Open Editor | `macroCategory.openEditor` | n/a | Button, no `@Expose`, not saved. Same as `/skysens macros` |
| Several Matches Warning | `macroCategory.multipleMatchWarning` | `CHAT` | `MatchWarningDisplay`: `CHAT`, `TOAST`, `OFF` |
| Several Matches Warning Level | `macroCategory.multipleMatchWarningLevel` | `IMPORTANT` | Level of that toast |
| Show Blocked Hint | `macroCategory.showBlockedHint` | on | Red hint for cooldown, rate limit, unknown state, transfer wait |
| Execution Log Size | `macroCategory.executionLogSize` | 50 | Slider 10-200, in memory only |
| (hidden) | `macroCategory.macroTutorialSeen` | off | Set when the editor tour was shown; `@Expose` without `@ConfigOption` |
| Wait for Transfer Cooldown | `generalCategory.waitForTransferCooldown` | off | Hold transfer-limited macro commands until the cooldown is over |
| Transfer Cooldown Queue | `generalCategory.transferCooldownQueueMode` | `KEEP_NEWEST` | `TransferCooldownQueueMode`: `KEEP_NEWEST` or `QUEUE_ALL` |
| Macros Outside SkyBlock | `devCategory.macroIgnoreSkyBlock` | off | Dev client only (`isDevelopmentEnvironment()`); lets macros fire off SkyBlock for offline testing |

The Macros category is a feature-owned category (`features/macro/config/MacroCategory`, "Macros" in the sidebar). The wait
options live in General because the transfer cooldown is generic SkyBlock knowledge, not a macro detail. Macro timings (double
tap 250 ms, long hold 450 ms, rate limit 500 ms, warning throttle 10 s) are constants in `MacroConstants`, deliberately not options.

## Compatibility rule

Users' files have to keep loading across updates, so these names are a persisted format:

- repository class simple names (they are the file names),
- `@Expose` field names in config and repository classes (they are the JSON keys),
- enum constant names stored in them.

Renaming any of these resets that data for every user unless you add a migration. Changing a package is safe.
