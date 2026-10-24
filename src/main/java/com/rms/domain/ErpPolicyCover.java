package com.rms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Generated from table `erp_policy_cover`. */
@Entity
@Table(name = "erp_policy_cover")
@Getter
@Setter
@NoArgsConstructor
public class ErpPolicyCover extends BaseEntity {

    @Column(name = "policy_txn_id", nullable = false)
    private Long policyTxnId;

    @Column(name = "section_id", nullable = false)
    private Long sectionId;

    @Column(name = "section_code", nullable = false, length = 50)
    private String sectionCode;

    @Column(name = "section_desc", nullable = false, length = 255)
    private String sectionDesc;

    @Column(name = "cover_id", nullable = false)
    private Long coverId;

    @Column(name = "cover_code", nullable = false, length = 50)
    private String coverCode;

    @Column(name = "cover_desc", nullable = false, length = 255)
    private String coverDesc;

    @Column(name = "sum_insured", nullable = false, precision = 20, scale = 4)
    private BigDecimal sumInsured;

    @Column(name = "sum_insured_after", nullable = false, precision = 20, scale = 4)
    private BigDecimal sumInsuredAfter;

    @Column(name = "premium", nullable = false, precision = 20, scale = 4)
    private BigDecimal premium;

    @Column(name = "deductible_amt", precision = 20, scale = 4)
    private BigDecimal deductibleAmt;
}
