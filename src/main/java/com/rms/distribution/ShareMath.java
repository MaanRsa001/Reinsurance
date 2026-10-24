package com.rms.distribution;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

final class ShareMath {
    private ShareMath() { }

    /** Splits total by percentages to 2 decimals; the rounding remainder goes to the largest share so parts always add up to the total. */
    static List<BigDecimal> split(BigDecimal total, List<BigDecimal> pcts) {
        BigDecimal t = total == null ? BigDecimal.ZERO : total.setScale(2, RoundingMode.HALF_UP);
        List<BigDecimal> out = new ArrayList<>();
        BigDecimal sum = BigDecimal.ZERO;
        int largest = 0;
        for (int i = 0; i < pcts.size(); i++) {
            BigDecimal part = t.multiply(pcts.get(i)).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            out.add(part);
            sum = sum.add(part);
            if (pcts.get(i).compareTo(pcts.get(largest)) > 0) largest = i;
        }
        if (!out.isEmpty()) out.set(largest, out.get(largest).add(t.subtract(sum)));
        return out;
    }
}
