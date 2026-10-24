package com.rms.allocation;

import com.rms.allocation.AllocationEngine.EngineStep;
import com.rms.allocation.AllocationEngine.Outcome;
import com.rms.allocation.AllocationEngine.Params;
import com.rms.common.ApiException;
import com.rms.domain.Treaty;
import com.rms.domain.TreatyCoverAllocationRule;
import com.rms.domain.TreatyCoverMap;
import com.rms.domain.TreatyXolLayer;
import com.rms.master.CoverHierarchyService;
import com.rms.master.LookupCodes;
import com.rms.master.LookupService;
import com.rms.master.MasterSnapshotService;
import com.rms.repository.TreatyCoverMapRepository;
import com.rms.repository.TreatyRepository;
import com.rms.domain.TreatySurplusLayer;
import com.rms.domain.TreatySurplusRetention;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Builds the execution plan (ordered chain of treaty steps) of a cover from treaty_cover_map + treaty_cover_allocation_rule and runs it. */
@Service
public class AllocationPlanService {
    public record SimulationOutput(Long coverId, LocalDate date, List<PlanStep> plan, Outcome outcome) { }

    private final TreatyCoverMapRepository maps;
    private final TreatyRepository treaties;
    private final CoverHierarchyService hierarchy;
    private final LookupService lookups;
    private final MasterSnapshotService masters;

    public AllocationPlanService(TreatyCoverMapRepository maps, TreatyRepository treaties, CoverHierarchyService hierarchy, LookupService lookups, MasterSnapshotService masters) {
        this.maps = maps;
        this.treaties = treaties;
        this.hierarchy = hierarchy;
        this.lookups = lookups;
        this.masters = masters;
    }

    // ------------------------------------------------------------------ plan from stored treaties
    @Transactional(readOnly = true)
    public List<PlanStep> plan(Long coverId, LocalDate date) {
        return steps(coverId, date, null, false).stream().map(Held::step).toList();
    }

    /** Steps of other ACTIVE / PENDING treaties on a cover (used to validate a draft treaty's place in the chain). */
    @Transactional(readOnly = true)
    public List<PlanStep> otherTreatySteps(Long coverId, LocalDate date, String excludeTreatyCode) {
        return steps(coverId, date, excludeTreatyCode, true).stream().map(Held::step).toList();
    }

    private record Held(PlanStep step, Treaty treaty, TreatyCoverMap map) { }

    private static String key(TreatyCoverMap m) { return m.getTreatyId() + "/" + m.getTreatyTypeId(); }

    private List<Held> steps(Long coverId, LocalDate date, String excludeCode, boolean includePending) {
        List<Long> statuses = new ArrayList<>();
        statuses.add(lookups.snap(LookupCodes.TREATY_STATUS, LookupCodes.ACTIVE).id());
        if (includePending) statuses.add(lookups.snap(LookupCodes.TREATY_STATUS, LookupCodes.PENDING_APPROVAL).id());
        List<Held> out = new ArrayList<>();
        Map<Long, Treaty> cache = new HashMap<>();
        // per treaty AND treaty type, the most specific mapping row that reaches this cover (cover beats section beats product beats LOB)
        Map<String, Integer> bestRank = new HashMap<>();
        List<TreatyCoverMap> reaching = new ArrayList<>();
        for (TreatyCoverMap m : maps.findInForce(statuses, date)) {
            if (!hierarchy.expand(m.getLobId(), m.getProductId(), m.getSectionId(), m.getCoverId()).contains(coverId)) continue;
            reaching.add(m);
            bestRank.merge(key(m), PlanStep.rank(m.getProductId(), m.getSectionId(), m.getCoverId()), Math::max);
        }
        for (TreatyCoverMap m : reaching) {
            int rank = PlanStep.rank(m.getProductId(), m.getSectionId(), m.getCoverId());
            if (rank < bestRank.get(key(m))) continue;     // a more specific row of the same treaty and type overrides this one here
            Treaty t = cache.computeIfAbsent(m.getTreatyId(), id -> treaties.findById(id).orElse(null));
            if (t == null || (excludeCode != null && excludeCode.equals(t.getTreatyCode()))) continue;
            String strategy = m.getTreatyTypeId() == null ? null : masters.strategyOf(m.getTreatyTypeId());
            for (TreatyCoverAllocationRule r : m.getRules()) {
                if (r.getEffectiveFrom() != null && date.isBefore(r.getEffectiveFrom()) || r.getEffectiveTo() != null && date.isAfter(r.getEffectiveTo())) continue;
                out.add(new Held(new PlanStep(m.getSequenceNo(), r.getRuleNo(), t.getId(), t.getTreatyCode(), m.getTreatyTypeDesc(), strategy, r.getAllocationMethodCode(),
                    r.getAllocationMethodDesc(), r.getAllocationBasisCode(), r.getAllocationBasisDesc(), r.getResidualActionCode(), r.getResidualActionDesc(), r.getAllocationPct(),
                    r.getMinAllocationAmt(), r.getMaxAllocationAmt(), Boolean.TRUE.equals(r.getStopOnLimitFlag()), r.getConditionRule(), m.getMapLevelDesc(), m.getId(), rank, m.getTreatyTypeId()), t, m));
            }
        }
        out.sort(Comparator.comparingInt((Held h) -> h.step().sequenceNo()).thenComparingInt(h -> h.step().ruleNo()));
        return out;
    }

    // ------------------------------------------------------------------ run
    @Transactional(readOnly = true)
    public SimulationOutput simulate(Long coverId, LocalDate date, BigDecimal sumInsured, BigDecimal premium, Long riskGradeId) {
        if (coverId == null || date == null || sumInsured == null) throw ApiException.badRequest("SIM_INPUT", "coverId, date and sumInsured are required");
        List<Held> held = steps(coverId, date, null, false);
        List<EngineStep> es = new ArrayList<>();
        for (Held h : held) es.add(new EngineStep(h.step(), params(h.treaty(), h.map(), h.step().methodCode(), riskGradeId)));
        Outcome o = AllocationEngine.run(es, sumInsured, premium);
        return new SimulationOutput(coverId, date, held.stream().map(Held::step).toList(), o);
    }

    /** Numbers for one step, all from the mapping row's own terms (each row carries its treaty type and terms). Sliding-scale quota share earns the provisional rate. */
    private Params params(Treaty t, TreatyCoverMap m, String method, Long riskGradeId) {
        BigDecimal qsPct = null, qsLimit = null, retention = null, capacity = null, xolLimit = null, xolDed = null, comm = null;
        List<AllocationEngine.Layer> layers = List.of();
        if ("QS".equals(method)) {
            var q = m.getQsParams().stream().findFirst().orElse(null);
            if (q != null) {
                qsPct = q.getCessionPct();
                qsLimit = q.getPerRiskLimit();
                comm = q.getFlatCommissionPct() != null ? q.getFlatCommissionPct() : t.getScScales().stream().findFirst().map(sc -> sc.getProvisionalPct()).orElse(null);
            }
        } else if ("SURPLUS".equals(method)) {
            retention = m.getRetentions().stream().filter(r -> riskGradeId != null && riskGradeId.equals(r.getRiskGradeId())).map(TreatySurplusRetention::getRetentionAmt).findFirst()
                .orElseGet(() -> m.getRetentions().stream().map(TreatySurplusRetention::getRetentionAmt).max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO));
            final BigDecimal ret = retention;
            // capacity = retention used for this risk x lines of each layer
            layers = m.getSurplusLayers().stream().sorted(Comparator.comparing(TreatySurplusLayer::getLayerNo))
                .map(l -> new AllocationEngine.Layer(l.getLayerName(), ret.multiply(BigDecimal.valueOf(l.getNoOfLines() == null ? 0 : l.getNoOfLines())), l.getCommissionPct())).toList();
            capacity = layers.stream().map(AllocationEngine.Layer::capacity).reduce(BigDecimal.ZERO, BigDecimal::add);
        } else if ("XOL".equals(method)) {
            xolLimit = m.getXolLayers().stream().map(l -> l.getLimitAmt() == null ? BigDecimal.ZERO : l.getLimitAmt()).reduce(BigDecimal.ZERO, BigDecimal::add);
            xolDed = m.getXolLayers().stream().min(Comparator.comparing(TreatyXolLayer::getLayerNo)).map(TreatyXolLayer::getDeductibleAmt).orElse(BigDecimal.ZERO);
        }
        return new Params(qsPct, qsLimit, retention, capacity, xolLimit, xolDed, comm, layers, rowLabel(m));
    }

    /** e.g. "Cover row: Earthquake" - the mapping row whose type and terms a step uses. */
    static String rowLabel(TreatyCoverMap m) {
        String what = m.getCoverDesc() != null ? m.getCoverDesc() : m.getSectionDesc() != null ? m.getSectionDesc() : m.getProductDesc() != null ? m.getProductDesc() : m.getLobDesc();
        return m.getMapLevelDesc() + " row: " + what;
    }

    // ------------------------------------------------------------------ chain rules (used by the treaty validator)
    /**
     * Problems in one cover's ordered chain. Each entry starts with "E:" (error) or "W:" (warning).
     * Dependencies: a NET_AFTER_x basis needs an EARLIER step of method x; RESIDUAL needs an earlier step and a FAC method; LOSS_AMOUNT is for XOL / STOP_LOSS.
     */
    public static List<String> chainProblems(List<PlanStep> sorted) {
        List<String> out = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        boolean hasSurplus = false, facInChain = false, facAction = false;
        for (PlanStep s : sorted) {
            if ("SURPLUS".equals(s.methodCode())) hasSurplus = true;
            if ("FAC".equals(s.methodCode())) facInChain = true;
            if ("FACULTATIVE".equals(s.residualActionCode())) facAction = true;
        }
        for (PlanStep s : sorted) {
            Set<String> before = new LinkedHashSet<>();
            for (PlanStep e : sorted) if (e.sequenceNo() < s.sequenceNo()) before.add(e.methodCode());
            String b = s.basisCode(), who = s.treatyCode() + " (seq " + s.sequenceNo() + ")";
            switch (b) {
                case "NET_AFTER_QS" -> { if (!before.contains("QS")) out.add("E:" + who + " uses basis NET_AFTER_QS but no quota share step runs earlier"); }
                case "NET_AFTER_SURPLUS" -> { if (!before.contains("SURPLUS")) out.add("E:" + who + " uses basis NET_AFTER_SURPLUS but no surplus step runs earlier"); }
                case "NET_AFTER_QS_SURPLUS" -> { if (!before.contains("QS") || !before.contains("SURPLUS")) out.add("E:" + who + " uses basis NET_AFTER_QS_SURPLUS but quota share and surplus do not both run earlier"); }
                case "NET_AFTER_XOL" -> { if (!before.contains("XOL")) out.add("E:" + who + " uses basis NET_AFTER_XOL but no XOL step runs earlier"); }
                case "NET_RETAINED" -> { if (before.isEmpty()) out.add("W:" + who + " uses basis NET_RETAINED but is the first step (same as GROSS)"); }
                case "RESIDUAL" -> {
                    if (!"FAC".equals(s.methodCode())) out.add("E:" + who + " uses basis RESIDUAL, which only a FAC step may use");
                    if (before.isEmpty()) out.add("E:" + who + " uses basis RESIDUAL but nothing runs earlier, so there is no residual");
                }
                case "LOSS_AMOUNT" -> { if (!"XOL".equals(s.methodCode()) && !"STOP_LOSS".equals(s.methodCode())) out.add("E:" + who + " uses basis LOSS_AMOUNT, which only XOL / STOP_LOSS steps may use"); }
                case "GROSS" -> { if (!before.isEmpty() && ("QS".equals(s.methodCode()) || "SURPLUS".equals(s.methodCode())))
                    out.add("W:" + who + " is applied to GROSS although earlier steps already allocated exposure (double allocation risk)"); }
                default -> { }
            }
        }
        if (hasSurplus && !facInChain && !facAction) out.add("W:Surplus capacity can be exceeded but nothing in the chain places the overflow (add a FAC / RESIDUAL step or a FACULTATIVE residual action)");
        out.forEach(seen::add);
        return new ArrayList<>(seen);
    }
}
