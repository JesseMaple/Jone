package com.jone.convert;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 转换服务：应用唯一的格式转换入口，界面层只与它打交道。
 *
 * 接口约束（PRD 第四章）：
 * - 所有对外方法只接收 String / boolean 等原生类型，不传任何自定义实体；
 * - 所有异常都在内部消化，只向调用方返回空字符串（成功）或中文错误信息（失败）。
 *
 * 转换语义：
 * - CSV ↔ JSON ↔ YAML 任意两两互转，同格式直接拦截；
 * - CSV 以表头作为对象字段名，值为字符串；JSON/YAML 转 CSV 时表头取所有行键的并集；
 * - 覆盖参数为 false 且目标已存在时，自动在文件名后追加 _1、_2 递增序号。
 *
 * 《TODO:后续可增加批量目录转换接口，一次处理多个文件。》
 */
public final class ConversionService {

    private static final Gson 紧凑输出器 = new GsonBuilder()
            .serializeNulls()
            .disableHtmlEscaping()
            .create();

    private ConversionService() {
        throw new UnsupportedOperationException("工具类禁止实例化");
    }

    // ------------------------------------------------------------------
    // 校验类接口
    // ------------------------------------------------------------------

    /**
     * 校验一对源/目标格式是否允许转换。
     *
     * @return 空字符串表示允许；否则是中文原因
     */
    public static String 校验(String 源格式, String 目标格式) {
        String 源 = FormatCatalog.标准格式名(源格式);
        String 目标 = FormatCatalog.标准格式名(目标格式);
        if (源.isEmpty()) {
            return "不支持源格式：" + (源格式 == null ? "空" : 源格式);
        }
        if (目标.isEmpty()) {
            return "不支持目标格式：" + (目标格式 == null ? "空" : 目标格式);
        }
        if (源.equals(目标)) {
            return "源格式与目标格式不能相同（" + 源 + "）。";
        }
        return "";
    }

    /**
     * 判断一对格式是否允许转换。
     */
    public static boolean 可转换(String 源格式, String 目标格式) {
        return 校验(源格式, 目标格式).isEmpty();
    }

    // ------------------------------------------------------------------
    // 路径计算接口
    // ------------------------------------------------------------------

    /**
     * 根据源文件路径与目标格式，推测默认输出路径（与源文件同目录、同主文件名、换后缀）。
     *
     * @return 输出路径；无法推断时返回空字符串
     */
    public static String 推测输出路径(String 输入路径, String 目标格式) {
        String 后缀 = FormatCatalog.后缀(目标格式);
        if (输入路径 == null || 输入路径.isBlank() || 后缀.isEmpty()) {
            return "";
        }
        try {
            Path 源 = Path.of(输入路径);
            String 文件名 = 源.getFileName() == null ? "" : 源.getFileName().toString();
            int 点位置 = 文件名.lastIndexOf('.');
            String 主名 = 点位置 > 0 ? 文件名.substring(0, 点位置) : 文件名;
            String 新文件名 = 主名 + "." + 后缀;
            Path 目录 = 源.getParent();
            Path 结果 = 目录 == null ? Path.of(新文件名) : 目录.resolve(新文件名);
            return 结果.toString();
        } catch (InvalidPathException 异常) {
            return "";
        }
    }

    /**
     * 按「是否覆盖」规则解决同名冲突。
     * 覆盖为 true 或目标不存在时原样返回；否则追加 _1、_2……直到不冲突。
     */
    public static String 解决命名冲突(String 期望输出路径, boolean 覆盖) {
        if (期望输出路径 == null || 期望输出路径.isBlank()) {
            return "";
        }
        try {
            Path 期望 = Path.of(期望输出路径);
            if (覆盖 || !Files.exists(期望)) {
                return 期望.toString();
            }
            String 文件名 = 期望.getFileName() == null ? "" : 期望.getFileName().toString();
            int 点位置 = 文件名.lastIndexOf('.');
            String 主名 = 点位置 > 0 ? 文件名.substring(0, 点位置) : 文件名;
            String 扩展名 = 点位置 > 0 ? 文件名.substring(点位置) : "";
            Path 目录 = 期望.getParent();
            for (int 序号 = 1; 序号 <= 9999; 序号++) {
                String 候选名 = 主名 + "_" + 序号 + 扩展名;
                Path 候选 = 目录 == null ? Path.of(候选名) : 目录.resolve(候选名);
                if (!Files.exists(候选)) {
                    return 候选.toString();
                }
            }
            return 期望.toString();
        } catch (InvalidPathException 异常) {
            return 期望输出路径;
        }
    }

    /**
     * 计算一次转换实际会写入的文件路径（供界面提示使用）。
     */
    public static String 计算实际输出路径(String 输入路径, String 目标格式, boolean 覆盖) {
        return 解决命名冲突(推测输出路径(输入路径, 目标格式), 覆盖);
    }

    // ------------------------------------------------------------------
    // 转换接口
    // ------------------------------------------------------------------

    /**
     * 按源文件所在目录与目标格式自动决定输出路径并转换。
     *
     * @return 空字符串表示成功；否则是中文错误信息
     */
    public static String 按格式转换(String 输入路径, String 目标格式, boolean 覆盖) {
        String 输出路径 = 推测输出路径(输入路径, 目标格式);
        if (输出路径.isEmpty()) {
            return "无法推断输出路径，请检查源文件路径与目标格式。";
        }
        return 转换(输入路径, 输出路径, 覆盖);
    }

    /**
     * 转换主接口：把源文件转换为目标格式并写入指定路径。
     *
     * @param 输入路径 源文件路径
     * @param 输出路径 期望的输出路径（同目录冲突时可能被自动改名）
     * @param 覆盖     目标已存在时是否直接覆盖
     * @return 空字符串表示成功；否则是中文错误信息
     */
    public static String 转换(String 输入路径, String 输出路径, boolean 覆盖) {
        try {
            return 执行转换(输入路径, 输出路径, 覆盖);
        } catch (转换异常 异常) {
            // 只有 IO 类问题（权限、占用、路径非法）才把底层堆栈打到终端；
            // 用户输入格式错误属于预期情况，堆栈刷屏没有意义。
            Throwable 原因 = 异常.getCause();
            if (原因 instanceof IOException) {
                原因.printStackTrace();
            }
            return 异常.getMessage();
        } catch (InvalidPathException 异常) {
            异常.printStackTrace();
            return "文件路径非法：" + 异常.getInput();
        } catch (IOException 异常) {
            异常.printStackTrace();
            return "文件读写失败：" + 异常.getMessage();
        } catch (RuntimeException 异常) {
            异常.printStackTrace();
            return "转换失败（内部异常）：" + 异常.getClass().getSimpleName()
                    + (异常.getMessage() == null ? "" : " - " + 异常.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // 内部实现
    // ------------------------------------------------------------------

    /** 真正执行转换的流程 */
    private static String 执行转换(String 输入路径, String 输出路径, boolean 覆盖)
            throws 转换异常, IOException {

        if (输入路径 == null || 输入路径.isBlank()) {
            throw new 转换异常("请选择要转换的文件。");
        }
        if (输出路径 == null || 输出路径.isBlank()) {
            throw new 转换异常("输出路径为空，无法转换。");
        }

        Path 源文件 = Path.of(输入路径);
        if (!Files.exists(源文件)) {
            throw new 转换异常("源文件不存在：" + 输入路径);
        }
        if (Files.isDirectory(源文件)) {
            throw new 转换异常("源路径是文件夹，请选择具体文件：" + 输入路径);
        }

        String 源格式 = FormatCatalog.根据文件名推断格式(输入路径);
        String 目标格式 = FormatCatalog.根据文件名推断格式(输出路径);

        // 先拦住「输出就是源文件本身」这种自毁式操作，再谈格式合法性
        Path 期望输出 = Path.of(输出路径);
        if (源文件.toAbsolutePath().normalize().equals(期望输出.toAbsolutePath().normalize())) {
            throw new 转换异常("输出路径不能与源文件相同。");
        }

        String 校验结果 = 校验(源格式, 目标格式);
        if (!校验结果.isEmpty()) {
            throw new 转换异常(校验结果);
        }

        Path 最终输出 = Path.of(解决命名冲突(输出路径, 覆盖));
        if (最终输出.getParent() != null) {
            try {
                Files.createDirectories(最终输出.getParent());
            } catch (IOException 异常) {
                throw new 转换异常(描述写入失败(异常, 最终输出.toString()), 异常);
            }
        }

        String 源文本;
        try {
            源文本 = 读取文本(源文件);
        } catch (IOException 异常) {
            throw new 转换异常(描述读取失败(异常, 输入路径), 异常);
        }

        Object 数据 = 解析(源格式, 源文本);
        String 目标文本 = 序列化(目标格式, 数据);

        try {
            Files.writeString(最终输出, 目标文本, StandardCharsets.UTF_8);
        } catch (IOException 异常) {
            throw new 转换异常(描述写入失败(异常, 最终输出.toString()), 异常);
        }
        return "";
    }

    /**
     * 把读取阶段的 IO 异常翻译成用户能照着处理的中文提示。
     *
     * @param 异常     原始异常
     * @param 源路径   读取的源文件路径
     */
    static String 描述读取失败(IOException 异常, String 源路径) {
        if (异常 instanceof java.nio.file.AccessDeniedException) {
            return "无权限读取该文件：" + 源路径 + "（文件被占用、只读或权限不足，请复制到有权限的目录后重试）";
        }
        if (异常 instanceof java.nio.file.NoSuchFileException) {
            return "源文件不存在：" + 源路径;
        }
        return "读取源文件失败：" + 源路径 + "（" + 异常.getMessage() + "）";
    }

    /**
     * 把写入阶段的 IO 异常翻译成用户能照着处理的中文提示。
     *
     * 注意：这里不做任何路径白名单或权限预判，写入永远直接交给 Files.writeString；
     * 本方法只负责在系统真的拒绝之后，把原因说清楚。
     *
     * @param 异常     原始异常
     * @param 目标路径 期望写入的文件路径
     */
    static String 描述写入失败(IOException 异常, String 目标路径) {
        if (异常 instanceof java.nio.file.AccessDeniedException) {
            Path 目标 = Path.of(目标路径);
            if (Files.exists(目标)) {
                return "无权限覆盖该文件：" + 目标路径 + "（文件只读或被其他程序占用，请关闭占用程序后重试）";
            }
            Path 目录 = 目标.getParent();
            String 目录文本 = 目录 == null ? 目标路径 : 目录.toString();
            if (com.jone.util.运行环境诊断.是否为受限环境()) {
                return "无权限写入该目录：" + 目录文本
                        + "（" + com.jone.util.运行环境诊断.环境说明()
                        + "；请改用普通 PowerShell 窗口启动，或双击项目根目录的 启动Jone.cmd）";
            }
            return "无权限写入该目录：" + 目录文本
                    + "（系统拒绝写入，请换到有权限的目录，例如项目内的 test_data）";
        }
        if (异常 instanceof java.nio.file.FileAlreadyExistsException) {
            return "目标路径的上层不是文件夹，无法写入：" + 目标路径;
        }
        if (异常 instanceof java.nio.file.FileSystemException 文件异常 && 文件异常.getReason() != null) {
            return "写入文件失败：" + 目标路径 + "（" + 文件异常.getReason() + "）";
        }
        return "写入文件失败：" + 目标路径 + "（" + 异常.getMessage() + "）";
    }

    /** 读取文本并剥离 UTF-8 BOM */
    private static String 读取文本(Path 文件) throws IOException {
        String 文本 = Files.readString(文件, StandardCharsets.UTF_8);
        if (!文本.isEmpty() && 文本.charAt(0) == '\uFEFF') {
            return 文本.substring(1);
        }
        return 文本;
    }

    /** 按格式把文本解析为原生数据结构 */
    private static Object 解析(String 格式, String 文本) throws 转换异常 {
        return switch (格式) {
            case FormatCatalog.CSV -> 解析CSV(文本);
            case FormatCatalog.JSON -> JsonCodec.解析(文本);
            case FormatCatalog.YAML -> YamlCodec.解析(文本);
            default -> throw new 转换异常("不支持的源格式：" + 格式);
        };
    }

    /**
     * CSV 解析：第一行作为表头，其余行转换为「键值对象」，
     * 这样 CSV、JSON、YAML 三条链路共用同一种数据结构。
     */
    private static Object 解析CSV(String 文本) throws 转换异常 {
        List<List<String>> 表格 = CsvCodec.解析(文本);
        if (表格.isEmpty()) {
            throw new 转换异常("CSV 内容为空，无法解析。");
        }

        // 计算最大列数：允许后续行比表头更宽，多出来的列自动补名
        int 列数 = 0;
        for (List<String> 行 : 表格) {
            列数 = Math.max(列数, 行.size());
        }
        if (列数 == 0) {
            throw new 转换异常("CSV 内容为空，无法解析。");
        }

        // 归一化表头：空表头补「列N」，重名追加序号，避免字段互相覆盖
        List<String> 原始表头 = 表格.get(0);
        List<String> 表头 = new ArrayList<>(列数);
        Set<String> 已用名称 = new java.util.HashSet<>();
        for (int 列 = 0; 列 < 列数; 列++) {
            String 名称 = 列 < 原始表头.size() ? 原始表头.get(列).trim() : "";
            if (名称.isEmpty()) {
                名称 = "列" + (列 + 1);
            }
            String 唯一名称 = 名称;
            int 序号 = 2;
            while (!已用名称.add(唯一名称)) {
                唯一名称 = 名称 + "_" + 序号;
                序号++;
            }
            表头.add(唯一名称);
        }

        List<Map<String, Object>> 行对象列表 = new ArrayList<>();
        for (int 行号 = 1; 行号 < 表格.size(); 行号++) {
            List<String> 行 = 表格.get(行号);
            // 跳过纯空行
            if (行.size() == 1 && 行.get(0).isBlank()) {
                continue;
            }
            Map<String, Object> 行对象 = new LinkedHashMap<>();
            for (int 列 = 0; 列 < 表头.size(); 列++) {
                String 值 = 列 < 行.size() ? 行.get(列) : "";
                行对象.put(表头.get(列), 值);
            }
            行对象列表.add(行对象);
        }
        return 行对象列表;
    }

    /** 按目标格式序列化数据结构 */
    private static String 序列化(String 格式, Object 数据) throws 转换异常 {
        return switch (格式) {
            case FormatCatalog.CSV -> 生成CSV(数据);
            case FormatCatalog.JSON -> JsonCodec.生成(数据);
            case FormatCatalog.YAML -> YamlCodec.生成(数据);
            default -> throw new 转换异常("不支持的目标格式：" + 格式);
        };
    }

    /** 把「行列表」结构生成为 CSV 文本 */
    private static String 生成CSV(Object 数据) throws 转换异常 {
        List<Map<String, Object>> 行列表 = 提取表格(数据);

        Set<String> 表头集合 = new LinkedHashSet<>();
        for (Map<String, Object> 行 : 行列表) {
            表头集合.addAll(行.keySet());
        }
        if (表头集合.isEmpty()) {
            throw new 转换异常("没有可写入 CSV 的字段。");
        }
        List<String> 表头 = new ArrayList<>(表头集合);

        List<List<String>> 表格 = new ArrayList<>();
        表格.add(表头);
        for (Map<String, Object> 行 : 行列表) {
            List<String> 字段行 = new ArrayList<>(表头.size());
            for (String 列名 : 表头) {
                字段行.add(转单元格文本(行.get(列名)));
            }
            表格.add(字段行);
        }
        return CsvCodec.生成(表格, true);
    }

    /** 把解析结果规整为「每行一个键值表」的表格结构 */
    private static List<Map<String, Object>> 提取表格(Object 数据) throws 转换异常 {
        if (数据 == null) {
            throw new 转换异常("内容为空，没有可转换的数据。");
        }
        if (!(数据 instanceof List<?> 列表)) {
            throw new 转换异常("转换为 CSV 要求根节点是对象数组，当前内容是 "
                    + 描述类型(数据.getClass().getName()) + "。");
        }
        if (列表.isEmpty()) {
            throw new 转换异常("数组为空，没有可转换的数据行。");
        }
        List<Map<String, Object>> 结果 = new ArrayList<>(列表.size());
        int 序号 = 0;
        for (Object 项 : 列表) {
            序号++;
            if (!(项 instanceof Map<?, ?> 映射)) {
                throw new 转换异常("数组第 " + 序号 + " 项不是对象，无法写入 CSV。");
            }
            Map<String, Object> 行 = new LinkedHashMap<>();
            for (Map.Entry<?, ?> 条目 : 映射.entrySet()) {
                行.put(String.valueOf(条目.getKey()), 条目.getValue());
            }
            结果.add(行);
        }
        return 结果;
    }

    /** 把任意单元格值转为 CSV 文本 */
    private static String 转单元格文本(Object 值) {
        if (值 == null) {
            return "";
        }
        if (值 instanceof String 文本) {
            return 文本;
        }
        if (值 instanceof Boolean 布尔值) {
            return 布尔值 ? "true" : "false";
        }
        if (值 instanceof Double 小数) {
            if (!小数.isNaN() && !小数.isInfinite() && 小数 == Math.floor(小数)) {
                return String.valueOf(小数.longValue());
            }
            return String.valueOf(小数);
        }
        if (值 instanceof BigDecimal 精确小数) {
            return 精确小数.toPlainString();
        }
        if (值 instanceof Number || 值 instanceof Character) {
            return String.valueOf(值);
        }
        if (值 instanceof Map<?, ?> || 值 instanceof List<?>) {
            return 紧凑输出器.toJson(值);
        }
        return String.valueOf(值);
    }

    /** 用于错误提示的类型说明 */
    private static String 描述类型(String 类型名) {
        return switch (类型名) {
            case "java.util.LinkedHashMap", "java.util.HashMap", "java.util.LinkedTreeMap" -> "对象（JSON 大括号或 YAML 映射）";
            case "java.lang.String" -> "字符串";
            case "java.lang.Long", "java.lang.Integer", "java.lang.Double", "java.math.BigDecimal" -> "数字";
            case "java.lang.Boolean" -> "布尔值";
            default -> "类型 " + 类型名;
        };
    }
}
