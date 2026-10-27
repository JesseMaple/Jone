# Jone - 跨格式配置文件转换工具

![Java](https://img.shields.io/badge/Java-25-orange.svg)
![JavaFX](https://img.shields.io/badge/JavaFX-21.0.2-blue.svg)
![Maven](https://img.shields.io/badge/Maven-3.9.16-red.svg)
![License](https://img.shields.io/badge/License-MIT-green.svg)

**Jone** 是一款基于 JavaFX 纯代码构建的现代桌面级配置文件转换工具。它不仅支持 `CSV`、`JSON`、`YAML` 三种格式的任意互转，更拥有完全自定义的无边框现代 UI、丝滑的主题引擎以及极致安全的本地读写能力。

## ✨ 核心特性

- **🎨 纯代码现代化 UI**：拒绝 FXML，完全使用 Java 代码实现无边框窗口、自绘标题栏与动态侧边栏。
- **🌗 动态主题引擎**：内置 5 种预设颜色（DeepSeek 蓝、樱花粉等），支持 RGB 自定义拾色，完美支持“跟随系统深浅色模式”。
- **⚙️ 配置驱动视图**：界面所有状态（主题、窗口尺寸、停留页面）均持久化于 `config.json`，即改即生效。
- **🔄 强大的转换内核**：支持 CSV ↔ JSON ↔ YAML 两两互转；自动处理 BOM、表头、内嵌逗号与引号；同名文件冲突时自动生成 `_1`、`_2` 后缀。
- **🛡️ 极致纯净**：自带便携版 JDK 与 Maven 环境隔离，拒绝污染系统环境变量，真正做到“解压即用”。

---

## 🚀 如何构建与运行

本项目使用了极致的绿色便携开发环境，所有依赖（JDK 25, Maven 3.9.16, 依赖仓库）均已收纳在项目内的 `envs` 目录中。

### 方法一：双击启动（推荐，日常使用）

直接双击项目根目录下的 **`启动Jone.cmd`** 即可。
> 该脚本会自动应用隔离环境并拉起 JavaFX 窗口，且不会在你的 C 盘中留下任何环境变量。

### 方法二：通过 PowerShell 命令行启动（开发调试）

如果你的终端要求手动配置环境，请在项目根目录执行以下命令：

    # 1. 仅对当前窗口放开脚本执行权限
    Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -Force

    # 2. 激活项目级环境（注意“点 + 空格”）
    . .\env.ps1

    # 3. 启动应用
    mvn javafx:run

> ⚠️ **注意**：请勿在受限的沙盒终端（如某些 AI 运行环境）中启动。Windows 的完整性级别机制（Low Integrity）会拒绝 Java 进程写入外部磁盘。若启动日志提示“受限环境”，请改用双击启动或普通 PowerShell 窗口。

---

## 🛠️ 项目架构

项目遵循极致的解耦原则，接口只传纯原生数据类型（String/boolean），绝不暴露自定义实体。

    src/com/jone
    ├── App.java                  # 应用入口与自动化自检调度器
    ├── config/
    │   └── ConfigStore.java      # 极简 Json 读写：点号键、原子落盘
    ├── convert/
    │   ├── ConversionService.java# 转换唯一入口（只返回原生与中文错误串）
    │   ├── CsvCodec.java         # CSV 解析器
    │   ├── JsonCodec.java        # JSON 解析器
    │   ├── YamlCodec.java        # YAML 解析器
    │   └── FormatCatalog.java    # 格式注册表
    ├── theme/
    │   ├── ThemeEngine.java      # 颜色计算引擎（混白、压暗、跟随系统）
    │   └── 系统主题监听.java       # JNA 包装层
    ├── ui/
    │   ├── 主窗口.java            # 无边框外壳与重建调度
    │   ├── 标题栏.java            # 自绘拖拽与窗口控制
    │   ├── 侧边栏.java            # 动态动画与选中线条
    │   ├── 转换页.java            # 格式选择与覆盖弹窗
    │   ├── 设置页.java            # RGB拾色器与系统跟随
    │   └── 主页.java              # 动态比例大时钟
    └── util/
        └── 运行环境诊断.java       # 受限沙盒识别

---

## 🧪 测试与验收

本项目带有高度完备的自动化测试链路：

    # 运行 45 项单元测试（覆盖配置、格式转换、主题计算）
    .\tests\run-tests.ps1

    # 运行 7 项 UI 交互自检（自动模拟侧边栏悬停、窗口按钮点击、弹窗交互）
    .\tests\run-ui-checks.ps1

---

## 🖼️ 界面预览

*(建议你在本地截一张主页大时钟和设置页的图，放到根目录，替换下面的链接)*

![主页预览](test_data/snapshots/第6步-主页.png)
![设置页预览](test_data/snapshots/第5步-设置页.png)