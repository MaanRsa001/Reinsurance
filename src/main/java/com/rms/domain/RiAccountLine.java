package com.rms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Generated from table `ri_account_line`. */
@Entity
@Table(name = "ri_account_line")
@Getter
@Setter
@NoArgsConstructor
public class RiAccountLine extends BaseEntity {

    @Column(name = "ri_account_id", nullable = false)
    private Long riAccountId;

    @Column(name = "line_type_id", nullable = false)
    private Long lineTypeId;

    @Column(name = "line_type_desc", nullable = false, length = 100)
    private String lineTypeDesc;

    @Column(name = "source_ref", nullable = false, length = 100)
    private String sourceRef;

    @Column(name = "description", nullable = false, length = 255)
    private String description;

    @Column(name = "amount_signed", nullable = false, precision = 20, scale = 4)
    private BigDecimal amountSigned;

    @Column(name = "currency_code", nullable = false, length = 10)
    private String currencyCode;
}
