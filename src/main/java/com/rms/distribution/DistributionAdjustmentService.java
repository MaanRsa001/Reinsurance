package com.rms.distribution;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rms.common.ApiException;
import com.rms.common.Snap;
import com.rms.distribution.DistributionDtos.AdjustmentRequest;
import com.rms.distribution.DistributionDtos.AdjustmentResult;
import com.rms.distribution.DistributionDtos.ShareInput;
import com.rms.domain.Cession;
import com.rms.domain.CessionLayer;
import com.rms.domain.ClaimRecovery;
import com.rms.makerchecker.MakerCheckerService;
import com.rms.master.MasterSnapshotService;
import com.rms.repository.CessionLayerRepository;
import com.rms.repository.CessionReinsurerShareRepository;
import com.rms.repository.CessionRepository;
import com.rms.repository.ClaimRecoveryRepository;
import com.rms.repository.ClaimRecoveryReinsurerRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Modification of a distribution = a proposal to re-split ONE policy cession layer or ONE claim recovery between reinsurers.
 * Layer / recovery totals are fixed by the treaty terms, only the reinsurer split changes. The proposal goes through maker-checker;
 * amounts are recomputed from the stored totals when it is approved (see DistributionApprovalHandler).
 */
@Service
public class DistributionAdjustmentService {
    public static final String POLICY = "POLICY";
    public static final String CLAIM = "CLAIM";

    private final CessionLayerRepository layers;
    private final CessionRepository cessions;
    private final CessionReinsurerShareRepository policyShares;
    private final ClaimRecoveryRepository recoveries;
    private final ClaimRecoveryReinsurerRepository claimShares;
    private final MasterSnapshotService masters;
    private final MakerCheckerService makerChecker;
    private final ObjectMapper om;
    private final int levels;

    public DistributionAdjustmentService(CessionLayerRepository layers, CessionRepository cessions, CessionReinsurerShareRepository policyShares,
                                         ClaimRecoveryRepository recoveries, ClaimRecoveryReinsurerRepository claimShares, MasterSnapshotService masters,
                                         MakerCheckerService makerChecker, ObjectMapper om, @Value("${rms.makerchecker.levels.distribution:1}") int levels) {
        this.layers = layers;
        this.cessions = cessions;
        this.policyShares = policyShares;
        this.recoveries = recoveries;
        this.claimShares = claimShares;
        this.masters = masters;
        this.makerChecker = makerChecker;
        this.om = om;
        this.levels = levels;
    }

    @Transactional
    public AdjustmentResult propose(AdjustmentRequest r) {
        if (r.reason() == null || r.reason().trim().length() < 5) throw ApiException.badRequest("REASON_REQUIRED", "Give a reason of at least 5 characters");
        List<ShareInput> in = r.shares() == null ? List.of() : r.shares();
        if (in.isEmpty()) throw ApiException.badRequest("SHARES_REQUIRED", "At least one reinsurer is required");
        BigDecimal total = BigDecimal.ZERO;
        Set<Long> seen = new HashSet<>();
        for (ShareInput s : in) {
            if (s.reinsurerId() == null || s.sharePct() == null || s.sharePct().signum() <= 0) throw ApiException.badRequest("SHARE_INVALID", "Every row needs a reinsurer and a positive share");
            if (!seen.add(s.reinsurerId())) throw ApiException.badRequest("SHARE_DUPLICATE", "A reinsurer appears twice");
            masters.reinsurer(s.reinsurerId());
            total = total.add(s.sharePct());
        }
        if (total.compareTo(BigDecimal.valueOf(100)) != 0) throw ApiException.badRequest("SHARE_TOTAL", "Shares total " + total.stripTrailingZeros().toPlainString() + "% - they must total 100%");

        List<BigDecimal> pcts = in.stream().map(ShareInput::sharePct).toList();
        ObjectNode before = om.createObjectNode();
        ArrayNode beforeShares = before.putArray("shares");
        ObjectNode after = om.createObjectNode();
        after.put("scope", r.scope());
        after.put("targetId", r.targetId());
        after.put("reason", r.reason().trim());
        ArrayNode afterShares = after.putArray("shares");
        String desc;
        String entityType;

        if (POLICY.equals(r.scope())) {
            CessionLayer l = layers.findById(r.targetId()).orElseThrow(() -> ApiException.notFound("LAYER_NOT_FOUND", "Cession layer " + r.targetId() + " not found"));
            Cession c = cessions.findById(l.getCessionId()).orElseThrow();
            policyShares.findByCessionLayerId(l.getId()).forEach(s -> beforeShares.add(om.createObjectNode().put("reinsurerId", s.getReinsurerId())
                .put("reinsurerDesc", s.getReinsurerDesc()).put("sharePct", s.getSharePct())));
            List<BigDecimal> si = ShareMath.split(l.getCededSi(), pcts), pr = ShareMath.split(l.getCededPremium(), pcts), cm = ShareMath.split(l.getCededCommission(), pcts);
            for (int i = 0; i < in.size(); i++) {
                Snap ri = masters.reinsurer(in.get(i).reinsurerId());
                afterShares.add(om.createObjectNode().put("reinsurerId", ri.id()).put("reinsurerDesc", ri.desc()).put("sharePct", in.get(i).sharePct())
                    .put("cededSi", si.get(i)).put("cededPremium", pr.get(i)).put("cededCommission", cm.get(i)));
            }
            desc = c.getPolicyNo() + " / " + c.getTreatyCode() + " / " + l.getLayerDesc();
            entityType = "POLICY_DISTRIBUTION";
        } else if (CLAIM.equals(r.scope())) {
            ClaimRecovery rec = recoveries.findById(r.targetId()).orElseThrow(() -> ApiException.notFound("RECOVERY_NOT_FOUND", "Claim recovery " + r.targetId() + " not found"));
            claimShares.findByClaimRecoveryId(rec.getId()).forEach(s -> beforeShares.add(om.createObjectNode().put("reinsurerId", s.getReinsurerId())
                .put("reinsurerDesc", s.getReinsurerDesc()).put("sharePct", s.getSharePct())));
            List<BigDecimal> paid = ShareMath.split(rec.getPaidRecoverableDelta(), pcts), os = ShareMath.split(rec.getOsRecoverableDelta(), pcts);
            for (int i = 0; i < in.size(); i++) {
                Snap ri = masters.reinsurer(in.get(i).reinsurerId());
                afterShares.add(om.createObjectNode().put("reinsurerId", ri.id()).put("reinsurerDesc", ri.desc()).put("sharePct", in.get(i).sharePct())
                    .put("paidRecoverable", paid.get(i)).put("osRecoverable", os.get(i)));
            }
            desc = rec.getClaimNo() + " / " + rec.getTreatyCode() + " recovery";
            entityType = "CLAIM_DISTRIBUTION";
        } else {
            throw ApiException.badRequest("SCOPE_INVALID", "Scope must be POLICY or CLAIM");
        }
        var req = makerChecker.submit(entityType, r.targetId(), desc, "UPDATE", before, after, r.reason().trim(), levels);
        return new AdjustmentResult(req.getId(), req.getRequestNo(), req.getStatusDesc(), "Sent for approval. The distribution changes only after the checker approves.");
    }

    static JsonNode shares(JsonNode payload) { return payload.get("shares"); }
}
