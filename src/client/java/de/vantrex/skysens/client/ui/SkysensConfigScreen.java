package de.vantrex.skysens.client.ui;

import com.mojang.blaze3d.platform.InputConstants;
import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.ui.model.CategoryNode;
import de.vantrex.skysens.client.ui.model.ConfigTree;
import de.vantrex.skysens.client.ui.model.OptionKind;
import de.vantrex.skysens.client.ui.model.OptionNode;
import de.vantrex.skysens.client.ui.render.Animated;
import de.vantrex.skysens.client.ui.render.RenderUtil;
import de.vantrex.skysens.client.ui.render.Theme;
import de.vantrex.skysens.client.ui.render.UiScale;
import de.vantrex.skysens.client.ui.render.UiText;
import de.vantrex.skysens.client.ui.widget.AccordionWidget;
import de.vantrex.skysens.client.ui.widget.ConfirmButtons;
import de.vantrex.skysens.client.ui.widget.OptionWidget;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
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
    private static final int NAV_FOOTER_H = 38;
    /** Height of the "Reset all…" row at the top of the sidebar footer. */
    private static final int RESET_ROW_H = 16;
    /** Row height of a section sub-item under the active category. */
    private static final int NAV_SUB_H = 14;
    /** Space after the last sub-item before the next category. */
    private static final int NAV_GROUP_GAP = 2;
    private static final int NAV_FADE_H = 12;
    /** Offset of the sub-item guide line and text from the nav column's left edge. */
    private static final int NAV_GUIDE_X = 12;
    private static final int NAV_SUB_INDENT = 20;
    /** Height of the expand/collapse-all row at the top of the content. */
    private static final int TOOLBAR_H = 14;
    private static final int SCROLLBAR_W = 3;
    private static final int SEARCH_W = 160;
    private static final int SEARCH_H = 18;
    /** Header space taken by the theme button (button + gap). */
    private static final int PICKER_SLOT = ThemePicker.BUTTON_SIZE + 4;
    /** How long the reset-all confirmation keeps its button disabled. */
    private static final long RESET_ALL_DELAY_MS = 10_000;
    /** How long the footer shows the result of a reset-all. */
    private static final long RESET_NOTE_MS = 3000;
    private static final int DIALOG_W = 260;
    private static final int DIALOG_PAD = 12;
    private static final int DIALOG_BTN_H = 18;
    private static final int DIALOG_BTN_GAP = 8;
    private static final int PROGRESS_H = 2;
    /** How often the "anything to reset?" check re-reads the config. */
    private static final long RESETTABLE_CHECK_MS = 250;

    // remembered across openings
    private static String lastCategoryId;
    private static float lastScroll;

    private final Screen parent;
    private final List<CategoryNode> tree;
    /** Every category in sidebar order. */
    private final List<NavEntry> categoryEntries = new ArrayList<>();
    /** Currently visible sidebar rows: categories plus the section sub-items of the active one. */
    private final List<NavEntry> nav = new ArrayList<>();
    private final Map<String, List<OptionWidget>> categoryWidgets = new HashMap<>();
    private final SearchField searchField;
    private final ThemePicker themePicker = new ThemePicker();
    private final Animated indicatorY = new Animated(0f, 160f);
    /** Grows the active category's sub-item group open (0..1). */
    private final Animated groupAnim = new Animated(1f, 180f);
    private final Animated tickY = new Animated(0f, 140f);
    private final String version;
    /** The whole config tree including the Dev category, built on first use; the source of what reset-all resets. */
    private List<CategoryNode> fullTree;

    private CategoryNode active;
    private String query = "";
    private List<OptionWidget> searchWidgets = new ArrayList<>();
    private int searchMatchCount;
    private int selectedResult;

    // virtual screen size: the screen size divided by the effective scale factor (see UiScale)
    private int sw, sh;

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
    private float navScroll;
    private float navScrollTarget;
    private boolean navReveal = true;
    private boolean navRevealSnap = true;
    private boolean navSearching;
    private boolean indicatorSnapped;
    private CategoryNode tickFor;
    private int[] navOffsets = new int[0];
    private int[] navHeights = new int[0];
    private int navContentH;

    // scroll anchor: keeps the content scrolled to a widget while accordions animate open
    private OptionWidget anchor;
    private int anchorMargin;
    /** Section the sidebar highlights after a click, until the user scrolls manually. */
    private OptionNode pinnedSection;

    // expand/collapse-all row at the top of the content
    private boolean hasToolbar;
    private int toolbarY;
    private final Animated toolbarHover = new Animated(0f, 120f);

    // "Reset all…" footer button and its confirmation dialog
    private final Animated resetAllHover = new Animated(0f, 120f);
    private boolean resetAllOpen;
    private long resetAllOpenedAt;
    private int resetAllCount;
    private int resettableCount;
    private long resettableCheckedAt = -RESETTABLE_CHECK_MS;
    private String resetNote = "";
    private long resetNoteUntil;

    /** A sidebar row: a category, or (when {@code section} is set) a section of the active category. */
    private record NavEntry(CategoryNode node, OptionNode section, int depth) {

        boolean isSection() {
            return section != null;
        }
    }

    public SkysensConfigScreen(Screen parent) {
        this(parent, false);
    }

    /**
     * @param showDev whether to show the Dev category even outside a development environment (it is also shown
     *                when debug mode is already on)
     */
    public SkysensConfigScreen(Screen parent, boolean showDev) {
        super(Component.literal("SkySens"));
        this.parent = parent;
        Theme.loadFromConfig();
        UiText.loadFromConfig();
        this.tree = visibleTree(showDev);
        this.searchField = new SearchField(this::onQueryChanged);
        this.version = FabricLoader.getInstance().getModContainer("skysens")
                .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("dev");
        for (CategoryNode root : tree) {
            addNav(root, 0);
        }
        CategoryNode initial = null;
        if (lastCategoryId != null) {
            for (NavEntry e : categoryEntries) {
                if (e.node().id().equals(lastCategoryId)) {
                    initial = e.node();
                    break;
                }
            }
        }
        if (initial == null && !categoryEntries.isEmpty()) {
            initial = categoryEntries.get(0).node();
        } else if (initial != null) {
            this.scrollTarget = lastScroll;
            this.scroll = lastScroll;
        }
        this.active = initial;
        rebuildNav();
    }

    /** MoulConfig identifier of the Dev category: the name of its field in {@link SkysensConfig}. */
    private static final String DEV_CATEGORY_ID = "devCategory";

    /** The config tree without the Dev category, unless it should be shown. Search uses this same list. */
    private static List<CategoryNode> visibleTree(boolean showDev) {
        List<CategoryNode> all = ConfigTree.build();
        boolean dev = showDev || FabricLoader.getInstance().isDevelopmentEnvironment()
                || SkysensConfig.getInstance().devCategory.debugMode;
        if (dev) {
            return all;
        }
        List<CategoryNode> visible = new ArrayList<>(all);
        visible.removeIf(c -> DEV_CATEGORY_ID.equals(c.id()));
        return visible;
    }

    private void addNav(CategoryNode node, int depth) {
        categoryEntries.add(new NavEntry(node, null, depth));
        for (CategoryNode child : node.children()) {
            addNav(child, depth + 1);
        }
    }

    // ------------------------------------------------------------------ lifecycle

    @Override
    protected void init() {
        UiScale.begin();
        sw = UiScale.width();
        sh = UiScale.height();
        winW = Math.min(sw - 32, 760);
        winH = Math.min(sh - 32, 440);
        winX = (sw - winW) / 2;
        winY = (sh - winH) / 2;
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
                winY + (Theme.HEADER_HEIGHT - ThemePicker.BUTTON_SIZE) / 2, sw, sh, this::init);
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
        UiScale.end();
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

    /** Top-level accordions of the active category, offered as sidebar sub-items when there are at least two. */
    private List<OptionNode> sectionNodes() {
        if (active == null || searching()) {
            return List.of();
        }
        List<OptionNode> sections = new ArrayList<>();
        for (OptionNode n : active.options()) {
            if (n.kind() == OptionKind.ACCORDION) {
                sections.add(n);
            }
        }
        return sections.size() >= 2 ? sections : List.of();
    }

    private void rebuildNav() {
        nav.clear();
        navSearching = searching();
        List<OptionNode> sections = sectionNodes();
        for (NavEntry e : categoryEntries) {
            nav.add(e);
            if (e.node() == active) {
                for (OptionNode section : sections) {
                    nav.add(new NavEntry(e.node(), section, e.depth() + 1));
                }
            }
        }
        layoutNav();
    }

    private void restartGroupAnim() {
        groupAnim.snap(0f);
        groupAnim.target(1f);
        navReveal = true;
    }

    private int activeNavIndex() {
        for (int i = 0; i < nav.size(); i++) {
            if (!nav.get(i).isSection() && nav.get(i).node() == active) {
                return i;
            }
        }
        return -1;
    }

    private void onQueryChanged(String text) {
        closeAllOverlays();
        anchor = null;
        pinnedSection = null;
        selectedResult = 0;
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
        anchor = null;
        pinnedSection = null;
        scrollTarget = 0f;
        if (node != active) {
            active = node;
            scroll = 0f;
            rebuildNav();
            restartGroupAnim();
        }
    }

    /** Opens a top-level section of the active category and scrolls it to the top of the content. */
    private void jumpToSection(OptionNode section) {
        AccordionWidget widget = findAccordion(currentWidgets(), section);
        if (widget == null) {
            return;
        }
        closeAllOverlays();
        widget.setExpanded(true);
        anchor = widget;
        anchorMargin = 0;
        pinnedSection = section;
    }

    /** Leaves the search and scrolls to the given result, opening its accordions and flashing it. */
    private void jumpToResult(OptionWidget result) {
        OptionNode target = result.node();
        searchField.setFocused(false);
        selectCategory(target.category());
        List<OptionWidget> widgets = currentWidgets();
        for (OptionNode p = target.parent(); p != null; p = p.parent()) {
            AccordionWidget accordion = findAccordion(widgets, p);
            if (accordion != null) {
                accordion.setExpanded(true);
            }
        }
        OptionWidget widget = findWidget(widgets, target);
        if (widget != null) {
            widget.flash();
            anchor = widget;
            anchorMargin = vh / 4;
        }
    }

    private static OptionWidget findWidget(List<OptionWidget> widgets, OptionNode node) {
        for (OptionWidget w : widgets) {
            if (w.node() == node) {
                return w;
            }
            if (w instanceof AccordionWidget a) {
                OptionWidget inner = findWidget(a.children(), node);
                if (inner != null) {
                    return inner;
                }
            }
        }
        return null;
    }

    private static AccordionWidget findAccordion(List<OptionWidget> widgets, OptionNode node) {
        return findWidget(widgets, node) instanceof AccordionWidget a ? a : null;
    }

    private static boolean anyAnimating(List<OptionWidget> widgets) {
        for (OptionWidget w : widgets) {
            if (w instanceof AccordionWidget a && a.isAnimating()) {
                return true;
            }
        }
        return false;
    }

    private List<AccordionWidget> topAccordions() {
        List<AccordionWidget> list = new ArrayList<>();
        for (OptionWidget w : currentWidgets()) {
            if (w instanceof AccordionWidget a) {
                list.add(a);
            }
        }
        return list;
    }

    /** Whether the expand/collapse-all row is shown for the current page. */
    private boolean toolbarVisible() {
        return !searching() && topAccordions().size() >= 2;
    }

    private boolean anyCollapsed() {
        for (AccordionWidget a : topAccordions()) {
            if (!a.isExpanded()) {
                return true;
            }
        }
        return false;
    }

    private String toolbarLabel() {
        return anyCollapsed() ? "Expand all" : "Collapse all";
    }

    private int toolbarButtonW() {
        return UiText.width(toolbarLabel()) + 12;
    }

    private boolean overToolbar(double mx, double my) {
        if (!hasToolbar) {
            return false;
        }
        int w = toolbarButtonW();
        return RenderUtil.inside(mx, my, vx + vw - Theme.CONTENT_PADDING - w, toolbarY, w, TOOLBAR_H);
    }

    private void toggleAll() {
        boolean open = anyCollapsed();
        for (AccordionWidget a : topAccordions()) {
            a.setExpanded(open);
        }
        anchor = null;
    }

    /** The section the sidebar marks as current: the pinned one, else the last one above 1/3 of the viewport. */
    private OptionNode currentSection() {
        if (pinnedSection != null) {
            return pinnedSection;
        }
        List<OptionNode> sections = sectionNodes();
        if (sections.isEmpty()) {
            return null;
        }
        int threshold = vy + vh / 3;
        OptionNode current = sections.get(0);
        for (OptionWidget w : currentWidgets()) {
            if (sections.contains(w.node()) && w.top() <= threshold) {
                current = w.node();
            }
        }
        return current;
    }

    /** Scrolls the content just enough to bring the widget into view. */
    private void scrollIntoView(OptionWidget w) {
        float offset = w.top() - (vy + Theme.CONTENT_PADDING - Math.round(scroll));
        float room = vh - 2 * Theme.CONTENT_PADDING;
        if (offset < scrollTarget) {
            scrollTarget = offset;
        } else if (offset + w.height() > scrollTarget + room) {
            scrollTarget = offset + w.height() - room;
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

    // ------------------------------------------------------------------ reset all

    private List<CategoryNode> fullTree() {
        if (fullTree == null) {
            fullTree = ConfigTree.build();
        }
        return fullTree;
    }

    /** Number of options (Dev included) whose value currently differs from the default. */
    private int countResettable() {
        int count = 0;
        for (OptionNode leaf : ConfigTree.allLeaves(fullTree())) {
            if (!leaf.isDefault()) {
                count++;
            }
        }
        return count;
    }

    /** Cached {@link #countResettable()}, refreshed a few times per second so the footer button tracks edits. */
    private int resettableNow() {
        long now = Util.getMillis();
        if (now - resettableCheckedAt >= RESETTABLE_CHECK_MS) {
            resettableCheckedAt = now;
            resettableCount = countResettable();
        }
        return resettableCount;
    }

    private int resetAllX() {
        return winX + 1 + 8;
    }

    private int resetAllY() {
        return navTop() + navHeightAvailable() + 2;
    }

    private int resetAllW() {
        return sidebarW - 16;
    }

    private boolean overResetAll(double mx, double my) {
        return RenderUtil.inside(mx, my, resetAllX(), resetAllY(), resetAllW(), RESET_ROW_H);
    }

    private void openResetAll() {
        searchField.setFocused(false);
        closeAllOverlays();
        draggingBar = false;
        resetAllCount = countResettable();
        resetAllOpenedAt = Util.getMillis();
        resetAllOpen = true;
    }

    private long resetAllRemainingMs() {
        return ConfirmButtons.remainingMs(resetAllOpenedAt, RESET_ALL_DELAY_MS);
    }

    private String resetAllBody() {
        return resetAllCount + (resetAllCount == 1 ? " setting" : " settings")
                + " will go back to " + (resetAllCount == 1 ? "its default" : "their defaults")
                + ". This can't be undone.";
    }

    private int dialogW() {
        return Math.min(DIALOG_W, sw - 32);
    }

    private List<FormattedCharSequence> dialogBodyLines() {
        return UiText.wrap(resetAllBody(), dialogW() - 2 * DIALOG_PAD);
    }

    /** Dialog bounds as {x, y, w, h}, centred over the config window. */
    private int[] dialogBounds() {
        int w = dialogW();
        int lh = UiText.lineHeight();
        int h = DIALOG_PAD + lh + 6 + dialogBodyLines().size() * (lh + 1) + 12 + DIALOG_BTN_H + 3 + PROGRESS_H
                + DIALOG_PAD;
        return new int[]{winX + (winW - w) / 2, winY + (winH - h) / 2, w, h};
    }

    /** Button bounds; index 0 is Cancel, 1 is Reset all. They sit above the progress bar's row. */
    private int[] dialogButton(int[] p, int index) {
        int[] b = ConfirmButtons.button(p, index, DIALOG_PAD, DIALOG_BTN_H, DIALOG_BTN_GAP);
        b[1] -= 3 + PROGRESS_H;
        return b;
    }

    private void renderResetAllDialog(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        RenderUtil.roundedRect(g, winX, winY, winW, winH, Theme.WINDOW_RADIUS, Theme.BACKDROP);
        int[] p = dialogBounds();
        RenderUtil.shadow(g, p[0], p[1], p[2], p[3], 6);
        RenderUtil.roundedRectWithBorder(g, p[0], p[1], p[2], p[3], 6, Theme.POPOVER, Theme.POPOVER_BORDER);
        int lh = UiText.lineHeight();
        int tx = p[0] + DIALOG_PAD;
        int ty = p[1] + DIALOG_PAD;
        UiText.draw(g, "Reset all settings?", tx, ty, Theme.TEXT, UiText.Weight.SEMIBOLD);
        ty += lh + 6;
        for (FormattedCharSequence line : dialogBodyLines()) {
            UiText.drawLine(g, line, tx, ty, Theme.TEXT_MUTED);
            ty += lh + 1;
        }
        long remaining = resetAllRemainingMs();
        ConfirmButtons.drawCancel(g, dialogButton(p, 0), mouseX, mouseY);
        int[] rb = dialogButton(p, 1);
        ConfirmButtons.drawDanger(g, rb, "Reset all", remaining, mouseX, mouseY);
        // thin bar under the button filling over the countdown
        float progress = 1f - remaining / (float) RESET_ALL_DELAY_MS;
        int barY = rb[1] + rb[3] + 3;
        RenderUtil.rect(g, rb[0], barY, rb[2], PROGRESS_H, Theme.TRACK);
        RenderUtil.rect(g, rb[0], barY, Math.round(rb[2] * progress), PROGRESS_H,
                RenderUtil.withAlpha(Theme.DANGER, remaining == 0 ? 1f : 0.7f));
    }

    private boolean resetAllClicked(double mx, double my, int button) {
        int[] p = dialogBounds();
        if (!RenderUtil.inside(mx, my, p[0], p[1], p[2], p[3])) {
            resetAllOpen = false;
            return true;
        }
        if (button != 0) {
            return true;
        }
        int[] cancel = dialogButton(p, 0);
        int[] confirm = dialogButton(p, 1);
        if (RenderUtil.inside(mx, my, cancel[0], cancel[1], cancel[2], cancel[3])) {
            resetAllOpen = false;
            OptionWidget.playClick();
        } else if (RenderUtil.inside(mx, my, confirm[0], confirm[1], confirm[2], confirm[3])
                && resetAllRemainingMs() == 0) {
            resetAllOpen = false;
            resetAllSettings();
            OptionWidget.playClick();
        }
        return true;
    }

    /**
     * Sets every resettable option to its default and refreshes the cached widgets. The GUI preferences (theme,
     * font, scale mode) are plain config fields, not tree options, so they are untouched.
     */
    private void resetAllSettings() {
        int changed = 0;
        for (OptionNode leaf : ConfigTree.allLeaves(fullTree())) {
            if (leaf.resetToDefault()) {
                changed++;
            }
        }
        for (OptionWidget w : searchWidgets) {
            w.notifyReset();
        }
        for (List<OptionWidget> list : categoryWidgets.values()) {
            for (OptionWidget w : list) {
                w.notifyReset();
            }
        }
        SkysensConfig.getConfig().saveToFile();
        resetNote = changed + (changed == 1 ? " setting" : " settings") + " reset";
        resetNoteUntil = Util.getMillis() + RESET_NOTE_MS;
        resettableCheckedAt = -RESETTABLE_CHECK_MS;
    }

    // ------------------------------------------------------------------ layout

    private void layoutContent() {
        int x = vx + Theme.CONTENT_PADDING;
        int w = Math.max(40, vw - 2 * Theme.CONTENT_PADDING);
        int y = vy + Theme.CONTENT_PADDING - Math.round(scroll);
        int total = Theme.CONTENT_PADDING;
        List<OptionWidget> widgets = currentWidgets();
        hasToolbar = toolbarVisible();
        toolbarY = y;
        if (hasToolbar) {
            y += TOOLBAR_H + Theme.CARD_GAP;
            total += TOOLBAR_H + Theme.CARD_GAP;
        }
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

    /**
     * While an anchor is set, re-aims the scroll at it every frame: the target is clamped to the content height
     * of the moment, which keeps growing while accordions animate open. Cleared once everything has settled.
     */
    private void updateAnchor() {
        if (anchor == null) {
            return;
        }
        float offset = anchor.top() - (vy + Theme.CONTENT_PADDING - Math.round(scroll));
        scrollTarget = offset - anchorMargin;
        if (!anyAnimating(currentWidgets())) {
            anchor = null;
        }
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
        updateNavScroll(k);
    }

    private void updateNavScroll(float k) {
        int avail = navHeightAvailable();
        int max = navMaxScroll();
        int ai = activeNavIndex();
        if (navReveal && ai >= 0 && ai < navOffsets.length) {
            // keep the active category and its whole sub-item group in view
            int catTop = navOffsets[ai];
            int groupRows = 0;
            for (int i = ai + 1; i < nav.size() && nav.get(i).isSection(); i++) {
                groupRows++;
            }
            int end = catTop + NAV_ITEM_H + NAV_GAP + groupRows * NAV_SUB_H;
            float t = navScrollTarget;
            if (end - catTop > avail) {
                t = catTop;
            } else if (end > t + avail) {
                t = end - avail;
            }
            if (catTop < t) {
                t = catTop;
            }
            navScrollTarget = t;
            if (!groupAnim.isAnimating()) {
                navReveal = false;
            }
        }
        navScrollTarget = Mth.clamp(navScrollTarget, 0f, max);
        if (navRevealSnap) {
            navScroll = navScrollTarget;
            navRevealSnap = false;
        }
        navScroll += (navScrollTarget - navScroll) * k;
        if (Math.abs(navScrollTarget - navScroll) < 0.05f) {
            navScroll = navScrollTarget;
        }
        navScroll = Mth.clamp(navScroll, 0f, max);
    }

    private int navTop() {
        return winY + NAV_TOP_OFFSET;
    }

    private int navHeightAvailable() {
        return winY + winH - NAV_FOOTER_H - navTop();
    }

    private int navMaxScroll() {
        return Math.max(0, navContentH - NAV_GAP - navHeightAvailable());
    }

    /** Computes each row's offset and height; the sub-item rows scale with {@link #groupAnim}. */
    private void layoutNav() {
        int n = nav.size();
        if (navOffsets.length != n) {
            navOffsets = new int[n];
            navHeights = new int[n];
        }
        float anim = groupAnim.value();
        int y = 0;
        for (int i = 0; i < n; i++) {
            navOffsets[i] = y;
            if (nav.get(i).isSection()) {
                navHeights[i] = Math.round(NAV_SUB_H * anim);
                y += navHeights[i];
                if (i + 1 == n || !nav.get(i + 1).isSection()) {
                    y += Math.round(NAV_GROUP_GAP * anim);
                }
            } else {
                navHeights[i] = NAV_ITEM_H;
                y += NAV_ITEM_H + NAV_GAP;
            }
        }
        navContentH = y;
    }

    private int navItemY(int index) {
        return navTop() + navOffsets[index] - Math.round(navScroll);
    }

    private int navX() {
        return winX + 1 + 6;
    }

    private int navW() {
        return sidebarW - 12;
    }

    // ------------------------------------------------------------------ rendering

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int rawMouseX, int rawMouseY, float partialTick) {
        float f = UiScale.factor();
        int realMouseX = (int) (rawMouseX / f);
        int realMouseY = (int) (rawMouseY / f);
        // while the reset-all dialog is open nothing behind it reacts to the mouse
        int mouseX = resetAllOpen ? -10000 : realMouseX;
        int mouseY = resetAllOpen ? -10000 : realMouseY;
        Theme.update();
        if (navSearching != searching()) {
            rebuildNav();
            if (!searching()) {
                restartGroupAnim();
            }
        }
        layoutNav();
        layoutContent();
        updateAnchor();
        updateScroll();
        layoutContent(); // re-layout with the updated scroll so widgets and hit-tests agree

        // everything below is laid out in virtual units and scaled to the effective GUI scale
        g.pose().pushMatrix();
        g.pose().scale(f, f);

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
        if (hasToolbar) {
            renderToolbar(g, hx, hy);
        }
        for (int i = 0; i < widgets.size(); i++) {
            OptionWidget w = widgets.get(i);
            // visibility culling
            int wy = w.top();
            if (wy + w.height() < vy || wy > vy + vh) {
                continue;
            }
            w.render(g, hx, hy, partialTick);
            if (searching() && searchField.isFocused() && i == selectedResult) {
                int radius = Theme.style().cards() ? Theme.CARD_RADIUS : 0;
                RenderUtil.roundedRing(g, vx + Theme.CONTENT_PADDING, wy, vw - 2 * Theme.CONTENT_PADDING,
                        w.height(), radius, Theme.ACCENT);
            }
        }
        if (widgets.isEmpty()) {
            renderEmptyState(g);
        }
        RenderUtil.unscissor(g);

        renderScrollbar(g, mouseX, mouseY);

        // overlays above everything
        g.pose().popMatrix();
        g.nextStratum();
        g.pose().pushMatrix(); // re-applied per stratum so it holds whether or not nextStratum keeps the pose
        g.pose().scale(f, f);
        for (OptionWidget w : widgets) {
            w.renderOverlay(g, mouseX, mouseY, partialTick);
        }
        themePicker.renderOverlay(g, mouseX, mouseY);
        if (resetAllOpen) {
            renderResetAllDialog(g, realMouseX, realMouseY);
        }
        g.pose().popMatrix();
    }

    /** Small right-aligned "Expand all" / "Collapse all" button above the first section. */
    private void renderToolbar(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        String label = toolbarLabel();
        int w = toolbarButtonW();
        int bx = vx + vw - Theme.CONTENT_PADDING - w;
        toolbarHover.target(overToolbar(mouseX, mouseY) ? 1f : 0f);
        float hover = toolbarHover.value();
        RenderUtil.roundedRect(g, bx, toolbarY, w, TOOLBAR_H, 4, RenderUtil.withAlpha(Theme.HOVER_OVERLAY, hover));
        UiText.draw(g, label, bx + 6, toolbarY + (TOOLBAR_H - UiText.lineHeight()) / 2 + 1,
                RenderUtil.lerpColor(hover, Theme.TEXT_MUTED, Theme.TEXT));
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
        int scrollPx = Math.round(navScroll);

        int activeIndex = activeNavIndex();
        if (activeIndex >= 0 && activeIndex < navOffsets.length) {
            float targetY = top + navOffsets[activeIndex];
            if (!indicatorSnapped) {
                indicatorY.snap(targetY);
                indicatorSnapped = true;
            } else {
                indicatorY.target(targetY);
            }
        }

        OptionNode currentSection = currentSection();
        float anim = groupAnim.value();
        float textAlpha = anim * anim;
        int firstSection = -1;
        int lastSection = -1;
        int currentIndex = -1;

        RenderUtil.scissor(g, sx, top, sidebarW, avail);
        if (activeIndex >= 0 && !searching()) {
            int iy = Math.round(indicatorY.value()) - scrollPx;
            RenderUtil.roundedRect(g, navX, iy, navW, NAV_ITEM_H, 4, Theme.ACCENT_SOFT);
            RenderUtil.roundedRect(g, navX, iy + 3, 2, NAV_ITEM_H - 6, 1, Theme.ACCENT);
        }
        boolean navHover = RenderUtil.inside(mouseX, mouseY, sx, top, sidebarW, avail);
        for (int i = 0; i < nav.size() && i < navOffsets.length; i++) {
            NavEntry e = nav.get(i);
            int iy = navItemY(i);
            int ih = navHeights[i];
            if (e.isSection()) {
                if (firstSection < 0) {
                    firstSection = i;
                }
                lastSection = i;
                if (e.section() == currentSection) {
                    currentIndex = i;
                }
                if (ih < 4 || iy + ih < top || iy > top + avail) {
                    continue;
                }
                boolean current = e.section() == currentSection;
                boolean hovered = navHover && RenderUtil.inside(mouseX, mouseY, navX, iy, navW, ih);
                int colour = current ? Theme.ACCENT : (hovered ? Theme.TEXT : Theme.TEXT_MUTED);
                String label = UiText.ellipsize(e.section().name(), navW - NAV_SUB_INDENT - 10);
                UiText.draw(g, label, navX + NAV_SUB_INDENT, iy + (NAV_SUB_H - UiText.lineHeight()) / 2 + 1,
                        RenderUtil.withAlpha(colour, textAlpha));
                continue;
            }
            if (iy + ih < top || iy > top + avail) {
                continue;
            }
            boolean isActive = e.node() == active && !searching();
            boolean hovered = navHover && RenderUtil.inside(mouseX, mouseY, navX, iy, navW, ih);
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
        if (firstSection >= 0 && anim > 0.5f) {
            // guide line along the sub-item group, with a tick at the current section
            int gx = navX + NAV_GUIDE_X;
            int gy = navItemY(firstSection) + 1;
            int gh = navItemY(lastSection) + navHeights[lastSection] - 1 - gy;
            RenderUtil.rect(g, gx, gy, 1, gh, lineColour());
            if (currentIndex >= 0) {
                float tickTarget = navOffsets[currentIndex] + 3;
                if (tickFor != active) {
                    tickFor = active;
                    tickY.snap(tickTarget);
                } else {
                    tickY.target(tickTarget);
                }
                RenderUtil.roundedRect(g, gx - 1, top + Math.round(tickY.value()) - scrollPx, 3, NAV_SUB_H - 6, 1,
                        Theme.ACCENT);
            }
        }
        RenderUtil.unscissor(g);

        // fades hint that the list continues above / below
        int max = navMaxScroll();
        if (max > 0) {
            int fadeColour = (Theme.style().sidebarBlock() ? Theme.SIDEBAR : Theme.PANEL) | 0xFF000000;
            int clear = fadeColour & 0x00FFFFFF;
            if (scrollPx > 0) {
                g.fillGradient(sx, top, sx + sidebarW, top + NAV_FADE_H, fadeColour, clear);
            }
            if (scrollPx < max) {
                g.fillGradient(sx, top + avail - NAV_FADE_H, sx + sidebarW, top + avail, clear, fadeColour);
            }
            int th = Math.max(14, (int) ((float) avail * avail / Math.max(1, navContentH - NAV_GAP)));
            int ty = top + (int) ((avail - th) * (scrollPx / (float) max));
            boolean hot = RenderUtil.inside(mouseX, mouseY, sx + sidebarW - SCROLLBAR_W - 4, top, SCROLLBAR_W + 4,
                    avail);
            RenderUtil.roundedRect(g, sx + sidebarW - SCROLLBAR_W - 2, ty, SCROLLBAR_W, th, 1,
                    hot ? Theme.TEXT_FAINT : Theme.TRACK);
        }

        // footer, separated from the list by a hairline: quiet "Reset all…" above the auto-save note
        int lineY = top + avail;
        RenderUtil.rect(g, sx + 8, lineY, sidebarW - 16, 1, lineColour());
        boolean canReset = resettableNow() > 0;
        resetAllHover.target(canReset && overResetAll(mouseX, mouseY) ? 1f : 0f);
        int resetColour = canReset
                ? RenderUtil.lerpColor(resetAllHover.value(), Theme.TEXT_FAINT, Theme.DANGER)
                : RenderUtil.withAlpha(Theme.TEXT_FAINT, 0.5f);
        UiText.drawCentered(g, "Reset all…", sx + sidebarW / 2,
                resetAllY() + (RESET_ROW_H - UiText.lineHeight()) / 2, resetColour);
        String footer = "Changes save automatically";
        int footerColour = Theme.TEXT_FAINT;
        if (Util.getMillis() < resetNoteUntil) {
            footer = resetNote;
            footerColour = Theme.TEXT_MUTED;
        } else if (UiText.width(footer) > sidebarW - 8) {
            footer = "Auto-saved";
        }
        UiText.drawCentered(g, UiText.ellipsize(footer, sidebarW - 8), sx + sidebarW / 2,
                resetAllY() + RESET_ROW_H + (NAV_FOOTER_H - 2 - RESET_ROW_H - UiText.lineHeight()) / 2, footerColour);
    }

    private void renderHeader(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int hx = vx + Theme.CONTENT_PADDING;
        int maxTextW = Math.max(40, (winX + winW - 12 - searchW - PICKER_SLOT - 12) - hx);

        String title;
        String desc;
        if (searching()) {
            title = "Results for \"" + query.trim() + "\"";
            desc = searchMatchCount + (searchMatchCount == 1 ? " setting" : " settings") + " found";
            if (searchMatchCount > 0 && searchField.isFocused()) {
                desc += " · Enter to jump";
            }
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
        double mx = e.x() / UiScale.factor();
        double my = e.y() / UiScale.factor();
        int button = e.button();

        // 0. the reset-all dialog is modal
        if (resetAllOpen) {
            return resetAllClicked(mx, my, button);
        }

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

        // 1d. reset-all button in the sidebar footer
        if (button == 0 && overResetAll(mx, my)) {
            if (resettableNow() > 0) {
                openResetAll();
                OptionWidget.playClick();
            }
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
                for (int i = 0; i < nav.size() && i < navOffsets.length; i++) {
                    if (RenderUtil.inside(mx, my, navX(), navItemY(i), navW(), navHeights[i])) {
                        NavEntry entry = nav.get(i);
                        if (entry.isSection()) {
                            jumpToSection(entry.section());
                        } else {
                            selectCategory(entry.node());
                        }
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
            if (button == 0 && overToolbar(mx, my)) {
                toggleAll();
                OptionWidget.playClick();
                return true;
            }
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
        anchor = null;
        pinnedSection = null;
        int th = barThumbHeight();
        double range = Math.max(1, vh - th);
        double frac = Mth.clamp((my - barGrabOffset - vy) / range, 0.0, 1.0);
        scrollTarget = (float) (frac * maxScroll);
        scroll = scrollTarget;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent e) {
        draggingBar = false;
        if (resetAllOpen) {
            return true;
        }
        double mx = e.x() / UiScale.factor();
        double my = e.y() / UiScale.factor();
        for (OptionWidget w : currentWidgets()) {
            w.mouseReleased(mx, my, e.button());
        }
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent e, double dragX, double dragY) {
        if (resetAllOpen) {
            return true;
        }
        double f = UiScale.factor();
        double mx = e.x() / f;
        double my = e.y() / f;
        if (draggingBar) {
            dragBarTo(my);
            return true;
        }
        for (OptionWidget w : currentWidgets()) {
            if (w.mouseDragged(mx, my, e.button(), dragX / f, dragY / f)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double rawX, double rawY, double scrollX, double scrollY) {
        if (resetAllOpen) {
            return true;
        }
        double mx = rawX / UiScale.factor();
        double my = rawY / UiScale.factor();
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
            anchor = null;
            pinnedSection = null;
            scrollTarget = Mth.clamp(scrollTarget - (float) scrollY * 24f, 0f, maxScroll);
            return true;
        }
        if (RenderUtil.inside(mx, my, winX + 1, navTop(), sidebarW, navHeightAvailable())) {
            navReveal = false;
            navScrollTarget = Mth.clamp(navScrollTarget - (float) scrollY * 24f, 0f, navMaxScroll());
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent e) {
        if (resetAllOpen) {
            if (e.isEscape()) {
                resetAllOpen = false;
            }
            return true;
        }
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
        if (searchField.isFocused() && searching() && !searchWidgets.isEmpty()) {
            int last = searchWidgets.size() - 1;
            switch (e.key()) {
                case InputConstants.KEY_DOWN, InputConstants.KEY_UP -> {
                    selectedResult = Mth.clamp(selectedResult + (e.key() == InputConstants.KEY_DOWN ? 1 : -1), 0,
                            last);
                    anchor = null;
                    scrollIntoView(searchWidgets.get(selectedResult));
                    return true;
                }
                case InputConstants.KEY_RETURN, InputConstants.KEY_NUMPADENTER -> {
                    jumpToResult(searchWidgets.get(Mth.clamp(selectedResult, 0, last)));
                    return true;
                }
                default -> {
                }
            }
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
        if (resetAllOpen) {
            return true;
        }
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
