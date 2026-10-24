package com.rms;

import com.rms.common.ApiException;
import com.rms.allocation.AllocationPlanService;
import com.rms.domain.*;
import com.rms.master.*;
import com.rms.repository.*;
import com.rms.treaty.*;
import com.rms.treaty.TreatyDtos.*;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Treaty create / update / amend semantics through the REAL TreatyService, TreatyMapper and TreatyValidator with an in-memory TreatyRepository. */
public class TreatyServiceScenarioTest {
    static int fails = 0;
    static void check(boolean c, String m) { System.out.println((c ? "ok   " : "FAIL ") + m); if (!c) fails++; }
    static BigDecimal n(double d) { return BigDecimal.valueOf(d); }

    @SuppressWarnings("unchecked")
    static <T> T fake(Class<T> iface, Map<String, Function<Object[], Object>> impl) {
        return (T) Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[]{iface}, (p, m, a) -> {
            Function<Object[], Object> f = impl.get(m.getName());
            if (f != null) return f.apply(a);
            Class<?> r = m.getReturnType();
            if (r == Optional.class) return Optional.empty();
            if (r == List.class) return List.of();
            if (r == boolean.class) return false;
            if (r == int.class) return 0;
            return null;
        });
    }
    static long seq = 1000;
    static void ids(Object o) { }

    static ApiException expect(Runnable r) { try { r.run(); return null; } catch (ApiException e) { return e; } }

    public static void run() throws Exception {
        // ---------- masters
        Map<String, Function<Object[], Object>> none = Map.of();
        TreatyType sur = new TreatyType(); sur.setId(2L); sur.setCode("SUR"); sur.setDescription("Surplus"); sur.setCalcStrategyCode("SurplusCalculator");
        Lob fire = new Lob(); fire.setId(1L); fire.setCode("FIRE"); fire.setDescription("Fire & Property");
        Lob motor = new Lob(); motor.setId(2L); motor.setCode("MOTOR"); motor.setDescription("Motor");
        Product pf = new Product(); pf.setId(1L); pf.setLobId(1L); pf.setCode("FIR"); pf.setDescription("Fire product");
        Product pm = new Product(); pm.setId(2L); pm.setLobId(2L); pm.setCode("MOT"); pm.setDescription("Motor product");
        ProductSection s1 = new ProductSection(); s1.setId(1L); s1.setProductId(1L); s1.setCode("PD"); s1.setDescription("Property damage");
        SectionCover c1 = new SectionCover(); c1.setId(1L); c1.setSectionId(1L); c1.setCode("FIRE"); c1.setDescription("Fire");
        CommissionType flat = new CommissionType(); flat.setId(1L); flat.setCode("FLAT"); flat.setDescription("Flat");
        TreatyBasis tb = new TreatyBasis(); tb.setId(1L); tb.setCode("RA"); tb.setDescription("Risk attaching");
        CessionBasis cb = new CessionBasis(); cb.setId(1L); cb.setCode("SI"); cb.setDescription("Sum insured");
        com.rms.domain.Currency inr = new com.rms.domain.Currency(); inr.setId(1L); inr.setCode("INR"); inr.setName("Indian Rupee");
        Broker brk = new Broker(); brk.setId(1L); brk.setCode("TRI"); brk.setName("Trident");
        Reinsurer r1 = new Reinsurer(); r1.setId(1L); r1.setCode("ALP"); r1.setName("Alpine Re");
        Reinsurer r2 = new Reinsurer(); r2.setId(2L); r2.setCode("PAC"); r2.setName("Pacific Re");
        Reinsurer r4 = new Reinsurer(); r4.setId(4L); r4.setCode("MER"); r4.setName("Meridian Re");
        List<Lob> lobs = List.of(fire, motor); List<Product> prods = List.of(pf, pm);
        LobRepository lobRepo = fake(LobRepository.class, Map.of("findById", x -> lobs.stream().filter(l -> l.getId().equals(x[0])).findFirst()));
        ProductRepository prodRepo = fake(ProductRepository.class, Map.of("findById", x -> prods.stream().filter(l -> l.getId().equals(x[0])).findFirst(),
            "findByLobId", x -> prods.stream().filter(p -> p.getLobId().equals(x[0])).collect(Collectors.toList())));
        ProductSectionRepository secRepo = fake(ProductSectionRepository.class, Map.of("findById", x -> Optional.ofNullable(x[0].equals(1L) ? s1 : null), "findByProductId", x -> x[0].equals(1L) ? List.of(s1) : List.of()));
        SectionCoverRepository covRepo = fake(SectionCoverRepository.class, Map.of("findById", x -> Optional.ofNullable(x[0].equals(1L) ? c1 : null), "findBySectionId", x -> x[0].equals(1L) ? List.of(c1) : List.of()));
        List<Reinsurer> ris = List.of(r1, r2, r4);
        TreatyType qsT = new TreatyType(); qsT.setId(1L); qsT.setCode("QS"); qsT.setDescription("Quota share"); qsT.setCalcStrategyCode("QuotaShareCalculator");
        MasterSnapshotService masters = new MasterSnapshotService(fake(TreatyTypeRepository.class, Map.of("findById", x -> Optional.of(Long.valueOf(1L).equals(x[0]) ? qsT : sur))),
            fake(TreatyBasisRepository.class, Map.of("findById", x -> Optional.of(tb))), fake(CessionBasisRepository.class, Map.of("findById", x -> Optional.of(cb))),
            fake(CurrencyRepository.class, Map.of("findById", x -> Optional.of(inr))), lobRepo, fake(BrokerRepository.class, Map.of("findById", x -> Optional.of(brk))),
            fake(ReinsurerRepository.class, Map.of("findById", x -> ris.stream().filter(r -> r.getId().equals(x[0])).findFirst())),
            fake(CommissionTypeRepository.class, Map.of("findById", x -> Optional.of(flat))), prodRepo, secRepo, covRepo);

        // ---------- lookups
        String[][] defs = {{"1", "TREATY_STATUS", "DRAFT", "PENDING_APPROVAL", "ACTIVE", "RUN_OFF", "SUPERSEDED", "CLOSED"}, {"2", "AMEND_MODE", "PROSPECTIVE", "RECESSION"},
            {"3", "MAP_LEVEL", "LOB", "PRODUCT", "SECTION", "COVER"}, {"4", "ALLOCATION_METHOD", "QS", "SURPLUS", "XOL", "FAC", "STOP_LOSS"}, {"5", "RISK_GRADE", "A", "B"},
            {"6", "ALLOCATION_BASIS", "GROSS", "NET_AFTER_QS", "NET_AFTER_SURPLUS", "NET_AFTER_QS_SURPLUS", "NET_AFTER_XOL", "NET_RETAINED", "RESIDUAL", "LOSS_AMOUNT"},
            {"7", "RESIDUAL_ACTION", "CONTINUE", "RETAIN", "FACULTATIVE", "ERROR", "UNALLOCATED"}};
        List<RefLookupValue> values = new ArrayList<>(); Map<String, Long> typeIds = new HashMap<>();
        for (String[] d : defs) { long t = Long.parseLong(d[0]); typeIds.put(d[1], t);
            for (int i = 2; i < d.length; i++) { RefLookupValue v = new RefLookupValue(); v.setId(t * 100 + i - 1); v.setTypeId(t); v.setCode(d[i]); v.setDescription(d[i]); values.add(v); } }
        RefLookupValueRepository lvRepo = fake(RefLookupValueRepository.class, Map.of("findById", x -> values.stream().filter(v -> v.getId().equals(x[0])).findFirst(),
            "findByTypeCodeAndCode", x -> values.stream().filter(v -> v.getTypeId().equals(typeIds.get(x[0])) && v.getCode().equals(x[1])).findFirst()));
        RefLookupTypeRepository ltRepo = fake(RefLookupTypeRepository.class, Map.of("findByCode", x -> { RefLookupType t = new RefLookupType(); t.setId(typeIds.get(x[0])); t.setCode((String) x[0]); return Optional.of(t); }));
        LookupService lookups = new LookupService(lvRepo, ltRepo);
        long DRAFT = lookups.snap("TREATY_STATUS", "DRAFT").id(), ACTIVE = lookups.snap("TREATY_STATUS", "ACTIVE").id(), PENDING = lookups.snap("TREATY_STATUS", "PENDING_APPROVAL").id();

        // ---------- in-memory TreatyRepository
        Map<Long, Treaty> store = new LinkedHashMap<>(); long[] next = {1};
        Function<Object[], Object> save = x -> {
            Treaty t = (Treaty) x[0];
            if (t.getId() == null) { t.setId(next[0]++); store.put(t.getId(), t); }
            t.getPanel().forEach(p -> { if (p.getId() == null) p.setId(seq++); });
            t.getCoverMaps().forEach(m -> { if (m.getId() == null) m.setId(seq++); m.getSurplusLayers().forEach(l -> { if (l.getId() == null) l.setId(seq++); });
                m.getQsParams().forEach(q -> { if (q.getId() == null) q.setId(seq++); }); m.getRetentions().forEach(q -> { if (q.getId() == null) q.setId(seq++); }); });
            return t; };
        Map<String, Function<Object[], Object>> tr = new HashMap<>();
        tr.put("findById", x -> Optional.ofNullable(store.get(x[0]))); tr.put("save", save); tr.put("saveAndFlush", save);
        tr.put("existsByTreatyCode", x -> store.values().stream().anyMatch(t -> t.getTreatyCode().equals(x[0])));
        tr.put("maxVersion", x -> store.values().stream().filter(t -> t.getTreatyCode().equals(x[0])).mapToInt(Treaty::getVersionNo).max().orElse(0));
        tr.put("existsByTreatyCodeAndStatusIdInAndRecordStatusNot", x -> store.values().stream().anyMatch(t -> t.getTreatyCode().equals(x[0]) && ((Collection<?>) x[1]).contains(t.getStatusId()) && !t.getRecordStatus().equals(x[2])));
        tr.put("findByTreatyCodeAndRecordStatusNotOrderByVersionNoDesc", x -> store.values().stream().filter(t -> t.getTreatyCode().equals(x[0])).sorted(Comparator.comparing(Treaty::getVersionNo).reversed()).collect(Collectors.toList()));
        TreatyRepository repo = fake(TreatyRepository.class, tr);

        CommissionTypeRepository ctRepo = fake(CommissionTypeRepository.class, Map.of("findById", x -> Optional.of(flat)));
        CoverHierarchyService h = new CoverHierarchyService(lobRepo, prodRepo, secRepo, covRepo, masters);
        TreatyCoverMapRepository cmRepo = fake(TreatyCoverMapRepository.class, none);
        TreatyValidator validator = new TreatyValidator(masters, ctRepo, secRepo, covRepo, cmRepo, lookups, h, new AllocationPlanService(cmRepo, repo, h, lookups, masters));
        TreatyMapper mapper = new TreatyMapper(lookups, masters, h);
        TreatyService svc = new TreatyService(repo, mapper, validator, new TreatySimulator(masters), masters, h, lookups, null, new com.fasterxml.jackson.databind.ObjectMapper(), 2);

        LocalDate f = LocalDate.of(2027, 1, 1), t = LocalDate.of(2027, 12, 31);
        AllocationRuleDto surRule = new AllocationRuleDto(null, null, null, "SURPLUS", null, null, "GROSS", null, null, null, null, null, "CONTINUE", null, null, "overflow continues to fac", null, null);
        java.util.function.BiFunction<Integer, Integer, List<SurplusLayerDto>> layers = (l1, l2) -> l2 == null
            ? List.of(new SurplusLayerDto(null, 1, "1st Surplus", l1, n(5_000_000), null, 1L, null, n(25)))
            : List.of(new SurplusLayerDto(null, 1, "1st Surplus", l1, n(5_000_000), null, 1L, null, n(25)), new SurplusLayerDto(null, 2, "2nd Surplus", l2, n(5_000_000), null, 1L, null, n(22.5)));
        // a surplus mapping row on the fire product (lobId lets a test send a wrong-LOB row)
        java.util.function.BiFunction<Long, List<SurplusLayerDto>, CoverMapDto> surRow = (lobId, ls) -> new CoverMapDto(null, null, null, lobId, null, lobId == 1L ? 1L : 2L, null, null, null, null, null, 1, f, t, null,
            List.of(surRule), 2L, null, null, null, null, List.of(new RetentionDto(null, 501L, "A", n(5_000_000))), ls, List.of(), null, "fire product");
        List<PanelDto> panel2 = List.of(new PanelDto(null, "SUR", 1, 1L, null, null, n(60), n(1), true), new PanelDto(null, "SUR", 1, 2L, null, null, n(40), n(1), false),
            new PanelDto(null, "SUR", 2, 1L, null, null, n(50), n(1), true), new PanelDto(null, "SUR", 2, 4L, null, null, n(50), n(1), false));
        Function<Long, TreatyRequest> req = lobId -> new TreatyRequest("fire-sur-2027", "Fire Surplus 2027", 1L, 1L, 1L, 1L, 1L, f, t, null, null, "remarks", null, null,
            panel2, List.of(new ExclusionDto(null, "Fireworks", "Refer to facultative")), List.of(surRow.apply(lobId, layers.apply(4, 6))));

        // ---------- create
        var created = svc.create(req.apply(1L));
        var v1 = created.treaty();
        check("FIRE-SUR-2027".equals(v1.treatyCode()) && v1.versionNo() == 1 && "DRAFT".equals(v1.status().code()) && v1.editable(), "create: upper-cased code, version 1, DRAFT, editable");
        check(v1.treatyTypes().size() == 1 && "Surplus".equals(v1.treatyTypes().get(0).desc()) && "Fire & Property".equals(v1.lob().desc()) && "Alpine Re".equals(v1.panel().get(0).reinsurerDesc()),
            "create: treaty types come from the mapping rows; master snapshots stored");
        var row0 = v1.coverMap().get(0);
        check("SUR".equals(row0.treatyTypeCode()) && "SurplusCalculator".equals(row0.calcStrategyCode()) && row0.surplusLayers().get(1).capacityAmt().compareTo(n(30_000_000)) == 0,
            "create: the row stores its type (id + code + desc) and its own layers; capacity = retention x lines computed on the server (30,000,000)");
        check(store.get(v1.id()).getTreatyTypeId() == null, "create: the treaty header carries no type any more");
        check(v1.panel().stream().filter(p -> "SUR".equals(p.layerKind()) && p.layerNo() == 2).count() == 2, "create: panel rows grouped by row type + layer (SUR layer 2)");
        check("PRODUCT".equals(row0.levelCode()) && row0.coverCount() == 1 && "fire product".equals(row0.note()), "create: mapping level derived (PRODUCT), expanded to covers, note kept");
        var rl = row0.rules().get(0);
        check(rl.ruleNo() == 1 && rl.methodId() != null && "SURPLUS".equals(rl.methodCode()) && "SURPLUS".equals(rl.methodDesc()) && "GROSS".equals(rl.basisCode()) && "CONTINUE".equals(rl.residualActionCode()) && Boolean.TRUE.equals(rl.stopOnLimit()), "create: allocation rule stored with method/basis/residual id + code + description");
        check(rl.effectiveFrom().equals(LocalDate.of(2027, 1, 1)) && rl.effectiveTo().equals(LocalDate.of(2027, 12, 31)), "create: rule dates default to the mapping period");
        check(created.issues().stream().noneMatch(i -> i.isError()), "create: no validation errors on a consistent treaty");
        check("DUP".equals(Optional.ofNullable(expect(() -> svc.create(req.apply(1L)))).map(e -> "DUP").orElse("none")), "create: duplicate code rejected");
        var wrongLob = expect(() -> svc.create(new TreatyRequest("FIRE-X", "x", 1L, 1L, 1L, 1L, 1L, f, t, null, null, null, null, null, List.of(), List.of(),
            List.of(surRow.apply(2L, layers.apply(4, null))))));
        check(wrongLob != null && "COVER_MAP_INVALID".equals(wrongLob.getCode()) && wrongLob.getMessage().contains("treaty is for"), "create: motor mapping on a fire treaty rejected with COVER_MAP_INVALID");

        // ---------- update a DRAFT: in place
        final TreatyRequest edited = new TreatyRequest("FIRE-SUR-2027", "Fire Surplus 2027 (revised)", 1L, 1L, 1L, 1L, 1L, f, t, null, null, "r2", null, null,
            List.of(new PanelDto(null, "SUR", 1, 1L, null, null, n(100), n(1), true)), List.of(), List.of(surRow.apply(1L, layers.apply(5, null))));
        var upd = svc.update(v1.id(), edited);
        check(!upd.newVersionCreated() && upd.treaty().id().equals(v1.id()) && upd.treaty().versionNo() == 1 && upd.treaty().treatyName().endsWith("(revised)"), "update DRAFT: same record, same version, changed in place");
        check(upd.treaty().coverMap().get(0).surplusLayers().size() == 1 && upd.treaty().coverMap().get(0).surplusLayers().get(0).noOfLines() == 5 && upd.treaty().panel().size() == 1, "update DRAFT: children fully replaced (1 layer on the row, 1 panel row)");

        // ---------- amend an ACTIVE treaty -> new draft version
        store.get(v1.id()).setStatusId(ACTIVE);
        var noDate = expect(() -> svc.update(v1.id(), edited));
        check(noDate != null && "EFFECTIVE_DATE_REQUIRED".equals(noDate.getCode()), "amend ACTIVE without an effective date -> 400");
        TreatyRequest amend = new TreatyRequest("FIRE-SUR-2027", "Fire Surplus 2027 v2", 1L, 1L, 1L, 1L, 1L, f, t, LocalDate.of(2027, 7, 1), null, "mid-term change", null, null,
            List.of(new PanelDto(null, "SUR", 1, 2L, null, null, n(100), n(1), true)), List.of(), List.of(surRow.apply(1L, layers.apply(8, null))));
        var am = svc.update(v1.id(), amend);
        check(am.newVersionCreated() && am.treaty().versionNo() == 2 && !am.treaty().id().equals(v1.id()) && "DRAFT".equals(am.treaty().status().code()) && v1.id().equals(am.treaty().supersedesTreatyId()), "amend ACTIVE: new DRAFT version 2 supersedes version 1");
        check("PROSPECTIVE".equals(am.treaty().amendMode().desc()) && LocalDate.of(2027, 7, 1).equals(am.treaty().effectiveFrom()), "amend: default prospective mode and the effective date are recorded");
        check(store.get(v1.id()).getCoverMaps().get(0).getSurplusLayers().get(0).getNoOfLines() == 5 && store.get(v1.id()).getStatusId() == ACTIVE, "amend: the live version v1 is untouched");
        var open = expect(() -> svc.update(v1.id(), amend));
        check(open != null && "OPEN_VERSION_EXISTS".equals(open.getCode()), "amend again while a draft version exists -> 409");
        store.get(am.treaty().id()).setStatusId(PENDING);
        var pend = expect(() -> svc.update(am.treaty().id(), amend));
        check(pend != null && "UNDER_APPROVAL".equals(pend.getCode()), "edit a PENDING_APPROVAL version -> 409 until withdrawn");
        store.get(am.treaty().id()).setStatusId(DRAFT);
        var upd2 = svc.update(am.treaty().id(), amend);
        check(!upd2.newVersionCreated() && upd2.treaty().id().equals(am.treaty().id()), "draft version 2 is then edited in place");

        // ---------- ONE treaty with two types against the same product: QS 20% (seq 1) + surplus on the net after QS (seq 2)
        AllocationRuleDto qsRule = new AllocationRuleDto(null, null, null, "QS", null, null, "GROSS", null, null, null, null, null, "CONTINUE", null, null, null, null, null);
        AllocationRuleDto surNet = new AllocationRuleDto(null, null, null, "SURPLUS", null, null, "NET_AFTER_QS", null, null, null, null, null, "FACULTATIVE", null, null, null, null, null);
        CoverMapDto qsRow = new CoverMapDto(null, null, null, 1L, null, 1L, null, null, null, null, null, 1, f, t, null, List.of(qsRule), 1L, null, null, null,
            new QsParamsDto(null, n(20), null, null, null, 1L, null, null, n(27.5), null), List.of(), List.of(), List.of(), null, "20% QS");
        CoverMapDto surRow2 = new CoverMapDto(null, null, null, 1L, null, 1L, null, null, null, null, null, 2, f, t, null, List.of(surNet), 2L, null, null, null, null,
            List.of(new RetentionDto(null, 501L, "A", n(4_000_000))), layers.apply(4, 6), List.of(), null, null);
        TreatyRequest prog = new TreatyRequest("FIRE-PROG-2027", "Fire programme", 1L, 1L, 1L, 1L, 1L, f, t, null, null, null, null, null,
            List.of(new PanelDto(null, "QS", null, 1L, null, null, n(100), n(1), true), new PanelDto(null, "SUR", 1, 2L, null, null, n(100), n(1), true),
                    new PanelDto(null, "SUR", 2, 4L, null, null, n(100), n(1), true)), List.of(), List.of(qsRow, surRow2));
        var pr = svc.create(prog);
        check(pr.issues().stream().noneMatch(i -> i.isError()) && pr.treaty().treatyTypes().size() == 2, "multi-type treaty saves without errors and lists both types (Quota share + Surplus)");
        check(pr.treaty().coverMap().get(0).qsParams().cessionPct().compareTo(n(20)) == 0 && pr.treaty().coverMap().get(0).qsParams().retentionPct().compareTo(n(80)) == 0
            && pr.treaty().coverMap().get(0).surplusLayers().isEmpty() && pr.treaty().coverMap().get(1).qsParams() == null && pr.treaty().coverMap().get(1).surplusLayers().size() == 2,
            "each row keeps only the terms of its own type (retention % derived = 100 - cession)");
        check(store.get(pr.treaty().id()).getCoverMaps().get(0).getQsParams().get(0).getTreatyId().equals(pr.treaty().id()), "row terms also record the owning treaty id");
        var sim0 = svc.simulate(pr.treaty().id(), new SimulationRequest(n(40_000_000), n(100_000), null, null, 0));
        check(sim0.lines().size() == 1 && sim0.lines().get(0).amount().compareTo(n(8_000_000)) == 0 && sim0.lines().get(0).commission().compareTo(new BigDecimal("5500.00")) == 0
            && sim0.lines().get(0).shares().size() == 1, "simulate row 1 (QS 20%): 8m ceded, commission 27.5% of 20,000 = 5,500, split on the QS panel");
        var sim1 = svc.simulate(pr.treaty().id(), new SimulationRequest(n(40_000_000), n(100_000), null, null, 1));
        check(sim1.lines().size() == 2 && sim1.lines().get(0).amount().compareTo(n(16_000_000)) == 0 && sim1.lines().get(1).amount().compareTo(n(20_000_000)) == 0
            && sim1.retainedSi().compareTo(n(4_000_000)) == 0 && sim1.notes().stream().anyMatch(x -> x.contains("Mapping row 2")), "simulate row 2 (surplus, retention 4m): 16m + 20m, retained 4m");
        TreatyRequest badMix = new TreatyRequest("FIRE-BAD", "x", 1L, 1L, 1L, 1L, 1L, f, t, null, null, null, null, null, prog.panel(), List.of(),
            List.of(qsRow, new CoverMapDto(null, null, null, 1L, null, 1L, null, null, null, null, null, 1, f, t, null, List.of(surNet), 2L, null, null, null, null,
                surRow2.retentions(), surRow2.surplusLayers(), List.of(), null, null)));
        check(svc.validate(badMix).stream().anyMatch(i -> i.isError() && i.message().contains("same sequence 1")), "two types of one treaty at the same sequence on the same cover are rejected");

        // ---------- versions / delete / renew
        check(svc.versions("FIRE-SUR-2027").size() == 2 && svc.versions("FIRE-SUR-2027").get(0).versionNo() == 2, "versions listed newest first");
        check("Surplus".equals(svc.versions("FIRE-SUR-2027").get(0).treatyTypeDesc()), "summary shows the types used on the mapping");
        var ren = svc.renew(pr.treaty().id(), new RenewRequest("FIRE-PROG-2028", null, LocalDate.of(2028, 1, 1), LocalDate.of(2028, 12, 31)));
        check("FIRE-PROG-2028".equals(ren.treaty().treatyCode()) && ren.treaty().versionNo() == 1 && "2028-01-01".equals(String.valueOf(ren.treaty().coverMap().get(0).effectiveFrom()))
            && "2028-12-31".equals(String.valueOf(ren.treaty().coverMap().get(1).rules().get(0).effectiveTo())), "renew: new code, version 1 draft, mapping and rule dates re-based to the new period");
        check(ren.treaty().coverMap().get(0).qsParams().cessionPct().compareTo(n(20)) == 0 && ren.treaty().coverMap().get(1).surplusLayers().size() == 2 && ren.treaty().panel().size() == 3,
            "renew copies each row's type and terms and the panel");
        svc.delete(ren.treaty().id());
        check("DELETED".equals(store.get(ren.treaty().id()).getRecordStatus()) && expect(() -> svc.get(ren.treaty().id())) != null, "delete draft = soft delete, then not found");
        var delActive = expect(() -> svc.delete(v1.id()));
        check(delActive != null && "NOT_DRAFT".equals(delActive.getCode()), "an active version cannot be deleted");

        System.out.println(fails == 0 ? "ALL PASSED" : fails + " FAILED");
        if (fails > 0) throw new AssertionError(fails + " check(s) failed");
    }
}
