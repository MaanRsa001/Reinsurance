package com.rms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Generated from table `cession`. */
@Entity
@Table(name = "cession")
@Getter
@Setter
@NoArgsConstructor
public class Cession extends BaseEntity {

    @Column(name = "policy_txn_id", nullable = false)
    private Long policyTxnId;

    @Column(name = "policy_no", nullable = false, length = 50)
    private String policyNo;

    @Column(name = "endorsement_no", length = 50)
    private String endorsementNo;

    @Column(name = "txn_type_id", nullable = false)
    private Long txnTypeId;

    @Column(name = "txn_type_desc", nullable = false, length = 100)
    private String txnTypeDesc;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_code", nullable = false, length = 50)
    private String productCode;

    @Column(name = "product_desc", nullable = false, length = 255)
    private String productDesc;

    @Column(name = "section_id", nullable = false)
    private Long sectionId;

    @Column(name = "section_code", nullable = false, length = 50)
    private String sectionCode;

    @Column(name = "section_desc", nullable = false, length = 255)
    private String sectionDesc;

    @Column(name = "cover_id", nullable = false)
    private Long coverId;

    @Column(name = "cover_code", nullable = false, length = 50)
    private String coverCode;

    @Column(name = "cover_desc", nullable = false, length = 255)
    private String coverDesc;

    @Column(name = "treaty_id", nullable = false)
    private Long treatyId;

    @Column(name = "treaty_code", nullable = false, length = 50)
    private String treatyCode;

    @Column(name = "treaty_desc", nullable = false, length = 255)
    private String treatyDesc;

    @Column(name = "treaty_type_id", nullable = false)
    private Long treatyTypeId;

    @Column(name = "treaty_type_code", nullable = false, length = 50)
    private String treatyTypeCode;

    @Column(name = "treaty_type_desc", nullable = false, length = 255)
    private String treatyTypeDesc;

    @Column(name = "treaty_version_no", nullable = false)
    private Integer treatyVersionNo;

    /** Mapping row (and so the per-cover terms) the cession was calculated with; null for cessions created before per-cover terms. */
//    @Column(name = "treaty_cover_map_id")
//    private Long treatyCoverMapId;

    @Column(name = "cession_basis_id", nullable = false)
    private Long cessionBasisId;

    @Column(name = "cession_basis_code", nullable = false, length = 50)
    private String cessionBasisCode;

    @Column(name = "cession_basis_desc", nullable = false, length = 255)
    private String cessionBasisDesc;

    @Column(name = "currency_id", nullable = false)
    private Long currencyId;

    @Column(name = "currency_code", nullable = false, length = 50)
    private String currencyCode;

    @Column(name = "currency_desc", nullable = false, length = 255)
    private String currencyDesc;

    @Column(name = "basis_si", nullable = false, precision = 20, scale = 4)
    private BigDecimal basisSi;

    @Column(name = "retained_si", nullable = false, precision = 20, scale = 4)
    private BigDecimal retainedSi;

    @Column(name = "ceded_si", nullable = false, precision = 20, scale = 4)
    private BigDecimal cededSi;

    @Column(name = "excess_si", nullable = false, precision = 20, scale = 4)
    private BigDecimal excessSi;

    @Column(name = "gross_premium", nullable = false, precision = 20, scale = 4)
    private BigDecimal grossPremium;

    @Column(name = "retained_premium", nullable = false, precision = 20, scale = 4)
    private BigDecimal retainedPremium;

    @Column(name = "ceded_premium", nullable = false, precision = 20, scale = 4)
    private BigDecimal cededPremium;

    @Column(name = "excess_premium", nullable = false, precision = 20, scale = 4)
    private BigDecimal excessPremium;

    @Column(name = "ceded_commission", nullable = false, precision = 20, scale = 4)
    private BigDecimal cededCommission;

    @Column(name = "remarks", length = 500)
    private String remarks;
}
