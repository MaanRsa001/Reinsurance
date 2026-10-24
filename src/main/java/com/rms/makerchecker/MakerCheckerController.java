package com.rms.makerchecker;

import com.rms.makerchecker.McDtos.DecisionRequest;
import com.rms.makerchecker.McDtos.McRequestView;
import com.rms.master.LookupCodes;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/maker-checker")
public class MakerCheckerController {
    private final MakerCheckerService service;

    public MakerCheckerController(MakerCheckerService service) {
        this.service = service;
    }

    @GetMapping("/requests")
    @PreAuthorize("hasAuthority('TREATY_VIEW')")
    public List<McRequestView> list(@RequestParam(defaultValue = "pending") String scope) {
        return service.list(scope);
    }

    @GetMapping("/requests/{id}")
    @PreAuthorize("hasAuthority('TREATY_VIEW')")
    public McRequestView get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping("/requests/{id}/approve")
    @PreAuthorize("hasAuthority('TREATY_CHECK')")
    public McRequestView approve(@PathVariable Long id, @RequestBody(required = false) DecisionRequest body) {
        return service.decide(id, LookupCodes.APPROVED, body == null ? null : body.remarks());
    }

    @PostMapping("/requests/{id}/reject")
    @PreAuthorize("hasAuthority('TREATY_CHECK')")
    public McRequestView reject(@PathVariable Long id, @RequestBody DecisionRequest body) {
        return service.decide(id, LookupCodes.REJECTED, body.remarks());
    }

    @PostMapping("/requests/{id}/return")
    @PreAuthorize("hasAuthority('TREATY_CHECK')")
    public McRequestView returnToMaker(@PathVariable Long id, @RequestBody DecisionRequest body) {
        return service.decide(id, LookupCodes.RETURNED, body.remarks());
    }
}
