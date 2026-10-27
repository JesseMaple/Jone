package com.jone.ui;

import com.jone.theme.ThemeEngine;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

/**
 * 自绘标题栏：无边框窗口的拖动与窗口控制按钮。
 *
 * 交互约定：
 * - 在标题栏空白处按住拖动可移动窗口；
 * - 双击标题栏切换最大化/还原；
 * - 三枚窗口按钮的标识颜色随主题变化，不使用系统原生边框。
 *
 * 《TODO:后续可增加「按住顶边拖拽改变窗口高度」的缩放热区。》
 */
public final class 标题栏 extends HBox {

    private final Stage 舞台;
    private final String 强调色;
    private final String 悬停底色;

    private double 拖动起点X;
    private double 拖动起点Y;
    private boolean 拖动起点在按钮上;

    /**
     * @param 舞台   所属窗口
     * @param 标题   标题文字
     * @param 强调色 按钮标识与标题文字颜色
     * @param 背景色 窗口背景色（用于计算按钮悬停底色）
     */
    public 标题栏(Stage 舞台, String 标题, String 强调色, String 背景色) {
        this.舞台 = 舞台;
        this.强调色 = 强调色;
        this.悬停底色 = ThemeEngine.混合(背景色, 强调色, 0.14);

        setAlignment(Pos.CENTER_LEFT);
        setPrefHeight(44);
        setMinHeight(44);
        setMaxHeight(44);
        setStyle("-fx-background-color: transparent; -fx-padding: 0 0 0 18;");

        Label 标题文字 = new Label(标题);
        标题文字.setStyle("-fx-text-fill: " + 强调色 + "; -fx-font-size: 14px; -fx-font-weight: bold;");

        Region 弹性空白 = new Region();
        HBox.setHgrow(弹性空白, Priority.ALWAYS);

        HBox 按钮组 = new HBox(2);
        按钮组.setAlignment(Pos.CENTER_RIGHT);
        按钮组.setStyle("-fx-padding: 0 8 0 0;");

        Button 最小化按钮 = 创建按钮("—", "最小化", () -> 舞台.setIconified(true));
        Button 最大化按钮 = 创建按钮("□", "最大化", this::切换最大化);
        Button 关闭按钮 = 创建按钮("✕", "关闭", () -> {
            舞台.close();
            javafx.application.Platform.exit();
        });
        // 固定 id 便于自动化自检定位
        最小化按钮.setId("最小化按钮");
        最大化按钮.setId("最大化按钮");
        关闭按钮.setId("关闭按钮");
        按钮组.getChildren().addAll(最小化按钮, 最大化按钮, 关闭按钮);

        getChildren().addAll(标题文字, 弹性空白, 按钮组);

        安装拖动逻辑(this);
        安装拖动逻辑(标题文字);
        安装拖动逻辑(弹性空白);
    }

    /** 创建一枚无边框窗口按钮 */
    private Button 创建按钮(String 标识, String 提示, Runnable 点击行为) {
        Button 按钮 = new Button(标识);
        按钮.setFocusTraversable(false);
        按钮.setCursor(Cursor.HAND);
        按钮.setPrefSize(40, 28);
        按钮.setMinSize(40, 28);
        按钮.setMaxSize(40, 28);
        按钮.setStyle(按钮样式(false));
        按钮.setOnAction(事件 -> 点击行为.run());
        按钮.setOnMouseEntered(事件 -> 按钮.setStyle(按钮样式(true)));
        按钮.setOnMouseExited(事件 -> 按钮.setStyle(按钮样式(false)));
        // 注意：这里绝不能 consume MOUSE_PRESSED，否则 Button 永远无法进入按下状态、onAction 不会触发。
        // 拖动窗口的防误触改为在拖动逻辑里判断「按下点是否落在按钮上」。
        按钮.setTooltip(new javafx.scene.control.Tooltip(提示));
        return 按钮;
    }

    /** 按钮样式：无边框，悬停时出现一层淡色底 */
    private String 按钮样式(boolean 悬停) {
        return "-fx-background-color: " + (悬停 ? 悬停底色 : "transparent") + ";"
                + "-fx-background-radius: 6;"
                + "-fx-text-fill: " + 强调色 + ";"
                + "-fx-font-size: 13px;"
                + "-fx-padding: 0;";
    }

    /** 切换最大化 / 还原 */
    private void 切换最大化() {
        舞台.setMaximized(!舞台.isMaximized());
    }

    /** 安装「按住拖动窗口」的鼠标处理 */
    private void 安装拖动逻辑(javafx.scene.Node 节点) {
        节点.setOnMousePressed(事件 -> {
            // 在窗口按钮上按下时不启动拖动，避免点按钮却把窗口拖走
            拖动起点在按钮上 = 是否来自按钮(事件.getTarget());
            if (拖动起点在按钮上) {
                return;
            }
            拖动起点X = 事件.getScreenX() - 舞台.getX();
            拖动起点Y = 事件.getScreenY() - 舞台.getY();
        });
        节点.setOnMouseDragged(事件 -> {
            if (拖动起点在按钮上 || 舞台.isMaximized()) {
                return;
            }
            舞台.setX(事件.getScreenX() - 拖动起点X);
            舞台.setY(事件.getScreenY() - 拖动起点Y);
        });
        节点.setOnMouseClicked(事件 -> {
            if (事件.getClickCount() == 2 && !是否来自按钮(事件.getTarget())) {
                切换最大化();
            }
        });
    }

    /** 判断事件目标是否是窗口按钮（或其子节点） */
    private boolean 是否来自按钮(Object 事件目标) {
        if (!(事件目标 instanceof javafx.scene.Node 节点)) {
            return false;
        }
        javafx.scene.Node 当前 = 节点;
        while (当前 != null) {
            if (当前 instanceof Button) {
                return true;
            }
            当前 = 当前.getParent();
        }
        return false;
    }
}
