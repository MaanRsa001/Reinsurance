package com.rms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Generated from table `product_section`. */
@Entity
@Table(name = "product_section")
@Getter
@Setter
@NoArgsConstructor
public class ProductSection extends BaseEntity {

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "description", nullable = false, length = 255)
    private String description;

    @Column(name = "product_id", nullable = false)
    private Long productId;
}
