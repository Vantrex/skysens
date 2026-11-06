package de.vantrex.skysens.client.features.macro.editor;

import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.features.macro.MacroService;
import de.vantrex.skysens.client.features.macro.editor.canvas.CanvasView;
import de.vantrex.skysens.client.features.macro.editor.details.NodeDetailsPanel;
import de.vantrex.skysens.client.features.macro.editor.field.DropdownControl;
import de.vantrex.skysens.client.features.macro.editor.field.FieldUi;
import de.vantrex.skysens.client.features.macro.editor.field.KeybindParamField;
import de.vantrex.skysens.client.features.macro.editor.history.EditorCommand;
import de.vantrex.skysens.client.features.macro.editor.history.WorkflowCommands;
import de.vantrex.skysens.client.features.macro.editor.picker.NodePicker;
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
import net.minecraft.client.input.CharacterEvent;
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
    private static final int HISTORY_W = 44;
    private static final int KEYS_W = 36;

    private final @Nullable Screen parent;
    private final EditorState state = new EditorState();
    private final MacroListPanel list = new MacroListPanel(this.state, this::toggleActive);
    private final CanvasView canvas = new CanvasView(this.state);
    private final NodeDetailsPanel details = new NodeDetailsPanel(this.state);
    private final NodePicker picker = new NodePicker();
    private final ExecutionsPanel executions = new ExecutionsPanel(this.state, workflow -> this.state.select(workflow));
    private final TutorialOverlay tutorial = new TutorialOverlay(this::anchor, this::tutorialFinished);
    private final ShortcutsOverlay shortcuts = new ShortcutsOverlay();

    private boolean tutorialChecked;
    private int lastMouseX;
    private int lastMouseY;

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
    private int[] keysButton = new int[4];
    private int[] undoButton = new int[4];
    private int[] redoButton = new int[4];
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
        this.canvas.setPickerRequester(open -> this.picker.open(open.anchorX(), open.anchorY(), open.needInput(), open.request()));
        this.picker.setOnChoose(this.canvas::place);
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
        DropdownControl.closeActive();
        KeybindParamField.cancelCapture();
        if (this.state.dirty() || this.state.viewportDirty()) {
            this.save(false);
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

    /** Writes the working copy into the repository and rebuilds the trigger index (Ctrl+S and when the screen closes). */
    private void save(boolean announce) {
        MacroService.getInstance().commit(this.state.workflows());
        this.state.clearDirty();
        this.state.clearViewportDirty();
        if (announce) {
            this.state.toast("Saved", false);
        }
    }

    /** Active toggle (list knob and header): flips, then saves the repository and rebuilds the trigger index. */
    private void toggleActive(Workflow workflow) {
        this.state.history().execute(new WorkflowCommands.ToggleActive(workflow));
        MacroService.getInstance().commit(this.state.workflows());
        this.state.clearDirty();
        OptionWidget.playClick();
    }

    /**
     * The key as the keyboard layout labels it. GLFW key codes are US-layout positions, so on QWERTZ the key printed
     * "Z" arrives as KEY_Y (and AZERTY swaps A/Q and Z/W). Letter shortcuts follow the printed letter, like in other
     * apps; non-letter keys keep their code.
     */
    private static int layoutKey(KeyEvent e) {
        final String name = GLFW.glfwGetKeyName(e.key(), e.scancode());
        if (name != null && name.length() == 1) {
            final char c = Character.toLowerCase(name.charAt(0));
            if (c >= 'a' && c <= 'z') {
                return InputConstants.KEY_A + (c - 'a');
            }
        }
        return e.key();
    }

    /** Ctrl+Z / the Undo button (or Ctrl+Y / Ctrl+Shift+Z / Redo). Refused while a drag is running. */
    private void stepHistory(boolean undo) {
        if (!this.canvas.isIdle()) {
            return;
        }
        final EditorCommand command = undo ? this.state.history().undo() : this.state.history().redo();
        if (command == null) {
            this.state.toast(undo ? "Nothing to undo" : "Nothing to redo", false);
            return;
        }
        if (command.commitsImmediately()) {
            MacroService.getInstance().commit(this.state.workflows());
            this.state.clearDirty();
        }
        this.details.blur();
        this.state.toast((undo ? "Undid: " : "Redid: ") + command.label(), false);
        this.layoutAll();
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
        this.keysButton = new int[]{this.helpButton[0] - 6 - KEYS_W, headerMid - TAB_H / 2, KEYS_W, TAB_H};
        final int tabsX = this.keysButton[0] - 12 - 2 * TAB_W;
        this.redoButton = new int[]{tabsX - 12 - HISTORY_W, headerMid - TAB_H / 2, HISTORY_W, TAB_H};
        this.undoButton = new int[]{this.redoButton[0] - 4 - HISTORY_W, headerMid - TAB_H / 2, HISTORY_W, TAB_H};
        this.editorTab = new int[]{tabsX, headerMid - TAB_H / 2, TAB_W, TAB_H};
        this.executionsTab = new int[]{tabsX + TAB_W, headerMid - TAB_H / 2, TAB_W, TAB_H};
        this.nameMaxW = Math.max(30, this.undoButton[0] - 12 - (this.contentX + 12));

        if (this.state.tab() == EditorState.Tab.EXECUTIONS) {
            final int execW = Mth.clamp(this.contentW * 2 / 5, 230, 300);
            this.executions.layout(this.contentX, this.bodyY, execW, this.bodyH);
            this.canvas.layout(this.contentX + execW + 1, this.bodyY, this.contentW - execW - 1, this.bodyH);
        } else {
            this.canvas.layout(this.contentX, this.bodyY, this.contentW, this.bodyH);
        }
        final int[] c = this.canvas.rect();
        this.details.layout(c[0], c[1], c[2], c[3]);
        this.canvas.setRightInset(this.detailsShown() ? this.details.width() : 0);
    }

    /** The tutorial or the shortcut list is open: the editor underneath takes no input. */
    private boolean modalOpen() {
        return this.tutorial.isOpen() || this.shortcuts.isOpen();
    }

    private boolean detailsShown() {
        return this.state.tab() == EditorState.Tab.EDITOR && this.details.isOpen();
    }

    private int[] anchor(TutorialOverlay.Anchor anchor) {
        final int[] canvasRect = this.canvas.rect();
        return switch (anchor) {
            case LIST -> this.list.rect();
            case ADD_BUTTON -> this.canvas.addButtonRect();
            case CANVAS -> canvasRect;
            case DETAILS -> {
                final int width = Math.min(NodeDetailsPanel.WIDTH, canvasRect[2]);
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
        final boolean modal = this.modalOpen();
        final int realMouseX = (int) (rawMouseX / f);
        final int realMouseY = (int) (rawMouseY / f);
        this.lastMouseX = realMouseX;
        this.lastMouseY = realMouseY;
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
        if (this.detailsShown()) {
            this.details.render(g, mouseX, mouseY);
        }
        this.renderToast(g);
        this.list.renderOverlay(g, mouseX, mouseY);
        final DropdownControl dropdown = DropdownControl.active();
        if (dropdown != null) {
            dropdown.renderPopover(g, mouseX, mouseY);
        }
        this.picker.render(g, mouseX, mouseY);

        HoverTip.render(g, this.sw, this.sh);
        this.tutorial.render(g, realMouseX, realMouseY, this.sw, this.sh);
        this.shortcuts.render(g, this.sw, this.sh);
        g.pose().popMatrix();
    }

    private void renderToast(GuiGraphicsExtractor g) {
        final String text = this.state.toastText();
        if (text.isEmpty()) {
            return;
        }
        final float fade = Math.min(1f, (EditorState.TOAST_MS - this.state.toastAge()) / 400f);
        final int tw = UiText.width(text) + 24;
        final int tx = this.contentX + this.contentW / 2 - tw / 2;
        final int ty = this.bodyY + this.bodyH - 36;
        final int accent = this.state.toastIsError() ? Theme.DANGER : Theme.ACCENT;
        RenderUtil.roundedRectWithBorder(g, tx, ty, tw, 24, 6, RenderUtil.withAlpha(Theme.POPOVER | 0xFF000000, fade), RenderUtil.withAlpha(accent, fade));
        UiText.drawCentered(g, text, tx + tw / 2, ty + 8, RenderUtil.withAlpha(Theme.TEXT, fade));
    }

    private void renderHeader(GuiGraphicsExtractor g, int mx, int my) {
        final Workflow workflow = this.state.current();
        final int mid = this.winY + 1 + HEADER_HEIGHT / 2;
        final String name = workflow == null ? "Macro editor" : workflow.name;
        UiText.draw(g, UiText.ellipsize(name, this.nameMaxW, UiText.Weight.SEMIBOLD), this.contentX + 12, mid - 4, Theme.TEXT, UiText.Weight.SEMIBOLD);

        // keybind conflict chip: the trigger keys of this macro that vanilla or another mod also uses
        if (workflow != null) {
            final var conflicts = this.state.validation().conflicts(workflow);
            if (!conflicts.isEmpty()) {
                final String text = conflicts.size() + (conflicts.size() == 1 ? " keybind conflict" : " keybind conflicts");
                final int nameW = Math.min(this.nameMaxW, UiText.width(name, UiText.Weight.SEMIBOLD));
                final int cw = UiText.width(text) + 14;
                final int cx = this.contentX + 12 + nameW + 10;
                if (cx + cw < this.undoButton[0] - 8) {
                    final boolean over = RenderUtil.inside(mx, my, cx, mid - 8, cw, 16);
                    RenderUtil.roundedRect(g, cx, mid - 8, cw, 16, 8, RenderUtil.withAlpha(FieldUi.AMBER, over ? 0.35f : 0.22f));
                    UiText.draw(g, text, cx + 7, mid - 4, FieldUi.AMBER);
                    if (over) {
                        HoverTip.setIssues(conflicts, mx, my);
                    }
                }
            }
        }

        // Undo | Redo
        this.historyButton(g, this.undoButton, "Undo", this.state.history().canUndo(), mx, my);
        this.historyButton(g, this.redoButton, "Redo", this.state.history().canRedo(), mx, my);

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
        // keyboard shortcuts (F1)
        this.historyButton(g, this.keysButton, "Keys", true, mx, my);

        // active toggle
        final boolean on = workflow != null && workflow.active;
        final int tx = this.activeToggle[0];
        final int ty = this.activeToggle[1];
        RenderUtil.roundedRect(g, tx, ty, TOGGLE_W, TOGGLE_H, TOGGLE_H / 2, on ? Theme.ACCENT : Theme.TRACK);
        final int r = (TOGGLE_H - 4) / 2;
        RenderUtil.knob(g, on ? tx + TOGGLE_W - 2 - r : tx + 2 + r, ty + TOGGLE_H / 2, r, on ? 1f : 0f);
        UiText.draw(g, "Active", tx + TOGGLE_W + 6, mid - 4, workflow == null ? Theme.TEXT_FAINT : on ? Theme.TEXT : Theme.TEXT_MUTED);
    }

    private void historyButton(GuiGraphicsExtractor g, int[] rect, String label, boolean enabled, int mx, int my) {
        final boolean hover = enabled && RenderUtil.inside(mx, my, rect[0], rect[1], rect[2], rect[3]);
        RenderUtil.roundedRect(g, rect[0], rect[1], rect[2], rect[3], 5, hover ? Theme.CARD_HOVER : Theme.CARD);
        UiText.drawCentered(g, label, rect[0] + rect[2] / 2, rect[1] + (rect[3] - 9) / 2, enabled ? Theme.TEXT : Theme.TEXT_FAINT);
    }

    private void tabLabel(GuiGraphicsExtractor g, int[] rect, String label, boolean selected, int mx, int my) {
        final boolean hover = RenderUtil.inside(mx, my, rect[0], rect[1], rect[2], rect[3]);
        UiText.drawCentered(g, label, rect[0] + rect[2] / 2, rect[1] + (rect[3] - 9) / 2,
                selected ? Theme.TEXT : hover ? Theme.TEXT : Theme.TEXT_MUTED,
                selected ? UiText.Weight.SEMIBOLD : UiText.Weight.REGULAR);
    }

    // ------------------------------------------------------------------ input

    private boolean headerClicked(double mx, double my) {
        if (RenderUtil.inside(mx, my, this.undoButton[0], this.undoButton[1], HISTORY_W, TAB_H)) {
            if (this.state.history().canUndo()) {
                OptionWidget.playClick();
                this.stepHistory(true);
            }
            return true;
        }
        if (RenderUtil.inside(mx, my, this.redoButton[0], this.redoButton[1], HISTORY_W, TAB_H)) {
            if (this.state.history().canRedo()) {
                OptionWidget.playClick();
                this.stepHistory(false);
            }
            return true;
        }
        if (RenderUtil.inside(mx, my, this.editorTab[0], this.editorTab[1], TAB_W, TAB_H)) {
            this.setTab(EditorState.Tab.EDITOR);
            return true;
        }
        if (RenderUtil.inside(mx, my, this.executionsTab[0], this.executionsTab[1], TAB_W, TAB_H)) {
            this.setTab(EditorState.Tab.EXECUTIONS);
            return true;
        }
        if (RenderUtil.inside(mx, my, this.keysButton[0], this.keysButton[1], KEYS_W, TAB_H)) {
            OptionWidget.playClick();
            this.shortcuts.open();
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
        if (this.shortcuts.isOpen()) {
            this.shortcuts.close();
            return true;
        }
        if (this.tutorial.isOpen()) {
            return this.tutorial.mouseClicked(mx, my, e.button());
        }
        // key capture takes the next mouse button, side buttons included
        if (KeybindParamField.isCapturing()) {
            return KeybindParamField.captureMouse(e);
        }
        final DropdownControl dropdown = DropdownControl.active();
        if (dropdown != null) {
            dropdown.popoverClicked(mx, my);
            return true;
        }
        if (this.picker.isOpen()) {
            return this.picker.mouseClicked(mx, my, e.button());
        }
        if (this.list.menuOpen()) {
            return this.list.menuClicked(mx, my);
        }
        if (e.button() == 0 && this.headerClicked(mx, my)) {
            return true;
        }
        if (this.detailsShown() && this.details.contains(mx, my)) {
            this.list.mouseClicked(-1, -1, 0, false); // commits a running rename
            final boolean handled = this.details.mouseClicked(mx, my, e.button());
            this.layoutAll();
            return handled;
        }
        if (this.list.mouseClicked(mx, my, e.button(), doubleClick)) {
            return true;
        }
        if (this.state.tab() == EditorState.Tab.EXECUTIONS && this.executions.mouseClicked(mx, my, e.button())) {
            return true;
        }
        if (this.canvas.mouseClicked(mx, my, e.button(), e.hasShiftDown() || e.hasControlDown(), doubleClick)) {
            this.details.blur();
            this.layoutAll();
            return true;
        }
        return super.mouseClicked(e, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent e, double dragX, double dragY) {
        if (this.modalOpen()) {
            return true;
        }
        final float f = UiScale.factor();
        final double mx = e.x() / f;
        final double my = e.y() / f;
        return this.list.mouseDragged(mx, my) || this.canvas.mouseDragged(mx, my) || super.mouseDragged(e, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent e) {
        final float f = UiScale.factor();
        this.list.mouseReleased();
        this.canvas.mouseReleased(e.x() / f, e.y() / f);
        this.layoutAll();
        return super.mouseReleased(e);
    }

    @Override
    public boolean mouseScrolled(double rawX, double rawY, double scrollX, double scrollY) {
        if (this.modalOpen()) {
            return true;
        }
        final float f = UiScale.factor();
        final double mx = rawX / f;
        final double my = rawY / f;
        final DropdownControl dropdown = DropdownControl.active();
        if (dropdown != null) {
            dropdown.mouseScrolled(mx, my, scrollY);
            return true;
        }
        if (this.picker.isOpen()) {
            return this.picker.mouseScrolled(mx, my, scrollY);
        }
        if (this.detailsShown() && this.details.mouseScrolled(mx, my, scrollY)) {
            return true;
        }
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
        if (this.shortcuts.isOpen()) {
            if (e.isEscape() || e.key() == InputConstants.KEY_F1) {
                this.shortcuts.close();
            }
            return true;
        }
        if (e.key() == InputConstants.KEY_F1 && !this.tutorial.isOpen() && !KeybindParamField.isCapturing()) {
            this.shortcuts.open();
            return true;
        }
        if (this.tutorial.isOpen()) {
            return this.tutorial.keyPressed(e.key());
        }
        if (KeybindParamField.isCapturing()) {
            return KeybindParamField.captureKey(e);
        }
        final DropdownControl dropdown = DropdownControl.active();
        if (dropdown != null) {
            return dropdown.keyPressed(e);
        }
        if (this.picker.isOpen()) {
            return this.picker.keyPressed(e);
        }
        if (this.list.menuOpen()) {
            if (e.isEscape()) {
                this.list.closeMenu();
            }
            return true;
        }
        if (this.list.isEditing()) {
            return this.list.keyPressed(e);
        }
        final int key = layoutKey(e);
        if (key == InputConstants.KEY_S && e.hasControlDown()) {
            this.canvas.storeViewport();
            this.save(true);
            return true;
        }
        if (e.hasControlDown() && this.state.tab() == EditorState.Tab.EDITOR) {
            if (key == InputConstants.KEY_Z) {
                this.stepHistory(!e.hasShiftDown());
                return true;
            }
            if (key == InputConstants.KEY_Y) {
                this.stepHistory(false);
                return true;
            }
        }
        if (this.detailsShown() && this.details.isTextFocused()) {
            if (e.isEscape()) {
                this.details.blur();
                return true;
            }
            return this.details.keyPressed(e) || true;
        }
        if (e.isEscape()) {
            if (this.detailsShown()) {
                this.details.close();
                this.layoutAll();
            } else {
                this.onClose();
            }
            return true;
        }
        switch (key) {
            case InputConstants.KEY_TAB -> {
                if (this.state.tab() == EditorState.Tab.EDITOR && this.state.current() != null) {
                    final boolean over = this.canvas.contains(this.lastMouseX, this.lastMouseY);
                    this.canvas.openPicker(this.lastMouseX, this.lastMouseY, over ? new double[]{this.lastMouseX, this.lastMouseY} : null);
                }
                return true;
            }
            case InputConstants.KEY_DELETE, InputConstants.KEY_BACKSPACE -> {
                if (this.state.tab() == EditorState.Tab.EDITOR && this.canvas.deleteSelection()) {
                    this.layoutAll();
                    return true;
                }
            }
            case InputConstants.KEY_RETURN, InputConstants.KEY_NUMPADENTER -> {
                if (this.canvas.openDetailsOfSelection()) {
                    this.layoutAll();
                    return true;
                }
            }
            case InputConstants.KEY_A -> {
                if (e.hasControlDown()) {
                    this.canvas.selectAll();
                    return true;
                }
            }
            case InputConstants.KEY_C -> {
                if (e.hasControlDown() && this.state.tab() == EditorState.Tab.EDITOR && this.canvas.copySelection()) {
                    return true;
                }
            }
            case InputConstants.KEY_X -> {
                if (e.hasControlDown() && this.state.tab() == EditorState.Tab.EDITOR && this.canvas.cutSelection()) {
                    this.layoutAll();
                    return true;
                }
            }
            case InputConstants.KEY_V -> {
                if (e.hasControlDown() && this.state.tab() == EditorState.Tab.EDITOR && this.canvas.paste(this.lastMouseX, this.lastMouseY)) {
                    this.layoutAll();
                    return true;
                }
            }
            case InputConstants.KEY_D -> {
                if (this.state.tab() == EditorState.Tab.EDITOR
                        && (e.hasControlDown() ? this.canvas.duplicateSelection() : this.canvas.toggleDisabledOnSelection())) {
                    return true;
                }
            }
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
        if (KeybindParamField.isCapturing()) {
            return KeybindParamField.captureKeyRelease(e);
        }
        if (e.key() == InputConstants.KEY_SPACE) {
            this.canvas.setSpaceDown(false);
            return true;
        }
        return super.keyReleased(e);
    }

    @Override
    public boolean charTyped(CharacterEvent e) {
        if (this.modalOpen() || KeybindParamField.isCapturing()) {
            return true;
        }
        final DropdownControl dropdown = DropdownControl.active();
        if (dropdown != null) {
            return dropdown.charTyped(e);
        }
        if (this.picker.isOpen()) {
            return this.picker.charTyped(e);
        }
        if (this.list.isEditing()) {
            return this.list.charTyped(e);
        }
        if (this.detailsShown() && this.details.charTyped(e)) {
            return true;
        }
        return super.charTyped(e);
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
