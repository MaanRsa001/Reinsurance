package com.rms;

import com.rms.allocation.AllocationEngine;
import com.rms.allocation.AllocationEngine.*;
import com.rms.allocation.AllocationPlanService;
import com.rms.allocation.PlanStep;
import java.math.BigDecimal;
import java.util.List;

/** Pure checks of the deterministic execution chain (no repositories needed). */
public class AllocationChainScenarioTest {
    static int fails = 0;
    static void check(boolean c, String m) { System.out.println((c ? "ok   " : "FAIL ") + m); if (!c) fails++; }
    static BigDecimal n(long v) { return BigDecimal.valueOf(v); }
    static boolean eq(BigDecimal a, long b) { return a.compareTo(n(b)) == 0; }

    static PlanStep step(int seq, String treaty, String method, String basis, String residual, boolean stop) {
        return new PlanStep(seq, 1, (long) seq, treaty, null, null, method, method, basis, basis, residual, residual, null, null, null, stop, null, "Product");
    }
    static EngineStep qs(int seq, String basis, long pct, Long limit) { return new EngineStep(step(seq, "QS-T", "QS", basis, "CONTINUE", true), new Params(n(pct), limit == null ? null : n(limit), null, null, null, null)); }
    static EngineStep sur(int seq, String basis, String residual, boolean stop, long ret, long cap) { return new EngineStep(step(seq, "SUR-T", "SURPLUS", basis, residual, stop), new Params(null, null, n(ret), n(cap), null, null)); }
    static EngineStep xol(int seq, String basis, long limit) { return new EngineStep(step(seq, "XOL-T", "XOL", basis, "CONTINUE", true), new Params(null, null, null, null, n(limit), n(500000))); }
    static EngineStep fac(int seq) { return new EngineStep(step(seq, "FAC-T", "FAC", "RESIDUAL", "FACULTATIVE", true), new Params(null, null, null, null, null, null)); }

    public static void run() {
        // Fire chain as in the sample data: surplus (gross) -> cat XOL (net after surplus) -> FAC residual. SI 70m, retention 5m, 10 lines.
        Outcome o = AllocationEngine.run(List.of(sur(1, "GROSS", "CONTINUE", true, 5_000_000, 50_000_000), xol(2, "NET_AFTER_SURPLUS", 30_000_000), fac(3)), n(70_000_000), n(140_000));
        check(eq(o.retained(), 5_000_000) && eq(o.totalCeded(), 65_000_000) && eq(o.facPlaced(), 15_000_000) && eq(o.facRequired(), 0) && eq(o.unallocated(), 0), "SI 70m: retained 5m, surplus 50m, overflow 15m placed by the FAC step");
        check(eq(o.steps().get(0).allocated(), 50_000_000) && eq(o.steps().get(0).overflow(), 15_000_000) && eq(o.steps().get(1).baseAmount(), 5_000_000) && eq(o.steps().get(1).allocated(), 0), "XOL sees the 5m net retained and does not consume exposure");
        check(o.steps().get(2).allocatedPremium().compareTo(new BigDecimal("30000.00")) == 0, "FAC step carries 15/70 of the 140,000 premium = 30,000");

        Outcome inside = AllocationEngine.run(List.of(sur(1, "GROSS", "CONTINUE", true, 5_000_000, 50_000_000), fac(3)), n(40_000_000), n(100_000));
        check(eq(inside.retained(), 5_000_000) && eq(inside.facPlaced(), 0) && eq(inside.steps().get(1).allocated(), 0), "SI 40m fits the surplus: nothing reaches FAC");

        // Residual action matters when no FAC step exists
        Outcome noFacCont = AllocationEngine.run(List.of(sur(1, "GROSS", "CONTINUE", true, 5_000_000, 50_000_000)), n(70_000_000), n(0));
        check(eq(noFacCont.unallocated(), 15_000_000) && eq(noFacCont.facRequired(), 0), "CONTINUE with no later step handling it leaves the overflow unallocated");
        Outcome noFacFlag = AllocationEngine.run(List.of(sur(1, "GROSS", "FACULTATIVE", true, 5_000_000, 50_000_000)), n(70_000_000), n(0));
        check(eq(noFacFlag.facRequired(), 15_000_000) && eq(noFacFlag.unallocated(), 0), "FACULTATIVE flags the overflow as fac required");
        Outcome retain = AllocationEngine.run(List.of(sur(1, "GROSS", "RETAIN", true, 5_000_000, 50_000_000)), n(70_000_000), n(0));
        check(eq(retain.retained(), 20_000_000) && eq(retain.unallocated(), 0) && eq(retain.facRequired(), 0), "RETAIN keeps the overflow net (5m + 15m)");
        Outcome err = AllocationEngine.run(List.of(sur(1, "GROSS", "ERROR", true, 5_000_000, 50_000_000), fac(2)), n(70_000_000), n(0));
        check(err.rejected() && err.rejectReason().contains("Capacity of SUR-T exceeded") && err.steps().size() == 1, "ERROR rejects the transaction and stops the chain");
        Outcome noStop = AllocationEngine.run(List.of(sur(1, "GROSS", "ERROR", false, 5_000_000, 50_000_000), fac(2)), n(70_000_000), n(0));
        check(!noStop.rejected() && eq(noStop.facPlaced(), 15_000_000), "stop_on_limit = N: overflow always continues, whatever the residual action says");

        // Four-step chain: QS 30% -> surplus on net after QS -> XOL -> FAC. SI 100m; surplus retention 10m, 4 lines.
        Outcome four = AllocationEngine.run(List.of(qs(1, "GROSS", 30, null), sur(2, "NET_AFTER_QS", "CONTINUE", true, 10_000_000, 40_000_000), xol(3, "NET_AFTER_QS_SURPLUS", 20_000_000), fac(4)), n(100_000_000), n(200_000));
        check(eq(four.steps().get(0).allocated(), 30_000_000) && eq(four.steps().get(1).baseAmount(), 70_000_000) && eq(four.steps().get(1).allocated(), 40_000_000), "QS takes 30m of 100m; surplus works on the 70m net after QS and takes 40m");
        check(eq(four.steps().get(1).overflow(), 20_000_000) && eq(four.facPlaced(), 20_000_000) && eq(four.retained(), 10_000_000) && eq(four.totalCeded(), 90_000_000), "20m overflow goes to FAC, 10m retained, 90m ceded in total");
        check(eq(four.steps().get(2).baseAmount(), 10_000_000), "XOL on NET_AFTER_QS_SURPLUS protects exactly the 10m retention");

        Outcome qsLimit = AllocationEngine.run(List.of(qs(1, "GROSS", 30, 10_000_000L)), n(40_000_000), n(0));
        check(eq(qsLimit.steps().get(0).allocated(), 3_000_000) && qsLimit.steps().get(0).notes().stream().anyMatch(s -> s.contains("per-risk limit")), "QS cession % applies only up to the per-risk limit");

        // per-layer commission: retention 5m, layer 1 = 20m at 25%, layer 2 = 30m at 22.5%; SI 40m, premium 100,000 -> 20m + 15m ceded
        EngineStep layered = new EngineStep(step(1, "SUR-T", "SURPLUS", "GROSS", "CONTINUE", true), new Params(null, null, n(5_000_000), null, null, null, null,
            List.of(new Layer("1st Surplus", n(20_000_000), n(25)), new Layer("2nd Surplus", n(30_000_000), new BigDecimal("22.5"))), "Section row: Property damage"));
        Outcome lay = AllocationEngine.run(List.of(layered), n(40_000_000), n(100_000));
        check(eq(lay.totalCeded(), 35_000_000) && lay.steps().get(0).commission().compareTo(new BigDecimal("20937.50")) == 0 && lay.totalCommission().compareTo(new BigDecimal("20937.50")) == 0,
            "surplus commission per layer: 50,000 x 25% + 37,500 x 22.5% = 20,937.50");
        check(lay.steps().get(0).notes().stream().anyMatch(x -> x.contains("from Section row: Property damage")), "a step names the mapping row that supplied its terms");
        Outcome qsComm = AllocationEngine.run(List.of(new EngineStep(step(1, "QS-T", "QS", "GROSS", "CONTINUE", true), new Params(n(20), null, null, null, null, null, new BigDecimal("22.5"), List.of(), "Cover row: Fire"))), n(10_000_000), n(100_000));
        check(eq(qsComm.totalCeded(), 2_000_000) && qsComm.totalCommission().compareTo(new BigDecimal("4500.00")) == 0, "QS 20% / 22.5%: 2m ceded, commission 4,500");

        // chain rules
        List<String> bad = AllocationPlanService.chainProblems(List.of(step(1, "A", "SURPLUS", "GROSS", "CONTINUE", true), step(2, "B", "XOL", "NET_AFTER_QS", "CONTINUE", true)));
        check(bad.stream().anyMatch(s -> s.startsWith("E:") && s.contains("NET_AFTER_QS but no quota share")), "NET_AFTER_QS without an earlier QS is an error");
        List<String> good = AllocationPlanService.chainProblems(List.of(step(1, "Q", "QS", "GROSS", "CONTINUE", true), step(2, "S", "SURPLUS", "NET_AFTER_QS", "CONTINUE", true),
            step(3, "X", "XOL", "NET_AFTER_QS_SURPLUS", "CONTINUE", true), step(4, "F", "FAC", "RESIDUAL", "FACULTATIVE", true)));
        check(good.isEmpty(), "the full QS -> surplus -> XOL -> FAC chain has no problems");
        check(AllocationPlanService.chainProblems(List.of(step(1, "S", "SURPLUS", "GROSS", "CONTINUE", true))).stream().anyMatch(s -> s.startsWith("W:") && s.contains("nothing in the chain places the overflow")), "surplus without any FAC / FACULTATIVE handling warns");
        check(AllocationPlanService.chainProblems(List.of(step(1, "S", "SURPLUS", "RESIDUAL", "CONTINUE", true))).stream().anyMatch(s -> s.contains("only a FAC step may use")), "RESIDUAL is reserved for FAC steps");
        check(AllocationPlanService.chainProblems(List.of(step(1, "F", "FAC", "RESIDUAL", "FACULTATIVE", true))).stream().anyMatch(s -> s.contains("nothing runs earlier")), "RESIDUAL as the first step has nothing to place");
        check(AllocationPlanService.chainProblems(List.of(step(1, "S", "SURPLUS", "GROSS", "FACULTATIVE", true), step(2, "X", "XOL", "LOSS_AMOUNT", "CONTINUE", true))).isEmpty(), "XOL may use LOSS_AMOUNT; FACULTATIVE residual action satisfies the overflow rule");

        System.out.println(fails == 0 ? "ALL PASSED" : fails + " FAILED");
        if (fails > 0) throw new AssertionError(fails + " check(s) failed");
    }
}
