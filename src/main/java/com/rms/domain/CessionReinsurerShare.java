package com.rms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Generated from table `cession_reinsurer_share`. */
@Entity
@Table(name = "cession_reinsurer_share")
@Getter
@Setter
@NoArgsConstructor
public class CessionReinsurerShare extends BaseEntity {

    @Column(name = "cession_layer_id", nullable = false)
    private Long cessionLayerId;

    @Column(name = "reinsurer_id", nullable = false)
    private Long reinsurerId;

    @Column(name = "reinsurer_code", nullable = false, length = 50)
    private String reinsurerCode;

    @Column(name = "reinsurer_desc", nullable = false, length = 255)
    private String reinsurerDesc;

    @Column(name = "share_pct", nullable = false, precision = 12, scale = 6)
    private BigDecimal sharePct;

    @Column(name = "ceded_si", nullable = false, precision = 20, scale = 4)
    private BigDecimal cededSi;

    @Column(name = "ceded_premium", nullable = false, precision = 20, scale = 4)
    private BigDecimal cededPremium;

    @Column(name = "ceded_commission", nullable = false, precision = 20, scale = 4)
    private BigDecimal cededCommission;
}
