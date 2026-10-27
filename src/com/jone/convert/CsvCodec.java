package com.jone.convert;

import java.util.ArrayList;
import java.util.List;

/**
 * CSV 编解码器：只负责「文本 ↔ 行与字段」的机械转换，不涉及业务语义。
 *
 * 支持特性：UTF-8 BOM 自动剥离、双引号包裹字段、字段内换行、双引号转义（""）。
 */
final class CsvCodec {

    private CsvCodec() {
        throw new UnsupportedOperationException("工具类禁止实例化");
    }

    /**
     * 解析 CSV 文本为二维字段表。
     *
     * @param 文本 CSV 原文
     * @return 行列表，每行是字段列表；全空文本返回空列表
     */
    static List<List<String>> 解析(String 文本) {
        List<List<String>> 行列表 = new ArrayList<>();
        if (文本 == null || 文本.isEmpty()) {
            return 行列表;
        }
        String 内容 = 文本;
        // 剥离 UTF-8 BOM，避免首列表头带上不可见字符
        if (!内容.isEmpty() && 内容.charAt(0) == '\uFEFF') {
            内容 = 内容.substring(1);
        }

        List<String> 当前行 = new ArrayList<>();
        StringBuilder 字段 = new StringBuilder();
        boolean 在引号内 = false;
        boolean 行内有内容 = false;

        for (int 位置 = 0; 位置 < 内容.length(); 位置++) {
            char 字符 = 内容.charAt(位置);

            if (在引号内) {
                if (字符 == '"') {
                    boolean 是转义引号 = 位置 + 1 < 内容.length() && 内容.charAt(位置 + 1) == '"';
                    if (是转义引号) {
                        字段.append('"');
                        位置++;
                    } else {
                        在引号内 = false;
                    }
                } else {
                    字段.append(字符);
                }
                continue;
            }

            switch (字符) {
                case '"' -> {
                    在引号内 = true;
                    行内有内容 = true;
                }
                case ',' -> {
                    当前行.add(字段.toString());
                    字段.setLength(0);
                    行内有内容 = true;
                }
                case '\r' -> {
                    // 忽略 CRLF 中的 CR，由 LF 触发换行
                }
                case '\n' -> {
                    当前行.add(字段.toString());
                    字段.setLength(0);
                    行列表.add(当前行);
                    当前行 = new ArrayList<>();
                    行内有内容 = false;
                }
                default -> {
                    字段.append(字符);
                    行内有内容 = true;
                }
            }
        }

        // 收尾：最后一行没有以换行结束时也要记录
        if (行内有内容 || 字段.length() > 0 || !当前行.isEmpty()) {
            当前行.add(字段.toString());
            行列表.add(当前行);
        }
        return 行列表;
    }

    /**
     * 把二维字段表序列化为 CSV 文本（CRLF 换行，必要时加引号）。
     *
     * @param 行列表     行数据
     * @param 使用BOM    是否在开头写入 UTF-8 BOM（便于 Excel 正确识别中文）
     */
    static String 生成(List<List<String>> 行列表, boolean 使用BOM) {
        StringBuilder 结果 = new StringBuilder();
        if (使用BOM) {
            结果.append('\uFEFF');
        }
        for (int 行号 = 0; 行号 < 行列表.size(); 行号++) {
            List<String> 行 = 行列表.get(行号);
            for (int 列号 = 0; 列号 < 行.size(); 列号++) {
                if (列号 > 0) {
                    结果.append(',');
                }
                结果.append(转义字段(行.get(列号)));
            }
            结果.append("\r\n");
        }
        return 结果.toString();
    }

    /** 按 CSV 规则决定是否给字段加引号 */
    private static String 转义字段(String 值) {
        String 安全值 = 值 == null ? "" : 值;
        boolean 需要引号 = 安全值.indexOf(',') >= 0
                || 安全值.indexOf('"') >= 0
                || 安全值.indexOf('\n') >= 0
                || 安全值.indexOf('\r') >= 0;
        if (!需要引号) {
            return 安全值;
        }
        return '"' + 安全值.replace("\"", "\"\"") + '"';
    }
}
