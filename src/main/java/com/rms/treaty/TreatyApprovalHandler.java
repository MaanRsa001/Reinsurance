package com.rms.treaty;

import com.rms.common.ApiException;
import com.rms.common.Snap;
import com.rms.domain.McRequest;
import com.rms.domain.Treaty;
import com.rms.makerchecker.ApprovalHandler;
import com.rms.master.LookupCodes;
import com.rms.master.LookupService;
import com.rms.repository.TreatyRepository;
import org.springframework.stereotype.Component;

/** Applies maker-checker outcomes to treaty versions. The live record changes only here, after the last approval level. */
@Component
public class TreatyApprovalHandler implements ApprovalHandler {
    private final TreatyRepository treaties;
    private final LookupService lookups;

    public TreatyApprovalHandler(TreatyRepository treaties, LookupService lookups) {
        this.treaties = treaties;
        this.lookups = lookups;
    }

    @Override
    public String entityTypeCode() { return LookupCodes.ENTITY_TREATY; }

    private Treaty load(McRequest r) {
        return treaties.findById(r.getEntityId()).orElseThrow(() -> ApiException.notFound("TREATY_NOT_FOUND", "Treaty " + r.getEntityId() + " not found"));
    }

    private void setStatus(Treaty t, String code) {
        Snap s = lookups.snap(LookupCodes.TREATY_STATUS, code);
        t.setStatusId(s.id());
        t.setStatusDesc(s.desc());
    }

    @Override
    public void onApproved(McRequest r) {
        Treaty t = load(r);
        setStatus(t, LookupCodes.ACTIVE);
        treaties.save(t);
        if (t.getSupersedesTreatyId() != null) {
            treaties.findById(t.getSupersedesTreatyId()).ifPresent(prev -> {
                setStatus(prev, LookupCodes.SUPERSEDED);   // history stays readable; cessions keep the version they were made under
                treaties.save(prev);
            });
        }
    }

    @Override
    public void onRejected(McRequest r, String remarks) {
        Treaty t = load(r);
        setStatus(t, LookupCodes.DRAFT);
        treaties.save(t);
    }

    @Override
    public void onReturned(McRequest r, String remarks) {
        onRejected(r, remarks);
    }
}
