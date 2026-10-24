package com.rms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Generated from table `mc_approval_step`. */
@Entity
@Table(name = "mc_approval_step")
@Getter
@Setter
@NoArgsConstructor
public class McApprovalStep extends BaseEntity {

    @Column(name = "request_id", nullable = false)
    private Long requestId;

    @Column(name = "level_no", nullable = false)
    private Integer levelNo;

    @Column(name = "checker_role_desc", nullable = false, length = 100)
    private String checkerRoleDesc;

    @Column(name = "checker_user", nullable = false, length = 50)
    private String checkerUser;

    @Column(name = "decision_id", nullable = false)
    private Long decisionId;

    @Column(name = "decision_desc", nullable = false, length = 100)
    private String decisionDesc;

    @Column(name = "decided_at", nullable = false)
    private LocalDateTime decidedAt;

    @Column(name = "remarks", length = 500)
    private String remarks;
}
