package com.rms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Generated from table `claim_recovery`. */
@Entity
@Table(name = "claim_recovery")
@Getter
@Setter
@NoArgsConstructor
public class ClaimRecovery extends BaseEntity {

    @Column(name = "claim_txn_id")
    private Long claimTxnId;

    @Column(name = "claim_no", nullable = false, length = 50)
    private String claimNo;

    @Column(name = "cession_id")
    private Long cessionId;

    @Column(name = "xol_event_id")
    private Long xolEventId;

    @Column(name = "treaty_id", nullable = false)
    private Long treatyId;

    @Column(name = "treaty_code", nullable = false, length = 50)
    private String treatyCode;

    @Column(name = "treaty_desc", nullable = false, length = 255)
    private String treatyDesc;

    @Column(name = "treaty_type_id", nullable = false)
    private Long treatyTypeId;

    @Column(name = "treaty_type_code", nullable = false, length = 50)
    private String treatyTypeCode;

    @Column(name = "treaty_type_desc", nullable = false, length = 255)
    private String treatyTypeDesc;

    @Column(name = "surplus_layer_id")
    private Long surplusLayerId;

    @Column(name = "xol_layer_id")
    private Long xolLayerId;

    @Column(name = "cession_pct", precision = 12, scale = 6)
    private BigDecimal cessionPct;

    @Column(name = "paid_recoverable_delta", nullable = false, precision = 20, scale = 4)
    private BigDecimal paidRecoverableDelta;

    @Column(name = "os_recoverable_delta", nullable = false, precision = 20, scale = 4)
    private BigDecimal osRecoverableDelta;

    @Column(name = "status_id", nullable = false)
    private Long statusId;

    @Column(name = "status_desc", nullable = false, length = 100)
    private String statusDesc;
}
