package com.rms.allocation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Deterministic execution of a cover's chain: steps run strictly in (sequence, rule) order.
 *
 * Per step the engine splits the basis amount into  allocated (ceded)  /  kept net  /  overflow.
 *  - QS: pct of the basis (treaty cession %, optionally capped by the rule pct and the per-risk limit)
 *  - SURPLUS: basis above the retention up to total line capacity; what exceeds the capacity is OVERFLOW
 *  - XOL / STOP_LOSS: protect the net retained account; they do not consume exposure (losses attach later)
 *  - FAC: places the pool of overflow (basis RESIDUAL)
 * The rule's residual action decides what happens to overflow: CONTINUE (pool for later steps), FACULTATIVE (pool and flag),
 * RETAIN (keep net), ERROR (reject the transaction) or UNALLOCATED (leave flagged). With stop_on_limit = false the overflow always continues.
 */
public final class AllocationEngine {
    private AllocationEngine() { }

    /** One surplus layer as the engine sees it: capacity (retention x lines) and the commission % that applies to it on this cover. */
    public record Layer(String name, BigDecimal capacity, BigDecimal commissionPct) { }

    /**
     * Numbers of one step, taken from the mapping row that applies to the cover (each row carries its treaty type and terms).
     * commissionPct = QS commission; surplusLayers = per-layer capacity + commission (surplusCapacity is their total when layers are given);
     * termsSource = which mapping row supplied them (shown in the notes).
     */
    public record Params(BigDecimal qsPct, BigDecimal qsLimit, BigDecimal retention, BigDecimal surplusCapacity, BigDecimal xolLimit, BigDecimal xolDeductible,
                         BigDecimal commissionPct, List<Layer> surplusLayers, String termsSource) {
        public Params(BigDecimal qsPct, BigDecimal qsLimit, BigDecimal retention, BigDecimal surplusCapacity, BigDecimal xolLimit, BigDecimal xolDeductible) {
            this(qsPct, qsLimit, retention, surplusCapacity, xolLimit, xolDeductible, null, List.of(), null);
        }
    }

    public record EngineStep(PlanStep plan, Params params) { }

    public record StepResult(int sequenceNo, int ruleNo, String treatyCode, String method, String basis, BigDecimal baseAmount, BigDecimal allocated,
                             BigDecimal allocatedPremium, BigDecimal overflow, String overflowAction, BigDecimal poolAfter, BigDecimal netAfter, List<String> notes,
                             BigDecimal commissionPct, BigDecimal commission, String termsSource) { }

    public record Outcome(List<StepResult> steps, BigDecimal retained, BigDecimal totalCeded, BigDecimal facPlaced, BigDecimal facRequired, BigDecimal unallocated,
                          boolean rejected, String rejectReason, List<String> notes, BigDecimal totalCommission) { }

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static BigDecimal z(BigDecimal b) { return b == null ? BigDecimal.ZERO : b; }
    private static BigDecimal s2(BigDecimal b) { return b.setScale(2, RoundingMode.HALF_UP); }

    public static Outcome run(List<EngineStep> steps, BigDecimal sumInsured, BigDecimal premium) {
        BigDecimal si = z(sumInsured);
        BigDecimal prem = z(premium);
        Map<String, BigDecimal> ceded = new HashMap<>();
        BigDecimal pool = BigDecimal.ZERO, unallocated = BigDecimal.ZERO, facRequired = BigDecimal.ZERO;
        boolean facFlag = false, rejected = false;
        String rejectReason = null;
        List<StepResult> results = new ArrayList<>();
        List<String> notes = new ArrayList<>();

        for (EngineStep es : steps) {
            PlanStep p = es.plan();
            Params q = es.params();
            List<String> n = new ArrayList<>();
            BigDecimal outstanding = pool.add(unallocated).add(facRequired);
            BigDecimal totalCeded = ceded.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal net = si.subtract(totalCeded).subtract(outstanding);
            BigDecimal qs = ceded.getOrDefault("QS", BigDecimal.ZERO), sur = ceded.getOrDefault("SURPLUS", BigDecimal.ZERO);

            BigDecimal base = switch (p.basisCode()) {
                case "GROSS" -> si;
                case "NET_AFTER_QS" -> si.subtract(qs).subtract(outstanding);
                case "NET_AFTER_SURPLUS" -> si.subtract(sur).subtract(outstanding);
                case "NET_AFTER_QS_SURPLUS" -> si.subtract(qs).subtract(sur).subtract(outstanding);
                case "RESIDUAL" -> pool;
                default -> net;      // NET_AFTER_XOL, NET_RETAINED, LOSS_AMOUNT: what is still retained
            };
            base = base.max(BigDecimal.ZERO);

            BigDecimal allocated = BigDecimal.ZERO, overflow = BigDecimal.ZERO;
            BigDecimal commission = BigDecimal.ZERO, commPct = null;
            switch (p.methodCode()) {
                case "QS" -> {
                    BigDecimal capped = q.qsLimit() != null ? base.min(q.qsLimit()) : base;
                    if (q.qsLimit() != null && base.compareTo(q.qsLimit()) > 0) n.add("Basis exceeds the quota share per-risk limit; the excess stays net for later steps.");
                    BigDecimal pct = q.qsPct() != null ? q.qsPct() : z(p.allocationPct());
                    if (q.qsPct() != null && p.allocationPct() != null) pct = pct.min(p.allocationPct());
                    allocated = capped.multiply(pct).divide(HUNDRED, 4, RoundingMode.HALF_UP);
                    if (p.maxAllocationAmt() != null && allocated.compareTo(p.maxAllocationAmt()) > 0) { allocated = p.maxAllocationAmt(); n.add("Capped by the rule's maximum allocation."); }
                }
                case "SURPLUS" -> {
                    BigDecimal ret = z(q.retention());
                    BigDecimal cap = q.surplusLayers() != null && !q.surplusLayers().isEmpty()
                        ? q.surplusLayers().stream().map(l -> z(l.capacity())).reduce(BigDecimal.ZERO, BigDecimal::add) : z(q.surplusCapacity());
                    allocated = base.subtract(ret).max(BigDecimal.ZERO).min(cap);
                    overflow = base.subtract(ret).subtract(cap).max(BigDecimal.ZERO);
                    if (p.maxAllocationAmt() != null && allocated.compareTo(p.maxAllocationAmt()) > 0) {
                        overflow = overflow.add(allocated.subtract(p.maxAllocationAmt()));
                        allocated = p.maxAllocationAmt();
                        n.add("Capped by the rule's maximum allocation; the difference is overflow.");
                    }
                    n.add("Retention " + s2(ret) + ", line capacity " + s2(cap) + ".");
                }
                case "XOL" -> n.add("Protects the net retained exposure of " + s2(base) + " (layers: limit " + s2(z(q.xolLimit())) + ", first attachment " + s2(z(q.xolDeductible()))
                    + "); does not consume exposure - recoveries attach on losses.");
                case "STOP_LOSS" -> n.add("Aggregate protection of the net retained account; evaluated on loss ratio, no exposure consumed.");
                case "FAC" -> {
                    allocated = base;
                    if (p.maxAllocationAmt() != null && allocated.compareTo(p.maxAllocationAmt()) > 0) { allocated = p.maxAllocationAmt(); n.add("Capped by the rule's maximum allocation."); }
                    if ("RESIDUAL".equals(p.basisCode())) pool = pool.subtract(allocated);
                    n.add("Facultative placement of " + s2(allocated) + " (placed case by case in fac_out_placement).");
                }
                default -> n.add("Unknown method " + p.methodCode() + ": step skipped.");
            }
            if (p.minAllocationAmt() != null && allocated.signum() > 0 && allocated.compareTo(p.minAllocationAmt()) < 0) {
                n.add("Allocation " + s2(allocated) + " is below the rule's minimum " + s2(p.minAllocationAmt()) + ": nothing allocated.");
                allocated = BigDecimal.ZERO;
            }
            ceded.merge(p.methodCode(), allocated, BigDecimal::add);

            BigDecimal stepPremium = si.signum() == 0 ? BigDecimal.ZERO : prem.multiply(allocated).divide(si, 2, RoundingMode.HALF_UP);
            if ("QS".equals(p.methodCode()) && q.commissionPct() != null) {
                commPct = q.commissionPct();
                commission = stepPremium.multiply(commPct).divide(HUNDRED, 2, RoundingMode.HALF_UP);
            } else if ("SURPLUS".equals(p.methodCode()) && q.surplusLayers() != null && !q.surplusLayers().isEmpty() && si.signum() > 0) {
                // fill the layers in order; each layer's share of the premium earns that layer's commission
                BigDecimal left = allocated;
                for (Layer l : q.surplusLayers()) {
                    BigDecimal take = left.min(z(l.capacity())).max(BigDecimal.ZERO);
                    left = left.subtract(take);
                    BigDecimal lp = prem.multiply(take).divide(si, 2, RoundingMode.HALF_UP);
                    commission = commission.add(lp.multiply(z(l.commissionPct())).divide(HUNDRED, 2, RoundingMode.HALF_UP));
                    if (take.signum() > 0) n.add(l.name() + ": " + s2(take) + " at " + z(l.commissionPct()).stripTrailingZeros().toPlainString() + "% commission.");
                }
                commPct = stepPremium.signum() == 0 ? null : commission.multiply(HUNDRED).divide(stepPremium, 6, RoundingMode.HALF_UP);
            }
            if (q.termsSource() != null) n.add("Type and terms from " + q.termsSource() + ".");

            String action = "-";
            if (overflow.signum() > 0) {
                action = p.stopOnLimit() ? p.residualActionCode() : "CONTINUE";
                switch (action) {
                    case "FACULTATIVE" -> { pool = pool.add(overflow); facFlag = true; }
                    case "RETAIN" -> n.add("Overflow " + s2(overflow) + " retained by the company.");
                    case "UNALLOCATED" -> unallocated = unallocated.add(overflow);
                    case "ERROR" -> { rejected = true; rejectReason = "Capacity of " + p.treatyCode() + " exceeded by " + s2(overflow) + " and the rule says reject"; }
                    default -> pool = pool.add(overflow);          // CONTINUE
                }
            }
            BigDecimal outAfter = pool.add(unallocated).add(facRequired);
            BigDecimal cededAfter = ceded.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal netAfter = si.subtract(cededAfter).subtract(outAfter);
            if ("RETAIN".equals(action)) netAfter = si.subtract(cededAfter).subtract(outAfter);
            results.add(new StepResult(p.sequenceNo(), p.ruleNo(), p.treatyCode(), p.methodCode(), p.basisCode(), s2(base), s2(allocated), stepPremium, s2(overflow), action,
                s2(pool), s2(netAfter), n, commPct, commission, q.termsSource()));
            if (rejected) break;
        }
        if (!rejected && pool.signum() > 0) {
            if (facFlag) { facRequired = facRequired.add(pool); notes.add("Overflow of " + s2(pool) + " needs facultative placement: no FAC step in the chain absorbed it."); }
            else { unallocated = unallocated.add(pool); notes.add("Overflow of " + s2(pool) + " is left unallocated: nothing in the chain handles it."); }
            pool = BigDecimal.ZERO;
        }
        BigDecimal totalCeded = ceded.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal retained = si.subtract(totalCeded).subtract(facRequired).subtract(unallocated);
        return new Outcome(results, s2(retained), s2(totalCeded), s2(ceded.getOrDefault("FAC", BigDecimal.ZERO)), s2(facRequired), s2(unallocated), rejected, rejectReason, notes,
            results.stream().map(StepResult::commission).reduce(BigDecimal.ZERO, BigDecimal::add));
    }
}
