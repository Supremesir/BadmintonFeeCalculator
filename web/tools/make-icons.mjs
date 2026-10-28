/**
 * 由 SVG 源生成 PWA 图标 PNG（零依赖，用本机 Edge/Chrome 无头光栅化）。
 *
 *   node web/tools/make-icons.mjs
 *
 * 产物：
 *   icon-180.png             iOS 主屏（apple-touch-icon，满幅不留透明边）
 *   icon-192.png / 512.png   Web App Manifest
 *   icon-maskable-512.png    Android/maskable，主体在 80% 安全区内
 */
import { spawnSync } from 'node:child_process';
import { mkdtempSync, rmSync, writeFileSync, existsSync, statSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join, resolve, dirname } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

const sleepSync = (ms) => Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, ms);

const here = dirname(fileURLToPath(import.meta.url));
const iconsDir = resolve(here, '../icons');

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

const browser = BROWSERS.find((path) => existsSync(path));
if (!browser) {
  console.error('找不到 Chrome/Edge，无法生成图标。可设置 CHROME_PATH 环境变量。');
  process.exit(1);
}

const targets = [
  { svg: 'icon.svg', out: 'icon-180.png', size: 180 },
  { svg: 'icon.svg', out: 'icon-192.png', size: 192 },
  { svg: 'icon.svg', out: 'icon-512.png', size: 512 },
  { svg: 'icon-maskable.svg', out: 'icon-maskable-512.png', size: 512 },
];

const work = mkdtempSync(join(tmpdir(), 'bfc-icons-'));

try {
  for (const target of targets) {
    const { size } = target;
    const page = join(work, `page-${target.out}.html`);
    const outPath = join(iconsDir, target.out);
    const svgUrl = pathToFileURL(join(iconsDir, target.svg)).href;

    writeFileSync(
      page,
      `<!doctype html><html><head><meta charset="utf-8"><style>
        html,body{margin:0;padding:0;background:transparent}
        img{display:block;width:${size}px;height:${size}px}
      </style></head><body><img src="${svgUrl}" alt=""></body></html>`
    );

    const result = spawnSync(
      browser,
      [
        '--headless=new',
        '--disable-gpu',
        '--hide-scrollbars',
        '--force-device-scale-factor=1',
        '--default-background-color=00000000',
        `--user-data-dir=${join(work, 'profile')}`,
        `--window-size=${size},${size}`,
        `--screenshot=${outPath}`,
        pathToFileURL(page).href,
      ],
      { stdio: 'ignore', timeout: 90_000 }
    );

    // Windows 上 Edge 的启动器会先返回，截图由后台进程稍后落盘，这里等文件真出现
    const deadline = Date.now() + 30_000;
    while (Date.now() < deadline) {
      if (existsSync(outPath) && statSync(outPath).size > 512) break;
      sleepSync(150);
    }

    if (!existsSync(outPath) || statSync(outPath).size <= 512) {
      throw new Error(`生成 ${target.out} 失败（浏览器退出码 ${result.status}）`);
    }
    console.log(`✓ ${target.out}  ${size}×${size}  ${statSync(outPath).size} bytes`);
  }
} finally {
  // 浏览器可能仍占用临时 profile 目录，清不掉就算了
  try {
    rmSync(work, { recursive: true, force: true, maxRetries: 3, retryDelay: 200 });
  } catch {
    /* 忽略 */
  }
}
