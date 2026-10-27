package com.jone.ui;

import com.jone.theme.ThemeEngine;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

/**
 * 主页：中央偏上的大时钟 + 下方常用功能入口。
 *
 * 关键实现（PRD 第三章第四节）：
 * - 时钟容器宽度绑定为页面宽度的 33%，高度绑定为页面高度的 30%（Bindings 动态绑定，窗口缩放时实时跟随）；
 * - 时钟字体大小随容器尺寸联动，颜色跟随主题色，并叠加 DropShadow 增强立体感；
 * - 每秒刷新一次；页面被移出场景时自动停止计时器，避免重复换肤后累积无用动画。
 *
 * 《TODO:后续可增加「常用功能的可配置列表」，从 config.json 读取用户自定义入口。》
 */
public final class 主页 extends VBox {

    private static final DateTimeFormatter 时间格式 = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final String 主题色;
    private final String 强调色;
    private final String 文字色;
    private final String 卡片色;

    private final StackPane 时钟容器 = new StackPane();
    private final Label 时钟文字 = new Label();
    private final Timeline 秒表;

    /**
     * @param 主题色   当前主题色
     * @param 强调色   强调色（时钟字体颜色）
     * @param 文字色   正文文字色
     * @param 跳转回调 双击常用功能后的页面跳转回调
     */
    public 主页(String 主题色, String 强调色, String 文字色, Consumer<String> 跳转回调) {
        this.主题色 = 主题色;
        this.强调色 = 强调色;
        this.文字色 = 文字色;
        this.卡片色 = ThemeEngine.混白(主题色, 0.975);

        setAlignment(Pos.TOP_CENTER);
        setStyle("-fx-background-color: transparent;");

        构建时钟();
        构建常用功能(跳转回调);

        // 每秒刷新
        秒表 = new Timeline(new KeyFrame(Duration.seconds(1), 事件 -> 刷新时间()));
        秒表.setCycleCount(Animation.INDEFINITE);
        秒表.play();

        // 页面被移出场景（例如切换页面或重建主题）时停止计时，避免无主计时器继续运行
        sceneProperty().addListener((观察者, 旧场景, 新场景) -> {
            if (新场景 == null) {
                秒表.stop();
            }
        });

        刷新时间();
    }

    // ------------------------------------------------------------------
    // 大时钟
    // ------------------------------------------------------------------

    private void 构建时钟() {
        // 宽度占页面 33%、高度占 30%，随窗口尺寸实时变化
        时钟容器.prefWidthProperty().bind(widthProperty().multiply(0.33));
        时钟容器.prefHeightProperty().bind(heightProperty().multiply(0.30));
        // VBox 默认会把子节点横向拉满，这里锁定为「仅使用首选尺寸」，比例才能成立
        时钟容器.setMaxWidth(Region.USE_PREF_SIZE);
        时钟容器.setMinSize(0, 0);
        时钟容器.setAlignment(Pos.CENTER);

        时钟文字.setStyle("-fx-text-fill: " + 强调色 + "; -fx-font-weight: bold;");
        时钟文字.setAlignment(Pos.CENTER);

        // 字号随容器尺寸联动：取宽高两方向中更小的那个，保证任何时候都不溢出
        DoubleBinding 字号 = Bindings.createDoubleBinding(
                () -> Math.max(18, Math.min(时钟容器.getWidth() / 5.2, 时钟容器.getHeight() / 2.0)),
                时钟容器.widthProperty(), 时钟容器.heightProperty());
        时钟文字.styleProperty().bind(Bindings.createStringBinding(
                () -> "-fx-text-fill: " + 强调色 + "; -fx-font-weight: bold; -fx-font-size: "
                        + String.format("%.1f", 字号.get()) + "px;",
                字号));

        DropShadow 立体阴影 = new DropShadow();
        立体阴影.setColor(Color.web(主题色, 0.38));
        立体阴影.setRadius(14);
        立体阴影.setOffsetY(4);
        时钟文字.setEffect(立体阴影);

        时钟容器.getChildren().add(时钟文字);

        // 顶部留白：让时钟整体位于页面中央偏上
        Region 顶部留白 = new Region();
        顶部留白.prefHeightProperty().bind(heightProperty().multiply(0.13));
        getChildren().addAll(顶部留白, 时钟容器);
    }

    private void 刷新时间() {
        时钟文字.setText(LocalDateTime.now().format(时间格式));
    }

    // ------------------------------------------------------------------
    // 常用功能
    // ------------------------------------------------------------------

    private void 构建常用功能(Consumer<String> 跳转回调) {
        Region 间距 = new Region();
        间距.setPrefHeight(26);

        HBox 功能行 = new HBox(18);
        功能行.setAlignment(Pos.CENTER);
        功能行.getChildren().addAll(
                构建功能卡片("文件格式转换", "双击打开", "converter", 跳转回调),
                构建功能卡片("设置", "双击打开", "settings", 跳转回调)
        );

        getChildren().addAll(间距, 功能行);
    }

    private VBox 构建功能卡片(String 标题, String 提示, String 目标页面, Consumer<String> 跳转回调) {
        Label 标题文字 = new Label(标题);
        标题文字.setStyle("-fx-text-fill: " + 文字色 + "; -fx-font-size: 14px; -fx-font-weight: bold;");

        Label 提示文字 = new Label(提示);
        提示文字.setStyle("-fx-text-fill: " + 文字色 + "; -fx-font-size: 11px; -fx-opacity: 0.65;");

        VBox 卡片 = new VBox(3, 标题文字, 提示文字);
        卡片.setAlignment(Pos.CENTER_LEFT);
        卡片.setPadding(new Insets(12, 24, 12, 24));
        卡片.setCursor(Cursor.HAND);
        卡片.setStyle(卡片样式(false));

        卡片.setOnMouseEntered(事件 -> 卡片.setStyle(卡片样式(true)));
        卡片.setOnMouseExited(事件 -> 卡片.setStyle(卡片样式(false)));
        卡片.setOnMouseClicked(事件 -> {
            if (事件.getClickCount() == 2 && 跳转回调 != null) {
                跳转回调.accept(目标页面);
            }
        });
        return 卡片;
    }

    private String 卡片样式(boolean 悬停) {
        String 底色 = 悬停 ? ThemeEngine.混合(卡片色, 主题色, 0.10) : 卡片色;
        return "-fx-background-color: " + 底色 + ";"
                + "-fx-background-radius: 12;"
                + "-fx-border-color: " + ThemeEngine.侧边栏边框色() + ";"
                + "-fx-border-radius: 12;";
    }

    // ------------------------------------------------------------------
    // 自动化自检
    // ------------------------------------------------------------------

    /**
     * 返回时钟与页面的实测比例，用于自动化验收「33% / 30%」这条硬性要求。
     */
    public String 自检比例() {
        double 页宽 = getWidth();
        double 页高 = getHeight();
        double 钟宽 = 时钟容器.getWidth();
        double 钟高 = 时钟容器.getHeight();
        if (页宽 <= 0 || 页高 <= 0) {
            return "页面尚未完成布局，无法测量";
        }
        return String.format("页面 %.0f×%.0f，时钟 %.0f×%.0f，宽比 %.3f，高比 %.3f",
                页宽, 页高, 钟宽, 钟高, 钟宽 / 页宽, 钟高 / 页高);
    }
}
