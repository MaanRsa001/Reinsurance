package com.rms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Generated from table `fac_out_reinsurer`. */
@Entity
@Table(name = "fac_out_reinsurer")
@Getter
@Setter
@NoArgsConstructor
public class FacOutReinsurer extends BaseEntity {

    @Column(name = "placement_id", nullable = false)
    private Long placementId;

    @Column(name = "reinsurer_id", nullable = false)
    private Long reinsurerId;

    @Column(name = "reinsurer_code", nullable = false, length = 50)
    private String reinsurerCode;

    @Column(name = "reinsurer_desc", nullable = false, length = 255)
    private String reinsurerDesc;

    @Column(name = "share_pct", nullable = false, precision = 12, scale = 6)
    private BigDecimal sharePct;

    @Column(name = "placed_si", nullable = false, precision = 20, scale = 4)
    private BigDecimal placedSi;

    @Column(name = "premium", nullable = false, precision = 20, scale = 4)
    private BigDecimal premium;

    @Column(name = "commission", nullable = false, precision = 20, scale = 4)
    private BigDecimal commission;

    @Column(name = "brokerage", nullable = false, precision = 20, scale = 4)
    private BigDecimal brokerage;
}
