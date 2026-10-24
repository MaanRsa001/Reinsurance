package com.rms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Generated from table `ifrs_assumption_set`. */
@Entity
@Table(name = "ifrs_assumption_set")
@Getter
@Setter
@NoArgsConstructor
public class IfrsAssumptionSet extends BaseEntity {

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "description", nullable = false, length = 255)
    private String description;

    @Column(name = "discount_curve_ref", nullable = false, length = 60)
    private String discountCurveRef;

    @Column(name = "ra_pct", nullable = false, precision = 12, scale = 6)
    private BigDecimal raPct;

    @Column(name = "locked_in_rate_pct", nullable = false, precision = 12, scale = 6)
    private BigDecimal lockedInRatePct;

    @Column(name = "approved_flag", nullable = false)
    private Boolean approvedFlag;
}
