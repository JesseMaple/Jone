package com.jone.ui;

import com.jone.config.ConfigStore;
import com.jone.theme.ThemeEngine;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * 设置页：左侧分级菜单（系统设置 → 应用主题），右侧对应的设置面板。
 *
 * 所有修改都遵守「先写 config.json，再刷新界面」的顺序：
 * 点击预设、应用自定义 RGB、切换跟随系统，都会先落盘再回调主窗口重建界面，
 * 因此即使应用立刻崩溃，配置也已经保存。
 *
 * 《TODO:后续可在「系统设置」下增加「窗口行为」「默认格式」等子项。》
 */
public final class 设置页 extends HBox {

    private final String 配置路径;
    private final String 主题色;
    private final String 背景色;
    private final String 强调色;
    private final String 文字色;
    private final boolean 系统深色;
    private final Runnable 应用主题回调;

    /**
     * @param 配置路径      config.json 路径
     * @param 主题色        当前主题色
     * @param 背景色        应用背景色
     * @param 强调色        强调色
     * @param 文字色        正文文字色
     * @param 系统深色      当前系统是否为深色
     * @param 应用主题回调  配置落盘后触发的界面刷新
     */
    public 设置页(String 配置路径, String 主题色, String 背景色, String 强调色,
                  String 文字色, boolean 系统深色, Runnable 应用主题回调) {
        this.配置路径 = 配置路径;
        this.主题色 = 主题色;
        this.背景色 = 背景色;
        this.强调色 = 强调色;
        this.文字色 = 文字色;
        this.系统深色 = 系统深色;
        this.应用主题回调 = 应用主题回调;

        setPadding(new Insets(26, 28, 26, 28));
        setSpacing(26);
        setAlignment(Pos.TOP_LEFT);

        VBox 菜单 = 构建菜单();
        VBox 内容 = 构建主题面板();
        HBox.setHgrow(内容, Priority.ALWAYS);

        getChildren().addAll(菜单, 内容);
    }

    // ------------------------------------------------------------------
    // 左侧分级菜单
    // ------------------------------------------------------------------

    private VBox 构建菜单() {
        VBox 菜单 = new VBox(6);
        菜单.setPrefWidth(172);
        菜单.setMinWidth(172);
        菜单.setMaxWidth(172);

        Label 标题 = new Label("设置");
        标题.setStyle("-fx-text-fill: " + 文字色 + "; -fx-font-size: 18px; -fx-font-weight: bold;");
        VBox.setMargin(标题, new Insets(0, 0, 10, 4));

        Button 系统设置 = 菜单按钮("系统设置  ▾", false);
        VBox 子菜单 = new VBox(2);
        VBox.setMargin(子菜单, new Insets(2, 0, 0, 14));
        Button 应用主题 = 菜单按钮("应用主题", true);
        子菜单.getChildren().add(应用主题);

        系统设置.setOnAction(事件 -> {
            boolean 展开 = 子菜单.isVisible();
            子菜单.setVisible(!展开);
            子菜单.setManaged(!展开);
            系统设置.setText(展开 ? "系统设置  ▸" : "系统设置  ▾");
        });
        应用主题.setOnAction(事件 -> 内容替换(构建主题面板()));

        菜单.getChildren().addAll(标题, 系统设置, 子菜单);
        return 菜单;
    }

    /** 右侧内容容器：面板切换时整块替换 */
    private void 内容替换(VBox 新面板) {
        if (getChildren().size() < 2) {
            return;
        }
        VBox 旧面板 = (VBox) getChildren().get(1);
        HBox.setHgrow(新面板, Priority.ALWAYS);
        getChildren().set(1, 新面板);
        旧面板.getChildren().clear();
    }

    private Button 菜单按钮(String 文本, boolean 选中) {
        Button 按钮 = new Button(文本);
        按钮.setCursor(Cursor.HAND);
        按钮.setMaxWidth(Double.MAX_VALUE);
        按钮.setAlignment(Pos.CENTER_LEFT);
        按钮.setStyle("-fx-background-color: " + (选中 ? ThemeEngine.选中背景色(主题色, 系统深色) : "transparent") + ";"
                + "-fx-background-radius: 8;"
                + "-fx-text-fill: " + (选中 ? ThemeEngine.选中文字色(主题色, 系统深色) : 文字色) + ";"
                + "-fx-font-size: 14px;"
                + "-fx-padding: 8 10 8 10;");
        return 按钮;
    }

    // ------------------------------------------------------------------
    // 应用主题面板
    // ------------------------------------------------------------------

    private VBox 构建主题面板() {
        VBox 面板 = new VBox(16);
        面板.setAlignment(Pos.TOP_LEFT);

        Label 标题 = new Label("应用主题");
        标题.setStyle("-fx-text-fill: " + 文字色 + "; -fx-font-size: 18px; -fx-font-weight: bold;");

        Label 预设说明 = new Label("预设颜色");
        预设说明.setStyle("-fx-text-fill: " + 文字色 + "; -fx-font-size: 13px;");

        FlowPane 预设区 = new FlowPane(16, 12);
        预设区.setPrefWrapLength(560);
        for (String 预设标识 : ThemeEngine.预设标识()) {
            预设区.getChildren().add(构建预设项(预设标识));
        }

        Label 自定义说明 = new Label("自定义颜色（RGB 拾色器）");
        自定义说明.setStyle("-fx-text-fill: " + 文字色 + "; -fx-font-size: 13px;");

        HBox 自定义区 = 构建自定义颜色区();

        Label 跟随说明 = new Label("跟随系统");
        跟随说明.setStyle("-fx-text-fill: " + 文字色 + "; -fx-font-size: 13px;");

        HBox 跟随区 = 构建跟随系统区();

        面板.getChildren().addAll(标题, 预设说明, 预设区, 分隔线(), 自定义说明, 自定义区, 分隔线(), 跟随说明, 跟随区);
        return 面板;
    }

    /** 一个预设选项：色块 + 无边框按钮 */
    private HBox 构建预设项(String 预设标识) {
        String 颜色 = ThemeEngine.预设颜色(预设标识);
        boolean 选中 = !"custom".equals(ConfigStore.读取(配置路径, "theme.mode", "preset"))
                && 预设标识.equals(ConfigStore.读取(配置路径, "theme.presetId", "deepseekBlue"));

        Region 色块 = new Region();
        色块.setPrefSize(16, 16);
        色块.setMinSize(16, 16);
        色块.setMaxSize(16, 16);
        色块.setStyle("-fx-background-color: " + 颜色 + "; -fx-background-radius: 4;"
                + "-fx-border-color: " + ThemeEngine.侧边栏边框色() + "; -fx-border-radius: 4;");

        Button 按钮 = new Button(ThemeEngine.预设名称(预设标识));
        按钮.setCursor(Cursor.HAND);
        按钮.setStyle("-fx-background-color: transparent;"
                + "-fx-text-fill: " + 文字色 + ";"
                + "-fx-font-size: 13px;"
                + "-fx-padding: 2 4 2 0;"
                + (选中 ? "-fx-font-weight: bold;" : ""));
        按钮.setOnAction(事件 -> {
            // 先落盘，再刷新界面
            ConfigStore.写入(配置路径, "theme.mode", "preset");
            ConfigStore.写入(配置路径, "theme.presetId", 预设标识);
            ConfigStore.写入(配置路径, "theme.customHex", 颜色);
            触发界面刷新();
        });

        HBox 项 = new HBox(6, 色块, 按钮);
        项.setAlignment(Pos.CENTER_LEFT);
        项.setPadding(new Insets(4, 8, 4, 8));
        项.setStyle("-fx-background-radius: 8;"
                + (选中 ? "-fx-border-color: " + 强调色 + "; -fx-border-radius: 8;" : ""));
        return 项;
    }

    /** 自定义 RGB 拾色器：三个数字选择器 + 实时预览 + 应用按钮 */
    private HBox 构建自定义颜色区() {
        String 当前自定义 = ConfigStore.读取(配置路径, "theme.customHex", ThemeEngine.默认主题色);
        if (!ThemeEngine.是合法颜色(当前自定义)) {
            当前自定义 = ThemeEngine.默认主题色;
        }
        int 红值 = Integer.parseInt(当前自定义.substring(1, 3), 16);
        int 绿值 = Integer.parseInt(当前自定义.substring(3, 5), 16);
        int 蓝值 = Integer.parseInt(当前自定义.substring(5, 7), 16);

        Spinner<Integer> 红 = 创建数值选择器(红值);
        Spinner<Integer> 绿 = 创建数值选择器(绿值);
        Spinner<Integer> 蓝 = 创建数值选择器(蓝值);

        Region 预览 = new Region();
        预览.setPrefSize(34, 26);
        预览.setMinSize(34, 26);
        预览.setStyle("-fx-background-color: " + 当前自定义 + "; -fx-background-radius: 6;"
                + "-fx-border-color: " + ThemeEngine.侧边栏边框色() + "; -fx-border-radius: 6;");

        Runnable 更新预览 = () -> {
            String 新颜色 = 转十六进制(红.getValue(), 绿.getValue(), 蓝.getValue());
            预览.setStyle("-fx-background-color: " + 新颜色 + "; -fx-background-radius: 6;"
                    + "-fx-border-color: " + ThemeEngine.侧边栏边框色() + "; -fx-border-radius: 6;");
        };
        红.valueProperty().addListener((观察者, 旧值, 新值) -> 更新预览.run());
        绿.valueProperty().addListener((观察者, 旧值, 新值) -> 更新预览.run());
        蓝.valueProperty().addListener((观察者, 旧值, 新值) -> 更新预览.run());

        Button 应用 = new Button("应用自定义颜色");
        应用.setCursor(Cursor.HAND);
        String 应用底色 = ThemeEngine.选中背景色(主题色, 系统深色);
        应用.setStyle("-fx-background-color: " + 应用底色 + ";"
                + "-fx-background-radius: 8;"
                + "-fx-text-fill: " + 强调色 + ";"
                + "-fx-font-size: 13px;"
                + "-fx-padding: 6 14 6 14;");
        应用.setOnAction(事件 -> {
            String 新颜色 = 转十六进制(红.getValue(), 绿.getValue(), 蓝.getValue());
            // 先落盘，再刷新界面
            ConfigStore.写入(配置路径, "theme.mode", "custom");
            ConfigStore.写入(配置路径, "theme.customHex", 新颜色);
            触发界面刷新();
        });

        HBox 行 = new HBox(10,
                标签("R", 文字色), 红,
                标签("G", 文字色), 绿,
                标签("B", 文字色), 蓝,
                预览, 应用);
        行.setAlignment(Pos.CENTER_LEFT);
        return 行;
    }

    /** 跟随系统开关 + 说明 */
    private HBox 构建跟随系统区() {
        boolean 跟随 = ThemeEngine.是否跟随系统(配置路径);

        Button 开关 = new Button(跟随 ? "跟随系统：开" : "跟随系统：关");
        开关.setCursor(Cursor.HAND);
        开关.setStyle("-fx-background-color: " + ThemeEngine.选中背景色(主题色, 系统深色) + ";"
                + "-fx-background-radius: 8;"
                + "-fx-text-fill: " + 强调色 + ";"
                + "-fx-font-size: 13px;"
                + "-fx-padding: 6 14 6 14;");
        开关.setOnAction(事件 -> {
            // 先落盘，再刷新界面
            ConfigStore.写入(配置路径, "theme.followSystem", !跟随);
            触发界面刷新();
        });

        Label 说明 = new Label(系统深色
                ? "当前系统为深色：开启后应用改为极暗背景 + DeepSeek 蓝文字。"
                : "当前系统为浅色：开启后应用改为纯白背景 + 黑色文字。");
        说明.setStyle("-fx-text-fill: " + 文字色 + "; -fx-font-size: 12px;");

        HBox 行 = new HBox(14, 开关, 说明);
        行.setAlignment(Pos.CENTER_LEFT);
        return 行;
    }

    // ------------------------------------------------------------------
    // 组件与工具
    // ------------------------------------------------------------------

    private Spinner<Integer> 创建数值选择器(int 初值) {
        Spinner<Integer> 选择器 = new Spinner<>();
        SpinnerValueFactory.IntegerSpinnerValueFactory 值工厂 =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 255, 初值);
        选择器.setValueFactory(值工厂);
        选择器.setEditable(true);
        选择器.setPrefWidth(76);
        选择器.setStyle("-fx-font-size: 12px;");
        return 选择器;
    }

    private Label 标签(String 文本, String 颜色) {
        Label 标签 = new Label(文本);
        标签.setStyle("-fx-text-fill: " + 颜色 + "; -fx-font-size: 12px;");
        return 标签;
    }

    private Region 分隔线() {
        Region 线 = new Region();
        线.setPrefHeight(1);
        线.setMinHeight(1);
        线.setMaxHeight(1);
        线.setStyle("-fx-background-color: " + ThemeEngine.侧边栏边框色() + ";");
        VBox.setMargin(线, new Insets(2, 0, 2, 0));
        return 线;
    }

    private void 触发界面刷新() {
        if (应用主题回调 != null) {
            应用主题回调.run();
        }
    }

    private String 转十六进制(int 红, int 绿, int 蓝) {
        return String.format("#%02X%02X%02X", 红, 绿, 蓝);
    }
}
