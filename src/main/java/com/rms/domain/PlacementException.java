package com.rms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Generated from table `placement_exception`. */
@Entity
@Table(name = "placement_exception")
@Getter
@Setter
@NoArgsConstructor
public class PlacementException extends BaseEntity {

    @Column(name = "inbound_message_id", nullable = false)
    private Long inboundMessageId;

    @Column(name = "policy_txn_id")
    private Long policyTxnId;

    @Column(name = "exception_type_id", nullable = false)
    private Long exceptionTypeId;

    @Column(name = "exception_type_desc", nullable = false, length = 100)
    private String exceptionTypeDesc;

    @Column(name = "exception_status_id", nullable = false)
    private Long exceptionStatusId;

    @Column(name = "exception_status_desc", nullable = false, length = 100)
    private String exceptionStatusDesc;

    @Column(name = "details", nullable = false, length = 1000)
    private String details;

    @Column(name = "excess_amt", precision = 20, scale = 4)
    private BigDecimal excessAmt;

    @Column(name = "resolved_by", length = 50)
    private String resolvedBy;

    @Column(name = "resolved_remarks", length = 500)
    private String resolvedRemarks;
}
