package com.rms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Generated from table `treaty_xol_reinstatement`. */
@Entity
@Table(name = "treaty_xol_reinstatement")
@Getter
@Setter
@NoArgsConstructor
public class TreatyXolReinstatement extends BaseEntity {

    @Column(name = "xol_layer_id", nullable = false, insertable = false, updatable = false)
    private Long xolLayerId;

    @Column(name = "reinstatement_no", nullable = false)
    private Integer reinstatementNo;

    @Column(name = "premium_pct", nullable = false, precision = 12, scale = 6)
    private BigDecimal premiumPct;

    @Column(name = "reinstatement_basis_id", nullable = false)
    private Long reinstatementBasisId;

    @Column(name = "reinstatement_basis_desc", nullable = false, length = 100)
    private String reinstatementBasisDesc;
}
