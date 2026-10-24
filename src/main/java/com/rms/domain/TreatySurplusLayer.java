package com.rms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Generated from table `treaty_surplus_layer`. */
@Entity
@Table(name = "treaty_surplus_layer")
@Getter
@Setter
@NoArgsConstructor
public class TreatySurplusLayer extends BaseEntity {

    /** Owning treaty (filled by the service; the terms belong to one mapping row, see treatyCoverMapId). */
    @Column(name = "treaty_id", nullable = false)
    private Long treatyId;

    /** Mapping row these terms belong to (managed by TreatyCoverMap). */
    @Column(name = "treaty_cover_map_id", insertable = false, updatable = false)
    private Long treatyCoverMapId;

    @Column(name = "layer_no", nullable = false)
    private Integer layerNo;

    @Column(name = "layer_name", nullable = false, length = 100)
    private String layerName;

    @Column(name = "no_of_lines", nullable = false)
    private Integer noOfLines;

    @Column(name = "retention_amt", nullable = false, precision = 20, scale = 4)
    private BigDecimal retentionAmt;

    @Column(name = "capacity_amt", nullable = false, precision = 20, scale = 4)
    private BigDecimal capacityAmt;

    @Column(name = "commission_type_id", nullable = false)
    private Long commissionTypeId;

    @Column(name = "commission_type_code", nullable = false, length = 50)
    private String commissionTypeCode;

    @Column(name = "commission_type_desc", nullable = false, length = 255)
    private String commissionTypeDesc;

    @Column(name = "commission_pct", nullable = false, precision = 12, scale = 6)
    private BigDecimal commissionPct;
}
