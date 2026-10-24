package com.rms.master;

import com.rms.common.ApiException;
import com.rms.common.Snap;
import com.rms.domain.TreatyType;
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
import java.util.function.Function;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

/** Resolves a master id into the id + code + description snapshot that transaction tables must store (rule 3A). */
@Service
public class MasterSnapshotService {
    private final TreatyTypeRepository treatyTypes;
    private final TreatyBasisRepository treatyBases;
    private final CessionBasisRepository cessionBases;
    private final CurrencyRepository currencies;
    private final LobRepository lobs;
    private final BrokerRepository brokers;
    private final ReinsurerRepository reinsurers;
    private final CommissionTypeRepository commissionTypes;
    private final ProductRepository products;
    private final ProductSectionRepository sections;
    private final SectionCoverRepository covers;

    public MasterSnapshotService(TreatyTypeRepository treatyTypes, TreatyBasisRepository treatyBases, CessionBasisRepository cessionBases,
                                 CurrencyRepository currencies, LobRepository lobs, BrokerRepository brokers, ReinsurerRepository reinsurers,
                                 CommissionTypeRepository commissionTypes, ProductRepository products, ProductSectionRepository sections,
                                 SectionCoverRepository covers) {
        this.treatyTypes = treatyTypes;
        this.treatyBases = treatyBases;
        this.cessionBases = cessionBases;
        this.currencies = currencies;
        this.lobs = lobs;
        this.brokers = brokers;
        this.reinsurers = reinsurers;
        this.commissionTypes = commissionTypes;
        this.products = products;
        this.sections = sections;
        this.covers = covers;
    }

    private <T> Snap find(Long id, JpaRepository<T, Long> repo, Function<T, Snap> f, String label) {
        if (id == null) throw ApiException.badRequest("MASTER_REQUIRED", label + " is required");
        return repo.findById(id).map(f).orElseThrow(() -> ApiException.badRequest("MASTER_NOT_FOUND", label + " " + id + " does not exist"));
    }

    public Snap treatyType(Long id) { return find(id, treatyTypes, x -> new Snap(x.getId(), x.getCode(), x.getDescription()), "Treaty type"); }
    public Snap treatyBasis(Long id) { return find(id, treatyBases, x -> new Snap(x.getId(), x.getCode(), x.getDescription()), "Treaty year basis"); }
    public Snap cessionBasis(Long id) { return find(id, cessionBases, x -> new Snap(x.getId(), x.getCode(), x.getDescription()), "Cession basis"); }
    public Snap currency(Long id) { return find(id, currencies, x -> new Snap(x.getId(), x.getCode(), x.getName()), "Currency"); }
    public Snap lob(Long id) { return find(id, lobs, x -> new Snap(x.getId(), x.getCode(), x.getDescription()), "Line of business"); }
    public Snap broker(Long id) { return find(id, brokers, x -> new Snap(x.getId(), x.getCode(), x.getName()), "Broker"); }
    public Snap reinsurer(Long id) { return find(id, reinsurers, x -> new Snap(x.getId(), x.getCode(), x.getName()), "Reinsurer"); }
    public Snap commissionType(Long id) { return find(id, commissionTypes, x -> new Snap(x.getId(), x.getCode(), x.getDescription()), "Commission type"); }
    public Snap product(Long id) { return find(id, products, x -> new Snap(x.getId(), x.getCode(), x.getDescription()), "Product"); }
    public Snap section(Long id) { return find(id, sections, x -> new Snap(x.getId(), x.getCode(), x.getDescription()), "Section"); }
    public Snap cover(Long id) { return find(id, covers, x -> new Snap(x.getId(), x.getCode(), x.getDescription()), "Cover"); }

    /** Behaviour key of a treaty type (Spring bean / strategy name) - the engine and the UI branch on this, never on an enum. */
    public String strategyOf(Long treatyTypeId) {
        return treatyTypes.findById(treatyTypeId).map(TreatyType::getCalcStrategyCode).orElse("");
    }
}
