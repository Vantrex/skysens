package de.vantrex.skysens.client.features.mining.config;

import com.google.gson.annotations.Expose;
import de.vantrex.skysens.client.notification.NotificationLevelEnum;
import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class MiningNotificationAccordion {

    @ConfigOption(name = "Consumable Buffs", desc = "Settings related to consumable buff notifications")
    @Accordion
    @Expose
    public MiningConsumableBuffsNotificationAccordion consumableBuffs = new MiningConsumableBuffsNotificationAccordion();

    @ConfigOption(name = "Pickaxe Ability", desc = "Notify when pickaxe ability is ready")
    @Expose
    @ConfigEditorBoolean
    public boolean pickaxeAbility;

    @ConfigOption(name = "Pickaxe Ability Level", desc = "Level of the pickaxe ability ready notification." + NotificationLevelEnum.LEVELS_DESC)
    @ConfigEditorDropdown
    @Expose
    public NotificationLevelEnum pickaxeAbilityLevel = NotificationLevelEnum.NORMAL;

}
