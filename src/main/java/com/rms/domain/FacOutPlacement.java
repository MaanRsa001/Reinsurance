package com.rms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Generated from table `fac_out_placement`. */
@Entity
@Table(name = "fac_out_placement")
@Getter
@Setter
@NoArgsConstructor
public class FacOutPlacement extends BaseEntity {

    @Column(name = "policy_txn_id", nullable = false)
    private Long policyTxnId;

    @Column(name = "policy_no", nullable = false, length = 50)
    private String policyNo;

    @Column(name = "cover_id", nullable = false)
    private Long coverId;

    @Column(name = "cover_code", nullable = false, length = 50)
    private String coverCode;

    @Column(name = "cover_desc", nullable = false, length = 255)
    private String coverDesc;

    @Column(name = "fac_ref", nullable = false, length = 50)
    private String facRef;

    @Column(name = "fac_mode_id", nullable = false)
    private Long facModeId;

    @Column(name = "fac_mode_desc", nullable = false, length = 100)
    private String facModeDesc;

    @Column(name = "requested_si", nullable = false, precision = 20, scale = 4)
    private BigDecimal requestedSi;

    @Column(name = "placed_si", nullable = false, precision = 20, scale = 4)
    private BigDecimal placedSi;

    @Column(name = "premium", nullable = false, precision = 20, scale = 4)
    private BigDecimal premium;

    @Column(name = "commission_pct", nullable = false, precision = 12, scale = 6)
    private BigDecimal commissionPct;

    @Column(name = "brokerage_pct", nullable = false, precision = 12, scale = 6)
    private BigDecimal brokeragePct;

    @Column(name = "status_id", nullable = false)
    private Long statusId;

    @Column(name = "status_desc", nullable = false, length = 100)
    private String statusDesc;

    @Column(name = "placement_exception_id")
    private Long placementExceptionId;
}
