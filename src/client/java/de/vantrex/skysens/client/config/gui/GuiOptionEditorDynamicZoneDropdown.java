package de.vantrex.skysens.client.config.gui;

import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.enums.location.SkyblockLocationEnum;
import de.vantrex.skysens.client.enums.location.zone.ZoneEnum;
import de.vantrex.skysens.client.service.LocationService;
import io.github.notenoughupdates.moulconfig.common.IFontRenderer;
import io.github.notenoughupdates.moulconfig.common.IMinecraft;
import io.github.notenoughupdates.moulconfig.common.text.StructuredText;
import io.github.notenoughupdates.moulconfig.gui.GuiComponent;
import io.github.notenoughupdates.moulconfig.gui.GuiImmediateContext;
import io.github.notenoughupdates.moulconfig.gui.MouseEvent;
import io.github.notenoughupdates.moulconfig.gui.editors.ComponentEditor;
import io.github.notenoughupdates.moulconfig.processor.ProcessedOption;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class GuiOptionEditorDynamicZoneDropdown extends ComponentEditor {
    private final List<ZoneEnum<?>> values = new ArrayList<>();
    private boolean useOrdinal;
    private SkyblockLocationEnum lastLocation = null;
    private String valuesForSearch;
    private int componentWidth = 0;

    public GuiOptionEditorDynamicZoneDropdown(ProcessedOption option, String fieldName) {
        super(option);
        Class<?> clazz = (Class<?>) option.getType();
        this.useOrdinal = clazz == int.class || clazz == Integer.class;
    }

    private void updateValues() {
        SkyblockLocationEnum currentLocation = SkysensConfig.getInstance().qolCategory.sensitivity.locationEnum.get();
        if (currentLocation == lastLocation && !values.isEmpty()) {
            return;
        }
        lastLocation = currentLocation;
        values.clear();
        if (currentLocation != null && currentLocation.getZoneEnum() != null) {
            ZoneEnum<?>[] constants = currentLocation.getZoneEnum().getEnumConstants();
            if (constants != null) {
                for (ZoneEnum<?> constant : constants) {
                    values.add(constant);
                }
                values.add(null);
            }
        }
        valuesForSearch = null;
    }

    private final GuiComponent dropdownOverlay = new GuiComponent() {
        @Override
        public int getWidth() {
            return componentWidth;
        }

        @Override
        public int getHeight() {
            return 13 + 12 * values.size();
        }

        @Override
        public boolean mouseEvent(@NotNull MouseEvent mouseEvent, @NotNull GuiImmediateContext context) {
            if (mouseEvent instanceof MouseEvent.Click) {
                MouseEvent.Click click = ((MouseEvent.Click) mouseEvent);
                if (click.getMouseState()) {
                    closeOverlay();
                }
                if (click.getMouseState() && click.getMouseButton() == 0 && context.isHovered()) {
                    int top = 0;
                    int mouseY = context.getMouseY();
                    int dropdownY = 13;
                    for (int ordinal = 0; ordinal < values.size(); ordinal++) {
                        if (mouseY >= top + 3 + dropdownY && mouseY <= top + 3 + dropdownY + 12) {
                            int selected = ordinal;
                            if (useOrdinal) {
                                option.set(selected);
                            } else {
                                option.set(values.get(selected));
                            }
                        }
                        dropdownY += 12;
                    }
                }
                return true;
            }
            return super.mouseEvent(mouseEvent, context);
        }

        @Override
        public void render(@NotNull GuiImmediateContext context) {
            updateValues();
            int selected = getSelectedIndex();
            StructuredText selectedString = StructuredText.of(" - Select - ");
            if (selected >= 0 && selected < values.size()) {
                selectedString = StructuredText.of(values.get(selected).getDisplayName());
            }

            int dropdownHeight = context.getHeight();
            int dropdownWidth = context.getWidth();

            int main = 0xff202026;
            int outlineColour = 0xffffffff;

            context.getRenderContext().pushMatrix();
            int left = 0;
            int top = 0;
            context.getRenderContext().drawColoredRect(left, top, left + 1, top + dropdownHeight, outlineColour); //Left
            context.getRenderContext().drawColoredRect(left + 1, top, left + dropdownWidth, top + 1, outlineColour); //Top
            context.getRenderContext().drawColoredRect(left + dropdownWidth - 1, top + 1, left + dropdownWidth, top + dropdownHeight, outlineColour); //Right
            context.getRenderContext().drawColoredRect(left + 1, top + dropdownHeight - 1, left + dropdownWidth - 1, top + dropdownHeight, outlineColour); //Bottom
            context.getRenderContext().drawColoredRect(left + 1, top + 1, left + dropdownWidth - 1, top + dropdownHeight - 1, main); //Middle

            context.getRenderContext().drawColoredRect(left + 1, top + 14 - 1, left + dropdownWidth - 1, top + 14, outlineColour); //Bar
            int dropdownY = 13;
            IFontRenderer fr = IMinecraft.INSTANCE.getDefaultFontRenderer();
            for (ZoneEnum<?> zone : values) {
                StructuredText optionText = StructuredText.of(selectedName(zone));
                if (optionText.getText().isEmpty()) {
                    optionText = StructuredText.of("<NONE>");
                }
                context.getRenderContext().drawStringScaledMaxWidth(
                        optionText,
                        fr,
                        left + 3,
                        top + 3 + dropdownY,
                        false,
                        dropdownWidth - 6,
                        0xffa0a0a0
                );
                dropdownY += 12;
            }
            context.getRenderContext().drawStringScaledMaxWidth(
                    selectedString, fr, left + 3, top + 3, false,
                    dropdownWidth - 16, 0xffa0a0a0
            );
            context.getRenderContext().drawOpenCloseTriangle(
                    false, context.getWidth() - 10, 4, 6, 6, -1
            );
            context.getRenderContext().popMatrix();
        }
    };

    private final GuiComponent component = wrapComponent(new GuiComponent() {
        @Override
        public int getWidth() {
            return 80;
        }

        @Override
        public int getHeight() {
            return 14;
        }

        @Override
        public boolean mouseEvent(@NotNull MouseEvent mouseEvent, @NotNull GuiImmediateContext context) {
            if (mouseEvent instanceof MouseEvent.Click && ((MouseEvent.Click) mouseEvent).getMouseState() && context.isHovered()) {
                if (!isOverlayOpen()) {
                    componentWidth = context.getWidth();
                    //Clamp the Y so that the dropdown can't go off the screen
                    int scaledHeight = context.getRenderContext().getMinecraft().getScaledHeight();
                    int clampedY;

                    if (context.getRenderOffsetY() + dropdownOverlay.getHeight() > scaledHeight) {
                        clampedY = scaledHeight - dropdownOverlay.getHeight();
                    } else {
                        clampedY = context.getRenderOffsetY();
                    }

                    openOverlay(dropdownOverlay, context.getRenderOffsetX(), clampedY);
                }
                return true;
            }
            return super.mouseEvent(mouseEvent, context);
        }

        @Override
        public void render(@NotNull GuiImmediateContext context) {
            updateValues();
            int dropdownWidth = context.getWidth();
            int selected = getSelectedIndex();
            StructuredText selectedString = StructuredText.of(" - Select - ");
            if (selected >= 0 && selected < values.size()) {
                selectedString = StructuredText.of(values.get(selected).getDisplayName());
            }

            context.getRenderContext().drawDarkRect(
                    0, 0, dropdownWidth, context.getHeight(), false
            );
            context.getRenderContext().drawOpenCloseTriangle(
                    true, context.getWidth() - 10, 4, 6, 6, -1
            );
            context.getRenderContext().drawStringScaledMaxWidth(
                    selectedString, IMinecraft.INSTANCE.getDefaultFontRenderer(),
                    3, 3, false, context.getWidth() - 16, 0xffa0a0a0
            );
        }
    });

    @Override
    public @NotNull GuiComponent getDelegate() {
        return component;
    }

    private int getSelectedIndex() {
        Object selectedObject = option.get();
        if (selectedObject == null) return -1;
        if (useOrdinal) {
            if (selectedObject instanceof Integer) {
                int selected = (int) selectedObject;
                if (selected >= 0 && selected < values.size()) {
                    return selected;
                }
            }
            return -1;
        } else {
            return values.indexOf(selectedObject);
        }
    }

    @Override
    public boolean fulfillsSearch(String word) {
        updateValues();
        if (valuesForSearch == null) {
            valuesForSearch = values.stream()
                    .map(ZoneEnum::getDisplayName)
                    .collect(Collectors.joining(" "))
                    .toLowerCase(Locale.ROOT);
        }
        return super.fulfillsSearch(word) || valuesForSearch.contains(word.toLowerCase(Locale.ROOT));
    }

    private String selectedName(ZoneEnum<?> zone) {
        return zone == null ? "Location" : zone.getDisplayName();
    }

}

