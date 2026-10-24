package com.rms.makerchecker;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import java.util.List;

public final class McDtos {
    private McDtos() { }

    public record StepView(Integer levelNo, String checkerRole, String checkerUser, String decision, LocalDateTime decidedAt, String remarks) { }

    public record McRequestView(Long id, String requestNo, String entityType, Long entityId, String entityDesc, String action, String status, String statusCode,
                                String makerUser, LocalDateTime makerAt, String makerRemarks, Integer currentLevel, Integer levelsRequired,
                                JsonNode payloadBefore, JsonNode payloadAfter, List<StepView> steps, boolean canDecide) { }

    public record DecisionRequest(String remarks) { }
}
