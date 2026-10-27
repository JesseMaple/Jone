package com.jone.theme;

import com.jone.config.ConfigStore;
import com.jone.testutil.临时目录;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 主题引擎单元测试。
 *
 * 重点验证 PRD 第三章的颜色规则：
 * - 背景色 = 主题色兑入大量白色；
 * - 选中背景按主题明暗动态计算；
 * - 跟随系统时的浅色/深色取值。
 */
@DisplayName("主题引擎 ThemeEngine")
class ThemeEngineTest {

    private Path 工作目录;
    private Path 配置文件;

    @BeforeEach
    void 准备目录() throws IOException {
        工作目录 = 临时目录.创建("主题引擎");
        配置文件 = 工作目录.resolve(ConfigStore.默认配置文件名);
    }

    @AfterEach
    void 清理目录() {
        临时目录.删除(工作目录);
    }

    /** 计算十六进制颜色的感知亮度，用于断言「变亮/变暗」 */
    private static double 亮度(String 十六进制) {
        int 红 = Integer.parseInt(十六进制.substring(1, 3), 16);
        int 绿 = Integer.parseInt(十六进制.substring(3, 5), 16);
        int 蓝 = Integer.parseInt(十六进制.substring(5, 7), 16);
        return 0.2126 * 红 / 255 + 0.7152 * 绿 / 255 + 0.0722 * 蓝 / 255;
    }

    @Test
    @DisplayName("颜色混合：黑兑白 50% 应得到中灰，纯白压黑应得到纯黑")
    void 颜色混合基础() {
        assertEquals("#808080", ThemeEngine.混白("#000000", 0.5));
        assertEquals("#000000", ThemeEngine.混黑("#FFFFFF", 1.0));
        assertEquals("#4D6BFE", ThemeEngine.混合("#4D6BFE", "#000000", 0.0));
        assertEquals("#000000", ThemeEngine.混合("#4D6BFE", "#000000", 1.0));
    }

    @Test
    @DisplayName("比例越界时应被夹到 0~1，不产生非法颜色")
    void 比例越界被夹紧() {
        assertEquals("#000000", ThemeEngine.混白("#000000", -5));
        assertEquals("#FFFFFF", ThemeEngine.混白("#000000", 5));
        assertTrue(ThemeEngine.是合法颜色(ThemeEngine.混白("#4D6BFE", 1.7)));
    }

    @Test
    @DisplayName("颜色合法性校验只接受 #RRGGBB")
    void 颜色合法性校验() {
        assertTrue(ThemeEngine.是合法颜色("#4D6BFE"));
        assertTrue(ThemeEngine.是合法颜色("#abcdef"));
        assertFalse(ThemeEngine.是合法颜色("4D6BFE"));
        assertFalse(ThemeEngine.是合法颜色("#FFF"));
        assertFalse(ThemeEngine.是合法颜色("#GGGGGG"));
        assertFalse(ThemeEngine.是合法颜色(null));
    }

    @Test
    @DisplayName("亮暗判定：明黄与樱花粉为亮色，DeepSeek 蓝与紫罗兰为暗色")
    void 亮暗判定() {
        assertTrue(ThemeEngine.是亮色("#F5C518"));
        assertTrue(ThemeEngine.是亮色("#FF9EB5"));
        assertFalse(ThemeEngine.是亮色("#4D6BFE"));
        assertFalse(ThemeEngine.是亮色("#8B5CF6"));
    }

    @Test
    @DisplayName("选中背景色：亮主题压暗、暗主题提亮")
    void 选中背景色随主题明暗变化() {
        String 明黄 = "#F5C518";
        String 深蓝 = "#4D6BFE";

        String 明黄选中 = ThemeEngine.选中背景色(明黄);
        assertTrue(亮度(明黄选中) < 亮度(明黄), "亮主题的选中背景应更暗，实际：" + 明黄选中);

        String 深蓝选中 = ThemeEngine.选中背景色(深蓝);
        assertTrue(亮度(深蓝选中) > 亮度(深蓝), "暗主题的选中背景应更亮，实际：" + 深蓝选中);

        String 深色模式选中 = ThemeEngine.选中背景色(深蓝, true);
        assertTrue(亮度(深色模式选中) < 亮度(深蓝), "深色模式下选中背景应压暗，实际：" + 深色模式选中);
    }

    @Test
    @DisplayName("应用背景色：不跟随时为极淡主题色，跟随浅色时为纯白，跟随深色时为极暗")
    void 应用背景色三种情形() {
        String 普通 = ThemeEngine.应用背景色("#4D6BFE", false, false);
        assertTrue(亮度(普通) > 0.85, "极淡主题色应非常亮，实际：" + 普通);

        assertEquals("#FFFFFF", ThemeEngine.应用背景色("#4D6BFE", true, false),
                "跟随系统浅色时应为纯白背景");

        String 深色背景 = ThemeEngine.应用背景色("#4D6BFE", true, true);
        assertTrue(亮度(深色背景) < 0.1, "跟随系统深色时应为极暗背景，实际：" + 深色背景);
    }

    @Test
    @DisplayName("强调色：跟随系统浅色为纯黑、深色为 DeepSeek 蓝，不跟随时为主题色")
    void 强调色规则() {
        assertEquals("#4D6BFE", ThemeEngine.强调色("#FF9EB5", true, true));
        assertEquals("#000000", ThemeEngine.强调色("#FF9EB5", true, false));
        assertEquals("#FF9EB5", ThemeEngine.强调色("#FF9EB5", false, false));
        assertEquals("#FF9EB5", ThemeEngine.强调色("#FF9EB5", false, true));
    }

    @Test
    @DisplayName("文字色：跟随系统时浅色纯黑、深色 DeepSeek 蓝")
    void 文字色规则() {
        assertEquals("#000000", ThemeEngine.文字色(true, false));
        assertEquals("#4D6BFE", ThemeEngine.文字色(true, true));
        assertTrue(亮度(ThemeEngine.文字色(false, false)) < 0.2, "非跟随浅色时应为深灰");
        assertTrue(亮度(ThemeEngine.文字色(false, true)) > 0.8, "非跟随深色时应为浅灰");
    }

    @Test
    @DisplayName("预设主题：标识、中文名与颜色应一一对应")
    void 预设主题() {
        assertEquals(5, ThemeEngine.预设标识().length);
        assertEquals("#4D6BFE", ThemeEngine.预设颜色("deepseekBlue"));
        assertEquals("#FF9EB5", ThemeEngine.预设颜色("sakuraPink"));
        assertEquals("#8B5CF6", ThemeEngine.预设颜色("violet"));
        assertEquals("#F5C518", ThemeEngine.预设颜色("brightYellow"));
        assertEquals("#8FBF5F", ThemeEngine.预设颜色("matchaGreen"));

        assertEquals("樱花粉", ThemeEngine.预设名称("sakuraPink"));
        assertEquals("", ThemeEngine.预设名称("不存在的预设"));
        assertEquals(ThemeEngine.默认主题色, ThemeEngine.预设颜色("不存在的预设"), "未知预设应回退默认色");
    }

    @Test
    @DisplayName("当前主题色应能按配置读取预设色与自定义色")
    void 从配置读取主题色() {
        ConfigStore.确保存在(配置文件.toString());

        ConfigStore.写入(配置文件.toString(), "theme.mode", "preset");
        ConfigStore.写入(配置文件.toString(), "theme.presetId", "matchaGreen");
        assertEquals("#8FBF5F", ThemeEngine.当前主题色(配置文件.toString()));

        ConfigStore.写入(配置文件.toString(), "theme.mode", "custom");
        ConfigStore.写入(配置文件.toString(), "theme.customHex", "#123456");
        assertEquals("#123456", ThemeEngine.当前主题色(配置文件.toString()));

        ConfigStore.写入(配置文件.toString(), "theme.customHex", "不是颜色");
        assertEquals(ThemeEngine.默认主题色, ThemeEngine.当前主题色(配置文件.toString()),
                "非法自定义色应回退默认主题色");
    }

    @Test
    @DisplayName("是否跟随系统应正确读取布尔配置")
    void 是否跟随系统() {
        ConfigStore.确保存在(配置文件.toString());
        assertFalse(ThemeEngine.是否跟随系统(配置文件.toString()));

        ConfigStore.写入(配置文件.toString(), "theme.followSystem", true);
        assertTrue(ThemeEngine.是否跟随系统(配置文件.toString()));
    }

    @Test
    @DisplayName("侧边栏边框色在任何主题下都是淡灰色")
    void 边框色固定淡灰() {
        assertEquals("#D8DCE3", ThemeEngine.侧边栏边框色());
        assertTrue(亮度(ThemeEngine.侧边栏边框色()) > 0.8, "淡灰色应偏亮");
    }
}
