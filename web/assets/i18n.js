/**
 * 极简 i18n：跟随系统语言（与 Android 版按 locale 取 strings.xml 的行为一致）。
 * 用户手动切换后写入 localStorage，下次进入沿用。
 */

export const STRINGS = {
  zh: {
    app_name: '球费计算器',
    court_fee_label: '场地费',
    shuttlecock_fee_label: '羽毛球费',
    extra_fee_label: '男生多于女生费用',
    man: '男',
    woman: '女',
    absent: '鸽子',
    calculate: '计算',
    other_fee_label: '组合金额',
    currency_symbol: '￥',
    common_num_fee_label: '{0} 男 {1} 女',
    shuttlecock_detail_title: '按桶计算',
    shuttlecock_per_tube: '一桶 12 颗',
    shuttlecock_tube_price: '桶价',
    shuttlecock_used_count: '颗数',
    shuttlecock_add_line: '添加一种球',
    shuttlecock_exit_detail: '改回总价',
    delete: '删除',
    copied: '已复制',
    lang_toggle: 'EN',
    lang_toggle_label: 'Switch to English',
    install_hint_title: '添加到主屏幕',
    install_hint_body: '点 Safari 的分享按钮，选择“添加到主屏幕”，即可像 App 一样全屏使用。',
    install_hint_ok: '知道了',
  },
  en: {
    app_name: 'Ball Fee',
    court_fee_label: 'Court Fee',
    shuttlecock_fee_label: 'Shuttlecock Fee',
    extra_fee_label: 'Male Extra Fee',
    man: 'Man',
    woman: 'Woman',
    absent: 'Absent',
    calculate: 'Calculate',
    other_fee_label: 'Combined Fee',
    currency_symbol: '$',
    common_num_fee_label: '{0} Man {1} Woman',
    shuttlecock_detail_title: 'By tube',
    shuttlecock_per_tube: '12 shuttlecocks per tube',
    shuttlecock_tube_price: 'Tube price',
    shuttlecock_used_count: 'Used',
    shuttlecock_add_line: 'Add a tube type',
    shuttlecock_exit_detail: 'Edit total instead',
    delete: 'Delete',
    copied: 'Copied',
    lang_toggle: '中',
    lang_toggle_label: '切换到中文',
    install_hint_title: 'Add to Home Screen',
    install_hint_body: 'Tap Share in Safari, then “Add to Home Screen” to use it full screen like an app.',
    install_hint_ok: 'Got it',
  },
};

const STORAGE_KEY = 'bfc.lang';

export function detectLang() {
  try {
    const saved = localStorage.getItem(STORAGE_KEY);
    if (saved === 'zh' || saved === 'en') return saved;
  } catch {
    /* 隐私模式下 localStorage 可能不可用 */
  }
  const list = navigator.languages && navigator.languages.length
    ? navigator.languages
    : [navigator.language || 'zh'];
  const first = String(list[0] || '').toLowerCase();
  return first.startsWith('zh') ? 'zh' : 'en';
}

let current = detectLang();

export function getLang() {
  return current;
}

export function setLang(lang, persist = true) {
  current = lang === 'zh' ? 'zh' : 'en';
  if (persist) {
    try {
      localStorage.setItem(STORAGE_KEY, current);
    } catch {
      /* 忽略写入失败 */
    }
  }
  return current;
}

export function t(key, ...args) {
  const table = STRINGS[current] || STRINGS.zh;
  let text = table[key] != null ? table[key] : key;
  for (let i = 0; i < args.length; i++) {
    text = text.replaceAll(`{${i}}`, String(args[i]));
  }
  return text;
}
