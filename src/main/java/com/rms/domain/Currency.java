package com.rms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Generated from table `currency`. */
@Entity
@Table(name = "currency")
@Getter
@Setter
@NoArgsConstructor
public class Currency extends BaseEntity {

    @Column(name = "code", nullable = false, length = 10)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "decimals", nullable = false)
    private Integer decimals;
}
