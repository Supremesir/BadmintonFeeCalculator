# 球费计算器

Android 羽毛球球费分摊小工具。当前界面基于液态玻璃风格实现。

## 网页版 / PWA

`web/` 是用同一套计算逻辑写的静态网页版，可安装到 iOS 主屏使用，也能直接部署到
GitHub Pages / Cloudflare Pages。详见 [web/README.md](web/README.md)。

```bash
cd web && python -m http.server 8123   # 本地预览 http://127.0.0.1:8123/
node --test web/tests                  # 与 JVM 版本的数值一致性测试
```

## 致谢

UI 中的 Liquid Glass 效果来自 [Kyant0/AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass)（Maven：`io.github.kyant0:backdrop`）。感谢 [Kyant0](https://github.com/Kyant0) 开源 Backdrop 库，以及 catalog 中按钮、高光等交互示例。
