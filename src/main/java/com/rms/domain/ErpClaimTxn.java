package com.rms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Generated from table `erp_claim_txn`. */
@Entity
@Table(name = "erp_claim_txn")
@Getter
@Setter
@NoArgsConstructor
public class ErpClaimTxn extends BaseEntity {

    @Column(name = "inbound_message_id", nullable = false)
    private Long inboundMessageId;

    @Column(name = "claim_no", nullable = false, length = 50)
    private String claimNo;

    @Column(name = "policy_no", nullable = false, length = 50)
    private String policyNo;

    @Column(name = "policy_txn_id")
    private Long policyTxnId;

    @Column(name = "cover_id", nullable = false)
    private Long coverId;

    @Column(name = "cover_code", nullable = false, length = 50)
    private String coverCode;

    @Column(name = "cover_desc", nullable = false, length = 255)
    private String coverDesc;

    @Column(name = "date_of_loss", nullable = false)
    private LocalDate dateOfLoss;

    @Column(name = "claim_txn_type_id", nullable = false)
    private Long claimTxnTypeId;

    @Column(name = "claim_txn_type_code", nullable = false, length = 50)
    private String claimTxnTypeCode;

    @Column(name = "claim_txn_type_desc", nullable = false, length = 255)
    private String claimTxnTypeDesc;

    @Column(name = "txn_date", nullable = false)
    private LocalDate txnDate;

    @Column(name = "outstanding_amt", nullable = false, precision = 20, scale = 4)
    private BigDecimal outstandingAmt;

    @Column(name = "paid_amt", nullable = false, precision = 20, scale = 4)
    private BigDecimal paidAmt;

    @Column(name = "cumulative_paid", nullable = false, precision = 20, scale = 4)
    private BigDecimal cumulativePaid;

    @Column(name = "currency_id", nullable = false)
    private Long currencyId;

    @Column(name = "currency_code", nullable = false, length = 50)
    private String currencyCode;

    @Column(name = "currency_desc", nullable = false, length = 255)
    private String currencyDesc;
}
