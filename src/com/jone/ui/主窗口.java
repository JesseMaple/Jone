package com.jone.ui;

import com.jone.config.ConfigStore;
import com.jone.theme.ThemeEngine;
import com.jone.theme.系统主题监听;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * 主窗口：无边框外壳、标题栏、动态侧边栏与页面容器。
 *
 * 主题是「可重算」的：任何设置变化都先写 config.json，再由 {@link #重新加载主题()} 重新计算颜色、
 * 重建整棵界面树。这样界面上不会残留旧主题的颜色，也不需要为每个控件维护一套换肤代码。
 *
 * 《TODO:后续可加入窗口边缘拖拽缩放热区。》
 */
public final class 主窗口 {

    private static final double 默认宽度 = 1000;
    private static final double 默认高度 = 700;
    private static final double 最小宽度 = 760;
    private static final double 最小高度 = 560;

    private final Stage 舞台;
    private final String 配置文件路径;

    /** 以下颜色在每次重建界面时重新计算 */
    private String 主题色;
    private String 强调色;
    private String 文字色;
    private String 背景色;
    private boolean 跟随系统;
    private boolean 系统深色;

    private BorderPane 根布局;
    private Scene 场景;
    private StackPane 页面容器;
    private 侧边栏 侧边栏;
    private 侧边栏宿主 侧边栏宿主;
    private 主页 当前主页;
    private 转换页 当前转换页;

    /**
     * @param 舞台         主窗口
     * @param 配置文件路径 config.json 路径
     */
    public 主窗口(Stage 舞台, String 配置文件路径) {
        this.舞台 = 舞台;
        this.配置文件路径 = 配置文件路径;
    }

    /**
     * 构建场景（无边框、自绘标题栏、动态侧边栏）。
     */
    public Scene 构建场景() {
        计算颜色();
        String 起始页面 = 读取起始页面();
        根布局 = 组装根布局(起始页面);

        场景 = new Scene(根布局, 读取宽度(), 读取高度());
        舞台.initStyle(StageStyle.UNDECORATED);
        舞台.setTitle("Jone");
        舞台.setScene(场景);
        舞台.setMinWidth(最小宽度);
        舞台.setMinHeight(最小高度);
        舞台.setOnHidden(事件 -> 保存窗口状态());
        return 场景;
    }

    /**
     * 显示窗口（在构建场景之后调用）。
     */
    public void 显示() {
        if (读取布尔("window.maximized")) {
            舞台.setMaximized(true);
        }
        舞台.show();
    }

    /**
     * 主题变化后重建界面：重新计算颜色，替换场景根节点，保留当前所在页面。
     */
    public void 重新加载主题() {
        if (场景 == null) {
            return;
        }
        // 放到下一帧执行，避免在控件自己的事件处理里销毁该控件
        Platform.runLater(() -> {
            String 当前页面 = 侧边栏 != null ? 侧边栏.当前页面() : 读取起始页面();
            计算颜色();
            BorderPane 新根布局 = 组装根布局(当前页面);
            根布局 = 新根布局;
            场景.setRoot(新根布局);
        });
    }

    /** 立即展开侧边栏（供自动化截图使用） */
    public void 展开侧边栏() {
        if (侧边栏宿主 != null) {
            侧边栏宿主.展开侧边栏();
        }
    }

    /** 侧边栏当前是否处于展开状态（供自动化自检使用） */
    public boolean 侧边栏是否展开() {
        return 侧边栏 != null && 侧边栏.是否展开();
    }

    // ------------------------------------------------------------------
    // 界面组装
    // ------------------------------------------------------------------

    /** 按当前颜色与指定页面组装整棵界面树 */
    private BorderPane 组装根布局(String 页面标识) {
        BorderPane 根 = new BorderPane();
        根.setStyle("-fx-background-color: " + 背景色 + ";");
        根.setTop(new 标题栏(舞台, "Jone", 强调色, 背景色));

        页面容器 = new StackPane();
        页面容器.setStyle("-fx-background-color: transparent;");
        页面容器.setAlignment(Pos.CENTER);

        侧边栏 = new 侧边栏(主题色, 背景色, 强调色, 文字色, 系统深色, 页面标识, this::切换页面);
        侧边栏宿主 = new 侧边栏宿主(页面容器, 侧边栏);
        根.setCenter(侧边栏宿主);

        显示页面(页面标识);
        return 根;
    }

    /** 按当前配置重新计算所有颜色 */
    private void 计算颜色() {
        主题色 = ThemeEngine.当前主题色(配置文件路径);
        跟随系统 = ThemeEngine.是否跟随系统(配置文件路径);
        // 只有开启「跟随系统」时才去查询系统深浅色，避免无谓地加载 JNA
        系统深色 = 跟随系统 && 系统主题监听.当前是否深色();
        强调色 = ThemeEngine.强调色(主题色, 跟随系统, 系统深色);
        文字色 = ThemeEngine.文字色(跟随系统, 系统深色);
        背景色 = ThemeEngine.应用背景色(主题色, 跟随系统, 系统深色);
    }

    // ------------------------------------------------------------------
    // 页面切换
    // ------------------------------------------------------------------

    /** 切换页面：先更新界面，再把状态写回配置文件 */
    private void 切换页面(String 页面标识) {
        显示页面(页面标识);
        ConfigStore.写入(配置文件路径, "lastPage", 页面标识);
    }

    /** 把指定页面放进页面容器 */
    private void 显示页面(String 页面标识) {
        if (页面容器 == null) {
            return;
        }
        页面容器.getChildren().setAll(构建页面(页面标识));
    }

    /** 按标识构建页面内容 */
    private Node 构建页面(String 页面标识) {
        当前主页 = null;
        当前转换页 = null;
        return switch (页面标识 == null ? "home" : 页面标识) {
            case "settings" -> new 设置页(配置文件路径, 主题色, 背景色, 强调色, 文字色,
                    系统深色, this::重新加载主题);
            case "converter" -> {
                当前转换页 = new 转换页(舞台, 配置文件路径, 主题色, 强调色, 文字色);
                yield 当前转换页;
            }
            default -> {
                当前主页 = new 主页(主题色, 强调色, 文字色, this::切换页面);
                yield 当前主页;
            }
        };
    }

    /** 主页时钟比例自检（供自动化验收使用） */
    public String 主页时钟自检() {
        return 当前主页 == null ? "当前不在主页" : 当前主页.自检比例();
    }

    /** 切换到指定页面（供自动化自检使用，等同点击侧边栏） */
    public void 打开页面(String 页面标识) {
        if (侧边栏 != null) {
            侧边栏.选中页面(页面标识);
        }
    }

    /**
     * 给转换页注入一个已选文件（供自动化截图使用，不触发转换）。
     */
    public void 注入转换文件(String 路径) {
        if (当前转换页 != null) {
            当前转换页.注入已选文件(路径);
        }
    }

    /** 当前转换页实例（未打开转换页时为 null，供自动化自检使用） */
    public 转换页 取当前转换页() {
        return 当前转换页;
    }

    /**
     * 转换页自检：切到转换页 → 注入文件 → 执行转换，返回结果（供自动化验收使用）。
     */
    public String 转换页自检(String 输入路径, boolean 覆盖) {
        if (当前转换页 == null) {
            切换页面("converter");
        }
        if (当前转换页 == null) {
            return "转换页未就绪";
        }
        当前转换页.注入已选文件(输入路径);
        String 错误 = 当前转换页.执行并显示(覆盖);
        return 错误.isEmpty() ? "转换成功" : 错误;
    }

    /** 尚未实现的页面先用占位内容 */
    private Node 构建占位页(String 标题) {
        Label 占位 = new Label(标题);
        占位.setStyle("-fx-text-fill: " + 文字色 + "; -fx-font-size: 16px;");
        StackPane 容器 = new StackPane(占位);
        容器.setStyle("-fx-background-color: transparent;");
        return 容器;
    }

    /** 读取上次停留的页面，非法值回退主页 */
    private String 读取起始页面() {
        String 页面 = ConfigStore.读取(配置文件路径, "lastPage", "home");
        return switch (页面) {
            case "home", "converter", "settings" -> 页面;
            default -> "home";
        };
    }

    // ------------------------------------------------------------------
    // 窗口状态
    // ------------------------------------------------------------------

    /** 从配置读取窗口宽度，非法值回退默认值 */
    private double 读取宽度() {
        long 宽 = ConfigStore.读取数值(配置文件路径, "window.width", (long) 默认宽度);
        return 宽 >= 最小宽度 ? 宽 : 默认宽度;
    }

    /** 从配置读取窗口高度，非法值回退默认值 */
    private double 读取高度() {
        long 高 = ConfigStore.读取数值(配置文件路径, "window.height", (long) 默认高度);
        return 高 >= 最小高度 ? 高 : 默认高度;
    }

    /** 读取单个布尔配置 */
    private boolean 读取布尔(String 键) {
        return ConfigStore.读取布尔(配置文件路径, 键, false);
    }

    /** 窗口隐藏时把尺寸写回 config.json（先落盘，再结束进程） */
    private void 保存窗口状态() {
        if (舞台.isIconified()) {
            return;
        }
        boolean 最大化 = 舞台.isMaximized();
        ConfigStore.写入(配置文件路径, "window.maximized", 最大化);
        if (!最大化) {
            ConfigStore.写入(配置文件路径, "window.width", (long) Math.round(舞台.getWidth()));
            ConfigStore.写入(配置文件路径, "window.height", (long) Math.round(舞台.getHeight()));
        }
    }
}
