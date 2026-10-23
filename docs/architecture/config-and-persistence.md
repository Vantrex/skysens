# Config, config screen and saved data

Everything is stored under `<minecraft>/config/skysens/`:

| File | Owner | Content |
|---|---|---|
| `config.json` | `SkysensConfig` (MoulConfig `ManagedConfig`) | All options |
| `sensitivity.json` | `SensitivityService` | Per-location/zone mouse sensitivity |
| `repositories/<ClassName>.json` | `RepositoryRegistry` | `SimpleKeyValueRepository`, `ProfitRepository`, `CoopRepository`, `BazaarOrderRepository`, `DungeonRunRepository`, `SlayerRngRepository` |
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
  `config/categories/` (General, QOL, Dev); chat commands keep theirs in `chatcommand/config/`.
- Every persisted field needs `@Expose`. Initialise it with its default (`public boolean foo = true;`).
- Supported editors (what the custom screen can draw, see `ui/model/ConfigTree.kindOf`): `@ConfigEditorBoolean`,
  `@ConfigEditorSlider`, `@ConfigEditorDropdown`, `@ConfigEditorColour`, `@ConfigEditorText`, `@Accordion`, and the
  SkySens-specific `@ConfigEditorDynamicZoneDropdown`. Any other editor shows as "unsupported" in the screen.
- `@ConfigEditorDynamicZoneDropdown` is registered as a custom processor in `SkysensConfig.getConfig()` and backed by
  `ZoneDropdownEditorStub`, which only exists so MoulConfig keeps the option in the tree.
- `guiTheme` and `guiFont` on `SkysensConfig` are `@Expose` without `@Category`, so they're saved but not shown as
  options. The theme picker in the screen header edits them.
- Read options live: `SkysensConfig.getInstance().<category>.<accordion>.<field>`.

### Loading safety and migrations

- If `config.json` can't be parsed, MoulConfig would fall back to defaults and overwrite the file on the next save.
  `SkysensConfig` first copies the unreadable file to `config.json.broken-<timestamp>`.
- Moving an option to another category changes its JSON path, so existing values would be lost. Add a one-off
  migration like `readLegacySwingSettings` / `migrateLegacySwingSettings` (swing settings moved from QOL to Visuals).

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

## Compatibility rule

Users' files have to keep loading across updates, so these names are a persisted format:

- repository class simple names (they are the file names),
- `@Expose` field names in config and repository classes (they are the JSON keys),
- enum constant names stored in them.

Renaming any of these resets that data for every user unless you add a migration. Changing a package is safe.
