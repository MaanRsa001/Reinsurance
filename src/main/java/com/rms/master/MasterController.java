package com.rms.master;

import com.rms.domain.ProductSection;
import com.rms.domain.RefLookupValue;
import com.rms.domain.SectionCover;
import com.rms.repository.BrokerRepository;
import com.rms.repository.CessionBasisRepository;
import com.rms.repository.CommissionTypeRepository;
import com.rms.repository.CurrencyRepository;
import com.rms.repository.LobRepository;
import com.rms.repository.ProductRepository;
import com.rms.repository.ProductSectionRepository;
import com.rms.repository.ReinsurerRepository;
import com.rms.repository.SectionCoverRepository;
import com.rms.repository.TreatyBasisRepository;
import com.rms.repository.TreatyTypeRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Read-only master data for dropdowns in the treaty admin console. */
@RestController
@RequestMapping("/api/v1/masters")
@PreAuthorize("hasAuthority('TREATY_VIEW')")
public class MasterController {

    public record MasterItem(Long id, String code, String description, Map<String, Object> attrs) { }
    public record CoverNode(Long id, String code, String description) { }
    public record SectionNode(Long id, String code, String description, List<CoverNode> covers) { }
    public record ProductNode(Long id, String code, String description, Long lobId, List<SectionNode> sections) { }

    private final TreatyTypeRepository treatyTypes;
    private final TreatyBasisRepository treatyBases;
    private final CessionBasisRepository cessionBases;
    private final CommissionTypeRepository commissionTypes;
    private final CurrencyRepository currencies;
    private final LobRepository lobs;
    private final BrokerRepository brokers;
    private final ReinsurerRepository reinsurers;
    private final ProductRepository products;
    private final ProductSectionRepository sections;
    private final SectionCoverRepository covers;
    private final LookupService lookups;

    public MasterController(TreatyTypeRepository treatyTypes, TreatyBasisRepository treatyBases, CessionBasisRepository cessionBases,
                            CommissionTypeRepository commissionTypes, CurrencyRepository currencies, LobRepository lobs, BrokerRepository brokers,
                            ReinsurerRepository reinsurers, ProductRepository products, ProductSectionRepository sections,
                            SectionCoverRepository covers, LookupService lookups) {
        this.treatyTypes = treatyTypes;
        this.treatyBases = treatyBases;
        this.cessionBases = cessionBases;
        this.commissionTypes = commissionTypes;
        this.currencies = currencies;
        this.lobs = lobs;
        this.brokers = brokers;
        this.reinsurers = reinsurers;
        this.products = products;
        this.sections = sections;
        this.covers = covers;
        this.lookups = lookups;
    }

    private static Map<String, Object> attrs(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    @GetMapping("/treaty-types")
    public List<MasterItem> treatyTypes() {
        return treatyTypes.findAll().stream().map(t -> new MasterItem(t.getId(), t.getCode(), t.getDescription(),
            attrs("calcStrategyCode", t.getCalcStrategyCode(), "proportional", t.getProportionalFlag()))).toList();
    }

    @GetMapping("/treaty-bases")
    public List<MasterItem> treatyBases() {
        return treatyBases.findAll().stream().map(x -> new MasterItem(x.getId(), x.getCode(), x.getDescription(), Map.of())).toList();
    }

    @GetMapping("/cession-bases")
    public List<MasterItem> cessionBases() {
        return cessionBases.findAll().stream().map(x -> new MasterItem(x.getId(), x.getCode(), x.getDescription(), Map.of())).toList();
    }

    @GetMapping("/commission-types")
    public List<MasterItem> commissionTypes() {
        return commissionTypes.findAll().stream().map(x -> new MasterItem(x.getId(), x.getCode(), x.getDescription(), Map.of())).toList();
    }

    @GetMapping("/currencies")
    public List<MasterItem> currencies() {
        return currencies.findAll().stream().map(x -> new MasterItem(x.getId(), x.getCode(), x.getName(), Map.of())).toList();
    }

    @GetMapping("/lobs")
    public List<MasterItem> lobs() {
        return lobs.findAll().stream().map(x -> new MasterItem(x.getId(), x.getCode(), x.getDescription(), Map.of())).toList();
    }

    @GetMapping("/brokers")
    public List<MasterItem> brokers() {
        return brokers.findAll().stream().map(x -> new MasterItem(x.getId(), x.getCode(), x.getName(), Map.of())).toList();
    }

    @GetMapping("/reinsurers")
    public List<MasterItem> reinsurers() {
        return reinsurers.findAll().stream().map(x -> new MasterItem(x.getId(), x.getCode(), x.getName(),
            attrs("country", x.getCountry(), "rating", x.getRating(), "ratingAgency", x.getRatingAgency()))).toList();
    }

    @GetMapping("/lookups/{typeCode}")
    public List<MasterItem> lookups(@PathVariable String typeCode) {
        List<MasterItem> out = new ArrayList<>();
        for (RefLookupValue v : lookups.list(typeCode)) out.add(new MasterItem(v.getId(), v.getCode(), v.getDescription(), Map.of()));
        return out;
    }

    /** Product -> Section -> Cover tree used by the mapping editor. */
    @GetMapping("/product-tree")
    public List<ProductNode> productTree() {
        List<ProductNode> out = new ArrayList<>();
        products.findAll().forEach(p -> {
            List<SectionNode> secs = new ArrayList<>();
            for (ProductSection s : sections.findByProductId(p.getId())) {
                List<CoverNode> cs = new ArrayList<>();
                for (SectionCover c : covers.findBySectionId(s.getId())) cs.add(new CoverNode(c.getId(), c.getCode(), c.getDescription()));
                secs.add(new SectionNode(s.getId(), s.getCode(), s.getDescription(), cs));
            }
            out.add(new ProductNode(p.getId(), p.getCode(), p.getDescription(), p.getLobId(), secs));
        });
        return out;
    }
}
