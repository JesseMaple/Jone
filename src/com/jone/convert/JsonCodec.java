package com.jone.convert;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JSON 编解码器。
 *
 * 解析结果统一转换为 JDK 原生结构（LinkedHashMap / ArrayList / String / Long / Double / Boolean / null），
 * 这样 JSON、YAML 两条链路可以共用同一套数据结构转换逻辑，避免格式之间各自为政。
 */
final class JsonCodec {

    private static final Gson 输出器 = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .disableHtmlEscaping()
            .create();

    private JsonCodec() {
        throw new UnsupportedOperationException("工具类禁止实例化");
    }

    /**
     * 把 JSON 文本解析为原生结构。
     */
    static Object 解析(String 文本) throws 转换异常 {
        if (文本 == null || 文本.isBlank()) {
            throw new 转换异常("JSON 内容为空，无法解析。");
        }
        try {
            JsonElement 根 = JsonParser.parseString(文本);
            return 转原生(根);
        } catch (JsonSyntaxException 异常) {
            throw new 转换异常("JSON 格式非法：" + 简明原因(异常), 异常);
        }
    }

    /**
     * 把原生结构序列化为带缩进的 JSON 文本（保留中文）。
     */
    static String 生成(Object 数据) {
        return 输出器.toJson(转Json(数据));
    }

    /** JsonElement → Map/List/原生值 */
    private static Object 转原生(JsonElement 元素) {
        if (元素 == null || 元素.isJsonNull()) {
            return null;
        }
        if (元素.isJsonObject()) {
            JsonObject 对象 = 元素.getAsJsonObject();
            Map<String, Object> 结果 = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> 条目 : 对象.entrySet()) {
                结果.put(条目.getKey(), 转原生(条目.getValue()));
            }
            return 结果;
        }
        if (元素.isJsonArray()) {
            JsonArray 数组 = 元素.getAsJsonArray();
            ArrayList<Object> 结果 = new ArrayList<>(数组.size());
            for (JsonElement 子元素 : 数组) {
                结果.add(转原生(子元素));
            }
            return 结果;
        }
        JsonPrimitive 原始值 = 元素.getAsJsonPrimitive();
        if (原始值.isBoolean()) {
            return 原始值.getAsBoolean();
        }
        if (原始值.isNumber()) {
            String 数字文本 = 原始值.getAsString();
            // 整数值保持整数类型，避免 1 变成 1.0
            if (数字文本.indexOf('.') < 0 && 数字文本.indexOf('e') < 0 && 数字文本.indexOf('E') < 0) {
                try {
                    return Long.parseLong(数字文本);
                } catch (NumberFormatException 忽略) {
                    return new BigDecimal(数字文本);
                }
            }
            return Double.parseDouble(数字文本);
        }
        return 原始值.getAsString();
    }

    /** 原生结构 → JsonElement */
    private static JsonElement 转Json(Object 值) {
        if (值 == null) {
            return com.google.gson.JsonNull.INSTANCE;
        }
        if (值 instanceof Map<?, ?> 映射) {
            JsonObject 对象 = new JsonObject();
            for (Map.Entry<?, ?> 条目 : 映射.entrySet()) {
                对象.add(String.valueOf(条目.getKey()), 转Json(条目.getValue()));
            }
            return 对象;
        }
        if (值 instanceof Iterable<?> 集合) {
            JsonArray 数组 = new JsonArray();
            for (Object 子元素 : 集合) {
                数组.add(转Json(子元素));
            }
            return 数组;
        }
        if (值 instanceof Number 数字) {
            return new JsonPrimitive(数字);
        }
        if (值 instanceof Boolean 布尔值) {
            return new JsonPrimitive(布尔值);
        }
        return new JsonPrimitive(String.valueOf(值));
    }

    /** 截取异常信息的第一行，避免把整段堆栈式描述甩给用户 */
    private static String 简明原因(Exception 异常) {
        String 信息 = 异常.getMessage();
        if (信息 == null || 信息.isBlank()) {
            return "无法识别的语法错误";
        }
        int 换行 = 信息.indexOf('\n');
        return 换行 > 0 ? 信息.substring(0, 换行) : 信息;
    }
}
