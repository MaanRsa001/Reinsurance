package com.rms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Generated from table `ifrs_period_run`. */
@Entity
@Table(name = "ifrs_period_run")
@Getter
@Setter
@NoArgsConstructor
public class IfrsPeriodRun extends BaseEntity {

    @Column(name = "run_code", nullable = false, length = 50)
    private String runCode;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "assumption_set_id", nullable = false)
    private Long assumptionSetId;

    @Column(name = "status_id", nullable = false)
    private Long statusId;

    @Column(name = "status_desc", nullable = false, length = 100)
    private String statusDesc;

    @Column(name = "run_by", nullable = false, length = 50)
    private String runBy;

    @Column(name = "run_at", nullable = false)
    private LocalDateTime runAt;
}
