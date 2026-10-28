/**
 * 触感反馈（haptics）。
 *
 * 三档：light（按钮）、medium（计算/确认）、heavy（保存、错误）。
 *
 * Android / 桌面 Chromium：直接用 navigator.vibrate。
 * iOS：Safari 至今没有 Vibration API，唯一的门路是借
 *   <input type="checkbox" switch>
 * 这个原生开关控件——用户点它时系统会给一下触感反馈，而我们用
 * label.click() 去"假装"用户点了它。iOS 17.4 起才有 switch 外观，
 * 更老的版本会退化成静默的勾选（无害，只是没声音没震动）。
 *
 * 因为这是"蹭"来的，所以：只在 iOS 上挂这个隐藏控件；限流 45ms，
 * 免得滚轮快速滑动时把系统触感刷成机关枪；所有调用都允许被系统忽略。
 */

const isIOS =
  /iPad|iPhone|iPod/.test(navigator.userAgent) ||
  (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1);

const canVibrate = typeof navigator.vibrate === 'function';

/** vibrate 的时长/节奏，和 Android 的 HapticFeedback 观感对齐 */
const PATTERNS = {
  light: 8,
  medium: 14,
  heavy: [0, 10, 30, 16],
};

const MIN_GAP = 45;
let lastAt = 0;

let switchInput = null;
let switchLabel = null;

/**
 * 只在真正需要时才创建：桌面浏览器凭空多两个节点没意义。
 */
function ensureSwitch() {
  if (!isIOS || switchInput) return;
  switchInput = document.createElement('input');
  switchInput.type = 'checkbox';
  switchInput.setAttribute('switch', '');
  switchInput.id = 'haptic-switch';
  switchInput.className = 'haptic-switch';
  switchInput.tabIndex = -1;
  switchInput.setAttribute('aria-hidden', 'true');

  switchLabel = document.createElement('label');
  switchLabel.htmlFor = 'haptic-switch';
  switchLabel.className = 'haptic-switch';
  switchLabel.setAttribute('aria-hidden', 'true');

  document.body.append(switchInput, switchLabel);
}

if (isIOS) {
  // 必须等 body 存在；脚本以 module 方式在 body 末尾加载，这里通常是就绪的
  if (document.body) ensureSwitch();
  else document.addEventListener('DOMContentLoaded', ensureSwitch, { once: true });
}

/**
 * @param {'light'|'medium'|'heavy'} kind
 */
export function haptic(kind = 'light') {
  const now = performance.now();
  if (now - lastAt < MIN_GAP) return;
  lastAt = now;

  if (canVibrate) {
    try {
      navigator.vibrate(PATTERNS[kind] ?? PATTERNS.light);
    } catch {
      /* 某些环境会抛（比如被权限策略禁用），静默忽略 */
    }
    return;
  }

  if (isIOS && switchLabel) {
    // 同步触发，脱离用户手势的调用在 iOS 上会被忽略
    switchLabel.click();
  }
}

/** 这个环境到底有没有反馈（给 UI 决定要不要做视觉补充） */
export const hapticsAvailable = canVibrate || isIOS;

/**
 * 全局委托：任何按钮/可点行被按下时给一下轻触感。
 * 用 pointerdown 而不是 click —— 手感和原生一致，按下即反馈。
 */
export function bindGlobalHaptics(root = document) {
  root.addEventListener(
    'pointerdown',
    (event) => {
      if (event.pointerType === 'mouse') return; // 桌面鼠标不需要震
      const target = event.target instanceof Element ? event.target : null;
      if (!target) return;

      const hit = target.closest(
        '[data-haptic], button, a[href], [role="button"], .row, .tile, .segmented__item, .chip'
      );
      if (!hit) return;
      if (hit.hasAttribute('disabled') || hit.getAttribute('aria-disabled') === 'true') return;

      haptic(hit.dataset.haptic || 'light');
    },
    { passive: true }
  );
}
