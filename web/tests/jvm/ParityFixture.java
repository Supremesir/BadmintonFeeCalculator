package com.supremesir.badmintonfeecalculator;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 生成 JVM 侧基准数据（fixture），用于校验 web/assets/core.js 的数值一致性。
 *
 * 直接调用真实的 FeeCalculate.java；ShuttlecockFee.kt 因为纯 Kotlin 无法在此编译，
 * 这里按其源码逐句转写（Math.round / String.format(Locale.US, "%.1f") 即 Kotlin round()
 * 与 String.format 的编译结果）。
 *
 * 用法：
 *   javac -d <out> app/src/main/java/com/supremesir/badmintonfeecalculator/FeeCalculate.java \
 *                  web/tests/jvm/ParityFixture.java
 *   java -cp <out> com.supremesir.badmintonfeecalculator.ParityFixture > web/tests/fixtures/jvm-parity.json
 *
 * 浮点数一律以 Double.toString() 输出字符串，保证 NaN / Infinity / 精度都能无损往返。
 */
public final class ParityFixture {

    /** ShuttlecockFee.kt: formatShuttlecockFee */
    private static String formatShuttlecockFee(double total) {
        double rounded = Math.round(total * 10.0) / 10.0;
        if (rounded == 0.0) return "";
        return (rounded % 1.0 == 0.0)
                ? String.valueOf((int) rounded)
                : String.format(Locale.US, "%.1f", rounded);
    }

    /** ShuttlecockFee.kt: shuttlecockLineFee */
    private static double shuttlecockLineFee(double tubePrice, int usedCount) {
        if (usedCount <= 0) return 0.0;
        return tubePrice / 12 * usedCount;
    }

    private static String q(double v) {
        return "\"" + Double.toString(v) + "\"";
    }

    public static void main(String[] args) {
        StringBuilder sb = new StringBuilder("{\n");

        // ---------------- 主计算 ----------------
        List<double[]> inputs = new ArrayList<>();
        List<Integer[]> counts = new ArrayList<>();

        long seed = 20261028L;
        java.util.Random rnd = new java.util.Random(seed);
        // 有种子随机，覆盖常见取值
        for (int i = 0; i < 400; i++) {
            double court = (200 + rnd.nextInt(201)) + (rnd.nextInt(4) / 4.0) * 0.5;
            double shuttle = (150 + rnd.nextInt(251)) + Math.round(rnd.nextDouble() * 10) / 10.0;
            double more = new double[]{0, 3, 5, 8, 10, 12.5}[rnd.nextInt(6)];
            int male = rnd.nextInt(9);
            int female = rnd.nextInt(9);
            int absent = rnd.nextInt(6);
            inputs.add(new double[]{court, shuttle, more});
            counts.add(new Integer[]{male, female, absent});
        }
        // 边界与极端值
        inputs.add(new double[]{0, 0, 5});
        counts.add(new Integer[]{0, 0, 0});      // 0/0 -> NaN
        inputs.add(new double[]{0, 0, 5});
        counts.add(new Integer[]{1, 1, 0});
        inputs.add(new double[]{300, 200, 5});
        counts.add(new Integer[]{0, 0, 3});      // 只剩鸽子
        inputs.add(new double[]{300, 0, 5});
        counts.add(new Integer[]{1, 0, 0});
        inputs.add(new double[]{0, 300, 5});
        counts.add(new Integer[]{0, 1, 0});
        inputs.add(new double[]{333.33, 199.99, 5});
        counts.add(new Integer[]{3, 4, 2});
        inputs.add(new double[]{1, 1, 5});
        counts.add(new Integer[]{1, 1, 1});
        inputs.add(new double[]{1e9, 1e9, 5});
        counts.add(new Integer[]{7, 7, 3});
        inputs.add(new double[]{300, 200, 0});
        counts.add(new Integer[]{4, 4, 0});
        inputs.add(new double[]{300, 200, -5});
        counts.add(new Integer[]{2, 2, 0});      // 负加价
        inputs.add(new double[]{-100, 200, 5});
        counts.add(new Integer[]{2, 3, 1});      // 负场地费
        inputs.add(new double[]{0.05, 0.05, 5});
        counts.add(new Integer[]{1, 1, 1});
        inputs.add(new double[]{300, 200, 5});
        counts.add(new Integer[]{20, 20, 20});

        sb.append("  \"feeCases\": [\n");
        for (int i = 0; i < inputs.size(); i++) {
            double[] in = inputs.get(i);
            Integer[] c = counts.get(i);
            double[] withAbsent = FeeCalculate.calculateWithAbsent(in[0], in[1], in[2], c[0], c[1], c[2]);
            double[] plain = FeeCalculate.calculate(in[0], in[1], in[2], c[0], c[1]);
            sb.append("    {\"in\": [")
              .append(in[0]).append(", ").append(in[1]).append(", ").append(in[2]).append(", ")
              .append(c[0]).append(", ").append(c[1]).append(", ").append(c[2]).append("], ")
              .append("\"withAbsent\": [").append(q(withAbsent[0])).append(", ").append(q(withAbsent[1])).append(", ").append(q(withAbsent[2])).append("], ")
              .append("\"plain\": [").append(q(plain[0])).append(", ").append(q(plain[1])).append("]}")
              .append(i == inputs.size() - 1 ? "\n" : ",\n");
        }
        sb.append("  ],\n");

        // ---------------- 组合金额 ----------------
        double[][] combos = {
                {0, 2, 25.0, 20.0}, {1, 1, 33.5, 28.5}, {1, 2, 41.7, 36.7},
                {2, 0, 0.0, 0.0}, {2, 1, 133.3, 128.3}, {2, 2, 66.6, 61.6},
                {3, 1, 25.05, 20.05}, {0, 0, 99.9, 88.8},
        };
        sb.append("  \"combinationCases\": [\n");
        for (int i = 0; i < combos.length; i++) {
            double[] x = combos[i];
            double fee = FeeCalculate.calculateOther((int) x[0], (int) x[1], x[2], x[3]);
            double feeWithAbsent = FeeCalculate.calculateOtherWithAbsent(
                    (int) x[0], x[2], (int) x[1], x[3], 2, 30.0);
            sb.append("    {\"male\": ").append((int) x[0])
              .append(", \"female\": ").append((int) x[1])
              .append(", \"maleFee\": ").append(x[2])
              .append(", \"femaleFee\": ").append(x[3])
              .append(", \"fee\": ").append(q(fee))
              .append(", \"feeWithAbsent\": ").append(q(feeWithAbsent))
              .append("}").append(i == combos.length - 1 ? "\n" : ",\n");
        }
        sb.append("  ],\n");

        // ---------------- 羽毛球按桶 ----------------
        String[] prices = {"", "0", "80", "85.5", "100", "120.5", "199.99", "-40"};
        sb.append("  \"shuttlecockCases\": [\n");
        boolean first = true;
        for (String price : prices) {
            for (int used = 0; used <= 36; used++) {
                double p = price.isEmpty() ? 0.0 : Double.parseDouble(price);
                double lineFee = shuttlecockLineFee(p, used);
                if (!first) sb.append(",\n");
                first = false;
                sb.append("    {\"price\": \"").append(price).append("\", \"used\": ").append(used)
                  .append(", \"fee\": ").append(q(lineFee))
                  .append(", \"formatted\": \"").append(formatShuttlecockFee(lineFee)).append("\"}");
            }
        }
        sb.append("\n  ],\n");

        // ---------------- 多行汇总 ----------------
        sb.append("  \"totals\": [\n");
        String[][] lineSets = {
                {"80:6", "100:12"},
                {"80:6"},
                {":12"},
                {"0:12"},
                {"85.5:7", "120.5:3", "199.99:36"},
                {"100:1"},
                {"100:11", "100:12"},
        };
        for (int i = 0; i < lineSets.length; i++) {
            double total = 0;
            StringBuilder rows = new StringBuilder();
            for (int j = 0; j < lineSets[i].length; j++) {
                String[] parts = lineSets[i][j].split(":");
                double p = parts[0].isEmpty() ? 0.0 : Double.parseDouble(parts[0]);
                int used = Integer.parseInt(parts[1]);
                total += shuttlecockLineFee(p, used);
                if (j > 0) rows.append(", ");
                rows.append("{\"tubePrice\": \"").append(parts[0]).append("\", \"usedCount\": ").append(used).append("}");
            }
            sb.append("    {\"lines\": [").append(rows).append("], \"total\": ").append(q(total))
              .append(", \"formatted\": \"").append(formatShuttlecockFee(total)).append("\"}")
              .append(i == lineSets.length - 1 ? "\n" : ",\n");
        }
        sb.append("  ]\n}\n");

        System.out.print(sb);
    }
}
