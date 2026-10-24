package com.rms.master;

import com.rms.common.Snap;
import com.rms.domain.Product;
import com.rms.domain.ProductSection;
import com.rms.domain.SectionCover;
import com.rms.repository.LobRepository;
import com.rms.repository.ProductRepository;
import com.rms.repository.ProductSectionRepository;
import com.rms.repository.SectionCoverRepository;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Line of business -> product -> section -> cover cross-check for treaty mappings.
 *  - the deepest id supplied decides the mapping level (LOB, PRODUCT, SECTION or COVER)
 *  - every ancestor supplied must agree with the chain stored in the product masters (contradictions are errors)
 *  - missing ancestors are derived, so snapshots (id, code, description) are always complete
 *  - the mapping LOB must equal the treaty LOB
 *  - the mapping expands to the set of covers it applies to, which is what overlap checks compare
 */
@Service
public class CoverHierarchyService {

    public record Resolved(String level, Snap lob, Snap product, Snap section, Snap cover, Set<Long> coverIds, List<String> errors) {
        public boolean ok() { return errors.isEmpty(); }
    }

    private final LobRepository lobs;
    private final ProductRepository products;
    private final ProductSectionRepository sections;
    private final SectionCoverRepository covers;
    private final MasterSnapshotService masters;

    public CoverHierarchyService(LobRepository lobs, ProductRepository products, ProductSectionRepository sections, SectionCoverRepository covers,
                                 MasterSnapshotService masters) {
        this.lobs = lobs;
        this.products = products;
        this.sections = sections;
        this.covers = covers;
        this.masters = masters;
    }

    private static Resolved fail(String msg) {
        return new Resolved(null, null, null, null, null, Set.of(), List.of(msg));
    }

    public Resolved resolve(Long lobId, Long productId, Long sectionId, Long coverId, Long treatyLobId) {
        List<String> errors = new ArrayList<>();
        Long c = coverId, s = sectionId, p = productId, l = lobId;
        String level = coverId != null ? "COVER" : sectionId != null ? "SECTION" : productId != null ? "PRODUCT" : "LOB";

        if (c != null) {
            SectionCover cv = covers.findById(c).orElse(null);
            if (cv == null) return fail("Cover " + c + " does not exist");
            if (s != null && !s.equals(cv.getSectionId())) errors.add("Cover '" + cv.getDescription() + "' does not belong to the selected section");
            s = cv.getSectionId();
        }
        if (s != null) {
            ProductSection sec = sections.findById(s).orElse(null);
            if (sec == null) return fail("Section " + s + " does not exist");
            if (p != null && !p.equals(sec.getProductId())) errors.add("Section '" + sec.getDescription() + "' does not belong to the selected product");
            p = sec.getProductId();
        }
        if (p != null) {
            Product pr = products.findById(p).orElse(null);
            if (pr == null) return fail("Product " + p + " does not exist");
            if (l != null && !l.equals(pr.getLobId())) errors.add("Product '" + pr.getDescription() + "' does not belong to the selected line of business");
            l = pr.getLobId();
        }
        if (l == null) return fail("Select at least a line of business for the mapping");
        if (treatyLobId != null && !l.equals(treatyLobId)) {
            errors.add("Mapping is for line of business '" + lobs.findById(l).map(x -> x.getDescription()).orElse(String.valueOf(l))
                + "' but the treaty is for '" + lobs.findById(treatyLobId).map(x -> x.getDescription()).orElse(String.valueOf(treatyLobId)) + "'");
        }
        if (!errors.isEmpty()) return new Resolved(level, null, null, null, null, Set.of(), errors);

        Snap lobS = masters.lob(l);
        Snap prodS = "LOB".equals(level) ? null : masters.product(p);
        Snap secS = "LOB".equals(level) || "PRODUCT".equals(level) ? null : masters.section(s);
        Snap covS = "COVER".equals(level) ? masters.cover(c) : null;
        return new Resolved(level, lobS, prodS, secS, covS, expand(l, p, s, c), List.of());
    }

    /** Covers a mapping applies to; the deepest non-null id decides how wide it is. */
    public Set<Long> expand(Long lobId, Long productId, Long sectionId, Long coverId) {
        Set<Long> out = new LinkedHashSet<>();
        if (coverId != null) {
            out.add(coverId);
        } else if (sectionId != null) {
            covers.findBySectionId(sectionId).forEach(c -> out.add(c.getId()));
        } else if (productId != null) {
            sections.findByProductId(productId).forEach(sec -> covers.findBySectionId(sec.getId()).forEach(c -> out.add(c.getId())));
        } else if (lobId != null) {
            products.findByLobId(lobId).forEach(pr -> sections.findByProductId(pr.getId())
                .forEach(sec -> covers.findBySectionId(sec.getId()).forEach(c -> out.add(c.getId()))));
        }
        return out;
    }

    public String coverName(Long coverId) {
        return covers.findById(coverId).map(SectionCover::getDescription).orElse(String.valueOf(coverId));
    }
}
