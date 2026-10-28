/**
 * 计算核心 —— 与 Android 版逐行对应，纯函数、无副作用、无依赖。
 *
 * 对应来源：
 *   FeeCalculate.java  -> calculate / calculateWithAbsent / calculateOther / calculateOtherWithAbsent
 *   ShuttlecockFee.kt  -> shuttlecockLineFee / shuttlecockFeeTotal / formatShuttlecockFee / nextLineId
 *   Calculator.kt      -> calculateString / calculateStringWithAbsent / getNumCombination
 *
 * 数值语义必须与 JVM 一致：IEEE-754 double、Math.ceil、Math.round（.5 向 +∞），
 * 因此刻意保留 "除以 0 得 Infinity/NaN" 的行为，展示层再兜底。
 */

export const SHUTTLECOCKS_PER_TUBE = 12;
export const SHUTTLECOCK_COUNT_MIN = 1;
export const SHUTTLECOCK_COUNT_MAX = 36;

/** 与 Kotlin 的 toDoubleOrNull() ?: 0.0 等价（整串匹配，不做部分解析） */
const NUMERIC_RE = /^[+-]?(?:\d+(?:\.\d*)?|\.\d+)(?:[eE][+-]?\d+)?$/;
/** 与 Kotlin 的 toIntOrNull() ?: 0 等价 */
const INTEGER_RE = /^[+-]?\d+$/;

export function safeToDouble(value) {
  if (typeof value === 'number') return Number.isFinite(value) ? value : 0;
  if (typeof value !== 'string') return 0;
  const trimmed = value.trim();
  if (!NUMERIC_RE.test(trimmed)) return 0;
  const parsed = Number(trimmed);
  return Number.isFinite(parsed) ? parsed : 0;
}

export function safeToInt(value) {
  if (typeof value === 'number') return Number.isFinite(value) ? Math.trunc(value) : 0;
  if (typeof value !== 'string') return 0;
  const trimmed = value.trim();
  if (!INTEGER_RE.test(trimmed)) return 0;
  const parsed = Number(trimmed);
  return Number.isFinite(parsed) ? Math.trunc(parsed) : 0;
}

/** Math.ceil(fee * 10) / 10 的别名，保留 JVM 语义 */
export function ceilToTenth(value) {
  return Math.ceil(value * 10) / 10;
}

/**
 * 场地费 + 羽毛球费按男/女分摊，男生比女生多收 moreFee。
 * @returns {[number, number]} [男生球费, 女生球费]
 */
export function calculate(courtFee, shuttlecockFee, moreFee, maleCount, femaleCount) {
  const femaleFee = (courtFee + shuttlecockFee - moreFee * maleCount) / (femaleCount + maleCount);
  const maleFee = femaleFee + moreFee;
  return [ceilToTenth(maleFee), ceilToTenth(femaleFee)];
}

/**
 * 带“鸽子”（占用场地但没打球）的分摊：场地费按总人数平摊，
 * 鸽子只付自己那一份场地费，其余球费由到场的人分摊。
 * @returns {[number, number, number]} [男生球费, 女生球费, 鸽子费用]
 */
export function calculateWithAbsent(
  courtFee,
  shuttlecockFee,
  moreFee,
  maleCount,
  femaleCount,
  absentCount
) {
  const totalCount = maleCount + femaleCount + absentCount;
  const perCourtFee = courtFee / totalCount;
  const absentFee = absentCount > 0 ? perCourtFee : 0;
  const femaleFee = perCourtFee + (shuttlecockFee - moreFee * maleCount) / (femaleCount + maleCount);
  const maleFee = femaleFee + moreFee;
  return [
    ceilToTenth(maleFee),
    ceilToTenth(femaleFee),
    ceilToTenth(absentFee),
  ];
}

/** 常用人数组合应收费金额（保留 1 位小数，四舍五入） */
export function calculateOther(maleCount, femaleCount, maleFee, femaleFee) {
  const fee = maleFee * maleCount + femaleFee * femaleCount;
  return Math.round(fee * 10) / 10;
}

/** 含鸽子的常用人数组合应收费金额 */
export function calculateOtherWithAbsent(
  maleCount,
  maleFee,
  femaleCount,
  femaleFee,
  absentCount,
  absentFee
) {
  const fee = maleFee * maleCount + femaleFee * femaleCount + absentCount * absentFee;
  return Math.round(fee * 10) / 10;
}

/* ------------------------------------------------------------------ */
/* 按桶计算的羽毛球费                                                  */
/* ------------------------------------------------------------------ */

export function shuttlecockLineFee(tubePrice, usedCount) {
  if (usedCount <= 0) return 0;
  return (tubePrice / SHUTTLECOCKS_PER_TUBE) * usedCount;
}

export function shuttlecockFeeTotal(lines) {
  return lines.reduce(
    (sum, line) => sum + shuttlecockLineFee(safeToDouble(line.tubePrice), line.usedCount),
    0
  );
}

/** 保留 1 位小数；整数去掉 .0；为 0 时返回空串（输入框显示为空） */
export function formatShuttlecockFee(total) {
  const rounded = Math.round(total * 10) / 10;
  if (rounded === 0) return '';
  return Number.isInteger(rounded) ? String(rounded) : rounded.toFixed(1);
}

export function nextLineId(lines) {
  return lines.reduce((max, line) => Math.max(max, line.id), 0) + 1;
}

/** 展示/复制用的金额文案：最多 1 位小数，去掉多余的 .0；非有限值显示占位符 */
export function formatFee(value) {
  if (!Number.isFinite(value)) return '—';
  const rounded = Math.round(value * 10) / 10;
  if (rounded === 0) return '0';
  return Number.isInteger(rounded) ? String(rounded) : rounded.toFixed(1);
}

/** 组合金额面板里的 6 组常用人数（对应 getNumCombination 的 0..5） */
export const NUM_COMBINATIONS = Object.freeze([
  Object.freeze([0, 2]),
  Object.freeze([1, 1]),
  Object.freeze([1, 2]),
  Object.freeze([2, 0]),
  Object.freeze([2, 1]),
  Object.freeze([2, 2]),
]);

export function getNumCombination(index) {
  const pair = NUM_COMBINATIONS[index];
  return pair ? [pair[0], pair[1]] : [0, 0];
}

/** 对应 calculateString(...) */
export function calculateString(courtFee, shuttlecockFee, extraMaleFee, maleCount, femaleCount) {
  const [maleFee, femaleFee] = calculate(
    safeToDouble(courtFee),
    safeToDouble(shuttlecockFee),
    safeToDouble(extraMaleFee),
    safeToInt(maleCount),
    safeToInt(femaleCount)
  );
  return { maleFee, femaleFee, absentFee: 0 };
}

/** 对应 calculateStringWithAbsent(...) */
export function calculateStringWithAbsent(
  courtFee,
  shuttlecockFee,
  extraMaleFee,
  maleCount,
  femaleCount,
  absentCount
) {
  const [maleFee, femaleFee, absentFee] = calculateWithAbsent(
    safeToDouble(courtFee),
    safeToDouble(shuttlecockFee),
    safeToDouble(extraMaleFee),
    safeToInt(maleCount),
    safeToInt(femaleCount),
    safeToInt(absentCount)
  );
  return { maleFee, femaleFee, absentFee };
}
