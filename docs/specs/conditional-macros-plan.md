# Conditional Macros: Implementation Plan

> **Status: implemented** on `feature/conditional-macros` (commits `1d0ad23..b9c2889`, milestones M1-M9). This is the design record; where it differs from the code, the code wins. See [../architecture/macros.md](../architecture/macros.md) for what exists.

Status: plan for `docs/specs/conditional-macros.md`, branch `feature/conditional-macros`. I explored the code read-only; nothing was written. The caller saves this as `docs/specs/conditional-macros-plan.md`.

Base path used below: `src/client/java/de/vantrex/skysens/client/` (abbreviated `client/`).

---

## 0. Findings from the codebase that shape the design

| Finding | Consequence |
|---|---|
| `KeyHandlers` uses a `KeyMapping` with `consumeClick()` in `END_CLIENT_TICK`. By then vanilla has already run `KeyMapping.set/click` inside `KeyboardHandler.keyPress`. | Passthrough ("consume only if an action ran") cannot use `KeyMapping`. It needs **HEAD-cancellable mixins on raw input**: `KeyboardHandler.keyPress(long window, int action, KeyEvent)` and `MouseHandler.onButton(long window, MouseButtonInfo, int action)`. Both signatures were checked in the 26.2 jar; the GLFW action is 1 = press, 0 = release, 2 = repeat. |
| `KeyEvent(key, scancode, modifiers)` and `MouseButtonInfo(button, modifiers)` both implement `InputWithModifiers` (`hasShiftDown/hasControlDown/hasAltDown`). | Modifier matching comes straight from the event. |
| In 26.2 the current screen is `Minecraft.getInstance().gui.screen()`, and screens are opened with `client.gui.setScreen(...)`. | Use these accessors everywhere (not `minecraft.screen`). |
| `keyPress` checks `EditBox.canConsumeInput()` on the focused element before keymappings run. | GUI gating must skip screens whose focused element is an `EditBox` that can consume input (AH/bazaar search, anvil, sign). |
| `SkysensClient.isOnSkyBlock()` is set only by the Hypixel Mod API packet. `/mocklocation` only calls `LocationService.handleLocationUpdate` and never sets `isOnSkyBlock`. | Without a dev bypass, no macro can fire in the offline dev client, so no milestone could be tested. A dev option is needed (see M1). |
| `AfterTickFeature` only runs while on SkyBlock. | Long-hold timing and the double-tap timeout use their own `ClientTickEvents.END_CLIENT_TICK` listener, which respects the dev bypass. |
| `RepositoryRegistry.loadRepository` catches only `IOException`. A `JsonSyntaxException` crashes client init. Plain `Gson`, no type adapters. Repositories are saved only in the shutdown hook (or `saveRepository(obj)`). | Keep the repository schema simple (node params as `JsonObject`). Make loading tolerant: back up the broken file and start empty. The editor calls `RepositoryRegistry.saveRepository(...)` on close. |
| `ChatCommandService.sendCommand` is private, delays sends and appends an anti-spam suffix. | Don't reuse it. Write a dedicated `MacroSender` that calls `ClientPacketListener.sendCommand/sendChat` directly. |
| `HypixelExtraAttributes.getExtraAttributes` reads `custom_data` (root or `ExtraAttributes`) and copies the tag on every call. | Read the held item once per press, cached in the press context. Offline test: `/give @s diamond_sword[custom_data={id:"HYPERION",modifier:"heroic",enchantments:{ultimate_wise:5}}]`. |
| `ui/` widgets (`DropdownWidget`, `TextWidget`, …) are built around `OptionNode`, which wraps a MoulConfig `ProcessedOption`. | They can't be reused directly for node parameters. Reuse the primitives: `Theme`, `RenderUtil` (`roundedRect`, `inputBox`, `knob`, `shadow`, `scissor`, `circle`), `UiText`, `UiScale`, `SearchField`, `OptionWidget.playClick()`. |
| `RenderUtil` has no line primitive. The access widener already opens `GuiGraphicsExtractor.guiRenderState` and `innerFill`. `GuiElementRenderState` (`buildVertices`, `pipeline`, `textureSetup`, `scissorArea`) exists. | Wires: start with short quads along the bezier. Later, a custom `GuiElementRenderState` emitting a quad strip. |
| `HudEditorScreen` already zooms and pans with `graphics.pose().pushMatrix/translate/scale`. | Same approach for the canvas. |

### Data gaps (each is a small extension task; none exists today)

| Needed for | Today | Planned extension |
|---|---|---|
| Mayor condition | `MayorService` exposes only `isSlayerXpBuffActive()` | Store mayor name, minister name and perk names from the election JSON it already fetches. Add `getMayorName()`, `getMinisterName()`, `hasPerk(String)`, all `@Nullable`/boolean. |
| SkyBlock time condition | `SkyblockCalendar` has `year()`, `currentYear()`, `isYearOfTheWitch()` | Add `SkyblockCalendar.now()` returning `record SkyblockTime(int year, int month /*1..12*/, int day, int hour, int minute)`, plus `seasonName(month)`. SkyBlock day = 20 real minutes. |
| Slayer type/tier/"done" | `SlayerTracker` has `isQuestActive()`, `isBossSpawned()`, `isBossCocooned()`, `getBoss()` (set only on spawn) | Parse the sidebar line after "Slayer Quest" (e.g. "Revenant Horror IV") into `getQuestBoss()` and `getQuestTier()`. Add an `isQuestDone()` state, true from "SLAYER QUEST COMPLETE!" until the next quest or SkyBlock quit. |
| "Current activity" | `ActivityTracker` only records finished activities; `ActivityType` is a `features/activity` internal | Derive it in the macro feature with its own persisted enum `MacroActivity { DUNGEON, KUUDRA, SLAYER, DIANA }`. DUNGEON = location `CATACOMBS`, KUUDRA = location `KUUDRA`, SLAYER = `SlayerTracker.isQuestActive()`. **DIANA has no live source** (see open questions). |
| Dungeon floor | `features/dungeon/DungeonRunStats.getCurrentFloor()` is another feature's internal; `DungeonRunDetector` sets it from the entry message | Move the "current floor" into `skyblock/dungeon/`, alongside the parallel class tracker. Until then, `DungeonFacts` reads `DungeonRunStats` behind one adapter method, marked TEMPORARY. |
| Dungeon class, pet | Built in parallel, not on this branch yet | One adapter class per tracker (§4.4). Assumed getters, adjusted on merge. The conditions come in a late milestone (M8). |

---

## 1. Package layout

Everything for the feature lives under `client/features/macro/`. Shared domain code stays in `skyblock/`. Mixins stay in `platform/`.

```
features/macro/
├── MacroFeature.java              RawInputFeature impl; registered in FeatureService; delegates to MacroEngine
├── MacroService.java              singleton: owns the MacroRepository view, TriggerIndex, rebuild on change
├── MacroRepository.java           PERSISTED CLASS NAME → repositories/MacroRepository.json
├── MacroCommands.java             /skysens macros [log|reload|debug] subtree, called from SkysensClient
├── config/
│   └── MacroCategory.java         @Category "Macros" in SkysensConfig (fields below)
├── model/
│   ├── MacroDocument.java         envelope {format, version, workflows|nodes}; also the clipboard format
│   ├── Workflow.java              id, name, active, nodes, connections, viewport
│   ├── MacroNode.java             id, type, name, disabled, x, y, w, h, params (JsonObject)
│   ├── Connection.java            fromNode, fromPort, toNode
│   ├── ViewportState.java         panX, panY, zoom (per workflow, so the editor reopens where you left it)
│   ├── MacroJson.java             Gson instance, read/write, version upgrade chain, defensive parsing
│   ├── MacroFormatUpgrader.java   upgrade(JsonObject root, int fromVersion) → current
│   └── Ids.java                   short random ids (8 chars base62), remap helper for paste
├── node/
│   ├── NodeType.java              interface (see §3)
│   ├── NodeCategory.java          TRIGGER, LOGIC, CONDITION, ACTION, NOTE (picker groups)
│   ├── NodeTypes.java             explicit registry: register(...) calls in one static block
│   ├── Port.java                  record Port(String id, String label, PortKind kind)
│   ├── param/
│   │   ├── ParamSpec.java         key, label, description, ParamKind, default, options supplier, visibleIf
│   │   ├── ParamKind.java         TEXT, TEMPLATE_TEXT, INT, BOOL, ENUM, ENUM_MULTI, KEYBIND, ZONE, CONFIG_TOGGLE, RULE_LIST
│   │   └── Params.java            typed, defensive getters on JsonObject (getString(key, def), getEnum(...), …)
│   ├── trigger/HotkeyTriggerNode.java
│   ├── logic/IfNode.java, SwitchNode.java
│   ├── action/SendCommandNode.java, SendChatNode.java, OpenUiNode.java, ToastNode.java,
│   │          PlaySoundNode.java, ToggleOptionNode.java
│   ├── note/StickyNoteNode.java
│   └── UnknownNode.java           placeholder for an unrecognised type: preserved, drawn with an error badge
├── condition/
│   ├── ConditionType.java         interface (see §3.3)
│   ├── ConditionTypes.java        explicit registry
│   ├── ConditionRule.java         {id, type, negate, params}, a view over the JsonObject
│   ├── RuleGroup.java             combinator AND/OR + rules (used by If; Switch has one group per output)
│   ├── impl/                      one class per condition (IslandCondition, ZoneCondition, ActivityCondition,
│   │                              DungeonFloorCondition, DungeonClassCondition, SlayerCondition, PartyCondition,
│   │                              HeldItemCondition, InventoryItemCondition, ItemPathCondition,
│   │                              ContainerCondition, SneakingCondition, MayorCondition, SkyblockTimeCondition,
│   │                              ActivePetCondition)
│   └── MacroActivity.java         persisted enum DUNGEON, KUUDRA, SLAYER, DIANA
├── facts/                         "condition providers": read-only views of trackers/services
│   ├── MacroContext.java          per-press snapshot, lazily memoised facts (one instance per evaluation)
│   ├── LocationFacts.java         LocationService
│   ├── PartyFacts.java            PartyTracker
│   ├── SlayerFacts.java           SlayerTracker
│   ├── ItemFacts.java             held item / inventory EA via HypixelExtraAttributes, glob matching
│   ├── ScreenFacts.java           open container title, sneaking
│   ├── MayorFacts.java, TimeFacts.java
│   ├── DungeonFacts.java          adapter → skyblock/dungeon tracker (+ temporary DungeonRunStats floor)
│   └── PetFacts.java              adapter → skyblock/pet tracker + PetInfo parser
├── engine/
│   ├── InputChord.java            record(InputDevice device, int code, int mods) + display name
│   ├── InputDevice.java           KEYBOARD, MOUSE
│   ├── PressMode.java             SINGLE, DOUBLE_TAP, LONG_HOLD (persisted)
│   ├── InputTracker.java          press/release state, double-tap windows, hold timers, consumed-key set
│   ├── TriggerIndex.java          Map<InputChord, List<TriggerRef>> rebuilt from active workflows
│   ├── TriggerRef.java            record(workflowIndex, Workflow, MacroNode trigger, PressMode, boolean inGuis)
│   ├── MacroGate.java             can a macro fire now? (SkyBlock/bypass, chat, editor, GUI rules)
│   ├── GraphWalker.java           trigger → … → action, returns a PathResult
│   ├── MacroEngine.java           resolve(press) → Resolution; runs the chosen action; logs
│   ├── Resolution.java            candidates (all matching paths), chosen, outcome
│   ├── ExecutionLog.java          ring buffer (size from config), lastPathByWorkflow for highlighting
│   ├── ExecutionRecord.java       time, chord, mode, gui title, candidates, chosen, outcome, message
│   └── MatchWarnings.java         throttled "several matched" chat/toast message
├── action/
│   ├── ActionNodeType.java        NodeType subinterface: execute(MacroNode, MacroContext) → ActionOutcome
│   ├── ActionOutcome.java         enum RAN, RATE_LIMITED, ON_COOLDOWN, FAILED, BLOCKED (+ message)
│   ├── MacroSender.java           sendCommand/sendChat with global rate limit, sanitising, length cap
│   ├── CooldownTracker.java       per action node id → next-allowed time (not persisted)
│   ├── CloseScreenStep.java       "close inventory first"
│   ├── SkysensScreenTarget.java   persisted enum CONFIG, HUD_EDITOR, WARP_MENU, MACRO_EDITOR
│   └── placeholder/
│       ├── PlaceholderRenderer.java   "{{ path | default "x" }}" → String
│       └── PlaceholderRegistry.java   path → Function<MacroContext, String>, list for the editor's autocomplete
├── analysis/
│   ├── GraphValidator.java        per-node Issues (errors/warnings/info), used by the editor and on load
│   ├── OverlapAnalyzer.java       the editor-time overlap checks (§7)
│   └── NodeIssue.java             record(severity, nodeId, message)
├── template/
│   └── MacroTemplates.java        loads assets/skysens/macro/templates/*.json (listed explicitly)
└── editor/
    ├── MacroEditorScreen.java     Screen; layout of list / canvas / details / executions; input routing
    ├── EditorState.java           working copy MacroDocument, selected workflow, selection, dirty flag
    ├── list/MacroListPanel.java   left sidebar: workflows, active toggles, drag reorder, rename, menu
    ├── canvas/
    │   ├── Viewport.java          pan/zoom, screen↔world transforms, discrete zoom steps, fit
    │   ├── CanvasView.java        hit testing, drag state machine, rubber band, wire dragging
    │   ├── NodeLayout.java        node rect + port positions per type (cached per frame)
    │   ├── GridRenderer.java      dotted grid
    │   ├── NodeRenderer.java      node body, icon glyph, title, summary line, badges, disabled look
    │   ├── WireRenderer.java      bezier → segments; colours for normal/selected/executed/matched
    │   ├── QuadStripRenderState.java  (phase 2) custom GuiElementRenderState for smooth wires
    │   ├── StickyNoteRenderer.java
    │   └── CanvasControls.java    bottom-left zoom in/out, fit, reset; top-right "+" button
    ├── picker/NodePicker.java     searchable grouped popup (reuses ui/SearchField)
    ├── details/
    │   ├── NodeDetailsPanel.java  right side panel, generated from ParamSpecs
    │   ├── field/ParamField.java  base (layout/render/mouse/key like OptionWidget)
    │   ├── field/TextParamField.java, IntParamField.java, BoolParamField.java, EnumParamField.java,
    │   │   EnumMultiParamField.java, KeybindParamField.java, ZoneParamField.java,
    │   │   ConfigToggleParamField.java, RuleListParamField.java (rule rows with a type dropdown + nested fields)
    ├── executions/ExecutionsPanel.java   "Executions" tab: list of ExecutionRecords, click → highlight path
    ├── history/
    │   ├── EditorCommand.java     interface do/undo/label/mergeWith
    │   ├── CommandHistory.java    undo/redo stacks (limit 200), merging for drags/typing
    │   └── cmd/AddNodesCommand, RemoveNodesCommand, MoveNodesCommand, ResizeNoteCommand,
    │           ConnectCommand, DisconnectCommand, EditParamsCommand, RenameNodeCommand,
    │           ToggleDisabledCommand, CompositeCommand
    └── clipboard/MacroClipboard.java  copy selection / workflow → JSON; paste with id remap + offset
```

Touched outside the package:

- `core/feature/RawInputFeature.java` (new), `core/feature/FeatureRegistry.java` (list, `instanceof`, sort), `core/feature/FeatureService.java` (`register(new MacroFeature())`).
- `platform/mixin/KeyboardHandlerMixin.java` (new), `platform/mixin/MouseMixin.java` (add an `onButton` injection), `src/main/resources/skysens.client.mixins.json` (add `KeyboardHandlerMixin`).
- `persistence/RepositoryRegistry.java` (`MACRO_REPOSITORY` field, load/save lines, tolerant parse).
- `config/SkysensConfig.java` (`@Category public MacroCategory macroCategory`), `config/categories/dev/DevCategory.java` (`macroIgnoreSkyBlock`).
- `SkysensClient.java` (register the `/skysens macros` subtree; call `MacroService.getInstance().init()` after `loadRepositories()`).
- `skyblock/mayor/MayorService.java`, `skyblock/time/SkyblockCalendar.java`, `skyblock/slayer/SlayerTracker.java` (the extensions in §0).
- `features/dungeon/DungeonRunStats` → floor state moves to `skyblock/dungeon/` (M8, coordinated with the dungeon class tracker).
- Docs: `docs/architecture/features.md` (hook table), `platform.md` (mixin table), `config-and-persistence.md` (repository list), `skyblock-and-integrations.md` (new extensions).

Rules check:

- The feature is registered explicitly in `FeatureService.register()`. Node types and conditions are registered explicitly in `NodeTypes`/`ConditionTypes` static blocks, and templates come from an explicit file list. Nothing uses classpath scanning.
- `Optional` appears only as a return type (e.g. `ItemFacts.heldExtraAttributes()`). Parameters use `@Nullable`.
- No SkyHanni/Odin dependency. The facts read SkySens' own trackers only.
- `features/macro` doesn't read other features' internals, with two documented temporary exceptions: `DungeonRunStats` floor until M8, and `WarpToMenuScreen.open()`, already a sanctioned cross-reference (see Risks).

---

## 2. Data model and JSON format

### 2.1 Repository (persisted)

```java
/** Persisted: the class name is the file name (repositories/MacroRepository.json). Never rename. */
public class MacroRepository {
    @Expose public int version = MacroJson.CURRENT_VERSION;   // 1
    @Expose public List<JsonObject> workflows = new ArrayList<>();   // raw, see below
}
```

The workflows are kept as `JsonObject`s on disk, and `MacroService` parses them into `Workflow` objects through `MacroJson`. Reasons:

- The plain registry `Gson` never fails on node shapes it doesn't know.
- An unknown node type (from a newer version or a typo) stays exactly as it is through a save.
- One parser (`MacroJson`) serves the repository, the clipboard and the templates.

If the project's Gson is configured with `excludeFieldsWithoutExposeAnnotation`, the `@Expose` annotations matter. The current registry Gson isn't, but annotate anyway for consistency.

On save, `MacroService.commit(MacroDocument)` serialises back into `workflows` and calls `RepositoryRegistry.saveRepository(RepositoryRegistry.MACRO_REPOSITORY)`.

**Tolerant loading** (change to `RepositoryRegistry.loadRepository`, which benefits all repositories): catch `JsonParseException`/`IllegalStateException`, copy the file to `<Name>.json.broken-<timestamp>` (the same convention as `config.json`), log a warning and return a new instance. This keeps a malformed import or hand edit from crashing client init.

### 2.2 Document / clipboard envelope (version 1)

```json
{
  "format": "skysens-macro",
  "version": 1,
  "kind": "workflows",              // or "nodes" for a canvas selection
  "workflows": [ {
    "id": "wf_k3J9aQ2x",
    "name": "Context warp",
    "active": true,
    "viewport": { "x": 0, "y": 0, "zoom": 1.0 },
    "nodes": [
      { "id": "n_trg01", "type": "trigger.hotkey", "name": "Hotkey", "disabled": false,
        "x": 40, "y": 80,
        "params": { "input": { "device": "KEYBOARD", "code": 71, "mods": ["CTRL"] },
                    "mode": "SINGLE", "inGuis": false } },
      { "id": "n_sw01", "type": "logic.switch", "x": 240, "y": 80,
        "params": { "rules": [
            { "id": "r_a", "combinator": "AND", "conditions": [
                { "id": "c_1", "type": "island", "negate": false, "params": { "islands": ["HUB"] } } ] },
            { "id": "r_b", "combinator": "AND", "conditions": [ … ] } ],
          "fallback": true } },
      { "id": "n_act1", "type": "action.command", "x": 480, "y": 40,
        "params": { "command": "/warp home", "closeScreenFirst": false, "cooldownMs": 0 } },
      { "id": "n_note", "type": "note.sticky", "x": 20, "y": -60, "w": 200, "h": 80,
        "params": { "text": "G warps depending on island", "color": "YELLOW" } }
    ],
    "connections": [
      { "from": "n_trg01", "port": "main",       "to": "n_sw01" },
      { "from": "n_sw01",  "port": "rule:r_a",   "to": "n_act1" },
      { "from": "n_sw01",  "port": "fallback",   "to": "n_act2" }
    ]
  } ]
}
```

- **`kind: "nodes"`** carries `nodes` and `connections` at the top level (only connections between copied nodes). This is what Ctrl+C on a selection produces.
- **Ports are stable ids, not indexes.** If outputs are `true`/`false`, Switch outputs are `rule:<ruleId>` plus `fallback`, and the trigger output and all inputs are `main`. Reordering Switch rules never rewires anything.
- **Fan-out:** each output port holds at most one wire, and connecting a new wire replaces the old one. Inputs accept any number of wires. One action per press makes fan-out meaningless, and banning it keeps first-match semantics obvious.
- **Graph must be a DAG.** `ConnectCommand` rejects a wire that would create a cycle. `GraphWalker` also stops at a depth of 64.
- **Ids** are generated by `Ids.next(prefix)`. On paste or import, every node, rule and condition id is remapped (`Ids.remap(MacroDocument)`), and workflow ids too when importing into an existing list.
- **Versioning:** `MacroJson.read(String)` checks `format`, then `MacroFormatUpgrader.upgrade(root, version)` runs `v1→v2→…` steps, the same idea as `ConfigMigration` but local to the JSON. A version higher than current is rejected with "made with a newer SkySens".
- **Persisted names (never rename):** `MacroRepository`; every JSON key above; node `type` ids; condition `type` ids; param keys; enum constant names stored in params (`SkyblockLocationEnum`, every `*ZoneEnum`, `MacroActivity`, `PressMode`, `InputDevice`, modifier names `CTRL/SHIFT/ALT`, `SkysensScreenTarget`, chat channel `ALL/PARTY/COOP`, sticky colours, the dungeon class and pet rarity enums from the trackers). Input codes are stored as GLFW codes plus `InputConstants` key names for readability (`"name": "key.keyboard.g"`); the code wins on load.

### 2.3 Node type ids (v1)

| Category | id | Ports |
|---|---|---|
| Trigger | `trigger.hotkey` | out `main` |
| Logic | `logic.if` | in `main`; out `true`, `false` |
| Logic | `logic.switch` | in `main`; out `rule:<id>`…, `fallback` (if enabled) |
| Action | `action.command`, `action.chat`, `action.open_ui`, `action.toast`, `action.sound`, `action.toggle_option` | in `main` |
| Note | `note.sticky` | none |

Condition rule types (inside If/Switch): `island`, `zone`, `activity`, `dungeon.floor`, `dungeon.class`, `slayer`, `party`, `item.held`, `item.inventory`, `item.path`, `screen.container`, `player.sneaking`, `mayor`, `time.skyblock`, `pet.active`.

---

## 3. Node and condition registries (extension points)

### 3.1 NodeType

```java
public interface NodeType {
    String id();                          // persisted
    String displayName();
    NodeCategory category();
    String icon();                        // single glyph/short text drawn in the node
    List<ParamSpec> params();             // drives NodeDetailsPanel and default params
    List<Port> inputs(MacroNode node);
    List<Port> outputs(MacroNode node);   // dynamic for Switch
    String summary(MacroNode node);       // one-line subtitle on the canvas ("Hub, Village")
    void validate(MacroNode node, List<NodeIssue> out);
    default JsonObject defaultParams() { return ParamSpec.defaults(params()); }
    default int width(MacroNode n) { return 120; }
}
```

- `LogicNodeType extends NodeType` adds `@Nullable String route(MacroNode node, MacroContext ctx, RouteTrace trace)`. It returns the output port id to follow (or null if the path ends) and records each rule's result in `trace` for the log ("rule r_a matched, r_b matched → took r_a").
- `ActionNodeType extends NodeType` adds `ActionOutcome execute(MacroNode node, MacroContext ctx)` and `boolean sendsToServer()`, which decides whether the global rate limit applies.
- `TriggerNodeType` adds `@Nullable TriggerSpec spec(MacroNode node)` (chord, mode, inGuis), consumed by `TriggerIndex`.

**Adding a node = one class + one line in `NodeTypes`.** The side panel, picker entry, default params and validation all come from the class.

### 3.2 ParamSpec

```java
public record ParamSpec(String key, String label, String description, ParamKind kind,
                        JsonElement defaultValue, @Nullable Supplier<List<Choice>> choices,
                        @Nullable Predicate<JsonObject> visibleIf) {}
```

`ParamKind.ZONE` gets its choices from the island param in the same object (via `SkyblockLocationEnum`'s zone enum class). `CONFIG_TOGGLE` lists boolean leaves from `ui/model/ConfigTree.build()` + `allLeaves(...)` filtered by `OptionKind` BOOLEAN, keyed by field path. `RULE_LIST` is the If/Switch rule editor.

### 3.3 ConditionType ("conditions backed by providers")

```java
public interface ConditionType {
    String id();                               // persisted
    String displayName();
    String group();                            // "Location", "Items", "Party", "Dungeon", … (picker/dropdown grouping)
    List<ParamSpec> params();
    boolean test(JsonObject params, MacroContext ctx);
    String describe(JsonObject params);        // "island is Hub or Dwarven Mines"
    default void validate(JsonObject params, List<String> problems) {}
    default boolean isAvailable() { return true; }   // e.g. pet tracker missing → greyed out, badge
    /** Canonical form for overlap detection: two rules with equal keys are "identical". */
    default String canonicalKey(JsonObject params) { return id() + ":" + Params.canonical(params); }
}
```

A `ConditionRule` adds `negate`. A `RuleGroup` (If's single group, or one per Switch output) has `combinator AND|OR`. An empty group is **always true**, which overlap detection flags.

Conditions read state only through `MacroContext`, which exposes the facts lazily:

```java
public final class MacroContext {
    public static MacroContext capture(InputChord chord, PressMode mode, @Nullable Screen screen);
    public LocationFacts location();  public PartyFacts party();  public SlayerFacts slayer();
    public ItemFacts items();         public ScreenFacts screen(); public MayorFacts mayor();
    public TimeFacts time();          public DungeonFacts dungeon(); public PetFacts pet();
}
```

Each facts object is created on first use and cached for this press. For example, `ItemFacts.held()` reads the main-hand stack and its `CompoundTag` once. All of this runs on the client thread inside the input callback.

### 3.4 Condition catalogue (v1)

| id | Params | Backed by |
|---|---|---|
| `island` | `islands: [SkyblockLocationEnum]` (any of) | `LocationService.getCurrentLocation()` |
| `zone` | `island`, `zones: [ZoneEnum names]` | `LocationService.getCurrentZone()` |
| `activity` | `activity: MacroActivity` | Derived (see §0). DIANA is "unavailable" until a source exists |
| `dungeon.floor` | `floors: ["F1".."F7","M1".."M7","E"]` | `DungeonFacts.floor()` and location `CATACOMBS` |
| `dungeon.class` | `who: SELF\|ANY_MEMBER\|NAMED`, `name`, `classes: [...]`, `minLevel` | `DungeonFacts` → dungeon class tracker (M8) |
| `slayer` | `state: ANY_ACTIVE\|QUEST_ACTIVE\|BOSS_SPAWNED\|DONE\|NONE`, `boss` (optional), `minTier` | `SlayerTracker` (+ extension) |
| `party` | `check: IN_PARTY\|IS_LEADER\|SIZE\|MEMBER_PRESENT`, `op: GE\|LE\|EQ`, `n`, `name` | `PartyTracker.isInParty/isLeader/getSize/hasMember` |
| `item.held` | `id` (glob, e.g. `*_SWORD`), `reforge`, `enchant` + `minLevel`, `petType`, `petRarity` | `HypixelExtraAttributes` (`id`, `modifier`, `enchantments`, `petInfo` → shared PetInfo parser in M8) |
| `item.inventory` | `id` glob, `minCount` | Player inventory (main + hotbar + offhand) EA ids |
| `item.path` | `slot: HELD`, `path` (dotted, e.g. `enchantments.ultimate_wise`), `op: EXISTS\|EQUALS\|CONTAINS`, `value` | `HypixelExtraAttributes.findCompoundByKey` and path walking |
| `screen.container` | `op: EQUALS\|CONTAINS`, `text`, `ignoreCase` | `AbstractContainerScreen.getTitle()` stripped via `ChatUtil.strip` |
| `player.sneaking` | none (use negate for "not") | `LocalPlayer.isShiftKeyDown()` |
| `mayor` | `mayor` name or `perk` name, `includeMinister` | `MayorService` (+ extension) |
| `time.skyblock` | `months` (multi), `fromHour`, `toHour` | `SkyblockCalendar.now()` (+ extension) |
| `pet.active` | `types`, `rarities`, `minLevel`, `heldItem` | `PetFacts` → pet tracker (M8) |

Glob matching: `*` → `.*`, other chars quoted, case-insensitive, compiled patterns cached in `ItemFacts` (LRU 64). The held item uses the **SkyBlock id from ExtraAttributes only**. A stack without EA has id `null`, and every `item.held` id condition is false for it.

### 3.5 Tracker adapters (assumed APIs, adjust on merge)

```java
final class DungeonFacts {
    @Nullable String floor();                          // M1–M7: temporary read of DungeonRunStats.getCurrentFloor()
    @Nullable String ownClass();                       // DungeonClassTracker.getInstance().getSelectedClass() (assumed)
    int ownClassLevel();                               // …getSelectedClassLevel() (assumed)
    Map<String, ClassEntry> partyClasses();            // …getPartyClasses() (assumed)
    boolean available();                               // tracker class present on the branch
}
final class PetFacts {
    Optional<PetView> active();                        // PetTracker.getInstance().getActivePet() (assumed)
    Optional<PetView> fromItem(CompoundTag ea);        // shared PetInfo parser for ExtraAttributes "petInfo"
    record PetView(String type, String rarity, int level, @Nullable String heldItem) {}
}
```

Store class and rarity as **strings** in params, not the trackers' enum types, so a tracker enum rename doesn't break saved macros. Map them through `name()` at the adapter boundary. Both adapters are the only files that import `skyblock/dungeon` or `skyblock/pet`.

---

## 4. Runtime engine

### 4.1 Input capture (mixins and hook)

New hook interface `core/feature/RawInputFeature extends Feature`:

```java
public interface RawInputFeature extends Feature {
    /** action: 1 press, 0 release, 2 repeat. Cancel ci to swallow the event before screens and key mappings. */
    void keyPress$Head(KeyEvent event, int action, CallbackInfo ci);
    void mouseButton$Head(MouseButtonInfo button, int action, CallbackInfo ci);
}
```

- **`platform/mixin/KeyboardHandlerMixin`** (new): `@Inject(method = "keyPress", at = @At("HEAD"), cancellable = true) private void skysens$keyPress$Head(long window, int action, KeyEvent event, CallbackInfo ci)`. It returns if `window != minecraft.getWindow().handle()` (vanilla does the same check), then loops `FeatureRegistry.getRawInputFeature()` with `isActive()`.
- **`platform/mixin/MouseMixin`**: add `@Inject(method = "onButton", at = @At("HEAD"), cancellable = true) private void skysens$onButton$Head(long window, MouseButtonInfo info, int action, CallbackInfo ci)`, same loop.
- `FeatureRegistry`: field `rawInputFeature`, `instanceof` branch, sort entry. Add `KeyboardHandlerMixin` to `skysens.client.mixins.json`.
- `MacroFeature implements RawInputFeature`. `isActive()` returns `macroCategory.enabled`. It delegates to `MacroEngine.onInput(InputChord, action, ci)`.

**Why HEAD of `keyPress`/`onButton`:** this is the only point where cancelling stops all three of `Screen.keyPressed`, `KeyMapping.click/set` and other mods' Fabric screen events, and where not cancelling leaves vanilla untouched. That is exactly the passthrough requirement.

**Repeats and releases:**

- When a press was consumed, the engine puts the key into `InputTracker.consumedHeld`. Repeats (action 2) of that key are cancelled too, otherwise vanilla would see a "repeat" press. Releases are never cancelled (`KeyMapping.set(key,false)` is harmless) and clear `consumedHeld`.
- Releases are always observed for hold timing.

**Modifiers:**

- Matching is **exact on {Ctrl, Shift, Alt}**: Ctrl+G does not fire a plain-G trigger, and vice versa. Caps/Num Lock and Super bits are ignored.
- When the trigger key itself is a modifier (e.g. Left Alt), its own bit is stripped before matching.
- On macOS, `hasControlDown` follows vanilla's semantics. Use `hasControlDownWithQuirk()` only for editor shortcuts, as vanilla does.

**Mouse:** all GLFW buttons 0–7 (side buttons are 3/4) can be bound. Binding LMB/RMB triggers a warning badge ("blocks attacking/using when an action runs"). LMB/RMB with "also fire in GUIs" is rejected in validation, because it would break inventory clicking.

### 4.2 Gating (`MacroGate`)

A press is evaluated only if all of these hold:

1. `macroCategory.enabled`.
2. `SkysensClient.getInstance().isOnSkyBlock()` **or** (`FabricLoader.isDevelopmentEnvironment()` and `devCategory.macroIgnoreSkyBlock`).
3. `minecraft.player != null`.
4. No screen open: `gui.screen() == null`. Otherwise:
   - the screen is an `AbstractContainerScreen` (this includes the player inventory),
   - and it is not a `ChatScreen`, `MacroEditorScreen`, `SkysensConfigScreen` or `HudEditorScreen`, and not the `KeybindParamField` capture state,
   - and its focused element is not an `EditBox` whose `canConsumeInput()` is true,
   - and, when the trigger is looked up, only triggers with `inGuis = true` are considered.

A gated press is never cancelled. It is logged only in debug mode, to keep the log useful.

### 4.3 Press modes and the passthrough policy (main design decision)

Triggers are indexed per `InputChord`. All triggers for one chord, across all active workflows, form a **binding group**. The group's mix of modes decides when resolution happens:

| Group contains | On press | Later | Can consume? |
|---|---|---|---|
| Only SINGLE | Resolve SINGLE triggers **synchronously** | – | Yes: cancel if an action ran |
| SINGLE + DOUBLE_TAP | Press 1: arm a double-tap window (default **250 ms**), **pass through**. Press 2 within the window: resolve DOUBLE_TAP synchronously | Window expires without a second press → resolve SINGLE (deferred) | Press 2: yes. Deferred single: **no** (the press already reached vanilla) |
| DOUBLE_TAP only | Press 1: arm window, pass through. Press 2: resolve | – | Press 2: yes |
| LONG_HOLD (± SINGLE) | Press: start hold timer, **pass through** | Held ≥ threshold (default **450 ms**, tick-checked) → resolve LONG_HOLD; released earlier → resolve SINGLE on release (deferred) | No |
| All three | Combination of the above: press 2 within the window → DOUBLE_TAP; hold → LONG_HOLD; otherwise SINGLE after window/release | | Only the double-tap second press |

Effects:

- A key bound only with SINGLE triggers behaves exactly as the spec says.
- Adding double tap or long hold to a key makes its single-press triggers **deferred and non-consuming**. The editor shows an info badge on those triggers ("Fires after 250 ms; the key also reaches Minecraft").
- This keeps "one action per press": each physical press resolves at most once, in at most one mode.

Deferred resolutions run from `InputTracker.tick()`, registered on `ClientTickEvents.END_CLIENT_TICK` from `MacroService.init()` (not `AfterTickFeature`, which is SkyBlock-only and ignores the dev bypass). Hold detection uses `System.nanoTime()` from the press, checked each tick (50 ms granularity). Gating is re-checked at resolution time: if a screen was opened in between, the GUI rules apply then.

Thresholds are config sliders: `doubleTapWindowMs` 150–500 (default 250) and `longHoldMs` 250–1500 (default 450).

### 4.4 Evaluation (`MacroEngine` + `GraphWalker`)

```java
Resolution resolve(InputChord chord, PressMode mode, MacroContext ctx);
```

1. `TriggerIndex.lookup(chord, mode, guiOpen)` returns `TriggerRef`s in **evaluation order**: workflow order in the macro list, then within a workflow the trigger's order (by `y`, then `x`, as on the canvas). The editor shows a small "#n" on triggers sharing a chord so the order is visible.
2. For **every** ref (not only the first), `GraphWalker.walk(ref, ctx)` follows `main` → logic nodes route → … until:
   - an **action node** (candidate),
   - a missing wire (`END_NO_ACTION`),
   - a **disabled node** (`END_DISABLED`; disabled nodes block the path, and a disabled trigger is never indexed),
   - an `UnknownNode`/invalid node (`END_INVALID`),
   - or the depth cap.

   Each walk records the node path and per-rule results in a `PathResult`. Walking everything is cheap (a handful of nodes, memoised facts), and the "all branches that matched" log and warning need it.
3. Within a Switch, the **first matching rule wins**. All matching rule ids are recorded so "several rules matched" can be shown.
4. The **chosen** candidate is the first `PathResult` that ends in an action. The others ending in an action are "also matched".
5. Execute the chosen action (§4.6). Outcome → `ExecutionRecord`. If the outcome is `RAN`, the press is consumed (when consumable per §4.3).
6. If more than one candidate ended in an action, or the chosen Switch had several matching rules, `MatchWarnings.maybeWarn(record)`. It is throttled per chord (default 10 s) and configurable as `CHAT`, `TOAST` or `OFF`. Message format: `[Macros] G: ran "Context warp → /warp home"; also matched "Menus key" (#2)`.

**Default for blocked first match** (rate limit/cooldown): the press is **dropped, not handed to the next candidate, and passed through** to vanilla (the literal spec reading: "consume only if an action ran"). A short hint is shown on the secondary action bar, `ClientUtil.sendSecondaryActionBar(..., 1500)`. See open question Q1.

### 4.5 Rate limit and cooldown

- `MacroSender` keeps `nextAllowedAt` for server-bound sends (command/chat). The global minimum gap is `macroCategory.globalRateLimitMs`, default **500 ms**, slider 250–2000. A press that comes too early returns `RATE_LIMITED`.
- Client-side actions (toast, sound, open UI, toggle option) are not rate limited, but per-action cooldowns still apply.
- `CooldownTracker`: `Map<String /*workflowId:nodeId*/, Long>`, set after `RAN`. It is not persisted and is cleared on `SkyblockQuitEvent`. The param is `cooldownMs` (0 = none) on every action node, a shared `ParamSpec` from `ActionParams.common()`.

### 4.6 Actions

All action types get these common params: `closeScreenFirst` (bool, default false) and `cooldownMs` (int).

| Type | Params | Execution |
|---|---|---|
| `action.command` | `command` (template text) | Render placeholders, sanitise, strip one leading `/`, `connection.sendCommand(cmd)` |
| `action.chat` | `channel ALL\|PARTY\|COOP`, `message` | `ALL` → `connection.sendChat(msg)`; `PARTY` → `sendCommand("pc " + msg)`; `COOP` → `sendCommand("cc " + msg)` |
| `action.open_ui` | `target: SkysensScreenTarget` | CONFIG → `gui.setScreen(new SkysensConfigScreen(null))`; HUD_EDITOR → `HudEditorScreen.open()`; WARP_MENU → `WarpToMenuScreen.open()`; MACRO_EDITOR → `MacroEditorScreen.open()` |
| `action.toast` | `title`, `subtitle` (templates), `display: TITLE\|CHAT\|ACTION_BAR\|DEFAULT` | `NotificationService.sendNotification(Notification.builder()...level(IMPORTANT))`; IMPORTANT so it isn't held back while AFK or in combat |
| `action.sound` | `sound` (dropdown of a curated list of `SoundEvents`, stored by registry id string), `volume`, `pitch` | `ClientUtil.playSound(...)` |
| `action.toggle_option` | `option` (ConfigTree field path, e.g. `qolCategory.junkHighlight.enabled`) | Reflectively flip the boolean via the `OptionNode` from `ConfigTree`; show a toast with the new state; the config is saved on the next config save |

**Close inventory first** (`CloseScreenStep.run()`): if `gui.screen()` is an `AbstractContainerScreen`, call `player.closeContainer()`, which sends the close packet and clears the screen; if another screen is open, call `gui.setScreen(null)`. Then the action runs in the same call. This is the only allowed two-step chain.

**Sending and sanitising (`MacroSender`):**

- Placeholder **values** are sanitised before insertion: strip `§` codes and newlines, collapse whitespace.
- The final text is capped at 256 characters (Hypixel's limit). Over-long text gives `FAILED("too long")`, not truncation, so commands never run with partial arguments.
- Chat messages never start with `/`.
- Sends are skipped when `connection == null`.

Messages SkySens sends should be flagged like `ClientUtil.isSendingOwnMessage()` so SkySens' own chat listeners ignore them. Check how that flag is set and reuse it.

### 4.7 Placeholders

Syntax: `{{ path }}` and `{{ path | default "text" }}`. No arbitrary expressions. `PlaceholderRenderer.render(String template, MacroContext ctx, List<String> unresolved)` keeps a list of unresolved paths for the execution log and validation.

v1 paths: `player`, `island`, `zone`, `party.leader`, `party.size`, `party.members` (comma-joined), `heldItem.id`, `heldItem.name`, `heldItem.reforge`, `slayer.boss`, `slayer.tier`, `dungeon.floor`, `container.title`, `mayor`, `time` (SkyBlock "Spring 3rd, 14:00"). Added in M8: `dungeon.class`, `pet.type`, `pet.rarity`, `pet.level`.

`PlaceholderRegistry` also feeds autocomplete in `TextParamField` (typing `{{` shows the list). Validation marks unknown paths.

### 4.8 Executions log

- `ExecutionLog`: ring buffer, `macroCategory.executionLogSize` (default 50). Not persisted.
- `ExecutionRecord` holds: time, chord + mode, open container title, list of `PathResult` (workflow name, trigger id, node path, per-rule results, end reason), chosen index, `ActionOutcome` + message (e.g. "rate limited, 220 ms left", "no path reached an action", "gated: chat open" in debug only).
- `lastPathByWorkflow: Map<String, ExecutionRecord>` drives editor highlighting.
- `/skysens macros log` prints the last 10 records to chat. This is useful before the editor exists (M2/M3).

---

## 5. Config (`features/macro/config/MacroCategory.java`)

Add `@Category(name = "Macros", desc = "...") public MacroCategory macroCategory = new MacroCategory();` to `SkysensConfig`. Field names are persisted.

| Field | Editor | Default |
|---|---|---|
| `enabled` | Boolean | `true` (no workflows exist by default, so nothing fires) |
| `openEditor` | `@ConfigEditorButton(buttonText = "Open Editor")` Runnable → `MacroEditorScreen.open(parent)` (same pattern as "Edit HUD Positions" in `GeneralCategory`) | – |
| `globalRateLimitMs` | Slider 250–2000 step 50 | 500 |
| `doubleTapWindowMs` | Slider 150–500 | 250 |
| `longHoldMs` | Slider 250–1500 | 450 |
| `multipleMatchWarning` | Dropdown `CHAT`, `TOAST`, `OFF` (enum `MatchWarningDisplay`) | CHAT |
| `multipleMatchWarningLevel` | `NotificationLevelEnum` (house rule for notifications, `LEVELS_DESC` suffix) | IMPORTANT |
| `showBlockedHint` | Boolean (action-bar hint when rate-limited or on cooldown) | true |
| `executionLogSize` | Slider 10–200 | 50 |

`DevCategory.macroIgnoreSkyBlock` (Boolean, default false): "Let macros fire outside SkyBlock (dev client only)". It only has an effect when `FabricLoader.getInstance().isDevelopmentEnvironment()`.

Also add `/skysens macros` (opens the editor), `/skysens macros log` and `/skysens macros reload` (re-read the repository file) to the command tree in `SkysensClient`, via `MacroCommands.register(builder)`.

---

## 6. Editor screen (n8n look and feel)

`MacroEditorScreen extends Screen`, opened via config button, `/skysens macros`, or the `MACRO_EDITOR` action. It uses `UiScale.begin()/end()` and `Theme` colours like `SkysensConfigScreen`, `isPauseScreen() = false`, and saves in `removed()`.

### 6.1 Layout

```
┌───────────────┬───────────────────────────────────────────────────────┬──────────────────┐
│ Macros    [+] │ [Context warp ✎]   ( Editor | Executions )   [● Active]│                  │
│ ● Context warp│                                                       │  Node details    │
│ ○ Menus key   │   · · · · · · · · · · · · · · · · · · · · · ·   [+]   │  (NodeDetails-   │
│ ● Bazaar key  │   ·  ┌Hotkey┐────╮   ┌Switch┐──● Hub ───┌/warp…┐      │   Panel, opens   │
│               │   ·  │ G    │    ╰──►│island│──● Mines ─┌/warp…┐      │   on double-     │
│ [Templates]   │   ·  └──────┘        └──────┘──● else                 │   click)         │
│ [Import]      │                                                       │                  │
│               │ [−][+][⤢][1:1]                                         │                  │
└───────────────┴───────────────────────────────────────────────────────┴──────────────────┘
```

- **Left, `MacroListPanel`** (width `Theme.SIDEBAR_WIDTH`):
  - each row has a knob toggle drawn with `RenderUtil.knob` (active/inactive);
  - click selects, double-click renames inline (a `SearchField`-like text box);
  - drag a row to reorder; the order is the evaluation order and the panel says so in a hint;
  - right-click menu: Duplicate, Export to clipboard, Delete (with `ConfirmButtons`);
  - "+" creates a new workflow with one Hotkey trigger;
  - "Templates" opens a list of `MacroTemplates`;
  - "Import" pastes a `kind: workflows` document from the clipboard.
- **Header:** workflow name, an **Editor / Executions** tab switch, the active toggle, and a key-conflict chip ("G also used by 'Menus key'").
- **Canvas** (centre): `GridRenderer` dotted grid (dot every 20 world units, faded at low zoom), nodes, wires, sticky notes (drawn first, behind everything).
- **Bottom-left controls** (`CanvasControls`): zoom out, zoom in, zoom to fit (Shift+1), reset 100% (0).
- **Top-right "+"** and **Tab** open `NodePicker`.
- **Right, `NodeDetailsPanel`** (≈220 px) slides in on node double-click (or Enter). Esc or clicking empty canvas closes it.

### 6.2 Canvas rendering

- **Viewport:** `worldToScreen(x,y) = (x * zoom + panX, y * zoom + panY)`. Render under `g.pose().pushMatrix(); translate(panX, panY); scale(zoom, zoom)` inside `RenderUtil.scissor(canvasRect)`. Zoom uses discrete steps `{0.5, 0.67, 0.8, 1.0, 1.25, 1.5, 2.0}`, because cached `UiText` glyphs at arbitrary fractional scales look soft. Ctrl+scroll or plain scroll zooms around the cursor; Shift+scroll pans horizontally.
- **Nodes** (`NodeRenderer`, `NodeLayout`):
  - rounded rect 120×44 (Switch grows by 14 per output), `RenderUtil.shadow` + `roundedRectWithBorder`;
  - category colour stripe on the left (trigger green, logic orange, action blue, condition-shortcut purple), icon glyph, title (node name or type), summary line in `TEXT_MUTED`;
  - input port circle on the left; output ports on the right, labelled for If/Switch;
  - selected: `ACCENT` border; disabled: 40% alpha with a strike glyph;
  - an issue badge (top-right circle: red error, amber warning, grey info) shows its tooltip on hover.
- **Wires** (`WireRenderer`):
  - cubic bezier from out-port to in-port with control points offset horizontally by `max(40, |dx|/2)`;
  - phase 1 samples about 24 points and fills 2-px quads between them (via `RenderUtil.rect` with small rotated steps, or axis-aligned "dots" every 1.5 px);
  - phase 2 replaces this with `QuadStripRenderState implements GuiElementRenderState`, submitted through the widened `guiRenderState` with pipeline `RenderPipelines.GUI` and proper joined quads;
  - hover highlights a wire, clicking selects it, Delete removes it;
  - a midpoint "×" button appears on hover (n8n has this too).
- **Execution highlight:** wires and nodes on the last chosen path draw in green (`0xFF3FB950`), and "also matched" paths in amber. Both fade out over 15 s after the record time. In the Executions tab, clicking a record pins its highlight.
- **Sticky notes:** resizable (bottom-right handle), coloured fill, text wrapped by `UiText.wrap`, edited in the side panel or by double-click inline editing. Supports `**bold**` lines only (v1).

### 6.3 Interaction (`CanvasView` state machine)

| Gesture | Effect |
|---|---|
| Left-drag on empty canvas | Pan (spec: "pan by drag") |
| Shift+left-drag or right-drag on empty canvas | Rubber-band selection |
| Middle-drag anywhere | Pan |
| Click node | Select (Ctrl/Shift+click toggles) |
| Drag node | Move the selection, snapping to a 10-unit grid unless Alt is held. One `MoveNodesCommand` on release |
| Drag from an output port | Rubber wire. Released on an input port: `ConnectCommand` (replaces the existing wire on that output; cycles rejected with a toast). Released on empty canvas: opens `NodePicker` and auto-connects the new node (n8n behaviour) |
| Drag from a connected input port | Picks up the wire (disconnect, then re-route) |
| Double-click node | Open details panel |
| `D` | Toggle disabled on the selection |
| Delete / Backspace | Delete selected nodes and wires |
| Ctrl+A / C / X / V / D(uplicate) | Select all, copy, cut, paste (at the mouse position), duplicate |
| Ctrl+Z, Ctrl+Y / Ctrl+Shift+Z | Undo / redo |
| Tab | Node picker |
| Shift+1 / 0 | Fit / 100% |
| Ctrl+S | Commit + save now |
| Esc | Close picker → close details → close screen |

### 6.4 Node picker (`NodePicker`)

- A popover at the "+" button or the cursor, with `ui/SearchField` at the top and grouped lists: **Triggers, Logic, Conditions, Actions, Notes**. Arrow keys plus Enter to choose, fuzzy contains-match on name, group and keywords.
- **Conditions group:** n8n has no standalone condition nodes. Picking a condition (e.g. "On island") inserts an **If node pre-filled with that single rule**. This keeps the spec's four groups without a separate condition node type.
- When opened from a dangling wire, only node types with an input port are listed.

### 6.5 Node details panel (`NodeDetailsPanel`)

- Built from `NodeType.params()`. Every `ParamSpec` becomes a `ParamField`, and `visibleIf` hides irrelevant fields (e.g. `minTier` only for slayer state ≠ NONE).
- Fields follow `OptionWidget`'s method surface (`layout/render/renderOverlay/mouseClicked/keyPressed/charTyped/height`) and draw with `RenderUtil.inputBox`, `knob` and dropdown overlays styled like `DropdownWidget`. Optional later refactor: extract a `ValueBinding` interface so `ui/widget/*` can take a binding instead of an `OptionNode`, and both screens share widgets. That is not required for this feature.
- **`KeybindParamField`:** "Press a key or mouse button…". While capturing, `MacroEditorScreen` routes the next raw `keyPressed`/`mouseClicked` (including side buttons) into it, including modifiers. Esc cancels and Backspace clears. It shows conflicts: other workflows with the same chord, and vanilla `KeyMapping`s bound to the key (info only).
- **`RuleListParamField`** (If/Switch):
  - rows of `[condition type ▼] [params…] [not] [⋮]`;
  - an AND/OR toggle per group;
  - Switch shows one card per output rule with drag handles for order, plus a "fallback output" toggle;
  - renaming an output label is optional (`label` param per rule).
- Every edit goes through `EditParamsCommand(nodeId, beforeJson, afterJson)`. Consecutive typing in the same field within 1 s merges into one command.
- The bottom of the panel lists the node's issues (from `GraphValidator`) and, for triggers, "Last fired: 12 s ago".

### 6.6 Undo/redo (`history/`)

```java
public interface EditorCommand {
    void apply(EditorState s);
    void revert(EditorState s);
    String label();
    default boolean mergeWith(EditorCommand next) { return false; }
}
```

- `CommandHistory.execute(cmd)` applies and pushes, and clears redo. The limit is 200. One history per workflow, kept while the screen is open.
- Every command captures exactly what it needs (JSON copies of removed nodes plus their wires, old/new positions, old/new params).
- `CompositeCommand` wraps paste, delete-with-wires and auto-connect-from-picker.
- Workflow-list operations (rename, reorder, toggle active, delete workflow) use a separate document-level history, so undo on the canvas never silently reorders workflows.

### 6.7 Clipboard (`MacroClipboard`)

- Copy: build a `kind: nodes` document from the selection and its internal wires, then `Minecraft.getInstance().keyboardHandler.setClipboard(MacroJson.write(doc))`.
- Paste: `getClipboard()` → `MacroJson.read`. On failure, show a toast "Clipboard does not contain SkySens macro JSON". On success, remap ids, offset positions to the cursor, and run `AddNodesCommand`.
- A `kind: workflows` paste on the canvas inserts the workflows' nodes. From the list panel's "Import", it adds them as new workflows, **inactive by default**, so shared macros never fire without a look first.
- Size guard: refuse more than 500 nodes or more than 256 KB.

### 6.8 Executions tab (`ExecutionsPanel`)

A list of `ExecutionRecord`s, newest first. Each row shows: time, chord + mode, outcome chip (Ran / Rate-limited / Cooldown / No match / Failed), and the chosen workflow → action summary. Expanding a row shows every path: workflow, trigger, nodes visited, rule results (✓/✗ per rule), end reason. Clicking a path switches to its workflow and pins the highlight.

### 6.9 Templates (`MacroTemplates`)

These are JSON files in `src/client/resources/assets/skysens/macro/templates/` (the client source set holds `assets/skysens/`), listed explicitly in `MacroTemplates.FILES`. Inserting one adds it as an inactive workflow. Proposed set, for the user to confirm (Q4):

1. **Menus key:** one key. Single → `/storage`, double tap → `/pets`, long hold → `/wardrobe`. Shows press modes.
2. **Context warp:** Switch on island: Private Island → `/warp hub`; Dwarven Mines/Crystal Hollows → `/warp forge`; fallback → `/warp home`.
3. **Bazaar/AH from menus:** "also fire in GUIs". Switch: container title contains "Bazaar" → close first + `/ah`; else → close first + `/bz`.
4. **Party helper:** If in party AND is leader → `/p warp`; else If in party → party chat `{{ player }} is ready`; else → toast "Not in a party".
5. **Readme:** a sticky note explaining first-match-wins, passthrough and placeholders.

---

## 7. Editor-time overlap and validation detection (obvious cases only)

`GraphValidator.validate(MacroDocument)` produces a `Map<nodeId, List<NodeIssue>>`. It is recomputed on every committed command (cheap) and on load. `OverlapAnalyzer` covers:

| Check | Badge on | Severity |
|---|---|---|
| Same chord + mode bound by triggers in **more than one active workflow** | Each trigger, naming the others and the run order (#1, #2) | Warning |
| Same chord + mode on two triggers **in one workflow** | Both | Warning |
| **Unconditional path:** trigger → action with no logic, or through an If whose group is empty or always true, while a later trigger with the same chord exists | Later triggers: "never reached, '<wf>' always handles G" | Warning |
| Switch rule whose `canonicalKey` set equals an **earlier rule's** (identical conditions, same combinator) | The Switch, on that rule row: "never matches, same as rule 1" | Warning |
| Switch rule with no conditions before other rules | Same: "always matches, later rules unreachable" | Warning |
| If with no rules | The If: "always true" | Info |
| Deferred single-press triggers (group has double tap/long hold) | Single trigger | Info |
| LMB/RMB binding; LMB/RMB with inGuis | Trigger | Warning / Error |
| Trigger without a key; trigger without an outgoing wire; output never reaching an action | Node | Error / Info / Info |
| Action with empty text; unknown placeholder; text > 256 after placeholders with sample values; unknown config option path | Action | Error / Warning |
| Unknown node type / unknown condition type / unknown enum constant (e.g. a removed zone) | Node | Error (node preserved as is) |
| Condition unavailable (tracker missing, DIANA) | If/Switch | Warning |

Out of scope: semantic overlap between different conditions (island Hub vs zone Village, held-item glob overlaps, etc.). These are caught at press time by the multiple-match warning instead.

The list panel shows a warning dot on workflows with any warning, and the header chip shows chord conflicts.

---

## 8. Milestones

Each milestone builds (`sh ./gradlew build`) and is testable with `sh ./gradlew runClient`. Before each test session, back up `run/config/skysens/`. After each, check `run/logs/latest.log` for `Mixin apply failed`, `InvalidMixinException`, `ClassNotFoundException`/`NoClassDefFoundError` mentioning `de.vantrex`, and `run/crash-reports/`.

### M1: Model, persistence, config skeleton
Files:
- `features/macro/model/*` (MacroDocument, Workflow, MacroNode, Connection, ViewportState, MacroJson, MacroFormatUpgrader, Ids)
- `features/macro/MacroRepository.java`, `MacroService.java` (load/parse/commit, empty TriggerIndex), `MacroCommands.java` (`/skysens macros reload`, `/skysens macros debug` prints workflows/nodes/issues count)
- `features/macro/config/MacroCategory.java` + `SkysensConfig` field; `DevCategory.macroIgnoreSkyBlock`
- `persistence/RepositoryRegistry.java` (field, load/save lines, tolerant parse with `.broken-<ts>` backup)
- `features/macro/node/NodeType.java`, `NodeTypes.java`, `UnknownNode.java`, `param/*` (registry with no real types yet)

Manual test:
1. Start: `MacroRepository.json` is created on shutdown with `"version": 1, "workflows": []`.
2. Hand-edit it to add a workflow with an unknown node type, then `/skysens macros reload` and `debug`: the node is listed as unknown, and after restart the file still contains it unchanged.
3. Corrupt the JSON: the client starts, `MacroRepository.json.broken-*` exists, and a warning is in the log.
4. The Macros category shows in `/skysens` and its sliders persist across a restart.

### M2: Input pipeline + simplest end-to-end macro (trigger → action)
Files:
- `core/feature/RawInputFeature.java`, `FeatureRegistry` (field/branch/sort), `FeatureService.register(new MacroFeature())`
- `platform/mixin/KeyboardHandlerMixin.java` (new) + `skysens.client.mixins.json`; `platform/mixin/MouseMixin.java` (`onButton` inject)
- `features/macro/MacroFeature.java`, `engine/{InputChord, InputDevice, PressMode, InputTracker (SINGLE only), TriggerIndex, TriggerRef, MacroGate, GraphWalker (trigger→action only), MacroEngine, Resolution, ExecutionLog, ExecutionRecord}`
- `node/trigger/HotkeyTriggerNode`, `node/action/{SendCommandNode, SendChatNode, ToastNode, PlaySoundNode}`
- `action/{ActionNodeType, ActionOutcome, MacroSender, CooldownTracker, CloseScreenStep}`, `action/placeholder/{PlaceholderRenderer, PlaceholderRegistry}` (player, island, zone, heldItem.*)
- `MacroCommands`: `/skysens macros log`

Manual test (singleplayer, dev bypass on in `/skysens` → Dev):
1. Hand-write a workflow: Hotkey `K` → Toast "Hello {{ player }}". Press K: the toast shows, and `/skysens macros log` shows "Ran".
2. Bind a trigger to `Q` → Sound. With the macro active, Q plays the sound and does **not** drop the held item. Deactivate it (`"active": false` + reload) and Q drops the item again (passthrough).
3. Mouse side button (button 4) → Toast works.
4. Ctrl+K vs K: exact modifier matching.
5. Open chat and press K: nothing fires. Open the inventory and press K: nothing (inGuis false). Set inGuis true: it fires in the inventory, but not while typing in a creative search box.
6. Bind `action.command` `/help` to two presses within 500 ms: the second shows the "rate limited" hint. Set a cooldown of 3000 ms: the log says "On cooldown".
7. Turn the dev bypass off: nothing fires offline.

### M3: Logic, core conditions, press modes, warnings
Files:
- `node/logic/{IfNode, SwitchNode}`, `condition/{ConditionType, ConditionTypes, ConditionRule, RuleGroup, MacroActivity}`
- `condition/impl/{Island, Zone, Activity (no DIANA), Slayer, Party, HeldItem (no pet), InventoryItem, ItemPath, Container, Sneaking}Condition`
- `facts/{MacroContext, LocationFacts, PartyFacts, SlayerFacts, ItemFacts, ScreenFacts}`
- `skyblock/slayer/SlayerTracker` extension (quest boss/tier, done state)
- `engine/InputTracker` (double tap, long hold, deferred resolution via `ClientTickEvents`), `engine/MatchWarnings`
- `node/action/OpenUiNode`, `ToggleOptionNode`, `action/SkysensScreenTarget`; `closeScreenFirst` wired in
- Placeholders: party.*, slayer.*, container.title

Manual test:
1. `/mocklocation hub`, then a Switch on island (HUB → toast A, DWARVEN_MINES → toast B, fallback → toast C). Switch `/mocklocation mining_3` and press: B.
2. `/give @s diamond_sword[custom_data={id:"HYPERION",modifier:"heroic"}]`: held-item `*ION` matches, `HYPERION` with reforge `heroic` matches; a vanilla diamond sword with no EA does not.
3. Container condition: a chest renamed "Bazaar Test" + inGuis + close-first → the chest closes, then the toast shows.
4. Sneaking + negate.
5. Double tap: single → toast S, double → toast D. A quick double press shows only D and the second press is consumed. A single press shows S after about 250 ms and the key still reaches vanilla. Long hold: hold 0.5 s → toast H.
6. Two workflows on the same key, both matching: only the first runs, a chat warning lists both, and the warning repeats at most every 10 s. Reordering the workflows in JSON flips which one runs.
7. On Hypixel (user): party condition (leader/not), slayer state with a running quest, `/p warp` command via the party helper. One press sends one command.

### M4: Editor screen (view only): canvas, list, highlighting, executions
Files:
- `editor/MacroEditorScreen`, `EditorState`, `list/MacroListPanel` (select + active toggle only)
- `editor/canvas/{Viewport, CanvasView (pan/zoom only), NodeLayout, GridRenderer, NodeRenderer, WireRenderer (segment version), CanvasControls}`
- `editor/executions/ExecutionsPanel`; `MacroCategory.openEditor` button; `/skysens macros` opens the screen
- `MacroGate` excludes `MacroEditorScreen`

Manual test:
1. Open the editor via the config button and via the command. Workflows from M3 render with nodes, port labels and wires.
2. Pan by drag, zoom at the cursor, fit, reset. The canvas is clipped correctly and UI scale changes look right (try GUI scale 2/3/auto).
3. Press a macro key, then open the editor: the path is green and fades. The Executions tab lists presses with per-rule ✓/✗.
4. Toggle a workflow inactive in the list, close, press the key: nothing runs. The file shows `"active": false`.
5. Keys pressed while the editor is open never fire macros.

### M5: Editing
Files:
- `CanvasView` (select, rubber band, drag, wire drag/connect/disconnect, cycle rejection), `picker/NodePicker`
- `details/NodeDetailsPanel` + all `field/*` incl. `KeybindParamField`, `RuleListParamField`
- `MacroListPanel` (new, rename, reorder, duplicate, delete); save on `removed()` and Ctrl+S → `MacroService.commit` → TriggerIndex rebuild
- Simple direct mutations (no history yet)

Manual test:
1. Build the Context-warp workflow purely in the editor: Tab → Hotkey, capture key G; drag a wire to empty canvas → picker → Switch; add rules via dropdowns, including a zone dropdown filtered by island; attach actions.
2. Close and reopen: everything is identical. Restart the client: still identical.
3. Capture mouse side button and Ctrl+Shift combos.
4. Reorder workflows by drag: the run order changes (verify with the M3 two-workflow test).
5. Try to create a cycle: rejected with a toast.

### M6: Undo/redo, clipboard, sticky notes, disable, templates
Files:
- `editor/history/*` (all commands); route every M5 mutation through `CommandHistory`
- `editor/clipboard/MacroClipboard`, list panel Export/Import
- `node/note/StickyNoteNode`, `canvas/StickyNoteRenderer`, `ToggleDisabledCommand`, disabled rendering and walker semantics
- `template/MacroTemplates` + JSON files under `src/client/resources/assets/skysens/macro/templates/`

Manual test:
1. Add/move/connect/edit/delete, then Ctrl+Z through all of it back to the start and Ctrl+Y forward again. Dragging a node is a single undo step.
2. Copy two nodes and paste into another workflow: new ids, wires between them kept. Export a workflow, paste the JSON into a text editor: the version field is present. Import it back: it arrives as a new, inactive workflow.
3. Paste garbage: a toast, no crash.
4. Disable the Switch: a press logs "path ends at disabled node" and the key passes through.
5. Insert every template; each validates without errors.

### M7: Validation badges, overlap detection, remaining conditions
Files:
- `analysis/{GraphValidator, OverlapAnalyzer, NodeIssue}`; badges in `NodeRenderer`, list warning dots, header conflict chip
- `skyblock/mayor/MayorService` (+ mayor/minister/perks), `skyblock/time/SkyblockCalendar.now()`
- `condition/impl/{MayorCondition, SkyblockTimeCondition}`, placeholders `mayor`, `time`
- `WireRenderer` phase 2 (`QuadStripRenderState`) if phase 1 looks poor

Manual test:
1. Two active workflows on G show a badge on both triggers. Deactivating one removes the badge.
2. Trigger → action directly plus a second G trigger later: "never reached" badge.
3. Duplicate Switch rule: "never matches" badge on the second.
4. Bind LMB with inGuis: error badge. Unknown placeholder: warning.
5. On Hypixel (user): a mayor condition with the current mayor matches, and a SkyBlock time window matches.

### M8: Tracker-backed conditions (after the dungeon class and pet trackers are merged)
Files:
- `facts/{DungeonFacts, PetFacts}` against the real APIs; `condition/impl/{DungeonFloorCondition, DungeonClassCondition, ActivePetCondition}`; pet type/rarity in `HeldItemCondition` via the shared PetInfo parser
- Move the "current floor" from `features/dungeon/DungeonRunStats` to `skyblock/dungeon/` (keep `DungeonRunStats` delegating), update `DungeonRunDetector`
- Placeholders `dungeon.floor`, `dungeon.class`, `pet.*`

Manual test:
- Offline: a pet item via `/give @s player_head[custom_data={id:"PET",petInfo:'{"type":"GOLDEN_DRAGON","tier":"LEGENDARY","exp":0}'}]` → held-item pet type/rarity matches.
- On Hypixel (user): a floor condition in F7 vs M7; own class Mage; a party member with class Healer; active pet changes after `/pets` summon; placeholder `{{ pet.type }}` in a toast.

### M9: Docs and polish
- Update `docs/architecture/features.md` (RawInputFeature in the hook table), `platform.md` (`KeyboardHandlerMixin`, `MouseMixin` purpose), `config-and-persistence.md` (`MacroRepository` row, tolerant loading), `skyblock-and-integrations.md` (mayor/time/slayer extensions, floor location).
- Add a short `docs/features/macros.md` user guide if wanted.
- Full test pass with and without SkyHanni/Odin installed. They don't change macro paths, but their own key bindings may interact with passthrough.

---

## 9. Risks and open questions

**Open questions (need a user decision):**

- **Q1. Blocked first match.** When the first matching action is rate-limited or on cooldown, the plan drops the press, passes it through and does not try the next candidate (literal spec). Alternative: fall through to the next matching candidate. Which one?
- **Q2. Deferred single press.** For keys that also have double-tap/long-hold triggers, the plan's policy is §4.3: the single press fires late and never consumes. Is that acceptable, or should those keys instead consume the first press and replay it to vanilla (fragile, not recommended)?
- **Q3. Diana activity** has no live state. Options:
  - (a) Diana is mayor/minister (from the extended `MayorService`) and on Hub;
  - (b) a burrow dug within the last N minutes (needs a small Diana tracker in `skyblock/`);
  - (c) both;
  - (d) drop DIANA from v1.
- **Q4. Templates:** confirm the five in §6.9. Prefer only commands known to exist on Hypixel.
- **Q5. Defaults:** double tap 250 ms, long hold 450 ms, global rate limit 500 ms, warning throttle 10 s. Confirm.
- **Q6. Pan gesture:** left-drag on empty canvas = pan (spec), rubber band = Shift/right-drag. n8n uses drag for rubber band and Space/middle-drag for pan. Keep the spec version?
- **Q7. `WarpToMenuScreen`** is a `features/warp` internal. Accept the direct call (it is already a sanctioned cross-reference from `platform`), or add a small screen registry in `core/` that features fill?

**Risks:**

- **Other mods' input mixins.** Mods injecting at `keyPress` HEAD earlier than ours, or using Fabric's `ScreenKeyboardEvents`, still see the event when we don't cancel. When we do cancel, they don't. That is intended, but it can surprise users of other key mods. Mitigation: the editor info badge for vanilla-bound keys, and a release-note mention.
- **Hypixel rules.** One server-bound action per physical press, the global rate limit, no repeats while a key is held (repeats are ignored for resolution), no sequences. "Close inventory first" sends a container-close packet plus one command, which the spec allows. Keep templates free of anything that looks like automation.
- **The parallel trackers' APIs** may differ from the assumed getters. They are isolated in `DungeonFacts`/`PetFacts` and stored as strings in params, so only the adapters change.
- **Persisted format freeze.** Every node/condition `type` id and param key ships as a format. Review the id list in M1 before the first alpha. Afterwards, renames need a `MacroFormatUpgrader` step.
- **Tolerant repository loading** changes behaviour for all repositories (a corrupt file is backed up and reset instead of crashing). This is probably desirable, but it is a cross-cutting change; call it out in the PR.
- **Rendering at fractional zoom:** text cache quality, and many small fills for wires (performance with large graphs). Mitigation: discrete zoom steps, wire sampling adaptive to on-screen length, phase-2 quad strip.
- **Floor state relocation** touches `features/dungeon` (DungeonRunStats/RewardChestFeature floor use). Coordinate with the dungeon class tracker agent so only one of them moves it.
- **`ConfigTree.build()`** is relatively heavy (it walks the MoulConfig processor). Call it only in the editor for the CONFIG_TOGGLE dropdown and cache the resolved `OptionNode` per path for `ToggleOptionNode` at runtime.
- **Offline testability is limited.** Party, slayer, mayor, dungeon and pet conditions need Hypixel and the user's in-game help (see docs/testing.md). The dev bypass plus `/mocklocation` and `/give` with `custom_data` cover location, items, containers and all engine behaviour.

---

### Critical Files for Implementation
- `D:\work\coding\skysens\src\client\java\de\vantrex\skysens\client\platform\mixin\MouseMixin.java` (+ new `KeyboardHandlerMixin.java` alongside it, and `D:\work\coding\skysens\src\main\resources\skysens.client.mixins.json`)
- `D:\work\coding\skysens\src\client\java\de\vantrex\skysens\client\core\feature\FeatureRegistry.java` (+ new `RawInputFeature.java`, and `FeatureService.java` registration)
- `D:\work\coding\skysens\src\client\java\de\vantrex\skysens\client\persistence\RepositoryRegistry.java`
- `D:\work\coding\skysens\src\client\java\de\vantrex\skysens\client\SkysensClient.java` (command tree, `isOnSkyBlock` gate, init order)
- `D:\work\coding\skysens\src\client\java\de\vantrex\skysens\client\ui\SkysensConfigScreen.java` with `ui\render\RenderUtil.java`, `ui\render\Theme.java`, `ui\SearchField.java` (the patterns and primitives the editor reuses)

---

# Amendment A: tri-state conditions and the real tracker APIs

This amendment replaces §0 (data gaps), §3.3–3.5 (conditions and adapters), §4.4/§4.8 (evaluation and log), §7
(validation), milestones M3/M8 and §9 wherever they conflict. Background: the player can already be in a party when
(re)joining Hypixel, so tracker state can be *unknown* right after a join. The dungeon class tracker
(`skyblock/dungeon/`) and the pet tracker (`skyblock/pet/`) are on the branch. `PartyTracker.getInParty()` returns
`Optional<Boolean>`, where empty means not known yet.

## A1. Tri-state condition model (replaces the boolean `test`)

```java
/** Persisted nowhere; runtime only. */
public enum Tri {
    TRUE, FALSE, UNKNOWN;
    public static Tri of(boolean b) { return b ? TRUE : FALSE; }
    public Tri not()          { return this == UNKNOWN ? UNKNOWN : (this == TRUE ? FALSE : TRUE); }
    public boolean matches()  { return this == TRUE; }       // UNKNOWN never matches
}

/** Result of one rule, kept for the executions log. */
public record RuleResult(String ruleId, Tri value, @Nullable String unknownReason) {}
```

`ConditionType` becomes `Tri test(JsonObject params, MacroContext ctx, ReasonSink reasons)`. Every condition that
returns UNKNOWN calls `reasons.unknown("…")` with a reason.

These rules are enforced centrally in `ConditionRule`, `RuleGroup` and `SwitchNode`, never inside condition classes:

- **Negation.** `negate` uses `Tri.not()`, so `not UNKNOWN` stays UNKNOWN. "Not in party" does **not** match while the party state is unknown.
- **AND.** Any FALSE gives FALSE. Otherwise any UNKNOWN gives UNKNOWN. Otherwise TRUE.
- **OR.** Any TRUE gives TRUE. Otherwise any UNKNOWN gives UNKNOWN. Otherwise FALSE.
- **Empty group.** TRUE.
- **If node.** TRUE follows the `true` output and FALSE follows `false`. UNKNOWN follows **neither** output; the path ends as `END_UNKNOWN` and the reasons are recorded.
- **Switch node.** The first TRUE rule wins and UNKNOWN rules are skipped and recorded. The fallback fires only when every rule is FALSE. If any rule was UNKNOWN and none was TRUE, the result is `END_UNKNOWN`.
- **Engine.** A path that ends in `END_UNKNOWN` is not a candidate. If no candidate ran and at least one path was UNKNOWN, the outcome is `NO_MATCH_UNKNOWN_STATE` and the key passes through to vanilla controls. When `showBlockedHint` is on, the action bar shows a throttled hint such as "Macro skipped: party state not known yet".
- **Executions log.** `PathResult` gets a `List<RuleResult>` per logic node and the end reason `END_UNKNOWN`. `ExecutionsPanel` shows each UNKNOWN rule as a grey "?" chip with its reason, and the outcome chip reads "Unknown state". `/skysens macros log` prints the same reasons.
- **Editor badges:**
  - On every negated rule over a tracker-backed condition: "Doesn't match while the state is unknown (e.g. right after joining)".
  - On a Switch whose rules use tracker-backed conditions: "Fallback is skipped while any rule is unknown".
  - Overlap analysis treats only empty groups as "always true". `x OR not x` is not always true.

## A2. Rule: read state, not one-time messages

Every condition reads state that is rebuilt after a rejoin or reconnect: Mod API, sidebar, tab list, item NBT, or
saved tracker state that has been confirmed. A one-time start message alone is never enough. When the source hasn't
produced data since the last join, the condition returns UNKNOWN. Conditions never read `features/*` internals. The
dungeon floor comes from `DungeonClassTracker.getFloor()`, not from `DungeonRunStats`.

| Condition | Source | UNKNOWN when |
|---|---|---|
| `island` | `LocationService.getCurrentLocation()` | The location is null while on SkyBlock |
| `zone` | `LocationService.getCurrentZone()` | The island is known but the zone is null |
| `party` | `PartyTracker.getInParty()` | `getInParty()` is empty. For leader, size and member checks: also when the member list is empty although `inParty == true` |
| `activity` DUNGEON / KUUDRA | `DungeonClassTracker.isInDungeon()` / the location | The location is unknown |
| `activity` SLAYER, `slayer` | `SlayerTracker` (sidebar) | No sidebar has been read since the join. The boss/tier filter is also UNKNOWN when a quest is active but the boss line isn't parsed yet. DONE only ships if the sidebar shows it (Q9) |
| `activity` DIANA | No live source yet | Always UNKNOWN until a source exists (shown as "unavailable") |
| `dungeon.floor` | `DungeonClassTracker.getFloor()` | In a run but the floor is null. Outside a run it is FALSE |
| `dungeon.class` SELF | `getSelectedClass()` / `getSelectedClassLevel()` | The class is null, or `minLevel` is set and the level is 0 |
| `dungeon.class` ANY_MEMBER / NAMED | `getPartyMembers()` / `getClassOf(name)` | In a run but the member list is empty, or the named member's class is null, or the level is needed and 0. Outside a run it is FALSE. Optional `aliveOnly` |
| `pet.active` | `PetTracker.getActivePet()` / `isActive(type)` | The profile is not known yet (then "no pet" also counts as UNKNOWN). `level == 0` with `minLevel` set, or `PetRarity.UNKNOWN` with a rarity filter set |
| `item.*` (incl. pet items) | Live `ItemStack` NBT; `PetInfoParser.fromStack` | Never, except a pet filter set on a pet item whose `petInfo` can't be parsed |
| `screen.container`, `player.sneaking`, `time.skyblock` | Live client state / the clock | Never |
| `mayor` | `MayorService` | The first fetch hasn't succeeded yet |

**Tracker API additions (put these in the trackers, so they stay generic):**
- `PetTracker.isStateKnown()`: false until the per-profile pet has been restored on `ProfileChangeEvent`.
- `SlayerTracker.isSidebarKnown()`: true once the sidebar has been read since the last `SkyblockJoinEvent` or island change.
- `ProfileService`: a `@Nullable` getter for the current profile, if one doesn't exist.

## A3. Facts layer (replaces §3.5)

Facts return `Tri`, or a nullable value with a known flag. They never return a plain `false` that means "don't know".
Only `DungeonFacts` and `PetFacts` import `skyblock/dungeon` and `skyblock/pet`.

```java
final class PartyFacts {
    Tri inParty();                       // getInParty() empty → UNKNOWN
    Tri isLeader();                      // FALSE when not in party
    @Nullable Integer size();            // null = unknown; 1 when known not in party
    Tri hasMember(String name);
}

final class DungeonFacts {
    boolean inRun();                     // isInDungeon()
    @Nullable String floor();            // getFloor()
    @Nullable DungeonClass ownClass();   // getSelectedClass()
    int ownLevel();                      // getSelectedClassLevel(), 0 = unknown
    List<DungeonPartyMember> members();  // getPartyMembers()
    @Nullable DungeonClass classOf(String name);
}

final class PetFacts {
    Tri known();                         // PetTracker.isStateKnown()
    Optional<Pet> active();              // only meaningful when known() == TRUE
    Optional<Pet> fromStack(ItemStack s);// PetInfoParser.fromStack
}
```

- **Saved enum values:** dungeon class and pet rarity are stored as `name()` strings. `PetRarity.UNKNOWN` can't be chosen in the editor.
- **Pet type:** stored as the Hypixel pet type id. The editor suggests ids from `PetTracker.getKnownPets()` and also accepts free text.
- **Stale enum names:** an unknown enum name (e.g. after an update) shows a validation badge and never crashes.
- **Placeholders:**
  - `dungeon.floor`, `dungeon.class`, `dungeon.classLevel`
  - `pet.type`, `pet.name`, `pet.rarity`, `pet.level`, `pet.heldItem`
  - An unknown value renders `| default "…"` or an empty string. An action whose required text renders empty fails with "… not known yet" and doesn't send.

## A4. Milestone changes

- **M3** ships `Tri`, `RuleResult`, `ReasonSink` and `END_UNKNOWN` from the start. The Party, Slayer, Activity and Island/Zone conditions are tri-state.
  - Test on Hypixel: be in a party, run `/lobby`, rejoin SkyBlock, and immediately press a key bound to `If not in party → toast`.
  - Expected: nothing fires, the log shows "party state not known yet", and the key passes through.
  - Offline: party rules are always UNKNOWN, which is itself the check.
- **M8** can follow M3/M7 directly, since both trackers exist. It contains:
  - `DungeonFacts` and `PetFacts`;
  - `DungeonFloorCondition` and `DungeonClassCondition` (SELF / ANY_MEMBER / NAMED, `minLevel`, `aliveOnly`);
  - `ActivePetCondition`, and pet filters in `HeldItemCondition` / `InventoryItemCondition`;
  - the dungeon and pet placeholders.

  The "move the floor out of `DungeonRunStats`" task is dropped.
- **M1** lists `Tri`, `RuleResult` and `ReasonSink`. They exist only at runtime and add no saved keys.

## A5. More open questions and risks

- **Q8.** Should a pet condition also require the restored pet to be confirmed by the tab list or the pets menu? The default is no: knowing the profile is enough.
- **Q9.** Does the sidebar show a stable "boss slain" line? If not, the slayer state DONE is dropped from v1.
- **Risk:** users may read "nothing happened" as a bug. The mitigations are the throttled action-bar hint, the "?" chips in the log and the editor badges.

---

# Decisions (Q&A 2026-11-05)

These override the plan and Amendment A where they differ.

| # | Question | Decision |
|---|---|---|
| Q1 | First match is rate-limited or on cooldown | **Drop the press.** It passes through to vanilla controls and the next candidate is not tried. |
| Q2 | Single press on a key that also has double tap / long hold | **Accepted.** The single press fires late and never consumes the key. |
| Q3 | Diana | **Add a Diana tracker** (generic, in `skyblock/`, with events). Trackers for other SkyBlock events follow the research (see `docs/specs/skyblock-events-research.md` once written). |
| Q4 | Timings | Double tap 250 ms, long hold 450 ms, global rate limit 500 ms, warning throttle 10 s. These are **constants in code** (`features/macro/MacroConstants`), not config options. |
| Q5 | Canvas controls | **n8n's way:** drag on empty canvas draws a selection box; pan with middle-drag, Space+drag or the scroll wheel (Ctrl+wheel zooms). There is also a **first-run tutorial** (see below). |
| Q6 | Pet condition after a rejoin | The pet restored when the profile becomes known is enough; no tab-list confirmation needed. |
| Q7 | Slayer state DONE | **Dropped from v1.** |
| Q8 | Loadouts | Switching shows only "You equipped Loadout N!" and no pet message. `PetTracker` tags the next tab-list pet change within 10 s as `LOADOUT` (exact pattern `^You equipped Loadout \d+!$`). |

## First-run tutorial (part of M4/M5)

- **When it appears:** an overlay of steps the first time the editor opens. A "?" button in the toolbar opens it again. A boolean `macroTutorialSeen` is stored in the macro config (a persisted name, never rename it).
- **What each step does:** highlights the relevant part of the UI and shows a short text with Next, Back and Skip:
  1. **Macro list:** create a macro and switch it on or off; macros higher in the list win when they share a key.
  2. **Add a node:** "+" or Tab opens the node picker; every macro starts with a Hotkey Trigger.
  3. **Connect:** drag from an output dot to an input dot; Delete removes a wire or node.
  4. **Configure:** double-click a node to open its settings panel, which holds conditions, the command text and placeholders like `{{ island }}`.
  5. **If and Switch:** If has true and false outputs. In a Switch, the first matching rule wins. "Unknown" (e.g. right after joining) never matches.
  6. **Navigate:** drag to select; pan with middle-drag or Space+drag; zoom with Ctrl+wheel; Ctrl+C/V copies, Ctrl+Z/Y undoes and redoes.
  7. **Test:** press the key in game; the path it took lights up green. The Executions tab shows why something did or didn't run.
- **Where it lives:** `features/macro/editor/TutorialOverlay` with a static list of steps (anchor + text), so new steps are easy to add.

## Keybind conflicts (added 2026-11-06, part of M3, editor badge in M7)

A trigger whose key or mouse button is also bound to a key mapping in `Minecraft.options.keyMappings` (vanilla and
other mods) gets a WARNING issue: "Q is also bound to Drop Item: that binding only works when no macro action runs".

- **Matching:** device and code against the mapping's bound `InputConstants.Key`. Vanilla mappings have no modifiers, so a conflict counts regardless of the trigger's Ctrl/Shift/Alt, but the message says "Ctrl+Q also triggers Drop Item" for modified chords.
- **Where it lives:** `features/macro/analysis/KeybindConflicts` returns `List<NodeIssue>`. Results are cached per index rebuild and invalidated when the options are saved, e.g. on Controls screen close.
- **Where it shows:**
  - `/skysens macros debug`
  - one chat line per conflicting trigger after `MacroService` reload/commit, throttled
  - the trigger node's warning badge in the editor (M7)

## Dev-bypass display fixes (2026-11-06)

Off SkyBlock, which is only reachable with the dev bypass, toasts are shown directly via
`NotificationService.displayNow` and blocked hints use the vanilla action bar. The notification queue and the
secondary action bar only render on SkyBlock.
