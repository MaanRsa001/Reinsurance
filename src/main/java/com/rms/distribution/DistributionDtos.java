package com.rms.distribution;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Policy-wise and claim-wise reinsurance distribution views plus the share-modification request. */
public final class DistributionDtos {
    private DistributionDtos() { }

    // ---------------------------------------------------------------- policy side
    public record ShareDist(Long id, Long reinsurerId, String reinsurerDesc, BigDecimal sharePct, BigDecimal cededSi, BigDecimal cededPremium, BigDecimal cededCommission) { }

    public record LayerDist(Long id, Integer layerNo, String layerDesc, BigDecimal cededSi, BigDecimal cessionPct, BigDecimal cededPremium,
                            BigDecimal commissionPct, BigDecimal cededCommission, List<ShareDist> shares) { }

    public record CessionDist(Long id, Long treatyId, String treatyCode, Integer treatyVersion, String treatyTypeDesc, BigDecimal basisSi, BigDecimal retainedSi,
                              BigDecimal cededSi, BigDecimal excessSi, BigDecimal grossPremium, BigDecimal retainedPremium, BigDecimal cededPremium,
                              BigDecimal excessPremium, BigDecimal cededCommission, String remarks, List<LayerDist> layers) { }

    public record CoverDist(Long coverId, String coverDesc, String sectionDesc, BigDecimal sumInsured, BigDecimal sumInsuredAfter, BigDecimal premium,
                            List<CessionDist> cessions) { }

    public record TxnDist(Long id, String endorsementNo, String txnType, LocalDate txnDate, List<CoverDist> covers) { }

    public record FacShare(String reinsurerDesc, BigDecimal sharePct, BigDecimal placedSi, BigDecimal premium, BigDecimal commission, BigDecimal brokerage) { }

    public record FacDist(String facRef, String coverDesc, BigDecimal placedSi, BigDecimal premium, BigDecimal commissionPct, BigDecimal brokeragePct,
                          String status, List<FacShare> reinsurers) { }

    public record ReinsurerTotal(Long reinsurerId, String reinsurerDesc, BigDecimal cededSi, BigDecimal cededPremium, BigDecimal cededCommission) { }

    public record PolicyListItem(String policyNo, String insuredName, String productDesc, int txnCount, LocalDate latestTxnDate, BigDecimal premium,
                                 BigDecimal sumInsured, int cessionCount) { }

    public record PolicyDistribution(String policyNo, String insuredName, String productDesc, String locationDesc, LocalDate inceptionDate,
                                     LocalDate expiryDate, List<TxnDist> transactions, List<FacDist> facultative, List<ReinsurerTotal> totals) { }

    // ---------------------------------------------------------------- claim side
    public record ClaimShare(Long id, Long reinsurerId, String reinsurerDesc, BigDecimal sharePct, BigDecimal paidRecoverable, BigDecimal osRecoverable,
                             String status, String remarks) { }

    public record RecoveryDist(Long id, String treatyCode, String treatyTypeDesc, String layerLabel, BigDecimal cessionPct, BigDecimal paidDelta,
                               BigDecimal osDelta, String status, List<ClaimShare> shares) { }

    public record ClaimTxnDist(Long id, String txnType, LocalDate txnDate, BigDecimal paidAmt, BigDecimal outstandingAmt, BigDecimal cumulativePaid,
                               List<RecoveryDist> recoveries) { }

    public record EventDist(String eventCode, String eventDesc, String treatyCode, BigDecimal grossLoss, BigDecimal inuringRecovery, BigDecimal unlAmount,
                            BigDecimal reinstatementPremium, List<RecoveryDist> recoveries) { }

    public record ClaimReinsurerTotal(Long reinsurerId, String reinsurerDesc, BigDecimal paidRecoverable, BigDecimal osRecoverable) { }

    public record ClaimListItem(String claimNo, String policyNo, String coverDesc, LocalDate dateOfLoss, BigDecimal cumulativePaid, BigDecimal outstanding, int txnCount) { }

    public record ClaimDistribution(String claimNo, String policyNo, String coverDesc, LocalDate dateOfLoss, List<ClaimTxnDist> transactions,
                                    List<EventDist> events, List<ClaimReinsurerTotal> totals) { }

    // ---------------------------------------------------------------- modification
    public record ShareInput(Long reinsurerId, BigDecimal sharePct) { }

    /** scope = POLICY (targetId = cession layer id) or CLAIM (targetId = claim recovery id). */
    public record AdjustmentRequest(String scope, Long targetId, String reason, List<ShareInput> shares) { }

    public record AdjustmentResult(Long requestId, String requestNo, String status, String message) { }
}
