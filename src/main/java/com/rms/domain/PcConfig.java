package com.rms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Generated from table `pc_config`. */
@Entity
@Table(name = "pc_config")
@Getter
@Setter
@NoArgsConstructor
public class PcConfig extends BaseEntity {

    @Column(name = "treaty_id", nullable = false, insertable = false, updatable = false)
    private Long treatyId;

    @Column(name = "pc_pct", nullable = false, precision = 12, scale = 6)
    private BigDecimal pcPct;

    @Column(name = "mgmt_expense_pct", nullable = false, precision = 12, scale = 6)
    private BigDecimal mgmtExpensePct;

    @Column(name = "carry_forward_years", nullable = false)
    private Integer carryForwardYears;

    @Column(name = "daf_enabled", nullable = false)
    private Boolean dafEnabled;
}
