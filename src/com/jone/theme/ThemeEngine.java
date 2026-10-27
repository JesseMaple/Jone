package com.jone.theme;

import com.jone.config.ConfigStore;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 主题引擎：所有颜色的唯一计算入口。
 *
 * 设计原则：
 * - 界面绝不在 CSS 里写死颜色，一律由本类算出十六进制字符串后交给 setStyle()；
 * - 对外接口只收 String / boolean / double 等原生类型，不传出 JavaFX 的 Color 对象；
 * - 颜色计算（兑白、加深、亮度判定、选中背景）集中在此，便于单测。
 *
 * 《TODO:后续支持深色模式下的图表配色时，可在此增加一组语义色。》
 */
public final class ThemeEngine {

    /** 默认主题色：DeepSeek 蓝 */
    public static final String 默认主题色 = "#4D6BFE";

    /** 跟随系统且系统为深色时，强调色固定使用 DeepSeek 蓝（PRD 第三章） */
    public static final String 深色强调色 = "#4D6BFE";

    private static final Pattern 颜色格式 = Pattern.compile("^#([0-9a-fA-F]{6})$");

    /** 预设主题：标识、中文名、颜色 */
    private static final List<String[]> 预设清单 = List.of(
            new String[]{"deepseekBlue", "DeepSeek 蓝", "#4D6BFE"},
            new String[]{"sakuraPink", "樱花粉", "#FF9EB5"},
            new String[]{"violet", "紫罗兰", "#8B5CF6"},
            new String[]{"brightYellow", "明黄", "#F5C518"},
            new String[]{"matchaGreen", "抹茶绿", "#8FBF5F"}
    );

    private ThemeEngine() {
        throw new UnsupportedOperationException("工具类禁止实例化");
    }

    // ------------------------------------------------------------------
    // 预设主题
    // ------------------------------------------------------------------

    /** 返回全部预设主题标识 */
    public static String[] 预设标识() {
        return 预设清单.stream().map(项 -> 项[0]).toArray(String[]::new);
    }

    /** 返回预设主题的中文名；未知标识返回空字符串 */
    public static String 预设名称(String 预设标识) {
        for (String[] 项 : 预设清单) {
            if (项[0].equals(预设标识)) {
                return 项[1];
            }
        }
        return "";
    }

    /** 返回预设主题的颜色；未知标识回退默认主题色 */
    public static String 预设颜色(String 预设标识) {
        for (String[] 项 : 预设清单) {
            if (项[0].equals(预设标识)) {
                return 项[2];
            }
        }
        return 默认主题色;
    }

    /** 判断是否为合法颜色（仅接受 #RRGGBB） */
    public static boolean 是合法颜色(String 值) {
        return 值 != null && 颜色格式.matcher(值.trim()).matches();
    }

    // ------------------------------------------------------------------
    // 从配置读取当前主题
    // ------------------------------------------------------------------

    /**
     * 读取当前生效的主题色十六进制值。
     * 配置缺失或非法时回退为默认主题色。
     */
    public static String 当前主题色(String 配置文件路径) {
        String 模式 = ConfigStore.读取(配置文件路径, "theme.mode", "preset");
        if ("custom".equals(模式)) {
            String 自定义 = ConfigStore.读取(配置文件路径, "theme.customHex", 默认主题色);
            return 是合法颜色(自定义) ? 自定义 : 默认主题色;
        }
        String 预设 = ConfigStore.读取(配置文件路径, "theme.presetId", "deepseekBlue");
        // 跟随系统时只改变「背景 / 文字 / 强调色」，主题色本身仍然来自预设或自定义
        return 预设颜色(预设);
    }

    /** 配置里是否开启了「跟随系统」 */
    public static boolean 是否跟随系统(String 配置文件路径) {
        return ConfigStore.读取布尔(配置文件路径, "theme.followSystem", false);
    }

    // ------------------------------------------------------------------
    // 颜色计算
    // ------------------------------------------------------------------

    /** 把主题色按比例兑入白色（比例 0~1，越大越接近纯白） */
    public static String 混白(String 颜色, double 白色比例) {
        Color 基础 = 转颜色(颜色);
        return 转十六进制(基础.interpolate(Color.WHITE, 限定比例(白色比例)));
    }

    /** 把主题色按比例兑入黑色（比例 0~1，越大越接近纯黑） */
    public static String 混黑(String 颜色, double 黑色比例) {
        Color 基础 = 转颜色(颜色);
        return 转十六进制(基础.interpolate(Color.BLACK, 限定比例(黑色比例)));
    }

    /** 判断颜色是否为「亮色」（按相对亮度阈值 0.62） */
    public static boolean 是亮色(String 颜色) {
        Color 基础 = 转颜色(颜色);
        return 亮度(基础) >= 0.62;
    }

    /**
     * 应用整体背景色。
     * - 跟随系统 + 浅色：纯白背景；
     * - 跟随系统 + 深色：主题色重度压暗，得到近似 Trae 的极暗底色；
     * - 不跟随系统：主题色兑入大量白色，得到极淡的主题色。
     */
    public static String 应用背景色(String 主题色, boolean 跟随系统, boolean 系统深色) {
        if (跟随系统) {
            return 系统深色 ? 混黑(主题色, 0.92) : "#FFFFFF";
        }
        return 混白(主题色, 0.92);
    }

    /**
     * 侧边栏选中项的背景色。
     * 主题色本身较亮则压暗，较暗则提亮，保证与侧边栏底色有区分度；
     * 深色模式下改为压暗，避免一块亮色过于刺眼。
     */
    public static String 选中背景色(String 主题色, boolean 系统深色) {
        if (系统深色) {
            return 混黑(主题色, 0.55);
        }
        return 选中背景色(主题色);
    }

    /**
     * 侧边栏选中项的背景色（浅色模式）。
     * 主题色较亮则背景暗一些，反之亮一些。
     */
    public static String 选中背景色(String 主题色) {
        if (是亮色(主题色)) {
            return 混黑(主题色, 0.18);
        }
        return 混白(主题色, 0.72);
    }

    /**
     * 选中项的文字色。
     * 选中底色偏亮时用「同色系压暗」的深色，偏暗时用白色，保证任何主题下都清晰可读。
     */
    public static String 选中文字色(String 主题色, boolean 系统深色) {
        String 选中底色 = 选中背景色(主题色, 系统深色);
        return 亮度(转颜色(选中底色)) > 0.5 ? 混黑(主题色, 0.72) : "#FFFFFF";
    }

    /**
     * 强调色（窗口按钮标识、标题文字等）。
     * 跟随系统且系统为深色时固定 DeepSeek 蓝；浅色时用纯黑（PRD 第三章）。
     */
    public static String 强调色(String 主题色, boolean 跟随系统, boolean 系统深色) {
        if (跟随系统) {
            return 系统深色 ? 深色强调色 : "#000000";
        }
        return 主题色;
    }

    /**
     * 正文文字色：跟随系统时浅色纯黑、深色 DeepSeek 蓝；否则按明暗用深灰/浅灰。
     */
    public static String 文字色(boolean 跟随系统, boolean 系统深色) {
        if (跟随系统) {
            return 系统深色 ? 深色强调色 : "#000000";
        }
        return 系统深色 ? "#E6E8EF" : "#1F2430";
    }

    /** 侧边栏边框色：无论什么主题，始终是淡灰色（PRD 第三章） */
    public static String 侧边栏边框色() {
        return "#D8DCE3";
    }

    /** 错误提示色（与主题无关的语义色） */
    public static String 错误色() {
        return "#D9534F";
    }

    /** 覆盖弹窗「是」按钮：绿底白字（PRD 第三章第五节硬性要求） */
    public static String 覆盖确认绿() {
        return "#2EAD6B";
    }

    /** 覆盖弹窗「否」按钮：红底黑字（PRD 第三章第五节硬性要求） */
    public static String 覆盖取消红() {
        return "#E05C5C";
    }

    /**
     * 两个十六进制颜色之间的线性混合，比例 0 返回前者，1 返回后者。
     */
    public static String 混合(String 起始色, String 目标色, double 比例) {
        Color 起点 = 转颜色(起始色);
        Color 终点 = 转颜色(目标色);
        return 转十六进制(起点.interpolate(终点, 限定比例(比例)));
    }

    // ------------------------------------------------------------------
    // 内部工具
    // ------------------------------------------------------------------

    private static double 限定比例(double 比例) {
        if (比例 <= 0) {
            return 0;
        }
        return Math.min(比例, 1);
    }

    private static Color 转颜色(String 十六进制) {
        if (!是合法颜色(十六进制)) {
            return Color.web(默认主题色);
        }
        return Color.web(十六进制.trim());
    }

    private static String 转十六进制(Color 颜色) {
        int 红 = (int) Math.round(颜色.getRed() * 255);
        int 绿 = (int) Math.round(颜色.getGreen() * 255);
        int 蓝 = (int) Math.round(颜色.getBlue() * 255);
        return String.format("#%02X%02X%02X", 红, 绿, 蓝);
    }

    /** 相对亮度（感知加权，0 黑 1 白） */
    private static double 亮度(Color 颜色) {
        return 0.2126 * 颜色.getRed() + 0.7152 * 颜色.getGreen() + 0.0722 * 颜色.getBlue();
    }
}
