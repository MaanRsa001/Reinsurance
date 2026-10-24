package com.rms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Generated from table `ifrs_group`. */
@Entity
@Table(name = "ifrs_group")
@Getter
@Setter
@NoArgsConstructor
public class IfrsGroup extends BaseEntity {

    @Column(name = "group_code", nullable = false, length = 50)
    private String groupCode;

    @Column(name = "portfolio_id", nullable = false)
    private Long portfolioId;

    @Column(name = "portfolio_code", nullable = false, length = 50)
    private String portfolioCode;

    @Column(name = "portfolio_desc", nullable = false, length = 255)
    private String portfolioDesc;

    @Column(name = "cohort_year", nullable = false)
    private Integer cohortYear;

    @Column(name = "profitability_id", nullable = false)
    private Long profitabilityId;

    @Column(name = "profitability_desc", nullable = false, length = 100)
    private String profitabilityDesc;

    @Column(name = "model_id", nullable = false)
    private Long modelId;

    @Column(name = "model_desc", nullable = false, length = 100)
    private String modelDesc;

    @Column(name = "treaty_id", nullable = false)
    private Long treatyId;

    @Column(name = "treaty_code", nullable = false, length = 50)
    private String treatyCode;

    @Column(name = "treaty_desc", nullable = false, length = 255)
    private String treatyDesc;
}
