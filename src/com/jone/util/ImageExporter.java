package com.jone.util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 图像导出工具：把 JavaFX 场景截取的像素数据写成 PNG 文件。
 *
 * 设计要点：对外接口只接收原生数据类型（宽、高、int 像素数组、路径），
 * 不把 JavaFX 的图像对象传进工具层，符合工作区「接口纯粹性」要求。
 */
public final class ImageExporter {

    private ImageExporter() {
        throw new UnsupportedOperationException("工具类禁止实例化");
    }

    /**
     * 把 ARGB 像素数组保存为 PNG 文件。
     *
     * @param 路径       目标文件路径
     * @param 宽         图像宽度
     * @param 高         图像高度
     * @param 像素数组   按行优先排列的 ARGB 像素
     * @return 是否保存成功
     */
    public static boolean 保存PNG(String 路径, int 宽, int 高, int[] 像素数组) {
        if (路径 == null || 路径.isBlank() || 宽 <= 0 || 高 <= 0 || 像素数组 == null) {
            return false;
        }
        if (像素数组.length < 宽 * 高) {
            return false;
        }
        try {
            Path 目标 = Path.of(路径);
            if (目标.getParent() != null) {
                Files.createDirectories(目标.getParent());
            }
            BufferedImage 图像 = new BufferedImage(宽, 高, BufferedImage.TYPE_INT_ARGB);
            图像.setRGB(0, 0, 宽, 高, 像素数组, 0, 宽);
            return ImageIO.write(图像, "png", new File(路径));
        } catch (IOException 异常) {
            System.out.println("【错误】保存截图失败：" + 异常.getMessage());
            return false;
        }
    }
}
