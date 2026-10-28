/**
 * 由应用图标母版生成 PWA 图标（零依赖，用本机 Edge/Chrome 无头光栅化）。
 *
 *   node web/tools/make-icons.mjs
 *
 * 母版：app/src/main/new_ic_launcher-playstore.png（512×512）
 * 也就是 Android 工程里真正上架的图标，保证两边同款。
 *
 * 产物：
 *   icon-180.png             iOS 主屏（apple-touch-icon，满幅不留透明边）
 *   icon-192.png / 512.png   Web App Manifest
 *   icon-maskable-512.png    Android/Chrome maskable，图案收进 80% 安全区
 *   favicon.png              浏览器标签页
 */
import { spawn } from 'node:child_process';
import { mkdtempSync, rmSync, writeFileSync, readFileSync, existsSync, statSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join, resolve, dirname } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

const here = dirname(fileURLToPath(import.meta.url));
const iconsDir = resolve(here, '../icons');
// 直接用 Android 工程的 Play Store 图标当母版，避免在 web/ 里再存一份
const source = resolve(here, '../../app/src/main/new_ic_launcher-playstore.png');

if (!existsSync(source)) {
  console.error(`缺少图标母版：${source}`);
  process.exit(1);
}

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
  console.error('找不到 Chrome/Edge，无法生成图标。可设置 CHROME_PATH 环境变量。');
  process.exit(1);
}

/** mime 决定体积：同样的金币图，WebP 只有 PNG 的五分之一左右 */
// 母版里金币几乎顶到画布边缘，直接塞进 iOS 的 squircle / Android 的圆形遮罩会把币缘切掉，
// 所以统一留 10% 白边（和 Android adaptive icon 的观感也接近）
const TARGETS = [
  { out: 'icon-180.png', size: 180, inset: 0.1, mime: 'image/png', note: 'iOS 主屏（PNG 兼容性最好）' },
  { out: 'icon-192.webp', size: 192, inset: 0.1, mime: 'image/webp', quality: 0.92, note: 'manifest' },
  { out: 'icon-512.webp', size: 512, inset: 0.1, mime: 'image/webp', quality: 0.92, note: 'manifest' },
  { out: 'icon-maskable-512.webp', size: 512, inset: 0.1, mime: 'image/webp', quality: 0.92, note: 'maskable（收进 80% 安全区）' },
  { out: 'favicon.png', size: 64, inset: 0, mime: 'image/png', note: '标签页' },
];

const work = mkdtempSync(join(tmpdir(), 'bfc-icons-'));
const profile = join(work, 'profile');

let msgId = 0;
const pending = new Map();

function send(ws, method, params = {}, sessionId) {
  const id = ++msgId;
  const payload = { id, method, params };
  if (sessionId) payload.sessionId = sessionId;
  ws.send(JSON.stringify(payload));
  return new Promise((res, rej) => pending.set(id, { res, rej, method }));
}

async function connect() {
  const portFile = join(profile, 'DevToolsActivePort');
  const deadline = Date.now() + 30_000;
  while (Date.now() < deadline) {
    if (existsSync(portFile)) {
      const [port, path] = readFileSync(portFile, 'utf8').split('\n');
      if (port && path) {
        const ws = new WebSocket(`ws://127.0.0.1:${port}${path.trim()}`);
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

// 页面是 file://，直接引 file:// 图片会污染 canvas（toDataURL 会抛 SecurityError），
// 所以把母版内联成 data URI
const sourceUrl = `data:image/png;base64,${readFileSync(source).toString('base64')}`;

function pageHtml(size, inset) {
  const inner = Math.round(size * (1 - inset * 2));
  return `<!doctype html><html><head><meta charset="utf-8"><style>
    html, body { margin: 0; padding: 0; background: #fff; }
    img { width: ${inner}px; height: ${inner}px; display: block; }
  </style></head><body><img src="${sourceUrl}" alt=""></body></html>`;
}

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

try {
  const ws = await connect();
  ws.addEventListener('message', (event) => {
    const msg = JSON.parse(event.data);
    if (msg.id && pending.has(msg.id)) {
      const { res, rej, method } = pending.get(msg.id);
      pending.delete(msg.id);
      if (msg.error) rej(new Error(`${method}: ${msg.error.message}`));
      else res(msg.result);
    }
  });

  for (const target of TARGETS) {
    const { out, size, inset } = target;
    const page = join(work, `${out}.html`);
    const outPath = join(iconsDir, out);
    writeFileSync(page, pageHtml(size, inset));

    const { targetId } = await send(ws, 'Target.createTarget', { url: 'about:blank' });
    const { sessionId } = await send(ws, 'Target.attachToTarget', { targetId, flatten: true });

    await send(ws, 'Page.enable', {}, sessionId);
    await send(ws, 'Emulation.setDeviceMetricsOverride', { width: size, height: size, deviceScaleFactor: 1, mobile: false }, sessionId);

    await send(ws, 'Page.navigate', { url: pathToFileURL(page).href }, sessionId);
    await sleep(400); // 等位图解码

    // 用 canvas 重采样后直接导出，避免截图的缩放误差
    const inner = Math.round(size * (1 - inset * 2));
    const expr = `(async () => {
      const img = document.querySelector('img');
      await img.decode();
      const canvas = document.createElement('canvas');
      canvas.width = ${size}; canvas.height = ${size};
      const ctx = canvas.getContext('2d');
      ctx.fillStyle = '#fff';
      ctx.fillRect(0, 0, ${size}, ${size});
      ctx.imageSmoothingQuality = 'high';
      ctx.drawImage(img, ${Math.round((size - inner) / 2)}, ${Math.round((size - inner) / 2)}, ${inner}, ${inner});
      return canvas.toDataURL('${target.mime}', ${target.quality ?? 1});
    })()`;

    const encoded = await send(
      ws,
      'Runtime.evaluate',
      { expression: expr, awaitPromise: true, returnByValue: true },
      sessionId
    );
    const dataUrl = encoded.result?.value;
    if (typeof dataUrl !== 'string' || !dataUrl.startsWith('data:image/')) {
      throw new Error(`${out} 导出失败：${JSON.stringify(encoded).slice(0, 300)}`);
    }
    rmSync(outPath, { force: true });
    writeFileSync(outPath, Buffer.from(dataUrl.split(',')[1], 'base64'));
    await send(ws, 'Target.closeTarget', { targetId });

    console.log(`✓ ${out}  ${size}×${size}  ${(statSync(outPath).size / 1024).toFixed(0)} KB  ${target.note}`);
  }

  ws.close();
} finally {
  child.kill();
  try {
    rmSync(work, { recursive: true, force: true, maxRetries: 3, retryDelay: 200 });
  } catch {
    /* 浏览器可能还占着临时目录 */
  }
}
