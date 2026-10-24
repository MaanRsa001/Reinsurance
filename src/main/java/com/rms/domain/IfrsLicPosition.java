package com.rms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Generated from table `ifrs_lic_position`. */
@Entity
@Table(name = "ifrs_lic_position")
@Getter
@Setter
@NoArgsConstructor
public class IfrsLicPosition extends BaseEntity {

    @Column(name = "run_id", nullable = false)
    private Long runId;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "os_recoveries", nullable = false, precision = 20, scale = 4)
    private BigDecimal osRecoveries;

    @Column(name = "discount_factor", nullable = false, precision = 12, scale = 6)
    private BigDecimal discountFactor;

    @Column(name = "pv_recoveries", nullable = false, precision = 20, scale = 4)
    private BigDecimal pvRecoveries;

    @Column(name = "ra_pct", nullable = false, precision = 12, scale = 6)
    private BigDecimal raPct;

    @Column(name = "ra_amount", nullable = false, precision = 20, scale = 4)
    private BigDecimal raAmount;

    @Column(name = "lic_asset", nullable = false, precision = 20, scale = 4)
    private BigDecimal licAsset;
}
