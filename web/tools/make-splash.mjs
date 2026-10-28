/**
 * 生成 iOS 主屏启动图（apple-touch-startup-image）。
 *
 *   node web/tools/make-splash.mjs
 *
 * 为什么需要：没提供启动图时，iOS 从主屏图标启动会先铺一帧纯色（浅色接近白、
 * 深色接近黑），再跳进界面，看起来就像“网页在重新加载”。给每个机型准备一张
 * 与界面同色系的图，启动帧和首屏就接得上，观感接近原生 App。
 *
 * 实现说明：必须用 CDP 的 Emulation.setDeviceMetricsOverride 而不是
 * --window-size —— 后者会被物理屏幕高度截断，高分辨率机型（2532/2796 px）
 * 会拍出上下黑边。截图完会自动把 <link> 写回 index.html 的标记块。
 */
import { spawn } from 'node:child_process';
import {
  mkdtempSync, rmSync, writeFileSync, readFileSync, existsSync, statSync, mkdirSync,
} from 'node:fs';
import { tmpdir } from 'node:os';
import { join, resolve, dirname } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

const here = dirname(fileURLToPath(import.meta.url));
const webDir = resolve(here, '..');
const splashDir = join(webDir, 'splash');
const iconsDir = join(webDir, 'icons');
const indexPath = join(webDir, 'index.html');

const BROWSERS = [
  process.env.CHROME_PATH,
  'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',
  'C:/Program Files/Microsoft/Edge/Application/msedge.exe',
  'C:/Program Files/Google/Chrome/Application/chrome.exe',
  'C:/Program Files (x86)/Google/Chrome/Application/chrome.exe',
  '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',
  '/Applications/Microsoft Edge.app/Contents/MacOS/Microsoft Edge',
  '/usr/bin/google-chrome',
  '/usr/bin/chromium',
  '/usr/bin/chromium-browser',
].filter(Boolean);

const browser = BROWSERS.find((p) => existsSync(p));
if (!browser) {
  console.error('找不到 Chrome/Edge，无法生成启动图。可设置 CHROME_PATH 环境变量。');
  process.exit(1);
}

/** CSS 像素尺寸 + 像素比 —— 必须和 media query 完全一致，iOS 才认得出 */
const DEVICES = [
  { cw: 402, ch: 874, dpr: 3, note: 'iPhone 16 Pro' },
  { cw: 440, ch: 956, dpr: 3, note: 'iPhone 16 Pro Max' },
  { cw: 393, ch: 852, dpr: 3, note: 'iPhone 15/15 Pro/14 Pro' },
  { cw: 390, ch: 844, dpr: 3, note: 'iPhone 12/13/14' },
  { cw: 430, ch: 932, dpr: 3, note: 'iPhone 14/15 Pro Max、15 Plus' },
  { cw: 428, ch: 926, dpr: 3, note: 'iPhone 12/13 Pro Max' },
  { cw: 375, ch: 812, dpr: 3, note: 'iPhone X/XS/11 Pro/12·13 mini' },
  { cw: 414, ch: 896, dpr: 3, note: 'iPhone XS Max/11 Pro Max' },
  { cw: 414, ch: 896, dpr: 2, note: 'iPhone XR/11' },
  { cw: 375, ch: 667, dpr: 2, note: 'iPhone SE 2/3、8' },
  { cw: 320, ch: 568, dpr: 2, note: 'iPhone SE 1、5s' },
];

/**
 * 取 assets/app.css 里 --bg-top / --bg-bottom 的中值。
 * 纯色能压到几十 KB（渐变的抖动噪点会让 PNG 涨到 150 KB 以上），
 * 而和首屏渐变只差十几个色阶，切换时看不出来。
 */
const THEMES = [
  { key: 'light', bg: '#D8E2ED' },
  { key: 'dark', bg: '#141A22' },
];

const work = mkdtempSync(join(tmpdir(), 'bfc-splash-'));
const profile = join(work, 'profile');
mkdirSync(splashDir, { recursive: true });

/* -------------------------------------------------------------------------- */
/* 极简 CDP 客户端（零依赖，Node 内置 WebSocket）                                */
/* -------------------------------------------------------------------------- */

let msgId = 0;
const pending = new Map();

function send(ws, method, params = {}, sessionId) {
  const id = ++msgId;
  const payload = { id, method, params };
  if (sessionId) payload.sessionId = sessionId;
  ws.send(JSON.stringify(payload));
  return new Promise((resolve, reject) => {
    pending.set(id, { resolve, reject, method });
  });
}

function nextEvent(ws, name, sessionId, timeoutMs = 20_000) {
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error(`等待 ${name} 超时`)), timeoutMs);
    const onMessage = (event) => {
      const msg = JSON.parse(event.data);
      if (msg.method === name && (!sessionId || msg.sessionId === sessionId)) {
        clearTimeout(timer);
        ws.removeEventListener('message', onMessage);
        resolve(msg.params);
      }
    };
    ws.addEventListener('message', onMessage);
  });
}

async function connect() {
  const activePort = join(profile, 'DevToolsActivePort');
  const deadline = Date.now() + 30_000;
  while (Date.now() < deadline) {
    if (existsSync(activePort)) {
      const [port, browserPath] = readFileSync(activePort, 'utf8').split('\n');
      if (port && browserPath) {
        const ws = new WebSocket(`ws://127.0.0.1:${port}${browserPath.trim()}`);
        await new Promise((res, rej) => {
          ws.onopen = res;
          ws.onerror = rej;
        });
        return ws;
      }
    }
    await sleep(120);
  }
  throw new Error('浏览器没有在 30s 内启动完成');
}

/* -------------------------------------------------------------------------- */

// 用 maskable 版：金币四周留了 10% 白边，套上圆角后不会把币缘裁掉
const iconUrl = `data:image/webp;base64,${readFileSync(join(iconsDir, 'icon-maskable-512.webp')).toString('base64')}`;

function splashHtml(width, height, theme) {
  // 图标按 22% 宽度绘制；素材是方形的（主屏图标由 iOS 自己裁圆角），
  // 这里按 iOS 图标的观感补上约 22.5% 的圆角
  const iconSize = Math.round(width * 0.28);
  return `<!doctype html><html><head><meta charset="utf-8"><style>
    html, body { margin: 0; padding: 0; width: 100%; height: 100%; }
    body {
      background: ${theme.bg};
      display: flex; align-items: center; justify-content: center;
      overflow: hidden;
    }
    img {
      width: ${iconSize}px; height: ${iconSize}px; display: block;
      border-radius: ${Math.round(iconSize * 0.225)}px;
      filter: drop-shadow(0 ${Math.round(width * 0.018)}px ${Math.round(width * 0.045)}px rgba(23, 42, 66, 0.26));
    }
  </style></head><body><img src="${iconUrl}" alt=""></body></html>`;
}

const results = [];

async function main() {
  const child = spawn(
    browser,
    [
      '--headless=new',
      '--disable-gpu',
      '--hide-scrollbars',
      '--no-first-run',
      '--no-default-browser-check',
      '--remote-debugging-port=0',
      `--user-data-dir=${profile}`,
      'about:blank',
    ],
    { stdio: 'ignore' }
  );

  const ws = await connect();

  ws.addEventListener('message', (event) => {
    const msg = JSON.parse(event.data);
    if (msg.id && pending.has(msg.id)) {
      const { resolve, reject, method } = pending.get(msg.id);
      pending.delete(msg.id);
      if (msg.error) reject(new Error(`${method}: ${msg.error.message}`));
      else resolve(msg.result);
    }
  });

  try {
    for (const device of DEVICES) {
      const width = device.cw * device.dpr;
      const height = device.ch * device.dpr;

      for (const theme of THEMES) {
        const file = `splash-${device.cw}x${device.ch}@${device.dpr}x-${theme.key}.png`;
        const outPath = join(splashDir, file);
        const page = join(work, `${file}.html`);
        writeFileSync(page, splashHtml(width, height, theme));

        const { targetId } = await send(ws, 'Target.createTarget', { url: 'about:blank' });
        const { sessionId } = await send(ws, 'Target.attachToTarget', { targetId, flatten: true });

        await send(ws, 'Page.enable', {}, sessionId);
        await send(
          ws,
          'Emulation.setDeviceMetricsOverride',
          { width, height, deviceScaleFactor: 1, mobile: true },
          sessionId
        );

        const loaded = nextEvent(ws, 'Page.loadEventFired', sessionId);
        await send(ws, 'Page.navigate', { url: pathToFileURL(page).href }, sessionId);
        await loaded;
        await sleep(150); // 等 SVG 解码完成

        const shot = await send(
          ws,
          'Page.captureScreenshot',
          { format: 'png', captureBeyondViewport: false },
          sessionId
        );
        // 先删旧文件再写，避免生成失败时留下半截产物
        rmSync(outPath, { force: true });
        writeFileSync(outPath, Buffer.from(shot.data, 'base64'));

        await send(ws, 'Target.closeTarget', { targetId });

        const bytes = statSync(outPath).size;
        results.push({ device, theme, file, bytes });
        console.log(`✓ ${file}  ${width}×${height}  ${(bytes / 1024).toFixed(0)} KB  ${device.note}`);
      }
    }
  } finally {
    try {
      ws.close();
    } catch {
      /* ignore */
    }
    child.kill();
  }
}

try {
  await main();

  // 把 <link> 写回 index.html 的标记块
  const links = results
    .map(({ device, theme, file }) => {
      const media =
        `(prefers-color-scheme: ${theme.key}) and (device-width: ${device.cw}px) and ` +
        `(device-height: ${device.ch}px) and (-webkit-device-pixel-ratio: ${device.dpr}) and ` +
        `(orientation: portrait)`;
      return `    <link rel="apple-touch-startup-image" href="./splash/${file}" media="${media}" />`;
    })
    .join('\n');

  const html = readFileSync(indexPath, 'utf8');
  const startTag = '<!-- 启动图开始 -->';
  const endTag = '<!-- 启动图结束 -->';
  const start = html.indexOf(startTag);
  const end = html.indexOf(endTag);
  if (start === -1 || end === -1) throw new Error('index.html 里找不到启动图标记块');

  writeFileSync(
    indexPath,
    html.slice(0, start + startTag.length) + '\n' + links + '\n    ' + html.slice(end)
  );

  const totalKb = results.reduce((sum, r) => sum + r.bytes, 0) / 1024;
  console.log(`\n共 ${results.length} 张，合计 ${(totalKb / 1024).toFixed(2)} MB（每次启动只取其中一张）`);
  console.log('已更新 index.html 的 apple-touch-startup-image 链接。');
} catch (error) {
  console.error(error.message);
  process.exit(1);
} finally {
  try {
    rmSync(work, { recursive: true, force: true, maxRetries: 3, retryDelay: 200 });
  } catch {
    /* 浏览器可能还占着临时目录 */
  }
}
