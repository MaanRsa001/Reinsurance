package com.rms.distribution;

import com.fasterxml.jackson.databind.JsonNode;
import com.rms.common.ApiException;
import com.rms.common.CurrentUser;
import com.rms.common.Snap;
import com.rms.domain.CessionLayer;
import com.rms.domain.CessionReinsurerShare;
import com.rms.domain.ClaimRecovery;
import com.rms.domain.ClaimRecoveryReinsurer;
import com.rms.domain.DistributionAdjustment;
import com.rms.domain.McRequest;
import com.rms.makerchecker.ApprovalHandler;
import com.rms.master.LookupService;
import com.rms.master.MasterSnapshotService;
import com.rms.repository.CessionLayerRepository;
import com.rms.repository.CessionReinsurerShareRepository;
import com.rms.repository.ClaimRecoveryRepository;
import com.rms.repository.ClaimRecoveryReinsurerRepository;
import com.rms.repository.DistributionAdjustmentRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Component;

/** Applies an approved share modification. Amounts are recomputed from the stored layer / recovery totals, never taken from the payload. */
@Component
public class DistributionApprovalHandler implements ApprovalHandler {
    private final CessionLayerRepository layers;
    private final CessionReinsurerShareRepository policyShares;
    private final ClaimRecoveryRepository recoveries;
    private final ClaimRecoveryReinsurerRepository claimShares;
    private final DistributionAdjustmentRepository audit;
    private final MasterSnapshotService masters;
    private final LookupService lookups;

    public DistributionApprovalHandler(CessionLayerRepository layers, CessionReinsurerShareRepository policyShares, ClaimRecoveryRepository recoveries,
                                       ClaimRecoveryReinsurerRepository claimShares, DistributionAdjustmentRepository audit, MasterSnapshotService masters,
                                       LookupService lookups) {
        this.layers = layers;
        this.policyShares = policyShares;
        this.recoveries = recoveries;
        this.claimShares = claimShares;
        this.audit = audit;
        this.masters = masters;
        this.lookups = lookups;
    }

    @Override
    public String entityTypeCode() { return "POLICY_DISTRIBUTION"; }

    @Override
    public Collection<String> entityTypeCodes() { return List.of("POLICY_DISTRIBUTION", "CLAIM_DISTRIBUTION"); }

    @Override
    public void onApproved(McRequest r) {
        JsonNode p = r.getPayloadAfter();
        if (p == null || !p.has("shares")) throw ApiException.badRequest("PAYLOAD_INVALID", "Approval payload has no shares");
        String scope = p.get("scope").asText();
        long targetId = p.get("targetId").asLong();
        List<Long> ids = new ArrayList<>();
        List<BigDecimal> pcts = new ArrayList<>();
        for (JsonNode s : p.get("shares")) { ids.add(s.get("reinsurerId").asLong()); pcts.add(new BigDecimal(s.get("sharePct").asText())); }

        DistributionAdjustment log = new DistributionAdjustment();
        log.setMcRequestId(r.getId());
        Snap sc = lookups.snap("ADJ_SCOPE", scope);
        log.setScopeId(sc.id()); log.setScopeDesc(sc.desc());
        log.setTargetRef(r.getEntityDesc());
        log.setReason(p.path("reason").asText(""));
        log.setBeforeJson(r.getPayloadBefore());
        log.setAfterJson(p);
        log.setAppliedAt(LocalDateTime.now());
        log.setAppliedBy(CurrentUser.username());

        if (DistributionAdjustmentService.POLICY.equals(scope)) {
            CessionLayer l = layers.findById(targetId).orElseThrow(() -> ApiException.notFound("LAYER_NOT_FOUND", "Cession layer " + targetId + " not found"));
            policyShares.deleteAll(policyShares.findByCessionLayerId(l.getId()));
            List<BigDecimal> si = ShareMath.split(l.getCededSi(), pcts), pr = ShareMath.split(l.getCededPremium(), pcts), cm = ShareMath.split(l.getCededCommission(), pcts);
            for (int i = 0; i < ids.size(); i++) {
                Snap ri = masters.reinsurer(ids.get(i));
                CessionReinsurerShare e = new CessionReinsurerShare();
                e.setCessionLayerId(l.getId());
                e.setReinsurerId(ri.id()); e.setReinsurerCode(ri.code()); e.setReinsurerDesc(ri.desc());
                e.setSharePct(pcts.get(i)); e.setCededSi(si.get(i)); e.setCededPremium(pr.get(i)); e.setCededCommission(cm.get(i));
                policyShares.save(e);
            }
            log.setCessionLayerId(l.getId());
        } else {
            ClaimRecovery rec = recoveries.findById(targetId).orElseThrow(() -> ApiException.notFound("RECOVERY_NOT_FOUND", "Claim recovery " + targetId + " not found"));
            claimShares.deleteAll(claimShares.findByClaimRecoveryId(rec.getId()));
            List<BigDecimal> paid = ShareMath.split(rec.getPaidRecoverableDelta(), pcts), os = ShareMath.split(rec.getOsRecoverableDelta(), pcts);
            for (int i = 0; i < ids.size(); i++) {
                Snap ri = masters.reinsurer(ids.get(i));
                ClaimRecoveryReinsurer e = new ClaimRecoveryReinsurer();
                e.setClaimRecoveryId(rec.getId());
                e.setReinsurerId(ri.id()); e.setReinsurerCode(ri.code()); e.setReinsurerDesc(ri.desc());
                e.setSharePct(pcts.get(i)); e.setPaidRecoverable(paid.get(i)); e.setOsRecoverable(os.get(i));
                e.setStatusId(rec.getStatusId()); e.setStatusDesc(rec.getStatusDesc());
                e.setRemarks("Share modified via " + r.getRequestNo());
                claimShares.save(e);
            }
            log.setClaimRecoveryId(rec.getId());
        }
        audit.save(log);
    }

    @Override
    public void onRejected(McRequest r, String remarks) { /* nothing was changed */ }

    @Override
    public void onReturned(McRequest r, String remarks) { /* nothing was changed */ }
}
