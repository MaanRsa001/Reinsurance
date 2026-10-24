package com.rms.treaty;

import com.rms.common.Snap;
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
import com.rms.master.LookupCodes;
import com.rms.master.CoverHierarchyService;
import com.rms.master.LookupService;
import com.rms.master.MasterSnapshotService;
import com.rms.treaty.TreatyDtos.AllocationRuleDto;
import com.rms.treaty.TreatyDtos.CoverMapDto;
import com.rms.treaty.TreatyDtos.ExclusionDto;
import com.rms.treaty.TreatyDtos.PanelDto;
import com.rms.treaty.TreatyDtos.PcConfigDto;
import com.rms.treaty.TreatyDtos.QsParamsDto;
import com.rms.treaty.TreatyDtos.Ref;
import com.rms.treaty.TreatyDtos.ReinstatementDto;
import com.rms.treaty.TreatyDtos.RetentionDto;
import com.rms.treaty.TreatyDtos.ScScaleDto;
import com.rms.treaty.TreatyDtos.StopLossDto;
import com.rms.treaty.TreatyDtos.SurplusLayerDto;
import com.rms.treaty.TreatyDtos.TreatyRequest;
import com.rms.treaty.TreatyDtos.TreatySummary;
import com.rms.treaty.TreatyDtos.TreatyView;
import com.rms.treaty.TreatyDtos.XolLayerDto;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Entity -> DTO conversion. (DTO -> entity happens in TreatyService because it needs master snapshots.) */
@Component
public class TreatyMapper {
    private final LookupService lookups;
    private final MasterSnapshotService masters;
    private final CoverHierarchyService hierarchy;

    public TreatyMapper(LookupService lookups, MasterSnapshotService masters, CoverHierarchyService hierarchy) {
        this.lookups = lookups;
        this.masters = masters;
        this.hierarchy = hierarchy;
    }

    private static Ref ref(Long id, String code, String desc) {
        return id == null ? null : new Ref(id, code, desc);
    }

    /** Distinct treaty types used on the mapping rows, in first-use order. */
    private static List<Ref> types(Treaty t) {
        Map<Long, Ref> out = new LinkedHashMap<>();
        for (TreatyCoverMap m : t.getCoverMaps()) if (m.getTreatyTypeId() != null) out.putIfAbsent(m.getTreatyTypeId(), new Ref(m.getTreatyTypeId(), m.getTreatyTypeCode(), m.getTreatyTypeDesc()));
        return new ArrayList<>(out.values());
    }

    public TreatySummary toSummary(Treaty t) {
        Snap st = lookups.byId(t.getStatusId());
        String types = types(t).stream().map(Ref::desc).collect(Collectors.joining(" + "));
        return new TreatySummary(t.getId(), t.getTreatyCode(), t.getTreatyName(), t.getVersionNo(), types.isEmpty() ? "(no mapping yet)" : types, t.getLobDesc(), st.desc(),
            st.code(), t.getInceptionDate(), t.getExpiryDate(), t.getUpdatedBy() != null ? t.getUpdatedBy() : t.getCreatedBy(),
            t.getUpdatedAt() != null ? t.getUpdatedAt() : t.getCreatedAt());
    }

    public TreatyView toView(Treaty t) {
        Snap st = lookups.byId(t.getStatusId());
        TreatyRequest r = toRequest(t);
        return new TreatyView(t.getId(), t.getTreatyCode(), t.getTreatyName(), t.getVersionNo(), types(t),
            ref(t.getTreatyBasisId(), t.getTreatyBasisCode(), t.getTreatyBasisDesc()),
            ref(t.getCessionBasisId(), t.getCessionBasisCode(), t.getCessionBasisDesc()),
            ref(t.getCurrencyId(), t.getCurrencyCode(), t.getCurrencyDesc()), ref(t.getLobId(), t.getLobCode(), t.getLobDesc()),
            ref(t.getBrokerId(), t.getBrokerCode(), t.getBrokerDesc()), t.getInceptionDate(), t.getExpiryDate(), t.getEffectiveFrom(),
            t.getSupersedesTreatyId(), ref(t.getAmendModeId(), null, t.getAmendModeDesc()), new Ref(st.id(), st.code(), st.desc()),
            LookupCodes.DRAFT.equals(st.code()), t.getRemarks(), r.scScale(), r.pcConfig(), r.panel(), r.exclusions(), r.coverMap(),
            t.getUpdatedBy() != null ? t.getUpdatedBy() : t.getCreatedBy(), t.getUpdatedAt() != null ? t.getUpdatedAt() : t.getCreatedAt());
    }

    /** Full editable payload of a stored treaty (used for edit forms, validation, renew and diff). */
    public TreatyRequest toRequest(Treaty t) {
        return new TreatyRequest(t.getTreatyCode(), t.getTreatyName(), t.getTreatyBasisId(), t.getCessionBasisId(), t.getCurrencyId(),
            t.getLobId(), t.getBrokerId(), t.getInceptionDate(), t.getExpiryDate(), t.getEffectiveFrom(), t.getAmendModeId(), t.getRemarks(),
            t.getScScales().stream().findFirst().map(this::sc).orElse(null), t.getPcConfigs().stream().findFirst().map(this::pc).orElse(null),
            t.getPanel().stream().map(this::panel).toList(), t.getExclusions().stream().map(this::exclusion).toList(),
            t.getCoverMaps().stream().map(this::coverMap).toList());
    }

    private QsParamsDto qs(TreatyParamQs q) {
        return new QsParamsDto(q.getId(), q.getCessionPct(), q.getRetentionPct(), q.getPerRiskLimit(), q.getPerEventLimit(), q.getCommissionTypeId(),
            q.getCommissionTypeCode(), q.getCommissionTypeDesc(), q.getFlatCommissionPct(), q.getCashLossLimit());
    }

    private RetentionDto retention(TreatySurplusRetention r) {
        return new RetentionDto(r.getId(), r.getRiskGradeId(), r.getRiskGradeDesc(), r.getRetentionAmt());
    }

    private SurplusLayerDto surplusLayer(TreatySurplusLayer l) {
        return new SurplusLayerDto(l.getId(), l.getLayerNo(), l.getLayerName(), l.getNoOfLines(), l.getRetentionAmt(), l.getCapacityAmt(),
            l.getCommissionTypeId(), l.getCommissionTypeDesc(), l.getCommissionPct());
    }

    private XolLayerDto xolLayer(TreatyXolLayer l) {
        List<ReinstatementDto> re = l.getReinstatements().stream().map(this::reinstatement).toList();
        return new XolLayerDto(l.getId(), l.getLayerNo(), l.getLayerName(), l.getLimitAmt(), l.getDeductibleAmt(), l.getRateOnLinePct(),
            l.getDepositPremium(), l.getMinPremium(), l.getEventHoursClause(), re);
    }

    private ReinstatementDto reinstatement(TreatyXolReinstatement r) {
        return new ReinstatementDto(r.getId(), r.getReinstatementNo(), r.getPremiumPct(), r.getReinstatementBasisId(), r.getReinstatementBasisDesc());
    }

    private StopLossDto stopLoss(TreatyStoploss s) {
        return new StopLossDto(s.getId(), s.getAttachmentLrPct(), s.getExitLrPct(), s.getLimitPctOfPremium(), s.getPremiumRatePct(), s.getPremiumBasisDesc());
    }

    private ScScaleDto sc(ScScale s) {
        return new ScScaleDto(s.getId(), s.getProvisionalPct(), s.getMinPct(), s.getMaxPct(), s.getLrAtMaxPct(), s.getLrAtMinPct(), s.getSlideRatio(),
            s.getLrBasisId(), s.getLrBasisDesc(), s.getAdjustmentMonths());
    }

    private PcConfigDto pc(PcConfig p) {
        return new PcConfigDto(p.getId(), p.getPcPct(), p.getMgmtExpensePct(), p.getCarryForwardYears(), p.getDafEnabled());
    }

    private PanelDto panel(TreatyPanel p) {
        return new PanelDto(p.getId(), p.getPanelGroup(), p.getLayerNo(), p.getReinsurerId(), p.getReinsurerCode(), p.getReinsurerDesc(), p.getSharePct(), p.getBrokeragePct(), p.getLeadFlag());
    }

    private ExclusionDto exclusion(TreatyExclusion e) {
        return new ExclusionDto(e.getId(), e.getExclusionDesc(), e.getActionDesc());
    }

    private CoverMapDto coverMap(TreatyCoverMap c) {
        int count = hierarchy.expand(c.getLobId(), c.getProductId(), c.getSectionId(), c.getCoverId()).size();
        return new CoverMapDto(c.getId(), lookups.byId(c.getMapLevelId()).code(), c.getMapLevelDesc(), c.getLobId(), c.getLobDesc(), c.getProductId(),
            c.getProductDesc(), c.getSectionId(), c.getSectionDesc(), c.getCoverId(), c.getCoverDesc(), c.getSequenceNo(), c.getEffectiveFrom(), c.getEffectiveTo(), count,
            c.getRules().stream().map(this::rule).toList(), c.getTreatyTypeId(), c.getTreatyTypeCode(), c.getTreatyTypeDesc(),
            c.getTreatyTypeId() == null ? null : masters.strategyOf(c.getTreatyTypeId()),
            c.getQsParams().stream().findFirst().map(this::qs).orElse(null), c.getRetentions().stream().map(this::retention).toList(),
            c.getSurplusLayers().stream().map(this::surplusLayer).toList(), c.getXolLayers().stream().map(this::xolLayer).toList(),
            c.getStopLosses().stream().findFirst().map(this::stopLoss).orElse(null), c.getNote());
    }

    private AllocationRuleDto rule(TreatyCoverAllocationRule r) {
        return new AllocationRuleDto(r.getId(), r.getRuleNo(), r.getAllocationMethodId(), r.getAllocationMethodCode(), r.getAllocationMethodDesc(), r.getAllocationBasisId(),
            r.getAllocationBasisCode(), r.getAllocationBasisDesc(), r.getAllocationPct(), r.getMinAllocationAmt(), r.getMaxAllocationAmt(), r.getResidualActionId(),
            r.getResidualActionCode(), r.getResidualActionDesc(), r.getStopOnLimitFlag(), r.getConditionRule(), r.getEffectiveFrom(), r.getEffectiveTo());
    }
}
