package de.vantrex.skysens.client.features.macro.node.action;

import com.google.gson.JsonPrimitive;
import de.vantrex.skysens.client.features.macro.action.ActionNodeType;
import de.vantrex.skysens.client.features.macro.action.ActionOutcome;
import de.vantrex.skysens.client.features.macro.action.ActionParams;
import de.vantrex.skysens.client.features.macro.facts.MacroContext;
import de.vantrex.skysens.client.features.macro.model.MacroNode;
import de.vantrex.skysens.client.features.macro.node.param.Choice;
import de.vantrex.skysens.client.features.macro.node.param.ParamKind;
import de.vantrex.skysens.client.features.macro.node.param.ParamSpec;
import de.vantrex.skysens.client.features.macro.node.param.Params;
import de.vantrex.skysens.client.util.ClientUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

import java.util.List;

/** Plays a sound from a curated list, stored by registry id. */
public final class PlaySoundNode implements ActionNodeType {

    private static final String DEFAULT_SOUND = "minecraft:entity.experience_orb.pickup";

    private static final List<Choice> SOUNDS = List.of(
            new Choice(DEFAULT_SOUND, "Experience orb"),
            new Choice("minecraft:block.note_block.pling", "Pling"),
            new Choice("minecraft:entity.player.levelup", "Level up"),
            new Choice("minecraft:ui.button.click", "Click"),
            new Choice("minecraft:entity.arrow.hit_player", "Arrow hit"),
            new Choice("minecraft:block.anvil.land", "Anvil"),
            new Choice("minecraft:entity.villager.yes", "Villager yes"),
            new Choice("minecraft:entity.villager.no", "Villager no"));

    @Override
    public String id() {
        return "action.sound";
    }

    @Override
    public String displayName() {
        return "Play Sound";
    }

    @Override
    public String icon() {
        return "♪";
    }

    @Override
    public List<ParamSpec> params() {
        return ActionParams.with(
                new ParamSpec("sound", "Sound", "The sound to play", ParamKind.ENUM, new JsonPrimitive(DEFAULT_SOUND), () -> SOUNDS, null),
                ParamSpec.of("volume", "Volume (%)", "0 to 200", ParamKind.INT, new JsonPrimitive(100)),
                ParamSpec.of("pitch", "Pitch (%)", "50 to 200", ParamKind.INT, new JsonPrimitive(100)));
    }

    @Override
    public String summary(MacroNode node) {
        final String id = Params.getString(node.params, "sound", DEFAULT_SOUND);
        return SOUNDS.stream().filter(c -> c.value().equals(id)).map(Choice::label).findFirst().orElse(id);
    }

    @Override
    public ActionOutcome execute(MacroNode node, MacroContext ctx) {
        final String id = Params.getString(node.params, "sound", DEFAULT_SOUND);
        final SoundEvent sound;
        try {
            sound = BuiltInRegistries.SOUND_EVENT.getOptional(Identifier.parse(id)).orElse(null);
        } catch (RuntimeException e) {
            return ActionOutcome.failed("invalid sound id " + id);
        }
        if (sound == null) {
            return ActionOutcome.failed("unknown sound " + id);
        }
        final float volume = Math.clamp(Params.getInt(node.params, "volume", 100), 0, 200) / 100f;
        final float pitch = Math.clamp(Params.getInt(node.params, "pitch", 100), 50, 200) / 100f;
        ClientUtil.playSound(sound, volume, pitch);
        return ActionOutcome.ran();
    }

}
