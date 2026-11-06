# Conditional macros (developer guide)

Hotkeys whose action depends on the game state, built in an in-game node editor. Everything lives in
`features/macro/` (package root `de.vantrex.skysens.client.features.macro`). User-facing description:
[../features/macros.md](../features/macros.md). Requirements and history: `docs/specs/conditional-macros.md` and
`docs/specs/conditional-macros-plan.md` (the plan is the design record; this page describes what the code does).

## Rules that shape everything

- **One action per key press** (Hypixel macro rules). A press resolves to at most one action node, no sequences, no
  delays. The only extra step is "close inventory first".
- **SkyBlock only**, except in the dev client with Dev -> "Macros Outside SkyBlock".
- **First match wins**, in workflow order and then trigger position on the canvas. More than one match only produces a
  warning.
- **Consume the key only when an action ran**, otherwise vanilla and other mods get the press.
- **Unknown is not false.** Conditions are tri-state; `UNKNOWN` never matches, not even negated.

## Package layout

| Package | Contents |
|---|---|
| `features/macro/` | `MacroFeature` (the `RawInputFeature`), `MacroService` (parsed workflows, trigger index, validation cache), `MacroRepository`, `MacroCommands` (`/skysens macros ...`), `MacroConstants` (timings) |
| `model/` | Persisted data model and the JSON parser: `Workflow`, `MacroNode`, `Connection`, `ViewportState`, `MacroDocument` (clipboard envelope), `MacroJson`, `MacroFormatUpgrader`, `Ids`, `IdRemap` |
| `node/` | `NodeType` + `NodeTypes` registry, `UnknownNode`, `Port`; `node/trigger` (hotkey), `node/logic` (`IfNode`, `SwitchNode`, `LogicNodeType`), `node/action` (six action nodes), `node/note` (sticky note), `node/param` (`ParamSpec`, `ParamKind`, `Params`, `Choice`) |
| `condition/` | `ConditionType` + `ConditionTypes` registry, `ConditionRule`, `RuleGroup`, `Tri`, `RuleResult`, `ReasonSink`, `MacroActivity`; `condition/impl` holds the 17 condition types |
| `facts/` | Per-press read-only views of game state (`MacroContext` and `LocationFacts`, `PartyFacts`, `SlayerFacts`, `DungeonFacts`, `PetFacts`, `ItemFacts`, `MayorFacts`, `TimeFacts`, `SidebarFacts`, `ScreenFacts`) |
| `engine/` | Input pipeline: `MacroEngine`, `MacroGate`, `InputChord`, `InputTracker`, `TriggerIndex`, `GraphWalker`, `Resolution`, `ExecutionLog`, `MatchWarnings`, `PressMode` |
| `action/` | What happens after the walker found an action: `ActionNodeType`, `ActionOutcome`, `ActionParams`, `MacroSender`, `TransferWaitQueue`, `CooldownTracker`, `CloseScreenStep`, `MacroToasts`, `SkysensScreenTarget`; `action/placeholder` (`PlaceholderRegistry`, `PlaceholderRenderer`) |
| `analysis/` | Editor/`debug` diagnostics: `GraphValidator`, `OverlapAnalyzer`, `KeybindConflicts`, `NodeIssue`, `ValidationResult` |
| `template/` | `MacroTemplates`: five shipped macros as JSON in `assets/skysens/macro/templates/` |
| `config/` | `MacroCategory`, `MatchWarningDisplay` |
| `editor/` | The editor screen, see [The editor](#the-editor) |

Dependencies: `engine` -> `condition`/`node`/`action` -> `facts` -> `skyblock/*`. Only the `facts` classes (plus
`ActivityCondition`/`SidebarEventCondition`, which also touch a tracker for one value) import tracker packages, so a
tracker API change has few call sites.

## Data model and JSON

### Repository

`config/skysens/repositories/MacroRepository.json` (do not rename the class, it is the file name):

```json
{ "version": 1, "workflows": [ { ...workflow... } ] }
```

`workflows` is a `List<JsonObject>` on purpose: Gson only ever sees raw JSON, and `MacroJson` parses it into the
model (`MacroService.parse()`) and writes it back (`MacroService.commit()` -> `MacroJson.workflowsToRaw`). The order of
the list is the evaluation order.

### Workflow, node, connection

```json
{
  "id": "wf_aB3dE9xZ", "name": "Context warp", "active": true,
  "viewport": { "x": 0, "y": 0, "zoom": 1.0 },
  "nodes": [
    { "id": "t", "type": "trigger.hotkey", "x": 0, "y": 0,
      "params": { "input": { "device": "KEYBOARD", "code": 75, "mods": ["CTRL"] }, "mode": "SINGLE", "inGuis": false } },
    { "id": "sw", "type": "logic.switch", "x": 240, "y": -10,
      "params": { "rules": [ { "id": "r_1", "name": "Island", "combinator": "AND",
                               "conditions": [ { "id": "c_1", "type": "island", "negate": false,
                                                 "params": { "islands": ["HUB"] } } ] } ],
                  "fallback": true } },
    { "id": "a", "type": "action.command", "x": 520, "y": 0,
      "params": { "command": "/hub", "closeScreenFirst": false, "cooldownMs": 1000 } }
  ],
  "connections": [ { "from": "t", "port": "main", "to": "sw" },
                   { "from": "sw", "port": "rule:r_1", "to": "a" } ]
}
```

- `MacroNode` optional keys: `name` (custom title), `disabled` (written only when true), `w`/`h` (only sticky notes, 0 = default).
- A wire goes from an **output port** (`main`, `true`, `false`, `rule:<ruleId>`, `fallback`) to the single input of
  another node. **Each output holds at most one wire** (the engine follows one path); the editor replaces the wire
  when you connect an occupied output and refuses cycles (`GraphEdit.ConnectResult`).
- An **If** node's params are one rule group (`combinator`, `conditions`). A **Switch** holds `rules`, a list of rule
  groups (each with its own `id`, `name`, `combinator`, `conditions`), and a `fallback` boolean. A rule is
  `{id, type, negate, params}`.
- Ids are `Ids.next(prefix)`: `wf_`, `n_`, `c_` (rule), `r_` (switch rule) plus 8 random characters.
- **Round trip guarantee:** `MacroJson` is defensive (wrong types fall back to defaults) and keeps every key it does not
  know in `extra` (workflow, node and connection level), and `params` stay raw `JsonObject`s. A node of a type this
  version does not know therefore survives a load and save untouched (`UnknownNode` only describes it in the editor and
  validation). Entries that are not objects are dropped.
- **Never rename** (persisted): node type ids (`trigger.hotkey`, `logic.if`, `logic.switch`, `action.command`,
  `action.chat`, `action.toast`, `action.sound`, `action.open_ui`, `action.toggle_option`, `note.sticky`), condition type
  ids, port ids, param keys, and the enum constant names stored in params (`PressMode`, `MacroActivity`,
  `SkysensScreenTarget`, `RuleGroup.Combinator`, `SendChatNode.Channel`, dungeon class, pet rarity, calendar event, ...).
  Changing a *default* is fine; changing a name resets or breaks users' macros.

### Clipboard / template envelope

Copy, cut, export and templates use the same parser (`MacroJson.read/write`, `MacroDocument`):

```json
{ "format": "skysens-macro", "version": 1, "kind": "workflows", "workflows": [ ... ] }
{ "format": "skysens-macro", "version": 1, "kind": "nodes", "nodes": [ ... ], "connections": [ ... ] }
```

- `kind: "nodes"` is a canvas selection (only wires between selected nodes). `kind: "workflows"` is Export and the
  templates. Pasting a `workflows` document onto the canvas contributes the nodes of all its workflows.
- `MacroJson.read` rejects text that is not JSON, not `format: "skysens-macro"` or has a `version` higher than
  `CURRENT_VERSION` ("Made with a newer SkySens"). Older versions go through `MacroFormatUpgrader` first.
- Limits (`MacroClipboard`): 500 nodes, 256 KiB of text. Pasted/imported content is **always re-parsed** and gets new
  ids (`IdRemap`); imported macros and templates arrive **switched off**, with a unique name and (templates) no key bound.
- Versioning: `MacroFormatUpgrader` runs for clipboard imports and templates (`MacroJson.read`) and for the saved
  repository (`MacroService.parse()` upgrades when the stored `version` is older than `CURRENT_VERSION`). When you bump
  `CURRENT_VERSION`, add the step to `MacroFormatUpgrader` only.

## Registries and extension points

Nothing is discovered by scanning. Everything below is an explicit `register(...)` line.

| Registry | Registers | Duplicate id |
|---|---|---|
| `node/NodeTypes` | `NodeType`s (trigger, logic, actions, note) | `IllegalStateException` at class init |
| `condition/ConditionTypes` | `ConditionType`s | `IllegalStateException` at class init |
| `action/placeholder/PlaceholderRegistry` | `{{ path }}` resolvers (also feeds editor autocomplete and the "unknown placeholder" validation) | map put, last wins |
| `template/MacroTemplates.FILES` | shipped template files | - |

### Add a condition

1. Create `condition/impl/<Name>Condition.java` implementing `ConditionType`:
   - `id()` is persisted, pick it once (`"pet.active"`, `"dungeon.floor"`, ...). `displayName()`, `group()` (picker group).
   - `params()` returns `ParamSpec`s with defaults. `visibleIf` hides a field depending on other params; `choices` feeds
     dropdowns (supplier, so live data such as the active pet can be offered). Use `TEXT_CHOICES` for "suggestions, but
     free text allowed".
   - `test(params, ctx, reasons)` returns `Tri`. **Do not handle negation, AND/OR or empty groups here**,
     `ConditionRule`/`RuleGroup`/`SwitchNode` do that centrally. Whenever you return `UNKNOWN`, call
     `reasons.unknown("...")` with a short reason (shown in the executions log).
   - `describe(params)` is the one-line text for the canvas summary and the log.
   - `validate(params, problems)` adds problems; prefix a message with `ConditionType.WARN` for a warning, otherwise it is
     an error. Tolerate stale enum names (an unknown name must produce a badge, never an exception).
   - `trackerBacked()` returns true if the answer comes from a tracker that can be unknown (editor adds the "negated rule
     does not match while unknown" and "Switch fallback is skipped" info badges).
2. Read game state through a `facts/` class (add a lazy accessor to `MacroContext` if you need a new one). Facts return
   `Tri`, or a nullable value plus a `known` flag, never a plain `false` that means "don't know". Follow the rule of
   [skyblock-and-integrations.md](skyblock-and-integrations.md#known-vs-unknown): read rebuildable state, not a one-time
   chat message, and return `UNKNOWN` until the source produced data since the last join.
3. Register it in `ConditionTypes` (static block). It appears in the node picker's "Conditions" group automatically:
   choosing one creates an If node pre-filled with that rule (`GraphEdit.createConditionNode`).
4. If the condition needs a new parameter editor, add a `ParamKind` constant and a branch in `editor/field/ParamFields.create`
   (and a `ParamField` subclass).
5. If it depends on data that must be fetched in the background (like the mayor), look at how `MacroService.rebuildIndex`
   sets `usesMayor` and refreshes in its tick.
6. Test offline with the dev bypass, see [../testing.md](../testing.md#macros).

### Add an action node

1. Create `node/action/<Name>Node.java` implementing `ActionNodeType`: `id()` (`"action.<name>"`, persisted),
   `displayName()`, `icon()`, `params()` built with `ActionParams.with(own specs...)` (adds *Close inventory first* and
   *Cooldown (ms)*), `summary(node)`, `validate(node, issues)`, and
   `execute(node, ctx)` returning an `ActionOutcome` (`ran()`, `failed(msg)`, ...).
2. Text parameters use `ParamKind.TEMPLATE_TEXT` and `PlaceholderRenderer.render(...)`; send through `MacroSender` (it
   sanitises colour codes/newlines, caps at 256 characters and applies the global rate limit). Return `true` from
   `sendsToServer()` if the action goes to the server so the 500 ms limit applies.
3. `execute` must not throw and must not block. Cooldown, rate limit and close-screen are done by the engine, not by the node.
4. Register it in `NodeTypes`. The picker lists it under "Actions" automatically.

### Add a logic or other node

A logic node implements `LogicNodeType`: `outputs(node)` declares ports (the id is persisted in wires) and
`route(node, ctx)` returns the port to follow (`null` ends the path; `unknown = true` marks "ended because a rule was
UNKNOWN") plus the per-rule `RuleResult`s and every matching port (for the "several matches" warning). Trigger kinds
other than the hotkey need engine work too: `TriggerIndex.build` only indexes `HotkeyTriggerNode`.

### Add a placeholder

Add `RESOLVERS.put("path", ctx -> ...)` in `PlaceholderRegistry`. Return `null` when the value is unavailable
(unknown state, no pet, ...). Users can write `{{ path | default "text" }}`. Send command and Send chat fail with
"{{ path }} not known yet" (shown as a red hint) instead of sending a half-filled text when a placeholder has no value
and no default; a toast just leaves the gap.

## Tri-state semantics

`Tri { TRUE, FALSE, UNKNOWN }`, runtime only, never saved. Rules (all in `ConditionRule`, `RuleGroup`, `SwitchNode`,
`IfNode`):

- `negate` uses `Tri.not()`: `not UNKNOWN` is `UNKNOWN`. "Not in party" does not match while the party is unknown.
- AND: any FALSE -> FALSE, else any UNKNOWN -> UNKNOWN, else TRUE. OR: any TRUE -> TRUE, else any UNKNOWN -> UNKNOWN,
  else FALSE. An **empty group is TRUE**.
- **If:** TRUE -> `true` port, FALSE -> `false` port, UNKNOWN follows **neither** port: the path ends as `END_UNKNOWN`.
- **Switch:** first TRUE rule wins; UNKNOWN rules are skipped and recorded; the fallback is taken only if every rule is
  FALSE; with an UNKNOWN rule and no TRUE rule the path ends as `END_UNKNOWN`.
- A path that ends `END_UNKNOWN` is not a candidate. If no candidate ran and at least one path was `END_UNKNOWN`, the
  outcome is `NO_MATCH_UNKNOWN_STATE`: nothing is consumed and a throttled hint (every 3 s, "Macro skipped: <reason>")
  appears if *Show Blocked Hint* is on.
- Why: right after (re)joining Hypixel, party, pet, sidebar and class state may not be rebuilt yet. Acting on a guess
  could send the wrong command.

Which conditions can be unknown and when is in the table in
[skyblock-and-integrations.md](skyblock-and-integrations.md#known-vs-unknown). Pure client state (held item, open
container, sneaking, the SkyBlock clock/calendar) is never unknown.

## Engine flow

```
KeyboardHandlerMixin / MouseMixin (HEAD, cancellable)
  -> FeatureRegistry.rawInputFeature -> MacroFeature (isActive = Macros "Enable") -> MacroEngine.onInput
       release (0): InputTracker.release (forget consumed key, maybe resolve a pending press); never cancelled
       repeat (2):  cancelled only if that key's press was consumed
       press (1):   InputChord.of(event)   (device, GLFW code, exact Ctrl/Shift/Alt mask)
          -> TriggerIndex.lookup(chord)    (retry without Shift/Ctrl/Alt that are only held as sneak/sprint keys)
          -> MacroGate.check()
          -> resolve now (only SINGLE triggers on the chord) or defer (double tap / long hold present)
  -> run(): MacroContext snapshot -> for each candidate trigger: GraphWalker.walk -> first path ending in an action wins
  -> engine.resolve(): cooldown -> rate limit -> close screen -> ActionNodeType.execute -> ExecutionLog + MatchWarnings
  -> press consumed (ci.cancel) only if the action ran
```

- **Mixin contract:** `RawInputFeature.keyPress$Head` / `mouseButton$Head`, called for every key/button event of the
  game window, off SkyBlock too. The feature gates itself.
- **Gate (`MacroGate.check`)**: `BLOCKED` unless macros are enabled, a player exists and we are on SkyBlock (or the dev
  bypass is on). No screen -> `WORLD`. An `AbstractContainerScreen` -> `GUI`, unless a text field that can consume input is
  focused (AH/bazaar search, anvil, sign) -> `BLOCKED`. Every other screen (chat, the macro editor, the config, pause) is
  `BLOCKED`. In `GUI` only triggers with *Also fire in inventories* apply. Left/right mouse buttons are validated as
  errors for `inGuis` because they would break clicking.
- **Trigger index (`TriggerIndex.build`)**: only active workflows and enabled (not `disabled`) hotkey nodes with a bound
  key. Order: workflow list order, then the trigger's `y`, then `x`. All triggers on one chord form a `BindingGroup`
  split by press mode.
- **Press modes** (`MacroConstants`: double tap 250 ms, long hold 450 ms, rate limit 500 ms, warning throttle 10 s;
  constants, not options):
  - Chord with only SINGLE triggers: resolved at the press; swallowed if an action ran.
  - Chord with a double tap or long hold trigger is *deferred*: the first press **always reaches vanilla**. `InputTracker`
    keeps the pending press and the client tick / key release resolves it: long hold after 450 ms held, single press after
    release (after the 250 ms window when a double tap trigger exists), double tap on the second press inside the window
    (this one is resolved synchronously and can be swallowed). SINGLE and LONG_HOLD resolved late never consume.
  - `resolveDeferred` re-checks the gate, a screen may have opened meanwhile.
- **Walker (`GraphWalker.walk`)**: from the trigger's `main` port follow the wire; at an If/Switch ask `route`; stop at an
  action (`ACTION`), a missing wire (`NO_ACTION`), a disabled node (`DISABLED`), a missing/unknown node (`INVALID`) or an
  unknown rule (`END_UNKNOWN`). Depth cap 64. **All candidates are walked** (for the log and the multiple-match warning), but
  the first one ending in `ACTION` is the one that runs.
- **First match wins, drop on block (decision Q1):** if the chosen action is on cooldown or the global rate limit
  blocks it, the press is dropped (passes through to vanilla) and no other candidate is tried.
- **Cooldown** is per action node (`CooldownTracker`, key `workflowId:nodeId`, not persisted) and starts when the action ran
  or started waiting. **Rate limit** (`MacroSender.rateLimitRemaining`) is global, 500 ms after each macro send, and only
  applies to actions with `sendsToServer()`.
- **Passthrough rules:** consumed = `Resolution.ran()` (outcome `RAN`, which includes "waiting for the transfer cooldown").
  `RATE_LIMITED`, `ON_COOLDOWN`, `FAILED`, `NO_ACTION`, `NO_MATCH_UNKNOWN_STATE` all pass the key through. A consumed key's
  repeats are also swallowed until its release.
- **Hints** (*Show Blocked Hint*): red text in the secondary action bar on SkyBlock, or the vanilla action bar off SkyBlock
  (the dev bypass), because the secondary bar and the notification queue only render on SkyBlock. Toasts from macros go
  through `MacroToasts` for the same reason (`displayNow` off SkyBlock).
- **Warnings:** `MatchWarnings` lists every extra matching path or Switch rule, throttled to once per 10 s per chord, as
  chat or toast (*Several Matches Warning*). `ExecutionLog` is an in-memory ring buffer (*Execution Log Size*, default 50).
- **Transfer cooldown wait** (General -> *Wait for Transfer Cooldown*, default off). `MacroSender.sendCommand` asks
  `TransferWaitQueue.shouldWait`: option on, `TransferCooldownTracker.isActive()` and the command's root is transfer-limited
  (`DEFAULT_TRANSFER_LIMITED` plus learned commands). Then the command is queued and the action returns `WAITING`, which
  the engine treats as *ran* (key consumed, red "Waiting for transfer cooldown (1.8s)" hint). `TransferWaitQueue.pump()`
  runs every tick and on `TransferCooldownEndEvent`, sending one command when no cooldown and no rate limit applies.
  *Transfer Cooldown Queue* decides what a second press does: `KEEP_NEWEST` (default, replaces the waiting one) or
  `QUEUE_ALL`. A command that was sent anyway and refused (`TransferCooldownDeniedEvent` within 2 s) is re-queued once.
  The queue is cleared on `SkyblockQuitEvent` and disconnect. Chat messages, party/co-op chat (sent as `/pc`, `/cc`) and
  client-side actions are never delayed.
- **Keys vs. layouts:** a trigger stores GLFW key codes, which are physical US-layout positions. The same macro on a QWERTZ
  keyboard binds the key at the same position, its label (`InputChord.displayName`) uses the local layout.
- **Dev bypass:** `Dev -> Macros Outside SkyBlock` (`devCategory.macroIgnoreSkyBlock`) only works when
  `FabricLoader.isDevelopmentEnvironment()`. With `/mocklocation <map>` the location-based conditions can then be tested offline.

## Context and facts

`MacroContext` is one snapshot per resolution (shared by all candidate paths of that press): chord, press mode, the
screen that was open at the press (kept even if *Close inventory first* closes it) and lazily created facts objects. The
held item's `ExtraAttributes` are read once per press (`ItemFacts.heldExtraAttributes`).

## The editor

`/skysens macros`, the Macros category's *Open Editor* button, or the *Open UI* action open
`editor/MacroEditorScreen`. It is a custom screen drawn with the `ui/render` primitives (`Theme`, `RenderUtil`, `UiText`,
`UiScale`), not MoulConfig widgets.

| Part | Class | Role |
|---|---|---|
| Screen | `MacroEditorScreen` | Layout, header (undo/redo, tabs, Keys, ?), key routing, save/close. `shouldCloseOnEsc` is false, Esc closes picker/details/overlays first. Blocks macros (gate) while open |
| State | `EditorState` | Working list of workflows (own `List`, shared `Workflow` objects), current workflow, tab, node/wire selection, pinned execution, `dirty`/`viewportDirty`, `revision`/`epoch` counters, toast |
| Macro list | `list/MacroListPanel` | Left sidebar: order = evaluation order, active knob, rename, duplicate, delete, import, export, templates |
| Canvas | `canvas/CanvasView` | State machine `IDLE, PAN, SELECT, DRAG_NODES, WIRE, RESIZE`; `Viewport` (discrete zoom steps), `GridRenderer`, `NodeRenderer`, `WireRenderer`, `StickyNoteRenderer`, `NodeLayout`, `CanvasControls` |
| Picker | `picker/NodePicker` | Tab or "+" or dropping a wire on empty canvas (then only nodes with an input); groups Triggers, Logic, Conditions, Actions, Notes |
| Details | `details/NodeDetailsPanel` | Right panel generated from `NodeType.params()`; lists the node's issues at the bottom |
| Fields | `field/ParamFields.create` | `ParamKind` -> `ParamField` (`TextParamField`, `MultiLineParamField`, `TextSuggestParamField`, `BoolParamField`, `ChoiceParamField`, `KeybindParamField`, `RuleListParamField`, `ConditionListEditor`). Fields read/write the node's JSON `params` directly and report through `FieldContext.changed` |
| Executions | `executions/ExecutionsPanel` | The `ExecutionLog`, per-rule ticks/crosses/"?" chips, pin a press to highlight its path |
| Overlays | `TutorialOverlay`, `ShortcutsOverlay` | First-run tour (`macroTutorialSeen`), F1 shortcut list; both are static lists, a new entry is one line |
| Tooltips | `HoverTip` | One tooltip per frame, drawn last |

### Undo/redo (`editor/history`)

A single linear `CommandHistory` (limit 200) for canvas and macro-list edits, alive while the screen is open.
`EditorCommand` has `apply`, `revert`, `label`, optional `mergeWith` (typing in one field folds into one step) and
`commitsImmediately` (the active toggle saves right away). `execute(cmd)` applies and records; `record(cmd)` records
something already applied (a finished drag, a typed character, `NodeDetailsPanel` -> `EditNodeCommand`). Commands:
`AddNodes`, `DeleteNodes`, `Connect`, `Disconnect`, `EditNode`, `MoveNodes`, `ToggleDisabled`, `Composite`,
`WorkflowCommands` (add, delete, rename, reorder, toggle active). **Any workflow mutation must go through a command**,
otherwise undo breaks and `revision` does not move (the validation cache would go stale). Undo is refused during a drag.

### Saving

Edits mutate the live `Workflow` objects, but the engine only sees changes of keys/order/active state after
`MacroService.commit(list)`: it rebuilds the raw repository JSON, saves `MacroRepository.json`, rebuilds the
`TriggerIndex`, drops the validation cache and prints keybind conflicts to chat (throttled). `commit` runs on Ctrl+S, on
closing (`removed()`, when dirty), and immediately for the active toggle. `/skysens macros reload` re-reads the file and
discards unsaved in-memory state.

### Validation cache

`GraphValidator.validate(workflows)` (per-node `validate`, placeholders, unconnected/unreachable nodes, wires to missing
nodes, `OverlapAnalyzer`, `KeybindConflicts`) is **not** run per frame. `EditorState.validation()` recomputes only when
`revision` changed (every `markDirty` and history step); `MacroService.validation()` is `null` after each rebuild and
computed lazily on first use. `NodeIssue` severities `ERROR`, `WARNING`, `INFO`; `Kind.KEYBIND` issues feed the header chip.
Overlap analysis only treats an empty group as "always true" (`x OR not x` is not), and only flags obvious cases: same
key+mode in several triggers, a trigger whose path always reaches an action, repeated/catch-all Switch rules.

## Pitfalls

- **No GLFW during mod init.** `MacroService.init()` parses the workflows while `onInitializeClient` runs, before GLFW is
  initialised. Key display names (`InputChord.displayName`, `InputConstants.Key.getDisplayName`, `GLFW.glfwGetKeyName`) call
  GLFW and crash the game on startup. That is why validation (`MacroService.validation`) is lazy and why
  `announceConflicts` returns early while no player exists. Anything new on the init path must not build key names,
  validation messages or open screens. `KeybindConflicts` and `InputChord.displayName` are for runtime use only.
- **Keyboard layouts.** Editor letter shortcuts resolve the printed letter (`MacroEditorScreen.layoutKey` uses
  `glfwGetKeyName`) so Ctrl+Z is the Z on QWERTZ. Trigger bindings are physical positions (see above); do not "fix" them
  to letters.
- **Dev bypass display paths.** Off SkyBlock the notification queue and secondary action bar do not render. New feedback
  from macros must go through `MacroToasts` / `MacroEngine.hint` style fallbacks, otherwise it is invisible in the dev client.
- **Tracker state must be rebuildable.** A condition reading a one-time start message ("Slayer quest started!") is wrong
  after a rejoin; read sidebar/tab list/NBT/Mod API state or return `UNKNOWN`.
- **Don't read `features/*` internals** from conditions (e.g. the dungeon floor comes from `DungeonClassTracker`, not from
  `DungeonRunStats`).
- **Persisted names** (see above) and `MacroCategory` field names (JSON keys) must stay.
- **Raw input runs on every key event**, including while typing in other mods' screens. Keep `onInput` cheap: a
  `HashMap` lookup first, everything else only after a chord matched.
- **Mixin gotcha:** the mixins ignore events whose `window` is not the game window (`window != minecraft.getWindow().handle()`).

## Known gaps (code vs. plan)

- Held-item and inventory-item conditions have no pet filters; pets are covered by the *Active pet* condition only
  (`HeldItemAttributes` deliberately does not offer pets).
- `MacroService.rebuildIndex` detects the need for mayor data by searching the params string for `"type":"mayor"`.
