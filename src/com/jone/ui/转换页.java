package com.jone.ui;

import com.jone.config.ConfigStore;
import com.jone.convert.ConversionService;
import com.jone.convert.FormatCatalog;
import com.jone.theme.ThemeEngine;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.effect.InnerShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * 格式转换页：源格式 / 目标格式选择、文件选择、确认与取消、覆盖确认弹窗。
 *
 * 关键约定（PRD 第三章第五节）：
 * - 左右格式不能相同，选择列表里会屏蔽对方的格式；
 * - 选择文件时按当前源格式过滤后缀；
 * - 「确认」未选文件时深灰禁用，选中文件后变主题色，尺寸 50×30、圆角 15px；
 * - 「取消」始终淡灰，点击清除已选文件；两按钮间距 40px；
 * - 点确认先弹无边框模态窗询问是否覆盖，选「否」时同名文件自动加序号。
 *
 * 《TODO:后续可支持批量选择多个文件并逐个转换。》
 */
public final class 转换页 extends VBox {

    private final Stage 舞台;
    private final String 配置路径;
    private final String 主题色;
    private final String 强调色;
    private final String 文字色;
    private final String 卡片色;

    private String 源格式;
    private String 目标格式;
    private String 已选文件路径 = "";

    private Button 源格式按钮;
    private Button 目标格式按钮;
    private Button 选择文件按钮;
    private Button 确认按钮;
    private Button 取消按钮;
    private Label 文件提示;
    private Label 状态提示;

    /**
     * @param 舞台     主窗口（文件选择器与弹窗的宿主）
     * @param 配置路径 config.json 路径
     * @param 主题色   当前主题色
     * @param 强调色   强调色
     * @param 文字色   正文文字色
     */
    public 转换页(Stage 舞台, String 配置路径, String 主题色, String 强调色, String 文字色) {
        this.舞台 = 舞台;
        this.配置路径 = 配置路径;
        this.主题色 = 主题色;
        this.强调色 = 强调色;
        this.文字色 = 文字色;
        this.卡片色 = ThemeEngine.混白(主题色, 0.975);

        读取格式配置();

        setAlignment(Pos.CENTER);
        setSpacing(20);
        setStyle("-fx-background-color: transparent;");

        构建界面();
        刷新按钮状态();
    }

    // ------------------------------------------------------------------
    // 界面构建
    // ------------------------------------------------------------------

    private void 构建界面() {
        Label 源标签 = 小型标签("源格式");
        Label 目标标签 = 小型标签("目标格式");
        Label 箭头 = 小型标签("→");

        源格式按钮 = 创建格式按钮(() -> 弹出格式菜单(true));
        目标格式按钮 = 创建格式按钮(() -> 弹出格式菜单(false));
        选择文件按钮 = 创建选择文件按钮();

        HBox 控制行 = new HBox(12, 源标签, 源格式按钮, 箭头, 目标标签, 目标格式按钮, 选择文件按钮);
        控制行.setAlignment(Pos.CENTER);

        文件提示 = new Label("尚未选择文件");
        文件提示.setStyle("-fx-text-fill: " + 文字色 + "; -fx-font-size: 12px; -fx-opacity: 0.75;");

        确认按钮 = 创建操作按钮("确认", true);
        取消按钮 = 创建操作按钮("取消", false);
        HBox 按钮行 = new HBox(40, 确认按钮, 取消按钮);
        按钮行.setAlignment(Pos.CENTER);

        状态提示 = new Label(" ");
        状态提示.setStyle("-fx-text-fill: " + 强调色 + "; -fx-font-size: 12px;");

        getChildren().addAll(控制行, 文件提示, 按钮行, 状态提示);
    }

    private Label 小型标签(String 文本) {
        Label 标签 = new Label(文本);
        标签.setStyle("-fx-text-fill: " + 文字色 + "; -fx-font-size: 13px;");
        return 标签;
    }

    /** 凹陷视觉的无边框格式选择按钮 */
    private Button 创建格式按钮(Runnable 点击行为) {
        Button 按钮 = new Button("CSV");
        按钮.setCursor(Cursor.HAND);
        按钮.setMinWidth(96);
        按钮.setStyle("-fx-background-color: " + 卡片色 + ";"
                + "-fx-background-radius: 8;"
                + "-fx-border-color: " + ThemeEngine.侧边栏边框色() + ";"
                + "-fx-border-radius: 8;"
                + "-fx-text-fill: " + 文字色 + ";"
                + "-fx-font-size: 13px;"
                + "-fx-padding: 6 16 6 16;");

        // 内阴影模拟「凹陷」质感
        InnerShadow 凹陷 = new InnerShadow();
        凹陷.setColor(Color.web("#000000", 0.20));
        凹陷.setRadius(5);
        凹陷.setOffsetY(1);
        按钮.setEffect(凹陷);

        按钮.setOnAction(事件 -> 点击行为.run());
        return 按钮;
    }

    private Button 创建选择文件按钮() {
        Button 按钮 = new Button("选择文件");
        按钮.setCursor(Cursor.HAND);
        按钮.setStyle("-fx-background-color: " + ThemeEngine.选中背景色(主题色, false) + ";"
                + "-fx-background-radius: 8;"
                + "-fx-text-fill: " + ThemeEngine.选中文字色(主题色, false) + ";"
                + "-fx-font-size: 13px;"
                + "-fx-padding: 6 16 6 16;");
        按钮.setOnAction(事件 -> 选择文件());
        return 按钮;
    }

    /** 操作按钮：确认 / 取消，尺寸 50×30、圆角 15px */
    private Button 创建操作按钮(String 文本, boolean 是确认) {
        Button 按钮 = new Button(文本);
        按钮.setCursor(Cursor.HAND);
        按钮.setPrefSize(50, 30);
        按钮.setMinSize(50, 30);
        按钮.setMaxSize(50, 30);
        按钮.setStyle(操作按钮样式(是确认, false));
        if (是确认) {
            按钮.setOnAction(事件 -> 执行转换());
        } else {
            按钮.setOnAction(事件 -> 清除已选文件());
        }
        return 按钮;
    }

    private String 操作按钮样式(boolean 是确认, boolean 可用) {
        if (!是确认) {
            // 取消：始终淡灰
            return "-fx-background-color: #E9ECF1; -fx-background-radius: 15;"
                    + "-fx-text-fill: #6B7280; -fx-font-size: 12px;";
        }
        if (!可用) {
            // 未选文件：深灰禁用
            return "-fx-background-color: #B9BEC7; -fx-background-radius: 15;"
                    + "-fx-text-fill: #F6F7F9; -fx-font-size: 12px;";
        }
        String 字色 = ThemeEngine.是亮色(强调色) ? "#1F2430" : "#FFFFFF";
        return "-fx-background-color: " + 强调色 + "; -fx-background-radius: 15;"
                + "-fx-text-fill: " + 字色 + "; -fx-font-size: 12px; -fx-font-weight: bold;";
    }

    // ------------------------------------------------------------------
    // 格式选择
    // ------------------------------------------------------------------

    private void 读取格式配置() {
        源格式 = FormatCatalog.标准格式名(ConfigStore.读取(配置路径, "converter.sourceFormat", "CSV"));
        目标格式 = FormatCatalog.标准格式名(ConfigStore.读取(配置路径, "converter.targetFormat", "JSON"));
        if (源格式.isEmpty()) {
            源格式 = FormatCatalog.CSV;
        }
        if (目标格式.isEmpty() || 目标格式.equals(源格式)) {
            目标格式 = 第一个不同格式(源格式);
        }
    }

    /** 弹出一个只包含「允许的格式」的菜单 */
    private void 弹出格式菜单(boolean 是源格式) {
        String 当前 = 是源格式 ? 源格式 : 目标格式;
        String 对方 = 是源格式 ? 目标格式 : 源格式;

        ContextMenu 菜单 = new ContextMenu();
        菜单.setStyle("-fx-background-color: " + 卡片色 + "; -fx-background-radius: 8;");
        for (String 格式 : FormatCatalog.全部格式()) {
            if (格式.equals(对方)) {
                continue; // 左右格式不能相同
            }
            MenuItem 项 = new MenuItem(格式 + (格式.equals(当前) ? "  ✓" : ""));
            项.setOnAction(事件 -> 设置格式(是源格式, 格式));
            菜单.getItems().add(项);
        }
        Button 触发按钮 = 是源格式 ? 源格式按钮 : 目标格式按钮;
        菜单.show(触发按钮, javafx.geometry.Side.BOTTOM, 0, 4);
    }

    private void 保存格式配置() {
        ConfigStore.写入(配置路径, "converter.sourceFormat", 源格式);
        ConfigStore.写入(配置路径, "converter.targetFormat", 目标格式);
    }

    /**
     * 设置某一侧的格式（供菜单点击与自动化自检使用）。
     * 左右格式不能相同：如果选中的格式与另一侧重复，会把另一侧自动换成一个不同格式。
     *
     * @param 是源格式 true 表示设置源格式，false 表示设置目标格式
     * @param 格式     格式标识（CSV / JSON / YAML）
     */
    public void 设置格式(boolean 是源格式, String 格式) {
        String 标准 = FormatCatalog.标准格式名(格式);
        if (标准.isEmpty()) {
            return;
        }
        if (是源格式) {
            if (标准.equals(目标格式)) {
                目标格式 = 第一个不同格式(标准);
            }
            源格式 = 标准;
        } else {
            if (标准.equals(源格式)) {
                源格式 = 第一个不同格式(标准);
            }
            目标格式 = 标准;
        }
        // 先落盘，再刷新界面
        保存格式配置();
        刷新按钮状态();
        刷新文件提示();
    }

    /** 当前源格式标识 */
    public String 当前源格式() {
        return 源格式;
    }

    /** 当前目标格式标识 */
    public String 当前目标格式() {
        return 目标格式;
    }

    private void 刷新格式按钮() {
        源格式按钮.setText(源格式 + "  ▾");
        目标格式按钮.setText(目标格式 + "  ▾");
    }

    /** 取一个与指定格式不同的格式 */
    private String 第一个不同格式(String 排除格式) {
        for (String 格式 : FormatCatalog.全部格式()) {
            if (!格式.equals(排除格式)) {
                return 格式;
            }
        }
        return 排除格式;
    }

    // ------------------------------------------------------------------
    // 文件选择
    // ------------------------------------------------------------------

    private void 选择文件() {
        FileChooser 选择器 = new FileChooser();
        选择器.setTitle("选择要转换的 " + 源格式 + " 文件");
        选择器.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                FormatCatalog.选择器描述(源格式), toPatterns(FormatCatalog.全部后缀(源格式))));

        Path 起始目录 = Path.of(System.getProperty("user.dir"), "test_data");
        if (Files.isDirectory(起始目录)) {
            选择器.setInitialDirectory(起始目录.toFile());
        }

        File 选中 = 选择器.showOpenDialog(舞台);
        if (选中 == null) {
            return;
        }
        已选文件路径 = 选中.getAbsolutePath();
        状态提示.setText(" ");
        刷新按钮状态();
        刷新文件提示();
    }

    private String[] toPatterns(String[] 后缀) {
        String[] 结果 = new String[后缀.length];
        for (int 序号 = 0; 序号 < 后缀.length; 序号++) {
            结果[序号] = "*." + 后缀[序号];
        }
        return 结果;
    }

    private void 清除已选文件() {
        已选文件路径 = "";
        状态提示.setText(" ");
        刷新按钮状态();
        刷新文件提示();
    }

    /**
     * 刷新「已选择 / 将生成」提示。
     *
     * 之前这里只显示源文件路径，用户很容易误以为目标是同名 .json，
     * 因此把「将生成哪个文件」直接写出来，避免选错目标格式后以为转换没生效。
     */
    private void 刷新文件提示() {
        if (已选文件路径.isBlank()) {
            文件提示.setText("尚未选择文件");
            文件提示.setStyle("-fx-text-fill: " + 文字色 + "; -fx-font-size: 12px; -fx-opacity: 0.75;");
            return;
        }
        String 预计目标 = ConversionService.推测输出路径(已选文件路径, 目标格式);
        文件提示.setStyle("-fx-text-fill: " + 文字色 + "; -fx-font-size: 12px;");
        文件提示.setText("已选择：" + 已选文件路径
                + (预计目标.isEmpty() ? "" : "　→　将生成：" + 预计目标));
    }

    private void 刷新按钮状态() {
        刷新格式按钮();
        boolean 有文件 = !已选文件路径.isBlank();
        确认按钮.setDisable(!有文件);
        确认按钮.setStyle(操作按钮样式(true, 有文件));
    }

    // ------------------------------------------------------------------
    // 转换流程
    // ------------------------------------------------------------------

    private void 执行转换() {
        if (已选文件路径.isBlank()) {
            return;
        }
        String 校验 = ConversionService.校验(源格式, 目标格式);
        if (!校验.isEmpty()) {
            显示错误(校验);
            return;
        }

        // 先问是否覆盖原文件，用户作出选择后再执行转换
        询问是否覆盖(覆盖 -> 执行并显示(覆盖));
    }

    /**
     * 执行转换并把结果写到状态栏。
     *
     * @param 覆盖 是否覆盖已存在的目标文件
     * @return 空字符串表示成功；否则是中文错误信息（成功时状态栏显示最终路径）
     */
    public String 执行并显示(boolean 覆盖) {
        if (已选文件路径.isBlank()) {
            return "尚未选择文件。";
        }
        String 预计输出 = ConversionService.计算实际输出路径(已选文件路径, 目标格式, 覆盖);
        String 错误;
        try {
            错误 = ConversionService.转换(已选文件路径, 预计输出, 覆盖);
        } catch (RuntimeException 异常) {
            // 兜底：任何未预期异常都要暴露到终端，并给用户中文提示，绝不让界面静默失败
            异常.printStackTrace();
            错误 = "转换失败（内部异常）：" + 异常.getClass().getSimpleName()
                    + (异常.getMessage() == null ? "" : " - " + 异常.getMessage());
        }

        if (错误.isEmpty()) {
            状态提示.setStyle("-fx-text-fill: " + 强调色 + "; -fx-font-size: 12px;");
            状态提示.setText("转换成功：" + 预计输出);
        } else {
            显示错误(错误);
        }
        return 错误;
    }

    /**
     * 直接注入一个已选文件（供自动化自检使用，绕开系统文件选择框）。
     */
    public void 注入已选文件(String 路径) {
        已选文件路径 = 路径 == null ? "" : 路径;
        状态提示.setText(" ");
        刷新按钮状态();
        刷新文件提示();
    }

    /** 当前已选文件路径（未选择时为空串） */
    public String 当前已选文件() {
        return 已选文件路径;
    }

    /** 触发「确认」按钮（供自动化自检走真实点击链路） */
    public void 触发确认按钮() {
        确认按钮.fire();
    }

    /** 状态栏当前文字（供自动化自检读取结果） */
    public String 当前状态文本() {
        return 状态提示.getText();
    }

    private void 显示错误(String 信息) {
        状态提示.setStyle("-fx-text-fill: " + ThemeEngine.错误色() + "; -fx-font-size: 12px;");
        状态提示.setText(信息);
    }

    /**
     * 无边框模态弹窗：询问是否覆盖原文件。
     *
     * 这里刻意使用非阻塞的 show() + 回调，而不是 showAndWait()：
     * JavaFX 在动画 / 布局处理过程中调用 showAndWait 会抛
     * IllegalStateException（showAndWait is not allowed during animation or layout processing），
     * 一旦发生在按钮回调里，异常会中断整条转换流程且界面毫无反馈。
     * 非阻塞弹窗同样具备 APPLICATION_MODAL 的输入拦截效果，且不存在该限制。
     *
     * @param 回调 用户选择结果：true = 覆盖，false = 不覆盖（自动加序号）
     */
    private void 询问是否覆盖(Consumer<Boolean> 回调) {
        Stage 弹窗 = new Stage();
        弹窗.initOwner(舞台);
        弹窗.initModality(Modality.APPLICATION_MODAL);
        弹窗.initStyle(StageStyle.TRANSPARENT);

        // 明确告诉用户「原文件」指的是哪个目标文件，避免歧义
        String 目标路径 = ConversionService.计算实际输出路径(已选文件路径, 目标格式, false);

        Label 标题 = new Label("是否覆盖原文件");
        标题.setStyle("-fx-text-fill: " + 文字色 + "; -fx-font-size: 14px; -fx-font-weight: bold;");

        Label 路径提示 = new Label(目标路径);
        路径提示.setWrapText(true);
        路径提示.setMaxWidth(360);
        路径提示.setStyle("-fx-text-fill: " + 文字色 + "; -fx-font-size: 11px; -fx-opacity: 0.75;");

        Label 说明 = new Label("选「否」会在同名文件后自动加序号，不会删除已有文件。");
        说明.setWrapText(true);
        说明.setMaxWidth(360);
        说明.setStyle("-fx-text-fill: " + 文字色 + "; -fx-font-size: 11px; -fx-opacity: 0.75;");

        Button 是按钮 = new Button("是");
        是按钮.setCursor(Cursor.HAND);
        是按钮.setPrefSize(76, 32);
        是按钮.setId("覆盖确认-是");
        是按钮.setStyle("-fx-background-color: " + ThemeEngine.覆盖确认绿() + ";"
                + "-fx-background-radius: 10; -fx-text-fill: #FFFFFF; -fx-font-size: 13px;");

        Button 否按钮 = new Button("否");
        否按钮.setCursor(Cursor.HAND);
        否按钮.setPrefSize(76, 32);
        否按钮.setId("覆盖确认-否");
        否按钮.setStyle("-fx-background-color: " + ThemeEngine.覆盖取消红() + ";"
                + "-fx-background-radius: 10; -fx-text-fill: #000000; -fx-font-size: 13px;");

        boolean[] 已回调 = {false};
        是按钮.setOnAction(事件 -> {
            if (!已回调[0]) {
                已回调[0] = true;
                弹窗.close();
                回调.accept(true);
            }
        });
        否按钮.setOnAction(事件 -> {
            if (!已回调[0]) {
                已回调[0] = true;
                弹窗.close();
                回调.accept(false);
            }
        });
        // 兜底：窗口被意外关闭时按「不覆盖」处理，绝不吞掉这次转换
        弹窗.setOnHidden(事件 -> {
            if (!已回调[0]) {
                已回调[0] = true;
                回调.accept(false);
            }
        });

        HBox 按钮行 = new HBox(18, 是按钮, 否按钮);
        按钮行.setAlignment(Pos.CENTER);

        Region 间距 = new Region();
        间距.setPrefHeight(4);

        VBox 容器 = new VBox(10, 标题, 路径提示, 说明, 间距, 按钮行);
        容器.setAlignment(Pos.CENTER);
        容器.setPadding(new Insets(24, 36, 22, 36));
        容器.setStyle("-fx-background-color: " + ThemeEngine.混白(主题色, 0.97) + ";"
                + "-fx-background-radius: 14;"
                + "-fx-border-color: " + ThemeEngine.侧边栏边框色() + ";"
                + "-fx-border-radius: 14;");

        Scene 场景 = new Scene(容器);
        场景.setFill(Color.TRANSPARENT);
        弹窗.setScene(场景);
        弹窗.setOnShown(事件 -> 居中于主窗口(弹窗));
        // 非阻塞显示：模态只拦截输入，不阻塞调用线程
        弹窗.show();
        居中于主窗口(弹窗);
    }

    /** 把弹窗摆到主窗口正中央 */
    private void 居中于主窗口(Stage 弹窗) {
        if (舞台 == null) {
            return;
        }
        弹窗.setX(舞台.getX() + (舞台.getWidth() - 弹窗.getWidth()) / 2);
        弹窗.setY(舞台.getY() + (舞台.getHeight() - 弹窗.getHeight()) / 2);
    }
}
