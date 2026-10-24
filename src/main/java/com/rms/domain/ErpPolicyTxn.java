package com.rms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Generated from table `erp_policy_txn`. */
@Entity
@Table(name = "erp_policy_txn")
@Getter
@Setter
@NoArgsConstructor
public class ErpPolicyTxn extends BaseEntity {

    @Column(name = "inbound_message_id", nullable = false)
    private Long inboundMessageId;

    @Column(name = "policy_no", nullable = false, length = 50)
    private String policyNo;

    @Column(name = "endorsement_no", length = 50)
    private String endorsementNo;

    @Column(name = "txn_type_id", nullable = false)
    private Long txnTypeId;

    @Column(name = "txn_type_desc", nullable = false, length = 100)
    private String txnTypeDesc;

    @Column(name = "txn_date", nullable = false)
    private LocalDate txnDate;

    @Column(name = "inception_date", nullable = false)
    private LocalDate inceptionDate;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Column(name = "currency_id", nullable = false)
    private Long currencyId;

    @Column(name = "currency_code", nullable = false, length = 50)
    private String currencyCode;

    @Column(name = "currency_desc", nullable = false, length = 255)
    private String currencyDesc;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_code", nullable = false, length = 50)
    private String productCode;

    @Column(name = "product_desc", nullable = false, length = 255)
    private String productDesc;

    @Column(name = "insured_name", nullable = false, length = 255)
    private String insuredName;

    @Column(name = "location_desc", nullable = false, length = 255)
    private String locationDesc;

    @Column(name = "cat_zone_code", length = 30)
    private String catZoneCode;

    @Column(name = "risk_grade_id")
    private Long riskGradeId;

    @Column(name = "risk_grade_desc", length = 100)
    private String riskGradeDesc;
}
