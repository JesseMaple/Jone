package com.jone.convert;

/**
 * 转换过程中的可预期错误。
 *
 * 用受检异常强制调用方处理，最终由 {@link ConversionService} 统一翻译成中文错误信息返回给界面，
 * 保证 UI 层永远不需要关心异常类型。
 */
class 转换异常 extends Exception {

    private static final long serialVersionUID = 1L;

    转换异常(String 中文原因) {
        super(中文原因);
    }

    转换异常(String 中文原因, Throwable 原因) {
        super(中文原因, 原因);
    }
}
