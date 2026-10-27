package com.jone.theme;

import com.jthemedetecor.OsThemeDetector;

/**
 * 系统深浅色监听：包装 jSystemThemeDetector，屏蔽它可能抛出的各种底层异常。
 *
 * 为什么单独抽一层：
 * - 第三方库依赖 JNA 读取系统设置，在受限环境里可能失败；
 * - 界面层只关心「现在是不是深色」和「变了通知我」，不需要知道底层实现。
 *
 * 《TODO:后续若要支持 Linux 的 GTK 主题，可在此按操作系统分支处理。》
 */
public final class 系统主题监听 {

    /** 深色状态变化回调（只传原生 boolean） */
    public interface 监听器 {
        void 变化(boolean 深色);
    }

    private static OsThemeDetector 检测器;
    private static boolean 上次结果 = false;
    private static boolean 曾经成功 = false;

    private 系统主题监听() {
        throw new UnsupportedOperationException("工具类禁止实例化");
    }

    /**
     * 查询系统当前是否为深色。检测失败时返回上一次的已知结果（默认为浅色）。
     */
    public static synchronized boolean 当前是否深色() {
        try {
            boolean 深色 = 检测器().isDark();
            上次结果 = 深色;
            曾经成功 = true;
            return 深色;
        } catch (Throwable 异常) {
            System.out.println("【警告】系统主题检测失败，暂按浅色处理：" + 异常.getMessage());
            return 曾经成功 && 上次结果;
        }
    }

    /**
     * 注册系统主题变化监听；注册失败只记录日志，不影响应用运行。
     */
    public static synchronized void 注册(监听器 监听) {
        if (监听 == null) {
            return;
        }
        try {
            检测器().registerListener(深色 -> {
                上次结果 = 深色;
                曾经成功 = true;
                try {
                    监听.变化(深色);
                } catch (RuntimeException 异常) {
                    System.out.println("【警告】处理系统主题变化时出错：" + 异常.getMessage());
                }
            });
        } catch (Throwable 异常) {
            System.out.println("【警告】系统主题监听未启用：" + 异常.getMessage());
        }
    }

    /** 懒加载检测器，避免在没有必要的时候初始化 JNA */
    private static OsThemeDetector 检测器() {
        if (检测器 == null) {
            检测器 = OsThemeDetector.getDetector();
        }
        return 检测器;
    }
}
