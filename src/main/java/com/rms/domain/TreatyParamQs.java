package com.rms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Generated from table `treaty_param_qs`. */
@Entity
@Table(name = "treaty_param_qs")
@Getter
@Setter
@NoArgsConstructor
public class TreatyParamQs extends BaseEntity {

    /** Owning treaty (filled by the service; the terms belong to one mapping row, see treatyCoverMapId). */
    @Column(name = "treaty_id", nullable = false)
    private Long treatyId;

    /** Mapping row these terms belong to (managed by TreatyCoverMap). */
    @Column(name = "treaty_cover_map_id", insertable = false, updatable = false)
    private Long treatyCoverMapId;

    @Column(name = "cession_pct", nullable = false, precision = 12, scale = 6)
    private BigDecimal cessionPct;

    @Column(name = "retention_pct", nullable = false, precision = 12, scale = 6)
    private BigDecimal retentionPct;

    @Column(name = "per_risk_limit", precision = 20, scale = 4)
    private BigDecimal perRiskLimit;

    @Column(name = "per_event_limit", precision = 20, scale = 4)
    private BigDecimal perEventLimit;

    @Column(name = "commission_type_id", nullable = false)
    private Long commissionTypeId;

    @Column(name = "commission_type_code", nullable = false, length = 50)
    private String commissionTypeCode;

    @Column(name = "commission_type_desc", nullable = false, length = 255)
    private String commissionTypeDesc;

    @Column(name = "flat_commission_pct", precision = 12, scale = 6)
    private BigDecimal flatCommissionPct;

    @Column(name = "cash_loss_limit", precision = 20, scale = 4)
    private BigDecimal cashLossLimit;
}
