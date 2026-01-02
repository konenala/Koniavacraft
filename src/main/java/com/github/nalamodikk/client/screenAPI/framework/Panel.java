package com.github.nalamodikk.client.screenAPI.framework;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 容器元件。
 * 可以包含多個子元件，並自動處理座標偏移和事件傳遞。
 */
public class Panel extends AbstractWidget {
    protected final List<AbstractWidget> children = new ArrayList<>();

    // 背景設定 (可選)
    private boolean drawBackground = false;
    private int backgroundColor = 0x00000000;

    public Panel(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    public Panel add(AbstractWidget widget) {
        children.add(widget);
        widget.setParent(this);
        return this;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int localMouseX, int localMouseY, int screenMouseX, int screenMouseY) {
        // 1. 畫背景 (如果有)
        if (drawBackground) {
            graphics.fill(0, 0, width, height, backgroundColor);
        }

        // 2. 畫子元件
        // 💡 魔法發生的地方：因為 AbstractWidget.render 已經做了 translate(x, y)
        // 所以這裡我們畫子元件時，只要叫 child.render，它會再次 translate 自己的 x, y
        // 累積起來就是正確的絕對位置！

        for (AbstractWidget child : children) {
            // child.render 會再次 pushPose -> translate -> draw -> popPose
            // 所以這裡傳入的 localMouseX 是相對於 Panel 左上角的
            // 但為了讓 child 的 isMouseOver (使用絕對座標) 正常工作，我們還是傳入 screenMouseX
            // 可是 child.render 內部用的是 translate 之後的坐標系...

            // 修正策略：
            // child.render(graphics, mouseX, mouseY) 參數原本設計是傳 mouseX, mouseY
            // 我們在這裡傳 localMouseX (相對於 Panel) 給它，因為它會再次扣掉自己的 x, y

            // 等等，GuiGraphics 的 translate 只影響渲染位置，不影響 mouseX 數值。
            // 我們的 AbstractWidget.render 邏輯是：
            // translate(x, y);
            // renderWidget(graphics, mouseX - x, mouseY - y, ...)

            // 所以這裡我們只需要把 localMouseX 傳進去，遞迴就會自動扣除偏移
            child.render(graphics, localMouseX, localMouseY);
        }
    }

    @Override
    protected boolean onMouseClicked(int localMouseX, int localMouseY, int button) {
        // 事件傳遞給子元件
        // 從最後加入的開始檢查 (上層覆蓋下層)
        for (int i = children.size() - 1; i >= 0; i--) {
            AbstractWidget child = children.get(i);
            // 這裡我們需要傳入相對於 child 父容器 (也就是此 Panel) 的滑鼠座標
            // 而 localMouseX 已經是相對於此 Panel 的了
            // child.mouseClicked 內部會檢查 isMouseOver (絕對座標)
            // 這有點矛盾。

            // 讓我們統一一下：事件處理通常依賴絕對座標來判斷 isMouseOver，
            // 但邏輯處理依賴相對座標。

            // 為了簡單，我們直接傳遞原始的絕對座標 (透過還原計算)
            // 這裡的 localMouseX 是相對於 Panel 的。
            // 我們需要還原成絕對座標傳給 child.mouseClicked，因為它會再次呼叫 isMouseOver

            int screenX = this.getScreenX();
            int screenY = this.getScreenY();
            int absoluteMouseX = screenX + localMouseX;
            int absoluteMouseY = screenY + localMouseY;

            if (child.mouseClicked(absoluteMouseX, absoluteMouseY, button)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onMouseReleased(int localMouseX, int localMouseY, int button) {
        int screenX = this.getScreenX();
        int screenY = this.getScreenY();
        int absoluteMouseX = screenX + localMouseX;
        int absoluteMouseY = screenY + localMouseY;

        for (AbstractWidget child : children) {
            child.mouseReleased(absoluteMouseX, absoluteMouseY, button);
        }
    }

    /**
     * 收集所有子元件的 Tooltips
     */
    public List<Component> getChildrenTooltip(int screenMouseX, int screenMouseY) {
        for (int i = children.size() - 1; i >= 0; i--) {
            AbstractWidget child = children.get(i);
            if (child.visible && child.isMouseOver(screenMouseX, screenMouseY)) {
                // 如果是 Panel，遞迴查找
                if (child instanceof Panel panel) {
                    List<Component> result = panel.getChildrenTooltip(screenMouseX, screenMouseY);
                    if (!result.isEmpty()) return result;
                }

                List<Component> tooltip = child.getTooltip();
                if (!tooltip.isEmpty()) return tooltip;
            }
        }
        return Collections.emptyList();
    }
}
