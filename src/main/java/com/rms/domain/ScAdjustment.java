package com.rms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Generated from table `sc_adjustment`. */
@Entity
@Table(name = "sc_adjustment")
@Getter
@Setter
@NoArgsConstructor
public class ScAdjustment extends BaseEntity {

    @Column(name = "treaty_id", nullable = false)
    private Long treatyId;

    @Column(name = "treaty_code", nullable = false, length = 50)
    private String treatyCode;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "ceded_earned_premium", nullable = false, precision = 20, scale = 4)
    private BigDecimal cededEarnedPremium;

    @Column(name = "ceded_incurred_claims", nullable = false, precision = 20, scale = 4)
    private BigDecimal cededIncurredClaims;

    @Column(name = "loss_ratio_pct", nullable = false, precision = 12, scale = 6)
    private BigDecimal lossRatioPct;

    @Column(name = "unclamped_commission_pct", nullable = false, precision = 12, scale = 6)
    private BigDecimal unclampedCommissionPct;

    @Column(name = "applied_commission_pct", nullable = false, precision = 12, scale = 6)
    private BigDecimal appliedCommissionPct;

    @Column(name = "provisional_pct", nullable = false, precision = 12, scale = 6)
    private BigDecimal provisionalPct;

    @Column(name = "cumulative_adjustment", nullable = false, precision = 20, scale = 4)
    private BigDecimal cumulativeAdjustment;

    @Column(name = "previously_booked", nullable = false, precision = 20, scale = 4)
    private BigDecimal previouslyBooked;

    @Column(name = "incremental_adjustment", nullable = false, precision = 20, scale = 4)
    private BigDecimal incrementalAdjustment;

    @Column(name = "status_id", nullable = false)
    private Long statusId;

    @Column(name = "status_desc", nullable = false, length = 100)
    private String statusDesc;

    @Column(name = "mc_request_id")
    private Long mcRequestId;
}
