package com.rms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Generated from table `ifrs_paa_movement`. */
@Entity
@Table(name = "ifrs_paa_movement")
@Getter
@Setter
@NoArgsConstructor
public class IfrsPaaMovement extends BaseEntity {

    @Column(name = "run_id", nullable = false)
    private Long runId;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "group_code", nullable = false, length = 50)
    private String groupCode;

    @Column(name = "opening_arc", nullable = false, precision = 20, scale = 4)
    private BigDecimal openingArc;

    @Column(name = "premiums_ceded_paid", nullable = false, precision = 20, scale = 4)
    private BigDecimal premiumsCededPaid;

    @Column(name = "allocated_to_pl", nullable = false, precision = 20, scale = 4)
    private BigDecimal allocatedToPl;

    @Column(name = "closing_arc", nullable = false, precision = 20, scale = 4)
    private BigDecimal closingArc;
}
