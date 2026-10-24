package com.rms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Generated from table `treaty_surplus_retention`. */
@Entity
@Table(name = "treaty_surplus_retention")
@Getter
@Setter
@NoArgsConstructor
public class TreatySurplusRetention extends BaseEntity {

    /** Owning treaty (filled by the service; the terms belong to one mapping row, see treatyCoverMapId). */
    @Column(name = "treaty_id", nullable = false)
    private Long treatyId;

    /** Mapping row these terms belong to (managed by TreatyCoverMap). */
    @Column(name = "treaty_cover_map_id", insertable = false, updatable = false)
    private Long treatyCoverMapId;

    @Column(name = "risk_grade_id", nullable = false)
    private Long riskGradeId;

    @Column(name = "risk_grade_desc", nullable = false, length = 100)
    private String riskGradeDesc;

    @Column(name = "retention_amt", nullable = false, precision = 20, scale = 4)
    private BigDecimal retentionAmt;
}
