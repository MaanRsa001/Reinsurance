package com.rms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Generated from table `cession_layer`. */
@Entity
@Table(name = "cession_layer")
@Getter
@Setter
@NoArgsConstructor
public class CessionLayer extends BaseEntity {

    @Column(name = "cession_id", nullable = false)
    private Long cessionId;

    @Column(name = "layer_no", nullable = false)
    private Integer layerNo;

    @Column(name = "surplus_layer_id")
    private Long surplusLayerId;

    @Column(name = "layer_desc", nullable = false, length = 100)
    private String layerDesc;

    @Column(name = "ceded_si", nullable = false, precision = 20, scale = 4)
    private BigDecimal cededSi;

    @Column(name = "cession_pct", nullable = false, precision = 12, scale = 6)
    private BigDecimal cessionPct;

    @Column(name = "ceded_premium", nullable = false, precision = 20, scale = 4)
    private BigDecimal cededPremium;

    @Column(name = "commission_pct", nullable = false, precision = 12, scale = 6)
    private BigDecimal commissionPct;

    @Column(name = "ceded_commission", nullable = false, precision = 20, scale = 4)
    private BigDecimal cededCommission;
}
