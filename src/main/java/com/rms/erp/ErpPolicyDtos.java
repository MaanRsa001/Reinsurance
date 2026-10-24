package com.rms.erp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Request / response shapes of the ERP policy API (tables erp_policy_txn and erp_policy_cover).
 * Master references are sent as ids; the code / description snapshots are filled by the server.
 */
public final class ErpPolicyDtos {
    private ErpPolicyDtos() { }

    // ------------------------------------------------------------------ erp_policy_cover

    /** Create / update one cover line. sectionId must belong to the transaction's product and coverId to that section. */
    public record PolicyCoverRequest(Long sectionId, Long coverId, BigDecimal sumInsured, BigDecimal sumInsuredAfter,
                                     BigDecimal premium, BigDecimal deductibleAmt, Integer rowVersion) { }

    public record PolicyCoverView(Long id, Long policyTxnId, Long sectionId, String sectionCode, String sectionDesc,
                                  Long coverId, String coverCode, String coverDesc, BigDecimal sumInsured, BigDecimal sumInsuredAfter,
                                  BigDecimal premium, BigDecimal deductibleAmt, String recordStatus, Integer rowVersion,
                                  String createdBy, LocalDateTime createdAt, String updatedBy, LocalDateTime updatedAt) { }

    // ------------------------------------------------------------------ erp_policy_txn

    /**
     * Create / update a policy transaction. On create, covers (optional) are inserted with it.
     * On update, covers == null leaves the cover lines untouched; a list replaces them
     * (rows with an id are updated, rows without an id are added, existing rows not in the list are deleted).
     */
    public record PolicyTxnRequest(Long inboundMessageId, String policyNo, String endorsementNo, Long txnTypeId, LocalDate txnDate,
                                   LocalDate inceptionDate, LocalDate expiryDate, Long currencyId, Long productId, String insuredName,
                                   String locationDesc, String catZoneCode, Long riskGradeId, Integer rowVersion,
                                   List<PolicyCoverLine> covers) { }

    /** A cover line inside a transaction request (id null = new line). */
    public record PolicyCoverLine(Long id, Long sectionId, Long coverId, BigDecimal sumInsured, BigDecimal sumInsuredAfter,
                                  BigDecimal premium, BigDecimal deductibleAmt) { }

    public record PolicyTxnSummary(Long id, Long inboundMessageId, String policyNo, String endorsementNo, Long txnTypeId, String txnTypeDesc,
                                   LocalDate txnDate, LocalDate inceptionDate, LocalDate expiryDate, String currencyCode, String productCode,
                                   String productDesc, String insuredName, Integer coverCount, BigDecimal totalSumInsuredAfter,
                                   BigDecimal totalPremium, String recordStatus) { }

    public record PolicyTxnView(Long id, Long inboundMessageId, String policyNo, String endorsementNo, Long txnTypeId, String txnTypeDesc,
                                LocalDate txnDate, LocalDate inceptionDate, LocalDate expiryDate, Long currencyId, String currencyCode,
                                String currencyDesc, Long productId, String productCode, String productDesc, String insuredName,
                                String locationDesc, String catZoneCode, Long riskGradeId, String riskGradeDesc,
                                BigDecimal totalSumInsured, BigDecimal totalSumInsuredAfter, BigDecimal totalPremium,
                                List<PolicyCoverView> covers, String recordStatus, Integer rowVersion,
                                String createdBy, LocalDateTime createdAt, String updatedBy, LocalDateTime updatedAt) { }
}
