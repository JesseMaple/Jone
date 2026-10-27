package com.jone.util;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 运行环境诊断：判断当前进程是否运行在受限环境（低完整性/沙箱）中。
 *
 * 背景：
 * 本应用严格意义上没有任何路径白名单或权限预判，所有写入都直接交给 Files.writeString。
 * 但 Windows 存在「完整性级别」机制：当 Jone 从低完整性进程（例如某些智能终端/沙箱）启动时，
 * 子进程会继承低完整性令牌，即使当前账户对目标目录有完全控制权限，系统也会拒绝「向上写入」，
 * 表现为 AccessDeniedException。
 *
 * 本类只在错误诊断时使用：向用户说明真实原因，绝不参与放行或拦截任何转换请求。
 *
 * 《TODO:后续若接入更多沙箱形态，可在此扩展更精确的完整性级别读取（JNA）。》
 */
public final class 运行环境诊断 {

    private static Boolean 缓存结果;

    private 运行环境诊断() {
        throw new UnsupportedOperationException("工具类禁止实例化");
    }

    /**
     * 是否运行在受限环境。
     *
     * 判定方式：尝试在用户主目录写入并删除一个探针文件。
     * 正常账号一定可写主目录；若被拒绝，说明进程被降权（低完整性/沙箱），
     * 这正好解释了「账户有权限但程序写不进去」的现象。
     */
    public static synchronized boolean 是否为受限环境() {
        if (缓存结果 != null) {
            return 缓存结果;
        }
        缓存结果 = 探测受限();
        return 缓存结果;
    }

    /** 返回一段可直接展示给用户的环境说明 */
    public static String 环境说明() {
        return 是否为受限环境()
                ? "当前 Jone 运行在受限环境（低完整性/沙箱），只能写入其授权目录"
                : "当前 Jone 运行在普通用户环境";
    }

    private static boolean 探测受限() {
        String 主目录 = System.getProperty("user.home");
        if (主目录 == null || 主目录.isBlank()) {
            return false;
        }
        Path 探针 = null;
        try {
            探针 = Path.of(主目录, ".jone-write-probe-" + System.nanoTime() + ".tmp");
            Files.writeString(探针, "probe");
            Files.deleteIfExists(探针);
            return false;
        } catch (AccessDeniedException 异常) {
            return true;
        } catch (IOException 异常) {
            // 其它 IO 异常（磁盘满、网络盘不可达）不归为受限环境
            return false;
        } finally {
            if (探针 != null) {
                try {
                    Files.deleteIfExists(探针);
                } catch (IOException 忽略) {
                    // 清理失败不影响判定结果
                }
            }
        }
    }
}
