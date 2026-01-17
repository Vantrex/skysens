package de.vantrex.skysens.client.feature;

import kotlin.collections.CollectionsKt;
import lombok.Getter;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;

import java.util.ArrayList;
import java.util.List;

@Getter
public final class FeatureRegistry {

    private final List<HandledScreenFeature> handledScreenFeature = new ArrayList<>();
    private final List<LivingEntityFeature> livingEntityFeature = new ArrayList<>();
    private final List<MouseFeature> mouseFeature = new ArrayList<>();
    private final List<ItemRightClickFeature> itemRightClickFeature = new ArrayList<>();
    private final List<AfterTickFeature> afterTickFeature = new ArrayList<>();

    public void register(Feature feature) {
        if (feature instanceof HandledScreenFeature handledScreenFeature) {
            this.handledScreenFeature.add(handledScreenFeature);
        }
        if (feature instanceof LivingEntityFeature livingEntityFeature) {
            this.livingEntityFeature.add(livingEntityFeature);
        }
        if (feature instanceof MouseFeature mouseFeature) {
            this.mouseFeature.add(mouseFeature);
        }
        if (feature instanceof ItemRightClickFeature itemRightClickFeature) {
            this.itemRightClickFeature.add(itemRightClickFeature);
        }
        if (feature instanceof AfterTickFeature afterTickFeature) {
            this.afterTickFeature.add(afterTickFeature);
        }
        this.sort();
    }

    private void sort() {
        CollectionsKt.sortBy(this.handledScreenFeature, Feature::priority);
        CollectionsKt.sortBy(this.livingEntityFeature, Feature::priority);
        CollectionsKt.sortBy(this.mouseFeature, Feature::priority);
        CollectionsKt.sortBy(this.itemRightClickFeature, Feature::priority);
        CollectionsKt.sortBy(this.afterTickFeature, Feature::priority);
    }

}
