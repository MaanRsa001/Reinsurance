package com.rms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Generated from table `retro_placement`. */
@Entity
@Table(name = "retro_placement")
@Getter
@Setter
@NoArgsConstructor
public class RetroPlacement extends BaseEntity {

    @Column(name = "fac_in_acceptance_id", nullable = false)
    private Long facInAcceptanceId;

    @Column(name = "retro_method_id", nullable = false)
    private Long retroMethodId;

    @Column(name = "retro_method_desc", nullable = false, length = 100)
    private String retroMethodDesc;

    @Column(name = "reinsurer_id", nullable = false)
    private Long reinsurerId;

    @Column(name = "reinsurer_code", nullable = false, length = 50)
    private String reinsurerCode;

    @Column(name = "reinsurer_desc", nullable = false, length = 255)
    private String reinsurerDesc;

    @Column(name = "retro_share_pct", nullable = false, precision = 12, scale = 6)
    private BigDecimal retroSharePct;

    @Column(name = "retro_si", nullable = false, precision = 20, scale = 4)
    private BigDecimal retroSi;

    @Column(name = "retro_premium", nullable = false, precision = 20, scale = 4)
    private BigDecimal retroPremium;

    @Column(name = "commission_pct", nullable = false, precision = 12, scale = 6)
    private BigDecimal commissionPct;

    @Column(name = "commission_amt", nullable = false, precision = 20, scale = 4)
    private BigDecimal commissionAmt;

    @Column(name = "status_id", nullable = false)
    private Long statusId;

    @Column(name = "status_desc", nullable = false, length = 100)
    private String statusDesc;
}
