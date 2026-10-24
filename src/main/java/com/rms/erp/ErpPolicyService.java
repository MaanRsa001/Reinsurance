package com.rms.erp;

import com.rms.common.ApiException;
import com.rms.common.Snap;
import com.rms.common.ValidationFailedException;
import com.rms.common.ValidationIssue;
import com.rms.domain.ErpPolicyCover;
import com.rms.domain.ErpPolicyTxn;
import com.rms.domain.ProductSection;
import com.rms.domain.SectionCover;
import com.rms.erp.ErpPolicyDtos.PolicyCoverLine;
import com.rms.erp.ErpPolicyDtos.PolicyCoverRequest;
import com.rms.erp.ErpPolicyDtos.PolicyCoverView;
import com.rms.erp.ErpPolicyDtos.PolicyTxnRequest;
import com.rms.erp.ErpPolicyDtos.PolicyTxnSummary;
import com.rms.erp.ErpPolicyDtos.PolicyTxnView;
import com.rms.master.LookupCodes;
import com.rms.master.LookupService;
import com.rms.master.MasterSnapshotService;
import com.rms.repository.ErpPolicyCoverRepository;
import com.rms.repository.ErpPolicyTxnRepository;
import com.rms.repository.InboundMessageRepository;
import com.rms.repository.ProductSectionRepository;
import com.rms.repository.SectionCoverRepository;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CRUD for ERP policy transactions (erp_policy_txn) and their cover lines (erp_policy_cover).
 * Master ids are resolved into id + code + description snapshots (rule 3A); deletes are soft (record_status = DELETED).
 */
@Service
public class ErpPolicyService {
    static final String DELETED = "DELETED";

    private final ErpPolicyTxnRepository txns;
    private final ErpPolicyCoverRepository covers;
    private final InboundMessageRepository inbound;
    private final ProductSectionRepository sections;
    private final SectionCoverRepository sectionCovers;
    private final MasterSnapshotService masters;
    private final LookupService lookups;

    public ErpPolicyService(ErpPolicyTxnRepository txns, ErpPolicyCoverRepository covers, InboundMessageRepository inbound,
                            ProductSectionRepository sections, SectionCoverRepository sectionCovers, MasterSnapshotService masters,
                            LookupService lookups) {
        this.txns = txns;
        this.covers = covers;
        this.inbound = inbound;
        this.sections = sections;
        this.sectionCovers = sectionCovers;
        this.masters = masters;
        this.lookups = lookups;
    }

    // ================================================================== erp_policy_txn: queries

    @Transactional(readOnly = true)
    public Page<PolicyTxnSummary> search(String q, String policyNo, Long productId, Long txnTypeId, Long inboundMessageId,
                                         LocalDate txnDateFrom, LocalDate txnDateTo, Pageable pageable) {
        Specification<ErpPolicyTxn> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.notEqual(root.get("recordStatus"), DELETED));
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase() + "%";
                ps.add(cb.or(cb.like(cb.lower(root.get("policyNo")), like), cb.like(cb.lower(root.get("insuredName")), like),
                    cb.like(cb.lower(root.get("endorsementNo")), like)));
            }
            if (policyNo != null && !policyNo.isBlank()) ps.add(cb.equal(root.get("policyNo"), policyNo.trim()));
            if (productId != null) ps.add(cb.equal(root.get("productId"), productId));
            if (txnTypeId != null) ps.add(cb.equal(root.get("txnTypeId"), txnTypeId));
            if (inboundMessageId != null) ps.add(cb.equal(root.get("inboundMessageId"), inboundMessageId));
            if (txnDateFrom != null) ps.add(cb.greaterThanOrEqualTo(root.get("txnDate"), txnDateFrom));
            if (txnDateTo != null) ps.add(cb.lessThanOrEqualTo(root.get("txnDate"), txnDateTo));
            return cb.and(ps.toArray(new Predicate[0]));
        };
        return txns.findAll(spec, pageable).map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public PolicyTxnView get(Long id) {
        return toView(loadTxn(id));
    }

    /** All live transactions of one policy number, oldest first, with their covers. */
    @Transactional(readOnly = true)
    public List<PolicyTxnView> byPolicyNo(String policyNo) {
        return txns.findByPolicyNoOrderByTxnDateAscIdAsc(policyNo).stream()
            .filter(t -> !DELETED.equals(t.getRecordStatus())).map(this::toView).toList();
    }

    // ================================================================== erp_policy_txn: commands

    @Transactional
    public PolicyTxnView create(PolicyTxnRequest r) {
        ErpPolicyTxn t = new ErpPolicyTxn();
        applyHeader(t, r);
        List<PolicyCoverLine> lines = r.covers() == null ? List.of() : r.covers();
        validateLines(lines, t.getProductId(), null);
        t = txns.save(t);
        for (PolicyCoverLine l : lines) {
            ErpPolicyCover c = new ErpPolicyCover();
            c.setPolicyTxnId(t.getId());
            applyCover(c, l.sectionId(), l.coverId(), l.sumInsured(), l.sumInsuredAfter(), l.premium(), l.deductibleAmt());
            covers.save(c);
        }
        return toView(t);
    }

    @Transactional
    public PolicyTxnView update(Long id, PolicyTxnRequest r) {
        ErpPolicyTxn t = loadTxn(id);
        checkVersion(r.rowVersion(), t.getRowVersion(), "Policy transaction", id);
        Long oldProduct = t.getProductId();
        applyHeader(t, r);
        List<ErpPolicyCover> existing = liveCovers(t.getId());

        if (r.covers() == null) {
            // covers untouched, but they must still fit the (possibly changed) product
            if (!Objects.equals(oldProduct, t.getProductId())) {
                List<PolicyCoverLine> asLines = existing.stream().map(c -> new PolicyCoverLine(c.getId(), c.getSectionId(), c.getCoverId(),
                    c.getSumInsured(), c.getSumInsuredAfter(), c.getPremium(), c.getDeductibleAmt())).toList();
                validateLines(asLines, t.getProductId(), null);
            }
            return toView(txns.save(t));
        }

        Map<Long, ErpPolicyCover> byId = new HashMap<>();
        existing.forEach(c -> byId.put(c.getId(), c));
        validateLines(r.covers(), t.getProductId(), byId.keySet());
        txns.save(t);

        Set<Long> kept = new HashSet<>();
        for (PolicyCoverLine l : r.covers()) {
            ErpPolicyCover c;
            if (l.id() != null) {
                c = byId.get(l.id());
                kept.add(l.id());
            } else {
                c = new ErpPolicyCover();
                c.setPolicyTxnId(t.getId());
            }
            applyCover(c, l.sectionId(), l.coverId(), l.sumInsured(), l.sumInsuredAfter(), l.premium(), l.deductibleAmt());
            covers.save(c);
        }
        for (ErpPolicyCover c : existing) {
            if (!kept.contains(c.getId())) {
                c.setRecordStatus(DELETED);
                covers.save(c);
            }
        }
        return toView(t);
    }

    /** Soft-deletes the transaction and all its cover lines. */
    @Transactional
    public void delete(Long id) {
        ErpPolicyTxn t = loadTxn(id);
        for (ErpPolicyCover c : liveCovers(id)) {
            c.setRecordStatus(DELETED);
            covers.save(c);
        }
        t.setRecordStatus(DELETED);
        txns.save(t);
    }

    // ================================================================== erp_policy_cover

    @Transactional(readOnly = true)
    public List<PolicyCoverView> covers(Long policyTxnId) {
        loadTxn(policyTxnId);
        return liveCovers(policyTxnId).stream().map(this::toCoverView).toList();
    }

    @Transactional(readOnly = true)
    public PolicyCoverView getCover(Long coverId) {
        return toCoverView(loadCover(coverId));
    }

    @Transactional
    public PolicyCoverView addCover(Long policyTxnId, PolicyCoverRequest r) {
        ErpPolicyTxn t = loadTxn(policyTxnId);
        validateLines(List.of(toLine(null, r)), t.getProductId(), null);
        ErpPolicyCover c = new ErpPolicyCover();
        c.setPolicyTxnId(t.getId());
        applyCover(c, r.sectionId(), r.coverId(), r.sumInsured(), r.sumInsuredAfter(), r.premium(), r.deductibleAmt());
        return toCoverView(covers.save(c));
    }

    @Transactional
    public PolicyCoverView updateCover(Long coverId, PolicyCoverRequest r) {
        ErpPolicyCover c = loadCover(coverId);
        checkVersion(r.rowVersion(), c.getRowVersion(), "Policy cover", coverId);
        ErpPolicyTxn t = loadTxn(c.getPolicyTxnId());
        validateLines(List.of(toLine(null, r)), t.getProductId(), null);
        applyCover(c, r.sectionId(), r.coverId(), r.sumInsured(), r.sumInsuredAfter(), r.premium(), r.deductibleAmt());
        return toCoverView(covers.save(c));
    }

    @Transactional
    public void deleteCover(Long coverId) {
        ErpPolicyCover c = loadCover(coverId);
        c.setRecordStatus(DELETED);
        covers.save(c);
    }

    // ================================================================== helpers

    private ErpPolicyTxn loadTxn(Long id) {
        return txns.findById(id).filter(t -> !DELETED.equals(t.getRecordStatus()))
            .orElseThrow(() -> ApiException.notFound("POLICY_TXN_NOT_FOUND", "Policy transaction " + id + " does not exist"));
    }

    private ErpPolicyCover loadCover(Long id) {
        return covers.findById(id).filter(c -> !DELETED.equals(c.getRecordStatus()))
            .orElseThrow(() -> ApiException.notFound("POLICY_COVER_NOT_FOUND", "Policy cover " + id + " does not exist"));
    }

    private List<ErpPolicyCover> liveCovers(Long policyTxnId) {
        return covers.findByPolicyTxnId(policyTxnId);
    }

    private static void checkVersion(Integer sent, Integer current, String what, Long id) {
        if (sent != null && !sent.equals(current)) {
            throw new ObjectOptimisticLockingFailureException(what, id);
        }
    }

    private static PolicyCoverLine toLine(Long id, PolicyCoverRequest r) {
        return new PolicyCoverLine(id, r.sectionId(), r.coverId(), r.sumInsured(), r.sumInsuredAfter(), r.premium(), r.deductibleAmt());
    }

    /** Validates the header fields and copies them (with master snapshots) onto the entity. */
    private void applyHeader(ErpPolicyTxn t, PolicyTxnRequest r) {
        List<ValidationIssue> issues = new ArrayList<>();
        if (r == null) throw ApiException.badRequest("BODY_REQUIRED", "Request body is required");
        if (r.inboundMessageId() == null) issues.add(ValidationIssue.error("inboundMessageId", "Inbound message id is required"));
        else if (!inbound.existsById(r.inboundMessageId())) issues.add(ValidationIssue.error("inboundMessageId", "Inbound message " + r.inboundMessageId() + " does not exist"));
        required(issues, r.policyNo(), "policyNo", "Policy number", 50);
        maxLen(issues, r.endorsementNo(), "endorsementNo", "Endorsement number", 50);
        if (r.txnTypeId() == null) issues.add(ValidationIssue.error("txnTypeId", "Transaction type is required"));
        if (r.txnDate() == null) issues.add(ValidationIssue.error("txnDate", "Transaction date is required"));
        if (r.inceptionDate() == null) issues.add(ValidationIssue.error("inceptionDate", "Inception date is required"));
        if (r.expiryDate() == null) issues.add(ValidationIssue.error("expiryDate", "Expiry date is required"));
        if (r.inceptionDate() != null && r.expiryDate() != null && r.expiryDate().isBefore(r.inceptionDate())) {
            issues.add(ValidationIssue.error("expiryDate", "Expiry date cannot be before the inception date"));
        }
        if (r.currencyId() == null) issues.add(ValidationIssue.error("currencyId", "Currency is required"));
        if (r.productId() == null) issues.add(ValidationIssue.error("productId", "Product is required"));
        required(issues, r.insuredName(), "insuredName", "Insured name", 255);
        required(issues, r.locationDesc(), "locationDesc", "Location", 255);
        maxLen(issues, r.catZoneCode(), "catZoneCode", "Cat zone code", 30);
        if (issues.stream().anyMatch(ValidationIssue::isError)) throw new ValidationFailedException(issues);

        Snap txnType = lookups.byIdOfType(LookupCodes.TXN_TYPE, r.txnTypeId());
        Snap currency = masters.currency(r.currencyId());
        Snap product = masters.product(r.productId());
        Snap grade = r.riskGradeId() == null ? null : lookups.byIdOfType(LookupCodes.RISK_GRADE, r.riskGradeId());

        t.setInboundMessageId(r.inboundMessageId());
        t.setPolicyNo(r.policyNo().trim());
        t.setEndorsementNo(blankToNull(r.endorsementNo()));
        t.setTxnTypeId(txnType.id());
        t.setTxnTypeDesc(txnType.desc());
        t.setTxnDate(r.txnDate());
        t.setInceptionDate(r.inceptionDate());
        t.setExpiryDate(r.expiryDate());
        t.setCurrencyId(currency.id());
        t.setCurrencyCode(currency.code());
        t.setCurrencyDesc(currency.desc());
        t.setProductId(product.id());
        t.setProductCode(product.code());
        t.setProductDesc(product.desc());
        t.setInsuredName(r.insuredName().trim());
        t.setLocationDesc(r.locationDesc().trim());
        t.setCatZoneCode(blankToNull(r.catZoneCode()));
        t.setRiskGradeId(grade == null ? null : grade.id());
        t.setRiskGradeDesc(grade == null ? null : grade.desc());
    }

    /**
     * Validates cover lines: required amounts, section belongs to the product, cover belongs to the section,
     * no duplicate cover in one transaction, and (on update) line ids must be existing covers of this transaction.
     */
    private void validateLines(List<PolicyCoverLine> lines, Long productId, Set<Long> allowedIds) {
        List<ValidationIssue> issues = new ArrayList<>();
        Set<Long> seenCovers = new HashSet<>();
        Set<Long> seenIds = new HashSet<>();
        for (int i = 0; i < lines.size(); i++) {
            PolicyCoverLine l = lines.get(i);
            String p = lines.size() == 1 && allowedIds == null ? "" : "covers[" + i + "].";
            if (l == null) { issues.add(ValidationIssue.error("covers[" + i + "]", "Cover line is empty")); continue; }
            if (l.id() != null) {
                if (allowedIds == null || !allowedIds.contains(l.id())) issues.add(ValidationIssue.error(p + "id", "Cover " + l.id() + " is not a cover of this transaction"));
                else if (!seenIds.add(l.id())) issues.add(ValidationIssue.error(p + "id", "Cover " + l.id() + " is listed twice"));
            }
            if (l.sectionId() == null) issues.add(ValidationIssue.error(p + "sectionId", "Section is required"));
            if (l.coverId() == null) issues.add(ValidationIssue.error(p + "coverId", "Cover is required"));
            if (l.sumInsured() == null) issues.add(ValidationIssue.error(p + "sumInsured", "Sum insured is required"));
            if (l.sumInsuredAfter() == null) issues.add(ValidationIssue.error(p + "sumInsuredAfter", "Sum insured after the transaction is required"));
            else if (l.sumInsuredAfter().signum() < 0) issues.add(ValidationIssue.error(p + "sumInsuredAfter", "Sum insured after the transaction cannot be negative"));
            if (l.premium() == null) issues.add(ValidationIssue.error(p + "premium", "Premium is required"));
            if (l.deductibleAmt() != null && l.deductibleAmt().signum() < 0) issues.add(ValidationIssue.error(p + "deductibleAmt", "Deductible cannot be negative"));

            ProductSection sec = l.sectionId() == null ? null : sections.findById(l.sectionId()).orElse(null);
            if (l.sectionId() != null && sec == null) issues.add(ValidationIssue.error(p + "sectionId", "Section " + l.sectionId() + " does not exist"));
            else if (sec != null && !Objects.equals(sec.getProductId(), productId)) {
                issues.add(ValidationIssue.error(p + "sectionId", "Section " + sec.getCode() + " does not belong to the transaction's product"));
            }
            SectionCover cv = l.coverId() == null ? null : sectionCovers.findById(l.coverId()).orElse(null);
            if (l.coverId() != null && cv == null) issues.add(ValidationIssue.error(p + "coverId", "Cover " + l.coverId() + " does not exist"));
            else if (cv != null && sec != null && !Objects.equals(cv.getSectionId(), sec.getId())) {
                issues.add(ValidationIssue.error(p + "coverId", "Cover " + cv.getCode() + " does not belong to section " + sec.getCode()));
            }
            if (l.coverId() != null && !seenCovers.add(l.coverId())) {
                issues.add(ValidationIssue.error(p + "coverId", "Cover " + (cv == null ? l.coverId() : cv.getCode()) + " appears more than once"));
            }
        }
        if (issues.stream().anyMatch(ValidationIssue::isError)) throw new ValidationFailedException(issues);
    }

    private void applyCover(ErpPolicyCover c, Long sectionId, Long coverId, BigDecimal si, BigDecimal siAfter, BigDecimal premium, BigDecimal deductible) {
        Snap s = masters.section(sectionId);
        Snap cv = masters.cover(coverId);
        c.setSectionId(s.id());
        c.setSectionCode(s.code());
        c.setSectionDesc(s.desc());
        c.setCoverId(cv.id());
        c.setCoverCode(cv.code());
        c.setCoverDesc(cv.desc());
        c.setSumInsured(si);
        c.setSumInsuredAfter(siAfter);
        c.setPremium(premium);
        c.setDeductibleAmt(deductible);
    }

    private static void required(List<ValidationIssue> issues, String v, String field, String label, int max) {
        if (v == null || v.isBlank()) issues.add(ValidationIssue.error(field, label + " is required"));
        else maxLen(issues, v, field, label, max);
    }

    private static void maxLen(List<ValidationIssue> issues, String v, String field, String label, int max) {
        if (v != null && v.trim().length() > max) issues.add(ValidationIssue.error(field, label + " cannot exceed " + max + " characters"));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static BigDecimal sum(List<ErpPolicyCover> cs, Function<ErpPolicyCover, BigDecimal> f) {
        return cs.stream().map(f).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ================================================================== mapping

    private PolicyTxnSummary toSummary(ErpPolicyTxn t) {
        List<ErpPolicyCover> cs = liveCovers(t.getId());
        return new PolicyTxnSummary(t.getId(), t.getInboundMessageId(), t.getPolicyNo(), t.getEndorsementNo(), t.getTxnTypeId(), t.getTxnTypeDesc(),
            t.getTxnDate(), t.getInceptionDate(), t.getExpiryDate(), t.getCurrencyCode(), t.getProductCode(), t.getProductDesc(), t.getInsuredName(),
            cs.size(), sum(cs, ErpPolicyCover::getSumInsuredAfter), sum(cs, ErpPolicyCover::getPremium), t.getRecordStatus());
    }

    private PolicyTxnView toView(ErpPolicyTxn t) {
        List<ErpPolicyCover> cs = liveCovers(t.getId());
        return new PolicyTxnView(t.getId(), t.getInboundMessageId(), t.getPolicyNo(), t.getEndorsementNo(), t.getTxnTypeId(), t.getTxnTypeDesc(),
            t.getTxnDate(), t.getInceptionDate(), t.getExpiryDate(), t.getCurrencyId(), t.getCurrencyCode(), t.getCurrencyDesc(), t.getProductId(),
            t.getProductCode(), t.getProductDesc(), t.getInsuredName(), t.getLocationDesc(), t.getCatZoneCode(), t.getRiskGradeId(), t.getRiskGradeDesc(),
            sum(cs, ErpPolicyCover::getSumInsured), sum(cs, ErpPolicyCover::getSumInsuredAfter), sum(cs, ErpPolicyCover::getPremium),
            cs.stream().map(this::toCoverView).toList(), t.getRecordStatus(), t.getRowVersion(),
            t.getCreatedBy(), t.getCreatedAt(), t.getUpdatedBy(), t.getUpdatedAt());
    }

    private PolicyCoverView toCoverView(ErpPolicyCover c) {
        return new PolicyCoverView(c.getId(), c.getPolicyTxnId(), c.getSectionId(), c.getSectionCode(), c.getSectionDesc(), c.getCoverId(),
            c.getCoverCode(), c.getCoverDesc(), c.getSumInsured(), c.getSumInsuredAfter(), c.getPremium(), c.getDeductibleAmt(),
            c.getRecordStatus(), c.getRowVersion(), c.getCreatedBy(), c.getCreatedAt(), c.getUpdatedBy(), c.getUpdatedAt());
    }
}
