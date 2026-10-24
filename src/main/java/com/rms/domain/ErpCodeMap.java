package com.rms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Generated from table `erp_code_map`. */
@Entity
@Table(name = "erp_code_map")
@Getter
@Setter
@NoArgsConstructor
public class ErpCodeMap extends BaseEntity {

    @Column(name = "erp_system_code", nullable = false, length = 30)
    private String erpSystemCode;

    @Column(name = "master_type_code", nullable = false, length = 30)
    private String masterTypeCode;

    @Column(name = "erp_code", nullable = false, length = 50)
    private String erpCode;

    @Column(name = "rms_master_id", nullable = false)
    private Integer rmsMasterId;

    @Column(name = "rms_master_code", nullable = false, length = 50)
    private String rmsMasterCode;

    @Column(name = "rms_master_desc", nullable = false, length = 255)
    private String rmsMasterDesc;
}
