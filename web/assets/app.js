/**
 * 应用行为层 —— 对应 Compose 的 CalculatorScreen。
 *
 * 状态变化直接写 DOM（无虚拟 DOM、无框架），
 * 单屏应用的更新量极小，重绘成本远低于 diff 一套树。
 */

import {
  calculateOther,
  calculateOtherWithAbsent,
  calculateStringWithAbsent,
  formatFee,
  formatShuttlecockFee,
  getNumCombination,
  nextLineId,
  safeToDouble,
  SHUTTLECOCK_COUNT_MAX,
  SHUTTLECOCK_COUNT_MIN,
  shuttlecockFeeTotal,
} from './core.js';
import { createWheel } from './wheel.js';
import { getLang, setLang, t } from './i18n.js';
import { bindGlobalHaptics, haptic } from './haptics.js';

/* -------------------------------------------------------------------------- */
/* 元素引用                                                                    */
/* -------------------------------------------------------------------------- */

const $ = (id) => document.getElementById(id);

const app = $('app');
const appTitle = $('appTitle');
const langBtn = $('langBtn');

const courtFeeEl = $('courtFee');
const shuttleFeeEl = $('shuttleFee');
const shuttleAccessory = $('shuttleAccessory');

const extraRow = $('extraRow');
const extraFeeLabel = $('extraFeeLabel');
const extraValueEl = $('extraValue');
const extraInput = $('extraInput');

const wheelHosts = {
  male: $('wheelMale'),
  female: $('wheelFemale'),
  absent: $('wheelAbsent'),
};

const resultMale = $('resultMale');
const resultFemale = $('resultFemale');
const resultAbsent = $('resultAbsent');
const maleValueEl = $('maleValue');
const femaleValueEl = $('femaleValue');
const absentValueEl = $('absentValue');

const btnCombo = $('btnCombo');
const btnCalc = $('btnCalc');
const currencyGlyph = $('currencyGlyph');

const comboSheet = $('comboSheet');
const comboScroll = $('comboScroll');
const shuttleSheet = $('shuttleSheet');
const shuttleScroll = $('shuttleScroll');

const toastEl = $('toast');

const installHint = $('installHint');
const installHintOk = $('installHintOk');

/* -------------------------------------------------------------------------- */
/* 状态 —— 字段命名尽量对应 Compose 版                                          */
/* -------------------------------------------------------------------------- */

/** 诊断面板里显示的版本，每次发布手动改一下 */
const BUILD = 'v8 · 2026-10-28';

const state = {
  courtFee: '',
  shuttlecockFee: '',
  extraMaleFee: '5',

  maleCount: 0,
  femaleCount: 0,
  absentCount: 0,

  maleCost: 0,
  femaleCost: 0,
  absentCost: 0,

  lastInputs: null,

  // 按桶计算面板
  detailMode: false,
  lines: [],
  feeSnapshot: '',

  // 组合金额面板
  comboMale: 3,
  comboFemale: 0,
  comboAbsent: 0,

  openSheet: null,
};

const wheels = {};

/* -------------------------------------------------------------------------- */
/* 输入快照 / 过期判定（对应 CalculationInputs 与 needsRecalculate）             */
/* -------------------------------------------------------------------------- */

function currentInputs() {
  return {
    courtFee: safeToDouble(state.courtFee),
    badmintonFee: safeToDouble(state.shuttlecockFee),
    extraMaleFee: safeToDouble(state.extraMaleFee),
    maleCount: state.maleCount,
    femaleCount: state.femaleCount,
    absentCount: state.absentCount,
  };
}

function sameInputs(a, b) {
  if (!a || !b) return false;
  return (
    a.courtFee === b.courtFee &&
    a.badmintonFee === b.badmintonFee &&
    a.extraMaleFee === b.extraMaleFee &&
    a.maleCount === b.maleCount &&
    a.femaleCount === b.femaleCount &&
    a.absentCount === b.absentCount
  );
}

function needsRecalculate() {
  return state.lastInputs !== null && !sameInputs(state.lastInputs, currentInputs());
}

function renderStale() {
  btnCalc.dataset.stale = needsRecalculate() ? 'true' : 'false';
}

/* -------------------------------------------------------------------------- */
/* 结果区                                                                      */
/* -------------------------------------------------------------------------- */

function renderResults() {
  maleValueEl.textContent = formatFee(state.maleCost);
  femaleValueEl.textContent = formatFee(state.femaleCost);
  absentValueEl.textContent = formatFee(state.absentCost);
  resultAbsent.hidden = state.absentCount <= 0;
}

/* -------------------------------------------------------------------------- */
/* 复制 / 提示                                                                 */
/* -------------------------------------------------------------------------- */

let toastTimer = 0;

function showToast(text) {
  toastEl.textContent = text;
  toastEl.classList.add('is-visible');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => toastEl.classList.remove('is-visible'), 1500);
}

function legacyCopy(text) {
  const area = document.createElement('textarea');
  area.value = text;
  area.setAttribute('readonly', '');
  area.style.cssText = 'position:fixed;top:-1000px;left:0;opacity:0;';
  document.body.appendChild(area);
  area.select();
  area.setSelectionRange(0, text.length);
  try {
    document.execCommand('copy');
  } catch {
    /* 极端环境无剪贴板能力，忽略 */
  }
  area.remove();
}

async function copy(text) {
  showToast(t('copied'));
  try {
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(text);
      return;
    }
  } catch {
    /* 回退到 execCommand */
  }
  legacyCopy(text);
}

/* -------------------------------------------------------------------------- */
/* 计算                                                                        */
/* -------------------------------------------------------------------------- */

function calculate() {
  state.lastInputs = currentInputs();
  const result = calculateStringWithAbsent(
    state.courtFee,
    state.shuttlecockFee,
    state.extraMaleFee,
    String(state.maleCount),
    String(state.femaleCount),
    String(state.absentCount)
  );
  state.maleCost = result.maleFee;
  state.femaleCost = result.femaleFee;
  state.absentCost = result.absentFee;
  renderResults();
  renderStale();
}

/* -------------------------------------------------------------------------- */
/* 面板开关                                                                    */
/* -------------------------------------------------------------------------- */

function sheetElement(name) {
  return name === 'combo' ? comboSheet : shuttleSheet;
}

// 关闭动画结束后才清空 openSheet；期间若又打开同一个面板，必须取消这次清理
const closeTimers = { combo: 0, shuttle: 0 };

function openSheet(name) {
  const el = sheetElement(name);
  clearTimeout(closeTimers[name]);
  if (state.openSheet === name && el.classList.contains('is-open')) return;
  if (state.openSheet && state.openSheet !== name) closeSheet(state.openSheet, true);
  state.openSheet = name;
  el.classList.add('is-open');
  el.setAttribute('aria-hidden', 'false');
  document.activeElement?.blur?.();
}

function closeSheet(name = state.openSheet, immediate = false) {
  if (!name) return;
  const el = sheetElement(name);
  clearTimeout(closeTimers[name]);
  el.classList.remove('is-open');
  el.setAttribute('aria-hidden', 'true');
  if (immediate) {
    if (state.openSheet === name) state.openSheet = null;
    return;
  }
  closeTimers[name] = setTimeout(() => {
    if (state.openSheet === name) state.openSheet = null;
  }, 420);
}

/* -------------------------------------------------------------------------- */
/* 组合金额面板                                                                */
/* -------------------------------------------------------------------------- */

function renderComboBody() {
  comboScroll.textContent = '';

  const list = document.createElement('div');
  list.className = 'group';

  for (let i = 0; i < 6; i++) {
    const [male, female] = getNumCombination(i);
    const fee = calculateOther(male, female, state.maleCost, state.femaleCost);
    const text = formatFee(fee);

    const row = document.createElement('button');
    row.type = 'button';
    row.className = 'list-row';

    const label = document.createElement('span');
    label.className = 'list-row__label';
    label.textContent = t('common_num_fee_label', male, female);

    const chip = document.createElement('span');
    chip.className = 'chip';
    chip.textContent = text;

    row.append(label, chip);
    row.addEventListener('click', () => copy(text));
    list.appendChild(row);
  }

  comboScroll.appendChild(list);

  // 自定义人数
  const group = document.createElement('div');
  group.className = 'group';

  const row = document.createElement('div');
  row.className = 'custom-row';

  const wheelWrap = document.createElement('div');
  wheelWrap.className = 'custom-row__wheels';

  const chip = document.createElement('span');
  chip.className = 'chip';

  const updateCustomFee = () => {
    chip.textContent = formatFee(
      calculateOtherWithAbsent(
        state.comboMale,
        state.maleCost,
        state.comboFemale,
        state.femaleCost,
        state.comboAbsent,
        state.absentCost
      )
    );
  };

  const addField = (labelText, value, key) => {
    const field = document.createElement('div');
    field.className = 'mini-field';
    const label = document.createElement('span');
    label.className = 'mini-field__label';
    label.textContent = labelText;
    const wheel = createWheel({
      min: 0,
      max: 10,
      value,
      itemHeight: 30,
      visible: 3,
      label: labelText,
      onChange: (v) => {
        if (state[key] !== v) haptic('light');
        state[key] = v;
        updateCustomFee();
      },
    });
    wheel.el.classList.add('wheel--sm');
    field.append(label, wheel.el);
    wheelWrap.appendChild(field);
  };

  addField(t('man'), state.comboMale, 'comboMale');
  addField(t('woman'), state.comboFemale, 'comboFemale');
  // 有鸽子费用时才出现鸽子列（与原版一致）
  if (state.absentCost > 0) {
    addField(t('absent'), state.comboAbsent, 'comboAbsent');
  }

  row.append(wheelWrap, chip);
  group.appendChild(row);
  comboScroll.appendChild(group);

  updateCustomFee();
  chip.addEventListener('click', () => copy(chip.textContent));
}

/* -------------------------------------------------------------------------- */
/* 按桶计算面板                                                                */
/* -------------------------------------------------------------------------- */

function renderShuttleTile() {
  shuttleFeeEl.readOnly = state.detailMode;
  shuttleAccessory.dataset.active = state.detailMode ? 'true' : 'false';
  shuttleAccessory.textContent = state.detailMode
    ? (getLang() === 'zh' ? '桶' : '12')
    : '+';
}

function updateShuttleTotal() {
  const total = formatShuttlecockFee(shuttlecockFeeTotal(state.lines));
  state.shuttlecockFee = total;
  shuttleFeeEl.value = total;
  const chip = shuttleScroll.querySelector('[data-total]');
  if (chip) chip.textContent = total === '' ? '0' : total;
  renderStale();
}

function renderShuttleBody() {
  shuttleScroll.textContent = '';

  const head = document.createElement('div');
  head.className = 'sheet-head';

  const headText = document.createElement('div');
  const title = document.createElement('div');
  title.className = 'sheet-title';
  title.textContent = t('shuttlecock_detail_title');
  const subtitle = document.createElement('div');
  subtitle.className = 'sheet-subtitle';
  subtitle.textContent = t('shuttlecock_per_tube');
  headText.append(title, subtitle);

  const totalChip = document.createElement('span');
  totalChip.className = 'chip';
  totalChip.dataset.total = '';
  totalChip.textContent =
    formatShuttlecockFee(shuttlecockFeeTotal(state.lines)) || '0';

  head.append(headText, totalChip);
  shuttleScroll.appendChild(head);

  const list = document.createElement('div');
  list.className = 'group';

  state.lines.forEach((line) => {
    const row = document.createElement('div');
    row.className = 'shuttle-row';

    const priceLabel = document.createElement('span');
    priceLabel.className = 'field__label';
    priceLabel.textContent = t('shuttlecock_tube_price');

    const priceInput = document.createElement('input');
    priceInput.className = 'shuttle-price';
    priceInput.type = 'text';
    priceInput.inputMode = 'decimal';
    priceInput.autocomplete = 'off';
    priceInput.spellcheck = false;
    priceInput.enterKeyHint = 'done';
    priceInput.placeholder = '0';
    priceInput.value = line.tubePrice;
    priceInput.setAttribute('aria-label', t('shuttlecock_tube_price'));
    priceInput.addEventListener('input', () => {
      line.tubePrice = priceInput.value;
      updateShuttleTotal();
    });
    priceInput.addEventListener('keydown', (event) => {
      if (event.key === 'Enter') priceInput.blur();
    });

    const usedLabel = document.createElement('span');
    usedLabel.className = 'field__label';
    usedLabel.textContent = t('shuttlecock_used_count');

    const wheelHost = document.createElement('div');
    wheelHost.className = 'wheel-host';
    const wheel = createWheel({
      min: SHUTTLECOCK_COUNT_MIN,
      max: SHUTTLECOCK_COUNT_MAX,
      value: line.usedCount,
      itemHeight: 30,
      visible: 3,
      label: t('shuttlecock_used_count'),
      onChange: (v) => {
        line.usedCount = v;
        updateShuttleTotal();
      },
    });
    wheelHost.appendChild(wheel.el);

    const del = document.createElement('button');
    del.type = 'button';
    del.className = 'delete-btn';
    del.textContent = '×';
    del.setAttribute('aria-label', t('delete'));
    del.addEventListener('click', () => {
      const remaining = state.lines.filter((item) => item.id !== line.id);
      if (remaining.length === 0) {
        exitDetail();
      } else {
        state.lines = remaining;
        renderShuttleBody();
        updateShuttleTotal();
      }
    });

    row.append(priceLabel, priceInput, usedLabel, wheelHost, del);
    list.appendChild(row);
  });

  shuttleScroll.appendChild(list);

  const actions = document.createElement('div');
  actions.className = 'sheet-actions';

  const exitBtn = document.createElement('button');
  exitBtn.type = 'button';
  exitBtn.className = 'btn btn--secondary';
  exitBtn.textContent = t('shuttlecock_exit_detail');
  exitBtn.addEventListener('click', exitDetail);

  const addBtn = document.createElement('button');
  addBtn.type = 'button';
  addBtn.className = 'btn btn--primary';
  addBtn.textContent = t('shuttlecock_add_line');
  addBtn.addEventListener('click', () => {
    const line = { id: nextLineId(state.lines), tubePrice: '', usedCount: 12 };
    state.lines.push(line);
    renderShuttleBody();
    updateShuttleTotal();
    shuttleScroll.scrollTop = shuttleScroll.scrollHeight;
  });

  actions.append(exitBtn, addBtn);
  shuttleScroll.appendChild(actions);
}

function openShuttleSheet() {
  if (state.lines.length === 0) {
    state.feeSnapshot = state.shuttlecockFee;
    state.lines = [{ id: 1, tubePrice: '', usedCount: 12 }];
  }
  state.detailMode = true;
  renderShuttleTile();
  renderShuttleBody();
  openSheet('shuttle');
}

/** 退出按桶模式：有填桶价就用合计，否则恢复进入前的总价 */
function exitDetail() {
  const hadPrice = state.lines.some((line) => line.tubePrice !== '');
  const computed = formatShuttlecockFee(shuttlecockFeeTotal(state.lines));
  state.lines = [];
  state.detailMode = false;
  state.shuttlecockFee = hadPrice ? computed : state.feeSnapshot;
  shuttleFeeEl.value = state.shuttlecockFee;
  renderShuttleTile();
  renderStale();
  closeSheet('shuttle');
}

/* -------------------------------------------------------------------------- */
/* 底部面板拖拽关闭                                                            */
/* -------------------------------------------------------------------------- */

function enableSheetDrag(sheet) {
  const panel = sheet.querySelector('.sheet__panel');
  const handle = sheet.querySelector('.sheet__handle');
  let startY = 0;
  let delta = 0;
  let startTime = 0;
  let dragging = false;

  handle.addEventListener('pointerdown', (event) => {
    dragging = true;
    startY = event.clientY;
    delta = 0;
    startTime = performance.now();
    panel.style.transition = 'none';
    sheet.classList.add('is-dragging');
    handle.setPointerCapture?.(event.pointerId);
  });

  handle.addEventListener('pointermove', (event) => {
    if (!dragging) return;
    delta = Math.max(0, event.clientY - startY);
    panel.style.transform = `translate3d(0, ${delta}px, 0)`;
  });

  const finish = (event) => {
    if (!dragging) return;
    dragging = false;
    handle.releasePointerCapture?.(event.pointerId);
    const velocity = delta / Math.max(1, performance.now() - startTime);
    panel.style.transition = '';
    panel.style.transform = '';
    sheet.classList.remove('is-dragging');
    if (delta > panel.offsetHeight * 0.18 || velocity > 0.55) {
      closeSheet();
    }
  };

  handle.addEventListener('pointerup', finish);
  handle.addEventListener('pointercancel', finish);
}

/* -------------------------------------------------------------------------- */
/* 文案 / 语言                                                                 */
/* -------------------------------------------------------------------------- */

const TEXT_BINDINGS = [
  ['courtFeeLabel', 'court_fee_label'],
  ['shuttleFeeLabel', 'shuttlecock_fee_label'],
  ['extraFeeLabel', 'extra_fee_label'],
  ['manCaption', 'man'],
  ['womanCaption', 'woman'],
  ['absentCaption', 'absent'],
  ['resultMaleLabel', 'man'],
  ['resultFemaleLabel', 'woman'],
  ['resultAbsentLabel', 'absent'],
  ['installHintTitle', 'install_hint_title'],
  ['installHintBody', 'install_hint_body'],
  ['installHintOk', 'install_hint_ok'],
];

function applyI18n() {
  const lang = getLang();
  document.documentElement.lang = lang === 'zh' ? 'zh-CN' : 'en';
  document.title = t('app_name');
  appTitle.textContent = t('app_name');

  for (const [id, key] of TEXT_BINDINGS) {
    const el = $(id);
    if (el) el.textContent = t(key);
  }

  courtFeeEl.setAttribute('aria-label', t('court_fee_label'));
  shuttleFeeEl.setAttribute('aria-label', t('shuttlecock_fee_label'));
  extraInput.setAttribute('aria-label', t('extra_fee_label'));

  langBtn.textContent = t('lang_toggle');
  langBtn.setAttribute('aria-label', t('lang_toggle_label'));

  btnCalc.setAttribute('aria-label', t('calculate'));
  btnCombo.setAttribute('aria-label', t('other_fee_label'));
  comboSheet.setAttribute('aria-label', t('other_fee_label'));
  shuttleSheet.setAttribute('aria-label', t('shuttlecock_detail_title'));

  currencyGlyph.textContent = t('currency_symbol');

  wheels.male?.setLabel(t('man'));
  wheels.female?.setLabel(t('woman'));
  wheels.absent?.setLabel(t('absent'));

  const appleTitle = document.querySelector('meta[name="apple-mobile-web-app-title"]');
  if (appleTitle) appleTitle.setAttribute('content', t('app_name'));

  renderShuttleTile();
  if (state.openSheet === 'combo') renderComboBody();
  if (state.openSheet === 'shuttle') renderShuttleBody();
}

/* -------------------------------------------------------------------------- */
/* 事件绑定                                                                    */
/* -------------------------------------------------------------------------- */

// 场地费
courtFeeEl.addEventListener('input', () => {
  state.courtFee = courtFeeEl.value;
  renderStale();
});

// 羽毛球费（按桶模式下只读）
shuttleFeeEl.addEventListener('input', () => {
  if (state.detailMode) return;
  state.shuttlecockFee = shuttleFeeEl.value;
  renderStale();
});

shuttleFeeEl.addEventListener('click', () => {
  if (state.detailMode) openShuttleSheet();
});

shuttleAccessory.addEventListener('click', (event) => {
  event.preventDefault();
  openShuttleSheet();
});

// 男生多于女生费用：点一下进入编辑
function beginExtraEdit() {
  if (!extraInput.hidden) return;
  extraInput.value = state.extraMaleFee;
  extraInput.hidden = false;
  extraValueEl.hidden = true;
  extraRow.dataset.editing = 'true';
  extraInput.focus({ preventScroll: true });
  extraInput.select();
}

function endExtraEdit() {
  if (extraInput.hidden) return;
  const value = extraInput.value;
  state.extraMaleFee = value.trim() === '' ? '5' : value;
  extraValueEl.textContent = state.extraMaleFee;
  extraInput.hidden = true;
  extraValueEl.hidden = false;
  delete extraRow.dataset.editing;
  renderStale();
}

extraRow.addEventListener('click', (event) => {
  if (event.target === extraInput) return;
  beginExtraEdit();
});

extraRow.addEventListener('keydown', (event) => {
  if (event.key === 'Enter' || event.key === ' ') {
    event.preventDefault();
    beginExtraEdit();
  }
});

extraInput.addEventListener('input', () => {
  state.extraMaleFee = extraInput.value;
  renderStale();
});

extraInput.addEventListener('blur', endExtraEdit);

extraInput.addEventListener('keydown', (event) => {
  if (event.key === 'Enter') {
    event.preventDefault();
    extraInput.blur();
  }
});

// 滚轮
wheels.male = createWheel({
  min: 0,
  max: 20,
  value: state.maleCount,
  label: t('man'),
  onChange: (v) => {
    state.maleCount = v;
    renderStale();
  },
});
wheels.female = createWheel({
  min: 0,
  max: 20,
  value: state.femaleCount,
  label: t('woman'),
  onChange: (v) => {
    state.femaleCount = v;
    renderStale();
  },
});
wheels.absent = createWheel({
  min: 0,
  max: 20,
  value: state.absentCount,
  label: t('absent'),
  onChange: (v) => {
    state.absentCount = v;
    renderResults();
    renderStale();
  },
});

wheelHosts.male.appendChild(wheels.male.el);
wheelHosts.female.appendChild(wheels.female.el);
wheelHosts.absent.appendChild(wheels.absent.el);

// 结果复制
resultMale.addEventListener('click', () => copy(formatFee(state.maleCost)));
resultFemale.addEventListener('click', () => copy(formatFee(state.femaleCost)));
resultAbsent.addEventListener('click', () => copy(formatFee(state.absentCost)));

// 计算 / 组合金额
btnCalc.addEventListener('click', calculate);

btnCombo.addEventListener('click', () => {
  renderComboBody();
  openSheet('combo');
});

// 面板
comboSheet.querySelectorAll('[data-close]').forEach((el) => {
  el.addEventListener('click', () => closeSheet('combo'));
});
shuttleSheet.querySelectorAll('[data-close]').forEach((el) => {
  el.addEventListener('click', () => closeSheet('shuttle'));
});

enableSheetDrag(comboSheet);
enableSheetDrag(shuttleSheet);

document.addEventListener('keydown', (event) => {
  if (event.key === 'Escape' && state.openSheet) closeSheet();
});

// 语言
langBtn.addEventListener('click', () => {
  setLang(getLang() === 'zh' ? 'en' : 'zh');
  applyI18n();
});

// 安装引导（iOS Safari 无安装提示，只能手把手教）
installHintOk.addEventListener('click', () => {
  installHint.hidden = true;
  try {
    localStorage.setItem('bfc.installHint', 'seen');
  } catch {
    /* 忽略 */
  }
});

/* -------------------------------------------------------------------------- */
/* 视口高度（iOS 键盘弹出时保持按钮区可见）                                     */
/* -------------------------------------------------------------------------- */

const dock = $('dock');

function syncDockHeight() {
  document.documentElement.style.setProperty('--dock-h', `${dock.offsetHeight}px`);
}

/** 把 env(safe-area-inset-*) 解析成像素值 */
function envPx(prop) {
  const probe = document.createElement('div');
  probe.style.cssText = `position:fixed;top:0;left:0;width:0;height:env(${prop})`;
  document.body.append(probe);
  const value = Math.round(probe.getBoundingClientRect().height);
  probe.remove();
  return value;
}

function syncViewport() {
  const doc = document.documentElement;
  const layout = Math.round(doc.clientHeight || window.innerHeight);
  const vv = window.visualViewport;
  const visible = vv ? Math.round(vv.height) : layout;

  // 骨架本身用 position:fixed + inset:0 铺满 webview，不依赖任何"猜出来的高度"——
  // iOS 主屏运行时 clientHeight / visualViewport.height / screen.height 三者对不上，
  // 之前按它们算出来的高度会短一截，底部露出 body 背景（看起来像被什么挡住了）。
  // 唯一需要干预的是键盘：可视高度明显变小时把骨架收到可视区，否则底部按钮被顶出屏幕。
  const keyboardOpen = visible < layout * 0.75;
  if (keyboardOpen) {
    doc.style.setProperty('--app-height', `${visible}px`);
    return;
  }

  // 键盘没弹：高度交还给 inset:0，骨架正好铺满可视区。
  doc.style.removeProperty('--app-height');

  const standalone =
    window.navigator.standalone === true ||
    window.matchMedia('(display-mode: standalone)').matches;
  const screenH = Math.round(window.screen?.height ?? 0);
  const deadBottom = screenH - layout;

  // 主屏运行时可视区可能比屏幕矮一条（实测 874-812=62），那一条页面里的元素够不着，
  // 只有 canvas 背景能填色。所以：
  //   1) 骨架高度保持 = 可视区（让渐变的收尾正好落在裁剪线上，接上底部的同色）
  //   2) dock 不再额外留 safe-area，否则和那条重复留白，按钮会被顶高一截
  // 判断"底部死区"是否真的存在：
  //   内容压在状态栏下面（上安全区 > 0），且 可视高 + 上安全区 ≈ 屏幕高
  //   → 说明视口是从屏幕顶端算的，底部那一条被系统占掉了，页面画不过去。
  // 这时 dock 不能再额外留 safe-area，否则会重复留白，按钮被顶高一截。
  const insetTop = standalone ? envPx('safe-area-inset-top') : 0;
  const underStatusBar = insetTop > 0 && layout + insetTop >= screenH - 8;
  const dead = underStatusBar ? Math.max(0, deadBottom) : 0;

  doc.style.setProperty('--dead-bottom', `${dead}px`);
  doc.style.setProperty(
    '--safe-bottom',
    dead > 30 ? '0px' : 'env(safe-area-inset-bottom)'
  );
}


/* -------------------------------------------------------------------------- */
/* 诊断面板：长按标题 0.8s，或 ?diag=1                                          */
/* 主屏独立运行时用户没法清缓存，这个面板用来确认"跑的是不是最新版"、           */
/* 看 iOS 到底报了哪些视口数值，并一键强制更新。                                */
/* -------------------------------------------------------------------------- */

function closeDiagnostics() {
  document.querySelector('.diag')?.remove();
  document.querySelector('.diag-probes')?.remove();
}

async function openDiagnostics() {
  if (document.querySelector('.diag')) return;

  const cacheNames = 'caches' in window ? await caches.keys().catch(() => []) : [];
  const registration = navigator.serviceWorker?.controller ? '有' : '无';
  const standalone =
    window.navigator.standalone === true ||
    window.matchMedia('(display-mode: standalone)').matches;

  // env() 的解析值：iOS 上 safe-area-inset-top 是不是 0，决定内容有没有压在状态栏下面
  const insetTop = envPx('safe-area-inset-top');
  const insetBottom = envPx('safe-area-inset-bottom');

  const lines = [
    `版本      ${BUILD}`,
    `独立运行  ${standalone ? '是' : '否'}`,
    `SW 控制   ${registration}`,
    `缓存      ${cacheNames.join(', ') || '（无）'}`,
    '',
    `innerWH   ${window.innerWidth} × ${window.innerHeight}`,
    `clientWH  ${document.documentElement.clientWidth} × ${document.documentElement.clientHeight}`,
    `screenWH  ${window.screen?.width} × ${window.screen?.height}`,
    `visualVP  ${window.visualViewport ? Math.round(window.visualViewport.width) + ' × ' + Math.round(window.visualViewport.height) : '（无）'}`,
    `DPR       ${window.devicePixelRatio}`,
    `--app-h   ${document.documentElement.style.getPropertyValue('--app-height') || '（未设置）'}`,
    `骨架底边  ${Math.round(document.querySelector('.app').getBoundingClientRect().bottom)}`,
    `安全区    上 ${insetTop} / 下 ${insetBottom}`,
    `屏幕差    screenH − clientH = ${Math.round((window.screen?.height ?? 0) - document.documentElement.clientHeight)}`,
    `--dead-b  ${document.documentElement.style.getPropertyValue('--dead-bottom') || '（未设置）'}`,
    `--safe-b  ${document.documentElement.style.getPropertyValue('--safe-bottom') || '（未设置）'}`,
    `洋红条    = 内容顶端（top:0）`,
    `青色条    = 内容底端（bottom:0）`,
  ];

  const root = document.createElement('div');
  root.className = 'diag';
  root.innerHTML = `
    <div class="diag__card" role="dialog" aria-modal="true" aria-label="诊断信息">
      <h2 class="diag__title">诊断信息</h2>
      <pre class="diag__body"></pre>
      <div class="diag__actions">
        <button class="diag__btn" type="button" data-act="buzz">测试震动</button>
        <button class="diag__btn" type="button" data-act="close">关闭</button>
        <button class="diag__btn diag__btn--primary" type="button" data-act="reset">清缓存并重启</button>
      </div>
    </div>`;
  root.querySelector('.diag__body').textContent = lines.join(String.fromCharCode(10));

  root.addEventListener('click', async (event) => {
    const button = event.target.closest('[data-act]');
    if (!button) {
      if (event.target === root) closeDiagnostics();
      return;
    }
    const act = button.dataset.act;
    if (act === 'close') {
      closeDiagnostics();
    } else if (act === 'buzz') {
      button.textContent = typeof navigator.vibrate === 'function'
        ? 'vibrate 可用，已触发'
        : '无 vibrate，走 switch';
      haptic('heavy');
      document.querySelector('label.haptic-switch')?.click();
    } else if (act === 'reset') {
      button.textContent = '清理中…';
      try {
        const regs = await navigator.serviceWorker?.getRegistrations?.() ?? [];
        await Promise.all(regs.map((r) => r.unregister()));
        await Promise.all((await caches.keys()).map((k) => caches.delete(k)));
      } catch {
        /* 清理失败也照样 reload，至少能拿一次网络版 */
      }
      location.reload();
    }
  });

  // 两条探针：洋红贴着可视区顶，青色贴着可视区底。它们同时出现在截图里的位置，
  // 就能判断内容有没有被裁剪、以及裁在哪。
  const probes = document.createElement('div');
  probes.className = 'diag-probes';
  probes.innerHTML =
    '<div class="diag-probe diag-probe--top"></div><div class="diag-probe diag-probe--bottom"></div>';

  document.body.append(root, probes);
}

function setupDiagnostics() {
  if (new URLSearchParams(location.search).has('diag')) {
    openDiagnostics();
    return;
  }
  const title = document.querySelector('.topbar__title');
  if (!title) return;
  let timer = 0;
  const cancel = () => clearTimeout(timer);
  title.addEventListener('pointerdown', () => {
    cancel();
    timer = setTimeout(openDiagnostics, 800);
  });
  for (const type of ['pointerup', 'pointerleave', 'pointercancel', 'pointermove']) {
    title.addEventListener(type, cancel);
  }
}

/* -------------------------------------------------------------------------- */
/* 启动                                                                        */
/* -------------------------------------------------------------------------- */

function applyThemeFromQuery() {
  const theme = new URLSearchParams(location.search).get('theme');
  if (theme === 'dark' || theme === 'light') {
    document.documentElement.dataset.theme = theme;
  }
}

function setupInstallHint() {
  const isStandalone =
    window.navigator.standalone === true ||
    window.matchMedia('(display-mode: standalone)').matches;
  const isIOS =
    /iPad|iPhone|iPod/.test(navigator.userAgent) ||
    (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1);
  let seen = false;
  try {
    seen = localStorage.getItem('bfc.installHint') === 'seen';
  } catch {
    /* 忽略 */
  }
  if (isIOS && !isStandalone && !seen) installHint.hidden = false;
}

function registerServiceWorker() {
  if (!('serviceWorker' in navigator)) return;
  if (location.protocol === 'file:') return;

  // 新版本接管后自动刷一次，避免"发布了我这儿还是旧的"；
  // 首次安装时页面本来就没有 controller，不刷。
  const hadController = Boolean(navigator.serviceWorker.controller);
  let reloaded = false;
  navigator.serviceWorker.addEventListener('controllerchange', () => {
    if (!hadController || reloaded) return;
    reloaded = true;
    location.reload();
  });
  // GitHub Pages 会部署到 /<repo>/ 子路径，scope 必须按 sw.js 所在目录推导，
  // 不能写成 '../'（那会解析到站点根目录而被浏览器拒绝）。
  const swUrl = new URL('../sw.js', import.meta.url);
  const scope = new URL('./', swUrl);
  window.addEventListener('load', () => {
    navigator.serviceWorker
      .register(swUrl, { scope: scope.pathname, updateViaCache: 'none' })
      .then((registration) => {
        // 从主屏切回来时顺手查一次更新，避免在旧版本里停留好几天
        document.addEventListener('visibilitychange', () => {
          if (document.visibilityState === 'visible') registration.update().catch(() => {});
        });
      })
      .catch(() => {
        /* 离线能力注册失败不影响主流程 */
      });
  });
}

applyThemeFromQuery();
applyI18n();
renderResults();
renderStale();
renderShuttleTile();
syncViewport();
syncDockHeight();
if ('ResizeObserver' in window) new ResizeObserver(syncDockHeight).observe(dock);
setupInstallHint();
setupDiagnostics();
bindGlobalHaptics();
registerServiceWorker();

window.visualViewport?.addEventListener('resize', syncViewport);
window.visualViewport?.addEventListener('scroll', syncViewport);
window.addEventListener('resize', syncViewport);
window.addEventListener('orientationchange', () => setTimeout(syncViewport, 120));

// 首屏滚轮需要等布局稳定后再校正一次
requestAnimationFrame(() => {
  wheels.male.refresh();
  wheels.female.refresh();
  wheels.absent.refresh();
});
