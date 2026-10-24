package com.rms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.CascadeType;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Generated from table `treaty`. */
@Entity
@Table(name = "treaty")
@Getter
@Setter
@NoArgsConstructor
public class Treaty extends BaseEntity {

    @Column(name = "treaty_code", nullable = false, length = 50)
    private String treatyCode;

    @Column(name = "treaty_name", nullable = false, length = 255)
    private String treatyName;

    @Column(name = "version_no", nullable = false)
    private Integer versionNo;

    @Column(name = "treaty_type_id")
    private Long treatyTypeId;

    @Column(name = "treaty_type_code", length = 50)
    private String treatyTypeCode;

    @Column(name = "treaty_type_desc", length = 255)
    private String treatyTypeDesc;

    @Column(name = "treaty_basis_id", nullable = false)
    private Long treatyBasisId;

    @Column(name = "treaty_basis_code", nullable = false, length = 50)
    private String treatyBasisCode;

    @Column(name = "treaty_basis_desc", nullable = false, length = 255)
    private String treatyBasisDesc;

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

    @Column(name = "lob_id", nullable = false)
    private Long lobId;

    @Column(name = "lob_code", nullable = false, length = 50)
    private String lobCode;

    @Column(name = "lob_desc", nullable = false, length = 255)
    private String lobDesc;

    @Column(name = "broker_id", nullable = false)
    private Long brokerId;

    @Column(name = "broker_code", nullable = false, length = 50)
    private String brokerCode;

    @Column(name = "broker_desc", nullable = false, length = 255)
    private String brokerDesc;

    @Column(name = "inception_date", nullable = false)
    private LocalDate inceptionDate;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "supersedes_treaty_id")
    private Long supersedesTreatyId;

    @Column(name = "amend_mode_id")
    private Long amendModeId;

    @Column(name = "amend_mode_desc", length = 100)
    private String amendModeDesc;

    @Column(name = "status_id", nullable = false)
    private Long statusId;

    @Column(name = "status_desc", nullable = false, length = 100)
    private String statusDesc;

    @Column(name = "remarks", length = 500)
    private String remarks;






    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "treaty_id", nullable = false)
    private List<ScScale> scScales = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "treaty_id", nullable = false)
    private List<PcConfig> pcConfigs = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "treaty_id", nullable = false)
    private List<TreatyPanel> panel = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "treaty_id", nullable = false)
    private List<TreatyExclusion> exclusions = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "treaty_id", nullable = false)
    private List<TreatyCoverMap> coverMaps = new ArrayList<>();
}
