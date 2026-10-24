package com.rms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Generated from table `pc_computation`. */
@Entity
@Table(name = "pc_computation")
@Getter
@Setter
@NoArgsConstructor
public class PcComputation extends BaseEntity {

    @Column(name = "treaty_id", nullable = false)
    private Long treatyId;

    @Column(name = "treaty_code", nullable = false, length = 50)
    private String treatyCode;

    @Column(name = "accounting_year", nullable = false)
    private Integer accountingYear;

    @Column(name = "ceded_premium", nullable = false, precision = 20, scale = 4)
    private BigDecimal cededPremium;

    @Column(name = "ceded_commission", nullable = false, precision = 20, scale = 4)
    private BigDecimal cededCommission;

    @Column(name = "ceded_claims", nullable = false, precision = 20, scale = 4)
    private BigDecimal cededClaims;

    @Column(name = "mgmt_expense", nullable = false, precision = 20, scale = 4)
    private BigDecimal mgmtExpense;

    @Column(name = "result_before_daf", nullable = false, precision = 20, scale = 4)
    private BigDecimal resultBeforeDaf;

    @Column(name = "daf_brought_forward", nullable = false, precision = 20, scale = 4)
    private BigDecimal dafBroughtForward;

    @Column(name = "daf_added", nullable = false, precision = 20, scale = 4)
    private BigDecimal dafAdded;

    @Column(name = "daf_absorbed", nullable = false, precision = 20, scale = 4)
    private BigDecimal dafAbsorbed;

    @Column(name = "daf_expired", nullable = false, precision = 20, scale = 4)
    private BigDecimal dafExpired;

    @Column(name = "daf_carried_forward", nullable = false, precision = 20, scale = 4)
    private BigDecimal dafCarriedForward;

    @Column(name = "profit_after_daf", nullable = false, precision = 20, scale = 4)
    private BigDecimal profitAfterDaf;

    @Column(name = "pc_pct", nullable = false, precision = 12, scale = 6)
    private BigDecimal pcPct;

    @Column(name = "pc_amount", nullable = false, precision = 20, scale = 4)
    private BigDecimal pcAmount;

    @Column(name = "status_id", nullable = false)
    private Long statusId;

    @Column(name = "status_desc", nullable = false, length = 100)
    private String statusDesc;
}
