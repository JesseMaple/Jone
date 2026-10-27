package com.jone.convert;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;

/**
 * YAML 编解码器。
 *
 * 使用 SnakeYAML 的安全构造器（SafeConstructor），只允许解析标准数据类型，
 * 不会因为恶意 YAML 触发任意类实例化。
 */
final class YamlCodec {

    private YamlCodec() {
        throw new UnsupportedOperationException("工具类禁止实例化");
    }

    /**
     * 把 YAML 文本解析为 JDK 原生结构。
     */
    static Object 解析(String 文本) throws 转换异常 {
        if (文本 == null || 文本.isBlank()) {
            throw new 转换异常("YAML 内容为空，无法解析。");
        }
        LoaderOptions 加载选项 = new LoaderOptions();
        加载选项.setAllowDuplicateKeys(false);
        try {
            Yaml 加载器 = new Yaml(new SafeConstructor(加载选项));
            return 加载器.load(文本);
        } catch (YAMLException 异常) {
            throw new 转换异常("YAML 格式非法：" + 简明原因(异常), 异常);
        }
    }

    /**
     * 把原生结构序列化为块状 YAML 文本（保留中文与键顺序）。
     */
    static String 生成(Object 数据) {
        DumperOptions 输出选项 = new DumperOptions();
        输出选项.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        输出选项.setPrettyFlow(true);
        输出选项.setIndent(2);
        输出选项.setAllowUnicode(true);
        输出选项.setLineBreak(DumperOptions.LineBreak.UNIX);
        return new Yaml(输出选项).dump(数据);
    }

    /** 截取异常信息的第一行 */
    private static String 简明原因(Exception 异常) {
        String 信息 = 异常.getMessage();
        if (信息 == null || 信息.isBlank()) {
            return "无法识别的语法错误";
        }
        int 换行 = 信息.indexOf('\n');
        return 换行 > 0 ? 信息.substring(0, 换行) : 信息;
    }
}
