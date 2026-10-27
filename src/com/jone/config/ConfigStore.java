package com.jone.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * 配置仓库：config.json 是整个应用状态的唯一事实来源。
 *
 * 设计约束（对应 PRD 第二章与工作区接口规范）：
 * 1. 对外方法只接收/返回原生数据类型（String、long、boolean），不暴露任何自定义实体；
 * 2. 每次写入都“先落盘、后返回”，调用方拿到 true 时配置一定已经写进磁盘；
 * 3. 写入采用「临时文件 + 原子替换」，避免进程崩溃留下半截 JSON。
 *
 * 《TODO:后续若配置项膨胀，可增加按模块分区的读写接口。》
 */
public final class ConfigStore {

    /** 默认配置文件名 */
    public static final String 默认配置文件名 = "config.json";

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private ConfigStore() {
        throw new UnsupportedOperationException("工具类禁止实例化");
    }

    /**
     * 返回当前工作目录下的默认配置文件路径。
     * 应用通过 mvn javafx:run 启动时，工作目录即项目根目录。
     */
    public static String 默认配置路径() {
        return Path.of(System.getProperty("user.dir"), 默认配置文件名).toString();
    }

    /**
     * 确保配置文件存在：不存在则创建包含默认值的文件。
     *
     * @param 配置文件路径 配置文件绝对或相对路径
     * @return 空字符串表示成功；否则为中文错误信息
     */
    public static String 确保存在(String 配置文件路径) {
        if (配置文件路径 == null || 配置文件路径.isBlank()) {
            return "配置文件路径为空，无法初始化配置。";
        }
        Path 文件 = Path.of(配置文件路径);
        try {
            if (!Files.exists(文件)) {
                if (!写入整个对象(文件, 构建默认配置())) {
                    return "创建默认配置文件失败：" + 文件;
                }
                return "";
            }
            String 内容 = Files.readString(文件, StandardCharsets.UTF_8);
            if (内容.isBlank()) {
                // 空文件视为未初始化，直接写入默认值
                if (!写入整个对象(文件, 构建默认配置())) {
                    return "初始化空白配置文件失败：" + 文件;
                }
                return "";
            }
            解析(内容); // 仅做格式校验
            return "";
        } catch (JsonSyntaxException 异常) {
            return "配置文件格式错误，请修复或删除后重试：" + 文件.getFileName() + "（" + 异常.getMessage() + "）";
        } catch (IOException 异常) {
            return "读取配置文件失败：" + 异常.getMessage();
        }
    }

    /**
     * 读取文本型配置项。键使用点号分隔层级，例如 {@code theme.presetId}。
     *
     * @param 配置文件路径 配置文件路径
     * @param 键           点号分隔的配置键
     * @param 默认值       配置缺失或类型不符时返回的默认值
     */
    public static String 读取(String 配置文件路径, String 键, String 默认值) {
        JsonElement 节点 = 定位(读入(配置文件路径), 键);
        if (节点 == null || !节点.isJsonPrimitive()) {
            return 默认值;
        }
        return 节点.getAsString();
    }

    /**
     * 读取数值型配置项。
     *
     * @param 配置文件路径 配置文件路径
     * @param 键           点号分隔的配置键
     * @param 默认值       配置缺失或不是数字时返回的默认值
     */
    public static long 读取数值(String 配置文件路径, String 键, long 默认值) {
        JsonElement 节点 = 定位(读入(配置文件路径), 键);
        if (节点 == null || !节点.isJsonPrimitive()) {
            return 默认值;
        }
        try {
            return 节点.getAsLong();
        } catch (NumberFormatException | UnsupportedOperationException 异常) {
            return 默认值;
        }
    }

    /**
     * 读取布尔型配置项。
     *
     * @param 配置文件路径 配置文件路径
     * @param 键           点号分隔的配置键
     * @param 默认值       配置缺失或不是布尔值时返回的默认值
     */
    public static boolean 读取布尔(String 配置文件路径, String 键, boolean 默认值) {
        JsonElement 节点 = 定位(读入(配置文件路径), 键);
        if (节点 == null || !节点.isJsonPrimitive()) {
            return 默认值;
        }
        try {
            return 节点.getAsBoolean();
        } catch (UnsupportedOperationException 异常) {
            return 默认值;
        }
    }

    /**
     * 写入文本型配置项（重载之一）。写成功后配置已落盘。
     */
    public static boolean 写入(String 配置文件路径, String 键, String 值) {
        return 写入任意(配置文件路径, 键, new JsonPrimitive(值 == null ? "" : 值));
    }

    /**
     * 写入数值型配置项（重载之二）。写成功后配置已落盘。
     */
    public static boolean 写入(String 配置文件路径, String 键, long 值) {
        return 写入任意(配置文件路径, 键, new JsonPrimitive(值));
    }

    /**
     * 写入布尔型配置项（重载之三）。写成功后配置已落盘。
     */
    public static boolean 写入(String 配置文件路径, String 键, boolean 值) {
        return 写入任意(配置文件路径, 键, new JsonPrimitive(值));
    }

    // ------------------------------------------------------------------
    // 内部实现
    // ------------------------------------------------------------------

    /** 读取并解析整个配置文件；文件不存在或格式错误时返回空对象 */
    private static JsonObject 读入(String 配置文件路径) {
        if (配置文件路径 == null || 配置文件路径.isBlank()) {
            return new JsonObject();
        }
        Path 文件 = Path.of(配置文件路径);
        if (!Files.exists(文件)) {
            return new JsonObject();
        }
        try {
            String 内容 = Files.readString(文件, StandardCharsets.UTF_8);
            if (内容.isBlank()) {
                return new JsonObject();
            }
            return 解析(内容);
        } catch (IOException | JsonSyntaxException 异常) {
            System.out.println("【警告】配置文件读取失败，已回退为默认值：" + 异常.getMessage());
            return new JsonObject();
        }
    }

    /** 把 JSON 文本解析为对象，格式非法时抛出异常 */
    private static JsonObject 解析(String 内容) {
        JsonElement 根 = JsonParser.parseString(内容);
        if (!根.isJsonObject()) {
            throw new JsonSyntaxException("配置文件根节点必须是 JSON 对象");
        }
        return 根.getAsJsonObject();
    }

    /** 按点号路径定位节点，任一层级缺失即返回 null */
    private static JsonElement 定位(JsonObject 根, String 键) {
        if (根 == null || 键 == null || 键.isBlank()) {
            return null;
        }
        String[] 片段 = 键.split("\\.");
        JsonElement 当前 = 根;
        for (String 片段名 : 片段) {
            if (当前 == null || !当前.isJsonObject()) {
                return null;
            }
            JsonObject 对象 = 当前.getAsJsonObject();
            if (!对象.has(片段名)) {
                return null;
            }
            当前 = 对象.get(片段名);
        }
        return 当前;
    }

    /** 写入任意 JSON 值：读改写 + 原子落盘 */
    private static boolean 写入任意(String 配置文件路径, String 键, JsonElement 值) {
        if (配置文件路径 == null || 配置文件路径.isBlank() || 键 == null || 键.isBlank()) {
            System.out.println("【错误】写入配置失败：路径或键为空。");
            return false;
        }
        Path 文件 = Path.of(配置文件路径);
        JsonObject 根 = 读入(配置文件路径);
        if (根 == null) {
            根 = new JsonObject();
        }
        if (!赋值(根, 键, 值)) {
            System.out.println("【错误】写入配置失败：键路径非法（" + 键 + "）。");
            return false;
        }
        return 写入整个对象(文件, 根);
    }

    /** 在点号路径上赋值，缺失的中间节点自动创建 */
    private static boolean 赋值(JsonObject 根, String 键, JsonElement 值) {
        String[] 片段 = 键.split("\\.");
        JsonObject 当前 = 根;
        for (int 序号 = 0; 序号 < 片段.length - 1; 序号++) {
            String 片段名 = 片段[序号];
            if (片段名.isBlank()) {
                return false;
            }
            JsonElement 子节点 = 当前.get(片段名);
            if (子节点 == null || !子节点.isJsonObject()) {
                子节点 = new JsonObject();
                当前.add(片段名, 子节点);
            }
            当前 = 子节点.getAsJsonObject();
        }
        String 末级 = 片段[片段.length - 1];
        if (末级.isBlank()) {
            return false;
        }
        当前.add(末级, 值);
        return true;
    }

    /** 把整棵 JSON 对象原子写入磁盘：先写临时文件，再整体替换 */
    private static boolean 写入整个对象(Path 文件, JsonObject 内容) {
        try {
            if (文件.getParent() != null) {
                Files.createDirectories(文件.getParent());
            }
            Path 临时文件 = 文件.resolveSibling(文件.getFileName() + ".tmp");
            Files.writeString(临时文件, GSON.toJson(内容), StandardCharsets.UTF_8);
            try {
                Files.move(临时文件, 文件, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException 异常) {
                // 个别文件系统不支持原子替换，退化为普通替换，仍然先落盘再返回
                Files.move(临时文件, 文件, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException 异常) {
            System.out.println("【错误】写入配置文件失败：" + 异常.getMessage());
            return false;
        }
    }

    /** 构建默认配置内容 */
    private static JsonObject 构建默认配置() {
        JsonObject 主题 = new JsonObject();
        主题.addProperty("mode", "preset");
        主题.addProperty("presetId", "deepseekBlue");
        主题.addProperty("customHex", "#4D6BFE");
        主题.addProperty("followSystem", false);

        JsonObject 窗口 = new JsonObject();
        窗口.addProperty("width", 1000);
        窗口.addProperty("height", 700);
        窗口.addProperty("maximized", false);

        JsonObject 转换 = new JsonObject();
        转换.addProperty("sourceFormat", "CSV");
        转换.addProperty("targetFormat", "JSON");

        JsonObject 根 = new JsonObject();
        根.addProperty("version", 1);
        根.add("theme", 主题);
        根.addProperty("lastPage", "home");
        根.add("window", 窗口);
        根.add("converter", 转换);
        return 根;
    }
}
