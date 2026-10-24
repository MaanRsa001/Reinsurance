package com.rms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Generated from table `fac_in_acceptance`. */
@Entity
@Table(name = "fac_in_acceptance")
@Getter
@Setter
@NoArgsConstructor
public class FacInAcceptance extends BaseEntity {

    @Column(name = "acceptance_no", nullable = false, length = 50)
    private String acceptanceNo;

    @Column(name = "cedant_id", nullable = false)
    private Long cedantId;

    @Column(name = "cedant_code", nullable = false, length = 50)
    private String cedantCode;

    @Column(name = "cedant_desc", nullable = false, length = 255)
    private String cedantDesc;

    @Column(name = "risk_desc", nullable = false, length = 255)
    private String riskDesc;

    @Column(name = "lob_id", nullable = false)
    private Long lobId;

    @Column(name = "lob_code", nullable = false, length = 50)
    private String lobCode;

    @Column(name = "lob_desc", nullable = false, length = 255)
    private String lobDesc;

    @Column(name = "currency_id", nullable = false)
    private Long currencyId;

    @Column(name = "currency_code", nullable = false, length = 50)
    private String currencyCode;

    @Column(name = "currency_desc", nullable = false, length = 255)
    private String currencyDesc;

    @Column(name = "original_si", nullable = false, precision = 20, scale = 4)
    private BigDecimal originalSi;

    @Column(name = "our_share_pct", nullable = false, precision = 12, scale = 6)
    private BigDecimal ourSharePct;

    @Column(name = "accepted_si", nullable = false, precision = 20, scale = 4)
    private BigDecimal acceptedSi;

    @Column(name = "premium_100pct", nullable = false, precision = 20, scale = 4)
    private BigDecimal premium100pct;

    @Column(name = "premium_our_share", nullable = false, precision = 20, scale = 4)
    private BigDecimal premiumOurShare;

    @Column(name = "commission_pct", nullable = false, precision = 12, scale = 6)
    private BigDecimal commissionPct;

    @Column(name = "commission_amt", nullable = false, precision = 20, scale = 4)
    private BigDecimal commissionAmt;

    @Column(name = "inception_date", nullable = false)
    private LocalDate inceptionDate;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Column(name = "status_id", nullable = false)
    private Long statusId;

    @Column(name = "status_desc", nullable = false, length = 100)
    private String statusDesc;
}
