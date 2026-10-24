package com.rms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Generated from table `xol_layer_recovery`. */
@Entity
@Table(name = "xol_layer_recovery")
@Getter
@Setter
@NoArgsConstructor
public class XolLayerRecovery extends BaseEntity {

    @Column(name = "xol_event_id", nullable = false)
    private Long xolEventId;

    @Column(name = "xol_layer_id", nullable = false)
    private Long xolLayerId;

    @Column(name = "layer_desc", nullable = false, length = 100)
    private String layerDesc;

    @Column(name = "deductible_amt", nullable = false, precision = 20, scale = 4)
    private BigDecimal deductibleAmt;

    @Column(name = "limit_amt", nullable = false, precision = 20, scale = 4)
    private BigDecimal limitAmt;

    @Column(name = "loss_to_layer", nullable = false, precision = 20, scale = 4)
    private BigDecimal lossToLayer;

    @Column(name = "recovery_amt", nullable = false, precision = 20, scale = 4)
    private BigDecimal recoveryAmt;

    @Column(name = "aggregate_limit_remaining", nullable = false, precision = 20, scale = 4)
    private BigDecimal aggregateLimitRemaining;

    @Column(name = "reinstatement_premium", nullable = false, precision = 20, scale = 4)
    private BigDecimal reinstatementPremium;
}
