package com.rms.erp;

import com.rms.common.PageResponse;
import com.rms.erp.ErpPolicyDtos.PolicyTxnRequest;
import com.rms.erp.ErpPolicyDtos.PolicyTxnSummary;
import com.rms.erp.ErpPolicyDtos.PolicyTxnView;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
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

/**
 * ERP policy transactions (erp_policy_txn). Cover lines (erp_policy_cover) are created, updated and returned
 * through the "covers" array of the policy transaction request / response.
 * Policy numbers can contain slashes, so they are passed as query parameters.
 */
@RestController
@RequestMapping("/api/v1/erp")
public class ErpPolicyController {
    private final ErpPolicyService service;

    public ErpPolicyController(ErpPolicyService service) {
        this.service = service;
    }

    // ------------------------------------------------------------------ erp_policy_txn

    @GetMapping("/policy-txns")
    @PreAuthorize("hasAuthority('TREATY_VIEW')")
    public PageResponse<PolicyTxnSummary> search(@RequestParam(required = false) String q, @RequestParam(required = false) String policyNo,
                                                 @RequestParam(required = false) Long productId, @RequestParam(required = false) Long txnTypeId,
                                                 @RequestParam(required = false) Long inboundMessageId,
                                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate txnDateFrom,
                                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate txnDateTo,
                                                 @PageableDefault(size = 20, sort = {"policyNo", "txnDate", "id"}, direction = Sort.Direction.ASC) Pageable pageable) {
        return PageResponse.of(service.search(q, policyNo, productId, txnTypeId, inboundMessageId, txnDateFrom, txnDateTo, pageable));
    }

    @GetMapping("/policy-txns/{id}")
    @PreAuthorize("hasAuthority('TREATY_VIEW')")
    public PolicyTxnView get(@PathVariable Long id) {
        return service.get(id);
    }

    /** Full history of one policy (all its transactions, oldest first, with covers). */
    @GetMapping("/policy-txns/by-policy")
    @PreAuthorize("hasAuthority('TREATY_VIEW')")
    public List<PolicyTxnView> byPolicy(@RequestParam String policyNo) {
        return service.byPolicyNo(policyNo);
    }

    /**
     * Creates the policy transaction and its cover lines in one call.
     * Send the cover lines in the optional "covers" array of the request; the response returns them in "covers"
     * (with totalSumInsured / totalSumInsuredAfter / totalPremium calculated from them).
     */
    @PostMapping("/policy-txns")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('TREATY_MAKE')")
    public PolicyTxnView create(@RequestBody PolicyTxnRequest request) {
        return service.create(request);
    }

    @PutMapping("/policy-txns/{id}")
    @PreAuthorize("hasAuthority('TREATY_MAKE')")
    public PolicyTxnView update(@PathVariable Long id, @RequestBody PolicyTxnRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/policy-txns/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TREATY_MAKE')")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
