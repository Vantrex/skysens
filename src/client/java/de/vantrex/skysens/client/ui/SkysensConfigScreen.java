package de.vantrex.skysens.client.ui;

import com.mojang.blaze3d.platform.InputConstants;
import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.ui.model.CategoryNode;
import de.vantrex.skysens.client.ui.model.ConfigTree;
import de.vantrex.skysens.client.ui.model.OptionNode;
import de.vantrex.skysens.client.ui.render.Animated;
import de.vantrex.skysens.client.ui.render.RenderUtil;
import de.vantrex.skysens.client.ui.render.Theme;
import de.vantrex.skysens.client.ui.render.UiText;
import de.vantrex.skysens.client.ui.widget.OptionWidget;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Custom modern config screen. MoulConfig is only used as the data layer; everything visible is drawn here.
 */
public class SkysensConfigScreen extends Screen {

    private static final int NAV_ITEM_H = 20;
    private static final int NAV_GAP = 2;
    private static final int NAV_TOP_OFFSET = 46;
    private static final int NAV_FOOTER_H = 22;
    private static final int SCROLLBAR_W = 3;
    private static final int SEARCH_W = 160;
    private static final int SEARCH_H = 18;
    /** Header space taken by the theme button (button + gap). */
    private static final int PICKER_SLOT = ThemePicker.BUTTON_SIZE + 4;

    // remembered across openings
    private static String lastCategoryId;
    private static float lastScroll;

    private final Screen parent;
    private final List<CategoryNode> tree;
    private final List<NavEntry> nav = new ArrayList<>();
    private final Map<String, List<OptionWidget>> categoryWidgets = new HashMap<>();
    private final SearchField searchField;
    private final ThemePicker themePicker = new ThemePicker();
    private final Animated indicatorY = new Animated(0f, 160f);
    private final String version;

    private CategoryNode active;
    private String query = "";
    private List<OptionWidget> searchWidgets = new ArrayList<>();
    private int searchMatchCount;

    // window geometry
    private int winX, winY, winW, winH;
    private int sidebarW = Theme.SIDEBAR_WIDTH;
    private int searchW = SEARCH_W;
    private int vx, vy, vw, vh; // content viewport

    // scrolling
    private float scroll;
    private float scrollTarget;
    private float maxScroll;
    private int contentHeight;
    private long lastFrameMillis = -1;
    private boolean draggingBar;
    private double barGrabOffset;
    private int navScroll;
    private boolean indicatorSnapped;

    private record NavEntry(CategoryNode node, int depth) {
    }

    public SkysensConfigScreen(Screen parent) {
        super(Component.literal("SkySens"));
        this.parent = parent;
        Theme.loadFromConfig();
        UiText.loadFromConfig();
        this.tree = ConfigTree.build();
        this.searchField = new SearchField(this::onQueryChanged);
        this.version = FabricLoader.getInstance().getModContainer("skysens")
                .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("dev");
        for (CategoryNode root : tree) {
            addNav(root, 0);
        }
        CategoryNode initial = null;
        if (lastCategoryId != null) {
            for (NavEntry e : nav) {
                if (e.node().id().equals(lastCategoryId)) {
                    initial = e.node();
                    break;
                }
            }
        }
        if (initial == null && !nav.isEmpty()) {
            initial = nav.get(0).node();
        } else if (initial != null) {
            this.scrollTarget = lastScroll;
            this.scroll = lastScroll;
        }
        this.active = initial;
    }

    private void addNav(CategoryNode node, int depth) {
        nav.add(new NavEntry(node, depth));
        for (CategoryNode child : node.children()) {
            addNav(child, depth + 1);
        }
    }

    // ------------------------------------------------------------------ lifecycle

    @Override
    protected void init() {
        winW = Math.min(width - 32, 760);
        winH = Math.min(height - 32, 440);
        winX = (width - winW) / 2;
        winY = (height - winH) / 2;
        // narrow screens (high GUI scale): shrink the sidebar and search box so nothing overlaps
        sidebarW = Mth.clamp(winW / 4, 100, Theme.SIDEBAR_WIDTH);
        vx = winX + 2 + sidebarW;
        vy = winY + Theme.HEADER_HEIGHT + 1;
        vw = winX + winW - 1 - vx;
        vh = winY + winH - 1 - vy;
        searchW = Mth.clamp(vw - 2 * Theme.CONTENT_PADDING - 80 - PICKER_SLOT, 60, SEARCH_W);
        searchField.layout(winX + winW - 12 - searchW, winY + (Theme.HEADER_HEIGHT - SEARCH_H) / 2,
                searchW, SEARCH_H);
        themePicker.layout(winX + winW - 12 - searchW - 4 - ThemePicker.BUTTON_SIZE,
                winY + (Theme.HEADER_HEIGHT - ThemePicker.BUTTON_SIZE) / 2, width, height);
        indicatorSnapped = false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void onClose() {
        // same as vanilla sub-screens; setScreenAndShow would render a nested frame inside the input callback
        minecraft.gui.setScreen(parent);
    }

    @Override
    public void removed() {
        lastCategoryId = active == null ? null : active.id();
        lastScroll = query.isBlank() ? scrollTarget : 0f;
        closeAllOverlays();
        SkysensConfig.getConfig().saveToFile();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        Theme.update();
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(0, 0, width, height, Theme.BACKDROP);
    }

    // ------------------------------------------------------------------ model helpers

    private boolean searching() {
        return !query.isBlank();
    }

    private List<OptionWidget> currentWidgets() {
        if (searching()) {
            return searchWidgets;
        }
        if (active == null) {
            return List.of();
        }
        return categoryWidgets.computeIfAbsent(active.id(), id -> {
            List<OptionWidget> list = new ArrayList<>();
            for (OptionNode n : active.options()) {
                list.add(WidgetFactory.create(n));
            }
            return list;
        });
    }

    private void onQueryChanged(String text) {
        closeAllOverlays();
        query = text == null ? "" : text;
        searchWidgets = new ArrayList<>();
        searchMatchCount = 0;
        if (searching()) {
            String lower = query.trim().toLowerCase(Locale.ROOT);
            for (OptionNode leaf : ConfigTree.allLeaves(tree)) {
                if (leaf.matches(lower)) {
                    OptionWidget w = WidgetFactory.create(leaf);
                    w.setShowBreadcrumb(true);
                    searchWidgets.add(w);
                }
            }
            searchMatchCount = searchWidgets.size();
        }
        scroll = 0f;
        scrollTarget = 0f;
    }

    private void selectCategory(CategoryNode node) {
        closeAllOverlays();
        if (searching()) {
            searchField.setText("");
        }
        if (node != active) {
            active = node;
            scroll = 0f;
            scrollTarget = 0f;
        }
    }

    private void closeAllOverlays() {
        themePicker.close();
        for (OptionWidget w : searchWidgets) {
            w.closeOverlay();
        }
        for (List<OptionWidget> list : categoryWidgets.values()) {
            for (OptionWidget w : list) {
                w.closeOverlay();
            }
        }
    }

    private OptionWidget overlayOwner() {
        for (OptionWidget w : currentWidgets()) {
            if (w.hasOverlay()) {
                return w;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ layout

    private void layoutContent() {
        int x = vx + Theme.CONTENT_PADDING;
        int w = Math.max(40, vw - 2 * Theme.CONTENT_PADDING);
        int y = vy + Theme.CONTENT_PADDING - Math.round(scroll);
        int total = Theme.CONTENT_PADDING;
        List<OptionWidget> widgets = currentWidgets();
        for (OptionWidget widget : widgets) {
            widget.layout(x, y, w);
            int h = widget.height();
            y += h + Theme.CARD_GAP;
            total += h + Theme.CARD_GAP;
        }
        if (!widgets.isEmpty()) {
            total = total - Theme.CARD_GAP + Theme.CONTENT_PADDING;
        } else {
            total = 0;
        }
        contentHeight = total;
        maxScroll = Math.max(0, contentHeight - vh);
    }

    private void updateScroll() {
        long now = Util.getMillis();
        float dt = lastFrameMillis < 0 ? 0f : (now - lastFrameMillis) / 1000f;
        lastFrameMillis = now;
        scrollTarget = Mth.clamp(scrollTarget, 0f, maxScroll);
        float k = 1f - (float) Math.pow(0.0001, dt);
        scroll += (scrollTarget - scroll) * k;
        if (Math.abs(scrollTarget - scroll) < 0.05f) {
            scroll = scrollTarget;
        }
        scroll = Mth.clamp(scroll, 0f, maxScroll);
    }

    private int navTop() {
        return winY + NAV_TOP_OFFSET;
    }

    private int navHeightAvailable() {
        return winY + winH - NAV_FOOTER_H - navTop();
    }

    private int navTotalHeight() {
        return nav.size() * (NAV_ITEM_H + NAV_GAP);
    }

    private int navItemY(int index) {
        return navTop() + index * (NAV_ITEM_H + NAV_GAP) - navScroll;
    }

    private int navX() {
        return winX + 1 + 6;
    }

    private int navW() {
        return sidebarW - 12;
    }

    // ------------------------------------------------------------------ rendering

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        Theme.update();
        layoutContent();
        updateScroll();
        layoutContent(); // re-layout with the updated scroll so widgets and hit-tests agree

        // window
        RenderUtil.shadow(g, winX, winY, winW, winH, Theme.WINDOW_RADIUS);
        RenderUtil.roundedRectWithBorder(g, winX, winY, winW, winH, Theme.WINDOW_RADIUS, Theme.PANEL, Theme.BORDER);

        renderSidebar(g, mouseX, mouseY);
        renderHeader(g, mouseX, mouseY);

        // content
        OptionWidget owner = overlayOwner();
        boolean blockHover = (owner != null && owner.overlayContains(mouseX, mouseY))
                || themePicker.overlayContains(mouseX, mouseY);
        boolean inViewport = RenderUtil.inside(mouseX, mouseY, vx, vy, vw, vh);
        int hx = (inViewport && !blockHover) ? mouseX : -10000;
        int hy = (inViewport && !blockHover) ? mouseY : -10000;

        List<OptionWidget> widgets = currentWidgets();
        RenderUtil.scissor(g, vx, vy, vw, vh);
        for (OptionWidget w : widgets) {
            // visibility culling
            int wy = w.top();
            if (wy + w.height() < vy || wy > vy + vh) {
                continue;
            }
            w.render(g, hx, hy, partialTick);
        }
        if (widgets.isEmpty()) {
            renderEmptyState(g);
        }
        RenderUtil.unscissor(g);

        renderScrollbar(g, mouseX, mouseY);

        // overlays above everything
        g.nextStratum();
        for (OptionWidget w : widgets) {
            w.renderOverlay(g, mouseX, mouseY, partialTick);
        }
        themePicker.renderOverlay(g, mouseX, mouseY);
    }

    private void renderEmptyState(GuiGraphicsExtractor g) {
        String msg = searching()
                ? "No settings match “" + query.trim() + "”"
                : "Nothing to configure here yet";
        UiText.drawCentered(g, msg, vx + vw / 2, vy + vh / 2 - UiText.lineHeight() / 2, Theme.TEXT_MUTED);
    }

    private void renderSidebar(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int sx = winX + 1;
        // sidebar background with rounded left corners (flat themes share the panel surface instead)
        if (Theme.style().sidebarBlock()) {
            RenderUtil.roundedRect(g, sx, winY + 1, sidebarW, winH - 2, Math.max(0, Theme.WINDOW_RADIUS - 1),
                    Theme.SIDEBAR, true, false, true, false);
        }
        // divider
        RenderUtil.rect(g, sx + sidebarW, winY + 1, 1, winH - 2, lineColour());

        // brand
        int bx = sx + 12;
        int by = winY + 11;
        UiText.drawBrand(g, "SkySens", bx, by, UiText.Weight.DISPLAY);
        UiText.draw(g, "v" + version, bx, by + 17, Theme.TEXT_FAINT);

        // nav
        int navX = navX();
        int navW = navW();
        int top = navTop();
        int avail = navHeightAvailable();
        int maxNav = Math.max(0, navTotalHeight() - NAV_GAP - avail);
        navScroll = Mth.clamp(navScroll, 0, maxNav);

        int activeIndex = -1;
        for (int i = 0; i < nav.size(); i++) {
            if (nav.get(i).node() == active) {
                activeIndex = i;
                break;
            }
        }
        if (activeIndex >= 0) {
            float targetY = top + activeIndex * (NAV_ITEM_H + NAV_GAP);
            if (!indicatorSnapped) {
                indicatorY.snap(targetY);
                indicatorSnapped = true;
            } else {
                indicatorY.target(targetY);
            }
        }

        RenderUtil.scissor(g, sx, top, sidebarW, avail);
        if (activeIndex >= 0 && !searching()) {
            int iy = Math.round(indicatorY.value()) - navScroll;
            RenderUtil.roundedRect(g, navX, iy, navW, NAV_ITEM_H, 4, Theme.ACCENT_SOFT);
            RenderUtil.roundedRect(g, navX, iy + 3, 2, NAV_ITEM_H - 6, 1, Theme.ACCENT);
        }
        boolean navHover = RenderUtil.inside(mouseX, mouseY, sx, top, sidebarW, avail);
        for (int i = 0; i < nav.size(); i++) {
            NavEntry e = nav.get(i);
            int iy = navItemY(i);
            if (iy + NAV_ITEM_H < top || iy > top + avail) {
                continue;
            }
            boolean isActive = e.node() == active && !searching();
            boolean hovered = navHover && RenderUtil.inside(mouseX, mouseY, navX, iy, navW, NAV_ITEM_H);
            if (hovered && !isActive) {
                RenderUtil.roundedRect(g, navX, iy, navW, NAV_ITEM_H, 4, Theme.HOVER_OVERLAY);
            }
            int colour = (isActive || hovered) ? Theme.TEXT : Theme.TEXT_MUTED;
            int indent = e.depth() * 10;
            UiText.Weight weight = isActive ? UiText.Weight.SEMIBOLD : UiText.Weight.REGULAR;
            String label = UiText.ellipsize(e.node().name(), navW - 14 - indent, weight);
            UiText.draw(g, label, navX + 8 + indent, iy + (NAV_ITEM_H - UiText.lineHeight()) / 2 + 1, colour,
                    weight);
        }
        RenderUtil.unscissor(g);

        // footer
        String footer = "Changes save automatically";
        if (UiText.width(footer) > sidebarW - 8) {
            footer = "Auto-saved";
        }
        UiText.drawCentered(g, footer, sx + sidebarW / 2, winY + winH - 16, Theme.TEXT_FAINT);
    }

    private void renderHeader(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int hx = vx + Theme.CONTENT_PADDING;
        int maxTextW = Math.max(40, (winX + winW - 12 - searchW - PICKER_SLOT - 12) - hx);

        String title;
        String desc;
        if (searching()) {
            title = "Results for \"" + query.trim() + "\"";
            desc = searchMatchCount + (searchMatchCount == 1 ? " setting" : " settings") + " found";
        } else if (active != null) {
            title = active.name();
            desc = active.description() == null ? "" : active.description();
        } else {
            title = "SkySens";
            desc = "";
        }
        String shownTitle = UiText.ellipsize(title, maxTextW, UiText.Weight.DISPLAY);
        if (!searching() && active == null) {
            UiText.drawBrand(g, shownTitle, hx, winY + 8, UiText.Weight.DISPLAY);
        } else {
            UiText.draw(g, shownTitle, hx, winY + 8, Theme.TEXT, UiText.Weight.DISPLAY);
        }
        if (!desc.isEmpty()) {
            UiText.draw(g, UiText.ellipsize(desc, maxTextW), hx, winY + 8 + 14 + 4, Theme.TEXT_MUTED);
        }
        themePicker.renderButton(g, mouseX, mouseY);
        searchField.render(g, mouseX, mouseY);
        RenderUtil.rect(g, vx, winY + Theme.HEADER_HEIGHT, vw, 1, lineColour());
    }

    /** Colour of structural hairlines (sidebar divider, header divider): hairline in row themes. */
    private static int lineColour() {
        return Theme.style().cards() ? Theme.BORDER : Theme.DIVIDER;
    }

    private int barThumbHeight() {
        return Math.max(20, (int) ((float) vh * vh / Math.max(1, contentHeight)));
    }

    private int barThumbY() {
        int th = barThumbHeight();
        return vy + (int) ((vh - th) * (maxScroll <= 0 ? 0f : scroll / maxScroll));
    }

    private boolean overBar(double mx, double my) {
        return maxScroll > 0 && RenderUtil.inside(mx, my, vx + vw - SCROLLBAR_W - 3, vy, SCROLLBAR_W + 4, vh);
    }

    private void renderScrollbar(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        if (maxScroll <= 0) {
            return;
        }
        int th = barThumbHeight();
        int ty = barThumbY();
        boolean hot = draggingBar || overBar(mouseX, mouseY);
        int colour = hot ? Theme.TEXT_FAINT : Theme.TRACK;
        RenderUtil.roundedRect(g, vx + vw - SCROLLBAR_W - 2, ty, SCROLLBAR_W, th, 1, colour);
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
        double mx = e.x();
        double my = e.y();
        int button = e.button();

        // 1. open overlay takes the click first
        OptionWidget owner = overlayOwner();
        if (owner != null) {
            if (owner.overlayContains(mx, my)) {
                owner.mouseClicked(mx, my, button);
            } else {
                closeAllOverlays();
            }
            return true;
        }

        // 1b. theme picker popover
        if (themePicker.isOpen()) {
            if (themePicker.overlayContains(mx, my)) {
                themePicker.mouseClicked(mx, my, button);
            } else {
                themePicker.close();
            }
            return true;
        }

        // 1c. theme picker button
        if (button == 0 && themePicker.overButton(mx, my)) {
            searchField.setFocused(false);
            closeAllOverlays();
            themePicker.toggle();
            OptionWidget.playClick();
            return true;
        }

        // 2. search field
        if (searchField.mouseClicked(mx, my)) {
            return true;
        }

        // 3. sidebar nav
        int top = navTop();
        if (RenderUtil.inside(mx, my, winX + 1, top, sidebarW, navHeightAvailable())) {
            if (button == 0) {
                for (int i = 0; i < nav.size(); i++) {
                    if (RenderUtil.inside(mx, my, navX(), navItemY(i), navW(), NAV_ITEM_H)) {
                        selectCategory(nav.get(i).node());
                        OptionWidget.playClick();
                        break;
                    }
                }
            }
            return true;
        }

        // 4. scrollbar
        if (button == 0 && overBar(mx, my)) {
            int ty = barThumbY();
            int th = barThumbHeight();
            if (my >= ty && my < ty + th) {
                barGrabOffset = my - ty;
            } else {
                barGrabOffset = th / 2.0;
                dragBarTo(my);
            }
            draggingBar = true;
            return true;
        }

        // 5. content
        if (RenderUtil.inside(mx, my, vx, vy, vw, vh)) {
            for (OptionWidget w : currentWidgets()) {
                if (w.mouseClicked(mx, my, button)) {
                    return true;
                }
            }
            return true;
        }
        return RenderUtil.inside(mx, my, winX, winY, winW, winH);
    }

    private void dragBarTo(double my) {
        int th = barThumbHeight();
        double range = Math.max(1, vh - th);
        double frac = Mth.clamp((my - barGrabOffset - vy) / range, 0.0, 1.0);
        scrollTarget = (float) (frac * maxScroll);
        scroll = scrollTarget;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent e) {
        draggingBar = false;
        for (OptionWidget w : currentWidgets()) {
            w.mouseReleased(e.x(), e.y(), e.button());
        }
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent e, double dragX, double dragY) {
        if (draggingBar) {
            dragBarTo(e.y());
            return true;
        }
        for (OptionWidget w : currentWidgets()) {
            if (w.mouseDragged(e.x(), e.y(), e.button(), dragX, dragY)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        if (themePicker.isOpen()) {
            themePicker.mouseScrolled(mx, my, scrollY);
            return true;
        }
        OptionWidget owner = overlayOwner();
        if (owner != null) {
            if (owner.overlayContains(mx, my)) {
                owner.mouseScrolled(mx, my, scrollY);
            }
            return true;
        }
        if (RenderUtil.inside(mx, my, vx, vy, vw, vh)) {
            for (OptionWidget w : currentWidgets()) {
                if (w.isHovered(mx, my) && w.mouseScrolled(mx, my, scrollY)) {
                    return true;
                }
            }
            scrollTarget = Mth.clamp(scrollTarget - (float) scrollY * 24f, 0f, maxScroll);
            return true;
        }
        if (RenderUtil.inside(mx, my, winX + 1, navTop(), sidebarW, navHeightAvailable())) {
            int maxNav = Math.max(0, navTotalHeight() - NAV_GAP - navHeightAvailable());
            navScroll = Mth.clamp(navScroll - (int) (scrollY * 24), 0, maxNav);
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent e) {
        if (e.hasControlDown() && e.key() == InputConstants.KEY_F) {
            closeAllOverlays();
            searchField.setFocused(true);
            return true;
        }
        if (e.isEscape()) {
            if (themePicker.escape()) {
                return true;
            }
            if (overlayOwner() != null) {
                closeAllOverlays();
                return true;
            }
            if (searchField.isFocused()) {
                searchField.setFocused(false);
                if (!searchField.text().isEmpty()) {
                    searchField.setText("");
                }
                return true;
            }
            onClose();
            return true;
        }
        if (searchField.isFocused() && searchField.keyPressed(e)) {
            return true;
        }
        OptionWidget owner = overlayOwner();
        if (owner != null) {
            return owner.keyPressed(e);
        }
        for (OptionWidget w : currentWidgets()) {
            if (w.keyPressed(e)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean charTyped(CharacterEvent e) {
        if (searchField.charTyped(e)) {
            return true;
        }
        OptionWidget owner = overlayOwner();
        if (owner != null) {
            return owner.charTyped(e);
        }
        for (OptionWidget w : currentWidgets()) {
            if (w.charTyped(e)) {
                return true;
            }
        }
        return false;
    }
}
