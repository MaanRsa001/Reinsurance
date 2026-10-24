package com.rms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Generated from table `ri_account`. */
@Entity
@Table(name = "ri_account")
@Getter
@Setter
@NoArgsConstructor
public class RiAccount extends BaseEntity {

    @Column(name = "account_no", nullable = false, length = 30)
    private String accountNo;

    @Column(name = "reinsurer_id", nullable = false)
    private Long reinsurerId;

    @Column(name = "reinsurer_code", nullable = false, length = 50)
    private String reinsurerCode;

    @Column(name = "reinsurer_desc", nullable = false, length = 255)
    private String reinsurerDesc;

    @Column(name = "treaty_id", nullable = false)
    private Long treatyId;

    @Column(name = "treaty_code", nullable = false, length = 50)
    private String treatyCode;

    @Column(name = "treaty_desc", nullable = false, length = 255)
    private String treatyDesc;

    @Column(name = "period_from", nullable = false)
    private LocalDate periodFrom;

    @Column(name = "period_to", nullable = false)
    private LocalDate periodTo;

    @Column(name = "currency_id", nullable = false)
    private Long currencyId;

    @Column(name = "currency_code", nullable = false, length = 50)
    private String currencyCode;

    @Column(name = "currency_desc", nullable = false, length = 255)
    private String currencyDesc;

    @Column(name = "balance_amt", nullable = false, precision = 20, scale = 4)
    private BigDecimal balanceAmt;

    @Column(name = "status_id", nullable = false)
    private Long statusId;

    @Column(name = "status_desc", nullable = false, length = 100)
    private String statusDesc;
}
