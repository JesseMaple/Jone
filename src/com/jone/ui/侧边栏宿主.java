package com.jone.ui;

import javafx.animation.PauseTransition;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/**
 * 侧边栏宿主：把页面内容、透明感应区、侧边栏三者叠在一起，并负责悬停呼出逻辑。
 *
 * 交互规则（PRD 第三章第二节）：
 * - 鼠标进入左侧 12px 透明感应区并停留 0.2 秒，侧边栏平滑展开；
 * - 鼠标离开侧边栏后短暂延时再收起，避免误触闪烁；
 * - 侧边栏收起时感应区始终存在，因此不会出现「呼不出来」的情况。
 */
public final class 侧边栏宿主 extends StackPane {

    /** 左侧感应区宽度（PRD 要求 10~15px） */
    private static final double 感应区宽度 = 12;

    /** 悬停多少毫秒后展开 */
    private static final double 悬停延时毫秒 = 200;

    /** 离开后多少毫秒收起 */
    private static final double 收起延时毫秒 = 320;

    private final 侧边栏 侧边栏;
    private final Region 感应区 = new Region();
    private final PauseTransition 悬停计时 = new PauseTransition(Duration.millis(悬停延时毫秒));
    private final PauseTransition 收起计时 = new PauseTransition(Duration.millis(收起延时毫秒));

    /**
     * @param 内容   页面内容节点
     * @param 侧边栏 侧边栏实例
     */
    public 侧边栏宿主(Node 内容, 侧边栏 侧边栏) {
        this.侧边栏 = 侧边栏;

        感应区.setPrefWidth(感应区宽度);
        感应区.setMinWidth(感应区宽度);
        感应区.setMaxWidth(感应区宽度);
        感应区.setPickOnBounds(true);
        感应区.setStyle("-fx-background-color: transparent;");
        // 固定 id 便于自动化自检时定位到感应区
        感应区.setId("侧边栏感应区");
        StackPane.setAlignment(感应区, Pos.CENTER_LEFT);

        StackPane.setAlignment(侧边栏, Pos.CENTER_LEFT);

        getChildren().addAll(内容, 侧边栏, 感应区);

        安装交互();
    }

    /** 安装悬停展开、移出收起的完整交互链路 */
    private void 安装交互() {
        悬停计时.setOnFinished(事件 -> 侧边栏.展开());
        收起计时.setOnFinished(事件 -> {
            if (!侧边栏.isHover() && !感应区.isHover()) {
                侧边栏.收起();
            }
        });

        感应区.setOnMouseEntered(事件 -> {
            // 已经展开时不必再计时
            if (!侧边栏.是否展开()) {
                悬停计时.playFromStart();
            }
        });
        感应区.setOnMouseExited(事件 -> 悬停计时.stop());

        侧边栏.setOnMouseEntered(事件 -> 收起计时.stop());
        侧边栏.setOnMouseExited(事件 -> {
            悬停计时.stop();
            收起计时.playFromStart();
        });
    }

    /**
     * 立即展开侧边栏（供自动化截图或快捷键使用）。
     */
    public void 展开侧边栏() {
        悬停计时.stop();
        收起计时.stop();
        侧边栏.展开();
    }

    /**
     * 立即收起侧边栏。
     */
    public void 收起侧边栏() {
        悬停计时.stop();
        收起计时.stop();
        侧边栏.收起();
    }
}
