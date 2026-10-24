package com.rms.treaty;

import com.rms.common.ValidationIssue;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Request / response shapes of the treaty administration API. Master references are ids; descriptions are filled by the server. */
public final class TreatyDtos {
    private TreatyDtos() { }

    public record Ref(Long id, String code, String desc) { }

    public record QsParamsDto(Long id, BigDecimal cessionPct, BigDecimal retentionPct, BigDecimal perRiskLimit, BigDecimal perEventLimit,
                              Long commissionTypeId, String commissionTypeCode, String commissionTypeDesc, BigDecimal flatCommissionPct,
                              BigDecimal cashLossLimit) { }

    public record RetentionDto(Long id, Long riskGradeId, String riskGradeDesc, BigDecimal retentionAmt) { }

    public record SurplusLayerDto(Long id, Integer layerNo, String layerName, Integer noOfLines, BigDecimal retentionAmt, BigDecimal capacityAmt,
                                  Long commissionTypeId, String commissionTypeDesc, BigDecimal commissionPct) { }

    public record ReinstatementDto(Long id, Integer reinstatementNo, BigDecimal premiumPct, Long reinstatementBasisId, String reinstatementBasisDesc) { }

    public record XolLayerDto(Long id, Integer layerNo, String layerName, BigDecimal limitAmt, BigDecimal deductibleAmt, BigDecimal rateOnLinePct,
                              BigDecimal depositPremium, BigDecimal minPremium, Integer eventHoursClause, List<ReinstatementDto> reinstatements) { }

    public record StopLossDto(Long id, BigDecimal attachmentLrPct, BigDecimal exitLrPct, BigDecimal limitPctOfPremium, BigDecimal premiumRatePct,
                              String premiumBasisDesc) { }

    public record ScScaleDto(Long id, BigDecimal provisionalPct, BigDecimal minPct, BigDecimal maxPct, BigDecimal lrAtMaxPct, BigDecimal lrAtMinPct,
                             BigDecimal slideRatio, Long lrBasisId, String lrBasisDesc, Integer adjustmentMonths) { }

    public record PcConfigDto(Long id, BigDecimal pcPct, BigDecimal mgmtExpensePct, Integer carryForwardYears, Boolean dafEnabled) { }

    /**
     * Panel row. layerKind = the treaty type code of the mapping rows it serves (QS, SUR, XOL-RISK, XOL-CAT, STOP, FAC-OBL, FAC-RES);
     * layerNo = the surplus / XOL layer number (null for the other types). One panel per type / layer used anywhere in the treaty.
     */
    public record PanelDto(Long id, String layerKind, Integer layerNo, Long reinsurerId, String reinsurerCode, String reinsurerDesc, BigDecimal sharePct,
                           BigDecimal brokeragePct, Boolean leadFlag) { }

    public record ExclusionDto(Long id, String exclusionDesc, String actionDesc) { }

    /** HOW a treaty participates at the mapping's sequence (treaty_cover_allocation_rule). Ids or codes may be sent; the server stores id + code + description. */
    public record AllocationRuleDto(Long id, Integer ruleNo, Long methodId, String methodCode, String methodDesc, Long basisId, String basisCode, String basisDesc,
                                    BigDecimal allocationPct, BigDecimal minAllocationAmt, BigDecimal maxAllocationAmt, Long residualActionId, String residualActionCode,
                                    String residualActionDesc, Boolean stopOnLimit, String conditionRule, LocalDate effectiveFrom, LocalDate effectiveTo) { }

    /**
     * One mapping row = WHERE (LOB / product / section / cover; levelCode derived from the deepest id), WHICH treaty type, in WHICH ORDER (sequenceNo)
     * and on WHICH terms. A treaty can carry several types against the same product. The terms used depend on the row's type:
     * quota share / fac obligatory -> qsParams; surplus -> retentions + surplusLayers; XOL -> xolLayers; stop loss -> stopLoss; fac residual -> none.
     * When two rows of the SAME treaty and SAME type reach one cover, the more specific row (cover > section > product > LOB) applies there.
     * coverCount and calcStrategyCode are filled by the server.
     */
    public record CoverMapDto(Long id, String levelCode, String levelDesc, Long lobId, String lobDesc, Long productId, String productDesc, Long sectionId,
                              String sectionDesc, Long coverId, String coverDesc, Integer sequenceNo, LocalDate effectiveFrom, LocalDate effectiveTo,
                              Integer coverCount, List<AllocationRuleDto> rules, Long treatyTypeId, String treatyTypeCode, String treatyTypeDesc,
                              String calcStrategyCode, QsParamsDto qsParams, List<RetentionDto> retentions, List<SurplusLayerDto> surplusLayers,
                              List<XolLayerDto> xolLayers, StopLossDto stopLoss, String note) {

        /** Same row with new ids cleared and dates re-based (renew / clone). */
        public CoverMapDto copyFor(LocalDate from, LocalDate to) {
            List<AllocationRuleDto> rs = rules == null ? List.of() : rules.stream().map(r -> new AllocationRuleDto(null, r.ruleNo(), r.methodId(), r.methodCode(), r.methodDesc(),
                r.basisId(), r.basisCode(), r.basisDesc(), r.allocationPct(), r.minAllocationAmt(), r.maxAllocationAmt(), r.residualActionId(), r.residualActionCode(),
                r.residualActionDesc(), r.stopOnLimit(), r.conditionRule(), from, to)).toList();
            QsParamsDto q = qsParams == null ? null : new QsParamsDto(null, qsParams.cessionPct(), qsParams.retentionPct(), qsParams.perRiskLimit(), qsParams.perEventLimit(),
                qsParams.commissionTypeId(), qsParams.commissionTypeCode(), qsParams.commissionTypeDesc(), qsParams.flatCommissionPct(), qsParams.cashLossLimit());
            List<RetentionDto> rt = retentions == null ? List.of() : retentions.stream().map(x -> new RetentionDto(null, x.riskGradeId(), x.riskGradeDesc(), x.retentionAmt())).toList();
            List<SurplusLayerDto> sl = surplusLayers == null ? List.of() : surplusLayers.stream().map(x -> new SurplusLayerDto(null, x.layerNo(), x.layerName(), x.noOfLines(),
                x.retentionAmt(), x.capacityAmt(), x.commissionTypeId(), x.commissionTypeDesc(), x.commissionPct())).toList();
            List<XolLayerDto> xl = xolLayers == null ? List.of() : xolLayers.stream().map(x -> new XolLayerDto(null, x.layerNo(), x.layerName(), x.limitAmt(), x.deductibleAmt(),
                x.rateOnLinePct(), x.depositPremium(), x.minPremium(), x.eventHoursClause(), x.reinstatements() == null ? List.of() : x.reinstatements().stream()
                    .map(re -> new ReinstatementDto(null, re.reinstatementNo(), re.premiumPct(), re.reinstatementBasisId(), re.reinstatementBasisDesc())).toList())).toList();
            StopLossDto st = stopLoss == null ? null : new StopLossDto(null, stopLoss.attachmentLrPct(), stopLoss.exitLrPct(), stopLoss.limitPctOfPremium(),
                stopLoss.premiumRatePct(), stopLoss.premiumBasisDesc());
            return new CoverMapDto(null, levelCode, levelDesc, lobId, lobDesc, productId, productDesc, sectionId, sectionDesc, coverId, coverDesc, sequenceNo, from, to,
                coverCount, rs, treatyTypeId, treatyTypeCode, treatyTypeDesc, calcStrategyCode, q, rt, sl, xl, st, note);
        }
    }

    /**
     * Create / update payload. On update the treatyCode is ignored (immutable). The treaty itself has no type and no terms:
     * they are on each mapping row (coverMap). Sliding-scale (for quota share rows with sliding commission) and profit commission / DAF stay treaty-level.
     */
    public record TreatyRequest(String treatyCode, String treatyName, Long treatyBasisId, Long cessionBasisId, Long currencyId, Long lobId, Long brokerId,
                                LocalDate inceptionDate, LocalDate expiryDate, LocalDate effectiveFrom, Long amendModeId, String remarks, ScScaleDto scScale,
                                PcConfigDto pcConfig, List<PanelDto> panel, List<ExclusionDto> exclusions, List<CoverMapDto> coverMap) {

        /** Copy used by "renew / clone": new identity and period, mapping rows (with their terms) re-based to the new period. */
        public TreatyRequest withIdentity(String code, String name, LocalDate inception, LocalDate expiry) {
            List<CoverMapDto> shifted = coverMap == null ? List.of() : coverMap.stream().map(c -> c.copyFor(inception, expiry)).toList();
            List<PanelDto> pn = panel == null ? List.of() : panel.stream().map(p -> new PanelDto(null, p.layerKind(), p.layerNo(), p.reinsurerId(), p.reinsurerCode(),
                p.reinsurerDesc(), p.sharePct(), p.brokeragePct(), p.leadFlag())).toList();
            return new TreatyRequest(code, name, treatyBasisId, cessionBasisId, currencyId, lobId, brokerId, inception, expiry, inception, null, remarks, scScale, pcConfig,
                pn, exclusions, shifted);
        }
    }

    /** treatyTypes = the distinct types used on the mapping rows. */
    public record TreatyView(Long id, String treatyCode, String treatyName, Integer versionNo, List<Ref> treatyTypes, Ref treatyBasis,
                             Ref cessionBasis, Ref currency, Ref lob, Ref broker, LocalDate inceptionDate, LocalDate expiryDate, LocalDate effectiveFrom,
                             Long supersedesTreatyId, Ref amendMode, Ref status, boolean editable, String remarks, ScScaleDto scScale, PcConfigDto pcConfig,
                             List<PanelDto> panel, List<ExclusionDto> exclusions, List<CoverMapDto> coverMap, String updatedBy, LocalDateTime updatedAt) { }

    /** treatyTypeDesc = the types used on the mapping rows, joined with " + ". */
    public record TreatySummary(Long id, String treatyCode, String treatyName, Integer versionNo, String treatyTypeDesc, String lobDesc,
                                String statusDesc, String statusCode, LocalDate inceptionDate, LocalDate expiryDate, String updatedBy,
                                LocalDateTime updatedAt) { }

    public record SaveResult(TreatyView treaty, boolean newVersionCreated, List<ValidationIssue> issues) { }

    public record SubmitRequest(String remarks) { }

    public record RenewRequest(String newTreatyCode, String newTreatyName, LocalDate inceptionDate, LocalDate expiryDate) { }

    /** mappingRow (0-based): the mapping row whose type and terms are simulated (default: the first row). */
    public record SimulationRequest(BigDecimal sumInsured, BigDecimal premium, Long riskGradeId, BigDecimal lossAmount, Integer mappingRow) {
        public SimulationRequest(BigDecimal sumInsured, BigDecimal premium, Long riskGradeId, BigDecimal lossAmount) {
            this(sumInsured, premium, riskGradeId, lossAmount, null);
        }
    }

    public record SimShare(String reinsurerDesc, BigDecimal sharePct, BigDecimal amount, BigDecimal premium, BigDecimal commission) { }

    public record SimLine(String label, BigDecimal amount, BigDecimal pct, BigDecimal premium, BigDecimal commission, List<SimShare> shares) { }

    public record SimulationResult(String strategy, List<SimLine> lines, BigDecimal retainedSi, BigDecimal retainedPremium, BigDecimal excessSi,
                                   boolean facRequired, List<String> notes) { }

    public record DiffEntry(String path, String before, String after) { }
}
