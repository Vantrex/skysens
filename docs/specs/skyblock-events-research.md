# SkyBlock events: tracker research

Researched 2026-11-05 for the conditional macros feature. **Sources:** [SHR] marks regexes from the SkyHanni repo (`constants/regexesModern.json`), which are tested against real chat. [SH] marks SkyHanni source code (`at/hannibal2/skyhanni/...`) and [wiki] marks the wiki. We take formats as facts only and copy no code.

**Unverified until checked in game:** election API perk names, the burrow chain length, mob-spawn prefixes, and every row marked "low".

## Already in the repo

- **`MayorService`:** polls `/v2/resources/skyblock/election` every 10 minutes and matches perks by their display name. Today it only exposes `isSlayerXpBuffActive()`.
- **`SkyblockCalendar`:** knows the year and Year of the Witch, but not month, day, season or hour.
- **No tracker yet** for Diana, calendar events, Jacob's contests, Kuudra tier, Garden, Rift, Bingo, Arachne or the Dark Auction.

## Diana (Mythological Ritual)

**Is the ritual running?**
- **Primary:** the mayor has the "Mythological Ritual" perk, or Jerry's Perkpocalypse has rotated to Diana. SkyHanni treats "Perkpocalypse" as active too [SH `DianaApi.kt`].
- **Chat override:** `The mythological ritual isn't active!` forces it off [SHR `event-diana.ritual-not-active`].
- **No sidebar or tab-list line** exists for Diana.

**Is the player doing Diana?**
- On the HUB, the ritual is active, and a spade is in the hotbar.
- **Spades:** `ANCESTRAL_SPADE`, `ARCHAIC_SPADE`, `DEIFIC_SPADE` [SHR `constants/events/Diana.json`].
- **Griffin pet:** read through `PetTracker` (type `GRIFFIN`).

**Chat lines** (shown with `§` codes; strip them for colourless matching):

| Event | Pattern |
|---|---|
| Burrow dug / chain progress | `§eYou (?<type>finished the Griffin burrow chain!\|dug out a Griffin Burrow!) §r§7\((?<current>\d+)/(?<max>\d+)\)` |
| Mob spawn | `§c§l(?:Oh\|Uh oh\|Yikes\|Oi\|Good Grief\|Danger\|Woah)! §r§eYou dug out (?:a )?(?:§[a-f0-9r])*(?<creatureType>[\w\s]+)§r§e!` |
| Treasure | `§6§l(?:RARE DROP!\|Wow!) §r§eYou dug out(?: a)? §r§?.+§r§e!` |
| Coins (colourless) | `Wow! You dug out (?<coins>[\d,.]+) coins!` |
| Griffin Feather | `RARE DROP! You dug out a Griffin Feather!` |
| Treasure arrow | `Follow the arrows to find the treasure!` |

**Mobs** [SHR `Diana.json`]:
- Common: Gaia Construct, Minotaur, Minos Champion, Siamese Lynxes, Minos Hunter, Cretan Bull, Harpy, Stranded Nymph.
- Rare: Sphinx, Minos Inquisitor, King Minos, Manticore.

**Verify in game:**
- The chain shows `(x/8)` in SkyHanni's tests after the Nov 2025 Diana revamp, while the wiki says 4.
- The Inquisitor prefix is `Oi!` in SkyHanni's data, not `Woah!`.

**After a rejoin:**
- Ritual active, spade and pet are rebuilt from the API, inventory and `PetTracker`.
- Burrow chain progress is chat-only, so it stays unknown until the next `(x/y)` line.

## Other events

| Event | Detect active | Key pattern | Survives rejoin |
|---|---|---|---|
| Spooky Festival | Sidebar countdown, calendar | `§6Spooky Festival§f \d*:?\d+`; `FEAR\. A Primal Fear has been summoned!` | Yes (sidebar/calendar) |
| Traveling Zoo | Sidebar | `§aTraveling Zoo§f \d*:\d+` | Yes |
| New Year | Sidebar | `§dNew Year Event!§f (?<time>.*)` | Yes |
| Season of Jerry | Calendar (winter) | Chat not verified (low) | Calendar yes |
| Hoppity's Hunt | Calendar; chat start | `§dHoppity's Hunt §r§ehas begun!…`; `§d§lHOPPITY'S HUNT §r§dA .* §r§dhas appeared!` | Calendar yes, chat no |
| Carnival | Calendar | `[NPC] Carnival Cowboy: Good luck, pal!` (shootout) | Calendar yes |
| Fishing Festival / Mining Fiesta | Calendar; tab-list countdown | Not verified (low) | Calendar yes |
| Dark Auction | Time of day; chat | `[NPC] Sirius: (?:First\|Next) up we have an? (?<item>[^,]+), the starting bid is [\d,]+ Coins!` | Schedule yes |
| Dwarven mining events | Sidebar `Event:` line | `Event: §.§[lL].*`; `Zone: §.*`; `Event Bonus: §6\+\d+` | Yes |
| Glacite / Mineshaft | Sidebar location | `Glacite Tunnels\|Great Glacite Lake`; `(?:§.)*Not started.*` | Yes |
| Arachne | Chat only | `§4☄ §r.* §r§eplaced an Arachne Crystal! Something is awakening!`; `.*§r§6§lARACHNE DOWN!` | No |
| Crystal Hollows heat | Sidebar | `^Heat: (?<scoreboard>§.(?<heat>\d+\|IMMUNE)♨?)$` | Yes |
| Garden visitors / pests | Sidebar; Vacuum Bag lore | `\s+§.✌ §.\(§.(?<currentamount>\d+)…/(?<maxamount>\d+)`; `Vacuum Bag: (?<amount>[\d,]+) [] Pests` | Yes |
| Rift time | Action bar (in the Rift) | `§[a7](?<riftTime>[\dms ]+).*` | Yes, in the Rift |
| Bingo | Sidebar title/line | Title `SKYBLOCK Ⓑ`; line ` §.Ⓑ §.Bingo` | Yes |
| Kuudra tier | Sidebar | ` §7⏣ §cKuudra's Hollow §8\(T(?<tier>\d+)\)`; `(?:§.)*Tokens: §.(?<tokens>[\w,]+)` | Yes |
| Jacob's Contest | Sidebar | `§eJacob's Contest`; chat ` {3}» (?<crop>[\w ]+) Contest on (?<season>[\w ]+) (?<day>\d+[stndh]{2,3}), Year (?<year>\d+)` | Yes (sidebar) |
| Calendar (generic) | Calendar math; calendar menu | Sidebar date `\s*(?:(?:Late\|Early) )?(?:Spring\|Summer\|Autumn\|Winter) \d+(?:st\|nd\|rd\|th)?.*` | Yes |

## Priority (as macro conditions)

1. **Diana tracker**, plus `MayorService.hasPerk(name)`.
2. **Calendar layer:** `SkyblockCalendar` gains month, day, season and hour, plus calendar-driven event windows.
3. **Sidebar event tracker:** Spooky, Zoo, New Year, Dwarven event, Jacob's contest, Kuudra tier, Garden visitors, Glacite, Bingo.
4. **Later:** Arachne (chat only), Rift time, Garden pests, Dark Auction.
