package com.rms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Generated from table `treaty_stoploss`. */
@Entity
@Table(name = "treaty_stoploss")
@Getter
@Setter
@NoArgsConstructor
public class TreatyStoploss extends BaseEntity {

    /** Owning treaty (filled by the service; the terms belong to one mapping row, see treatyCoverMapId). */
    @Column(name = "treaty_id", nullable = false)
    private Long treatyId;

    /** Mapping row these terms belong to (managed by TreatyCoverMap). */
    @Column(name = "treaty_cover_map_id", insertable = false, updatable = false)
    private Long treatyCoverMapId;

    @Column(name = "attachment_lr_pct", nullable = false, precision = 12, scale = 6)
    private BigDecimal attachmentLrPct;

    @Column(name = "exit_lr_pct", nullable = false, precision = 12, scale = 6)
    private BigDecimal exitLrPct;

    @Column(name = "limit_pct_of_premium", nullable = false, precision = 12, scale = 6)
    private BigDecimal limitPctOfPremium;

    @Column(name = "premium_rate_pct", nullable = false, precision = 12, scale = 6)
    private BigDecimal premiumRatePct;

    @Column(name = "premium_basis_desc", nullable = false, length = 100)
    private String premiumBasisDesc;
}
