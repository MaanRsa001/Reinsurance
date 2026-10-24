package com.rms.treaty;

import com.rms.allocation.AllocationPlanService;
import com.rms.allocation.PlanStep;
import com.rms.common.ValidationIssue;
import com.rms.domain.ProductSection;
import com.rms.domain.SectionCover;
import com.rms.master.CoverHierarchyService;
import com.rms.master.LookupCodes;
import com.rms.master.LookupService;
import com.rms.master.MasterSnapshotService;
import com.rms.repository.CommissionTypeRepository;
import com.rms.repository.ProductSectionRepository;
import com.rms.repository.SectionCoverRepository;
import com.rms.repository.TreatyCoverMapRepository;
import com.rms.treaty.TreatyDtos.AllocationRuleDto;
import com.rms.treaty.TreatyDtos.CoverMapDto;
import com.rms.treaty.TreatyDtos.PanelDto;
import com.rms.treaty.TreatyDtos.SurplusLayerDto;
import com.rms.treaty.TreatyDtos.TreatyRequest;
import com.rms.treaty.TreatyDtos.XolLayerDto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Server-side rules for the treaty admin console (section 7.1A). ERROR issues block submission; WARNING issues do not. */
@Component
public class TreatyValidator {
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final MasterSnapshotService masters;
    private final CommissionTypeRepository commissionTypes;
    private final ProductSectionRepository sections;
    private final SectionCoverRepository covers;
    private final TreatyCoverMapRepository coverMaps;
    private final LookupService lookups;
    private final CoverHierarchyService hierarchy;
    private final AllocationPlanService plans;

    public TreatyValidator(MasterSnapshotService masters, CommissionTypeRepository commissionTypes, ProductSectionRepository sections,
                           SectionCoverRepository covers, TreatyCoverMapRepository coverMaps, LookupService lookups, CoverHierarchyService hierarchy, AllocationPlanService plans) {
        this.masters = masters;
        this.commissionTypes = commissionTypes;
        this.sections = sections;
        this.covers = covers;
        this.coverMaps = coverMaps;
        this.lookups = lookups;
        this.hierarchy = hierarchy;
        this.plans = plans;
    }

    private static <T> List<T> nz(List<T> l) { return l == null ? List.of() : l; }
    private static BigDecimal nv(BigDecimal b) { return b == null ? BigDecimal.ZERO : b; }

    public List<ValidationIssue> validate(TreatyRequest r) {
        List<ValidationIssue> out = new ArrayList<>();
        header(r, out);
        // the treaty type and its terms are on each mapping row
        List<CoverMapDto> maps = nz(r.coverMap());
        for (int i = 0; i < maps.size(); i++) rowTerms(r, maps.get(i), "coverMap[" + i + "]", out);
        panel(r, out);
        profitCommission(r, out);
        coverMap(r, out);
        return out;
    }

    private void header(TreatyRequest r, List<ValidationIssue> out) {
        if (blank(r.treatyCode())) out.add(ValidationIssue.error("treatyCode", "Treaty code is required"));
        if (blank(r.treatyName())) out.add(ValidationIssue.error("treatyName", "Treaty name is required"));
        if (r.treatyBasisId() == null) out.add(ValidationIssue.error("treatyBasisId", "Treaty year basis is required"));
        if (r.cessionBasisId() == null) out.add(ValidationIssue.error("cessionBasisId", "Cession basis is required"));
        if (r.currencyId() == null) out.add(ValidationIssue.error("currencyId", "Currency is required"));
        if (r.lobId() == null) out.add(ValidationIssue.error("lobId", "Line of business is required"));
        if (r.brokerId() == null) out.add(ValidationIssue.error("brokerId", "Broker is required"));
        if (r.inceptionDate() == null || r.expiryDate() == null) {
            out.add(ValidationIssue.error("inceptionDate", "Inception and expiry dates are required"));
        } else if (!r.inceptionDate().isBefore(r.expiryDate())) {
            out.add(ValidationIssue.error("expiryDate", "Expiry must be after inception"));
        }
        if (r.effectiveFrom() != null && r.inceptionDate() != null && r.expiryDate() != null
            && (r.effectiveFrom().isBefore(r.inceptionDate()) || r.effectiveFrom().isAfter(r.expiryDate()))) {
            out.add(ValidationIssue.error("effectiveFrom", "Effective date must fall inside the treaty period"));
        }
    }

    /** Terms of one mapping row, checked against the row's own treaty type. */
    private void rowTerms(TreatyRequest r, CoverMapDto m, String f, List<ValidationIssue> out) {
        if (m.treatyTypeId() == null) {
            out.add(ValidationIssue.error(f + ".treatyTypeId", "Treaty type is required on every mapping row"));
            return;
        }
        String strategy = masters.strategyOf(m.treatyTypeId());
        switch (strategy == null ? "" : strategy) {
            case LookupCodes.STRAT_QS, LookupCodes.STRAT_FAC_OBL -> quotaShare(r, m, f, out);
            case LookupCodes.STRAT_SURPLUS -> surplus(m, f, out);
            case LookupCodes.STRAT_XOL_RISK -> xol(m, f, out, false);
            case LookupCodes.STRAT_XOL_CAT -> xol(m, f, out, true);
            case LookupCodes.STRAT_STOP -> stopLoss(m, f, out);
            case LookupCodes.STRAT_FAC_RES -> { }          // standing facultative programme: no terms, placements are made case by case
            default -> out.add(ValidationIssue.error(f + ".treatyTypeId", "Treaty type has no calculation strategy configured"));
        }
    }

    private void quotaShare(TreatyRequest r, CoverMapDto m, String f, List<ValidationIssue> out) {
        var q = m.qsParams();
        if (q == null) {
            out.add(ValidationIssue.error(f + ".qsParams", "Quota share terms (cession %, commission) are required on this row"));
            return;
        }
        if (q.cessionPct() == null || q.cessionPct().signum() <= 0 || q.cessionPct().compareTo(HUNDRED) > 0)
            out.add(ValidationIssue.error(f + ".qsParams.cessionPct", "Cession % must be between 0 and 100"));
        else if (q.retentionPct() == null || q.cessionPct().add(q.retentionPct()).compareTo(HUNDRED) != 0)
            out.add(ValidationIssue.error(f + ".qsParams.retentionPct", "Cession % + retention % must equal 100"));
        if (q.commissionTypeId() == null) {
            out.add(ValidationIssue.error(f + ".qsParams.commissionTypeId", "Commission type is required"));
            return;
        }
        String code = commissionTypes.findById(q.commissionTypeId()).map(c -> c.getCode()).orElse("");
        if (LookupCodes.COMM_FLAT.equals(code) && q.flatCommissionPct() == null)
            out.add(ValidationIssue.error(f + ".qsParams.flatCommissionPct", "Flat commission % is required for flat commission"));
        if (LookupCodes.COMM_SLIDING.equals(code)) {
            var s = r.scScale();
            if (s == null) {
                out.add(ValidationIssue.error(f + ".scScale", "Sliding scale parameters are required when commission type is sliding scale"));
            } else {
                if (s.minPct() == null || s.maxPct() == null || s.provisionalPct() == null || s.lrAtMaxPct() == null || s.lrAtMinPct() == null || s.slideRatio() == null) {
                    out.add(ValidationIssue.error(f + ".scScale", "Provisional, minimum, maximum, loss-ratio bounds and slide ratio are all required"));
                } else {
                    if (s.minPct().compareTo(s.provisionalPct()) > 0 || s.provisionalPct().compareTo(s.maxPct()) > 0)
                        out.add(ValidationIssue.error(f + ".scScale.provisionalPct", "Must satisfy minimum <= provisional <= maximum commission"));
                    if (s.lrAtMaxPct().compareTo(s.lrAtMinPct()) >= 0)
                        out.add(ValidationIssue.error(f + ".scScale.lrAtMaxPct", "Loss ratio at maximum commission must be lower than loss ratio at minimum commission"));
                    if (s.slideRatio().signum() <= 0) out.add(ValidationIssue.error(f + ".scScale.slideRatio", "Slide ratio must be positive"));
                }
                if (s.lrBasisId() == null) out.add(ValidationIssue.error(f + ".scScale.lrBasisId", "Loss ratio basis is required"));
                if (s.adjustmentMonths() == null || s.adjustmentMonths() <= 0) out.add(ValidationIssue.error(f + ".scScale.adjustmentMonths", "Adjustment frequency (months) is required"));
            }
        }
    }

    private void surplus(CoverMapDto r, String f, List<ValidationIssue> out) {
        if (nz(r.retentions()).isEmpty()) out.add(ValidationIssue.error(f + ".retentions", "At least one retention row (by risk grade) is required"));
        for (var ret : nz(r.retentions())) {
            if (ret.riskGradeId() == null || ret.retentionAmt() == null || ret.retentionAmt().signum() <= 0)
                out.add(ValidationIssue.error(f + ".retentions", "Each retention needs a risk grade and a positive amount"));
        }
        List<SurplusLayerDto> layers = nz(r.surplusLayers()).stream().sorted(Comparator.comparing(l -> l.layerNo() == null ? 0 : l.layerNo())).toList();
        if (layers.isEmpty()) {
            out.add(ValidationIssue.error(f + ".surplusLayers", "At least one surplus layer is required"));
            return;
        }
        int expected = 1;
        for (SurplusLayerDto l : layers) {
            if (l.layerNo() == null || l.layerNo() != expected) out.add(ValidationIssue.error(f + ".surplusLayers", "Surplus layers must be numbered 1..n without gaps (expected " + expected + ")"));
            expected++;
            if (l.noOfLines() == null || l.noOfLines() <= 0) out.add(ValidationIssue.error(f + ".surplusLayers[" + l.layerNo() + "].noOfLines", "Number of lines must be positive"));
            if (l.retentionAmt() == null || l.retentionAmt().signum() <= 0) out.add(ValidationIssue.error(f + ".surplusLayers[" + l.layerNo() + "].retentionAmt", "Retention amount must be positive"));
            if (l.commissionTypeId() == null || l.commissionPct() == null) out.add(ValidationIssue.error(f + ".surplusLayers[" + l.layerNo() + "].commissionPct", "Commission is required for each layer"));
        }
        var first = layers.get(0);
        BigDecimal maxRet = nz(r.retentions()).stream().map(x -> nv(x.retentionAmt())).max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
        if (first.retentionAmt() != null && first.retentionAmt().compareTo(maxRet) < 0)
            out.add(ValidationIssue.warning(f + ".surplusLayers[1].retentionAmt", "Layer retention is lower than the highest retention in the retention table"));
    }

    private void xol(CoverMapDto r, String f0, List<ValidationIssue> out, boolean cat) {
        List<XolLayerDto> layers = nz(r.xolLayers()).stream().sorted(Comparator.comparing(l -> l.layerNo() == null ? 0 : l.layerNo())).toList();
        if (layers.isEmpty()) {
            out.add(ValidationIssue.error(f0 + ".xolLayers", "At least one XOL layer is required"));
            return;
        }
        int expected = 1;
        XolLayerDto prev = null;
        for (XolLayerDto l : layers) {
            String f = f0 + ".xolLayers[" + l.layerNo() + "]";
            if (l.layerNo() == null || l.layerNo() != expected) out.add(ValidationIssue.error(f0 + ".xolLayers", "XOL layers must be numbered 1..n without gaps (expected " + expected + ")"));
            expected++;
            if (l.limitAmt() == null || l.limitAmt().signum() <= 0) out.add(ValidationIssue.error(f + ".limitAmt", "Layer limit must be positive"));
            if (l.deductibleAmt() == null || l.deductibleAmt().signum() < 0) out.add(ValidationIssue.error(f + ".deductibleAmt", "Deductible must be zero or positive"));
            if (l.rateOnLinePct() == null || l.depositPremium() == null || l.minPremium() == null)
                out.add(ValidationIssue.error(f + ".depositPremium", "Rate on line, deposit premium and minimum premium are required"));
            else if (l.minPremium().compareTo(l.depositPremium()) > 0) out.add(ValidationIssue.warning(f + ".minPremium", "Minimum premium is higher than deposit premium"));
            if (cat && (l.eventHoursClause() == null || l.eventHoursClause() <= 0))
                out.add(ValidationIssue.error(f + ".eventHoursClause", "Catastrophe layers need an event hours clause"));
            for (var re : nz(l.reinstatements())) {
                if (re.premiumPct() == null || re.premiumPct().signum() < 0 || re.reinstatementBasisId() == null)
                    out.add(ValidationIssue.error(f + ".reinstatements", "Each reinstatement needs a premium % and a basis"));
            }
            if (prev != null && prev.deductibleAmt() != null && prev.limitAmt() != null && l.deductibleAmt() != null
                && l.deductibleAmt().compareTo(prev.deductibleAmt().add(prev.limitAmt())) != 0)
                out.add(ValidationIssue.warning(f + ".deductibleAmt", "Layer " + l.layerNo() + " does not attach where layer " + prev.layerNo() + " exhausts (gap or overlap)"));
            prev = l;
        }
    }

    private void stopLoss(CoverMapDto r, String f, List<ValidationIssue> out) {
        var s = r.stopLoss();
        if (s == null) {
            out.add(ValidationIssue.error(f + ".stopLoss", "Stop loss parameters are required"));
            return;
        }
        if (s.attachmentLrPct() == null || s.exitLrPct() == null || s.limitPctOfPremium() == null || s.premiumRatePct() == null) {
            out.add(ValidationIssue.error(f + ".stopLoss", "Attachment, exit, limit % and premium rate are required"));
            return;
        }
        if (s.attachmentLrPct().compareTo(s.exitLrPct()) >= 0) out.add(ValidationIssue.error(f + ".stopLoss.attachmentLrPct", "Attachment loss ratio must be lower than exit loss ratio"));
        if (s.limitPctOfPremium().compareTo(s.exitLrPct().subtract(s.attachmentLrPct())) > 0)
            out.add(ValidationIssue.warning(f + ".stopLoss.limitPctOfPremium", "Limit exceeds the width of the attachment-to-exit band"));
        if (blank(s.premiumBasisDesc())) out.add(ValidationIssue.error(f + ".stopLoss.premiumBasisDesc", "Premium basis (e.g. GNPI) is required"));
    }

    /**
     * Panel groups follow the mapping rows: one group per treaty type used (type code), split by layer for surplus / XOL (1..highest layer number used by any row
     * of that type). Every group needs a full panel (100%), except the facultative residual programme whose placements are made case by case.
     */
    static Set<String> requiredGroups(TreatyRequest r, MasterSnapshotService masters) {
        Set<String> required = new java.util.LinkedHashSet<>();
        for (CoverMapDto m : nz(r.coverMap())) {
            if (m.treatyTypeId() == null) continue;
            String strategy = masters.strategyOf(m.treatyTypeId()), code = masters.treatyType(m.treatyTypeId()).code();
            switch (strategy == null ? "" : strategy) {
                case LookupCodes.STRAT_SURPLUS -> nz(m.surplusLayers()).forEach(l -> required.add(code + ":" + l.layerNo()));
                case LookupCodes.STRAT_XOL_RISK, LookupCodes.STRAT_XOL_CAT -> nz(m.xolLayers()).forEach(l -> required.add(code + ":" + l.layerNo()));
                case LookupCodes.STRAT_FAC_RES -> { }
                default -> required.add(code + ":-");
            }
        }
        return required;
    }

    private void panel(TreatyRequest r, List<ValidationIssue> out) {
        Map<String, List<PanelDto>> groups = new LinkedHashMap<>();
        for (PanelDto p : nz(r.panel())) groups.computeIfAbsent(nvl(p.layerKind(), "?").toUpperCase() + ":" + (p.layerNo() == null ? "-" : p.layerNo()), k -> new ArrayList<>()).add(p);
        Set<String> required = requiredGroups(r, masters);
        Set<String> facGroups = new HashSet<>();
        for (CoverMapDto m : nz(r.coverMap()))
            if (m.treatyTypeId() != null && LookupCodes.STRAT_FAC_RES.equals(masters.strategyOf(m.treatyTypeId()))) facGroups.add(masters.treatyType(m.treatyTypeId()).code() + ":-");
        for (String key : required) {
            if (!groups.containsKey(key)) out.add(ValidationIssue.error("panel", "No reinsurers on the panel for " + label(key)));
        }
        for (var e : groups.entrySet()) {
            if (!required.contains(e.getKey()) && !facGroups.contains(e.getKey())) {
                out.add(ValidationIssue.error("panel", "Panel rows for " + label(e.getKey()) + " but no mapping row uses it"));
                continue;
            }
            BigDecimal total = e.getValue().stream().map(p -> nv(p.sharePct())).reduce(BigDecimal.ZERO, BigDecimal::add);
            if (total.compareTo(HUNDRED) != 0) out.add(ValidationIssue.error("panel", "Panel shares for " + label(e.getKey()) + " total " + total.stripTrailingZeros().toPlainString() + "% (must be 100%)"));
            Set<Long> seen = new HashSet<>();
            long leads = 0;
            for (PanelDto p : e.getValue()) {
                if (p.reinsurerId() == null) out.add(ValidationIssue.error("panel", "A panel row has no reinsurer"));
                else if (!seen.add(p.reinsurerId())) out.add(ValidationIssue.error("panel", "Reinsurer appears twice on " + label(e.getKey())));
                if (p.sharePct() == null || p.sharePct().signum() <= 0) out.add(ValidationIssue.error("panel", "Every panel share must be positive"));
                if (Boolean.TRUE.equals(p.leadFlag())) leads++;
            }
            if (leads > 1) out.add(ValidationIssue.error("panel", "More than one lead reinsurer on " + label(e.getKey())));
            if (leads == 0) out.add(ValidationIssue.warning("panel", "No lead reinsurer marked on " + label(e.getKey())));
        }
    }

    private static String label(String key) {
        String[] p = key.split(":");
        return "-".equals(p[1]) ? p[0] : p[0] + " layer " + p[1];
    }

    private void profitCommission(TreatyRequest r, List<ValidationIssue> out) {
        var pc = r.pcConfig();
        if (pc == null) return;
        if (pc.pcPct() == null || pc.pcPct().signum() < 0 || pc.pcPct().compareTo(HUNDRED) > 0) out.add(ValidationIssue.error("pcConfig.pcPct", "Profit commission % must be between 0 and 100"));
        if (pc.mgmtExpensePct() == null || pc.mgmtExpensePct().signum() < 0 || pc.mgmtExpensePct().compareTo(HUNDRED) > 0) out.add(ValidationIssue.error("pcConfig.mgmtExpensePct", "Management expense % must be between 0 and 100"));
        if (pc.carryForwardYears() == null || pc.carryForwardYears() < 0) out.add(ValidationIssue.error("pcConfig.carryForwardYears", "Carry-forward years must be zero or more"));
        if (Boolean.TRUE.equals(pc.dafEnabled()) && (pc.carryForwardYears() == null || pc.carryForwardYears() == 0))
            out.add(ValidationIssue.warning("pcConfig.carryForwardYears", "DAF is enabled but carry-forward years is 0, so deficits expire immediately"));
    }

    /** Methods a treaty type may use in the execution chain. */
    private static Set<String> allowedMethods(String strategy) {
        return switch (strategy) {
            case LookupCodes.STRAT_QS -> Set.of("QS");
            case LookupCodes.STRAT_FAC_OBL -> Set.of("FAC", "QS");
            case LookupCodes.STRAT_SURPLUS -> Set.of("SURPLUS");
            case LookupCodes.STRAT_XOL_RISK, LookupCodes.STRAT_XOL_CAT -> Set.of("XOL");
            case LookupCodes.STRAT_STOP -> Set.of("STOP_LOSS");
            case LookupCodes.STRAT_FAC_RES -> Set.of("FAC");
            default -> Set.of();
        };
    }

    private void coverMap(TreatyRequest r, List<ValidationIssue> out) {
        List<CoverMapDto> maps = nz(r.coverMap());
        if (maps.isEmpty()) out.add(ValidationIssue.warning("coverMap", "No LOB / product / section / cover mapping: the treaty will not be applied to any business"));
        List<Long> blockingStatuses = List.of(lookups.snap(LookupCodes.TREATY_STATUS, LookupCodes.ACTIVE).id(),
            lookups.snap(LookupCodes.TREATY_STATUS, LookupCodes.PENDING_APPROVAL).id());
        List<CoverHierarchyService.Resolved> resolved = new ArrayList<>();
        for (CoverMapDto m : maps) resolved.add(hierarchy.resolve(m.lobId(), m.productId(), m.sectionId(), m.coverId(), r.lobId()));
        List<PlanStep> draftSteps = new ArrayList<>();            // steps this treaty would add, with the covers each applies to
        List<Set<Long>> draftCovers = new ArrayList<>();
        for (int i = 0; i < maps.size(); i++) {
            CoverMapDto m = maps.get(i);
            var res = resolved.get(i);
            String f = "coverMap[" + i + "]";
            String strategy = m.treatyTypeId() == null ? "" : nvl(masters.strategyOf(m.treatyTypeId()), "");
            Set<String> allowed = allowedMethods(strategy);
            res.errors().forEach(msg -> out.add(ValidationIssue.error(f, msg)));
            if (!res.ok()) continue;
            if (res.coverIds().isEmpty()) out.add(ValidationIssue.error(f, "The selected " + res.level().toLowerCase() + " has no covers configured, so nothing would be mapped"));
            if (m.sequenceNo() == null || m.sequenceNo() <= 0) out.add(ValidationIssue.error(f + ".sequenceNo", "Sequence (execution order) must be 1 or more"));
            if (m.effectiveFrom() == null || m.effectiveTo() == null || m.effectiveFrom().isAfter(m.effectiveTo())) {
                out.add(ValidationIssue.error(f + ".effectiveFrom", "Effective from / to dates are required and from must not be after to"));
                continue;
            }
            if (r.inceptionDate() != null && r.expiryDate() != null && (m.effectiveFrom().isBefore(r.inceptionDate()) || m.effectiveTo().isAfter(r.expiryDate())))
                out.add(ValidationIssue.warning(f + ".effectiveFrom", "Mapping period extends outside the treaty period"));

            // ---- allocation rules: HOW this treaty participates at that sequence
            List<AllocationRuleDto> rules = nz(m.rules());
            if (rules.isEmpty()) out.add(ValidationIssue.error(f + ".rules", "Add at least one allocation rule (method, basis and residual action)"));
            Set<Integer> ruleNos = new HashSet<>();
            int auto = 0;
            for (AllocationRuleDto ru : rules) {
                int no = ru.ruleNo() != null ? ru.ruleNo() : ++auto;
                String rf = f + ".rules[" + no + "]";
                if (!ruleNos.add(no)) out.add(ValidationIssue.error(rf, "Rule number " + no + " is used twice on this mapping"));
                String method = code(ru.methodId(), ru.methodCode()), basis = code(ru.basisId(), ru.basisCode()), residual = code(ru.residualActionId(), ru.residualActionCode());
                if (method == null) out.add(ValidationIssue.error(rf + ".method", "Allocation method is required"));
                else if (!allowed.isEmpty() && !allowed.contains(method))
                    out.add(ValidationIssue.error(rf + ".method", "Method " + method + " cannot be used by a " + typeName(m) + " row (allowed: " + String.join(", ", allowed) + ")"));
                if (basis == null) out.add(ValidationIssue.error(rf + ".basis", "Allocation basis is required"));
                if (residual == null) out.add(ValidationIssue.error(rf + ".residualAction", "Residual action is required"));
                if (ru.allocationPct() != null && (ru.allocationPct().signum() <= 0 || ru.allocationPct().compareTo(HUNDRED) > 0))
                    out.add(ValidationIssue.error(rf + ".allocationPct", "Allocation % must be above 0 and at most 100"));
                if (ru.minAllocationAmt() != null && ru.maxAllocationAmt() != null && ru.minAllocationAmt().compareTo(ru.maxAllocationAmt()) > 0)
                    out.add(ValidationIssue.error(rf + ".maxAllocationAmt", "Minimum allocation is above the maximum"));
                if (ru.effectiveFrom() != null && ru.effectiveTo() != null && ru.effectiveFrom().isAfter(ru.effectiveTo()))
                    out.add(ValidationIssue.error(rf + ".effectiveFrom", "Rule period is back to front"));
                if (ru.effectiveFrom() != null && ru.effectiveFrom().isBefore(m.effectiveFrom()) || ru.effectiveTo() != null && ru.effectiveTo().isAfter(m.effectiveTo()))
                    out.add(ValidationIssue.warning(rf + ".effectiveFrom", "Rule period extends outside the mapping period"));
                if (method != null && basis != null && residual != null && m.sequenceNo() != null)
                    draftSteps.add(new PlanStep(m.sequenceNo(), no, null, r.treatyCode() == null ? "(this treaty)" : r.treatyCode().trim().toUpperCase(), null, strategy, method, null,
                        basis, null, residual, null, ru.allocationPct(), ru.minAllocationAmt(), ru.maxAllocationAmt(), ru.stopOnLimit() == null || ru.stopOnLimit(), ru.conditionRule(), res.level(),
                        m.id(), PlanStep.rank(res.level()), m.treatyTypeId()));
                if (method != null && basis != null && residual != null && m.sequenceNo() != null) draftCovers.add(res.coverIds());
            }


            // Inside one treaty, rows may carry different types on the same cover (QS at seq 1, surplus at seq 2 ...). Per cover and type the most specific
            // row applies (cover over section over product over LOB). Same type at the same level on one cover clashes; two types at the same sequence clash.
            for (int j = i + 1; j < maps.size(); j++) {
                CoverMapDto o = maps.get(j);
                var ores = resolved.get(j);
                if (!ores.ok() || o.effectiveFrom() == null || o.effectiveTo() == null) continue;
                if (!overlaps(m.effectiveFrom(), m.effectiveTo(), o.effectiveFrom(), o.effectiveTo())) continue;
                Set<Long> common = new HashSet<>(res.coverIds());
                common.retainAll(ores.coverIds());
                if (common.isEmpty()) continue;
                String eg = hierarchy.coverName(common.iterator().next());
                boolean sameType = m.treatyTypeId() != null && m.treatyTypeId().equals(o.treatyTypeId());
                int ri = PlanStep.rank(res.level()), rj = PlanStep.rank(ores.level());
                if (!sameType && m.sequenceNo() != null && m.sequenceNo().equals(o.sequenceNo())) {
                    out.add(ValidationIssue.error(f, "Rows " + (i + 1) + " and " + (j + 1) + " put " + typeName(m) + " and " + typeName(o)
                        + " at the same sequence " + m.sequenceNo() + " on " + common.size() + " cover(s) (e.g. " + eg + "); give each type its own sequence"));
                } else if (sameType && ri == rj) {
                    out.add(ValidationIssue.error(f, "Rows " + (i + 1) + " and " + (j + 1) + " both apply " + typeName(m) + " to " + common.size()
                        + " cover(s) (e.g. " + eg + ") at the same level; remove one row or map one of them more specifically"));
                } else if (sameType) {
                    int specific = ri > rj ? i : j, broad = ri > rj ? j : i;
                    out.add(ValidationIssue.warning(f, "Row " + (specific + 1) + " (" + levelLabel(resolved.get(specific)) + ") overrides row " + (broad + 1) + " (" + levelLabel(resolved.get(broad))
                        + ") for " + common.size() + " cover(s), e.g. " + eg + ": its own terms and sequence apply there"));
                }
            }
            if (!blank(r.treatyCode()) && m.sequenceNo() != null) {
                for (var cand : coverMaps.findCandidates(m.sequenceNo(), r.treatyCode().trim().toUpperCase(), blockingStatuses, m.effectiveFrom(), m.effectiveTo())) {
                    Set<Long> theirs = hierarchy.expand(cand.getLobId(), cand.getProductId(), cand.getSectionId(), cand.getCoverId());
                    Set<Long> common = new HashSet<>(res.coverIds());
                    common.retainAll(theirs);
                    if (!common.isEmpty()) {
                        String first = hierarchy.coverName(common.iterator().next());
                        out.add(ValidationIssue.error(f, "Sequence " + m.sequenceNo() + " already used by treaty " + cand.getTreatyCode() + " on " + common.size()
                            + " of these cover(s) (e.g. " + first + ") for overlapping dates"));
                    }
                }
            }
        }
        chain(r, resolved, draftSteps, draftCovers, out);
    }

    /** Executes the ordering rules on every cover this treaty touches: draft steps + the steps of other active / pending treaties. */
    private void chain(TreatyRequest r, List<CoverHierarchyService.Resolved> resolved, List<PlanStep> draftSteps, List<Set<Long>> draftCovers, List<ValidationIssue> out) {
        if (draftSteps.isEmpty()) return;
        Set<Long> covers = new java.util.LinkedHashSet<>();
        for (Set<Long> s : draftCovers) covers.addAll(s);
        LocalDate date = r.effectiveFrom() != null ? r.effectiveFrom() : r.inceptionDate();
        if (date == null) return;
        String code = r.treatyCode() == null ? "" : r.treatyCode().trim().toUpperCase();
        Map<String, List<Long>> problems = new LinkedHashMap<>();
        for (Long cover : covers) {
            List<PlanStep> steps = new ArrayList<>(plans.otherTreatySteps(cover, date, code));
            Map<Long, Integer> best = new java.util.HashMap<>();      // per treaty type, only the most specific of this treaty's rows reaching the cover takes part
            for (int k = 0; k < draftSteps.size(); k++) if (draftCovers.get(k).contains(cover)) best.merge(nvl(draftSteps.get(k).treatyTypeId(), -1L), draftSteps.get(k).levelRank(), Math::max);
            for (int k = 0; k < draftSteps.size(); k++)
                if (draftCovers.get(k).contains(cover) && draftSteps.get(k).levelRank() == best.get(nvl(draftSteps.get(k).treatyTypeId(), -1L))) steps.add(draftSteps.get(k));
            steps.sort(Comparator.comparingInt(PlanStep::sequenceNo).thenComparingInt(PlanStep::ruleNo));
            for (String p : AllocationPlanService.chainProblems(steps)) problems.computeIfAbsent(p, x -> new ArrayList<>()).add(cover);
        }
        problems.forEach((p, cs) -> {
            String msg = p.substring(2) + " - on " + cs.size() + " cover(s), e.g. " + hierarchy.coverName(cs.get(0));
            out.add(p.startsWith("E:") ? ValidationIssue.error("coverMap.chain", msg) : ValidationIssue.warning("coverMap.chain", msg));
        });
    }

    private static String levelLabel(CoverHierarchyService.Resolved r) { return r.level() == null ? "?" : r.level().toLowerCase() + " level"; }

    private String typeName(CoverMapDto m) {
        if (m.treatyTypeDesc() != null) return m.treatyTypeDesc();
        return m.treatyTypeId() == null ? "(no type)" : masters.treatyType(m.treatyTypeId()).desc();
    }

    private String code(Long id, String code) {
        if (code != null && !code.isBlank()) return code;
        return id == null ? null : lookups.byId(id).code();
    }

    private static boolean overlaps(LocalDate a1, LocalDate a2, LocalDate b1, LocalDate b2) { return !a1.isAfter(b2) && !b1.isAfter(a2); }
    private static boolean blank(String s) { return s == null || s.isBlank(); }
    private static <T> T nvl(T s, T d) { return s == null ? d : s; }
}
