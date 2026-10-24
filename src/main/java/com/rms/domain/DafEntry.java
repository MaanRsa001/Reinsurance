package com.rms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Generated from table `daf_entry`. */
@Entity
@Table(name = "daf_entry")
@Getter
@Setter
@NoArgsConstructor
public class DafEntry extends BaseEntity {

    @Column(name = "daf_account_id", nullable = false)
    private Long dafAccountId;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(name = "source_year", nullable = false)
    private Integer sourceYear;

    @Column(name = "entry_type_id", nullable = false)
    private Long entryTypeId;

    @Column(name = "entry_type_desc", nullable = false, length = 100)
    private String entryTypeDesc;

    @Column(name = "amount_signed", nullable = false, precision = 20, scale = 4)
    private BigDecimal amountSigned;

    @Column(name = "balance_after", nullable = false, precision = 20, scale = 4)
    private BigDecimal balanceAfter;

    @Column(name = "pc_computation_id")
    private Long pcComputationId;

    @Column(name = "remarks", length = 500)
    private String remarks;
}
