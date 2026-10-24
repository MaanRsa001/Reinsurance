package com.rms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Generated from table `distribution_adjustment`. */
@Entity
@Table(name = "distribution_adjustment")
@Getter
@Setter
@NoArgsConstructor
public class DistributionAdjustment extends BaseEntity {

    @Column(name = "mc_request_id", nullable = false)
    private Long mcRequestId;

    @Column(name = "scope_id", nullable = false)
    private Long scopeId;

    @Column(name = "scope_desc", nullable = false, length = 100)
    private String scopeDesc;

    @Column(name = "cession_layer_id")
    private Long cessionLayerId;

    @Column(name = "claim_recovery_id")
    private Long claimRecoveryId;

    @Column(name = "target_ref", nullable = false, length = 100)
    private String targetRef;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_json")
    private JsonNode beforeJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_json")
    private JsonNode afterJson;

    @Column(name = "applied_at", nullable = false)
    private LocalDateTime appliedAt;

    @Column(name = "applied_by", nullable = false, length = 50)
    private String appliedBy;
}
