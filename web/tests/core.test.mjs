/**
 * 数值一致性测试。
 *
 * 基准数据 jvm-parity.json 由真实的 Android 源码在 JVM 上生成
 * （见 tests/jvm/ParityFixture.java，直接调用 FeeCalculate.java）。
 * 这里要求 double 结果「逐位相等」，而不是近似相等，
 * 以确保 Web 版和原 App 算出的钱一分不差。
 *
 * 运行：node --test web/tests
 */
import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

import {
  calculate,
  calculateOther,
  calculateOtherWithAbsent,
  calculateStringWithAbsent,
  calculateWithAbsent,
  formatFee,
  formatShuttlecockFee,
  getNumCombination,
  nextLineId,
  safeToDouble,
  safeToInt,
  shuttlecockFeeTotal,
  shuttlecockLineFee,
} from '../assets/core.js';

const here = dirname(fileURLToPath(import.meta.url));
const fixture = JSON.parse(readFileSync(join(here, 'fixtures/jvm-parity.json'), 'utf8'));

/** JVM 的 Double.toString 字符串 -> double，可无损往返（含 NaN / Infinity） */
const toNum = (s) => Number(s);

function expectSame(actual, jvmString, label) {
  assert.ok(
    Object.is(actual, toNum(jvmString)),
    `${label}: Web=${actual} JVM=${jvmString}`
  );
}

test('calculateWithAbsent 与 JVM 逐位一致（413 组）', () => {
  for (const c of fixture.feeCases) {
    const [courtFee, shuttlecockFee, moreFee, male, female, absent] = c.in;
    const got = calculateWithAbsent(courtFee, shuttlecockFee, moreFee, male, female, absent);
    for (let i = 0; i < 3; i++) {
      expectSame(got[i], c.withAbsent[i], `case ${JSON.stringify(c.in)} idx ${i}`);
    }
  }
});

test('calculate 与 JVM 逐位一致', () => {
  for (const c of fixture.feeCases) {
    const [courtFee, shuttlecockFee, moreFee, male, female] = c.in;
    const got = calculate(courtFee, shuttlecockFee, moreFee, male, female);
    expectSame(got[0], c.plain[0], `male ${JSON.stringify(c.in)}`);
    expectSame(got[1], c.plain[1], `female ${JSON.stringify(c.in)}`);
  }
});

test('没有鸽子时，带鸽子算法与旧算法结果相同（原 FeeCalculateText 的断言）', () => {
  for (const c of fixture.feeCases) {
    const [courtFee, shuttlecockFee, moreFee, male, female, absent] = c.in;
    if (absent !== 0) continue;
    const withAbsent = calculateWithAbsent(courtFee, shuttlecockFee, moreFee, male, female, 0);
    const plain = calculate(courtFee, shuttlecockFee, moreFee, male, female);
    assert.ok(Object.is(withAbsent[0], plain[0]), `male @ ${JSON.stringify(c.in)}`);
    assert.ok(Object.is(withAbsent[1], plain[1]), `female @ ${JSON.stringify(c.in)}`);
    assert.equal(withAbsent[2], 0, '无鸽子时鸽子费用为 0');
  }
});

test('收上来的钱不少于成本（缺省不亏本，且溢出不超过取整误差）', () => {
  for (const c of fixture.feeCases) {
    const [courtFee, shuttlecockFee, moreFee, male, female, absent] = c.in;
    const [maleFee, femaleFee, absentFee] = calculateWithAbsent(
      courtFee, shuttlecockFee, moreFee, male, female, absent
    );
    const cost = courtFee + shuttlecockFee;
    const collected = maleFee * male + femaleFee * female + absentFee * absent;
    if (!Number.isFinite(collected) || !Number.isFinite(cost)) continue;
    // 逐项向上取整，最多每人多收 0.1
    const people = male + female + absent;
    assert.ok(
      collected >= cost - 1e-9,
      `亏本: ${JSON.stringify(c.in)} collected=${collected} cost=${cost}`
    );
    assert.ok(
      collected <= cost + 0.1 * people + 1e-9,
      `多收过多: ${JSON.stringify(c.in)} collected=${collected} cost=${cost}`
    );
  }
});

test('鸽子的费用等于平摊的场地费', () => {
  for (const c of fixture.feeCases) {
    const [courtFee, shuttlecockFee, moreFee, male, female, absent] = c.in;
    if (absent <= 0) continue;
    const absentFee = calculateWithAbsent(
      courtFee, shuttlecockFee, moreFee, male, female, absent
    )[2];
    const expected = Math.ceil((courtFee / (male + female + absent)) * 10) / 10;
    assert.ok(Object.is(absentFee, expected), JSON.stringify(c.in));
  }
});

test('常用人数组合金额与 JVM 一致', () => {
  for (const c of fixture.combinationCases) {
    const got = calculateOther(c.male, c.female, c.maleFee, c.femaleFee);
    expectSame(got, c.fee, `combo ${c.male}/${c.female}`);

    const gotWithAbsent = calculateOtherWithAbsent(
      c.male, c.maleFee, c.female, c.femaleFee, 2, 30.0
    );
    expectSame(gotWithAbsent, c.feeWithAbsent, `comboAbsent ${c.male}/${c.female}`);
  }
});

test('按桶计算：单行费用与 JVM 一致（296 组）', () => {
  for (const c of fixture.shuttlecockCases) {
    const price = c.price === '' ? 0 : Number(c.price);
    const got = shuttlecockLineFee(price, c.used);
    expectSame(got, c.fee, `price=${c.price} used=${c.used}`);
    assert.equal(
      formatShuttlecockFee(got),
      c.formatted,
      `format price=${c.price} used=${c.used}`
    );
  }
});

test('按桶计算：多行汇总与 JVM 一致', () => {
  for (const t of fixture.totals) {
    const total = shuttlecockFeeTotal(t.lines);
    expectSame(total, t.total, `total ${JSON.stringify(t.lines)}`);
    assert.equal(formatShuttlecockFee(total), t.formatted);
  }
});

test('ShuttlecockFeeTest.kt 原有用例', () => {
  assert.equal(shuttlecockLineFee(80, 6), 40);
  assert.equal(shuttlecockLineFee(100, 12), 100);
  assert.equal(
    shuttlecockFeeTotal([
      { id: 1, tubePrice: '80', usedCount: 6 },
      { id: 2, tubePrice: '100', usedCount: 12 },
    ]),
    140
  );
  assert.equal(shuttlecockFeeTotal([{ id: 1, tubePrice: '', usedCount: 12 }]), 0);
  assert.equal(formatShuttlecockFee(40), '40');
  assert.equal(formatShuttlecockFee((85 / 12) * 7), '49.6');
  assert.equal(formatShuttlecockFee(0), '');
});

test('计数为 0 时单行费用为 0', () => {
  assert.equal(shuttlecockLineFee(999, 0), 0);
  assert.equal(shuttlecockLineFee(999, -1), 0);
});

test('safeToDouble / safeToInt 与 Kotlin 解析行为一致', () => {
  assert.equal(safeToDouble(''), 0);
  assert.equal(safeToDouble('   '), 0);
  assert.equal(safeToDouble('12.5'), 12.5);
  assert.equal(safeToDouble(' 12.5 '), 12.5);
  assert.equal(safeToDouble('.5'), 0.5);
  assert.equal(safeToDouble('1e3'), 1000);
  assert.equal(safeToDouble('12px'), 0);
  assert.equal(safeToDouble('abc'), 0);
  assert.equal(safeToDouble('--1'), 0);
  assert.equal(safeToDouble(null), 0);
  assert.equal(safeToDouble(undefined), 0);
  assert.equal(safeToDouble(7), 7);

  assert.equal(safeToInt(''), 0);
  assert.equal(safeToInt('5'), 5);
  assert.equal(safeToInt(' 5 '), 5);
  assert.equal(safeToInt('5.5'), 0);
  assert.equal(safeToInt('5x'), 0);
  assert.equal(safeToInt(null), 0);
});

test('calculateStringWithAbsent 解析字符串入参', () => {
  const r = calculateStringWithAbsent('300', '200', '5', '3', '2', '1');
  const direct = calculateWithAbsent(300, 200, 5, 3, 2, 1);
  assert.ok(Object.is(r.maleFee, direct[0]));
  assert.ok(Object.is(r.femaleFee, direct[1]));
  assert.ok(Object.is(r.absentFee, direct[2]));

  // 非法输入按 0 处理
  const z = calculateStringWithAbsent('abc', 'xyz', '', '', '', '');
  assert.ok(Number.isNaN(z.maleFee));
});

test('getNumCombination 与 Kotlin 列表一致', () => {
  assert.deepEqual(getNumCombination(0), [0, 2]);
  assert.deepEqual(getNumCombination(1), [1, 1]);
  assert.deepEqual(getNumCombination(2), [1, 2]);
  assert.deepEqual(getNumCombination(3), [2, 0]);
  assert.deepEqual(getNumCombination(4), [2, 1]);
  assert.deepEqual(getNumCombination(5), [2, 2]);
  assert.deepEqual(getNumCombination(99), [0, 0]);
});

test('nextLineId 递增', () => {
  assert.equal(nextLineId([]), 1);
  assert.equal(nextLineId([{ id: 1 }, { id: 2 }]), 3);
  assert.equal(nextLineId([{ id: 5 }, { id: 2 }]), 6);
});

test('金额展示：去掉多余的 .0，非有限值用占位符', () => {
  assert.equal(formatFee(25.0), '25');
  assert.equal(formatFee(33.4), '33.4');
  assert.equal(formatFee(0), '0');
  assert.equal(formatFee(-0), '0');
  assert.equal(formatFee(Number.NaN), '—');
  assert.equal(formatFee(Number.POSITIVE_INFINITY), '—');
  assert.equal(formatFee(1234.5), '1234.5');
});
