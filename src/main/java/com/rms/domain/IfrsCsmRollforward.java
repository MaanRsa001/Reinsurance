package com.rms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Generated from table `ifrs_csm_rollforward`. */
@Entity
@Table(name = "ifrs_csm_rollforward")
@Getter
@Setter
@NoArgsConstructor
public class IfrsCsmRollforward extends BaseEntity {

    @Column(name = "run_id", nullable = false)
    private Long runId;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "opening_csm", nullable = false, precision = 20, scale = 4)
    private BigDecimal openingCsm;

    @Column(name = "accretion_pct", nullable = false, precision = 12, scale = 6)
    private BigDecimal accretionPct;

    @Column(name = "accretion_amt", nullable = false, precision = 20, scale = 4)
    private BigDecimal accretionAmt;

    @Column(name = "fcf_change_future_service", nullable = false, precision = 20, scale = 4)
    private BigDecimal fcfChangeFutureService;

    @Column(name = "csm_before_release", nullable = false, precision = 20, scale = 4)
    private BigDecimal csmBeforeRelease;

    @Column(name = "coverage_units_pct", nullable = false, precision = 12, scale = 6)
    private BigDecimal coverageUnitsPct;

    @Column(name = "release_amt", nullable = false, precision = 20, scale = 4)
    private BigDecimal releaseAmt;

    @Column(name = "closing_csm", nullable = false, precision = 20, scale = 4)
    private BigDecimal closingCsm;
}
