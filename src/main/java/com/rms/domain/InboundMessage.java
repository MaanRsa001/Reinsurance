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

/** Generated from table `inbound_message`. */
@Entity
@Table(name = "inbound_message")
@Getter
@Setter
@NoArgsConstructor
public class InboundMessage extends BaseEntity {

    @Column(name = "message_id", nullable = false, length = 60)
    private String messageId;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "message_type_id", nullable = false)
    private Long messageTypeId;

    @Column(name = "message_type_code", nullable = false, length = 50)
    private String messageTypeCode;

    @Column(name = "message_type_desc", nullable = false, length = 255)
    private String messageTypeDesc;

    @Column(name = "source_system", nullable = false, length = 30)
    private String sourceSystem;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload_json")
    private JsonNode payloadJson;

    @Column(name = "status_id", nullable = false)
    private Long statusId;

    @Column(name = "status_desc", nullable = false, length = 100)
    private String statusDesc;

    @Column(name = "attempts", nullable = false)
    private Integer attempts;

    @Column(name = "error_text", length = 1000)
    private String errorText;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;
}
