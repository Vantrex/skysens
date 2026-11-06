# Testing changes

There are no automated tests. A change is verified by building it and checking it in the game.

## Build

```bash
sh ./gradlew build
```

It must end with `BUILD SUCCESSFUL`. Gradle needs a JDK 25 in `JAVA_HOME`.

## Dev client (offline, singleplayer)

```bash
sh ./gradlew runClient
```

- Before testing settings or saved data, back up `run/config/skysens/` and restore it afterwards.
- Watch `run/logs/latest.log`. A broken change usually shows up as `Mixin apply failed`, `InvalidMixinException`,
  `ClassNotFoundException`/`NoClassDefFoundError` mentioning `de.vantrex`, or a new file in `run/crash-reports/`.
  The offline dev account always logs four harmless errors (user properties, Realms ×2, profile key pair).
- `/skysens` opens the config screen; Dev → Debug Mode turns on `ClientUtil.sendDebug` output in chat.
- `/mocklocation <map>` fakes a SkyBlock island, so location features can be checked offline. Most SkyBlock features
  still need Hypixel (`isOnSkyBlock()` comes from the Hypixel Mod API).
- After changing options, check that `run/config/skysens/config.json` holds them, that they survive a restart, and
  that no `config.json.broken-*` file appears.

## On Hypixel

Hypixel-only behaviour (chat commands, bazaar, dungeons, prices) is tested by the user in their own launcher instance.
Don't send automated input on Hypixel; ask the user to perform in-game actions, and only read the screen and logs.
Party chat commands are tested in a party with only the user in it. Test once without and once with SkyHanni and
Odin installed, because the integrations change which code path answers.

## Macros

Developer guide: [architecture/macros.md](architecture/macros.md). Run this list after touching `features/macro/`, the
raw input mixins, the party/pet/dungeon/slayer/sidebar/mayor/transfer trackers or the macro config. Back up
`run/config/skysens/` first (it contains `repositories/MacroRepository.json`).

### Offline (dev client)

Setup: `runClient`, then Dev -> **Macros Outside SkyBlock** on. `/mocklocation hub` (or `private_island`, `catacombs`, `null`)
fakes the island. Items: `/give @s diamond_sword[custom_data={id:"HYPERION",modifier:"heroic",enchantments:{ultimate_wise:5}}]`
and a pet item
`/give @s player_head[custom_data={id:"PET",petInfo:'{"type":"GOLDEN_DRAGON","tier":"LEGENDARY","exp":0}'}]`.

- [ ] Startup: no crash before the title screen (GLFW during init), no `Mixin apply failed` for `KeyboardHandlerMixin`/`MouseMixin`; `/skysens macros debug` prints the counts.
- [ ] **Basic:** `/skysens macros` -> **+** macro, bind a key (also a mouse side button and Ctrl+key), add a *Show toast* action, wire it, close. Press the key in the world: the toast shows, the key does nothing else.
- [ ] **Passthrough:** bind `Q` to an action behind *Island is Hub*. In the Hub `Q` fires and does not drop the item; after `/mocklocation private_island` `Q` drops the item as usual. Same for a path with no action.
- [ ] **Press modes:** one key with single press, double tap and long hold macros: single fires after the window, double tap on two quick presses (second press swallowed), long hold after about half a second.
- [ ] **Gate:** nothing fires with chat, the macro editor, the pause menu or an anvil rename field open. With a chest open it fires only for triggers with *Also fire in inventories*. LMB/RMB with that option shows an error badge.
- [ ] **Sneak/sprint:** a macro on a plain letter still fires while holding Shift (sneak) or Ctrl (sprint).
- [ ] **Logic:** If true/false; Switch picks the first matching rule; with two matching rules the *Several Matches Warning* appears (chat, toast, off).
- [ ] **Two macros on one key** show a badge on both triggers; switching one off removes it; the upper macro in the list wins.
- [ ] **Unknown state:** offline the party is never known. `If not in party -> toast` does **not** fire, the key passes through, the red hint "Macro skipped: party state not known yet" appears at most every 3 s, the Executions tab shows a grey "?" with the reason and `/skysens macros log` prints it. Switch *Else* is skipped too.
- [ ] **Conditions:** island/zone with `/mocklocation`; held item id, wildcard `*_SWORD`, reforge, enchantment; **Use held item** with the Hyperion and with an empty hand (disabled, tooltip); active pet condition with the pet item is *not* matched by held-item rules; container title; sneaking; SkyBlock time and calendar event; placeholders `{{ island }}`, `{{ heldItem.id }}` in a toast; an unknown `{{ foo }}` shows a warning badge.
- [ ] **Cooldown / rate limit:** *Cooldown (ms)* blocks a second press (hint "on cooldown"); two *Send chat* macros on different keys pressed quickly: the second is dropped with the rate-limit hint. *Close inventory first* closes an open chest before the action.
- [ ] **Editor:** drag-select, pan (middle-drag, Space+drag, wheel), zoom, fit; picker via Tab and by dropping a wire on empty canvas; disable node (D); sticky note; Ctrl+Z/Y over a move, an edit, a delete, a macro delete and rename; Ctrl+C/V between two macros; Ctrl+S "Saved"; F1 list; the first-run tour (delete `macroTutorialSeen` to see it again).
- [ ] **Import/export/templates:** Export a macro, Import it back (arrives switched off, unique name); import garbage and a newer-version document (toast with a readable message, nothing added); all five templates insert switched off.
- [ ] **Persistence:** restart: macros, order, active state and zoom/pan are back. Add `{"id":"x","type":"future.node","x":0,"y":0,"params":{"a":1},"zzz":true}` to a workflow in `MacroRepository.json`, open the editor (shows an "Unknown" node with an error badge), save, check the node and its extra key are still in the file.
- [ ] **Broken file:** replace `MacroRepository.json` with `{ not json`. The client starts, the macro list is empty, `MacroRepository.json.broken-<ms>` exists. Restore your backup afterwards.
- [ ] **Key conflicts:** a trigger on `Q` (Drop Item) shows a warning badge and one chat line after saving; F3-based debug mappings are ignored.
- [ ] **Layouts:** on a non-QWERTY layout Ctrl+Z in the editor is the key printed Z.
- [ ] `config.json` holds the Macros and General options, survives a restart, no `config.json.broken-*`.

### On Hypixel (ask the user)

Do not send automated input; ask for the actions and read the log and screen.

- [ ] **Rejoin / unknown state:** be in a party, `/lobby`, rejoin SkyBlock and immediately press a key bound to `If not in party -> toast`. Nothing fires, the log says "party state not known yet". After a few seconds it fires only if really not in a party.
- [ ] Party: in party / is leader / size / member present, with and without being leader; `{{ party.leader }}`.
- [ ] Dungeon: floor condition F7 vs M7, own class (Mage), a party member's class (Healer), `dungeon.*` placeholders; activity *In a dungeon*.
- [ ] Slayer: quest active / boss spawned / none, boss name and tier; after a rejoin the state comes back from the sidebar.
- [ ] Pet: active pet after `/pets` summon, autopet and a loadout switch; after a rejoin the saved pet counts as known; `{{ pet.type }}`.
- [ ] Mayor / perk (e.g. Diana's *Mythological Ritual*), SkyBlock time window, a sidebar event (mining event, Jacob's contest, Kuudra tier) and Diana activity.
- [ ] **Transfer cooldown:** option on, press a `/warp`/`/hub` macro right after a server switch: red "Waiting for transfer cooldown" hint, the command goes out when the cooldown ends. Press two different macros while waiting with *Keep newest* (only the last is sent) and *Queue all* (both, in order). With the option off the command is refused by Hypixel. A non-transfer command (e.g. `/pc hi`) is never delayed.
- [ ] One action per key press: no key ever produces two commands; mouse side buttons work; rate limit holds.
- [ ] Repeat once without and once with SkyHanni and Odin installed; their key bindings may interact with passthrough.

A full checklist for a large refactor is in [refactor-smoke-test-prompt.md](refactor-smoke-test-prompt.md).
