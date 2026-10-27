package com.jone.config;

import com.jone.testutil.临时目录;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 配置仓库单元测试。
 *
 * 覆盖 PRD 第二章要求：启动优先读 config.json、缺失则创建默认值、修改必须先落盘。
 */
@DisplayName("配置仓库 ConfigStore")
class ConfigStoreTest {

    private Path 工作目录;
    private Path 配置文件;

    @BeforeEach
    void 准备目录() throws IOException {
        工作目录 = 临时目录.创建("配置层");
        配置文件 = 工作目录.resolve(ConfigStore.默认配置文件名);
    }

    @AfterEach
    void 清理目录() {
        临时目录.删除(工作目录);
    }

    @Test
    @DisplayName("文件不存在时应创建包含默认值的配置文件")
    void 文件不存在时应创建默认配置() throws IOException {
        assertFalse(Files.exists(配置文件), "前置条件：配置文件此时不应存在");

        String 错误 = ConfigStore.确保存在(配置文件.toString());

        assertEquals("", 错误, "创建默认配置不应返回错误");
        assertTrue(Files.exists(配置文件), "应创建出配置文件");

        String 内容 = Files.readString(配置文件, StandardCharsets.UTF_8);
        assertTrue(内容.contains("\"presetId\": \"deepseekBlue\""), "默认主题预设应为 DeepSeek 蓝");
        assertTrue(内容.contains("\"lastPage\": \"home\""), "默认停留页面应为主页");
        assertTrue(内容.contains("\"sourceFormat\": \"CSV\""), "默认源格式应为 CSV");
        assertTrue(内容.contains("\"targetFormat\": \"JSON\""), "默认目标格式应为 JSON");
    }

    @Test
    @DisplayName("默认配置应能被正确读取")
    void 默认配置应可读取() {
        ConfigStore.确保存在(配置文件.toString());

        assertEquals("deepseekBlue", ConfigStore.读取(配置文件.toString(), "theme.presetId", "无"));
        assertEquals("preset", ConfigStore.读取(配置文件.toString(), "theme.mode", "无"));
        assertEquals("home", ConfigStore.读取(配置文件.toString(), "lastPage", "无"));
        assertEquals(1000L, ConfigStore.读取数值(配置文件.toString(), "window.width", -1L));
        assertFalse(ConfigStore.读取布尔(配置文件.toString(), "theme.followSystem", true));
    }

    @Test
    @DisplayName("写入后必须立即落盘，换一次读取仍能拿到新值")
    void 写入后应立即落盘() throws IOException {
        ConfigStore.确保存在(配置文件.toString());

        boolean 成功 = ConfigStore.写入(配置文件.toString(), "theme.presetId", "sakuraPink");
        assertTrue(成功, "写入应成功");

        // 不依赖内存状态，直接重新从磁盘解析
        String 磁盘内容 = Files.readString(配置文件, StandardCharsets.UTF_8);
        assertTrue(磁盘内容.contains("\"sakuraPink\""), "磁盘文件应已包含新值");
        assertEquals("sakuraPink", ConfigStore.读取(配置文件.toString(), "theme.presetId", "无"));
    }

    @Test
    @DisplayName("写入单个配置项不应破坏其他配置项")
    void 写入应保留其他配置项() {
        ConfigStore.确保存在(配置文件.toString());

        ConfigStore.写入(配置文件.toString(), "theme.customHex", "#123456");

        assertEquals("#123456", ConfigStore.读取(配置文件.toString(), "theme.customHex", "无"));
        assertEquals("deepseekBlue", ConfigStore.读取(配置文件.toString(), "theme.presetId", "无"),
                "其他主题配置不应被覆盖");
        assertEquals("home", ConfigStore.读取(配置文件.toString(), "lastPage", "无"),
                "非主题配置不应被覆盖");
    }

    @Test
    @DisplayName("数值与布尔值应按类型读写，不能变成字符串")
    void 数值与布尔应按类型读写() throws IOException {
        ConfigStore.确保存在(配置文件.toString());

        assertTrue(ConfigStore.写入(配置文件.toString(), "window.width", 1280L));
        assertTrue(ConfigStore.写入(配置文件.toString(), "window.maximized", true));

        String 磁盘内容 = Files.readString(配置文件, StandardCharsets.UTF_8);
        assertTrue(磁盘内容.contains("\"width\": 1280"), "宽度应以数字形式落盘");
        assertTrue(磁盘内容.contains("\"maximized\": true"), "最大化标记应以布尔形式落盘");

        assertEquals(1280L, ConfigStore.读取数值(配置文件.toString(), "window.width", -1L));
        assertTrue(ConfigStore.读取布尔(配置文件.toString(), "window.maximized", false));
    }

    @Test
    @DisplayName("写入不存在的层级键时应自动创建中间节点")
    void 写入应自动创建中间节点() {
        ConfigStore.确保存在(配置文件.toString());

        assertTrue(ConfigStore.写入(配置文件.toString(), "扩展.实验开关", "开启"));
        assertEquals("开启", ConfigStore.读取(配置文件.toString(), "扩展.实验开关", "无"));
    }

    @Test
    @DisplayName("配置格式非法时应返回中文错误信息，且不覆盖用户文件")
    void 格式非法时报错且不覆盖() throws IOException {
        Files.writeString(配置文件, "{ 这不是合法的 JSON ", StandardCharsets.UTF_8);

        String 错误 = ConfigStore.确保存在(配置文件.toString());
        assertNotNull(错误);
        assertTrue(错误.contains("配置文件格式错误"), "错误信息应为中文且指明格式问题，实际：" + 错误);

        String 原内容 = Files.readString(配置文件, StandardCharsets.UTF_8);
        assertTrue(原内容.contains("这不是合法的 JSON"), "不应覆盖用户已损坏的文件");
    }

    @Test
    @DisplayName("空配置文件应被初始化为默认值")
    void 空文件应初始化默认值() throws IOException {
        Files.writeString(配置文件, "   ", StandardCharsets.UTF_8);

        String 错误 = ConfigStore.确保存在(配置文件.toString());

        assertEquals("", 错误);
        assertEquals("deepseekBlue", ConfigStore.读取(配置文件.toString(), "theme.presetId", "无"));
    }

    @Test
    @DisplayName("原子写入不应残留临时文件")
    void 不应残留临时文件() throws IOException {
        ConfigStore.确保存在(配置文件.toString());
        ConfigStore.写入(配置文件.toString(), "lastPage", "converter");

        try (var 流 = Files.list(工作目录)) {
            boolean 有残留 = 流.anyMatch(路径 -> 路径.getFileName().toString().endsWith(".tmp"));
            assertFalse(有残留, "写入完成后不应残留 .tmp 文件");
        }
    }

    @Test
    @DisplayName("读取不存在的键应返回调用方给的默认值")
    void 缺失键返回默认值() {
        ConfigStore.确保存在(配置文件.toString());

        assertEquals("兜底值", ConfigStore.读取(配置文件.toString(), "theme.不存在的键", "兜底值"));
        assertEquals(42L, ConfigStore.读取数值(配置文件.toString(), "window.不存在", 42L));
        assertTrue(ConfigStore.读取布尔(配置文件.toString(), "window.不存在", true));
    }
}
