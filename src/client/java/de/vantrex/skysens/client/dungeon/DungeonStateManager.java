package de.vantrex.skysens.client.dungeon;

import de.vantrex.skysens.client.dungeon.enums.DungeonModeEnum;
import de.vantrex.skysens.client.dungeon.enums.DungeonStageEnum;
import de.vantrex.skysens.client.dungeon.enums.F7PhaseEnum;
import de.vantrex.skysens.client.dungeon.enums.DungeonFloorEnum;
import de.vantrex.skysens.client.dungeon.event.DungeonStageChangeEvent;
import de.vantrex.skysens.client.dungeon.event.F7PhaseChangedEvent;
import de.vantrex.skysens.client.enums.location.SkyblockLocationEnum;
import de.vantrex.skysens.client.event.EventListenerRegistry;
import de.vantrex.skysens.client.event.impl.LocationChangeEvent;
import de.vantrex.skysens.client.feature.AfterTickFeature;
import de.vantrex.skysens.client.feature.FeatureRegistry;
import de.vantrex.skysens.client.feature.GameMessageListeningFeature;
import de.vantrex.skysens.client.service.FeatureService;
import de.vantrex.skysens.client.util.ClientUtil;
import lombok.Getter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.text.Text;
import de.vantrex.skysens.client.dungeon.model.DungeonSplit;

import java.util.List;

@Getter
@Environment(EnvType.CLIENT)
public class DungeonStateManager implements GameMessageListeningFeature, AfterTickFeature {

    private static final DungeonStateManager INSTANCE = new DungeonStateManager();

    private F7PhaseEnum currentPhase = F7PhaseEnum.UNKNOWN;
    private DungeonStageEnum currentStage = DungeonStageEnum.CLEARING;
    private DungeonModeEnum dungeonMode = DungeonModeEnum.NORMAL;
    private boolean inDungeon = false;

    private DungeonFloorEnum currentFloor;

    private DungeonStateManager() {
        this.register();
    }

    public static DungeonStateManager getInstance() {
        return INSTANCE;
    }

    private void register() {
         FeatureRegistry registry = FeatureService.getInstance().getFeatureRegistry();
         registry.register(this);
         EventListenerRegistry.REGISTRY.registerListener(this::onLocationChange, LocationChangeEvent.class);
         SplitManager.getInstance().loadSplits();
    }

    private void onLocationChange(LocationChangeEvent event) {
        if (event.getNewLocation() == SkyblockLocationEnum.CATACOMBS) {
            this.inDungeon = true;
            this.currentPhase = F7PhaseEnum.CLEARING;
            this.currentStage = DungeonStageEnum.CLEARING;
            this.currentFloor = null; 
            ClientUtil.sendDebug("Entered Catacombs, setting phase to CLEARING");
        } else {
            this.inDungeon = false;
            this.currentPhase = F7PhaseEnum.UNKNOWN;
            this.currentFloor = null;
        }
    }

    @Override
    public boolean isActive() {
        return true;
    }
    
    @Override
    public int priority() {
        return 1000;
    }

    @Override
    public void onGameMessage(Text message, boolean overlay) {
        if (!this.inDungeon ||  this.currentFloor == null) return;
        
        String msg = message.getString();
        List<DungeonSplit> splits = SplitManager.getInstance().getSplitsForFloor(this.currentFloor.getKey());
        
        for (DungeonSplit split : splits) {
            // Strip formatting codes from split.getStart() before comparison
            // since message.getString() doesn't include formatting codes
            String splitStartStripped = ClientUtil.stripFormattingCodes(split.getStart());
            if (msg.contains(splitStartStripped)) {
                ClientUtil.sendDebug("Split started: " + split.getName());
                updatePhaseFromSplit(split);
                break; 
            }
        }
    }
    
    private void updatePhaseFromSplit(DungeonSplit split) {
        String name = split.getName();
        
        // Map split names to F7PhaseEnum (specific to F7/M7, but generic enough to not crash other floors)
        
        if (name.equalsIgnoreCase("Maxor")) {
            setPhase(F7PhaseEnum.MAXOR);
            setStage(DungeonStageEnum.BOSS);
        } else if (name.equalsIgnoreCase("Storm")) {
            setPhase(F7PhaseEnum.STORM);
            setStage(DungeonStageEnum.BOSS);
        } else if (name.equalsIgnoreCase("Terminals") || name.equalsIgnoreCase("Goldor")) {
            setPhase(F7PhaseEnum.GOLDOR);
            setStage(DungeonStageEnum.BOSS);
        } else if (name.equalsIgnoreCase("Necron")) {
            setPhase(F7PhaseEnum.NECRON);
            setStage(DungeonStageEnum.BOSS);
        } else if (name.equalsIgnoreCase("Dragons")) {
            setPhase(F7PhaseEnum.DRAGONS);
            setStage(DungeonStageEnum.BOSS);
        }
    }

    @Override
    public void afterTick() {
        if (!this.inDungeon) return;
        MinecraftClient client = MinecraftClient.getInstance();
        
        // Detection for Master Mode vs Normal and Floor Number
        if (client.world != null && client.world.getScoreboard() != null) {
            Scoreboard scoreboard = client.world.getScoreboard();
            ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
            if (objective != null) {
                String title = objective.getDisplayName().getString();
                 if (title.contains("(M7)")) {
                     this.dungeonMode = DungeonModeEnum.MASTER;
                     this.currentFloor = DungeonFloorEnum.MASTER_FLOOR_7;
                 } else if (title.contains("(F7)")) {
                     this.dungeonMode = DungeonModeEnum.NORMAL;
                     this.currentFloor = DungeonFloorEnum.FLOOR_7;
                 } else {
                     this.currentFloor = parseFloorEnum(title);
                 }
            }
        }
    }

    private DungeonFloorEnum parseFloorEnum(String title) {
        return switch (title) {
            case String s when s.contains("(F1)") -> DungeonFloorEnum.FLOOR_1;
            case String s when s.contains("(M1)") -> DungeonFloorEnum.MASTER_FLOOR_1;
            case String s when s.contains("(F2)") -> DungeonFloorEnum.FLOOR_2;
            case String s when s.contains("(M2)") -> DungeonFloorEnum.MASTER_FLOOR_2;
            case String s when s.contains("(F3)") -> DungeonFloorEnum.FLOOR_3;
            case String s when s.contains("(M3)") -> DungeonFloorEnum.MASTER_FLOOR_3;
            case String s when s.contains("(F4)") -> DungeonFloorEnum.FLOOR_4;
            case String s when s.contains("(M4)") -> DungeonFloorEnum.MASTER_FLOOR_4;
            case String s when s.contains("(F5)") -> DungeonFloorEnum.FLOOR_5;
            case String s when s.contains("(M5)") -> DungeonFloorEnum.MASTER_FLOOR_5;
            case String s when s.contains("(F6)") -> DungeonFloorEnum.FLOOR_6;
            case String s when s.contains("(M6)") -> DungeonFloorEnum.MASTER_FLOOR_6;
            case String s when s.contains("Entrance") -> DungeonFloorEnum.ENTRANCE;
            default -> null;
        };
    }

    public void setPhase(F7PhaseEnum newPhase) {
        if (this.currentPhase != newPhase) {
            F7PhaseEnum oldPhase = this.currentPhase;
            this.currentPhase = newPhase;
            EventListenerRegistry.REGISTRY.fireEvent(new F7PhaseChangedEvent(oldPhase, newPhase));
            ClientUtil.sendDebug("Dungeon Phase Changed: " + oldPhase + " -> " + newPhase);
        }
    }
    
    public void setStage(DungeonStageEnum newStage) {
      if (this.currentStage != newStage) {
            DungeonStageEnum oldStage = this.currentStage;
            this.currentStage = newStage;
            EventListenerRegistry.REGISTRY.fireEvent(new DungeonStageChangeEvent(oldStage, newStage));
            ClientUtil.sendDebug("Dungeon Stage Changed: " + oldStage + " -> " + newStage);
        }
    }
}
