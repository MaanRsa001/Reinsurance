package com.rms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Generated from table `treaty_cover_allocation_rule`. */
@Entity
@Table(name = "treaty_cover_allocation_rule")
@Getter
@Setter
@NoArgsConstructor
public class TreatyCoverAllocationRule extends BaseEntity {

    @Column(name = "treaty_cover_map_id", nullable = false, insertable = false, updatable = false)
    private Long treatyCoverMapId;

    @Column(name = "rule_no", nullable = false)
    private Integer ruleNo;

    @Column(name = "allocation_method_id", nullable = false)
    private Long allocationMethodId;

    @Column(name = "allocation_method_code", nullable = false, length = 50)
    private String allocationMethodCode;

    @Column(name = "allocation_method_desc", nullable = false, length = 100)
    private String allocationMethodDesc;

    @Column(name = "allocation_basis_id", nullable = false)
    private Long allocationBasisId;

    @Column(name = "allocation_basis_code", nullable = false, length = 50)
    private String allocationBasisCode;

    @Column(name = "allocation_basis_desc", nullable = false, length = 100)
    private String allocationBasisDesc;

    @Column(name = "allocation_pct", precision = 12, scale = 6)
    private BigDecimal allocationPct;

    @Column(name = "min_allocation_amt", precision = 20, scale = 4)
    private BigDecimal minAllocationAmt;

    @Column(name = "max_allocation_amt", precision = 20, scale = 4)
    private BigDecimal maxAllocationAmt;

    @Column(name = "residual_action_id", nullable = false)
    private Long residualActionId;

    @Column(name = "residual_action_code", nullable = false, length = 50)
    private String residualActionCode;

    @Column(name = "residual_action_desc", nullable = false, length = 100)
    private String residualActionDesc;

    @Column(name = "stop_on_limit_flag", nullable = false)
    private Boolean stopOnLimitFlag;

    @Column(name = "condition_rule", length = 2000)
    private String conditionRule;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to", nullable = false)
    private LocalDate effectiveTo;
}
