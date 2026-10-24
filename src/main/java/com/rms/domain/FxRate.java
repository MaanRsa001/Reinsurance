package com.rms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Generated from table `fx_rate`. */
@Entity
@Table(name = "fx_rate")
@Getter
@Setter
@NoArgsConstructor
public class FxRate extends BaseEntity {

    @Column(name = "from_currency_id", nullable = false)
    private Long fromCurrencyId;

    @Column(name = "to_currency_id", nullable = false)
    private Long toCurrencyId;

    @Column(name = "rate_date", nullable = false)
    private LocalDate rateDate;

    @Column(name = "rate", nullable = false, precision = 18, scale = 8)
    private BigDecimal rate;
}
