package com.rms.distribution;

import java.math.BigDecimal;
import java.util.List;

class ShareMathTest {
    static int fails = 0;
    static void check(boolean c, String m) { System.out.println((c ? "ok   " : "FAIL ") + m); if (!c) fails++; }
    static BigDecimal b(String s) { return new BigDecimal(s); }
    static BigDecimal sum(List<BigDecimal> l) { return l.stream().reduce(BigDecimal.ZERO, BigDecimal::add); }
    @org.junit.jupiter.api.Test
    void splitsAddUpToTheLayerTotal() {
        var r = ShareMath.split(b("20000000"), List.of(b("40"), b("35"), b("25")));
        check(r.equals(List.of(b("8000000.00"), b("7000000.00"), b("5000000.00"))), "clean split of a surplus layer");
        var odd = ShareMath.split(b("100.00"), List.of(b("33.333333"), b("33.333333"), b("33.333334")));
        check(sum(odd).compareTo(b("100.00")) == 0, "rounding remainder keeps the parts adding up to the total");
        var neg = ShareMath.split(b("-4000000"), List.of(b("40"), b("35"), b("25")));
        check(neg.get(0).compareTo(b("-1600000.00")) == 0 && sum(neg).compareTo(b("-4000000.00")) == 0, "negative outstanding movements split correctly");
        var odd2 = ShareMath.split(b("10082.19"), List.of(b("50"), b("50")));
        check(sum(odd2).compareTo(b("10082.19")) == 0, "odd cent goes to one reinsurer, total preserved (" + odd2 + ")");
        var motor = ShareMath.split(b("27000"), List.of(b("50"), b("25"), b("25")));
        check(motor.equals(List.of(b("13500.00"), b("6750.00"), b("6750.00"))), "motor claim re-split 50/25/25 = 13,500 / 6,750 / 6,750");
        System.out.println(fails == 0 ? "ALL PASSED" : fails + " FAILED");
        org.junit.jupiter.api.Assertions.assertEquals(0, fails);
    }
}
