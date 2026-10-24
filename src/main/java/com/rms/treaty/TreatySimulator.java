package com.rms.treaty;

import com.rms.master.LookupCodes;
import com.rms.master.MasterSnapshotService;
import com.rms.treaty.TreatyDtos.CoverMapDto;
import com.rms.treaty.TreatyDtos.PanelDto;
import com.rms.treaty.TreatyDtos.RetentionDto;
import com.rms.treaty.TreatyDtos.SimLine;
import com.rms.treaty.TreatyDtos.SimShare;
import com.rms.treaty.TreatyDtos.SimulationRequest;
import com.rms.treaty.TreatyDtos.SimulationResult;
import com.rms.treaty.TreatyDtos.SurplusLayerDto;
import com.rms.treaty.TreatyDtos.TreatyRequest;
import com.rms.treaty.TreatyDtos.XolLayerDto;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Dry-run of a (draft or live) treaty configuration against a sample risk or loss.
 * Mirrors the placement-engine rules (multi-line surplus fill, quota share, XOL layering, stop loss band).
 */
@Component
public class TreatySimulator {
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private final MasterSnapshotService masters;

    public TreatySimulator(MasterSnapshotService masters) {
        this.masters = masters;
    }

    private static BigDecimal z(BigDecimal b) { return b == null ? BigDecimal.ZERO : b; }
    private static BigDecimal pct(BigDecimal part, BigDecimal whole) { return whole.signum() == 0 ? BigDecimal.ZERO : part.multiply(HUNDRED).divide(whole, 6, RoundingMode.HALF_UP); }
    private static BigDecimal money(BigDecimal b) { return b.setScale(2, RoundingMode.HALF_UP); }

    /** Panel split of one amount for a panel group (row type code) and layer. */
    private List<SimShare> split(TreatyRequest t, String group, Integer layerNo, BigDecimal amount, BigDecimal premium, BigDecimal commission) {
        List<SimShare> out = new ArrayList<>();
        for (PanelDto p : t.panel() == null ? List.<PanelDto>of() : t.panel()) {
            if (p.layerKind() == null || !group.equalsIgnoreCase(p.layerKind()) || (layerNo != null && !layerNo.equals(p.layerNo()))) continue;
            BigDecimal s = z(p.sharePct());
            out.add(new SimShare(p.reinsurerDesc(), s, money(amount.multiply(s).divide(HUNDRED, 4, RoundingMode.HALF_UP)),
                money(premium.multiply(s).divide(HUNDRED, 4, RoundingMode.HALF_UP)), money(commission.multiply(s).divide(HUNDRED, 4, RoundingMode.HALF_UP))));
        }
        return out;
    }

    /** Dry-run of ONE mapping row (its treaty type and terms) against a sample risk or loss. mappingRow defaults to the first row. */
    public SimulationResult simulate(TreatyRequest t, SimulationRequest in) {
        List<String> notes = new ArrayList<>();
        List<SimLine> lines = new ArrayList<>();
        List<CoverMapDto> rows = t.coverMap() == null ? List.of() : t.coverMap();
        if (rows.isEmpty()) {
            notes.add("The treaty has no mapping row yet: add a row with a treaty type and terms to simulate it.");
            return new SimulationResult("", lines, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, false, notes);
        }
        int idx = in.mappingRow() == null || in.mappingRow() < 0 || in.mappingRow() >= rows.size() ? 0 : in.mappingRow();
        CoverMapDto row = rows.get(idx);
        if (row.treatyTypeId() == null) {
            notes.add("Mapping row " + (idx + 1) + " has no treaty type.");
            return new SimulationResult("", lines, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, false, notes);
        }
        String strategy = masters.strategyOf(row.treatyTypeId());
        String group = masters.treatyType(row.treatyTypeId()).code();
        String where = row.coverDesc() != null ? row.coverDesc() : row.sectionDesc() != null ? row.sectionDesc() : row.productDesc() != null ? row.productDesc() : "line of business";
        notes.add("Mapping row " + (idx + 1) + " (" + where + ", " + masters.treatyType(row.treatyTypeId()).desc() + ") simulated on its own terms.");
        BigDecimal si = z(in.sumInsured());
        BigDecimal premium = z(in.premium());
        BigDecimal retainedSi = si;
        BigDecimal retainedPremium = premium;
        BigDecimal excess = BigDecimal.ZERO;

        switch (strategy) {
            case LookupCodes.STRAT_SURPLUS -> {
                List<RetentionDto> rets = row.retentions() == null ? List.of() : row.retentions();
                BigDecimal retention = rets.stream()
                    .filter(r -> in.riskGradeId() != null && in.riskGradeId().equals(r.riskGradeId())).map(r -> z(r.retentionAmt())).findFirst()
                    .orElseGet(() -> rets.stream().map(r -> z(r.retentionAmt())).max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO));
                if (in.riskGradeId() == null) notes.add("No risk grade supplied: highest retention in the table was used.");
                BigDecimal remaining = si.subtract(retention).max(BigDecimal.ZERO);
                retainedSi = si.min(retention);
                BigDecimal ceded = BigDecimal.ZERO;
                List<SurplusLayerDto> layers = new ArrayList<>(row.surplusLayers() == null ? List.of() : row.surplusLayers());
                layers.sort(Comparator.comparing(SurplusLayerDto::layerNo));
                for (SurplusLayerDto l : layers) {
                    BigDecimal capacity = retention.multiply(BigDecimal.valueOf(l.noOfLines() == null ? 0 : l.noOfLines()));     // lines x retention applied
                    BigDecimal take = remaining.min(capacity);
                    if (take.signum() <= 0) continue;
                    BigDecimal prem = si.signum() == 0 ? BigDecimal.ZERO : premium.multiply(take).divide(si, 4, RoundingMode.HALF_UP);
                    BigDecimal comm = prem.multiply(z(l.commissionPct())).divide(HUNDRED, 4, RoundingMode.HALF_UP);
                    lines.add(new SimLine(nameOf(l.layerName(), "Surplus layer " + l.layerNo()), money(take), pct(take, si), money(prem), money(comm),
                        split(t, group, l.layerNo(), take, prem, comm)));
                    remaining = remaining.subtract(take);
                    ceded = ceded.add(take);
                }
                excess = remaining;
                retainedPremium = si.signum() == 0 ? premium : premium.multiply(retainedSi).divide(si, 4, RoundingMode.HALF_UP);
                if (excess.signum() > 0) notes.add("Treaty capacity exceeded: " + money(excess) + " must be placed facultatively.");
            }
            case LookupCodes.STRAT_QS, LookupCodes.STRAT_FAC_OBL -> {
                var q = row.qsParams();
                if (q == null || q.cessionPct() == null) {
                    notes.add("Quota share parameters are incomplete.");
                } else {
                    BigDecimal ceded = si.multiply(q.cessionPct()).divide(HUNDRED, 4, RoundingMode.HALF_UP);
                    BigDecimal prem = premium.multiply(q.cessionPct()).divide(HUNDRED, 4, RoundingMode.HALF_UP);
                    BigDecimal commPct = q.flatCommissionPct();
                    if (commPct == null && t.scScale() != null) {
                        commPct = t.scScale().provisionalPct();
                        notes.add("Sliding scale: provisional commission " + commPct + "% applied; adjusted later on loss ratio.");
                    }
                    BigDecimal comm = prem.multiply(z(commPct)).divide(HUNDRED, 4, RoundingMode.HALF_UP);
                    lines.add(new SimLine("Quota share " + q.cessionPct().stripTrailingZeros().toPlainString() + "%", money(ceded), q.cessionPct(), money(prem), money(comm),
                        split(t, group, null, ceded, prem, comm)));
                    retainedSi = si.subtract(ceded);
                    retainedPremium = premium.subtract(prem);
                    if (q.perRiskLimit() != null && si.compareTo(q.perRiskLimit()) > 0) {
                        excess = si.subtract(q.perRiskLimit());
                        notes.add("Sum insured exceeds the per-risk limit by " + money(excess) + ".");
                    }
                }
            }
            case LookupCodes.STRAT_XOL_RISK, LookupCodes.STRAT_XOL_CAT -> {
                BigDecimal loss = z(in.lossAmount());
                notes.add("XOL simulation: the loss amount is treated as the ultimate net loss on the net retained account.");
                List<XolLayerDto> layers = new ArrayList<>(row.xolLayers() == null ? List.of() : row.xolLayers());
                layers.sort(Comparator.comparing(XolLayerDto::layerNo));
                BigDecimal total = BigDecimal.ZERO;
                for (XolLayerDto l : layers) {
                    BigDecimal rec = loss.subtract(z(l.deductibleAmt())).max(BigDecimal.ZERO).min(z(l.limitAmt()));
                    BigDecimal rp = BigDecimal.ZERO;
                    var re = l.reinstatements() == null || l.reinstatements().isEmpty() ? null : l.reinstatements().get(0);
                    if (re != null && l.limitAmt() != null && l.limitAmt().signum() > 0) {
                        rp = z(l.depositPremium()).multiply(rec).divide(l.limitAmt(), 4, RoundingMode.HALF_UP).multiply(z(re.premiumPct())).divide(HUNDRED, 4, RoundingMode.HALF_UP);
                    }
                    lines.add(new SimLine(nameOf(l.layerName(), "XOL layer " + l.layerNo()), money(rec), pct(rec, z(l.limitAmt())), money(rp), BigDecimal.ZERO,
                        split(t, group, l.layerNo(), rec, rp, BigDecimal.ZERO)));
                    total = total.add(rec);
                }
                retainedSi = loss.subtract(total).max(BigDecimal.ZERO);
                retainedPremium = BigDecimal.ZERO;
                notes.add("Amount = layer recovery, % = share of layer limit used, premium column = reinstatement premium.");
            }
            case LookupCodes.STRAT_STOP -> {
                var s = row.stopLoss();
                if (s == null) {
                    notes.add("Stop loss parameters are incomplete.");
                } else {
                    BigDecimal claims = z(in.lossAmount());
                    BigDecimal lr = premium.signum() == 0 ? BigDecimal.ZERO : pct(claims, premium);
                    BigDecimal band = s.exitLrPct().subtract(s.attachmentLrPct());
                    BigDecimal points = lr.subtract(s.attachmentLrPct()).max(BigDecimal.ZERO).min(band);
                    BigDecimal rec = premium.multiply(points).divide(HUNDRED, 4, RoundingMode.HALF_UP);
                    BigDecimal limit = premium.multiply(z(s.limitPctOfPremium())).divide(HUNDRED, 4, RoundingMode.HALF_UP);
                    rec = rec.min(limit);
                    notes.add("Premium field = subject premium (GNPI); loss field = incurred claims. Loss ratio " + lr.setScale(2, RoundingMode.HALF_UP) + "%.");
                    lines.add(new SimLine("Stop loss recovery", money(rec), lr.setScale(2, RoundingMode.HALF_UP), BigDecimal.ZERO, BigDecimal.ZERO,
                        split(t, group, null, rec, BigDecimal.ZERO, BigDecimal.ZERO)));
                    retainedSi = claims.subtract(rec);
                    retainedPremium = premium;
                }
            }
            case LookupCodes.STRAT_FAC_RES -> notes.add("Facultative residual programme: placements are made case by case. Use the Allocation plan screen to see how much exposure reaches this step.");
            default -> notes.add("No simulation is available for this treaty type.");
        }
        return new SimulationResult(strategy, lines, money(retainedSi), money(retainedPremium), money(excess), excess.signum() > 0, notes);
    }

    private static String nameOf(String name, String fallback) { return name == null || name.isBlank() ? fallback : name; }
}
