package com.rms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Generated from table `reinsurer`. */
@Entity
@Table(name = "reinsurer")
@Getter
@Setter
@NoArgsConstructor
public class Reinsurer extends BaseEntity {

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "country", nullable = false, length = 100)
    private String country;

    @Column(name = "rating", nullable = false, length = 10)
    private String rating;

    @Column(name = "rating_agency", nullable = false, length = 50)
    private String ratingAgency;

    @Column(name = "active_flag", nullable = false)
    private Boolean activeFlag;
}
