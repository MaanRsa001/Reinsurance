package com.rms.allocation;

import java.math.BigDecimal;

/**
 * One step of the execution chain of a cover: a treaty participating at a sequence with a method, applied to a basis.
 * Comes from treaty_cover_map (which treaty, which order) + treaty_cover_allocation_rule (how it participates).
 * coverMapId identifies the mapping row (its treaty type and terms); levelRank = 4 cover, 3 section, 2 product, 1 LOB - when two rows of the
 * same treaty and the same treaty type reach one cover, only the highest-ranked (most specific) row takes part. treatyTypeId = the row's type.
 */
public record PlanStep(int sequenceNo, int ruleNo, Long treatyId, String treatyCode, String treatyTypeDesc, String strategy, String methodCode, String methodDesc,
                       String basisCode, String basisDesc, String residualActionCode, String residualActionDesc, BigDecimal allocationPct,
                       BigDecimal minAllocationAmt, BigDecimal maxAllocationAmt, boolean stopOnLimit, String conditionRule, String levelDesc,
                       Long coverMapId, int levelRank, Long treatyTypeId) {

    public PlanStep(int sequenceNo, int ruleNo, Long treatyId, String treatyCode, String treatyTypeDesc, String strategy, String methodCode, String methodDesc,
                    String basisCode, String basisDesc, String residualActionCode, String residualActionDesc, BigDecimal allocationPct,
                    BigDecimal minAllocationAmt, BigDecimal maxAllocationAmt, boolean stopOnLimit, String conditionRule, String levelDesc) {
        this(sequenceNo, ruleNo, treatyId, treatyCode, treatyTypeDesc, strategy, methodCode, methodDesc, basisCode, basisDesc, residualActionCode, residualActionDesc,
            allocationPct, minAllocationAmt, maxAllocationAmt, stopOnLimit, conditionRule, levelDesc, null, 0, null);
    }

    /** 4 = cover, 3 = section, 2 = product, 1 = line of business. */
    public static int rank(Long productId, Long sectionId, Long coverId) {
        return coverId != null ? 4 : sectionId != null ? 3 : productId != null ? 2 : 1;
    }

    public static int rank(String levelCode) {
        if (levelCode == null) return 0;
        return switch (levelCode) { case "COVER" -> 4; case "SECTION" -> 3; case "PRODUCT" -> 2; default -> 1; };
    }
}
