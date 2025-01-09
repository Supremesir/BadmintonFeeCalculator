package com.supremesir.badmintonfeecalculator;

public final class FeeCalculate {

    /**
     * 输入场地费、羽毛球费、男/女人数，分别计算男生女生球费
     * @param courtFee 场地费
     * @param shuttlecockFee 羽毛球费
     * @param moreFee 男生比女生多收的费用
     * @param maleCount 男生人数
     * @param femaleCount 女生人数
     * @return [男生球费、女生球费]
     */
    public static double[] calculate(
            double courtFee,
            double shuttlecockFee,
            double moreFee,
            int maleCount,
            int femaleCount
    ) {
        // 计算 femaleFee
        double femaleFee = (courtFee + shuttlecockFee - moreFee * maleCount) / (femaleCount + maleCount);
        // 计算 maleFee
        double maleFee = femaleFee + moreFee;

        // 分别向上取整
        double femaleFeeLast = Math.ceil(femaleFee * 10) / 10;
        double maleFeeLast = Math.ceil(maleFee * 10) / 10;

        return new double[]{maleFeeLast, femaleFeeLast};
    }

    /**
     * 输入场地费、羽毛球费、男/女人数，鸽子人数，分别计算男生女生球费
     * @param courtFee 场地费
     * @param shuttlecockFee 羽毛球费
     * @param moreFee 男生比女生多收的费用
     * @param maleCount 男生人数
     * @param femaleCount 女生人数
     * @param absentCount 没来打球的人数
     * @return [男生球费、女生球费]
     */
    public static double[] calculateWithAbsent(
            double courtFee,
            double shuttlecockFee,
            double moreFee,
            int maleCount,
            int femaleCount,
            int absentCount
    ) {
        // 总人数，包括男生、女生和没来打球的人
        int totalCount = maleCount + femaleCount + absentCount;

        // 计算每个人的场地费（场地费平均分摊）
        double perCourtFee = courtFee / totalCount;

        // 没来打球的人的费用（只需要支付场地费）
        double absentFee = perCourtFee;

        // 计算女生的总费用（场地费 + 平均羽毛球费）
        double femaleFee = perCourtFee + (shuttlecockFee - moreFee * maleCount) / (femaleCount + maleCount);

        // 计算男生的总费用（女生费用 + 多出的部分）
        double maleFee = femaleFee + moreFee;

        // 分别向上取整
        double femaleFeeLast = Math.ceil(femaleFee * 10) / 10;
        double maleFeeLast = Math.ceil(maleFee * 10) / 10;
        double absentFeeLast = Math.ceil(absentFee * 10) / 10;

        return new double[]{maleFeeLast, femaleFeeLast, absentFeeLast};
    }


    /**
     * 计算常用人数组的需收费金额
     * @return
     */
    public static double calculateOther(
            int maleCount,
            int femaleCount,
            double maleFee,
            double femaleFee
    ) {
        double fee = maleFee * maleCount + femaleFee * femaleCount;
        return Math.round(fee * 10) / 10.0;
    }
}
