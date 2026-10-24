package com.rms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Generated from table `stoploss_evaluation`. */
@Entity
@Table(name = "stoploss_evaluation")
@Getter
@Setter
@NoArgsConstructor
public class StoplossEvaluation extends BaseEntity {

    @Column(name = "treaty_id", nullable = false)
    private Long treatyId;

    @Column(name = "treaty_code", nullable = false, length = 50)
    private String treatyCode;

    @Column(name = "evaluation_date", nullable = false)
    private LocalDate evaluationDate;

    @Column(name = "subject_premium", nullable = false, precision = 20, scale = 4)
    private BigDecimal subjectPremium;

    @Column(name = "incurred_claims", nullable = false, precision = 20, scale = 4)
    private BigDecimal incurredClaims;

    @Column(name = "loss_ratio_pct", nullable = false, precision = 12, scale = 6)
    private BigDecimal lossRatioPct;

    @Column(name = "attachment_lr_pct", nullable = false, precision = 12, scale = 6)
    private BigDecimal attachmentLrPct;

    @Column(name = "exit_lr_pct", nullable = false, precision = 12, scale = 6)
    private BigDecimal exitLrPct;

    @Column(name = "limit_amt", nullable = false, precision = 20, scale = 4)
    private BigDecimal limitAmt;

    @Column(name = "recovery_cumulative", nullable = false, precision = 20, scale = 4)
    private BigDecimal recoveryCumulative;

    @Column(name = "previously_recognised", nullable = false, precision = 20, scale = 4)
    private BigDecimal previouslyRecognised;

    @Column(name = "incremental_recovery", nullable = false, precision = 20, scale = 4)
    private BigDecimal incrementalRecovery;

    @Column(name = "status_id", nullable = false)
    private Long statusId;

    @Column(name = "status_desc", nullable = false, length = 100)
    private String statusDesc;

    @Column(name = "mc_request_id")
    private Long mcRequestId;
}
