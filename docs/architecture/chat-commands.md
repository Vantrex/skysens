# Chat commands (`!ping`, `!tps`, …)

Other players type `!<command> [args]` in co-op or party chat, or send it via `/msg`, and SkySens answers in the same channel.

## Flow

1. `ChatCommandFeature` (a `GameMessageListeningFeature`) passes every chat line to
   `ChatCommandService.onChatMessage`, as long as co-op or party commands are enabled and the player is on SkyBlock.
2. `ChatChannel` parses the formatting-stripped line (`Co-op > [MVP++] Name: !ping`) into sender and message.
   Supporting another channel means adding a constant with its prefix and reply command.
   `PRIVATE` matches incoming private messages (`From [MVP+] Name: !warpme`) with its own pattern. Its replies go back
   via `/msg <sender>` (`ChatCommandService.sendPrivateMessage`); `send(PRIVATE, …)` throws, since it has no receiver.
   Commands only work in private messages if their `channels()` include `PRIVATE`; the default is co-op and party, so
   strangers can't DM you every command.
3. The service looks the label up in `ChatCommandRegistry` (case-insensitive, names and aliases), then checks:
   whether the command is enabled, whether it's allowed in this channel, the "only my messages" option, the global
   cooldown (`usesCooldown()`), and whether another mod answers it already (below).
4. `execute(ChatCommandContext)` runs on the client thread. Replies go through `context.reply(...)` from any thread
   and are sent after a short human-like delay. Replies are sanitised so they can never trigger a command themselves.

## Adding a command

1. Implement `ChatCommand`: `name()`, `description()` (shown by `!help`), and optionally `aliases()`, `usage()`,
   `channels()`, `usesCooldown()`, `defersToOtherMods()`.
2. Generic commands go in `chatcommand/impl/`. A command that belongs to one feature lives in that feature's
   package (`ActivityCommand`, `RunsCommand`, `SlayerCommand`, `WarpToCommand`, `ProfitCommand`, `VolcanoCommand`, `WarpMeCommand`).
   `!warpto` and `!warpme` share the invite/warp/disband flow in `skyblock/party/PartyWarpService`.
3. Add a toggle to `chatcommand/config/ChatCommandToggles` and return it from `isEnabled()`.
4. Register it in the `ChatCommandService` constructor. Labels must be unique; a duplicate throws at startup.

## Other mods answering

With "Let other mods answer" on, a command whose `defersToOtherMods()` is true stays quiet when a
`ChatCommandBridge` says Odin or SkyHanni handles that label for this sender (`OdinChatCommandsBridge`,
`SkyHanniPartyCommandsBridge` in `integration/`). This avoids double answers in party chat.
