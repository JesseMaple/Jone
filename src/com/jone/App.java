package com.jone;

import com.jone.config.ConfigStore;
import com.jone.theme.ThemeEngine;
import com.jone.theme.系统主题监听;
import com.jone.ui.主窗口;
import com.jone.ui.转换页;
import com.jone.util.ImageExporter;
import com.jone.util.运行环境诊断;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

import java.nio.file.Path;

/**
 * 应用入口：加载配置 → 构建主窗口 → 显示。
 *
 * 调试能力：设置环境变量 JONE_SNAPSHOT_DIR 后，程序启动 1.2 秒会自动把窗口截图保存成 PNG 并退出，
 * 便于在没有桌面截图权限的环境里做视觉验收；JONE_SNAPSHOT_NAME 可指定截图文件名。
 *
 * 《TODO:后续加入全局未捕获异常处理，把异常写进日志文件而不是直接崩溃。》
 */
public class App extends Application {

    @Override
    public void start(Stage 舞台) {
        // PRD 第二章：启动时优先读取 config.json，不存在则创建含默认值的文件
        String 配置路径 = ConfigStore.默认配置路径();
        String 配置错误 = ConfigStore.确保存在(配置路径);
        if (!配置错误.isEmpty()) {
            System.out.println("【错误】" + 配置错误);
        } else {
            System.out.println("【配置】已加载：" + 配置路径);
        }

        主窗口 窗口 = new 主窗口(舞台, 配置路径);
        Scene 场景 = 窗口.构建场景();
        窗口.显示();

        // 系统深浅色变化时，只有在「跟随系统」开启的情况下才重建界面
        系统主题监听.注册(深色 -> {
            if (ThemeEngine.是否跟随系统(配置路径)) {
                窗口.重新加载主题();
            }
        });

        System.out.println("【启动】Jone 窗口已显示，JavaFX 版本 "
                + System.getProperty("javafx.runtime.version", "未知"));

        // 环境自检：从受限终端（低完整性/沙箱）启动时，写入项目外目录会被系统拒绝，
        // 这里提前告知，避免用户误以为是程序拦截。
        if (运行环境诊断.是否为受限环境()) {
            System.out.println("【警告】" + 运行环境诊断.环境说明()
                    + "；如需转换项目目录以外的文件，请改用普通 PowerShell 窗口启动，"
                    + "或双击项目根目录的 启动Jone.cmd。");
        }

        // 自动化截图时可直接展开侧边栏，方便对比收起/展开两种状态
        if ("1".equals(System.getenv("JONE_SNAPSHOT_SIDEBAR"))) {
            窗口.展开侧边栏();
        }

        // 自动化截图时预置一个已选文件，便于截取「确认按钮可用」的状态
        String 预置文件 = System.getenv("JONE_SNAPSHOT_FILE");
        if (预置文件 != null && !预置文件.isBlank()) {
            窗口.注入转换文件(预置文件);
        }

        运行交互自检(场景, 窗口, 舞台, 配置路径);
        触发截图模式(场景);
    }

    /**
     * 交互自检：通过环境变量 JONE_UI_SMOKE 选择要验证的交互。
     * - sidebar：模拟鼠标进入左侧感应区，500 毫秒后检查侧边栏是否展开（验证悬停 0.2 秒规则）；
     * - theme  ：模拟切换主题，验证界面能热重建且不崩溃。
     */
    private void 运行交互自检(Scene 场景, 主窗口 窗口, Stage 舞台, String 配置路径) {
        String 模式 = System.getenv("JONE_UI_SMOKE");
        if (模式 == null || 模式.isBlank() || "0".equals(模式) || "false".equalsIgnoreCase(模式)) {
            return;
        }
        if ("theme".equals(模式)) {
            ConfigStore.写入(配置路径, "theme.presetId", "sakuraPink");
            窗口.重新加载主题();
            PauseTransition 检查 = new PauseTransition(Duration.millis(700));
            检查.setOnFinished(事件 -> {
                System.out.println("【自检】主题热切换后界面重建完成：通过");
                Platform.exit();
            });
            检查.play();
            return;
        }

        if ("convert".equals(模式)) {
            String 文件 = System.getenv("JONE_SMOKE_FILE");
            boolean 覆盖 = "1".equals(System.getenv("JONE_SMOKE_OVERWRITE"));
            PauseTransition 检查 = new PauseTransition(Duration.millis(900));
            检查.setOnFinished(事件 -> {
                String 结果 = 窗口.转换页自检(文件, 覆盖);
                System.out.println("【自检】转换页执行结果：" + 结果);
                Platform.exit();
            });
            检查.play();
            return;
        }

        if ("clock".equals(模式)) {
            窗口.打开页面("home");
            PauseTransition 检查 = new PauseTransition(Duration.millis(900));
            检查.setOnFinished(事件 -> {
                System.out.println("【自检】" + 窗口.主页时钟自检());
                Platform.exit();
            });
            检查.play();
            return;
        }

        if ("titlebar".equals(模式)) {
            运行标题栏自检(场景, 舞台);
            return;
        }

        if ("confirm".equals(模式)) {
            运行覆盖确认自检(窗口, 舞台);
            return;
        }

        if ("formats".equals(模式)) {
            运行格式切换自检(窗口);
            return;
        }

        Node 感应区 = 场景.lookup("#侧边栏感应区");
        if (感应区 == null) {
            System.out.println("【自检】未找到侧边栏感应区：失败");
            Platform.exit();
            return;
        }
        感应区.fireEvent(new MouseEvent(MouseEvent.MOUSE_ENTERED, 0, 0, 0, 0,
                MouseButton.NONE, 0,
                false, false, false, false, false, false, false, false, false, false, null));

        PauseTransition 检查 = new PauseTransition(Duration.millis(500));
        检查.setOnFinished(事件 -> {
            boolean 展开 = 窗口.侧边栏是否展开();
            System.out.println(展开
                    ? "【自检】悬停 0.2 秒后侧边栏已展开：通过"
                    : "【自检】侧边栏未展开：失败");
            Platform.exit();
        });
        检查.play();
    }

    /**
     * 标题栏自检：模拟真实的按下/松开，验证三枚窗口按钮的事件链没有被吞掉。
     */
    private void 运行标题栏自检(Scene 场景, Stage 舞台) {
        Node 节点 = 场景.lookup("#最大化按钮");
        if (!(节点 instanceof Button 最大化)) {
            System.out.println("【自检】未找到最大化按钮：失败");
            Platform.exit();
            return;
        }

        boolean 初始最大化 = 舞台.isMaximized();
        最大化.fire();
        boolean 触发切换 = 舞台.isMaximized() != 初始最大化;
        最大化.fire();
        boolean 已还原 = 舞台.isMaximized() == 初始最大化;

        // 再模拟一次真实的按下 + 松开，确认鼠标事件能走到 Button 的行为处理
        boolean[] 收到动作 = {false};
        javafx.event.EventHandler<javafx.event.ActionEvent> 监听 = 事件 -> 收到动作[0] = true;
        最大化.addEventHandler(javafx.event.ActionEvent.ACTION, 监听);
        Event.fireEvent(最大化, 鼠标事件(MouseEvent.MOUSE_PRESSED));
        Event.fireEvent(最大化, 鼠标事件(MouseEvent.MOUSE_RELEASED));
        最大化.removeEventHandler(javafx.event.ActionEvent.ACTION, 监听);

        System.out.println("【自检】窗口按钮：动作触发=" + (触发切换 ? "正常" : "异常")
                + "；最大化还原=" + (已还原 ? "正常" : "异常")
                + "；鼠标按下松开=" + (收到动作[0] ? "可触发" : "被吞掉")
                + "；按钮引用=" + (最大化.getParent() != null ? "已挂到界面" : "未挂载"));
        Platform.exit();
    }

    /** 构造落在按钮内部的鼠标事件 */
    private MouseEvent 鼠标事件(javafx.event.EventType<MouseEvent> 类型) {
        return new MouseEvent(类型, 20, 14, 0, 0, MouseButton.PRIMARY, 1,
                false, false, false, false, true, false, false, false, false, true, null);
    }

    /**
     * 格式切换自检：验证「改格式 → 先落盘 → 按钮与提示同步刷新」，并复位为 JSON。
     */
    private void 运行格式切换自检(主窗口 窗口) {
        窗口.打开页面("converter");
        PauseTransition 检查 = new PauseTransition(Duration.millis(500));
        检查.setOnFinished(事件 -> {
            转换页 页 = 窗口.取当前转换页();
            if (页 == null) {
                System.out.println("【自检】转换页未就绪：失败");
                Platform.exit();
                return;
            }
            String 配置路径 = ConfigStore.默认配置路径();

            页.设置格式(false, "YAML");
            String 目标格式 = 页.当前目标格式();
            String 落盘格式 = ConfigStore.读取(配置路径, "converter.targetFormat", "?");

            页.设置格式(false, "JSON");
            String 还原格式 = 页.当前目标格式();
            String 还原落盘 = ConfigStore.读取(配置路径, "converter.targetFormat", "?");

            boolean 通过 = "YAML".equals(目标格式) && "YAML".equals(落盘格式)
                    && "JSON".equals(还原格式) && "JSON".equals(还原落盘);
            System.out.println("【自检】格式切换：" + (通过 ? "通过" : "失败")
                    + "（切 YAML → 界面 " + 目标格式 + " / 配置 " + 落盘格式
                    + "；复位 JSON → 界面 " + 还原格式 + " / 配置 " + 还原落盘 + "）");
            Platform.exit();
        });
        检查.play();
    }

    /**
     * 覆盖确认自检：真实走「确认 → 弹窗 → 点是/否 → 转换」整条链路。
     */
    private void 运行覆盖确认自检(主窗口 窗口, Stage 舞台) {
        String 文件 = System.getenv("JONE_SMOKE_FILE");
        String 选择 = "否".equals(System.getenv("JONE_SMOKE_CHOICE")) ? "否" : "是";

        窗口.打开页面("converter");
        PauseTransition 准备 = new PauseTransition(Duration.millis(600));
        准备.setOnFinished(事件 -> {
            转换页 页 = 窗口.取当前转换页();
            if (页 == null) {
                System.out.println("【自检】转换页未就绪：失败");
                Platform.exit();
                return;
            }
            页.注入已选文件(文件);
            if (页.当前已选文件().isBlank()) {
                System.out.println("【自检】未注入文件：失败");
                Platform.exit();
                return;
            }

            // 先安排点击弹窗按钮，再触发确认（确认会进入 showAndWait 的嵌套事件循环）
            PauseTransition 点弹窗 = new PauseTransition(Duration.millis(500));
            点弹窗.setOnFinished(事件2 -> {
                boolean 成功 = 点击弹窗按钮(舞台, 选择);
                System.out.println("【自检】弹窗点击「" + 选择 + "」：" + (成功 ? "成功" : "未找到弹窗"));
            });
            点弹窗.play();

            页.触发确认按钮();

            PauseTransition 查看 = new PauseTransition(Duration.millis(800));
            查看.setOnFinished(事件3 -> {
                System.out.println("【自检】转换页状态栏：" + 页.当前状态文本());
                Platform.exit();
            });
            查看.play();
        });
        准备.play();
    }

    /** 在所有可见的附属窗口里找到指定文字的按钮并点击 */
    private boolean 点击弹窗按钮(Stage 主舞台, String 文字) {
        for (Window 窗口对象 : Window.getWindows()) {
            if (!(窗口对象 instanceof Stage 候选) || 候选 == 主舞台 || !候选.isShowing()) {
                continue;
            }
            if (候选.getScene() == null) {
                continue;
            }
            for (Node 节点 : 候选.getScene().getRoot().lookupAll(".button")) {
                if (节点 instanceof Button 按钮 && 文字.equals(按钮.getText())) {
                    按钮.fire();
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 截图模式：仅当环境变量 JONE_SNAPSHOT_DIR 存在时生效，用于自动化视觉验收。
     */
    private void 触发截图模式(Scene 场景) {
        String 截图目录 = System.getenv("JONE_SNAPSHOT_DIR");
        if (截图目录 == null || 截图目录.isBlank()) {
            return;
        }
        String 名称 = System.getenv("JONE_SNAPSHOT_NAME");
        String 文件名 = (名称 == null || 名称.isBlank() ? "窗口" : 名称) + ".png";

        PauseTransition 延时 = new PauseTransition(Duration.millis(1200));
        延时.setOnFinished(事件 -> {
            try {
                WritableImage 图像 = 场景.snapshot(null);
                int 宽 = (int) 图像.getWidth();
                int 高 = (int) 图像.getHeight();
                int[] 像素 = new int[宽 * 高];
                PixelReader 读取器 = 图像.getPixelReader();
                读取器.getPixels(0, 0, 宽, 高,
                        javafx.scene.image.PixelFormat.getIntArgbInstance(), 像素, 0, 宽);

                String 目标 = Path.of(截图目录, 文件名).toString();
                boolean 成功 = ImageExporter.保存PNG(目标, 宽, 高, 像素);
                System.out.println(成功 ? "【截图】已保存：" + 目标 : "【错误】截图保存失败");
            } catch (RuntimeException 异常) {
                System.out.println("【错误】截图过程异常：" + 异常.getMessage());
            }
            Platform.exit();
        });
        延时.play();
    }

    public static void main(String[] 参数) {
        launch(参数);
    }
}
