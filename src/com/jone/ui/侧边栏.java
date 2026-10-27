package com.jone.ui;

import com.jone.theme.ThemeEngine;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 动态侧边栏。
 *
 * 视觉与交互遵循 PRD 第三章第二节：
 * - 默认收起，靠左侧边缘的透明感应区呼出（感应与延时逻辑在 {@link 侧边栏宿主}）；
 * - 展开用 TranslateTransition 平滑滑入；
 * - 上方放「主页」「文件格式转换」，最下方放「设置」，中间用一条水平实线分隔成上下两块突起；
 * - 选中项上下各有一条短线，线条左右留出间距，不接触侧边栏边框；
 * - 选中背景色按主题明暗动态计算，侧边栏边框始终为淡灰色。
 *
 * 《TODO:后续若要支持侧边栏常驻（固定展开），可在 config.json 增加 sidebarPinned 开关。》
 */
public final class 侧边栏 extends VBox {

    /** 侧边栏展开后的宽度 */
    public static final double 宽度 = 208;

    private static final double 内容内边距 = 14;

    private final String 主题色;
    private final String 强调色;
    private final String 文字色;
    private final String 卡片色;
    private final String 选中背景色;
    private final String 选中文字色;

    private final List<导航项> 全部导航项 = new ArrayList<>();
    private final Consumer<String> 页面切换回调;

    private String 当前页面;
    private final TranslateTransition 展开动画;
    private final TranslateTransition 收起动画;

    /**
     * @param 主题色       当前主题色
     * @param 背景色       应用背景色
     * @param 强调色       强调色（选中线、选中文字）
     * @param 文字色       普通文字色
     * @param 当前页面     初始选中的页面标识
     * @param 页面切换回调 点击导航项后的回调，参数为页面标识
     */
    public 侧边栏(String 主题色, String 背景色, String 强调色, String 文字色,
                  boolean 系统深色, String 当前页面, Consumer<String> 页面切换回调) {
        this.主题色 = 主题色;
        this.强调色 = 强调色;
        this.文字色 = 文字色;
        this.当前页面 = 当前页面;
        this.页面切换回调 = 页面切换回调;
        this.卡片色 = ThemeEngine.混白(主题色, 0.965);
        this.选中背景色 = ThemeEngine.选中背景色(主题色, 系统深色);
        this.选中文字色 = ThemeEngine.选中文字色(主题色, 系统深色);

        setPrefWidth(宽度);
        setMinWidth(宽度);
        setMaxWidth(宽度);
        setSpacing(0);
        setPadding(new Insets(10, 8, 10, 8));

        // 边框：无论什么主题，始终是淡灰色，只画右侧一条
        setStyle("-fx-background-color: " + 背景色 + ";"
                + "-fx-border-color: transparent " + ThemeEngine.侧边栏边框色() + " transparent transparent;"
                + "-fx-border-width: 0 1 0 0;"
                + "-fx-background-radius: 0 14 14 0;"
                + "-fx-border-radius: 0 14 14 0;");

        DropShadow 阴影 = new DropShadow();
        阴影.setRadius(18);
        阴影.setOffsetX(4);
        阴影.setColor(Color.web("#000000", 0.10));
        setEffect(阴影);

        // 初始状态：整体平移到窗口外，完全收起
        setTranslateX(-宽度);
        setVisible(false);

        展开动画 = new TranslateTransition(Duration.millis(220), this);
        展开动画.setToX(0);
        收起动画 = new TranslateTransition(Duration.millis(180), this);
        收起动画.setToX(-宽度);
        收起动画.setOnFinished(事件 -> setVisible(false));

        构建内容();
    }

    /** 组装上下两块区域 */
    private void 构建内容() {
        VBox 上块 = 构建分组();
        上块.getChildren().addAll(
                创建导航项("home", "主页"),
                创建导航项("converter", "文件格式转换")
        );

        // 生成弹性空白，把「设置」压到最下方
        Region 弹性空白 = new Region();
        VBox.setVgrow(弹性空白, Priority.ALWAYS);

        // 水平实线：把侧边栏分为上下两块突起
        Region 分割线 = new Region();
        分割线.setPrefHeight(1);
        分割线.setMinHeight(1);
        分割线.setMaxHeight(1);
        分割线.setStyle("-fx-background-color: " + ThemeEngine.侧边栏边框色() + ";");
        VBox.setMargin(分割线, new Insets(8, 10, 8, 10));

        VBox 下块 = 构建分组();
        下块.getChildren().add(创建导航项("settings", "设置"));

        getChildren().addAll(上块, 弹性空白, 分割线, 下块);
        刷新选中状态();
    }

    /** 一块「突起」卡片：圆角 + 极淡底色 + 轻阴影 */
    private VBox 构建分组() {
        VBox 分组 = new VBox(2);
        分组.setPadding(new Insets(6));
        分组.setStyle("-fx-background-color: " + 卡片色 + ";"
                + "-fx-background-radius: 10;");
        DropShadow 卡片阴影 = new DropShadow();
        卡片阴影.setRadius(6);
        卡片阴影.setOffsetY(1);
        卡片阴影.setColor(Color.web("#000000", 0.06));
        分组.setEffect(卡片阴影);
        return 分组;
    }

    /** 创建一个导航项：上短线 + 按钮 + 下短线 */
    private VBox 创建导航项(String 标识, String 标题) {
        Region 上线 = new Region();
        Region 下线 = new Region();
        设置选中线样式(上线, false);
        设置选中线样式(下线, false);

        Label 文本 = new Label(标题);
        文本.setStyle(文字样式(false));

        StackPane 按钮区 = new StackPane(文本);
        按钮区.setAlignment(Pos.CENTER_LEFT);
        按钮区.setPadding(new Insets(0, 12, 0, 14));
        按钮区.setPrefHeight(38);
        按钮区.setMinHeight(38);
        按钮区.setStyle(按钮样式(false, false));
        按钮区.setCursor(Cursor.HAND);

        按钮区.setOnMouseClicked(事件 -> 选中页面(标识));
        按钮区.setOnMouseEntered(事件 -> {
            if (!标识.equals(当前页面)) {
                按钮区.setStyle(按钮样式(false, true));
            }
        });
        按钮区.setOnMouseExited(事件 -> {
            if (!标识.equals(当前页面)) {
                按钮区.setStyle(按钮样式(false, false));
            }
        });

        VBox 项容器 = new VBox(3, 上线, 按钮区, 下线);
        VBox.setMargin(上线, new Insets(0, 16, 0, 16));
        VBox.setMargin(下线, new Insets(0, 16, 0, 16));

        导航项 项 = new 导航项(标识, 项容器, 上线, 下线, 按钮区, 文本);
        全部导航项.add(项);
        return 项容器;
    }

    /** 选中某个页面（供点击与外部调用） */
    public void 选中页面(String 页面标识) {
        if (页面标识 == null || 页面标识.isBlank()) {
            return;
        }
        当前页面 = 页面标识;
        刷新选中状态();
        if (页面切换回调 != null) {
            页面切换回调.accept(页面标识);
        }
    }

    /** 返回当前选中的页面标识 */
    public String 当前页面() {
        return 当前页面;
    }

    /** 重新计算所有导航项的选中样式 */
    private void 刷新选中状态() {
        for (导航项 项 : 全部导航项) {
            boolean 选中 = 项.标识.equals(当前页面);
            项.按钮区.setStyle(按钮样式(选中, false));
            项.文本.setStyle(文字样式(选中));
            设置选中线样式(项.上线, 选中);
            设置选中线样式(项.下线, 选中);
        }
    }

    /** 展开侧边栏 */
    public void 展开() {
        setVisible(true);
        收起动画.stop();
        展开动画.stop();
        展开动画.playFromStart();
    }

    /** 收起侧边栏 */
    public void 收起() {
        展开动画.stop();
        收起动画.stop();
        收起动画.playFromStart();
    }

    /** 是否处于展开状态（动画结束位置为准） */
    public boolean 是否展开() {
        return isVisible() && getTranslateX() > -宽度 / 2;
    }

    // ------------------------------------------------------------------
    // 样式
    // ------------------------------------------------------------------

    private String 按钮样式(boolean 选中, boolean 悬停) {
        String 底色 = 选中 ? 选中背景色
                : (悬停 ? ThemeEngine.混合(卡片色, 主题色, 0.10) : "transparent");
        return "-fx-background-color: " + 底色 + ";"
                + "-fx-background-radius: 8;";
    }

    private String 文字样式(boolean 选中) {
        return "-fx-text-fill: " + (选中 ? 选中文字色 : 文字色) + ";"
                + "-fx-font-size: 14px;"
                + (选中 ? "-fx-font-weight: bold;" : "");
    }

    private void 设置选中线样式(Region 线, boolean 选中) {
        线.setPrefHeight(选中 ? 1.6 : 0);
        线.setMinHeight(选中 ? 1.6 : 0);
        线.setMaxHeight(选中 ? 1.6 : 0);
        线.setStyle("-fx-background-color: " + (选中 ? 强调色 : "transparent") + ";");
    }

    /** 导航项的内部数据载体 */
    private static final class 导航项 {
        private final String 标识;
        private final VBox 容器;
        private final Region 上线;
        private final Region 下线;
        private final StackPane 按钮区;
        private final Label 文本;

        导航项(String 标识, VBox 容器, Region 上线, Region 下线, StackPane 按钮区, Label 文本) {
            this.标识 = 标识;
            this.容器 = 容器;
            this.上线 = 上线;
            this.下线 = 下线;
            this.按钮区 = 按钮区;
            this.文本 = 文本;
        }
    }
}
