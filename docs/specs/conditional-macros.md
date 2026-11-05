# Conditional Macros (n8n-style) — Requirements

Status: requirements agreed (Q&A 2026-11-05), not yet planned or implemented.

## Goal

Bind hotkeys whose action depends on the current game state (island, zone, activity, party, held item, open
menu, …). Users build the logic visually in an in-game node editor that looks and behaves like **n8n**.

## Hard rules

- **One action per key press.** A press resolves to at most one action node (Hypixel macro rules). No
  sequences, no delays. The only allowed chain is the **"close inventory first"** pre-step (see Actions).
- **SkyBlock only.** Macros never fire outside SkyBlock.
- **First match wins.** When several branches could fire, the first in user-defined order runs, and the user is
  warned (see Warnings).
- Never fire while chat input or the macro editor is open.

## Editor (in-game, n8n look and feel)

| n8n concept | SkySens |
|---|---|
| Workflow list with active toggle | Macro list; each macro active/inactive, renamable |
| Canvas: dotted grid, bezier wires, zoom/fit controls bottom-left, pan by drag | Same |
| Node picker ("+" button / Tab, searchable, grouped) | Same: Triggers, Logic, Conditions, Actions |
| Node Details View (double-click opens side panel with parameters) | Same |
| Execution highlighting (green path of last run) | Last key press highlights the path it took |
| Node issues badge | Warning badge for overlaps/conflicts and invalid config |
| Sticky notes, disable node | Same |
| Copy/paste nodes & workflows as JSON | Same, via the system clipboard (sharing) |
| Executions tab | Log of recent presses: trigger, path taken, all branches that matched, action result, or why nothing ran |
| Undo/redo | Ctrl+Z / Ctrl+Y (Ctrl+Shift+Z) |
| Templates | A few built-in example workflows |
| Expressions `{{ … }}` | Placeholders in action text, e.g. `{{ party.leader }}`, `{{ island }}`, `{{ zone }}`, `{{ heldItem.id }}`, `{{ player }}` |

Several trigger nodes may live on one canvas (as in n8n).

## Nodes

### Trigger: Hotkey Trigger
- Key or mouse button (incl. side buttons), optional modifiers (Ctrl/Shift/Alt).
- Press mode: single press, double tap, long hold (thresholds configurable, sensible defaults).
- Option **"also fire in GUIs"** (default off). When on, the press is evaluated while a container screen is open.

### Logic
- **If**: several rules combined with AND/OR, outputs `true` / `false`.
- **Switch**: ordered rules, one output per rule, optional fallback output. First matching rule wins.
- A path that ends without an action does nothing (like n8n).

### Conditions (rules usable inside If/Switch)
| Condition | Notes |
|---|---|
| On island | `SkyblockLocationEnum` |
| In zone | Zone enums per island |
| Activity | Dungeon / slayer / Diana / Kuudra (`ActivityType`) |
| Dungeon floor | F1–F7, M1–M7 (existing `DungeonRunDetector`) |
| Dungeon class | **New tracking needed** |
| Slayer | Type, tier, quest state: active / boss spawned / done (`SlayerTracker`) |
| Party / co-op | In party, is leader, party size, member present |
| Held item | **SkyBlock item ID from `ExtraAttributes`**, never the vanilla material. Wildcards (`*_SWORD`), reforge, enchantments, pet type and rarity |
| Item in inventory | By SkyBlock ID |
| Advanced item path | Raw `ExtraAttributes` path, equals / contains / exists |
| Open container | Title contains / equals (e.g. "Bazaar", "Storage") |
| Other | Sneaking, mayor (`MayorService`), SkyBlock time |

### Actions (exactly one per press)
- **Send command**: `/command`, with placeholders.
- **Send chat message**: all, party or co-op chat, with placeholders.
- **Open SkySens UI**: warp menu, config, HUD editor, …
- **Client-side**: toast/notification, play sound, toggle a SkySens feature.
- Every action has an optional **"close inventory first"** step, which closes the open screen and then runs the action.
- Optional **per-action cooldown**.

## Runtime behaviour

- **Key passthrough**: the press is consumed only when an action actually ran. Otherwise vanilla and other
  bindings receive it.
- **Global rate limit**: minimum delay between macro-sent commands/messages (default e.g. 500 ms). Presses that
  come too early are dropped with a short hint.
- **Warnings**
  - At press time: a chat or toast message listing which branches/workflows matched and which one ran.
  - In the editor: badges on nodes that obviously overlap, and on keys bound in more than one active workflow.
  - **Keybind conflicts**: a trigger whose key or mouse button is also bound to a vanilla or mod key mapping
    (Controls screen) gets a warning. It names the conflicting binding(s), e.g. "Q is also bound to Drop Item: that
    binding only works when no macro action runs". It is shown on the trigger node in the editor, in
    `/skysens macros debug`, and once in chat after a reload or when a macro is saved.
- Order between workflows bound to the same key is user-defined (macro list order).
- **Unknown state**: the player can already be in a party when (re)joining Hypixel, and the party is only known
  once the Hypixel Mod API answered. Conditions are tri-state (true / false / unknown); unknown never matches, not
  even negated rules ("not in party"). The executions log names the unknown condition. Tracker-backed conditions
  must rely on state that is rebuilt after a rejoin, never on one-time "start" messages.

## Persistence

- Workflows are stored as JSON in a new repository (e.g. `MacroRepository`, registered in `RepositoryRegistry`).
  The class name is a persisted file name, so don't rename it later.
- The node/graph JSON format doubles as the clipboard export format and should carry a version field.

## Open points for planning

- Dungeon class detection (scoreboard/tab list) is new.
- Exact long-hold/double-tap thresholds and the default global rate limit.
- How overlap detection in the editor works (only obvious cases, e.g. identical rules or an unconditional path).
- Which example workflows to ship.
