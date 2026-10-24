package com.rms.treaty;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rms.common.ApiException;
import com.rms.common.Snap;
import com.rms.common.ValidationFailedException;
import com.rms.common.ValidationIssue;
import com.rms.domain.PcConfig;
import com.rms.domain.ScScale;
import com.rms.domain.Treaty;
import com.rms.domain.TreatyCoverAllocationRule;
import com.rms.domain.TreatyCoverMap;
import com.rms.domain.TreatyExclusion;
import com.rms.domain.TreatyPanel;
import com.rms.domain.TreatyParamQs;
import com.rms.domain.TreatyStoploss;
import com.rms.domain.TreatySurplusLayer;
import com.rms.domain.TreatySurplusRetention;
import com.rms.domain.TreatyXolLayer;
import com.rms.domain.TreatyXolReinstatement;
import com.rms.makerchecker.MakerCheckerService;
import com.rms.master.CoverHierarchyService;
import com.rms.master.LookupCodes;
import com.rms.master.LookupService;
import com.rms.master.MasterSnapshotService;
import com.rms.repository.TreatyRepository;
import com.rms.treaty.TreatyDtos.DiffEntry;
import com.rms.treaty.TreatyDtos.PanelDto;
import com.rms.treaty.TreatyDtos.RenewRequest;
import com.rms.treaty.TreatyDtos.SaveResult;
import com.rms.treaty.TreatyDtos.SimulationRequest;
import com.rms.treaty.TreatyDtos.SimulationResult;
import com.rms.treaty.TreatyDtos.TreatyRequest;
import com.rms.treaty.TreatyDtos.TreatySummary;
import com.rms.treaty.TreatyDtos.TreatyView;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Treaty administration use cases.
 *
 * Edit / update semantics (PUT /treaties/{id}):
 *  - DRAFT treaty            -> updated in place (header + all child rows replaced)
 *  - ACTIVE / RUN_OFF treaty -> a NEW DRAFT VERSION (version_no + 1, supersedes the live one) is created from the payload;
 *                               the live version keeps running until the new version is approved and activated
 *  - PENDING_APPROVAL        -> rejected with 409 (withdraw the submission first)
 */
@Service
public class TreatyService {
    private static final String DELETED = "DELETED";

    private final TreatyRepository repo;
    private final TreatyMapper mapper;
    private final TreatyValidator validator;
    private final TreatySimulator simulator;
    private final MasterSnapshotService masters;
    private final CoverHierarchyService hierarchy;
    private final LookupService lookups;
    private final MakerCheckerService makerChecker;
    private final ObjectMapper om;
    private final int treatyLevels;

    public TreatyService(TreatyRepository repo, TreatyMapper mapper, TreatyValidator validator, TreatySimulator simulator, MasterSnapshotService masters,
                         CoverHierarchyService hierarchy, LookupService lookups, MakerCheckerService makerChecker, ObjectMapper om, @Value("${rms.makerchecker.levels.treaty:2}") int treatyLevels) {
        this.repo = repo;
        this.mapper = mapper;
        this.validator = validator;
        this.simulator = simulator;
        this.masters = masters;
        this.hierarchy = hierarchy;
        this.lookups = lookups;
        this.makerChecker = makerChecker;
        this.om = om;
        this.treatyLevels = treatyLevels;
    }

    private static <T> List<T> nz(List<T> l) { return l == null ? List.of() : l; }

    // ------------------------------------------------------------------ queries

    @Transactional(readOnly = true)
    public Page<TreatySummary> search(String q, Long typeId, Long statusId, Long lobId, Integer uwYear, boolean latestOnly, Pageable pageable) {
        Specification<Treaty> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.notEqual(root.get("recordStatus"), DELETED));
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase() + "%";
                ps.add(cb.or(cb.like(cb.lower(root.get("treatyCode")), like), cb.like(cb.lower(root.get("treatyName")), like)));
            }
            if (typeId != null) {          // the treaty type is on the mapping rows: treaties having at least one row of that type
                Subquery<Long> tq = query.subquery(Long.class);
                Root<TreatyCoverMap> m = tq.from(TreatyCoverMap.class);
                tq.select(m.<Long>get("id")).where(cb.equal(m.get("treatyId"), root.get("id")), cb.equal(m.get("treatyTypeId"), typeId));
                ps.add(cb.exists(tq));
            }
            if (statusId != null) ps.add(cb.equal(root.get("statusId"), statusId));
            if (lobId != null) ps.add(cb.equal(root.get("lobId"), lobId));
            if (uwYear != null) ps.add(cb.equal(cb.function("year", Integer.class, root.get("inceptionDate")), uwYear));
            if (latestOnly) {
                Subquery<Integer> sq = query.subquery(Integer.class);
                Root<Treaty> r2 = sq.from(Treaty.class);
                sq.select(cb.max(r2.<Integer>get("versionNo")))
                  .where(cb.equal(r2.get("treatyCode"), root.get("treatyCode")), cb.notEqual(r2.get("recordStatus"), DELETED));
                ps.add(cb.equal(root.<Integer>get("versionNo"), sq));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        return repo.findAll(spec, pageable).map(mapper::toSummary);
    }

    @Transactional(readOnly = true)
    public TreatyView get(Long id) {
        return mapper.toView(load(id));
    }

    @Transactional(readOnly = true)
    public List<TreatySummary> versions(String code) {
        return repo.findByTreatyCodeAndRecordStatusNotOrderByVersionNoDesc(code, DELETED).stream().map(mapper::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public List<ValidationIssue> validate(TreatyRequest r) {
        return validator.validate(r);
    }

    @Transactional(readOnly = true)
    public List<ValidationIssue> validate(Long id) {
        return validator.validate(mapper.toRequest(load(id)));
    }

    @Transactional(readOnly = true)
    public SimulationResult simulate(Long id, SimulationRequest req) {
        return simulator.simulate(mapper.toRequest(load(id)), req);
    }

    @Transactional(readOnly = true)
    public SimulationResult simulateDraft(TreatyRequest draft, SimulationRequest req) {
        return simulator.simulate(draft, req);
    }

    @Transactional(readOnly = true)
    public List<DiffEntry> diff(Long id, Long otherId) {
        Map<String, String> a = flatten(om.valueToTree(mapper.toView(load(id))));
        Map<String, String> b = flatten(om.valueToTree(mapper.toView(load(otherId))));
        Map<String, DiffEntry> out = new TreeMap<>();
        for (String k : a.keySet()) if (!b.containsKey(k) || !b.get(k).equals(a.get(k))) out.put(k, new DiffEntry(k, a.get(k), b.get(k)));
        for (String k : b.keySet()) if (!a.containsKey(k)) out.put(k, new DiffEntry(k, null, b.get(k)));
        return new ArrayList<>(out.values());
    }

    private static final java.util.Set<String> DIFF_SKIP = java.util.Set.of("id", "supersedesTreatyId", "updatedBy", "updatedAt", "versionNo", "status", "editable");

    private Map<String, String> flatten(JsonNode node) {
        Map<String, String> out = new TreeMap<>();
        walk("", node, out);
        return out;
    }

    private void walk(String path, JsonNode n, Map<String, String> out) {
        if (n == null || n.isNull()) return;
        if (n.isObject()) {
            n.fields().forEachRemaining(e -> {
                if (!DIFF_SKIP.contains(e.getKey())) walk(path.isEmpty() ? e.getKey() : path + "." + e.getKey(), e.getValue(), out);
            });
        } else if (n.isArray()) {
            for (int i = 0; i < n.size(); i++) walk(path + "[" + i + "]", n.get(i), out);
        } else {
            out.put(path, n.asText());
        }
    }

    // ------------------------------------------------------------------ commands

    @Transactional
    public SaveResult create(TreatyRequest r) {
        if (r.treatyCode() == null || r.treatyCode().isBlank()) throw ApiException.badRequest("TREATY_CODE_REQUIRED", "Treaty code is required");
        String code = r.treatyCode().trim().toUpperCase();
        if (repo.existsByTreatyCode(code)) throw ApiException.conflict("TREATY_CODE_EXISTS", "Treaty code " + code + " already exists");
        Treaty t = new Treaty();
        t.setTreatyCode(code);
        t.setVersionNo(1);
        applyHeader(t, r);
        setStatus(t, LookupCodes.DRAFT);
        t = persistStructure(t, r);
        return result(t, false);
    }

    @Transactional
    public SaveResult update(Long id, TreatyRequest r) {
        Treaty cur = load(id);
        String status = lookups.byId(cur.getStatusId()).code();
        if (LookupCodes.DRAFT.equals(status)) {
            applyHeader(cur, r);
            cur = persistStructure(cur, r);
            return result(cur, false);
        }
        if (LookupCodes.ACTIVE.equals(status) || LookupCodes.RUN_OFF.equals(status)) return amend(cur, r);
        if (LookupCodes.PENDING_APPROVAL.equals(status))
            throw ApiException.conflict("UNDER_APPROVAL", "This version is awaiting approval. Withdraw the submission before editing.");
        throw ApiException.conflict("NOT_EDITABLE", "A " + status.toLowerCase() + " treaty version cannot be edited");
    }

    private SaveResult amend(Treaty live, TreatyRequest r) {
        List<Long> open = List.of(lookups.snap(LookupCodes.TREATY_STATUS, LookupCodes.DRAFT).id(), lookups.snap(LookupCodes.TREATY_STATUS, LookupCodes.PENDING_APPROVAL).id());
        if (repo.existsByTreatyCodeAndStatusIdInAndRecordStatusNot(live.getTreatyCode(), open, DELETED))
            throw ApiException.conflict("OPEN_VERSION_EXISTS", "A newer draft or pending version of " + live.getTreatyCode() + " already exists. Edit or withdraw it first.");
        if (r.effectiveFrom() == null) throw ApiException.badRequest("EFFECTIVE_DATE_REQUIRED", "An effective date is required when amending an active treaty");
        Treaty nt = new Treaty();
        nt.setTreatyCode(live.getTreatyCode());
        nt.setVersionNo(repo.maxVersion(live.getTreatyCode()) + 1);
        nt.setSupersedesTreatyId(live.getId());
        applyHeader(nt, r);
        Snap mode = r.amendModeId() != null ? lookups.byIdOfType(LookupCodes.AMEND_MODE, r.amendModeId()) : lookups.snap(LookupCodes.AMEND_MODE, LookupCodes.PROSPECTIVE);
        nt.setAmendModeId(mode.id());
        nt.setAmendModeDesc(mode.desc());
        setStatus(nt, LookupCodes.DRAFT);
        nt = persistStructure(nt, r);
        return result(nt, true);
    }

    @Transactional
    public void delete(Long id) {
        Treaty t = load(id);
        if (!LookupCodes.DRAFT.equals(lookups.byId(t.getStatusId()).code())) throw ApiException.conflict("NOT_DRAFT", "Only draft versions can be deleted");
        t.setRecordStatus(DELETED);
        repo.save(t);
    }

    @Transactional
    public SaveResult renew(Long id, RenewRequest rr) {
        Treaty src = load(id);
        if (rr.newTreatyCode() == null || rr.inceptionDate() == null || rr.expiryDate() == null) throw ApiException.badRequest("RENEW_INCOMPLETE", "New code and period are required");
        String name = rr.newTreatyName() != null && !rr.newTreatyName().isBlank() ? rr.newTreatyName() : src.getTreatyName();
        return create(mapper.toRequest(src).withIdentity(rr.newTreatyCode(), name, rr.inceptionDate(), rr.expiryDate()));
    }

    @Transactional
    public TreatyView submit(Long id, String remarks) {
        Treaty t = load(id);
        if (!LookupCodes.DRAFT.equals(lookups.byId(t.getStatusId()).code())) throw ApiException.conflict("NOT_DRAFT", "Only draft versions can be submitted");
        List<ValidationIssue> issues = validator.validate(mapper.toRequest(t));
        if (issues.stream().anyMatch(ValidationIssue::isError)) throw new ValidationFailedException(issues);
        Object before = t.getSupersedesTreatyId() == null ? null : repo.findById(t.getSupersedesTreatyId()).map(mapper::toView).orElse(null);
        TreatyView after = mapper.toView(t);
        makerChecker.submit(LookupCodes.ENTITY_TREATY, t.getId(), t.getTreatyCode() + " v" + t.getVersionNo(), LookupCodes.ACTION_ACTIVATE, before, after, remarks, treatyLevels);
        setStatus(t, LookupCodes.PENDING_APPROVAL);
        return mapper.toView(repo.save(t));
    }

    @Transactional
    public TreatyView withdraw(Long id) {
        Treaty t = load(id);
        if (!LookupCodes.PENDING_APPROVAL.equals(lookups.byId(t.getStatusId()).code())) throw ApiException.conflict("NOT_PENDING", "Only a pending version can be withdrawn");
        makerChecker.withdrawOpenRequest(LookupCodes.ENTITY_TREATY, t.getId());
        return mapper.toView(load(id));
    }

    // ------------------------------------------------------------------ building the aggregate

    private Treaty load(Long id) {
        Treaty t = repo.findById(id).orElseThrow(() -> ApiException.notFound("TREATY_NOT_FOUND", "Treaty " + id + " not found"));
        if (DELETED.equals(t.getRecordStatus())) throw ApiException.notFound("TREATY_NOT_FOUND", "Treaty " + id + " not found");
        return t;
    }

    private SaveResult result(Treaty t, boolean newVersion) {
        return new SaveResult(mapper.toView(t), newVersion, validator.validate(mapper.toRequest(t)));
    }

    private void setStatus(Treaty t, String code) {
        Snap s = lookups.snap(LookupCodes.TREATY_STATUS, code);
        t.setStatusId(s.id());
        t.setStatusDesc(s.desc());
    }

    /** Header fields + master snapshots (id, code, description). */
    private void applyHeader(Treaty t, TreatyRequest r) {
        if (r.treatyName() == null || r.treatyName().isBlank()) throw ApiException.badRequest("TREATY_NAME_REQUIRED", "Treaty name is required");
        if (r.inceptionDate() == null || r.expiryDate() == null) throw ApiException.badRequest("PERIOD_REQUIRED", "Inception and expiry dates are required");
        t.setTreatyName(r.treatyName().trim());
        // no treaty type on the header any more: each mapping row carries its own type (treaty_cover_map.treaty_type_*)
        t.setTreatyTypeId(null); t.setTreatyTypeCode(null); t.setTreatyTypeDesc(null);
        Snap s = masters.treatyBasis(r.treatyBasisId());
        t.setTreatyBasisId(s.id()); t.setTreatyBasisCode(s.code()); t.setTreatyBasisDesc(s.desc());
        s = masters.cessionBasis(r.cessionBasisId());
        t.setCessionBasisId(s.id()); t.setCessionBasisCode(s.code()); t.setCessionBasisDesc(s.desc());
        s = masters.currency(r.currencyId());
        t.setCurrencyId(s.id()); t.setCurrencyCode(s.code()); t.setCurrencyDesc(s.desc());
        s = masters.lob(r.lobId());
        t.setLobId(s.id()); t.setLobCode(s.code()); t.setLobDesc(s.desc());
        s = masters.broker(r.brokerId());
        t.setBrokerId(s.id()); t.setBrokerCode(s.code()); t.setBrokerDesc(s.desc());
        t.setInceptionDate(r.inceptionDate());
        t.setExpiryDate(r.expiryDate());
        t.setEffectiveFrom(r.effectiveFrom() != null ? r.effectiveFrom() : r.inceptionDate());
        t.setRemarks(r.remarks());
    }

    /**
     * Persists header + children. Existing children are removed in their own flush first (so unique keys such as layer numbers can be reused), the header
     * is saved so its id exists, then the mapping rows (each with its type, rules and terms) and the panel are re-created.
     */
    private Treaty persistStructure(Treaty t, TreatyRequest r) {
        if (t.getId() != null) {
            t.getPanel().clear();
            t.getScScales().clear();
            t.getPcConfigs().clear();
            t.getExclusions().clear();
            t.getCoverMaps().clear();
        }
        t = repo.saveAndFlush(t);
        addChildren(t, r);
        addPanel(t, r);
        return repo.saveAndFlush(t);
    }

    private void addChildren(Treaty t, TreatyRequest r) {
        if (r.scScale() != null) {
            var d = r.scScale();
            ScScale e = new ScScale();
            e.setProvisionalPct(d.provisionalPct());
            e.setMinPct(d.minPct());
            e.setMaxPct(d.maxPct());
            e.setLrAtMaxPct(d.lrAtMaxPct());
            e.setLrAtMinPct(d.lrAtMinPct());
            e.setSlideRatio(d.slideRatio());
            Snap b = lookups.byIdOfType(LookupCodes.LR_BASIS, d.lrBasisId());
            e.setLrBasisId(b.id()); e.setLrBasisDesc(b.desc());
            e.setAdjustmentMonths(d.adjustmentMonths());
            t.getScScales().add(e);
        }
        if (r.pcConfig() != null) {
            var d = r.pcConfig();
            PcConfig e = new PcConfig();
            e.setPcPct(d.pcPct());
            e.setMgmtExpensePct(d.mgmtExpensePct());
            e.setCarryForwardYears(d.carryForwardYears() == null ? 0 : d.carryForwardYears());
            e.setDafEnabled(Boolean.TRUE.equals(d.dafEnabled()));
            t.getPcConfigs().add(e);
        }
        for (var d : nz(r.exclusions())) {
            if (d.exclusionDesc() == null || d.exclusionDesc().isBlank()) continue;
            TreatyExclusion e = new TreatyExclusion();
            e.setExclusionDesc(d.exclusionDesc().trim());
            e.setActionDesc(d.actionDesc() == null || d.actionDesc().isBlank() ? "Refer to facultative" : d.actionDesc().trim());
            t.getExclusions().add(e);
        }
        for (var d : nz(r.coverMap())) {
            // LOB -> product -> section -> cover chain is cross-checked against the masters and against the treaty LOB; contradictions are rejected here
            var res = hierarchy.resolve(d.lobId(), d.productId(), d.sectionId(), d.coverId(), t.getLobId());
            if (!res.ok()) throw ApiException.badRequest("COVER_MAP_INVALID", String.join("; ", res.errors()));
            if (d.treatyTypeId() == null) throw ApiException.badRequest("TREATY_TYPE_REQUIRED", "Every mapping row needs a treaty type");
            TreatyCoverMap e = new TreatyCoverMap();
            e.setTreatyCode(t.getTreatyCode());
            Snap ty = masters.treatyType(d.treatyTypeId());
            e.setTreatyTypeId(ty.id()); e.setTreatyTypeCode(ty.code()); e.setTreatyTypeDesc(ty.desc());
            e.setNote(d.note() == null || d.note().isBlank() ? null : d.note().trim());
            Snap lv = lookups.snap("MAP_LEVEL", res.level());
            e.setMapLevelId(lv.id()); e.setMapLevelDesc(lv.desc());
            e.setLobId(res.lob().id()); e.setLobCode(res.lob().code()); e.setLobDesc(res.lob().desc());
            if (res.product() != null) { e.setProductId(res.product().id()); e.setProductCode(res.product().code()); e.setProductDesc(res.product().desc()); }
            if (res.section() != null) { e.setSectionId(res.section().id()); e.setSectionCode(res.section().code()); e.setSectionDesc(res.section().desc()); }
            if (res.cover() != null) { e.setCoverId(res.cover().id()); e.setCoverCode(res.cover().code()); e.setCoverDesc(res.cover().desc()); }
            e.setSequenceNo(d.sequenceNo());
            e.setEffectiveFrom(d.effectiveFrom());
            e.setEffectiveTo(d.effectiveTo());
            int ruleSeq = 0;
            for (var rd : nz(d.rules())) {
                TreatyCoverAllocationRule ru = new TreatyCoverAllocationRule();
                ru.setRuleNo(rd.ruleNo() != null ? rd.ruleNo() : ++ruleSeq);
                Snap m = pick(LookupCodes.ALLOCATION_METHOD, rd.methodId(), rd.methodCode());
                ru.setAllocationMethodId(m.id()); ru.setAllocationMethodCode(m.code()); ru.setAllocationMethodDesc(m.desc());
                Snap b = pick(LookupCodes.ALLOCATION_BASIS, rd.basisId(), rd.basisCode());
                ru.setAllocationBasisId(b.id()); ru.setAllocationBasisCode(b.code()); ru.setAllocationBasisDesc(b.desc());
                Snap ra = pick(LookupCodes.RESIDUAL_ACTION, rd.residualActionId(), rd.residualActionCode() != null ? rd.residualActionCode() : LookupCodes.R_CONTINUE);
                ru.setResidualActionId(ra.id()); ru.setResidualActionCode(ra.code()); ru.setResidualActionDesc(ra.desc());
                ru.setAllocationPct(rd.allocationPct());
                ru.setMinAllocationAmt(rd.minAllocationAmt());
                ru.setMaxAllocationAmt(rd.maxAllocationAmt());
                ru.setStopOnLimitFlag(rd.stopOnLimit() == null || rd.stopOnLimit());
                ru.setConditionRule(rd.conditionRule());
                ru.setEffectiveFrom(rd.effectiveFrom() != null ? rd.effectiveFrom() : d.effectiveFrom());
                ru.setEffectiveTo(rd.effectiveTo() != null ? rd.effectiveTo() : d.effectiveTo());
                e.getRules().add(ru);
            }
            addRowTerms(t, e, d, masters.strategyOf(ty.id()));
            t.getCoverMaps().add(e);
        }
    }

    /** Terms of one mapping row: only the set that belongs to the row's treaty type is stored (anything else in the payload is ignored). */
    private void addRowTerms(Treaty t, TreatyCoverMap e, TreatyDtos.CoverMapDto d, String strategy) {
        Long tid = t.getId();
        switch (strategy) {
            case LookupCodes.STRAT_QS, LookupCodes.STRAT_FAC_OBL -> {
                var q = d.qsParams();
                if (q == null) return;
                TreatyParamQs x = new TreatyParamQs();
                x.setTreatyId(tid);
                x.setCessionPct(q.cessionPct());
                x.setRetentionPct(q.retentionPct() != null ? q.retentionPct() : q.cessionPct() == null ? null : BigDecimal.valueOf(100).subtract(q.cessionPct()));
                x.setPerRiskLimit(q.perRiskLimit());
                x.setPerEventLimit(q.perEventLimit());
                Snap c = masters.commissionType(q.commissionTypeId());
                x.setCommissionTypeId(c.id()); x.setCommissionTypeCode(c.code()); x.setCommissionTypeDesc(c.desc());
                x.setFlatCommissionPct(q.flatCommissionPct());
                x.setCashLossLimit(q.cashLossLimit());
                e.getQsParams().add(x);
            }
            case LookupCodes.STRAT_SURPLUS -> {
                for (var rd : nz(d.retentions())) {
                    TreatySurplusRetention x = new TreatySurplusRetention();
                    x.setTreatyId(tid);
                    Snap g = lookups.byIdOfType(LookupCodes.RISK_GRADE, rd.riskGradeId());
                    x.setRiskGradeId(g.id()); x.setRiskGradeDesc(g.desc());
                    x.setRetentionAmt(rd.retentionAmt());
                    e.getRetentions().add(x);
                }
                for (var ld : nz(d.surplusLayers())) {
                    TreatySurplusLayer x = new TreatySurplusLayer();
                    x.setTreatyId(tid);
                    x.setLayerNo(ld.layerNo());
                    x.setLayerName(ld.layerName() == null || ld.layerName().isBlank() ? "Surplus layer " + ld.layerNo() : ld.layerName());
                    x.setNoOfLines(ld.noOfLines());
                    x.setRetentionAmt(ld.retentionAmt());
                    x.setCapacityAmt(ld.retentionAmt() == null || ld.noOfLines() == null ? BigDecimal.ZERO : ld.retentionAmt().multiply(BigDecimal.valueOf(ld.noOfLines())));
                    Snap c = masters.commissionType(ld.commissionTypeId());
                    x.setCommissionTypeId(c.id()); x.setCommissionTypeCode(c.code()); x.setCommissionTypeDesc(c.desc());
                    x.setCommissionPct(ld.commissionPct());
                    e.getSurplusLayers().add(x);
                }
            }
            case LookupCodes.STRAT_XOL_RISK, LookupCodes.STRAT_XOL_CAT -> {
                for (var ld : nz(d.xolLayers())) {
                    TreatyXolLayer x = new TreatyXolLayer();
                    x.setTreatyId(tid);
                    x.setLayerNo(ld.layerNo());
                    x.setLayerName(ld.layerName() == null || ld.layerName().isBlank() ? "XOL layer " + ld.layerNo() : ld.layerName());
                    x.setLimitAmt(ld.limitAmt());
                    x.setDeductibleAmt(ld.deductibleAmt());
                    x.setRateOnLinePct(ld.rateOnLinePct());
                    x.setDepositPremium(ld.depositPremium());
                    x.setMinPremium(ld.minPremium());
                    x.setEventHoursClause(ld.eventHoursClause());
                    int n = 1;
                    for (var rd : nz(ld.reinstatements())) {
                        TreatyXolReinstatement re = new TreatyXolReinstatement();
                        re.setReinstatementNo(rd.reinstatementNo() != null ? rd.reinstatementNo() : n);
                        re.setPremiumPct(rd.premiumPct());
                        Snap b = lookups.byIdOfType(LookupCodes.REINSTATEMENT_BASIS, rd.reinstatementBasisId());
                        re.setReinstatementBasisId(b.id()); re.setReinstatementBasisDesc(b.desc());
                        x.getReinstatements().add(re);
                        n++;
                    }
                    e.getXolLayers().add(x);
                }
            }
            case LookupCodes.STRAT_STOP -> {
                var sd = d.stopLoss();
                if (sd == null) return;
                TreatyStoploss x = new TreatyStoploss();
                x.setTreatyId(tid);
                x.setAttachmentLrPct(sd.attachmentLrPct());
                x.setExitLrPct(sd.exitLrPct());
                x.setLimitPctOfPremium(sd.limitPctOfPremium());
                x.setPremiumRatePct(sd.premiumRatePct());
                x.setPremiumBasisDesc(sd.premiumBasisDesc());
                e.getStopLosses().add(x);
            }
            default -> { }        // facultative residual: no terms
        }
    }

    /** Lookup value by id (checked to belong to the lookup type) or, for API clients, by code. */
    private Snap pick(String type, Long id, String code) {
        if (id != null) return lookups.byIdOfType(type, id);
        if (code != null && !code.isBlank()) return lookups.snap(type, code);
        throw ApiException.badRequest("LOOKUP_REQUIRED", type + " is required on every allocation rule");
    }

    /** Panel rows are keyed by panel group (treaty type code of the rows they serve) + layer number; the validator checks that every group used has a full panel. */
    private void addPanel(Treaty t, TreatyRequest r) {
        for (PanelDto d : nz(r.panel())) {
            if (d.layerKind() == null || d.layerKind().isBlank()) throw ApiException.badRequest("PANEL_GROUP_REQUIRED", "Every panel row needs its treaty type group (layerKind)");
            TreatyPanel e = new TreatyPanel();
            e.setPanelGroup(d.layerKind().trim().toUpperCase());
            e.setLayerNo(d.layerNo());
            Snap ri = masters.reinsurer(d.reinsurerId());
            e.setReinsurerId(ri.id()); e.setReinsurerCode(ri.code()); e.setReinsurerDesc(ri.desc());
            e.setSharePct(d.sharePct());
            e.setBrokeragePct(d.brokeragePct() == null ? BigDecimal.ZERO : d.brokeragePct());
            e.setLeadFlag(Boolean.TRUE.equals(d.leadFlag()));
            t.getPanel().add(e);
        }
    }
}
