package com.rms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Generated from table `treaty_panel`. */
@Entity
@Table(name = "treaty_panel")
@Getter
@Setter
@NoArgsConstructor
public class TreatyPanel extends BaseEntity {

    @Column(name = "treaty_id", nullable = false, insertable = false, updatable = false)
    private Long treatyId;

    /** Which part of the treaty this panel row serves: the treaty type code of the mapping rows (QS, SUR, XOL-CAT, ...); with layerNo for surplus / XOL layers. */
    @Column(name = "panel_group", nullable = false, length = 50)
    private String panelGroup;

    @Column(name = "layer_no")
    private Integer layerNo;

    /** Legacy link (before V3); new rows use panelGroup + layerNo. */
    @Column(name = "surplus_layer_id")
    private Long surplusLayerId;

    @Column(name = "xol_layer_id")
    private Long xolLayerId;

    @Column(name = "reinsurer_id", nullable = false)
    private Long reinsurerId;

    @Column(name = "reinsurer_code", nullable = false, length = 50)
    private String reinsurerCode;

    @Column(name = "reinsurer_desc", nullable = false, length = 255)
    private String reinsurerDesc;

    @Column(name = "share_pct", nullable = false, precision = 12, scale = 6)
    private BigDecimal sharePct;

    @Column(name = "brokerage_pct", nullable = false, precision = 12, scale = 6)
    private BigDecimal brokeragePct;

    @Column(name = "lead_flag", nullable = false)
    private Boolean leadFlag;
}
