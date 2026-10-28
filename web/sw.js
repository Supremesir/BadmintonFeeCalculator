/**
 * Service Worker —— 离线可用 + 秒开。
 *
 * 策略：
 *   页面导航  cache-first（立刻出首屏），后台静默更新
 *   静态资源  stale-while-revalidate（先给缓存，同时拉新）
 *
 * 改了源码后把 VERSION 加一，旧缓存会在 activate 时被清掉。
 */

const VERSION = 'v3';
const CACHE = `bfc-${VERSION}`;

const PRECACHE = [
  './',
  './index.html',
  './manifest.webmanifest',
  './assets/app.css',
  './assets/app.js',
  './assets/core.js',
  './assets/wheel.js',
  './assets/i18n.js',
];

self.addEventListener('install', (event) => {
  event.waitUntil(
    (async () => {
      const cache = await caches.open(CACHE);
      // 单个文件失败不应让整个安装失败
      await Promise.all(
        PRECACHE.map((url) => cache.add(url).catch(() => undefined))
      );
      await self.skipWaiting();
    })()
  );
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    (async () => {
      const keys = await caches.keys();
      await Promise.all(
        keys.filter((key) => key.startsWith('bfc-') && key !== CACHE).map((key) => caches.delete(key))
      );
      await self.clients.claim();
    })()
  );
});

async function fromCache(request) {
  const cache = await caches.open(CACHE);
  return cache.match(request, { ignoreSearch: true });
}

async function updateCache(request, response) {
  if (!response || !response.ok || response.type === 'opaque') return;
  const cache = await caches.open(CACHE);
  await cache.put(request, response.clone());
}

async function handleNavigate(request) {
  const cached = (await fromCache(request)) || (await fromCache('./index.html'));
  const network = fetch(request, { cache: 'no-cache' })
    .then((response) => {
      updateCache('./index.html', response);
      return response;
    })
    .catch(() => undefined);

  if (cached) {
    network.catch(() => undefined);
    return cached;
  }
  return (await network) || Response.error();
}

async function staleWhileRevalidate(request) {
  const cached = await fromCache(request);
  const network = fetch(request, { cache: 'no-cache' })
    .then((response) => {
      updateCache(request, response);
      return response;
    })
    .catch(() => undefined);

  return cached || (await network) || Response.error();
}

self.addEventListener('fetch', (event) => {
  const { request } = event;
  if (request.method !== 'GET') return;

  const url = new URL(request.url);
  if (url.origin !== self.location.origin) return;
  if (url.pathname.endsWith('/sw.js')) return;

  if (request.mode === 'navigate') {
    event.respondWith(handleNavigate(request));
    return;
  }

  event.respondWith(staleWhileRevalidate(request));
});
