# 球费计算器 · PWA 版

原 Android（Kotlin + Compose）版本的静态网页移植，可直接安装到 iOS 主屏使用。

<img src="screenshot.png" alt="界面预览" width="320">

**零依赖、零构建**：`web/` 目录本身就是产物，推送到任何静态托管即可。

```
web/
├── index.html                 # 页面骨架（含原版的 calculator 图标矢量）
├── manifest.webmanifest       # PWA 清单
├── sw.js                      # Service Worker（离线可用）
├── assets/
│   ├── core.js                # 计算核心（纯函数，对应 FeeCalculate.java + ShuttlecockFee.kt）
│   ├── wheel.js               # 滚轮选择器（原生滚动 + scroll-snap）
│   ├── i18n.js                # 中英文案
│   ├── app.js                 # 状态与交互（对应 CalculatorScreen）
│   └── app.css                # 液态玻璃视觉
├── icons/                     # 图标（SVG 源 + 各尺寸 PNG）
├── tools/make-icons.mjs       # 由 SVG 重新生成 PNG
└── tests/                     # 数值一致性测试
```

## 选型说明

单屏计算器用框架没有收益，反而多出 40 KB 以上的解析/执行开销。这里直接使用
**原生 ES Modules + CSS**：

| 关注点 | 做法 | 收益 |
| --- | --- | --- |
| 首屏 | 静态 HTML，样式内联在 CSS 文件里，无字体下载 | iOS 冷启动无白屏等待 |
| 交互 | 直接操作 DOM，无虚拟 DOM diff | 每次输入只更新几个文本节点 |
| 滚轮 | 原生滚动 + `scroll-snap`，`rAF` 内只改可见项的 transform | 惯性由系统接管，60fps |
| 玻璃质感 | `backdrop-filter` 只用在少数大面上 | GPU 合成，避免整页叠加掉帧 |
| 部署 | 纯静态文件 | GitHub Pages / Cloudflare Pages 免构建 |

字体使用系统字体栈（iOS 上即 SF Pro / 苹方），不加载 Web 字体。

## 本地预览

Service Worker 需要安全上下文（`localhost` 或 HTTPS），别直接双击 `index.html`：

```bash
cd web
python -m http.server 8123
# 或：npx serve .
```

打开 http://127.0.0.1:8123/

调试主题：`?theme=dark` / `?theme=light` 可强制配色（默认跟随系统）。

## 测试

```bash
node --test web/tests
```

`tests/fixtures/jvm-parity.json` 是用**真实的 Android 源码**在 JVM 上跑出来的基准：
413 组随机 + 边界输入，要求 Web 端 `double` 结果与 JVM **逐位相等**，不是近似相等。

重新生成基准（需要 JDK）：

```bash
javac -d /tmp/parity \
  app/src/main/java/com/supremesir/badmintonfeecalculator/FeeCalculate.java \
  web/tests/jvm/ParityFixture.java
java -cp /tmp/parity com.supremesir.badmintonfeecalculator.ParityFixture \
  > web/tests/fixtures/jvm-parity.json
```

## 部署

### GitHub Pages

仓库已包含 `.github/workflows/deploy-pages.yml`：推送到 `master` 且改动 `web/**` 时，
自动把 `web/` 发布到 Pages。

首次使用需在 **Settings → Pages → Source** 选择 **GitHub Actions**。
默认地址形如 `https://<user>.github.io/<repo>/`（子路径已适配，所有资源用相对路径）。

若默认分支不是 `master`，改一下 workflow 里的 `branches`。

### Cloudflare Pages

- Framework preset：`None`
- Build command：留空
- Build output directory：`web`

### 其他静态托管（Netlify / Vercel / 对象存储）

把 `web/` 整个目录作为站点根上传即可，无需任何构建步骤。

## iOS 安装

iOS 没有安装弹窗，需要手动添加：

1. Safari 打开站点
2. 分享按钮 → **添加到主屏幕**

之后从主屏图标启动即为全屏（`display: standalone`），状态栏透明、支持刘海安全区。
首次在 Safari 访问时页面会自动给出这个提示（可关闭，之后不再显示）。

> 注意：必须用 **Safari** 添加。Chrome for iOS 的“添加到主屏幕”只会建一个普通书签。

## 与 Android 版的一致性

计算逻辑（含 `Math.ceil(x*10)/10` 的向上取整、`Math.round` 的四舍五入、除以 0 时
产生 `Infinity`/`NaN` 的语义）与 `FeeCalculate.java` 完全一致，由测试锁定。

交互一一对应：场地费/羽毛球费输入、男生加价行（点击就地编辑、失焦回填 5）、
男/女/鸽子三个滚轮（0–20）、计算结果三块（格子可点击复制）、
计算按钮在「输入已改变但未重算」时变红、组合金额面板（6 组常用人数 + 自定义滚轮）、
按桶计算面板（桶价 × 颗数，多行累加，退出时可改回总价）。

有意的差异（都不影响算出来的金额）：

- 金额展示去掉多余的 `.0`：`25.0` 显示为 `25`（原版显示 `25.0`），复制的内容同样是 `25`。
- 无法计算时（例如人数全为 0 导致除零）显示 `—`，原版会显示 `NaN`。
- 新增右上角中/英切换（原版只跟随系统语言），选择会记住。
- 新增按桶计算面板顶部的实时合计（原版合计只回填到背后的输入框，被面板挡住看不见）。
- 去掉了 Compose 特有的透镜折射、色散、惯性高光等效果（浏览器里代价高、收益低），
  改用模糊 + 内高光 + 细描边实现同样的玻璃观感。

## 修改后记得

改动任何静态资源后，把 `sw.js` 里的 `VERSION` 加一（`v1` → `v2`），
否则已安装的用户可能继续用旧缓存。
