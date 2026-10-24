package com.rms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Generated from table `fac_out_quote`. */
@Entity
@Table(name = "fac_out_quote")
@Getter
@Setter
@NoArgsConstructor
public class FacOutQuote extends BaseEntity {

    @Column(name = "placement_id", nullable = false)
    private Long placementId;

    @Column(name = "reinsurer_id", nullable = false)
    private Long reinsurerId;

    @Column(name = "reinsurer_code", nullable = false, length = 50)
    private String reinsurerCode;

    @Column(name = "reinsurer_desc", nullable = false, length = 255)
    private String reinsurerDesc;

    @Column(name = "offered_share_pct", nullable = false, precision = 12, scale = 6)
    private BigDecimal offeredSharePct;

    @Column(name = "commission_pct", nullable = false, precision = 12, scale = 6)
    private BigDecimal commissionPct;

    @Column(name = "quote_status_id", nullable = false)
    private Long quoteStatusId;

    @Column(name = "quote_status_desc", nullable = false, length = 100)
    private String quoteStatusDesc;

    @Column(name = "remarks", length = 500)
    private String remarks;
}
