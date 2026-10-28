/**
 * 滚轮选择器 —— 对应 Compose 的 LiquidWheelPicker。
 *
 * 实现思路：原生滚动 + CSS scroll-snap 负责惯性与吸附（iOS 上由系统
 * momentum 驱动，手感与原生 UIPickerView 一致），
 * requestAnimationFrame 里只对可见项做 3D 变换，避免逐帧改布局。
 */

const clamp = (v, min, max) => (v < min ? min : v > max ? max : v);

export function createWheel(options) {
  const {
    min,
    max,
    value = min,
    itemHeight = 36,
    visible = 5,
    onChange = () => {},
    label = '',
  } = options;

  const padCount = Math.floor(visible / 2);
  const count = max - min + 1;

  const root = document.createElement('div');
  root.className = 'wheel';
  root.style.setProperty('--item-h', `${itemHeight}px`);
  root.style.height = `${itemHeight * visible}px`;

  const indicator = document.createElement('div');
  indicator.className = 'wheel__indicator';

  const track = document.createElement('div');
  track.className = 'wheel__track';
  track.style.paddingBlock = `${padCount * itemHeight}px`;
  track.tabIndex = 0;
  track.setAttribute('role', 'slider');
  track.setAttribute('aria-valuemin', String(min));
  track.setAttribute('aria-valuemax', String(max));
  track.setAttribute('aria-label', label);

  const items = [];
  for (let i = 0; i < count; i++) {
    const item = document.createElement('div');
    item.className = 'wheel__item';
    item.textContent = String(min + i);
    track.appendChild(item);
    items.push(item);
  }

  root.append(indicator, track);

  let current = clamp(Math.round(value), min, max);
  let programmaticTarget = null;
  let programmaticTimer = 0;
  let raf = 0;
  let idleTimer = 0;

  track.setAttribute('aria-valuenow', String(current));

  // 创建时元素可能还没进 DOM，此时给 scrollTop 赋值会被忽略，
  // 因此推迟一帧，等有布局之后再定位到初始值。
  requestAnimationFrame(() => {
    track.scrollTop = (current - min) * itemHeight;
    paint();
  });

  /** 依据滚动位置刷新可见项的透视/缩放/透明度 */
  function paint() {
    const scrollTop = track.scrollTop;
    for (let i = 0; i < items.length; i++) {
      const distance = (i * itemHeight - scrollTop) / itemHeight;
      const absDistance = Math.abs(distance);
      if (absDistance > 2.6) {
        // 视口外的项直接跳过样式计算
        if (items[i].style.opacity !== '0') {
          items[i].style.opacity = '0';
          items[i].style.transform = '';
        }
        continue;
      }
      const closeness = clamp(1 - absDistance / 2.4, 0, 1);
      const centerWeight = closeness * closeness;
      const scale = 0.8 + 0.42 * centerWeight;
      // 透视收敛保持克制：角度一大，最外圈数字会被压成一条短线，很脏
      items[i].style.transform = `rotateX(${(distance * -16).toFixed(2)}deg) scale(${scale.toFixed(3)})`;
      items[i].style.opacity = (0.34 + 0.66 * closeness).toFixed(3);
      items[i].style.fontWeight = centerWeight > 0.72 ? '700' : centerWeight > 0.32 ? '600' : '400';
    }
  }

  function schedulePaint() {
    if (raf) return;
    raf = requestAnimationFrame(() => {
      raf = 0;
      paint();
    });
  }

  function commit(next) {
    if (next === current) return;
    current = next;
    track.setAttribute('aria-valuenow', String(current));
    onChange(current);
  }

  function readIndex() {
    return clamp(Math.round(track.scrollTop / itemHeight) + min, min, max);
  }

  function onScroll() {
    schedulePaint();
    const index = readIndex();
    if (programmaticTarget != null) {
      if (index === programmaticTarget) programmaticTarget = null;
    } else {
      commit(index);
    }
    // 少数环境不支持 scroll-snap，滚动停止后手动对齐
    clearTimeout(idleTimer);
    idleTimer = setTimeout(() => {
      const offset = track.scrollTop / itemHeight;
      const nearest = Math.round(offset);
      if (Math.abs(offset - nearest) > 0.02) {
        track.scrollTo({ top: nearest * itemHeight, behavior: 'smooth' });
      }
    }, 160);
  }

  track.addEventListener('scroll', onScroll, { passive: true });

  track.addEventListener(
    'keydown',
    (event) => {
      const step = { ArrowUp: 1, ArrowRight: 1, ArrowDown: -1, ArrowLeft: -1 }[event.key];
      if (step) {
        api.setValue(current + step, true);
      } else if (event.key === 'PageUp') {
        api.setValue(current + 5, true);
      } else if (event.key === 'PageDown') {
        api.setValue(current - 5, true);
      } else if (event.key === 'Home') {
        api.setValue(min, true);
      } else if (event.key === 'End') {
        api.setValue(max, true);
      } else {
        return;
      }
      event.preventDefault();
    },
    { passive: false }
  );

  const api = {
    el: root,
    get value() {
      return current;
    },
    /** 外部同步值（例如重置），不触发 onChange */
    setValue(next, animate = false) {
      const target = clamp(Math.round(next), min, max);
      if (target === current) {
        paint();
        return;
      }
      current = target;
      track.setAttribute('aria-valuenow', String(current));
      programmaticTarget = target;
      clearTimeout(programmaticTimer);
      programmaticTimer = setTimeout(() => {
        programmaticTarget = null;
      }, 700);
      track.scrollTo({
        top: (target - min) * itemHeight,
        behavior: animate ? 'smooth' : 'auto',
      });
      schedulePaint();
    },
    refresh() {
      paint();
    },
    setLabel(nextLabel) {
      track.setAttribute('aria-label', nextLabel);
    },
  };

  paint();
  return api;
}
