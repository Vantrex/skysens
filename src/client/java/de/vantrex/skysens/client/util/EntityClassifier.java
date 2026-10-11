package de.vantrex.skysens.client.util;

import de.vantrex.skysens.client.SkysensClient;
import de.vantrex.skysens.client.enums.location.SkyblockLocationEnum;
import de.vantrex.skysens.client.service.LocationService;
import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.ClientMannequin;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.AbstractVillager;

@UtilityClass
public class EntityClassifier {

    public enum Kind {
        LOCAL_PLAYER,
        OTHER_PLAYER,
        NPC,
        VILLAGER,
        ITEM,
        OTHER
    }

    public Kind classify(Entity entity) {
        final Minecraft mc = Minecraft.getInstance();
        if (entity == mc.player) {
            return Kind.LOCAL_PLAYER;
        }
        if (entity instanceof ItemEntity) {
            return Kind.ITEM;
        }
        if (entity instanceof ClientMannequin) {
            return Kind.NPC;
        }
        if (entity instanceof AbstractVillager) {
            return Kind.VILLAGER;
        }
        if (entity instanceof AbstractClientPlayer player) {
            return isNpcPlayer(player) ? Kind.NPC : Kind.OTHER_PLAYER;
        }
        return Kind.OTHER;
    }

    /**
     * Heuristic for player-model NPCs on Hypixel SkyBlock. NPCs are spawned as fake players that
     * either use a non-v4 (v2) UUID, or are not present in the tab list / player info map.
     * Real players always have a v4 UUID and a player info entry. Armor stands are never NPCs.
     */
    public boolean isNpcPlayer(AbstractClientPlayer player) {
        if (player.getUUID().version() == 2) {
            return true;
        }
        final ClientPacketListener connection = Minecraft.getInstance().getConnection();
        return connection != null && connection.getPlayerInfo(player.getUUID()) == null;
    }

    public boolean npcScalingAllowed() {
        if (!SkysensClient.getInstance().isOnSkyBlock()) {
            return false;
        }
        return LocationService.getInstance().getCurrentLocation() != SkyblockLocationEnum.CATACOMBS;
    }

}
