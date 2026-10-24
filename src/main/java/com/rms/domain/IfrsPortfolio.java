package com.rms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Generated from table `ifrs_portfolio`. */
@Entity
@Table(name = "ifrs_portfolio")
@Getter
@Setter
@NoArgsConstructor
public class IfrsPortfolio extends BaseEntity {

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "description", nullable = false, length = 255)
    private String description;

    @Column(name = "lob_id", nullable = false)
    private Long lobId;

    @Column(name = "lob_code", nullable = false, length = 50)
    private String lobCode;

    @Column(name = "lob_desc", nullable = false, length = 255)
    private String lobDesc;
}
