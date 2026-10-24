package com.rms.distribution;

import com.rms.common.ApiException;
import com.rms.distribution.DistributionDtos.*;
import com.rms.domain.*;
import com.rms.repository.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read side: where did each policy / claim go, treaty by treaty, layer by layer, reinsurer by reinsurer. */
@Service
public class DistributionService {
    private final ErpPolicyTxnRepository policyTxns;
    private final ErpPolicyCoverRepository policyCovers;
    private final CessionRepository cessions;
    private final CessionLayerRepository layers;
    private final CessionReinsurerShareRepository shares;
    private final FacOutPlacementRepository facs;
    private final FacOutReinsurerRepository facShares;
    private final ErpClaimTxnRepository claimTxns;
    private final ClaimRecoveryRepository recoveries;
    private final ClaimRecoveryReinsurerRepository recoveryShares;
    private final XolEventRepository events;
    private final XolEventLossRepository eventLosses;
    private final XolLayerRecoveryRepository eventLayers;
    private final TreatySurplusLayerRepository surplusLayers;
    private final TreatyXolLayerRepository xolLayers;

    public DistributionService(ErpPolicyTxnRepository policyTxns, ErpPolicyCoverRepository policyCovers, CessionRepository cessions, CessionLayerRepository layers,
                               CessionReinsurerShareRepository shares, FacOutPlacementRepository facs, FacOutReinsurerRepository facShares,
                               ErpClaimTxnRepository claimTxns, ClaimRecoveryRepository recoveries, ClaimRecoveryReinsurerRepository recoveryShares,
                               XolEventRepository events, XolEventLossRepository eventLosses, XolLayerRecoveryRepository eventLayers,
                               TreatySurplusLayerRepository surplusLayers, TreatyXolLayerRepository xolLayers) {
        this.policyTxns = policyTxns;
        this.policyCovers = policyCovers;
        this.cessions = cessions;
        this.layers = layers;
        this.shares = shares;
        this.facs = facs;
        this.facShares = facShares;
        this.claimTxns = claimTxns;
        this.recoveries = recoveries;
        this.recoveryShares = recoveryShares;
        this.events = events;
        this.eventLosses = eventLosses;
        this.eventLayers = eventLayers;
        this.surplusLayers = surplusLayers;
        this.xolLayers = xolLayers;
    }

    private static BigDecimal z(BigDecimal b) { return b == null ? BigDecimal.ZERO : b; }

    // ------------------------------------------------------------------ policies
    @Transactional(readOnly = true)
    public List<PolicyListItem> searchPolicies(String q) {
        Map<String, List<ErpPolicyTxn>> byPolicy = new LinkedHashMap<>();
        for (ErpPolicyTxn t : policyTxns.search(q == null ? "" : q.trim())) byPolicy.computeIfAbsent(t.getPolicyNo(), k -> new ArrayList<>()).add(t);
        List<PolicyListItem> out = new ArrayList<>();
        byPolicy.forEach((no, txns) -> {
            ErpPolicyTxn last = txns.stream().max(Comparator.comparing(ErpPolicyTxn::getTxnDate).thenComparing(ErpPolicyTxn::getId)).get();
            BigDecimal premium = BigDecimal.ZERO;
            int cessionCount = 0;
            for (ErpPolicyTxn t : txns) {
                for (ErpPolicyCover c : policyCovers.findByPolicyTxnId(t.getId())) premium = premium.add(z(c.getPremium()));
                cessionCount += cessions.findByPolicyTxnId(t.getId()).size();
            }
            BigDecimal si = policyCovers.findByPolicyTxnId(last.getId()).stream().map(c -> z(c.getSumInsuredAfter())).reduce(BigDecimal.ZERO, BigDecimal::add);
            out.add(new PolicyListItem(no, last.getInsuredName(), last.getProductDesc(), txns.size(), last.getTxnDate(), premium, si, cessionCount));
        });
        return out;
    }

    @Transactional(readOnly = true)
    public PolicyDistribution policy(String policyNo) {
        List<ErpPolicyTxn> txns = policyTxns.findByPolicyNoOrderByTxnDateAscIdAsc(policyNo);
        if (txns.isEmpty()) throw ApiException.notFound("POLICY_NOT_FOUND", "Policy " + policyNo + " not found");
        ErpPolicyTxn first = txns.get(0);
        Map<Long, ReinsurerTotal> totals = new LinkedHashMap<>();
        List<TxnDist> txnViews = new ArrayList<>();
        for (ErpPolicyTxn t : txns) {
            List<Cession> cs = cessions.findByPolicyTxnId(t.getId());
            List<CoverDist> coverViews = new ArrayList<>();
            for (ErpPolicyCover c : policyCovers.findByPolicyTxnId(t.getId())) {
                List<CessionDist> cessionViews = new ArrayList<>();
                for (Cession cs1 : cs) {
                    if (!c.getCoverId().equals(cs1.getCoverId())) continue;
                    List<LayerDist> layerViews = new ArrayList<>();
                    for (CessionLayer l : layers.findByCessionIdOrderByLayerNo(cs1.getId())) {
                        List<ShareDist> shareViews = new ArrayList<>();
                        for (CessionReinsurerShare s : shares.findByCessionLayerId(l.getId())) {
                            shareViews.add(new ShareDist(s.getId(), s.getReinsurerId(), s.getReinsurerDesc(), s.getSharePct(), s.getCededSi(), s.getCededPremium(), s.getCededCommission()));
                            addTotal(totals, s.getReinsurerId(), s.getReinsurerDesc(), s.getCededSi(), s.getCededPremium(), s.getCededCommission());
                        }
                        layerViews.add(new LayerDist(l.getId(), l.getLayerNo(), l.getLayerDesc(), l.getCededSi(), l.getCessionPct(), l.getCededPremium(), l.getCommissionPct(),
                            l.getCededCommission(), shareViews));
                    }
                    cessionViews.add(new CessionDist(cs1.getId(), cs1.getTreatyId(), cs1.getTreatyCode(), cs1.getTreatyVersionNo(), cs1.getTreatyTypeDesc(), cs1.getBasisSi(),
                        cs1.getRetainedSi(), cs1.getCededSi(), cs1.getExcessSi(), cs1.getGrossPremium(), cs1.getRetainedPremium(), cs1.getCededPremium(),
                        cs1.getExcessPremium(), cs1.getCededCommission(), cs1.getRemarks(), layerViews));
                }
                coverViews.add(new CoverDist(c.getCoverId(), c.getCoverDesc(), c.getSectionDesc(), c.getSumInsured(), c.getSumInsuredAfter(), c.getPremium(), cessionViews));
            }
            txnViews.add(new TxnDist(t.getId(), t.getEndorsementNo(), t.getTxnTypeDesc(), t.getTxnDate(), coverViews));
        }
        List<FacDist> facViews = new ArrayList<>();
        for (FacOutPlacement f : facs.findByPolicyNo(policyNo)) {
            List<FacShare> fs = new ArrayList<>();
            for (FacOutReinsurer r : facShares.findByPlacementId(f.getId())) {
                fs.add(new FacShare(r.getReinsurerDesc(), r.getSharePct(), r.getPlacedSi(), r.getPremium(), r.getCommission(), r.getBrokerage()));
                addTotal(totals, r.getReinsurerId(), r.getReinsurerDesc(), r.getPlacedSi(), r.getPremium(), r.getCommission());
            }
            facViews.add(new FacDist(f.getFacRef(), f.getCoverDesc(), f.getPlacedSi(), f.getPremium(), f.getCommissionPct(), f.getBrokeragePct(), f.getStatusDesc(), fs));
        }
        return new PolicyDistribution(policyNo, first.getInsuredName(), first.getProductDesc(), first.getLocationDesc(), first.getInceptionDate(), first.getExpiryDate(),
            txnViews, facViews, new ArrayList<>(totals.values()));
    }

    private static void addTotal(Map<Long, ReinsurerTotal> m, Long id, String desc, BigDecimal si, BigDecimal prem, BigDecimal comm) {
        ReinsurerTotal o = m.get(id);
        m.put(id, o == null ? new ReinsurerTotal(id, desc, z(si), z(prem), z(comm))
            : new ReinsurerTotal(id, desc, o.cededSi().add(z(si)), o.cededPremium().add(z(prem)), o.cededCommission().add(z(comm))));
    }

    // ------------------------------------------------------------------ claims
    @Transactional(readOnly = true)
    public List<ClaimListItem> searchClaims(String q) {
        Map<String, List<ErpClaimTxn>> byClaim = new LinkedHashMap<>();
        for (ErpClaimTxn t : claimTxns.search(q == null ? "" : q.trim())) byClaim.computeIfAbsent(t.getClaimNo(), k -> new ArrayList<>()).add(t);
        List<ClaimListItem> out = new ArrayList<>();
        byClaim.forEach((no, txns) -> {
            ErpClaimTxn last = txns.stream().max(Comparator.comparing(ErpClaimTxn::getTxnDate).thenComparing(ErpClaimTxn::getId)).get();
            out.add(new ClaimListItem(no, last.getPolicyNo(), last.getCoverDesc(), last.getDateOfLoss(), last.getCumulativePaid(), last.getOutstandingAmt(), txns.size()));
        });
        return out;
    }

    @Transactional(readOnly = true)
    public ClaimDistribution claim(String claimNo) {
        List<ErpClaimTxn> txns = claimTxns.findByClaimNoOrderByTxnDateAscIdAsc(claimNo);
        if (txns.isEmpty()) throw ApiException.notFound("CLAIM_NOT_FOUND", "Claim " + claimNo + " not found");
        ErpClaimTxn first = txns.get(0);
        Map<Long, ClaimReinsurerTotal> totals = new LinkedHashMap<>();
        List<ClaimTxnDist> txnViews = new ArrayList<>();
        for (ErpClaimTxn t : txns) {
            List<RecoveryDist> rv = new ArrayList<>();
            for (ClaimRecovery r : recoveries.findByClaimTxnId(t.getId())) {
                RecoveryDist d = recovery(r);
                rv.add(d);
                for (ClaimShare s : d.shares()) {
                    ClaimReinsurerTotal o = totals.get(s.reinsurerId());
                    totals.put(s.reinsurerId(), o == null ? new ClaimReinsurerTotal(s.reinsurerId(), s.reinsurerDesc(), z(s.paidRecoverable()), z(s.osRecoverable()))
                        : new ClaimReinsurerTotal(s.reinsurerId(), s.reinsurerDesc(), o.paidRecoverable().add(z(s.paidRecoverable())), o.osRecoverable().add(z(s.osRecoverable()))));
                }
            }
            txnViews.add(new ClaimTxnDist(t.getId(), t.getClaimTxnTypeDesc(), t.getTxnDate(), t.getPaidAmt(), t.getOutstandingAmt(), t.getCumulativePaid(), rv));
        }
        List<EventDist> eventViews = new ArrayList<>();
        for (XolEventLoss el : eventLosses.findByClaimNo(claimNo)) {
            events.findById(el.getXolEventId()).ifPresent(ev -> {
                BigDecimal rp = eventLayers.findByXolEventId(ev.getId()).stream().map(x -> z(x.getReinstatementPremium())).reduce(BigDecimal.ZERO, BigDecimal::add);
                eventViews.add(new EventDist(ev.getEventCode(), ev.getEventDesc(), ev.getTreatyCode(), ev.getGrossLoss(), ev.getInuringRecovery(), ev.getUnlAmount(), rp,
                    recoveries.findByXolEventId(ev.getId()).stream().map(this::recovery).toList()));
            });
        }
        return new ClaimDistribution(claimNo, first.getPolicyNo(), first.getCoverDesc(), first.getDateOfLoss(), txnViews, eventViews, new ArrayList<>(totals.values()));
    }

    private RecoveryDist recovery(ClaimRecovery r) {
        String label = r.getSurplusLayerId() != null ? surplusLayers.findById(r.getSurplusLayerId()).map(TreatySurplusLayer::getLayerName).orElse("Surplus layer")
            : r.getXolLayerId() != null ? xolLayers.findById(r.getXolLayerId()).map(TreatyXolLayer::getLayerName).orElse("XOL layer") : "Whole treaty";
        List<ClaimShare> sh = recoveryShares.findByClaimRecoveryId(r.getId()).stream().map(s -> new ClaimShare(s.getId(), s.getReinsurerId(), s.getReinsurerDesc(),
            s.getSharePct(), s.getPaidRecoverable(), s.getOsRecoverable(), s.getStatusDesc(), s.getRemarks())).toList();
        return new RecoveryDist(r.getId(), r.getTreatyCode(), r.getTreatyTypeDesc(), label, r.getCessionPct(), r.getPaidRecoverableDelta(), r.getOsRecoverableDelta(),
            r.getStatusDesc(), sh);
    }
}
