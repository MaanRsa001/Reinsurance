package com.rms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Generated from table `daf_account`. */
@Entity
@Table(name = "daf_account")
@Getter
@Setter
@NoArgsConstructor
public class DafAccount extends BaseEntity {

    @Column(name = "treaty_id", nullable = false)
    private Long treatyId;

    @Column(name = "treaty_code", nullable = false, length = 50)
    private String treatyCode;

    @Column(name = "carry_forward_years", nullable = false)
    private Integer carryForwardYears;

    @Column(name = "opening_balance", nullable = false, precision = 20, scale = 4)
    private BigDecimal openingBalance;

    @Column(name = "current_balance", nullable = false, precision = 20, scale = 4)
    private BigDecimal currentBalance;
}
