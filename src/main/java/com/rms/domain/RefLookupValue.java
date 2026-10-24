package com.rms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Generated from table `ref_lookup_value`. */
@Entity
@Table(name = "ref_lookup_value")
@Getter
@Setter
@NoArgsConstructor
public class RefLookupValue extends BaseEntity {

    @Column(name = "type_id", nullable = false)
    private Long typeId;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "description", nullable = false, length = 255)
    private String description;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "active_flag", nullable = false)
    private Boolean activeFlag;
}
