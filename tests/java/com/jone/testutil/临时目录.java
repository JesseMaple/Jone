package com.jone.testutil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 测试专用临时目录工具。
 *
 * 为什么不用 JUnit 的 @TempDir：
 * 它默认落在系统临时目录（C 盘），会越过工作区隔离红线。
 * 这里统一把测试产物放在项目内 target/test-work 下，测试结束即清理。
 */
public final class 临时目录 {

    private static final AtomicInteger 计数器 = new AtomicInteger(0);

    private 临时目录() {
        throw new UnsupportedOperationException("工具类禁止实例化");
    }

    /**
     * 创建一个项目内的唯一临时目录。
     *
     * @param 场景名 场景名称，用于区分不同测试
     * @return 已创建好的目录路径
     */
    public static Path 创建(String 场景名) throws IOException {
        Path 基目录 = Path.of(System.getProperty("user.dir"), "target", "test-work");
        Path 目录 = 基目录.resolve(场景名 + "-" + 计数器.incrementAndGet());
        Files.createDirectories(目录);
        return 目录;
    }

    /**
     * 递归删除目录，忽略删除过程中的异常。
     */
    public static void 删除(Path 目录) {
        if (目录 == null || !Files.exists(目录)) {
            return;
        }
        try (var 流 = Files.walk(目录)) {
            流.sorted(Comparator.reverseOrder()).forEach(路径 -> {
                try {
                    Files.deleteIfExists(路径);
                } catch (IOException 忽略) {
                    // 清理失败不影响测试结论
                }
            });
        } catch (IOException 忽略) {
            // 清理失败不影响测试结论
        }
    }
}
