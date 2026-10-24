package com.rms.makerchecker;

import com.rms.domain.McRequest;
import java.util.Collection;
import java.util.List;

/** One handler per entity type that goes through maker-checker. Called inside the decision transaction. */
public interface ApprovalHandler {
    /** Lookup code in MC_ENTITY, e.g. TREATY. */
    String entityTypeCode();

    /** All entity types this handler serves (defaults to the single one). */
    default Collection<String> entityTypeCodes() {
        return List.of(entityTypeCode());
    }

    void onApproved(McRequest request);

    void onRejected(McRequest request, String remarks);

    void onReturned(McRequest request, String remarks);
}
