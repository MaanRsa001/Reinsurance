package com.rms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Generated from table `treaty_type`. */
@Entity
@Table(name = "treaty_type")
@Getter
@Setter
@NoArgsConstructor
public class TreatyType extends BaseEntity {

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "description", nullable = false, length = 255)
    private String description;

    @Column(name = "calc_strategy_code", nullable = false, length = 100)
    private String calcStrategyCode;

    @Column(name = "proportional_flag", nullable = false)
    private Boolean proportionalFlag;
}
