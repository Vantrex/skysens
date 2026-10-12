package de.vantrex.skysens.client.feature.chat;

import de.vantrex.skysens.client.SkysensClient;
import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.config.categories.chatfilter.ChatFilterCategory;
import de.vantrex.skysens.client.feature.GameMessageListeningFeature;
import de.vantrex.skysens.client.feature.SkySensFeature;
import de.vantrex.skysens.client.service.FeatureService;
import de.vantrex.skysens.client.util.ClientUtil;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Hides spammy chat messages, each message type has its own option. The first rule whose pattern matches decides,
 * so specific rules come before generic ones. A hidden message is still handed to the other game message features,
 * it is only kept out of the chat. Matching is done on the plain text without a trailing " (N)" counter.
 */
@SkySensFeature
public class ChatSpamFilterFeature implements GameMessageListeningFeature {

    private record Rule(Pattern pattern, Predicate<ChatFilterCategory> enabled) {
    }

    private static final String DAMAGE = "[\\d,.]+";
    private static final String HIT = " hit \\d+ enem(?:y|ies) for " + DAMAGE + " damage\\.";

    private static final List<Rule> RULES = List.of(
            // abilities
            rule("^Your Implosion" + HIT + "$", c -> c.abilities.implosion),
            rule("^Your Guided Sheep" + HIT + "$", c -> c.abilities.guidedSheep),
            rule("^Your Witherborn" + HIT + "$", c -> c.abilities.witherborn),
            rule("^Your [^\\d]+?" + HIT + "$", c -> c.abilities.otherAbilityDamage),
            rule("^Guided Sheep is now available!$", c -> c.abilities.sheepAvailable),
            rule("^Thunderstorm is ready to use! Press DROP to activate it!$", c -> c.abilities.thunderstormReady),
            rule("^Ragnarok is ready to use! Press DROP to activate it!$", c -> c.abilities.ragnarokReady),
            rule("^Castle of Stone is ready to use! Press DROP to activate it!$", c -> c.abilities.castleOfStoneReady),
            rule("^Rapid Fire is ready to use! Press DROP to activate it!$", c -> c.abilities.rapidFireReady),
            rule("^Wish is ready to use! Press DROP to activate it!$", c -> c.abilities.wishReady),
            rule("^Healing Circle is now available!$", c -> c.abilities.healingCircleAvailable),
            rule("^Maniac Miner is now available!$", c -> c.abilities.maniacMinerAvailable),
            rule("^Throwing Axe is now available!$", c -> c.abilities.throwingAxeAvailable),
            rule("^Your \\w+ ULTIMATE [\\w' ]+ is now available!$", c -> c.abilities.ultimateAvailable),
            rule("^(?:Your )?[\\w' ]+? (?:is now available!|is ready to use! Press DROP to activate it!)$", c -> c.abilities.abilityReady),
            rule("^This ability is on cooldown for [\\d.]+s\\.$", c -> c.abilities.abilityCooldown),
            // combat
            rule("^\\+[\\d,]+ Kill Combo \\+[\\d,.]+%.* Magic Find$", c -> c.combat.killComboMagicFind),
            rule("^\\+[\\d,]+ Kill Combo \\+[\\d,.]+ coins per kill$", c -> c.combat.killComboCoins),
            rule("^\\+[\\d,]+ Kill Combo \\+[\\d,.]+.* Combat Wisdom$", c -> c.combat.killComboWisdom),
            rule("^Your Kill Combo has expired! You reached a [\\d,]+ Kill Combo!$", c -> c.combat.killComboExpired),
            rule("^Granted you \\+.*$", c -> c.combat.blessingGranted),
            rule("^Also granted you \\+.*$", c -> c.combat.blessingAlsoGranted),
            rule("^Your radio is weak\\. Find another enjoyer to boost it\\.$", c -> c.combat.radioWeak),
            // stash and inventory
            rule("^You have [\\d,]+ items? stashed away!$", c -> c.stash.stashItems),
            rule("^You have [\\d,]+ materials? stashed away!$", c -> c.stash.stashMaterials),
            rule("^\\(This totals [\\d,]+ types? of materials? stashed!\\)$", c -> c.stash.stashTypes),
            rule("^>>> CLICK HERE to pick them up! <<<$", c -> c.stash.stashClick),
            rule("^\\[SkyHanni] You have \\S+ items? in stash.*$", c -> c.stash.stashSkyHanniItems),
            rule("^\\[SkyHanni] You have \\S+ materials? in stash.*$", c -> c.stash.stashSkyHanniMaterials),
            rule("^Your inventory is full!$", c -> c.stash.inventoryFull),
            // damage taken
            rule("^A Crypt Wither Skull exploded, hitting you for " + DAMAGE + " damage\\.$", c -> c.dungeons.cryptWitherSkull),
            rule("^Maxor's Frenzy hit you for " + DAMAGE + " damage\\.$", c -> c.dungeons.maxorFrenzy),
            rule("^[^:\\[]+? hit you for " + DAMAGE + " damage[.!]$", c -> c.dungeons.otherDamageTaken),
            rule("^Odin » \\w+ dragon spawned$", c -> c.dungeons.odinDragonSpawned),
            rule("^Odin » \\w+ dragon counts\\.?$", c -> c.dungeons.odinDragonCounts),
            // misc
            rule("^You earned [\\d,]+ GEXP(?: \\+ [\\d,]+ Event EXP)? from playing SkyBlock!$", c -> c.misc.gexp),
            rule("^You sold .+ x[\\d,]+ for [\\d,.]+ Coins?!$", c -> c.misc.npcSell),
            rule("^You salvaged [\\d,]+ items? for:$", c -> c.misc.salvage),
            rule("^There are blocks in the way!$", c -> c.misc.blocksInTheWay),
            rule("^Odin » Gained strength: [\\d,.]+$", c -> c.misc.odinStrength)
    );

    private static Rule rule(String regex, Predicate<ChatFilterCategory> enabled) {
        return new Rule(Pattern.compile(regex), enabled);
    }

    public ChatSpamFilterFeature() {
        // ALLOW_GAME runs before GAME, a cancelled message never reaches the normal GAME listeners. So hidden
        // messages are delivered to the other features right here.
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            if (overlay || !this.isActive() || !SkysensClient.getInstance().isOnSkyBlock()
                    || ClientUtil.isSendingOwnMessage()) {
                return true;
            }
            if (!shouldHide(message.getString())) {
                return true;
            }
            FeatureService.getInstance().getFeatureRegistry().getGameMessageListeningFeatures()
                    .forEach(feature -> feature.onGameMessage(message, false));
            return false;
        });
    }

    /** Whether the plain text matches a message type whose filter is turned on. */
    static boolean shouldHide(String text) {
        final ChatFilterCategory config = SkysensConfig.getInstance().chatFilterCategory;
        final String plain = text.strip().replaceFirst(" \\(\\d+\\)$", "");
        for (final Rule rule : RULES) {
            if (rule.pattern().matcher(plain).matches()) {
                return rule.enabled().test(config);
            }
        }
        return false;
    }

    /** Nothing to do for incoming messages, the filter hooks into the receive event itself. */
    @Override
    public void onGameMessage(Component message, boolean overlay) {
    }

    @Override
    public boolean isActive() {
        return SkysensConfig.getInstance().chatFilterCategory.enabled;
    }

}
