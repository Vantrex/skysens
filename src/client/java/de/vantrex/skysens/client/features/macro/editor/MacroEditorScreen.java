package de.vantrex.skysens.client.features.macro.editor;

import com.mojang.blaze3d.platform.InputConstants;
import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.features.macro.MacroService;
import de.vantrex.skysens.client.features.macro.editor.canvas.CanvasView;
import de.vantrex.skysens.client.features.macro.editor.executions.ExecutionsPanel;
import de.vantrex.skysens.client.features.macro.editor.list.MacroListPanel;
import de.vantrex.skysens.client.features.macro.model.Workflow;
import de.vantrex.skysens.client.ui.render.RenderUtil;
import de.vantrex.skysens.client.ui.render.Theme;
import de.vantrex.skysens.client.ui.render.UiScale;
import de.vantrex.skysens.client.ui.render.UiText;
import de.vantrex.skysens.client.ui.widget.OptionWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

/**
 * The macro editor ({@code /skysens macros}, the button in the Macros category, or the "Open Screen" action). View
 * only for now: the workflow list on the left, the canvas in the middle and the Executions tab. Drawn in the virtual
 * screen size of {@link UiScale} like the config screen. Macros never fire while this screen is open
 * ({@code MacroGate} blocks every non-container screen).
 */
public class MacroEditorScreen extends Screen {

    private static final int MARGIN = 12;
    private static final int HEADER_HEIGHT = MacroListPanel.TITLE_HEIGHT;
    private static final int TAB_W = 72;
    private static final int TAB_H = 20;
    private static final int TOGGLE_W = 22;
    private static final int TOGGLE_H = 12;
    private static final int HELP_SIZE = 20;

    private final @Nullable Screen parent;
    private final EditorState state = new EditorState();
    private final MacroListPanel list = new MacroListPanel(this.state, this::toggleActive);
    private final CanvasView canvas = new CanvasView(this.state);
    private final ExecutionsPanel executions = new ExecutionsPanel(this.state, workflow -> this.state.select(workflow));
    private final TutorialOverlay tutorial = new TutorialOverlay(this::anchor, this::tutorialFinished);

    private boolean tutorialChecked;

    // layout, in virtual units
    private int sw;
    private int sh;
    private int winX;
    private int winY;
    private int winW;
    private int winH;
    private int sidebarW;
    private int contentX;
    private int contentW;
    private int bodyY;
    private int bodyH;
    private int nameMaxW;
    private int[] editorTab = new int[4];
    private int[] executionsTab = new int[4];
    private int[] helpButton = new int[4];
    private int[] activeToggle = new int[4];

    public MacroEditorScreen(@Nullable Screen parent) {
        super(Component.literal("SkySens Macro Editor"));
        this.parent = parent;
    }

    /** Opens the editor. Queued, so a chat screen that is just being closed doesn't close it again. */
    public static void open() {
        final Minecraft client = Minecraft.getInstance();
        client.schedule(() -> {
            if (client.gui.screen() instanceof MacroEditorScreen) {
                return;
            }
            final Screen parent = client.gui.screen() instanceof ChatScreen ? null : client.gui.screen();
            client.gui.setScreen(new MacroEditorScreen(parent));
        });
    }

    // ------------------------------------------------------------------ lifecycle

    @Override
    protected void init() {
        UiScale.begin();
        this.layoutAll();
        if (!this.tutorialChecked) {
            this.tutorialChecked = true;
            if (!SkysensConfig.getInstance().macroCategory.macroTutorialSeen) {
                this.tutorial.open();
            }
        }
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
        this.minecraft.gui.setScreen(this.parent);
    }

    @Override
    public void removed() {
        this.canvas.setSpaceDown(false);
        this.canvas.storeViewport();
        if (this.state.viewportDirty()) {
            MacroService.getInstance().commit(this.state.workflows());
            this.state.clearViewportDirty();
        }
        SkysensConfig.getConfig().saveToFile();
        UiScale.end();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        Theme.update();
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(0, 0, this.width, this.height, Theme.BACKDROP);
    }

    private void tutorialFinished() {
        SkysensConfig.getInstance().macroCategory.macroTutorialSeen = true;
        SkysensConfig.getConfig().saveToFile();
    }

    /** Active toggle (list knob and header): flips, then saves the repository and rebuilds the trigger index. */
    private void toggleActive(Workflow workflow) {
        workflow.active = !workflow.active;
        MacroService.getInstance().commit(this.state.workflows());
        OptionWidget.playClick();
    }

    // ------------------------------------------------------------------ layout

    private void layoutAll() {
        this.sw = UiScale.width();
        this.sh = UiScale.height();
        this.winX = MARGIN;
        this.winY = MARGIN;
        this.winW = Math.max(200, this.sw - 2 * MARGIN);
        this.winH = Math.max(150, this.sh - 2 * MARGIN);
        this.sidebarW = Mth.clamp(this.winW / 4, 110, Theme.SIDEBAR_WIDTH);
        final int listX = this.winX + 1;
        this.list.layout(listX, this.winY + 1, this.sidebarW, this.winH - 2);
        this.contentX = listX + this.sidebarW + 1;
        this.contentW = this.winX + this.winW - 1 - this.contentX;
        this.bodyY = this.winY + 1 + HEADER_HEIGHT + 1;
        this.bodyH = this.winY + this.winH - 1 - this.bodyY;

        // header, laid out from the right edge: [Name .... Editor|Executions  ?  (o) Active]
        final int right = this.contentX + this.contentW - 12;
        final int headerMid = this.winY + 1 + HEADER_HEIGHT / 2;
        final int activeLabelW = UiText.width("Active");
        final int activeX = right - activeLabelW - 6 - TOGGLE_W;
        this.activeToggle = new int[]{activeX, headerMid - TOGGLE_H / 2, TOGGLE_W, TOGGLE_H};
        this.helpButton = new int[]{activeX - 12 - HELP_SIZE, headerMid - HELP_SIZE / 2, HELP_SIZE, HELP_SIZE};
        final int tabsX = this.helpButton[0] - 12 - 2 * TAB_W;
        this.editorTab = new int[]{tabsX, headerMid - TAB_H / 2, TAB_W, TAB_H};
        this.executionsTab = new int[]{tabsX + TAB_W, headerMid - TAB_H / 2, TAB_W, TAB_H};
        this.nameMaxW = Math.max(30, tabsX - 12 - (this.contentX + 12));

        if (this.state.tab() == EditorState.Tab.EXECUTIONS) {
            final int execW = Mth.clamp(this.contentW * 2 / 5, 230, 300);
            this.executions.layout(this.contentX, this.bodyY, execW, this.bodyH);
            this.canvas.layout(this.contentX + execW + 1, this.bodyY, this.contentW - execW - 1, this.bodyH);
        } else {
            this.canvas.layout(this.contentX, this.bodyY, this.contentW, this.bodyH);
        }
    }

    private int[] anchor(TutorialOverlay.Anchor anchor) {
        final int[] canvasRect = this.canvas.rect();
        return switch (anchor) {
            case LIST -> this.list.rect();
            case ADD_BUTTON -> this.canvas.addButtonRect();
            case CANVAS -> canvasRect;
            case DETAILS -> {
                final int width = Math.min(220, canvasRect[2]);
                yield new int[]{canvasRect[0] + canvasRect[2] - width, canvasRect[1], width, canvasRect[3]};
            }
            case LOGIC_NODE -> {
                final int[] node = this.canvas.firstLogicNodeRect();
                yield node != null ? node : canvasRect;
            }
            case EXECUTIONS_TAB -> this.executionsTab;
        };
    }

    // ------------------------------------------------------------------ rendering

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int rawMouseX, int rawMouseY, float partialTick) {
        final float f = UiScale.factor();
        final boolean modal = this.tutorial.isOpen();
        final int realMouseX = (int) (rawMouseX / f);
        final int realMouseY = (int) (rawMouseY / f);
        final int mouseX = modal ? -10000 : realMouseX;
        final int mouseY = modal ? -10000 : realMouseY;
        Theme.update();
        this.layoutAll();

        g.pose().pushMatrix();
        g.pose().scale(f, f);

        RenderUtil.shadow(g, this.winX, this.winY, this.winW, this.winH, Theme.WINDOW_RADIUS);
        RenderUtil.roundedRectWithBorder(g, this.winX, this.winY, this.winW, this.winH, Theme.WINDOW_RADIUS, Theme.PANEL, Theme.BORDER);
        // sidebar
        if (Theme.style().sidebarBlock()) {
            RenderUtil.roundedRect(g, this.winX + 1, this.winY + 1, this.sidebarW, this.winH - 2, Math.max(0, Theme.WINDOW_RADIUS - 1), Theme.SIDEBAR,
                    true, false, true, false);
        }
        RenderUtil.rect(g, this.winX + 1 + this.sidebarW, this.winY + 1, 1, this.winH - 2, Theme.DIVIDER);
        this.list.render(g, mouseX, mouseY);

        this.renderHeader(g, mouseX, mouseY);
        RenderUtil.rect(g, this.contentX, this.bodyY - 1, this.contentW, 1, Theme.DIVIDER);

        if (this.state.tab() == EditorState.Tab.EXECUTIONS) {
            this.executions.render(g, mouseX, mouseY);
            RenderUtil.rect(g, this.canvas.rect()[0] - 1, this.bodyY, 1, this.bodyH, Theme.DIVIDER);
        }
        this.canvas.render(g, mouseX, mouseY);

        this.tutorial.render(g, realMouseX, realMouseY, this.sw, this.sh);
        g.pose().popMatrix();
    }

    private void renderHeader(GuiGraphicsExtractor g, int mx, int my) {
        final Workflow workflow = this.state.current();
        final int mid = this.winY + 1 + HEADER_HEIGHT / 2;
        final String name = workflow == null ? "Macro editor" : workflow.name;
        UiText.draw(g, UiText.ellipsize(name, this.nameMaxW, UiText.Weight.SEMIBOLD), this.contentX + 12, mid - 4, Theme.TEXT, UiText.Weight.SEMIBOLD);

        // Editor | Executions
        final int tabsX = this.editorTab[0];
        RenderUtil.roundedRect(g, tabsX, this.editorTab[1], 2 * TAB_W, TAB_H, 6, Theme.INPUT);
        final boolean executionsOpen = this.state.tab() == EditorState.Tab.EXECUTIONS;
        final int[] activeTab = executionsOpen ? this.executionsTab : this.editorTab;
        RenderUtil.roundedRect(g, activeTab[0] + 2, activeTab[1] + 2, TAB_W - 4, TAB_H - 4, 5, Theme.ACCENT_SOFT);
        this.tabLabel(g, this.editorTab, "Editor", !executionsOpen, mx, my);
        this.tabLabel(g, this.executionsTab, "Executions", executionsOpen, mx, my);

        // help
        final boolean helpHover = RenderUtil.inside(mx, my, this.helpButton[0], this.helpButton[1], HELP_SIZE, HELP_SIZE);
        RenderUtil.roundedRect(g, this.helpButton[0], this.helpButton[1], HELP_SIZE, HELP_SIZE, HELP_SIZE / 2, helpHover ? Theme.CARD_HOVER : Theme.CARD);
        UiText.drawCentered(g, "?", this.helpButton[0] + HELP_SIZE / 2, this.helpButton[1] + (HELP_SIZE - 9) / 2, Theme.TEXT_MUTED, UiText.Weight.SEMIBOLD);

        // active toggle
        final boolean on = workflow != null && workflow.active;
        final int tx = this.activeToggle[0];
        final int ty = this.activeToggle[1];
        RenderUtil.roundedRect(g, tx, ty, TOGGLE_W, TOGGLE_H, TOGGLE_H / 2, on ? Theme.ACCENT : Theme.TRACK);
        final int r = (TOGGLE_H - 4) / 2;
        RenderUtil.knob(g, on ? tx + TOGGLE_W - 2 - r : tx + 2 + r, ty + TOGGLE_H / 2, r, on ? 1f : 0f);
        UiText.draw(g, "Active", tx + TOGGLE_W + 6, mid - 4, workflow == null ? Theme.TEXT_FAINT : on ? Theme.TEXT : Theme.TEXT_MUTED);
    }

    private void tabLabel(GuiGraphicsExtractor g, int[] rect, String label, boolean selected, int mx, int my) {
        final boolean hover = RenderUtil.inside(mx, my, rect[0], rect[1], rect[2], rect[3]);
        UiText.drawCentered(g, label, rect[0] + rect[2] / 2, rect[1] + (rect[3] - 9) / 2,
                selected ? Theme.TEXT : hover ? Theme.TEXT : Theme.TEXT_MUTED,
                selected ? UiText.Weight.SEMIBOLD : UiText.Weight.REGULAR);
    }

    // ------------------------------------------------------------------ input

    private boolean headerClicked(double mx, double my) {
        if (RenderUtil.inside(mx, my, this.editorTab[0], this.editorTab[1], TAB_W, TAB_H)) {
            this.setTab(EditorState.Tab.EDITOR);
            return true;
        }
        if (RenderUtil.inside(mx, my, this.executionsTab[0], this.executionsTab[1], TAB_W, TAB_H)) {
            this.setTab(EditorState.Tab.EXECUTIONS);
            return true;
        }
        if (RenderUtil.inside(mx, my, this.helpButton[0], this.helpButton[1], HELP_SIZE, HELP_SIZE)) {
            OptionWidget.playClick();
            this.tutorial.open();
            return true;
        }
        final Workflow workflow = this.state.current();
        if (workflow != null && RenderUtil.inside(mx, my, this.activeToggle[0] - 2, this.activeToggle[1] - 4,
                TOGGLE_W + 8 + UiText.width("Active"), TOGGLE_H + 8)) {
            this.toggleActive(workflow);
            return true;
        }
        return false;
    }

    private void setTab(EditorState.Tab tab) {
        if (this.state.tab() != tab) {
            OptionWidget.playClick();
            this.state.setTab(tab);
            this.layoutAll();
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
        final float f = UiScale.factor();
        final double mx = e.x() / f;
        final double my = e.y() / f;
        if (this.tutorial.isOpen()) {
            return this.tutorial.mouseClicked(mx, my, e.button());
        }
        if (e.button() == 0 && this.headerClicked(mx, my)) {
            return true;
        }
        if (this.list.mouseClicked(mx, my, e.button())) {
            return true;
        }
        if (this.state.tab() == EditorState.Tab.EXECUTIONS && this.executions.mouseClicked(mx, my, e.button())) {
            return true;
        }
        if (this.canvas.mouseClicked(mx, my, e.button(), e.hasShiftDown() || e.hasControlDown())) {
            return true;
        }
        return super.mouseClicked(e, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent e, double dragX, double dragY) {
        if (this.tutorial.isOpen()) {
            return true;
        }
        final float f = UiScale.factor();
        return this.canvas.mouseDragged(e.x() / f, e.y() / f) || super.mouseDragged(e, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent e) {
        this.canvas.mouseReleased();
        return super.mouseReleased(e);
    }

    @Override
    public boolean mouseScrolled(double rawX, double rawY, double scrollX, double scrollY) {
        if (this.tutorial.isOpen()) {
            return true;
        }
        final float f = UiScale.factor();
        final double mx = rawX / f;
        final double my = rawY / f;
        if (this.list.mouseScrolled(mx, my, scrollY)) {
            return true;
        }
        if (this.state.tab() == EditorState.Tab.EXECUTIONS && this.executions.mouseScrolled(mx, my, scrollY)) {
            return true;
        }
        return this.canvas.mouseScrolled(mx, my, scrollX, scrollY, controlDown(), shiftDown())
                || super.mouseScrolled(rawX, rawY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent e) {
        if (this.tutorial.isOpen()) {
            return this.tutorial.keyPressed(e.key());
        }
        if (e.isEscape()) {
            this.onClose();
            return true;
        }
        switch (e.key()) {
            case InputConstants.KEY_SPACE -> {
                this.canvas.setSpaceDown(true);
                return true;
            }
            case InputConstants.KEY_1 -> {
                if (e.hasShiftDown()) {
                    this.canvas.fit();
                    return true;
                }
            }
            case InputConstants.KEY_0 -> {
                this.canvas.resetZoom();
                return true;
            }
            default -> {
            }
        }
        return super.keyPressed(e);
    }

    @Override
    public boolean keyReleased(KeyEvent e) {
        if (e.key() == InputConstants.KEY_SPACE) {
            this.canvas.setSpaceDown(false);
            return true;
        }
        return super.keyReleased(e);
    }

    private static boolean controlDown() {
        final var window = Minecraft.getInstance().getWindow();
        return InputConstants.isKeyDown(window, InputConstants.KEY_LCONTROL) || InputConstants.isKeyDown(window, InputConstants.KEY_RCONTROL);
    }

    private static boolean shiftDown() {
        final var window = Minecraft.getInstance().getWindow();
        return InputConstants.isKeyDown(window, InputConstants.KEY_LSHIFT) || InputConstants.isKeyDown(window, InputConstants.KEY_RSHIFT);
    }

}
