package com.rms;

import com.rms.common.Snap;
import com.rms.common.ValidationIssue;
import com.rms.domain.*;
import com.rms.master.*;
import com.rms.repository.*;
import com.rms.allocation.AllocationPlanService;
import com.rms.allocation.PlanStep;
import com.rms.treaty.TreatyDtos.*;
import com.rms.treaty.TreatyValidator;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Runs the REAL CoverHierarchyService / TreatyValidator logic against in-memory fake repositories (java.lang.reflect.Proxy). */
public class CoverHierarchyScenarioTest {
    static int fails = 0;
    static void check(boolean c, String m) { System.out.println((c ? "ok   " : "FAIL ") + m); if (!c) fails++; }

    @SuppressWarnings("unchecked")
    static <T> T fake(Class<T> iface, Map<String, Function<Object[], Object>> impl) {
        return (T) Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[]{iface}, (p, m, a) -> {
            Function<Object[], Object> f = impl.get(m.getName());
            if (f != null) return f.apply(a);
            Class<?> r = m.getReturnType();
            if (r == Optional.class) return Optional.empty();
            if (r == List.class) return List.of();
            if (r == boolean.class) return false;
            return null;
        });
    }

    static Lob lob(long id, String code, String d) { Lob x = new Lob(); x.setId(id); x.setCode(code); x.setDescription(d); return x; }
    static Product prod(long id, long lobId, String code, String d) { Product x = new Product(); x.setId(id); x.setLobId(lobId); x.setCode(code); x.setDescription(d); return x; }
    static ProductSection sec(long id, long pid, String code, String d) { ProductSection x = new ProductSection(); x.setId(id); x.setProductId(pid); x.setCode(code); x.setDescription(d); return x; }
    static SectionCover cov(long id, long sid, String code, String d) { SectionCover x = new SectionCover(); x.setId(id); x.setSectionId(sid); x.setCode(code); x.setDescription(d); return x; }

    public static void run() {
        List<Lob> lobs = List.of(lob(1, "FIRE", "Fire & Property"), lob(2, "MOTOR", "Motor"), lob(3, "ENG", "Engineering"));
        List<Product> prods = List.of(prod(1, 1, "FIR", "Fire product"), prod(2, 2, "MOT", "Motor product"), prod(3, 3, "CAR", "Contractors all risk"));
        List<ProductSection> secs = List.of(sec(1, 1, "PD", "Property damage"), sec(2, 1, "BI", "Business interruption"), sec(3, 2, "OD", "Own damage"), sec(4, 2, "TP", "Third party"), sec(5, 3, "MD", "Material damage"), sec(6, 3, "TPL", "CAR TPL"));
        List<SectionCover> covs = List.of(cov(1, 1, "FIRE", "Fire"), cov(2, 1, "EQ", "Earthquake"), cov(3, 1, "STFI", "Storm flood"), cov(4, 2, "BIF", "Loss of profit"),
            cov(5, 3, "ODACC", "Accidental damage"), cov(6, 3, "ODTHEFT", "Theft"), cov(7, 4, "TPBI", "Third party"), cov(8, 5, "CARMD", "CAR MD"), cov(9, 6, "CARTPL", "CAR TPL cover"));

        LobRepository lobRepo = fake(LobRepository.class, Map.of("findById", x -> lobs.stream().filter(l -> l.getId().equals(x[0])).findFirst()));
        ProductRepository prodRepo = fake(ProductRepository.class, Map.of("findById", x -> prods.stream().filter(l -> l.getId().equals(x[0])).findFirst(),
            "findByLobId", x -> prods.stream().filter(p -> p.getLobId().equals(x[0])).collect(Collectors.toList())));
        ProductSectionRepository secRepo = fake(ProductSectionRepository.class, Map.of("findById", x -> secs.stream().filter(l -> l.getId().equals(x[0])).findFirst(),
            "findByProductId", x -> secs.stream().filter(s -> s.getProductId().equals(x[0])).collect(Collectors.toList())));
        SectionCoverRepository covRepo = fake(SectionCoverRepository.class, Map.of("findById", x -> covs.stream().filter(l -> l.getId().equals(x[0])).findFirst(),
            "findBySectionId", x -> covs.stream().filter(c -> c.getSectionId().equals(x[0])).collect(Collectors.toList())));

        Map<String, Function<Object[], Object>> none = Map.of();
        TreatyType qs = new TreatyType(); qs.setId(1L); qs.setCode("QS"); qs.setDescription("Quota share"); qs.setCalcStrategyCode("QuotaShareCalculator");
        TreatyType sur = new TreatyType(); sur.setId(2L); sur.setCode("SUR"); sur.setDescription("Surplus"); sur.setCalcStrategyCode("SurplusCalculator");
        TreatyType facr = new TreatyType(); facr.setId(6L); facr.setCode("FAC-RES"); facr.setDescription("Facultative residual"); facr.setCalcStrategyCode("FacResidualCalculator");
        Map<Long, TreatyType> tts = Map.of(1L, qs, 2L, sur, 6L, facr);
        TreatyTypeRepository ttRepo = fake(TreatyTypeRepository.class, Map.of("findById", x -> Optional.ofNullable(tts.get(x[0]))));
        CommissionType flat = new CommissionType(); flat.setId(1L); flat.setCode("FLAT"); flat.setDescription("Flat");
        CommissionTypeRepository ctRepo = fake(CommissionTypeRepository.class, Map.of("findById", x -> Optional.of(flat)));
        MasterSnapshotService masters = new MasterSnapshotService(ttRepo, fake(TreatyBasisRepository.class, none), fake(CessionBasisRepository.class, none), fake(CurrencyRepository.class, none),
            lobRepo, fake(BrokerRepository.class, none), fake(ReinsurerRepository.class, none), ctRepo, prodRepo, secRepo, covRepo);
        CoverHierarchyService h = new CoverHierarchyService(lobRepo, prodRepo, secRepo, covRepo, masters);

        // ---- hierarchy: expansion by level
        check(h.expand(1L, null, null, null).size() == 4, "LOB level expands to every cover of the LOB (4 fire covers)");
        check(h.expand(1L, 1L, null, null).size() == 4, "PRODUCT level expands to the product's covers");
        check(h.expand(1L, 1L, 1L, null).size() == 3, "SECTION level (property damage) expands to 3 covers");
        check(h.expand(1L, 1L, 1L, 2L).equals(Set.of(2L)), "COVER level is exactly one cover");
        check(h.expand(2L, null, null, null).size() == 3 && h.expand(3L, null, null, null).size() == 2, "motor LOB = 3 covers, engineering LOB = 2");

        // ---- hierarchy: resolve + cross-check
        var ok = h.resolve(1L, 1L, 1L, 2L, 1L);
        check(ok.ok() && "COVER".equals(ok.level()) && ok.lob().code().equals("FIRE") && ok.product().code().equals("FIR") && ok.section().code().equals("PD") && ok.cover().code().equals("EQ"), "cover level resolves full snapshots");
        var derived = h.resolve(null, null, null, 2L, 1L);
        check(derived.ok() && derived.product() != null && derived.section().code().equals("PD") && derived.lob().code().equals("FIRE"), "ancestors are derived from the deepest id");
        check(!h.resolve(1L, 2L, null, null, 1L).ok() && h.resolve(1L, 2L, null, null, 1L).errors().get(0).contains("does not belong to the selected line of business"), "product of another LOB rejected");
        check(!h.resolve(null, 1L, 3L, null, 1L).ok() && h.resolve(null, 1L, 3L, null, 1L).errors().get(0).contains("does not belong to the selected product"), "section of another product rejected");
        check(!h.resolve(null, null, 1L, 5L, 1L).ok() && h.resolve(null, null, 1L, 5L, 1L).errors().get(0).contains("does not belong to the selected section"), "cover of another section rejected");
        var wrongTreaty = h.resolve(2L, 2L, null, null, 1L);
        check(!wrongTreaty.ok() && wrongTreaty.errors().get(0).contains("treaty is for"), "mapping LOB must equal the treaty LOB");
        check(!h.resolve(null, null, null, null, 1L).ok(), "empty mapping rejected");
        check(!h.resolve(1L, 99L, null, null, 1L).ok(), "unknown product rejected");

        // ---- validator: overlap rules on covers (real TreatyValidator); each mapping row carries its own treaty type + terms
        RefLookupValue active = new RefLookupValue(); active.setId(11L); active.setCode("ACTIVE"); active.setDescription("Active");
        RefLookupValue pending = new RefLookupValue(); pending.setId(12L); pending.setCode("PENDING_APPROVAL"); pending.setDescription("Pending");
        RefLookupValueRepository lvRepo = fake(RefLookupValueRepository.class, Map.of("findByTypeCodeAndCode", x -> "ACTIVE".equals(x[1]) ? Optional.of(active) : Optional.of(pending)));
        LookupService lookups = new LookupService(lvRepo, fake(RefLookupTypeRepository.class, none));
        TreatyCoverMap other = new TreatyCoverMap(); other.setTreatyCode("FIRE-SUR-2026"); other.setLobId(1L); other.setProductId(1L); other.setSequenceNo(1);
        TreatyCoverMapRepository cmRepo = fake(TreatyCoverMapRepository.class, Map.of("findCandidates", x -> ((Integer) x[0]) == 1 ? List.of(other) : List.of()));
        Treaty otherTreaty = new Treaty(); otherTreaty.setId(50L); otherTreaty.setTreatyCode("OTHER-PROG");
        TreatyRepository trRepo = fake(TreatyRepository.class, Map.of("findById", x -> Optional.of(otherTreaty)));
        AllocationPlanService plans = new AllocationPlanService(cmRepo, trRepo, h, lookups, masters);
        TreatyValidator v = new TreatyValidator(masters, ctRepo, secRepo, covRepo, cmRepo, lookups, h, plans);

        LocalDate f = LocalDate.of(2027, 1, 1), t = LocalDate.of(2027, 12, 31);
        BigDecimal B30 = BigDecimal.valueOf(30), B70 = BigDecimal.valueOf(70), B25 = BigDecimal.valueOf(25);
        QsParamsDto qsp = new QsParamsDto(null, B30, B70, null, null, 1L, "FLAT", "Flat", B25, null);
        List<RetentionDto> rets = List.of(new RetentionDto(null, 1L, "A", BigDecimal.valueOf(4_000_000)));
        List<SurplusLayerDto> sl = List.of(new SurplusLayerDto(null, 1, "1st", 4, BigDecimal.valueOf(4_000_000), null, 1L, "Flat", B25),
                                           new SurplusLayerDto(null, 2, "2nd", 6, BigDecimal.valueOf(4_000_000), null, 1L, "Flat", BigDecimal.valueOf(22.5)));
        java.util.function.Function<String[], AllocationRuleDto> rule = a2 -> new AllocationRuleDto(null, 1, null, a2[0], null, null, a2[1], null, null, null, null, null, a2[2], null, true, null, null, null);
        // row(lob, product, section, cover, seq, typeId, method, basis, residual)
        java.util.function.Function<Object[], CoverMapDto> row = o -> {
            long ty = o.length > 5 ? (Long) o[5] : 1L;
            String[] r3 = o.length > 6 ? new String[]{(String) o[6], (String) o[7], (String) o[8]} : new String[]{"QS", "GROSS", "CONTINUE"};
            return new CoverMapDto(null, null, null, (Long) o[0], null, (Long) o[1], null, (Long) o[2], null, (Long) o[3], null, (Integer) o[4], f, t, null, List.of(rule.apply(r3)),
                ty, null, null, null, ty == 1L ? qsp : null, ty == 2L ? rets : List.of(), ty == 2L ? sl : List.of(), List.of(), null, null);
        };
        java.util.function.BiFunction<TreatyValidator, Object[], List<ValidationIssue>> runAll = (val, spec) -> {
            @SuppressWarnings("unchecked") List<CoverMapDto> maps = (List<CoverMapDto>) spec[0];
            @SuppressWarnings("unchecked") List<PanelDto> panel = spec.length > 2 ? (List<PanelDto>) spec[2] : List.of(new PanelDto(null, "QS", null, 1L, "ALP", "Alpine", BigDecimal.valueOf(100), BigDecimal.ZERO, true));
            return val.validate(new TreatyRequest("NEW-PROG-2027", "New programme", 1L, 1L, 1L, (Long) spec[1], 1L, f, t, f, null, null, null, null, panel, List.of(), maps));
        };
        java.util.function.BiFunction<TreatyValidator, Object[], List<ValidationIssue>> runWith = (val, spec) -> runAll.apply(val, spec).stream().filter(i -> i.field().startsWith("coverMap")).toList();
        java.util.function.BiFunction<List<CoverMapDto>, Long, List<ValidationIssue>> run = (maps, lobId) -> runWith.apply(v, new Object[]{maps, lobId});

        var i1 = run.apply(List.of(row.apply(new Object[]{1L, null, null, null, 1})), 1L);
        check(i1.stream().anyMatch(i -> i.isError() && i.message().contains("already used by treaty FIRE-SUR-2026")), "LOB-level row overlaps another treaty's product-level mapping at the same sequence");
        var i2 = run.apply(List.of(row.apply(new Object[]{1L, null, null, null, 2})), 1L);
        check(i2.stream().noneMatch(ValidationIssue::isError), "same covers at a different sequence is allowed");
        var i3 = run.apply(List.of(row.apply(new Object[]{1L, 1L, 1L, 2L, 2}), row.apply(new Object[]{1L, 1L, 1L, null, 2})), 1L);
        check(i3.stream().noneMatch(ValidationIssue::isError) && i3.stream().anyMatch(i -> !i.isError() && i.message().contains("Row 1 (cover level) overrides row 2 (section level)")),
            "same type: cover row inside a section row of the same treaty = override (warning)");
        var i3b = run.apply(List.of(row.apply(new Object[]{1L, 1L, 1L, null, 2}), row.apply(new Object[]{1L, 1L, 1L, null, 3})), 1L);
        check(i3b.stream().anyMatch(i -> i.isError() && i.message().contains("at the same level")), "same type twice at the same level on the same covers clashes (even at different sequences)");
        var mixed = run.apply(List.of(row.apply(new Object[]{1L, 1L, null, null, 2}),
            row.apply(new Object[]{1L, 1L, 1L, null, 3, 2L, "SURPLUS", "NET_AFTER_QS", "CONTINUE"}),
            row.apply(new Object[]{1L, 1L, null, null, 4, 6L, "FAC", "RESIDUAL", "FACULTATIVE"})), 1L);
        check(mixed.stream().noneMatch(ValidationIssue::isError), "ONE treaty with quota share (product, seq 2) + surplus (section, seq 3) + fac residual (seq 4) on the same product is valid");
        var sameSeq = run.apply(List.of(row.apply(new Object[]{1L, 1L, null, null, 2}), row.apply(new Object[]{1L, 1L, 1L, null, 2, 2L, "SURPLUS", "NET_AFTER_QS", "CONTINUE"})), 1L);
        check(sameSeq.stream().anyMatch(i -> i.isError() && i.message().contains("same sequence 2")), "two types of one treaty at the same sequence on the same cover clash");
        var i4 = run.apply(List.of(row.apply(new Object[]{2L, 2L, null, null, 2})), 1L);
        check(i4.stream().anyMatch(i -> i.isError() && i.message().contains("treaty is for")), "wrong-LOB mapping reported by the validator too");
        var i5 = run.apply(List.of(), 1L);
        check(i5.stream().anyMatch(i -> !i.isError() && i.message().contains("will not be applied")), "no mapping at all is a warning");
        var i6 = run.apply(List.of(row.apply(new Object[]{1L, 1L, 2L, null, 2}), row.apply(new Object[]{1L, 1L, 1L, null, 2})), 1L);
        check(i6.stream().noneMatch(ValidationIssue::isError), "two different sections of one product at the same sequence do not clash");

        // ---- row type and terms
        CoverMapDto noType = new CoverMapDto(null, null, null, 1L, null, null, null, null, null, null, null, 2, f, t, null, List.of(rule.apply(new String[]{"QS", "GROSS", "CONTINUE"})),
            null, null, null, null, null, List.of(), List.of(), List.of(), null, null);
        check(run.apply(List.of(noType), 1L).stream().anyMatch(i -> i.isError() && i.message().contains("Treaty type is required on every mapping row")), "a row without a treaty type is an error");
        CoverMapDto qsNoTerms = new CoverMapDto(null, null, null, 1L, null, null, null, null, null, null, null, 2, f, t, null, List.of(rule.apply(new String[]{"QS", "GROSS", "CONTINUE"})),
            1L, null, null, null, null, List.of(), List.of(), List.of(), null, null);
        check(run.apply(List.of(qsNoTerms), 1L).stream().anyMatch(i -> i.isError() && i.field().equals("coverMap[0].qsParams")), "a quota share row needs its cession / commission terms");
        CoverMapDto surNoTerms = new CoverMapDto(null, null, null, 1L, null, null, null, null, null, null, null, 2, f, t, null, List.of(rule.apply(new String[]{"SURPLUS", "GROSS", "CONTINUE"})),
            2L, null, null, null, null, List.of(), List.of(), List.of(), null, null);
        var sn = run.apply(List.of(surNoTerms), 1L);
        check(sn.stream().anyMatch(i -> i.isError() && i.field().equals("coverMap[0].retentions")) && sn.stream().anyMatch(i -> i.isError() && i.field().equals("coverMap[0].surplusLayers")),
            "a surplus row needs its retention and layers");
        CoverMapDto badCess = new CoverMapDto(null, null, null, 1L, null, null, null, null, null, null, null, 2, f, t, null, List.of(rule.apply(new String[]{"QS", "GROSS", "CONTINUE"})),
            1L, null, null, null, new QsParamsDto(null, BigDecimal.valueOf(130), BigDecimal.valueOf(-30), null, null, 1L, "FLAT", "Flat", B25, null), List.of(), List.of(), List.of(), null, null);
        check(run.apply(List.of(badCess), 1L).stream().anyMatch(i -> i.isError() && i.message().contains("Cession % must be between 0 and 100")), "row cession % is range-checked");

        // ---- panel groups follow the row types and layers
        var pn = runAll.apply(v, new Object[]{List.of(row.apply(new Object[]{1L, 1L, null, null, 2}), row.apply(new Object[]{1L, 1L, 1L, null, 3, 2L, "SURPLUS", "NET_AFTER_QS", "CONTINUE"}),
            row.apply(new Object[]{1L, 1L, null, null, 4, 6L, "FAC", "RESIDUAL", "FACULTATIVE"})), 1L});
        check(pn.stream().anyMatch(i -> i.isError() && i.message().contains("No reinsurers on the panel for SUR layer 1")) && pn.stream().anyMatch(i -> i.message().contains("SUR layer 2"))
            && pn.stream().noneMatch(i -> i.message().contains("panel for QS")) && pn.stream().noneMatch(i -> i.message().contains("FAC-RES")),
            "panel needed for QS and each surplus layer used; facultative residual panel optional");
        var stray = runAll.apply(v, new Object[]{List.of(row.apply(new Object[]{1L, 1L, null, null, 2})), 1L,
            List.of(new PanelDto(null, "QS", null, 1L, "ALP", "Alpine", BigDecimal.valueOf(100), BigDecimal.ZERO, true), new PanelDto(null, "XOL-CAT", 1, 1L, "ALP", "Alpine", BigDecimal.valueOf(100), BigDecimal.ZERO, true))});
        check(stray.stream().anyMatch(i -> i.isError() && i.message().contains("XOL-CAT layer 1 but no mapping row uses it")), "panel rows for a type no row uses are rejected");

        // ---- allocation rules (treaty_cover_allocation_rule)
        CoverMapDto noRules = new CoverMapDto(null, null, null, 1L, null, null, null, null, null, null, null, 2, f, t, null, List.of(), 1L, null, null, null, qsp, List.of(), List.of(), List.of(), null, null);
        check(run.apply(List.of(noRules), 1L).stream().anyMatch(i -> i.isError() && i.message().contains("at least one allocation rule")), "a mapping without a rule is an error");
        CoverMapDto wrongMethod = row.apply(new Object[]{1L, null, null, null, 2, 1L, "SURPLUS", "GROSS", "CONTINUE"});
        check(run.apply(List.of(wrongMethod), 1L).stream().anyMatch(i -> i.isError() && i.message().contains("cannot be used by a Quota share row")), "a surplus method on a quota share row is rejected");
        CoverMapDto badPct = new CoverMapDto(null, null, null, 1L, null, null, null, null, null, null, null, 2, f, t, null,
            List.of(new AllocationRuleDto(null, 1, null, "QS", null, null, "GROSS", null, BigDecimal.valueOf(120), BigDecimal.valueOf(500), BigDecimal.valueOf(100), null, "CONTINUE", null, true, null, null, null)),
            1L, null, null, null, qsp, List.of(), List.of(), List.of(), null, null);
        var pctIssues = run.apply(List.of(badPct), 1L);
        check(pctIssues.stream().anyMatch(i -> i.message().contains("Allocation % must be above 0")) && pctIssues.stream().anyMatch(i -> i.message().contains("Minimum allocation is above the maximum")), "allocation % and min/max caps validated");

        // ---- chain ordering (basis needs an earlier step of that method)
        CoverMapDto needsQs = row.apply(new Object[]{1L, null, null, null, 2, 1L, "QS", "NET_AFTER_QS", "CONTINUE"});
        check(run.apply(List.of(needsQs), 1L).stream().anyMatch(i -> i.isError() && i.message().contains("NET_AFTER_QS but no quota share step runs earlier")), "basis NET_AFTER_QS with no earlier QS step is an error");
        // another ACTIVE treaty already provides a QS step at sequence 1 on the same covers
        TreatyCoverAllocationRule qsRule = new TreatyCoverAllocationRule(); qsRule.setRuleNo(1); qsRule.setAllocationMethodCode("QS"); qsRule.setAllocationMethodDesc("Quota share");
        qsRule.setAllocationBasisCode("GROSS"); qsRule.setAllocationBasisDesc("Gross"); qsRule.setResidualActionCode("CONTINUE"); qsRule.setResidualActionDesc("Continue"); qsRule.setStopOnLimitFlag(true);
        java.util.function.Function<Object[], TreatyCoverMap> map = o -> {        // id, product, section, cover, seq, typeId, rule
            TreatyCoverMap m = new TreatyCoverMap(); m.setId((Long) o[0]); m.setTreatyId(50L); m.setTreatyCode("OTHER-PROG"); m.setLobId(1L); m.setLobDesc("Fire");
            m.setProductId((Long) o[1]); m.setSectionId((Long) o[2]); m.setCoverId((Long) o[3]); m.setSequenceNo((Integer) o[4]);
            TreatyType ty = tts.get((Long) o[5]); m.setTreatyTypeId(ty.getId()); m.setTreatyTypeCode(ty.getCode()); m.setTreatyTypeDesc(ty.getDescription());
            m.setMapLevelDesc(o[3] != null ? "Cover" : o[2] != null ? "Section" : o[1] != null ? "Product" : "Line of business"); m.setCoverDesc(o[3] != null ? "Earthquake" : null);
            m.setEffectiveFrom(LocalDate.of(2026, 1, 1)); m.setEffectiveTo(LocalDate.of(2030, 12, 31)); m.getRules().add((TreatyCoverAllocationRule) o[6]);
            return m;
        };
        TreatyCoverMap qsMap = map.apply(new Object[]{70L, null, null, null, 1, 1L, qsRule});
        TreatyParamQs q20 = new TreatyParamQs(); q20.setCessionPct(BigDecimal.valueOf(20)); q20.setFlatCommissionPct(BigDecimal.valueOf(27.5)); qsMap.getQsParams().add(q20);
        TreatyCoverMapRepository cmRepo2 = fake(TreatyCoverMapRepository.class, Map.of("findInForce", x -> List.of(qsMap)));
        TreatyValidator v2 = new TreatyValidator(masters, ctRepo, secRepo, covRepo, cmRepo2, lookups, h, new AllocationPlanService(cmRepo2, trRepo, h, lookups, masters));
        check(runWith.apply(v2, new Object[]{List.of(needsQs), 1L}).stream().noneMatch(i -> i.isError()), "NET_AFTER_QS is fine once another treaty runs a QS step earlier on the same covers");
        CoverMapDto grossAgain = row.apply(new Object[]{1L, null, null, null, 2});
        check(runWith.apply(v2, new Object[]{List.of(grossAgain), 1L}).stream().anyMatch(i -> !i.isError() && i.message().contains("applied to GROSS although earlier steps")), "GROSS after an earlier allocation warns about double allocation");
        check(plans.otherTreatySteps(1L, LocalDate.of(2027, 3, 1), "ZZZ").isEmpty(), "plan service returns nothing when no treaty is in force");
        var steps2 = new AllocationPlanService(cmRepo2, trRepo, h, lookups, masters).otherTreatySteps(1L, LocalDate.of(2027, 3, 1), "ZZZ");
        check(steps2.size() == 1 && steps2.get(0).treatyCode().equals("OTHER-PROG") && steps2.get(0).methodCode().equals("QS") && steps2.get(0).strategy().equals("QuotaShareCalculator")
            && "Quota share".equals(steps2.get(0).treatyTypeDesc()), "plan service builds the step from map (type) + rule + treaty");
        check(new AllocationPlanService(cmRepo2, trRepo, h, lookups, masters).otherTreatySteps(1L, LocalDate.of(2027, 3, 1), "OTHER-PROG").isEmpty(), "the treaty being edited is excluded from its own plan");
        check(new AllocationPlanService(cmRepo2, trRepo, h, lookups, masters).otherTreatySteps(5L, LocalDate.of(2027, 3, 1), "ZZZ").isEmpty(), "a motor cover is not part of a fire line-of-business mapping");

        // ---- engine on ONE treaty with several types: QS 20% (product, seq 1) -> surplus on property damage (seq 2, retention 4m, 4 lines) with an
        //      earthquake cover row (retention 2m, 3 lines) overriding it -> facultative residual (seq 3)
        TreatyCoverAllocationRule surRule = new TreatyCoverAllocationRule(); surRule.setRuleNo(1); surRule.setAllocationMethodCode("SURPLUS"); surRule.setAllocationMethodDesc("Surplus");
        surRule.setAllocationBasisCode("NET_AFTER_QS"); surRule.setAllocationBasisDesc("Net after QS"); surRule.setResidualActionCode("CONTINUE"); surRule.setResidualActionDesc("Continue"); surRule.setStopOnLimitFlag(true);
        TreatyCoverAllocationRule facRule = new TreatyCoverAllocationRule(); facRule.setRuleNo(1); facRule.setAllocationMethodCode("FAC"); facRule.setAllocationMethodDesc("Facultative");
        facRule.setAllocationBasisCode("RESIDUAL"); facRule.setAllocationBasisDesc("Residual"); facRule.setResidualActionCode("FACULTATIVE"); facRule.setResidualActionDesc("Place"); facRule.setStopOnLimitFlag(true);
        TreatyCoverMap surSec = map.apply(new Object[]{71L, 1L, 1L, null, 2, 2L, surRule});
        TreatySurplusRetention r4 = new TreatySurplusRetention(); r4.setRetentionAmt(BigDecimal.valueOf(4_000_000)); surSec.getRetentions().add(r4);
        TreatySurplusLayer l1 = new TreatySurplusLayer(); l1.setLayerNo(1); l1.setLayerName("1st"); l1.setNoOfLines(4); l1.setCommissionPct(B25); surSec.getSurplusLayers().add(l1);
        TreatyCoverMap surEq = map.apply(new Object[]{72L, 1L, 1L, 2L, 2, 2L, surRule});
        TreatySurplusRetention r2 = new TreatySurplusRetention(); r2.setRetentionAmt(BigDecimal.valueOf(2_000_000)); surEq.getRetentions().add(r2);
        TreatySurplusLayer e1 = new TreatySurplusLayer(); e1.setLayerNo(1); e1.setLayerName("EQ 1st"); e1.setNoOfLines(3); e1.setCommissionPct(BigDecimal.valueOf(20)); surEq.getSurplusLayers().add(e1);
        TreatyCoverMap facMap = map.apply(new Object[]{73L, 1L, null, null, 3, 6L, facRule});
        TreatyCoverMapRepository cmRepo3 = fake(TreatyCoverMapRepository.class, Map.of("findInForce", x -> List.of(qsMap, surSec, surEq, facMap)));
        AllocationPlanService plans3 = new AllocationPlanService(cmRepo3, trRepo, h, lookups, masters);
        LocalDate d = LocalDate.of(2027, 3, 1);
        BigDecimal si = BigDecimal.valueOf(100_000_000), pr = BigDecimal.valueOf(200_000);
        var fire = plans3.simulate(1L, d, si, pr, null);
        check(fire.plan().size() == 3 && fire.plan().stream().map(PlanStep::methodCode).toList().equals(List.of("QS", "SURPLUS", "FAC")), "Fire cover: one treaty runs QS -> surplus -> FAC (three types against the same product)");
        check(fire.outcome().steps().get(0).allocated().compareTo(BigDecimal.valueOf(20_000_000)) == 0 && fire.outcome().steps().get(1).allocated().compareTo(BigDecimal.valueOf(16_000_000)) == 0
            && fire.outcome().facPlaced().compareTo(BigDecimal.valueOf(60_000_000)) == 0 && fire.outcome().retained().compareTo(BigDecimal.valueOf(4_000_000)) == 0,
            "Fire 100m: QS 20m, surplus 16m (4m x 4 lines on the 80m net after QS), fac 60m, retained 4m");
        check(fire.outcome().steps().get(0).commission().compareTo(new BigDecimal("11000.00")) == 0, "QS row commission 27.5% of 40,000 = 11,000");
        var eq = plans3.simulate(2L, d, si, pr, null);
        check(eq.plan().size() == 3 && eq.plan().get(1).coverMapId() == 72L && eq.outcome().steps().get(1).allocated().compareTo(BigDecimal.valueOf(6_000_000)) == 0
            && eq.outcome().retained().compareTo(BigDecimal.valueOf(2_000_000)) == 0,
            "Earthquake: the cover-level surplus row overrides the section row (2m x 3 lines = 6m), QS and FAC rows still apply");
        check(eq.outcome().steps().get(1).notes().stream().anyMatch(x -> x.contains("Cover row: Earthquake")), "the step says which mapping row supplied its type and terms");

        System.out.println(fails == 0 ? "ALL PASSED" : fails + " FAILED");
        if (fails > 0) throw new AssertionError(fails + " check(s) failed");
    }
}
