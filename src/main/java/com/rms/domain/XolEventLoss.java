package com.rms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Generated from table `xol_event_loss`. */
@Entity
@Table(name = "xol_event_loss")
@Getter
@Setter
@NoArgsConstructor
public class XolEventLoss extends BaseEntity {

    @Column(name = "xol_event_id", nullable = false)
    private Long xolEventId;

    @Column(name = "claim_txn_id", nullable = false)
    private Long claimTxnId;

    @Column(name = "claim_no", nullable = false, length = 50)
    private String claimNo;

    @Column(name = "gross_loss", nullable = false, precision = 20, scale = 4)
    private BigDecimal grossLoss;

    @Column(name = "inuring_recovery", nullable = false, precision = 20, scale = 4)
    private BigDecimal inuringRecovery;

    @Column(name = "net_retained_loss", nullable = false, precision = 20, scale = 4)
    private BigDecimal netRetainedLoss;
}
