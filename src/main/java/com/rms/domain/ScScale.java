package com.rms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Generated from table `sc_scale`. */
@Entity
@Table(name = "sc_scale")
@Getter
@Setter
@NoArgsConstructor
public class ScScale extends BaseEntity {

    @Column(name = "treaty_id", nullable = false, insertable = false, updatable = false)
    private Long treatyId;

    @Column(name = "provisional_pct", nullable = false, precision = 12, scale = 6)
    private BigDecimal provisionalPct;

    @Column(name = "min_pct", nullable = false, precision = 12, scale = 6)
    private BigDecimal minPct;

    @Column(name = "max_pct", nullable = false, precision = 12, scale = 6)
    private BigDecimal maxPct;

    @Column(name = "lr_at_max_pct", nullable = false, precision = 12, scale = 6)
    private BigDecimal lrAtMaxPct;

    @Column(name = "lr_at_min_pct", nullable = false, precision = 12, scale = 6)
    private BigDecimal lrAtMinPct;

    @Column(name = "slide_ratio", nullable = false, precision = 12, scale = 6)
    private BigDecimal slideRatio;

    @Column(name = "lr_basis_id", nullable = false)
    private Long lrBasisId;

    @Column(name = "lr_basis_desc", nullable = false, length = 100)
    private String lrBasisDesc;

    @Column(name = "adjustment_months", nullable = false)
    private Integer adjustmentMonths;
}
