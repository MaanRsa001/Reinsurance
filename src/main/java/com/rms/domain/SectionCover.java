package com.rms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Generated from table `section_cover`. */
@Entity
@Table(name = "section_cover")
@Getter
@Setter
@NoArgsConstructor
public class SectionCover extends BaseEntity {

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "description", nullable = false, length = 255)
    private String description;

    @Column(name = "section_id", nullable = false)
    private Long sectionId;
}
