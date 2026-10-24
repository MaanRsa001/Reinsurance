package com.rms.treaty;

import com.rms.common.PageResponse;
import com.rms.common.ValidationIssue;
import com.rms.treaty.TreatyDtos.DiffEntry;
import com.rms.treaty.TreatyDtos.RenewRequest;
import com.rms.treaty.TreatyDtos.SaveResult;
import com.rms.treaty.TreatyDtos.SimulationRequest;
import com.rms.treaty.TreatyDtos.SimulationResult;
import com.rms.treaty.TreatyDtos.SubmitRequest;
import com.rms.treaty.TreatyDtos.TreatyRequest;
import com.rms.treaty.TreatyDtos.TreatySummary;
import com.rms.treaty.TreatyDtos.TreatyView;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/treaties")
public class TreatyController {
    private final TreatyService service;

    public TreatyController(TreatyService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('TREATY_VIEW')")
    public PageResponse<TreatySummary> search(@RequestParam(required = false) String q, @RequestParam(required = false) Long typeId,
                                              @RequestParam(required = false) Long statusId, @RequestParam(required = false) Long lobId,
                                              @RequestParam(required = false) Integer uwYear, @RequestParam(defaultValue = "true") boolean latestOnly,
                                              @PageableDefault(size = 20, sort = "treatyCode") Pageable pageable) {
        return PageResponse.of(service.search(q, typeId, statusId, lobId, uwYear, latestOnly, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('TREATY_VIEW')")
    public TreatyView get(@PathVariable Long id) {
        return service.get(id);
    }

    @GetMapping("/code/{code}/versions")
    @PreAuthorize("hasAuthority('TREATY_VIEW')")
    public List<TreatySummary> versions(@PathVariable String code) {
        return service.versions(code);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('TREATY_MAKE')")
    public SaveResult create(@RequestBody TreatyRequest request) {
        return service.create(request);
    }

    /** Edit / update: in place for drafts, new draft version for active treaties (see TreatyService javadoc). */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('TREATY_MAKE')")
    public SaveResult update(@PathVariable Long id, @RequestBody TreatyRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TREATY_MAKE')")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    /** Validate an unsaved payload (used by the wizard before saving). */
    @PostMapping("/validate")
    @PreAuthorize("hasAuthority('TREATY_VIEW')")
    public List<ValidationIssue> validate(@RequestBody TreatyRequest request) {
        return service.validate(request);
    }

    @GetMapping("/{id}/validate")
    @PreAuthorize("hasAuthority('TREATY_VIEW')")
    public List<ValidationIssue> validateStored(@PathVariable Long id) {
        return service.validate(id);
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('TREATY_MAKE')")
    public TreatyView submit(@PathVariable Long id, @RequestBody(required = false) SubmitRequest body) {
        return service.submit(id, body == null ? null : body.remarks());
    }

    @PostMapping("/{id}/withdraw")
    @PreAuthorize("hasAuthority('TREATY_MAKE')")
    public TreatyView withdraw(@PathVariable Long id) {
        return service.withdraw(id);
    }

    @PostMapping("/{id}/renew")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('TREATY_MAKE')")
    public SaveResult renew(@PathVariable Long id, @RequestBody RenewRequest request) {
        return service.renew(id, request);
    }

    @PostMapping("/{id}/simulate")
    @PreAuthorize("hasAuthority('TREATY_VIEW')")
    public SimulationResult simulate(@PathVariable Long id, @RequestBody SimulationRequest request) {
        return service.simulate(id, request);
    }

    @GetMapping("/{id}/diff")
    @PreAuthorize("hasAuthority('TREATY_VIEW')")
    public List<DiffEntry> diff(@PathVariable Long id, @RequestParam("with") Long otherId) {
        return service.diff(id, otherId);
    }
}
