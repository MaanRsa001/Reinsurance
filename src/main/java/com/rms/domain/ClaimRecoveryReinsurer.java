package com.rms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Generated from table `claim_recovery_reinsurer`. */
@Entity
@Table(name = "claim_recovery_reinsurer")
@Getter
@Setter
@NoArgsConstructor
public class ClaimRecoveryReinsurer extends BaseEntity {

    @Column(name = "claim_recovery_id", nullable = false)
    private Long claimRecoveryId;

    @Column(name = "reinsurer_id", nullable = false)
    private Long reinsurerId;

    @Column(name = "reinsurer_code", nullable = false, length = 50)
    private String reinsurerCode;

    @Column(name = "reinsurer_desc", nullable = false, length = 255)
    private String reinsurerDesc;

    @Column(name = "share_pct", nullable = false, precision = 12, scale = 6)
    private BigDecimal sharePct;

    @Column(name = "paid_recoverable", nullable = false, precision = 20, scale = 4)
    private BigDecimal paidRecoverable;

    @Column(name = "os_recoverable", nullable = false, precision = 20, scale = 4)
    private BigDecimal osRecoverable;

    @Column(name = "status_id", nullable = false)
    private Long statusId;

    @Column(name = "status_desc", nullable = false, length = 100)
    private String statusDesc;

    @Column(name = "remarks", length = 500)
    private String remarks;
}
