package com.rms.makerchecker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rms.common.ApiException;
import com.rms.common.CurrentUser;
import com.rms.common.Snap;
import com.rms.domain.McApprovalStep;
import com.rms.domain.McRequest;
import com.rms.makerchecker.McDtos.McRequestView;
import com.rms.makerchecker.McDtos.StepView;
import com.rms.master.LookupCodes;
import com.rms.master.LookupService;
import com.rms.repository.McApprovalStepRepository;
import com.rms.repository.McRequestRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Generic maker-checker. Rules enforced in code (not only in the UI):
 *  - the maker can never decide on his own request (segregation of duties)
 *  - the same user cannot approve two levels of one request
 *  - level N needs the authority configured for that level
 *  - nothing is applied to the live record until the final level approves (handler.onApproved)
 */
@Service
public class MakerCheckerService {
    private final McRequestRepository requests;
    private final McApprovalStepRepository steps;
    private final LookupService lookups;
    private final ObjectMapper om;
    private final Map<String, ApprovalHandler> handlers = new HashMap<>();
    private final String level1Authority;
    private final String level2Authority;

    public MakerCheckerService(McRequestRepository requests, McApprovalStepRepository steps, LookupService lookups, ObjectMapper om,
                               List<ApprovalHandler> handlerList, @Value("${rms.makerchecker.authority.level1}") String level1Authority,
                               @Value("${rms.makerchecker.authority.level2}") String level2Authority) {
        this.requests = requests;
        this.steps = steps;
        this.lookups = lookups;
        this.om = om;
        handlerList.forEach(h -> h.entityTypeCodes().forEach(c -> handlers.put(c, h)));
        this.level1Authority = level1Authority;
        this.level2Authority = level2Authority;
    }

    private String authorityFor(int level) { return level <= 1 ? level1Authority : level2Authority; }

    @Transactional
    public McRequest submit(String entityTypeCode, Long entityId, String entityDesc, String actionCode, Object before, Object after, String remarks, int levels) {
        Snap entity = lookups.snap(LookupCodes.MC_ENTITY, entityTypeCode);
        Snap action = lookups.snap(LookupCodes.MC_ACTION, actionCode);
        Snap status = lookups.snap(LookupCodes.MC_STATUS, LookupCodes.SUBMITTED);
        if (requests.findByEntityTypeIdAndEntityIdOrderByIdDesc(entity.id(), entityId).stream().anyMatch(r -> r.getStatusId().equals(status.id())))
            throw ApiException.conflict("REQUEST_OPEN", "There is already an open approval request for this record");
        McRequest r = new McRequest();
        r.setRequestNo("TMP-" + UUID.randomUUID());
        r.setEntityTypeId(entity.id()); r.setEntityTypeDesc(entity.desc());
        r.setEntityId(entityId);
        r.setEntityDesc(entityDesc);
        r.setActionId(action.id()); r.setActionDesc(action.desc());
        r.setPayloadBefore(before == null ? null : om.valueToTree(before));
        r.setPayloadAfter(after == null ? null : om.valueToTree(after));
        r.setStatusId(status.id()); r.setStatusDesc(status.desc());
        r.setMakerUser(CurrentUser.username());
        r.setMakerAt(LocalDateTime.now());
        r.setMakerRemarks(remarks);
        r.setCurrentLevel(1);
        r.setLevelsRequired(Math.max(1, levels));
        r = requests.saveAndFlush(r);
        r.setRequestNo(String.format("MC-%d-%04d", LocalDate.now().getYear(), r.getId()));
        return requests.save(r);
    }

    @Transactional
    public McRequestView decide(Long requestId, String decisionCode, String remarks) {
        McRequest r = requests.findById(requestId).orElseThrow(() -> ApiException.notFound("MC_NOT_FOUND", "Request " + requestId + " not found"));
        Snap submitted = lookups.snap(LookupCodes.MC_STATUS, LookupCodes.SUBMITTED);
        if (!submitted.id().equals(r.getStatusId())) throw ApiException.conflict("MC_NOT_OPEN", "This request is no longer open");
        String user = CurrentUser.username();
        if (user.equals(r.getMakerUser())) throw ApiException.forbidden("SOD_VIOLATION", "Segregation of duties: you cannot decide on your own request");
        int level = r.getCurrentLevel();
        if (!CurrentUser.has(authorityFor(level))) throw ApiException.forbidden("MC_NO_AUTHORITY", "Level " + level + " approval needs the " + authorityFor(level) + " authority");
        List<McApprovalStep> done = steps.findByRequestIdOrderByLevelNoAsc(requestId);
        if (done.stream().anyMatch(s -> user.equals(s.getCheckerUser()))) throw ApiException.forbidden("SOD_VIOLATION", "You already decided on an earlier level of this request");
        if ((LookupCodes.REJECTED.equals(decisionCode) || LookupCodes.RETURNED.equals(decisionCode)) && (remarks == null || remarks.isBlank()))
            throw ApiException.badRequest("REMARKS_REQUIRED", "Remarks are required when rejecting or returning a request");

        Snap decision = lookups.snap(LookupCodes.MC_STATUS, decisionCode);
        McApprovalStep s = new McApprovalStep();
        s.setRequestId(requestId);
        s.setLevelNo(level);
        s.setCheckerRoleDesc(level <= 1 ? "Treaty Checker" : "Head - Reinsurance");
        s.setCheckerUser(user);
        s.setDecisionId(decision.id()); s.setDecisionDesc(decision.desc());
        s.setDecidedAt(LocalDateTime.now());
        s.setRemarks(remarks);
        steps.save(s);

        ApprovalHandler handler = handlerFor(r);
        switch (decisionCode) {
            case LookupCodes.APPROVED -> {
                if (level < r.getLevelsRequired()) {
                    r.setCurrentLevel(level + 1);
                } else {
                    setStatus(r, LookupCodes.APPROVED);
                    handler.onApproved(r);
                }
            }
            case LookupCodes.REJECTED -> {
                setStatus(r, LookupCodes.REJECTED);
                handler.onRejected(r, remarks);
            }
            case LookupCodes.RETURNED -> {
                setStatus(r, LookupCodes.RETURNED);
                handler.onReturned(r, remarks);
            }
            default -> throw ApiException.badRequest("MC_BAD_DECISION", "Unknown decision " + decisionCode);
        }
        return view(requests.save(r));
    }

    /** Maker withdraws his open request (the record goes back to draft). */
    @Transactional
    public void withdrawOpenRequest(String entityTypeCode, Long entityId) {
        Snap entity = lookups.snap(LookupCodes.MC_ENTITY, entityTypeCode);
        Snap submitted = lookups.snap(LookupCodes.MC_STATUS, LookupCodes.SUBMITTED);
        McRequest r = requests.findByEntityTypeIdAndEntityIdOrderByIdDesc(entity.id(), entityId).stream()
            .filter(x -> x.getStatusId().equals(submitted.id())).findFirst()
            .orElseThrow(() -> ApiException.notFound("MC_NOT_FOUND", "No open request found"));
        setStatus(r, LookupCodes.RETURNED);
        r.setMakerRemarks((r.getMakerRemarks() == null ? "" : r.getMakerRemarks() + " | ") + "Withdrawn by " + CurrentUser.username());
        requests.save(r);
        handlerFor(r).onReturned(r, "Withdrawn by maker");
    }

    private ApprovalHandler handlerFor(McRequest r) {
        ApprovalHandler h = handlers.get(lookups.byId(r.getEntityTypeId()).code());
        if (h == null) throw ApiException.badRequest("MC_NO_HANDLER", "No approval handler for " + r.getEntityTypeDesc());
        return h;
    }

    private void setStatus(McRequest r, String code) {
        Snap s = lookups.snap(LookupCodes.MC_STATUS, code);
        r.setStatusId(s.id());
        r.setStatusDesc(s.desc());
    }

    @Transactional(readOnly = true)
    public McRequestView get(Long id) {
        return view(requests.findById(id).orElseThrow(() -> ApiException.notFound("MC_NOT_FOUND", "Request " + id + " not found")));
    }

    /** scope: "pending" = open items this user may decide now, "mine" = submitted by this user, anything else = all. */
    @Transactional(readOnly = true)
    public List<McRequestView> list(String scope) {
        String user = CurrentUser.username();
        List<McRequest> all = switch (scope == null ? "all" : scope) {
            case "mine" -> requests.findByMakerUserOrderByIdDesc(user);
            case "pending" -> requests.findByStatusIdOrderByIdDesc(lookups.snap(LookupCodes.MC_STATUS, LookupCodes.SUBMITTED).id());
            default -> requests.findAllByOrderByIdDesc();
        };
        return all.stream().map(this::view).filter(v -> !"pending".equals(scope) || v.canDecide()).toList();
    }

    private McRequestView view(McRequest r) {
        List<StepView> st = steps.findByRequestIdOrderByLevelNoAsc(r.getId()).stream()
            .map(s -> new StepView(s.getLevelNo(), s.getCheckerRoleDesc(), s.getCheckerUser(), s.getDecisionDesc(), s.getDecidedAt(), s.getRemarks())).toList();
        String code = lookups.byId(r.getStatusId()).code();
        String user = CurrentUser.username();
        boolean canDecide = LookupCodes.SUBMITTED.equals(code) && !user.equals(r.getMakerUser()) && CurrentUser.has(authorityFor(r.getCurrentLevel()))
            && st.stream().noneMatch(s -> user.equals(s.checkerUser()));
        return new McRequestView(r.getId(), r.getRequestNo(), r.getEntityTypeDesc(), r.getEntityId(), r.getEntityDesc(), r.getActionDesc(), r.getStatusDesc(), code,
            r.getMakerUser(), r.getMakerAt(), r.getMakerRemarks(), r.getCurrentLevel(), r.getLevelsRequired(), r.getPayloadBefore(), r.getPayloadAfter(), st, canDecide);
    }
}
