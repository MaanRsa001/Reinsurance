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

/** Generated from table `mc_request`. */
@Entity
@Table(name = "mc_request")
@Getter
@Setter
@NoArgsConstructor
public class McRequest extends BaseEntity {

    @Column(name = "request_no", nullable = false, length = 30)
    private String requestNo;

    @Column(name = "entity_type_id", nullable = false)
    private Long entityTypeId;

    @Column(name = "entity_type_desc", nullable = false, length = 100)
    private String entityTypeDesc;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "entity_desc", nullable = false, length = 255)
    private String entityDesc;

    @Column(name = "action_id", nullable = false)
    private Long actionId;

    @Column(name = "action_desc", nullable = false, length = 100)
    private String actionDesc;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload_before")
    private JsonNode payloadBefore;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload_after")
    private JsonNode payloadAfter;

    @Column(name = "status_id", nullable = false)
    private Long statusId;

    @Column(name = "status_desc", nullable = false, length = 100)
    private String statusDesc;

    @Column(name = "maker_user", nullable = false, length = 50)
    private String makerUser;

    @Column(name = "maker_at", nullable = false)
    private LocalDateTime makerAt;

    @Column(name = "maker_remarks", length = 500)
    private String makerRemarks;

    @Column(name = "current_level", nullable = false)
    private Integer currentLevel;

    @Column(name = "levels_required", nullable = false)
    private Integer levelsRequired;
}
