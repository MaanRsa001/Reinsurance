package com.rms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Generated from table `treaty_exclusion`. */
@Entity
@Table(name = "treaty_exclusion")
@Getter
@Setter
@NoArgsConstructor
public class TreatyExclusion extends BaseEntity {

    @Column(name = "treaty_id", nullable = false, insertable = false, updatable = false)
    private Long treatyId;

    @Column(name = "exclusion_desc", nullable = false, length = 255)
    private String exclusionDesc;

    @Column(name = "action_desc", nullable = false, length = 100)
    private String actionDesc;
}
