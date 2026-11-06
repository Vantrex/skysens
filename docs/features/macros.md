# Macros

Macros bind a key (or mouse button) to **one action that depends on what you are doing**. The same key can warp you
to the Hub on your island, open the bazaar in the Hub and send a party message in a dungeon. You build the logic
visually in an in-game editor that works like a small flow chart.

For developers: [../architecture/macros.md](../architecture/macros.md).

## The one rule: one action per key press

Hypixel allows a macro to do exactly one thing per key press. SkySens follows that:

- A press runs **at most one action**. There are no sequences and no delays.
- The only extra step is the optional **Close inventory first**, which closes the open screen right before the action.
- Macros only work on **SkyBlock**, never while chat, the macro editor or a menu with a text field is open.
- Macro commands and chat messages are spaced at least half a second apart.

## Turning macros on

Macros are on by default (**Macros -> Enable Macros**). Without any macro nothing happens.

## Opening the editor

- `/skysens macros`
- **Macros -> Open Editor** in the config screen
- The *Open UI* action can open it as well

The first time you open the editor a short tour explains the screen. The **?** button in the header shows it again.
**F1** (or the **Keys** button) shows the list of keyboard shortcuts:

| Keys | What it does |
|---|---|
| Drag on empty canvas | Select with a box |
| Middle-drag, Space + drag, mouse wheel | Move the canvas |
| Ctrl + wheel | Zoom |
| Shift+1 / 0 | Fit everything / reset zoom to 100% |
| Click, Shift+click, Ctrl+A | Select, add to selection, select all |
| Drag a node (hold Alt) | Move it (Alt: no grid snapping) |
| Drag from an output dot | Connect to another node; drop on empty space to add a node there |
| Delete / Backspace | Delete the selected nodes or wire |
| D | Switch the selected nodes off / on |
| Ctrl+Z, Ctrl+Y (Ctrl+Shift+Z) | Undo, redo |
| Ctrl+C / X / V, Ctrl+D | Copy, cut, paste, duplicate |
| Ctrl+S | Save (it also saves when you close the editor) |
| Tab | Open the node picker |
| Double-click / Enter | Open the node's settings |
| Esc | Close the picker, the settings or the editor |

Letter shortcuts follow your keyboard layout (Ctrl+Z is the key printed Z).

## Building a macro

1. In the list on the left press **+** to create a macro. It starts with a **Hotkey** node.
2. Click the hotkey node twice (double-click) to open its settings, click the key button and press the key or mouse
   button you want, with Ctrl, Shift or Alt if you like. Esc cancels.
3. Press **Tab** (or the **+** button on the canvas) to add nodes. Search by name; nodes are grouped as Triggers, Logic,
   Conditions, Actions and Notes. Picking a condition creates an **If** node with that condition already inside.
4. Drag from the dot on the right side of a node to the dot on the left side of the next one. Every output holds one
   wire, connecting it again replaces the wire.
5. Double-click a node to fill in its settings: the command text, the conditions, the sound.
6. Use the switch next to the macro name to turn it on. The path your last key press took lights up green on the
   canvas, so you can see what happened.

A path that ends without an action simply does nothing.

The macros in the list are checked **from top to bottom**. If two macros use the same key, the upper one wins; drag
rows to reorder them. Inside one macro, triggers are checked from top to bottom on the canvas.

### Hotkey node and press modes

| Setting | Meaning |
|---|---|
| Key | Any key or mouse button (side buttons too), optionally with Ctrl, Shift and/or Alt |
| Press: Single press | Fires when you press the key |
| Press: Double tap | Fires on the second press within a quarter of a second |
| Press: Long hold | Fires when you hold the key for about half a second |
| Also fire in inventories | Also works while a chest, bazaar or other container is open (never while you type into a search field) |

Notes:

- If a key has a double tap or long hold macro, the **single press fires slightly later** (after the double tap time)
  and the key still reaches the game normally. Only the second press of a double tap is swallowed.
- The key is swallowed (the game does not see it) **only when an action actually ran**. Otherwise the game and other
  mods get the key as usual.
- Left and right mouse buttons are not allowed to fire in inventories, they would break clicking.
- Shift and Ctrl held only as sneak or sprint do not stop a key from firing: a macro on `N` still works while sneaking.

## Logic nodes

| Node | What it does |
|---|---|
| **If** | Checks one or more conditions, combined with **All** (AND) or **Any** (OR). Two outputs: *True* and *False*. No conditions at all counts as true |
| **Switch** | Several rules, each with its own output and its own conditions, plus an optional **Else** output. The **first matching rule wins** |
| **Sticky note** | A comment on the canvas. It does nothing |

Every condition has a **Not** checkbox in the list. A node can be switched off with **D**, a path that reaches a switched-off
node does nothing.

## Conditions

| Condition | Matches when |
|---|---|
| Island | You are on one of the chosen islands |
| Zone | You are in one of the chosen zones of an island (none chosen = anywhere on it) |
| Activity | You are in a dungeon, in Kuudra, on a slayer quest or doing Diana |
| Slayer | A slayer quest is running / boss spawned / no quest; optional boss and minimum tier |
| Party | You are in a party, are its leader, the party has at least/at most/exactly N members, or a named player is in it |
| Held item | The item in your hand has this SkyBlock id (wildcards like `*_SWORD`), reforge, rarity or enchantment |
| Inventory item | You carry at least N items with this SkyBlock id |
| Item attribute | A raw attribute of your held item (`ExtraAttributes` path) exists, equals or contains a value |
| Open container | The open menu's title equals or contains your text (`Bazaar`, `Storage`, ...) |
| Sneaking | You are sneaking |
| Mayor | The mayor or minister is a given person and/or has a given perk |
| SkyBlock time | The SkyBlock clock is inside the chosen months, seasons, days of the month and hours |
| Calendar event | One of Spooky Festival, Season of Jerry, New Year Celebration or Traveling Zoo is on (computed from the SkyBlock calendar) |
| Sidebar event | A mining event, Jacob's contest (optionally for a crop), a Kuudra tier or the Spooky Festival / Traveling Zoo / New Year countdown is shown in the sidebar |
| Dungeon floor | The running dungeon is on one of the chosen floors (F1-F7, M1-M7) |
| Dungeon class | Your selected class, any party member's class or a named member's class is one of the chosen classes (optional minimum level, alive only) |
| Active pet | Your active pet has this type, rarity and/or minimum level, and optionally this pet item |

**Use held item:** in the held-item, inventory-item and item-attribute conditions the **Use held item** button reads the
item in your hand and offers its id, reforge, rarity, enchantments and other attributes as checkboxes. Every ticked
attribute becomes a condition that you can adjust afterwards (for example turn the id into `*_SWORD`). Pets are not
read this way, use the *Active pet* condition.

## Actions

Every action has two extra settings: **Close inventory first** and **Cooldown (ms)** (minimum time between two runs of
that action, 0 for none).

| Action | What it does |
|---|---|
| Send command | Sends a command such as `/warp hub` |
| Send chat message | Sends a message to all chat, party chat or co-op chat |
| Show toast | Shows a notification (default, title, chat or action bar style) |
| Play sound | Plays a Minecraft sound with volume and pitch |
| Open SkySens screen | Opens the config, the HUD editor, the warp menu or the macro editor |
| Toggle SkySens option | Flips a SkySens on/off option and tells you the new state |

Text actions are limited to 256 characters (longer text is refused, never cut off). Colour codes and line breaks are removed.

### Placeholders

Write `{{ name }}` in command, message and toast texts. `{{ name | default "text" }}` is used when the value is not
available.

| Placeholder | Value |
|---|---|
| `player` | Your name |
| `island`, `zone` | Current island and zone |
| `heldItem.id`, `heldItem.name`, `heldItem.reforge` | Your held item |
| `party.leader`, `party.size`, `party.members` | Your party |
| `slayer.boss`, `slayer.tier` | Your slayer quest |
| `container.title` | Title of the open menu |
| `mayor`, `minister` | Current mayor and minister |
| `time`, `date` | SkyBlock clock (`14:05`) and date (`Late Autumn 29, Year 450`) |
| `dungeon.floor`, `dungeon.class`, `dungeon.classLevel` | Your dungeon run |
| `pet.type`, `pet.name`, `pet.rarity`, `pet.level`, `pet.heldItem` | Your active pet |

If a placeholder has no value (and no `default`), a command or chat message is **not sent**: you get a red hint like
"{{ party.leader }} not known yet" instead of a half-filled command. In a toast the placeholder just turns into nothing.
Give important ones a `default` (`{{ party.leader | default "nobody" }}`) or add a condition that makes sure the value
exists (for example *Party: is leader* before using `party.members`).

## "Unknown" state and why nothing fires right after joining

Some information is only known a moment after you join Hypixel or SkyBlock: whether you are in a party, which pet you
have out, your dungeon class, what the sidebar shows. Until SkySens has it, the condition is **unknown**.

**Unknown never matches, not even "Not in party".** If a key press hits an unknown condition the path stops, the key is
passed on to the game, and (with *Show Blocked Hint* on) a short red hint such as "Macro skipped: party state not
known yet" appears. This avoids sending the wrong command because SkySens guessed. After a few seconds everything is known
and the macro works. A Switch's *Else* output also waits while a rule is unknown.

The **Executions** tab (and `/skysens macros log`) shows each press: which macro ran, which rules were true, false or
unknown ("?") and why.

## Warnings and badges

- **Badges on nodes** (hover them for the text): a red "!" for errors (no key bound, empty command, unknown node type) and an
  amber "!" for warnings (an unknown placeholder, a wire to a missing node, a key also used by another control). The same dot
  appears next to the macro's name in the list. Plain hints (for example "Negated rule doesn't match while the state is
  unknown") are listed in the node's settings panel only.
- **Several macros on one key:** two active macros, or two Switch rules, that could both match show a badge. At
  press time SkySens runs the first one and tells you in chat (or as a toast, or not at all, see *Several Matches
  Warning*) what else matched.
- **Key conflicts:** a trigger on a key that Minecraft or another mod also uses (for example `Q` = Drop Item) is
  marked, and the conflict is printed once in chat after a reload or save. The other binding then only works when no macro
  action ran.
- **Skipped presses:** *Show Blocked Hint* shows when a press was dropped because of a cooldown, the rate limit or an
  unknown state.
- `/skysens macros debug` lists all macros, their problems and unknown nodes; `/skysens macros reload` re-reads the
  saved macros from disk.

## Server switches and the transfer cooldown

Hypixel blocks `/warp`, `/hub`, `/is`, `/bz`, `/ah` and similar commands for a few seconds after you switch servers
("You may only use this command after 4s on the server!"). Turn on **General -> Wait for Transfer Cooldown** and a macro
command like that is held until the cooldown is over and then sent. Everything else (other commands, chat messages, toasts,
sounds) is never delayed. A red hint shows "Waiting for transfer cooldown (1.8s)".

**General -> Transfer Cooldown Queue** decides what happens when you press another key while one command waits:
**Keep newest** (default, only the latest command is sent) or **Queue all** (every command is sent in order). A command
that was sent anyway and refused is sent once more when the cooldown ends. SkySens also learns new affected commands
from the cooldown message.

## Templates, import and export

- **Templates** (button under the list) add a ready-made macro, switched off and without a key: *Menu key* (one key:
  `/storage`, double tap `/pets`, hold `/wardrobe`), *Context warp*, *Bazaar outside dungeons*, *Party helper* and
  *Quick tour* (notes that explain how macros are evaluated). Bind a key and switch it on.
- **Export** (button or right-click menu on a macro) copies the macro as text to your clipboard, **Import** reads such text
  from the clipboard. Imported macros arrive switched off. You can also copy only some nodes with Ctrl+C and paste them
  into another macro with Ctrl+V.
- Only paste macros from people you trust and look at what they send before you switch them on. A macro can send
  commands and chat messages for you.
- Macros made with a newer SkySens version cannot be imported.

Your macros are saved in `config/skysens/repositories/MacroRepository.json`. Keep a copy of the file to move them to
another computer. If the file is ever damaged, SkySens keeps it as `MacroRepository.json.broken-<time>` and starts empty.

## Settings

| Setting | Where | Meaning |
|---|---|---|
| Enable Macros | Macros | Master switch |
| Several Matches Warning (+ level) | Macros | Chat, toast or off when one press matched several branches |
| Show Blocked Hint | Macros | Hint when a press was skipped |
| Execution Log Size | Macros | How many presses the Executions tab remembers |
| Wait for Transfer Cooldown, Transfer Cooldown Queue | General | See above |
