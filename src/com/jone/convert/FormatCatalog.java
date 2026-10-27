package com.jone.convert;

import java.util.List;
import java.util.Locale;

/**
 * 格式目录：维护应用支持的配置格式及其后缀、展示名、文件选择器描述。
 *
 * 对外接口只接收/返回原生数据类型（String、boolean、数组），符合工作区接口规范。
 *
 * 《TODO:后续若要支持 TOML、XML、Properties，只需在此登记格式并补充对应编解码器。》
 */
public final class FormatCatalog {

    /** CSV 格式标识 */
    public static final String CSV = "CSV";
    /** JSON 格式标识 */
    public static final String JSON = "JSON";
    /** YAML 格式标识 */
    public static final String YAML = "YAML";

    private static final List<String> 全部格式清单 = List.of(CSV, JSON, YAML);

    private FormatCatalog() {
        throw new UnsupportedOperationException("工具类禁止实例化");
    }

    /**
     * 返回全部受支持的格式标识（按界面展示顺序）。
     */
    public static String[] 全部格式() {
        return 全部格式清单.toArray(String[]::new);
    }

    /**
     * 判断给定格式标识是否受支持。
     */
    public static boolean 是支持的格式(String 格式) {
        String 标准 = 标准格式名(格式);
        return 全部格式清单.contains(标准);
    }

    /**
     * 把用户输入或配置里的格式名标准化为大写标识；无法识别时返回空字符串。
     */
    public static String 标准格式名(String 格式) {
        if (格式 == null) {
            return "";
        }
        String 标准 = 格式.trim().toUpperCase(Locale.ROOT);
        return 全部格式清单.contains(标准) ? 标准 : "";
    }

    /**
     * 返回格式对应的主后缀名（不含点），例如 CSV 返回 "csv"。
     */
    public static String 后缀(String 格式) {
        return switch (标准格式名(格式)) {
            case CSV -> "csv";
            case JSON -> "json";
            case YAML -> "yaml";
            default -> "";
        };
    }

    /**
     * 返回格式在文件选择器里的过滤描述，例如 "CSV 文件 (*.csv)"。
     */
    public static String 选择器描述(String 格式) {
        String 标准 = 标准格式名(格式);
        if (标准.isEmpty()) {
            return "所有文件 (*.*)";
        }
        return 标准 + " 文件 (*." + 后缀(标准) + ")";
    }

    /**
     * 返回某格式可接受的全部后缀（YAML 同时接受 yml）。
     */
    public static String[] 全部后缀(String 格式) {
        return switch (标准格式名(格式)) {
            case CSV -> new String[]{"csv"};
            case JSON -> new String[]{"json"};
            case YAML -> new String[]{"yaml", "yml"};
            default -> new String[0];
        };
    }

    /**
     * 根据文件名后缀推断格式；无法识别时返回空字符串。
     */
    public static String 根据文件名推断格式(String 文件名或路径) {
        if (文件名或路径 == null || 文件名或路径.isBlank()) {
            return "";
        }
        String 名称 = 文件名或路径.trim();
        int 点位置 = 名称.lastIndexOf('.');
        if (点位置 < 0 || 点位置 == 名称.length() - 1) {
            return "";
        }
        String 后缀名 = 名称.substring(点位置 + 1).toLowerCase(Locale.ROOT);
        return switch (后缀名) {
            case "csv" -> CSV;
            case "json" -> JSON;
            case "yaml", "yml" -> YAML;
            default -> "";
        };
    }
}
