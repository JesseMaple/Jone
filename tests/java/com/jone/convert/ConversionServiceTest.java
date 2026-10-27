package com.jone.convert;

import com.jone.testutil.临时目录;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 转换服务单元测试。
 *
 * 覆盖 PRD 第五章要求：内容正确、同格式拦截、同名文件「否」时的序号规则。
 */
@DisplayName("转换服务 ConversionService")
class ConversionServiceTest {

    private Path 工作目录;

    @BeforeEach
    void 准备目录() throws IOException {
        工作目录 = 临时目录.创建("转换内核");
    }

    @AfterEach
    void 清理目录() {
        临时目录.删除(工作目录);
    }

    private Path 写入(String 文件名, String 内容) throws IOException {
        Path 文件 = 工作目录.resolve(文件名);
        Files.writeString(文件, 内容, StandardCharsets.UTF_8);
        return 文件;
    }

    private String 读取(Path 文件) throws IOException {
        return Files.readString(文件, StandardCharsets.UTF_8);
    }

    // ------------------------------------------------------------------
    // 内容正确性
    // ------------------------------------------------------------------

    @Test
    @DisplayName("CSV 转 JSON：表头成为字段名，值保持字符串")
    void CSV转JSON内容正确() throws IOException {
        Path 源 = 写入("学生.csv", "姓名,年龄\n张三,18\n李四,20\n");
        Path 目标 = 工作目录.resolve("学生.json");

        String 错误 = ConversionService.转换(源.toString(), 目标.toString(), true);

        assertEquals("", 错误, "转换应成功");
        String JSON = 读取(目标);
        assertTrue(JSON.contains("\"姓名\": \"张三\""), "应包含第一条记录，实际：" + JSON);
        assertTrue(JSON.contains("\"年龄\": \"18\""), "值应保持字符串形态");
        assertTrue(JSON.startsWith("["), "根节点应为数组");
    }

    @Test
    @DisplayName("CSV 带 BOM、含逗号与引号字段时应正确解析")
    void CSV转JSON处理特殊字符() throws IOException {
        Path 源 = 写入("特殊.csv", "\uFEFF名称,备注\n苹果,\"红, 甜\"\n梨,\"含\"\"引号\"\"\"\n");
        Path 目标 = 工作目录.resolve("特殊.json");

        String 错误 = ConversionService.转换(源.toString(), 目标.toString(), true);

        assertEquals("", 错误, "转换应成功");
        String JSON = 读取(目标);
        assertTrue(JSON.contains("红, 甜"), "字段内的逗号应保留，实际：" + JSON);
        // JSON 文本里双引号会被转义为 \"，这里断言转义后的形态
        assertTrue(JSON.contains("含\\\"引号\\\""), "双引号转义应还原，实际：" + JSON);
        assertFalse(JSON.contains("\uFEFF"), "BOM 不应进入数据");
    }

    @Test
    @DisplayName("JSON 转 CSV：表头取所有行键的并集，缺失字段留空")
    void JSON转CSV表头取并集() throws IOException {
        Path 源 = 写入("数据.json", "[{\"a\":1},{\"b\":2},{\"a\":3,\"c\":\"中文\"}]");
        Path 目标 = 工作目录.resolve("数据.csv");

        String 错误 = ConversionService.转换(源.toString(), 目标.toString(), true);

        assertEquals("", 错误, "转换应成功");
        String CSV = 读取(目标);
        assertTrue(CSV.contains("a,b,c"), "表头应是并集，实际：" + CSV);
        assertTrue(CSV.contains("1,,"), "缺失字段应留空，实际：" + CSV);
        assertTrue(CSV.contains(",2,"), "第二行只有 b，实际：" + CSV);
        assertTrue(CSV.contains("3,,中文"), "第三行应是 a 与 c，实际：" + CSV);
    }

    @Test
    @DisplayName("JSON 与 YAML 应可双向转换且内容一致")
    void JSON与YAML互转() throws IOException {
        Path 源 = 写入("配置.json", "{\"应用\":{\"主题\":\"深色\",\"宽度\":1200},\"列表\":[1,2,3]}");
        Path YAML = 工作目录.resolve("配置.yaml");
        Path 回转 = 工作目录.resolve("回转.json");

        assertEquals("", ConversionService.转换(源.toString(), YAML.toString(), true),
                "JSON 转 YAML 应成功");
        String YAML内容 = 读取(YAML);
        assertTrue(YAML内容.contains("应用:"), "YAML 应使用块状结构，实际：" + YAML内容);
        assertTrue(YAML内容.contains("主题: 深色"), "中文键值应保留");

        assertEquals("", ConversionService.转换(YAML.toString(), 回转.toString(), true),
                "YAML 转 JSON 应成功");
        String JSON内容 = 读取(回转);
        assertTrue(JSON内容.contains("\"宽度\": 1200"), "数字类型应保持为数字，实际：" + JSON内容);
    }

    @Test
    @DisplayName("CSV 转 YAML 应生成对象数组")
    void CSV转YAML() throws IOException {
        Path 源 = 写入("列表.csv", "键,值\n别名,Jone\n");
        Path 目标 = 工作目录.resolve("列表.yaml");

        String 错误 = ConversionService.转换(源.toString(), 目标.toString(), true);

        assertEquals("", 错误, "转换应成功");
        String YAML = 读取(目标);
        assertTrue(YAML.contains("- 键: 别名"), "应生成对象数组，实际：" + YAML);
    }

    // ------------------------------------------------------------------
    // 拦截规则
    // ------------------------------------------------------------------

    @Test
    @DisplayName("同格式转换应被拦截并返回中文原因")
    void 同格式转换被拦截() throws IOException {
        Path 源 = 写入("同名.json", "{\"a\":1}");
        Path 目标 = 工作目录.resolve("输出.json");

        String 错误 = ConversionService.转换(源.toString(), 目标.toString(), true);

        assertTrue(错误.contains("不能相同"), "应提示同格式被拦截，实际：" + 错误);
        assertFalse(Files.exists(目标), "被拦截时不应生成文件");
    }

    @Test
    @DisplayName("校验接口应对同格式返回中文原因、对合法组合返回空串")
    void 校验接口() {
        assertTrue(ConversionService.校验("CSV", "CSV").contains("不能相同"));
        assertFalse(ConversionService.可转换("JSON", "JSON"));
        assertEquals("", ConversionService.校验("CSV", "JSON"));
        assertTrue(ConversionService.可转换("YAML", "CSV"));
        assertFalse(ConversionService.可转换("CSV", "XML"), "不支持的格式应判为不可转换");
    }

    // ------------------------------------------------------------------
    // 覆盖与序号规则
    // ------------------------------------------------------------------

    @Test
    @DisplayName("目标已存在且不覆盖时，应自动追加 _1、_2 序号")
    void 不覆盖时自动加序号() throws IOException {
        Path 源 = 写入("a.csv", "x\n1\n");
        Path 目标 = 写入("a.json", "[]");

        String 错误1 = ConversionService.转换(源.toString(), 目标.toString(), false);
        assertEquals("", 错误1, "第一次转换应成功");

        Path 目标2 = 工作目录.resolve("a_1.json");
        assertTrue(Files.exists(目标2), "应生成 a_1.json");

        String 错误2 = ConversionService.转换(源.toString(), 目标.toString(), false);
        assertEquals("", 错误2, "第二次转换应成功");
        assertTrue(Files.exists(工作目录.resolve("a_2.json")), "应继续生成 a_2.json");
    }

    @Test
    @DisplayName("目标已存在且选择覆盖时，应直接覆盖原文件")
    void 覆盖时直接替换() throws IOException {
        Path 源 = 写入("b.csv", "x\n1\n");
        Path 目标 = 写入("b.json", "旧内容");

        String 错误 = ConversionService.转换(源.toString(), 目标.toString(), true);

        assertEquals("", 错误, "转换应成功");
        String 结果 = 读取(目标);
        assertFalse(结果.contains("旧内容"), "原内容应被覆盖");
        assertTrue(结果.contains("\"x\": \"1\""), "应写入新内容，实际：" + 结果);
        assertFalse(Files.exists(工作目录.resolve("b_1.json")), "覆盖模式不应产生序号文件");
    }

    @Test
    @DisplayName("计算实际输出路径应与真实落盘结果一致")
    void 计算实际输出路径() throws IOException {
        Path 源 = 写入("c.csv", "x\n1\n");
        写入("c.json", "[]");
        Path 目标 = 工作目录.resolve("c.json");

        String 预计 = ConversionService.计算实际输出路径(源.toString(), "JSON", false);
        assertEquals(工作目录.resolve("c_1.json").toString(), 预计, "应预测出 _1 序号文件");

        assertEquals("", ConversionService.转换(源.toString(), 目标.toString(), false));
        assertTrue(Files.exists(Path.of(预计)), "预测路径应真实存在");
    }

    // ------------------------------------------------------------------
    // 异常与边界
    // ------------------------------------------------------------------

    @Test
    @DisplayName("源文件不存在时返回中文错误")
    void 源文件不存在() {
        Path 不存在 = 工作目录.resolve("没有这个文件.csv");

        String 错误 = ConversionService.转换(不存在.toString(),
                工作目录.resolve("输出.json").toString(), true);

        assertTrue(错误.contains("源文件不存在"), "实际：" + 错误);
    }

    @Test
    @DisplayName("JSON 格式非法时返回中文错误")
    void JSON格式非法() throws IOException {
        Path 源 = 写入("坏.json", "{ 这不是 JSON ");
        Path 目标 = 工作目录.resolve("坏.yaml");

        String 错误 = ConversionService.转换(源.toString(), 目标.toString(), true);

        assertTrue(错误.contains("JSON 格式非法"), "实际：" + 错误);
        assertFalse(Files.exists(目标), "解析失败不应生成文件");
    }

    @Test
    @DisplayName("根节点不是对象数组时无法转 CSV，应给出中文原因")
    void 非数组转CSV报错() throws IOException {
        Path 源 = 写入("对象.json", "{\"a\":1}");
        Path 目标 = 工作目录.resolve("对象.csv");

        String 错误 = ConversionService.转换(源.toString(), 目标.toString(), true);

        assertTrue(错误.contains("对象数组"), "实际：" + 错误);
        assertFalse(Files.exists(目标), "失败时不应生成文件");
    }

    @Test
    @DisplayName("输出路径与源文件相同时应拒绝转换")
    void 输出与源相同被拒绝() throws IOException {
        Path 源 = 写入("d.csv", "x\n1\n");

        String 错误 = ConversionService.转换(源.toString(), 源.toString(), true);

        assertTrue(错误.contains("不能与源文件相同"), "实际：" + 错误);
    }

    @Test
    @DisplayName("推测输出路径应同目录、同主文件名、换后缀")
    void 推测输出路径() {
        String 结果 = ConversionService.推测输出路径("D:\\数据\\样例.csv", "JSON");
        assertEquals("D:\\数据\\样例.json", 结果);

        String 无后缀 = ConversionService.推测输出路径("D:\\数据\\无后缀", "YAML");
        assertEquals("D:\\数据\\无后缀.yaml", 无后缀);

        assertEquals("", ConversionService.推测输出路径("", "JSON"));
    }

    @Test
    @DisplayName("CSV 空内容应给出中文提示")
    void CSV空内容() throws IOException {
        Path 源 = 写入("空.csv", "");
        Path 目标 = 工作目录.resolve("空.json");

        String 错误 = ConversionService.转换(源.toString(), 目标.toString(), true);

        assertFalse(错误.isEmpty(), "空文件应返回错误信息");
        assertFalse(Files.exists(目标), "失败时不应生成文件");
    }

    // ------------------------------------------------------------------
    // 绝对路径与权限异常处理
    // ------------------------------------------------------------------

    @Test
    @DisplayName("绝对路径（工作区内）应能正常转换")
    void 绝对路径应可转换() throws IOException {
        Path 源 = 写入("绝对路径.csv", "键,值\n甲,1\n乙,2\n");
        Path 目标 = 工作目录.resolve("绝对路径.json").toAbsolutePath();

        String 错误 = ConversionService.转换(源.toAbsolutePath().toString(), 目标.toString(), true);

        assertEquals("", 错误, "绝对路径转换应成功");
        assertTrue(Files.exists(目标), "应在绝对路径生成目标文件");
        assertTrue(读取(目标).contains("\"键\": \"甲\""), "内容应正确");
    }

    @Test
    @DisplayName("目标目录不可写时应返回「无权限写入该目录」而不是空泛的失败")
    void 无权限写入应返回明确中文提示() {
        // 不真的往工作区外写文件：直接驱动错误翻译，验证权限异常会被翻译成可操作的中文提示。
        // 真实场景等价于用户选择 D:\TempArea\temp.csv 这类没有写权限的目录。
        java.io.IOException 异常 = new java.nio.file.AccessDeniedException("D:\\TempArea\\temp.json");
        String 提示 = ConversionService.描述写入失败(异常,
                工作目录.resolve("不存在的目录").resolve("temp.json").toString());

        assertTrue(提示.contains("无权限写入该目录"), "实际提示：" + 提示);
        assertTrue(提示.contains("有权限的目录") || 提示.contains("受限环境") || 提示.contains("启动Jone.cmd"),
                "提示里应给出可操作建议，实际：" + 提示);
    }

    @Test
    @DisplayName("读取被拒时应返回「无权限读取该文件」")
    void 无权限读取应返回明确中文提示() {
        java.io.IOException 异常 = new java.nio.file.AccessDeniedException("D:\\TempArea\\temp.csv");

        String 提示 = ConversionService.描述读取失败(异常, "D:\\TempArea\\temp.csv");

        assertTrue(提示.contains("无权限读取该文件"), "实际提示：" + 提示);
    }

    @Test
    @DisplayName("目标路径上层是文件时应给出中文提示且不崩溃")
    void 目标上层不是目录时报错() throws IOException {
        Path 占位 = 工作目录.resolve("占位文件");
        Files.writeString(占位, "我是一个文件", StandardCharsets.UTF_8);
        Path 源 = 写入("上层测试.csv", "a\n1\n");
        Path 目标 = 占位.resolve("输出.json");

        String 错误 = ConversionService.转换(源.toString(), 目标.toString(), true);

        assertFalse(错误.isEmpty(), "应返回错误信息");
        assertTrue(错误.contains("不是文件夹") || 错误.contains("无权限") || 错误.contains("写入文件失败"),
                "应说明无法写入的具体原因，实际：" + 错误);
        assertFalse(Files.exists(目标), "失败时不应生成目标文件");
    }

    @Test
    @DisplayName("源文件不可读时也应给出中文提示而不是抛出异常")
    void 源文件不存在提示() {
        Path 不存在 = 工作目录.resolve("完全不存在的源文件.csv");

        String 错误 = ConversionService.转换(不存在.toString(),
                工作目录.resolve("输出.json").toString(), true);

        assertTrue(错误.contains("源文件不存在"), "实际：" + 错误);
    }

    // ------------------------------------------------------------------
    // 跨盘符 / 项目外路径的真实写入能力
    // ------------------------------------------------------------------

    @Test
    @DisplayName("应能写入项目之外的跨盘符目录（系统临时目录）")
    void 跨盘符目录写入() throws IOException {
        Path 临时根 = Path.of(System.getProperty("java.io.tmpdir"));
        Assumptions.assumeTrue(Files.isDirectory(临时根), "系统临时目录不可用，跳过该用例");

        Path 外部目录 = 临时根.resolve("jone-跨盘符-" + System.nanoTime());
        try {
            Files.createDirectories(外部目录);
            Path 源 = 写入("跨盘符.csv", "键,值\n甲,1\n乙,2\n");
            Path 目标 = 外部目录.resolve("跨盘符.json");

            String 错误 = ConversionService.转换(源.toString(), 目标.toString(), true);

            assertEquals("", 错误, "写入项目外的跨盘符目录应成功，实际：" + 错误);
            assertTrue(Files.exists(目标), "目标文件应真实存在于系统临时目录：" + 目标);
            assertTrue(读取(目标).contains("\"键\": \"甲\""), "内容应正确");

            // 覆盖模式再写一次，确认覆盖路径同样可用
            assertEquals("", ConversionService.转换(源.toString(), 目标.toString(), true),
                    "覆盖写入应成功");
        } finally {
            临时目录.删除(外部目录);
        }
    }

    @Test
    @DisplayName("写入外部指定目录（通过 -Djone.externalPath 启用）")
    void 外部指定目录写入() throws IOException {
        String 外部目录路径 = System.getProperty("jone.externalPath");
        Assumptions.assumeTrue(外部目录路径 != null && !外部目录路径.isBlank(),
                "未指定 -Djone.externalPath，跳过外部目录用例");

        Path 外部目录 = Path.of(外部目录路径);
        Assumptions.assumeTrue(Files.isDirectory(外部目录), "外部目录不存在，跳过：" + 外部目录路径);

        Path 源 = 写入("外部路径.csv", "键,值\n甲,1\n");
        Path 目标 = 外部目录.resolve("jone外部路径测试-" + System.nanoTime() + ".json");
        try {
            String 错误 = ConversionService.转换(源.toString(), 目标.toString(), true);

            assertEquals("", 错误, "写入外部目录应成功，实际：" + 错误);
            assertTrue(Files.exists(目标), "目标文件应真实存在：" + 目标);
            assertTrue(读取(目标).contains("\"键\": \"甲\""), "内容应正确");
        } finally {
            Files.deleteIfExists(目标);
        }
    }
}
