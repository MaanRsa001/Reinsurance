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

/** Generated from table `treaty_cover_map`. */
@Entity
@Table(name = "treaty_cover_map")
@Getter
@Setter
@NoArgsConstructor
public class TreatyCoverMap extends BaseEntity {

    @Column(name = "treaty_id", nullable = false, insertable = false, updatable = false)
    private Long treatyId;

    @Column(name = "treaty_code", nullable = false, length = 50)
    private String treatyCode;

    /** Treaty type of THIS row (a treaty can carry several types against the same product). */
    @Column(name = "treaty_type_id", nullable = false)
    private Long treatyTypeId;

    @Column(name = "treaty_type_code", nullable = false, length = 50)
    private String treatyTypeCode;

    @Column(name = "treaty_type_desc", nullable = false, length = 255)
    private String treatyTypeDesc;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "map_level_id", nullable = false)
    private Long mapLevelId;

    @Column(name = "map_level_desc", nullable = false, length = 100)
    private String mapLevelDesc;

    @Column(name = "lob_id", nullable = false)
    private Long lobId;

    @Column(name = "lob_code", nullable = false, length = 50)
    private String lobCode;

    @Column(name = "lob_desc", nullable = false, length = 255)
    private String lobDesc;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "product_code", length = 50)
    private String productCode;

    @Column(name = "product_desc", length = 255)
    private String productDesc;

    @Column(name = "section_id")
    private Long sectionId;

    @Column(name = "section_code", length = 50)
    private String sectionCode;

    @Column(name = "section_desc", length = 255)
    private String sectionDesc;

    @Column(name = "cover_id")
    private Long coverId;

    @Column(name = "cover_code", length = 50)
    private String coverCode;

    @Column(name = "cover_desc", length = 255)
    private String coverDesc;

    @Column(name = "sequence_no", nullable = false)
    private Integer sequenceNo;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to", nullable = false)
    private LocalDate effectiveTo;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("ruleNo ASC")
    @JoinColumn(name = "treaty_cover_map_id", nullable = false)
    private List<TreatyCoverAllocationRule> rules = new ArrayList<>();

    // ---- terms of this row, by type: quota share / fac obligatory -> qsParams; surplus -> retentions + surplusLayers; XOL -> xolLayers; stop loss -> stopLosses
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "treaty_cover_map_id", nullable = false)
    private List<TreatyParamQs> qsParams = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "treaty_cover_map_id", nullable = false)
    private List<TreatySurplusRetention> retentions = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("layerNo ASC")
    @JoinColumn(name = "treaty_cover_map_id", nullable = false)
    private List<TreatySurplusLayer> surplusLayers = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("layerNo ASC")
    @JoinColumn(name = "treaty_cover_map_id", nullable = false)
    private List<TreatyXolLayer> xolLayers = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "treaty_cover_map_id", nullable = false)
    private List<TreatyStoploss> stopLosses = new ArrayList<>();
}
