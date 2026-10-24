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
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Generated from table `treaty_xol_layer`. */
@Entity
@Table(name = "treaty_xol_layer")
@Getter
@Setter
@NoArgsConstructor
public class TreatyXolLayer extends BaseEntity {

    /** Owning treaty (filled by the service; the terms belong to one mapping row, see treatyCoverMapId). */
    @Column(name = "treaty_id", nullable = false)
    private Long treatyId;

    /** Mapping row these terms belong to (managed by TreatyCoverMap). */
    @Column(name = "treaty_cover_map_id", insertable = false, updatable = false)
    private Long treatyCoverMapId;

    @Column(name = "layer_no", nullable = false)
    private Integer layerNo;

    @Column(name = "layer_name", nullable = false, length = 100)
    private String layerName;

    @Column(name = "limit_amt", nullable = false, precision = 20, scale = 4)
    private BigDecimal limitAmt;

    @Column(name = "deductible_amt", nullable = false, precision = 20, scale = 4)
    private BigDecimal deductibleAmt;

    @Column(name = "rate_on_line_pct", nullable = false, precision = 12, scale = 6)
    private BigDecimal rateOnLinePct;

    @Column(name = "deposit_premium", nullable = false, precision = 20, scale = 4)
    private BigDecimal depositPremium;

    @Column(name = "min_premium", nullable = false, precision = 20, scale = 4)
    private BigDecimal minPremium;

    @Column(name = "event_hours_clause")
    private Integer eventHoursClause;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("reinstatementNo ASC")
    @JoinColumn(name = "xol_layer_id", nullable = false)
    private List<TreatyXolReinstatement> reinstatements = new ArrayList<>();
}
