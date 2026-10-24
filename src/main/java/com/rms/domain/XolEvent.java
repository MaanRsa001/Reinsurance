package com.rms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Generated from table `xol_event`. */
@Entity
@Table(name = "xol_event")
@Getter
@Setter
@NoArgsConstructor
public class XolEvent extends BaseEntity {

    @Column(name = "event_code", nullable = false, length = 50)
    private String eventCode;

    @Column(name = "claim_txn_id")
    private Long claimTxnId;

    @Column(name = "treaty_id", nullable = false)
    private Long treatyId;

    @Column(name = "treaty_code", nullable = false, length = 50)
    private String treatyCode;

    @Column(name = "treaty_desc", nullable = false, length = 255)
    private String treatyDesc;

    @Column(name = "event_desc", nullable = false, length = 255)
    private String eventDesc;

    @Column(name = "loss_date", nullable = false)
    private LocalDate lossDate;

    @Column(name = "loss_date_to")
    private LocalDate lossDateTo;

    @Column(name = "peril_desc", length = 100)
    private String perilDesc;

    @Column(name = "gross_loss", nullable = false, precision = 20, scale = 4)
    private BigDecimal grossLoss;

    @Column(name = "inuring_recovery", nullable = false, precision = 20, scale = 4)
    private BigDecimal inuringRecovery;

    @Column(name = "unl_amount", nullable = false, precision = 20, scale = 4)
    private BigDecimal unlAmount;
}
