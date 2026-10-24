package com.rms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Generated from table `ifrs_journal`. */
@Entity
@Table(name = "ifrs_journal")
@Getter
@Setter
@NoArgsConstructor
public class IfrsJournal extends BaseEntity {

    @Column(name = "run_id", nullable = false)
    private Long runId;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "journal_ref", nullable = false, length = 40)
    private String journalRef;

    @Column(name = "line_no", nullable = false)
    private Integer lineNo;

    @Column(name = "gl_account_code", nullable = false, length = 30)
    private String glAccountCode;

    @Column(name = "gl_account_desc", nullable = false, length = 255)
    private String glAccountDesc;

    @Column(name = "debit_amt", nullable = false, precision = 20, scale = 4)
    private BigDecimal debitAmt;

    @Column(name = "credit_amt", nullable = false, precision = 20, scale = 4)
    private BigDecimal creditAmt;

    @Column(name = "narration", nullable = false, length = 255)
    private String narration;

    @Column(name = "outbox_status_id", nullable = false)
    private Long outboxStatusId;

    @Column(name = "outbox_status_desc", nullable = false, length = 100)
    private String outboxStatusDesc;
}
